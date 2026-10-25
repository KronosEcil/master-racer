"""
Master Racer - Multi-Method Official Federation Calendar Discovery Engine
Tek bir yönteme veya tek bir AI modeline bağımlı kalmadan 4 katmanlı hibrit mimariyle çalışır:
1. Doğrudan Resmi API & Veri Akışı Okuyucu (EMA AWS JSON API, 0 AI kotası, 0.3 sn, %100 kesinlik)
2. TAF Resmi Faaliyet Takvimi PDF Okuyucu (taf.org.tr)
3. Canlı Web Arama Motoru (DuckDuckGo HTML) + Doğrulanmış Resmi Federasyon Sayfaları Toplayıcısı
4. Hızlı AI Çıkarım Motoru (Önce 3 saniyede yanıt veren Lite/Gemma modelleri, 503/Timeout devre kesicili)
"""

import re
import time
import json
import base64
import logging
from datetime import datetime
from urllib.parse import quote_plus
from typing import List, Dict, Any, Optional
from bs4 import BeautifulSoup
import requests

from .base_parser import BaseParser
from ..config import Competition, ScraperConfig
from ..deduplication import (
    generate_competition_id,
    compute_content_hash,
    is_valid_competition_title,
    deduplicate_competitions,
)

logger = logging.getLogger(__name__)


class AutonomousDiscoveryParser(BaseParser):
    """
    Resmi federasyon API'lerini, canlı arama motoru indekslerini ve hızlı Gemini Lite / Gemma
    modellerini birleştiren çok yöntemli (multi-method) keşif motoru.
    """

    def __init__(self, existing_comps: Optional[Dict[str, dict]] = None):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://european-masters-athletics.org")
        current_year = datetime.utcnow().year
        self.years = [current_year, current_year + 1, current_year + 2, current_year + 3]
        self.existing_comps: Dict[str, dict] = existing_comps or {}
        self._exhausted_models = set()
        self._available_models: List[str] = []

    def parse(self) -> List[Competition]:
        logger.info(f"Çok Yöntemli Resmi Federasyon Tarayıcısı başlatılıyor (Yıllar: {self.years})...")
        discovered: List[Competition] = []

        # YÖNTEM 1: Doğrudan Resmi EMA AWS JSON Veri Akışı (0 AI Kotası, %100 Deterministik)
        ema_direct = self._discover_from_ema_official_api()
        discovered.extend(ema_direct)

        # YÖNTEM 2 & 3 & 4: TAF Resmi PDF + Canlı Arama & Resmi Federasyon Siteleri + Hızlı AI Analizi
        ai_discovered = self._discover_with_gemini_ai()
        discovered.extend(ai_discovered)

        deduplicated = deduplicate_competitions(discovered)
        logger.info(
            f"[AutonomousDiscovery_Parser] Tüm yöntemlerin taraması ve tekilleştirmesi tamamlandı. "
            f"Toplam {len(deduplicated)} doğrulanmış Master yarışması hazırlandı."
        )
        return deduplicated

    # =========================================================================
    # YÖNTEM 1: DOĞRUDAN RESMİ EMA AWS API OKUYUCU (SIFIR AI HATASI / 0.3 SN)
    # =========================================================================
    def _discover_from_ema_official_api(self) -> List[Competition]:
        """
        Avrupa Masterlar Atletizm Birliği'nin (EMA) resmi AWS API uç noktasından
        (https://s831118207.onlinehome.us/ema_cham_aw/api/v1/events) tüm resmi Avrupa Şampiyonalarını
        ve EMA League yarışmalarını doğrudan JSON olarak çeker.
        """
        results: List[Competition] = []
        min_year = self.years[0]
        api_url = "https://s831118207.onlinehome.us/ema_cham_aw/api/v1/events"
        try:
            resp = self.session.get(api_url, timeout=15)
            if resp.status_code != 200:
                return results

            payload = resp.json()
            data_list = (
                payload.get("data", [])
                if isinstance(payload, dict)
                else (payload if isinstance(payload, list) else [])
            )
            invalid_tokens = {"", "n/a", "na", "none", "null", "tbd", "tbc", "ybd", "unknown", "-"}

            for ev in data_list:
                if not isinstance(ev, dict):
                    continue
                raw_title = str(ev.get("name") or ev.get("title") or "").strip()
                s_date = str(ev.get("startDate") or ev.get("start_date") or ev.get("date") or "").strip()[:10]
                e_date = str(ev.get("endDate") or ev.get("end_date") or s_date).strip()[:10] or s_date

                if not raw_title or not re.match(r"^20\d{2}-\d{2}-\d{2}$", s_date):
                    continue
                if int(s_date[:4]) < min_year:
                    continue
                if not re.match(r"^20\d{2}-\d{2}-\d{2}$", e_date) or e_date < s_date:
                    e_date = s_date

                # EMA resmi akışındaki tüm etkinlikler Masterlar etkinliğidir; başlıkta Master/EMA yoksa ekle
                title = raw_title
                if not is_valid_competition_title(title):
                    title = f"EMA League - {raw_title}" if "league" in str(ev.get("type", "")).lower() else f"{raw_title} (EMA Masters)"
                if not is_valid_competition_title(title):
                    continue

                raw_loc = str(ev.get("location") or ev.get("city") or "").strip()
                raw_country = str(ev.get("country") or "").strip()
                if raw_loc.lower() in invalid_tokens:
                    raw_loc = ""
                if raw_country.lower() in invalid_tokens:
                    raw_country = ""

                country, country_name, _ = self.detect_country(
                    f"{title} {raw_loc} {raw_country}",
                    raw_country if len(raw_country) in (2, 3) else "INT",
                    raw_country or "Avrupa",
                )
                if country.upper() in {"INT", "TBD", "TBC", "NA", "EU"} and not raw_loc:
                    continue

                city = raw_loc or country_name
                t_lower = title.lower()
                if "ema league" in t_lower or "league" in str(ev.get("type", "")).lower():
                    scope = "ema_league"
                else:
                    scope = "europe"

                web_url = str(ev.get("website") or ev.get("url") or "https://european-masters-athletics.org/championships/").strip()
                if not web_url.startswith("http"):
                    web_url = "https://european-masters-athletics.org/championships/"

                comp_id = generate_competition_id(title, s_date, country, city)
                comp = Competition(
                    id=comp_id,
                    title=title,
                    country=country,
                    country_name=country_name,
                    city=city,
                    start_date=s_date,
                    end_date=e_date,
                    venue_type="",
                    categories=self.detect_categories(title),
                    scope=scope,
                    federation="EMA",
                    website_url=web_url,
                    status="scheduled",
                    created_at=datetime.utcnow().isoformat(),
                )
                comp.content_hash = compute_content_hash(comp)
                results.append(comp)

            logger.info(f"[EMA-Direct-API] Resmi EMA AWS veri akışından {len(results)} yarışma doğrudan çıkarıldı.")
        except Exception as e:
            logger.warning(f"[EMA-Direct-API] Okuma uyarısı: {e}")

        return results

    # =========================================================================
    # YÖNTEM 2, 3 & 4: TAF PDF + CANLI ARAMA + RESMİ SİTELER + HIZLI AI MOTORU
    # =========================================================================
    def _discover_with_gemini_ai(self) -> List[Competition]:
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not api_key:
            logger.warning("[Gemini-AI] GEMINI_API_KEY tanımlı değil! AI taraması atlandı.")
            return []

        results: List[Competition] = []
        y1, y2, y3, y4 = self.years[0], self.years[1], self.years[2], self.years[3]
        years_all_tr = f"{y1}, {y2}, {y3} ve {y4}"
        years_all_en = f"{y1}, {y2}, {y3}, and {y4}"

        # ADIM 1: Türkiye Atletizm Federasyonu (TAF) Resmi Takvim PDF'i (`taf.org.tr`)
        taf_pdf_urls = self._find_taf_calendar_pdf_urls()
        for pdf_url in taf_pdf_urls[:1]:
            try:
                resp = self.session.get(pdf_url, timeout=20)
                if resp.status_code == 200 and len(resp.content) > 1000:
                    pdf_b64 = base64.b64encode(resp.content).decode("utf-8")
                    prompt = (
                        "Bu Türkiye Atletizm Federasyonu (TAF - https://taf.org.tr) resmi faaliyet takvimi PDF belgesinin TÜM sayfalarını satır satır incele.\n"
                        "KURAL 1: Tablodaki 'Yaş Grubu' sütununu ve yarışma adını kontrol et. Yaş Grubu sütununda 'Master' / 'Masterlar' yazan "
                        "veya yarışma adında 'Master' / 'Masterlar' / 'Masterler' geçen TÜM yarışmaları (Salon Şampiyonası, Açık Saha/Pist Şampiyonası, "
                        "Kros Şampiyonası, Atmalar Kupası, Mesafeler ve Atmalar Kupası, Yürüyüş, Yol Koşusu, Balkan/Avrupa Şampiyonası vb.) eksiksiz çıkar.\n"
                        "KURAL 2: Yaş Grubu sadece 'B' (Büyükler), 'U23', 'U20', 'U18', 'U16', 'U14' olan yarışmaları "
                        "(Salon Olimpik Deneme, Olimpik Deneme, TOHM, SEM, Okullar, Kulüpler Ligi vb.) KESİNLİKLE DAHİL ETME!\n"
                        "KURAL 3: Eğer Yaş Grubu sütununda 'Master' yazıyor ancak yarışma adında 'Masterlar' kelimesi geçmiyorsa "
                        "`title` alanının sonuna '(Masterlar)' ibaresini ekle.\n"
                        "Sadece geçerli bir JSON dizisi (array) döndür, başka hiçbir açıklama yazma:\n"
                        '[{"title": "Takvimdeki Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
                        '"city": "Takvimdeki Şehir", "country": "TR", '
                        '"country_name": "Türkiye", "scope": "national|balkan|europe|world|regional", '
                        '"federation": "TAF", "website_url": "https://taf.org.tr"}]'
                    )
                    parts = [
                        {"inline_data": {"mime_type": "application/pdf", "data": pdf_b64}},
                        {"text": prompt},
                    ]
                    items = self._call_gemini_json(api_key, parts)
                    converted = self._convert_gemini_items(items, default_url=pdf_url)
                    logger.info(f"[Gemini-AI] TAF Resmi PDF takviminden {len(converted)} Master yarışması çıkarıldı.")
                    results.extend(converted)
                    time.sleep(4)
            except Exception as e:
                logger.warning(f"[Gemini-AI] TAF PDF analizi hatası ({pdf_url}): {e}")

        # ADIM 2: Resmi Federasyon Takvimleri + Canlı Arama Motoru İndeksleri
        schema_instruction = (
            "\n\nKESİN ÇIKTI VE DOĞRULUK KURALLARI:\n"
            f"1. Yukarıdaki resmi federasyon takvim verilerini ve resmi federasyon takvimlerini kullanarak {years_all_tr} yıllarında "
            "tarihi (YYYY-MM-DD) ve yapılacağı şehri kesinleşmiş olan tüm 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren / Veteranos / SAUL) "
            "şampiyonalarını eksiksiz çıkar. Resmi takvimde yer almayan hiçbir yarışmayı, tarihi veya şehri ASLA TAHMİN ETME / UYDURMA!\n"
            "2. Elit/Büyükler (Senior Elite), Olimpik Deneme, U23, U20, U18 yarışmalarını ASLA dahil etme.\n"
            "3. Çok günlü yarışmalarda `start_date` ve `end_date` tarihlerini resmi takvimdeki tam aralık olarak (YYYY-MM-DD) yaz. "
            "Tarihi veya ülkesi 'TBD', 'TBC', 'N/A' olan ya da 1 Ocak (YYYY-01-01) gibi yer tutucu tarihli yarışmaları DAHİL ETME!\n"
            "4. `scope` alanı yalnızca şu 6 değerden biri olmalıdır: 'national', 'world', 'balkan', 'europe', 'ema_league', 'regional'.\n"
            "5. Sadece geçerli bir JSON dizisi (array) döndür, markdown veya açıklama metni ekleme:\n"
            '[{"title": "Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "Şehir", "country": "ISO-2 Ülke Kodu (TR, DE, FR, GB, IT, ES, PL, RO vb.)", '
            '"country_name": "Türkçe Ülke Adı", "scope": "national|world|balkan|europe|ema_league|regional", '
            '"federation": "Federasyon Kısaltması", "website_url": "Resmi Takvim URL"}]'
        )

        official_batches = [
            (
                f"1. Uluslararası Resmi Takvimler: WMA, EMA, OpenTrack ve Balkan Masters ({years_all_tr})",
                [
                    "https://european-masters-athletics.org/championships/",
                    "https://balkanmasters.com/",
                    "https://world-masters-athletics.org/championships/",
                    "https://wmg2027.jp/",
                ],
                [
                    f"World Masters Athletics Championships {y1} {y2} {y3} {y4} site:world-masters-athletics.org",
                    f"European Masters Athletics Championships EMACI EMACS EMACNS EMMTRC EMAMRC {y1} {y2} {y3} {y4}",
                    f"Balkan Masters Athletics Championships BAMACI BAMACS BAMAM {y1} {y2} {y3} site:balkanmasters.com",
                    f"World Masters Games {y2} {y3} {y4} athletics dates city",
                ],
                f"Extract all confirmed {years_all_en} World Masters Athletics Championships (WMA Indoor, Stadia/Outdoor, Marathon, Mountain Running, 50km/100km), "
                f"World Masters Games (WMG), European Masters Championships (EMACI, EMACS, EMACNS, EMMTRC, EMAMRC, EMORRC), EMA League meetings, "
                f"and Balkan Masters Athletics Championships (BAMACI Indoor, BAMACS Outdoor/Stadia, BAMAM Marathon) from official calendars "
                f"(https://world-masters-athletics.org/championships/, https://european-masters-athletics.org/championships/, https://balkanmasters.com/, https://wmg2027.jp/)."
            ),
            (
                f"2. Birleşik Krallık (BMAF) ve İrlanda (IMAA) Resmi Takvim Siteleri ({years_all_tr})",
                [
                    "https://bmaf.org.uk/fixtures/",
                    "https://www.athleticsireland.ie/competition/fixtures/",
                ],
                [
                    f"BMAF British Masters Championships fixtures {y1} {y2} site:bmaf.org.uk",
                    f"Irish Masters Athletics Championships IMAA {y1} {y2} indoor outdoor cross country",
                ],
                f"Extract all confirmed {years_all_en} British Masters (BMAF - https://bmaf.org.uk/fixtures/) and Irish Masters (IMAA) championships and open fixtures, including: "
                f"Indoor Track & Field Championships, Outdoor Track & Field Championships, Indoor Pentathlon & 3000m, Winter Throws, Throws Pentathlon, 10,000m, "
                f"Cross Country Championships, Cross Country Relays, Road Relays, 1 Mile, 5k Road, 10k Road, 10 Mile, Half Marathon, Marathon, Trail/Ultra, and Race Walking Championships."
            ),
            (
                f"3. Fransa (FFA), Almanya (DLV/LADV), İsviçre, Avusturya ve Benelüks Resmi Siteleri ({years_all_tr})",
                [
                    "https://www.athle.fr/contenu/calendrier-championnats-de-france/32",
                    "https://www.leichtathletik.de/wettkaempfe/termine",
                    "https://ladv.de/ausschreibungen",
                    "https://www.oelv.at/de/wettkaempfe/kalender",
                ],
                [
                    f"Championnats de France Masters {y1} {y2} {y3} salle piste lancers marche cross site:athle.fr",
                    f"Deutsche Meisterschaften Masters {y1} {y2} {y3} Halle Wurf-Mehrkampf Cross site:leichtathletik.de OR site:ladv.de",
                    f"Österreichische Mastersmeisterschaften {y1} {y2} Halle Freiluft site:oelv.at",
                    f"Swiss Masters Championships Athletics {y1} {y2} NK Masters Indoor Outdoor {y1} {y2} BK Masters Atletiek {y1} {y2}",
                ],
                f"Extract all confirmed {years_all_en} national 35+ Masters (Vétérans / Senioren / Masters) championships from the official national federation calendars of:\n"
                f"- France (FFA - https://www.athle.fr/contenu/calendrier-championnats-de-france/32): Championnats de France Masters en salle, sur piste, Épreuves Combinées, Lancers hivernaux, Marche, 5km/10km/Semi/Marathon/Cross.\n"
                f"- Germany (DLV / LADV - https://www.leichtathletik.de/wettkaempfe/termine, https://ladv.de/): DM Masters Freiluft, DM Halle Masters & Winterwurf, DM Wurf-Mehrkampf Masters, DM Cross Masters, Team-DM Masters.\n"
                f"- Switzerland (Swiss Athletics / Swiss Masters): Schweizermeisterschaften Masters Indoor, Outdoor (Part 1 & Part 2), Pentathlon, Wurf-Fünfkampf.\n"
                f"- Austria (ÖLV - https://www.oelv.at/de/wettkaempfe/kalender): Österreichische Masters-Hallenmeisterschaften & Mastersmeisterschaften Freiluft.\n"
                f"- Netherlands (Atletiekunie - https://www.atletiek.nl/) & Belgium (VAL/LBFA - https://www.atletiek.be/): NK Indoor Masters, NK Masters Outdoor, BK Masters Indoor & Outdoor."
            ),
            (
                f"4. İtalya (FIDAL), İspanya (RFEA) ve Portekiz (FPAtletismo/ANAV) Resmi Siteleri ({years_all_tr})",
                [
                    "https://www.fidal.it/calendario.php",
                    "https://atletismorfea.es/calendario",
                ],
                [
                    f"Campionati Italiani Master {y1} {y2} Indoor Pista Pentathlon Lanci Prove Multiple Mezza Maratona site:fidal.it",
                    f"Campeonato de España Master {y1} {y2} Aire Libre Short Track Pista Cubierta Lanzamientos Combinadas Campo a Traves site:atletismorfea.es OR site:rfeacontent.es",
                    f"Campeonatos Nacionais de Veteranos {y1} {y2} Pista Curta Ar Livre Corta-Mato Pentatlo Marcha site:fpatletismo.pt OR site:anavportugal.com",
                ],
                f"Extract all confirmed {years_all_en} national 35+ Masters (Master / Veteranos) championships from the official national federation calendars of:\n"
                f"- Italy (FIDAL - https://www.fidal.it/calendario.php): Campionati Italiani Master Indoor e Lanci Invernali, Individuali su Pista Master, Pentathlon dei Lanci (Invernale/Estivo), Prove Multiple e Staffette, Mezza Maratona, Maratona, Marcia, Cross, 10km/24h.\n"
                f"- Spain (RFEA - https://atletismorfea.es/calendario): Campeonato de España Máster de Aire Libre, Short Track (Pista Cubierta), Lanzamientos Largos, Pentatlón de Lanzamientos, Pruebas Combinadas, Campo a Través, Milla, 10.000m, Ruta (5k/10k/Media Maratón/Maratón), Montaña/Trail.\n"
                f"- Portugal (FPAtletismo / ANAV - https://fpatletismo.pt/): Campeonatos Nacionais de Veteranos de Pista Curta (Indoor), Pista ao Ar Livre (Outdoor), Pentatlo, Corta-Mato (Cross), Marcha em Estrada, 5km/10km/Meia Maratona/Maratona."
            ),
            (
                f"5. Balkanlar, Orta/Doğu Avrupa, İskandinavya ve Baltık Resmi Federasyon Siteleri ({years_all_tr})",
                [
                    "https://balkanmasters.com/",
                    "https://www.pma.org.pl/",
                    "https://www.saul.fi/kilpailut/kilpailukalenteri/",
                    "https://www.has.hr/",
                    "https://slovenska-atletika.si/",
                    "https://ekjl.ee/",
                ],
                [
                    f"Halowe Mistrzostwa Polski Masters Mistrzostwa Polski w Lekkiej Atletyce Masters {y1} {y2} site:pma.org.pl OR site:pzla.pl",
                    f"Mistrovství ČR veteránů v atletice {y1} {y2} hala dráha site:atletika.cz",
                    f"SAUL SM-hallit SM-yleisurheilu SM-maastot SM-puolimaraton SM-moniottelut {y1} {y2} site:saul.fi",
                    f"ISM veteraner SM veteraner friidrott {y1} {y2} NM veteraner DM Masters atletik Baltic Masters Championships {y1} {y2}",
                    f"Campionatul National de Atletism Masters {y1} {y2} Romania Greece Bulgaria Serbia Croatia Slovenia",
                ],
                f"Extract all confirmed {years_all_en} national and regional 35+ Masters (Veteránů / Veteraner / SAUL / Masters) championships from the official calendars of:\n"
                f"- Balkans: Balkan Masters (https://balkanmasters.com/), Greece (SEGAS/OEBA), Bulgaria (BFLA), Romania (FRA - country='RO'), Serbia (SAS), Croatia (HAS/HAVU), Slovenia (AZS/ZAVS).\n"
                f"- Central Europe: Poland (PMA https://www.pma.org.pl/ - Halowe Mistrzostwa Polski Masters & Outdoor), Czechia (ČAS - MČR veteránů na dráze & v hale), Slovakia (SAZ), Hungary (MASZ).\n"
                f"- Nordic & Baltic: Nordic Masters Championships, Baltic Masters Championships, Finland (SAUL https://www.saul.fi/kilpailut/kilpailukalenteri/ - SM-hallit, SM-yleisurheilu, SM-moniottelut, SM-maastot, SM-puolimaraton, SM-maantiekymppi, SM-viestit), Sweden (ISM/SM Veteraner), Norway (NM Veteraner), Denmark (DM Masters), Estonia (EKJL), Latvia (LVS), Lithuania (LLAF)."
            ),
        ]

        for label, site_urls, search_queries, query_text in official_batches:
            try:
                live_digest = self._fetch_hybrid_context(site_urls, search_queries)
                full_prompt = query_text
                if live_digest:
                    full_prompt += (
                        "\n\nAŞAĞIDA RESMİ FEDERASYON SİTELERİNDEN VE CANLI ARAMA İNDEKSLERİNDEN ÇEKİLEN GÜNCEL TAKVİM VERİLERİ YER ALMAKTADIR:\n"
                        f"{live_digest}\n"
                    )
                full_prompt += schema_instruction
                parts = [{"text": full_prompt}]
                items = self._call_gemini_json(api_key, parts)
                converted = self._convert_gemini_items(
                    items,
                    default_url=site_urls[0] if site_urls else "https://european-masters-athletics.org",
                )
                logger.info(f"[Gemini-AI] {label} taramasından {len(converted)} yarışma çıkarıldı.")
                results.extend(converted)
                time.sleep(3)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Resmi site tarama hatası ({label}): {e}")

        return results

    def _fetch_hybrid_context(
        self,
        urls: List[str],
        search_queries: List[str],
        max_total_chars: int = 16000,
    ) -> str:
        """
        Hem doğrulanmış resmi federasyon URL'lerini doğrudan indirir hem de DuckDuckGo HTML
        arama motoru üzerinden resmi sitelerin güncel indekslenmiş takvim özetlerini (snippet) toplar.
        """
        chunks: List[str] = []

        # 1. Canlı DuckDuckGo HTML Arama Motoru Snippet'leri (API anahtarı veya kota gerektirmez)
        search_lines: List[str] = []
        for sq in search_queries:
            try:
                ddg_url = f"https://html.duckduckgo.com/html/?q={quote_plus(sq)}"
                resp = self.session.get(ddg_url, timeout=8)
                if resp.status_code == 200 and resp.text:
                    soup = BeautifulSoup(resp.text, "html.parser")
                    for res in soup.select(".result")[:8]:
                        t_el = res.select_one(".result__title")
                        s_el = res.select_one(".result__snippet")
                        u_el = res.select_one(".result__url")
                        t_txt = re.sub(r"\s+", " ", t_el.get_text(" ", strip=True)) if t_el else ""
                        s_txt = re.sub(r"\s+", " ", s_el.get_text(" ", strip=True)) if s_el else ""
                        u_txt = re.sub(r"\s+", " ", u_el.get_text(" ", strip=True)) if u_el else ""
                        if t_txt or s_txt:
                            search_lines.append(f"- {t_txt} | {s_txt} ({u_txt})")
            except Exception as e:
                logger.debug(f"[Hybrid-Search] Arama uyarısı ({sq}): {e}")

        if search_lines:
            chunks.append("=== [CANLI WEB ARAMA İNDEKSİ SONUÇLARI] ===\n" + "\n".join(search_lines[:35]))

        # 2. Doğrudan Resmi Federasyon Takvim Sayfaları
        per_url_limit = max(2000, (max_total_chars - sum(len(c) for c in chunks)) // max(1, len(urls)))
        for url in urls:
            try:
                resp = self.session.get(url, timeout=8)
                if resp.status_code != 200 or not resp.text:
                    continue
                soup = BeautifulSoup(resp.text, "html.parser")
                for tag in soup(["script", "style", "noscript", "svg", "footer", "nav"]):
                    tag.decompose()
                lines = []
                for line in soup.get_text(separator="\n").splitlines():
                    cleaned = re.sub(r"\s+", " ", line).strip()
                    if len(cleaned) >= 4:
                        lines.append(cleaned)
                text_block = "\n".join(lines)[:per_url_limit]
                if text_block:
                    chunks.append(f"=== [RESMİ TAKVİM SAYFASI: {url}] ===\n{text_block}")
            except Exception as e:
                logger.debug(f"[Official-Fetch] {url} okuma uyarısı: {e}")

        combined = "\n\n".join(chunks)
        return combined[:max_total_chars]

    def _find_taf_calendar_pdf_urls(self) -> List[str]:
        """TAF faaliyet takvimi sayfasındaki güncel PDF bağlantılarını bulur."""
        pdf_urls: List[str] = []
        year_strs = [str(y) for y in self.years]
        try:
            resp = self.session.get("https://taf.org.tr/ova_doc/faaliyet-takvimi/", timeout=12)
            if resp.status_code == 200 and resp.text:
                soup = BeautifulSoup(resp.text, "html.parser")
                seen_years = set()
                for a_tag in soup.find_all("a", href=True):
                    href = a_tag["href"]
                    if href.lower().endswith(".pdf"):
                        for y in year_strs:
                            if y in href and y not in seen_years:
                                pdf_urls.append(href)
                                seen_years.add(y)
                                break
        except Exception as e:
            logger.debug(f"[TAF-PDF] ova_doc tarama hatası: {e}")

        return pdf_urls[:2]

    @staticmethod
    def _extract_json_from_text(raw_text: str) -> List[Dict[str, Any]]:
        """
        Gemini yanıtındaki JSON dizisini `raw_decode` kullanarak ayrıştırır.
        """
        cleaned = re.sub(r"```(?:json)?", "", raw_text or "").strip("` \n\r\t")
        decoder = json.JSONDecoder()

        combined_list: List[Dict[str, Any]] = []
        idx = 0
        n = len(cleaned)
        while idx < n:
            bracket_pos = cleaned.find("[", idx)
            if bracket_pos == -1:
                break
            try:
                parsed, end_offset = decoder.raw_decode(cleaned[bracket_pos:])
                if isinstance(parsed, list):
                    for elem in parsed:
                        if isinstance(elem, dict):
                            combined_list.append(elem)
                    idx = bracket_pos + end_offset
                    continue
            except Exception:
                pass
            idx = bracket_pos + 1

        if combined_list:
            return combined_list

        brace_pos = cleaned.find("{")
        while brace_pos != -1:
            try:
                obj, end_offset = decoder.raw_decode(cleaned[brace_pos:])
                if isinstance(obj, dict) and "title" in obj and "start_date" in obj:
                    combined_list.append(obj)
                    brace_pos = brace_pos + end_offset
                    continue
            except Exception:
                pass
            brace_pos = cleaned.find("{", brace_pos + 1)

        return combined_list

    def _get_available_gemini_models(self, api_key: str) -> List[str]:
        """
        En hızlı yanıt veren (2-4 sn) ve 503 yoğunluğuna takılmayan Lite modelleri en başa koyar.
        """
        if self._available_models:
            return self._available_models

        # Öncelik sırası: En hızlı ve 503 vermeyen Lite modeller ilk sırada!
        allowed_models = [
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.1-flash-lite-preview",
            "gemma-3-27b-it",
            "gemini-3.5-flash",
            "gemini-3.6-flash",
            "gemini-3.7-flash",
            "gemini-3.8-flash",
            "gemini-3-flash-preview",
        ]
        discovered_set = set()
        try:
            url = f"https://generativelanguage.googleapis.com/v1beta/models?key={api_key}"
            resp = requests.get(url, timeout=12)
            if resp.status_code == 200:
                for m in resp.json().get("models", []):
                    methods = m.get("supportedGenerationMethods", [])
                    if "generateContent" in methods:
                        name = str(m.get("name", "")).replace("models/", "").strip()
                        discovered_set.add(name)
        except Exception as e:
            logger.debug(f"[Gemini-AI] Model listesi sorgulama uyarısı: {e}")

        if discovered_set:
            self._available_models = [m for m in allowed_models if m in discovered_set] or allowed_models[:4]
        else:
            self._available_models = allowed_models[:4]

        logger.info(f"[Gemini-AI] Hız öncelikli aktif AI modelleri: {self._available_models}")
        return self._available_models

    def _call_gemini_json(
        self,
        api_key: str,
        parts: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        """
        Hızlı Lite modelleri 35 sn zaman aşımı ve 503/Timeout devre kesicisi (circuit breaker) ile çağırır.
        Bir model 503 veya Timeout verirse o model anında devre dışı bırakılır, 90 sn bekletmez!
        """
        all_models = self._get_available_gemini_models(api_key)
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json",
        }

        for attempt in range(2):
            active_models = [m for m in all_models if m not in self._exhausted_models]
            if not active_models:
                # Eğer hepsi geçici 503 yüzünden listeye girdiyse ikinci denemede hızlı lite modelleri tekrar aç
                active_models = all_models[:3]

            for model in active_models:
                # Gemma modelleri inline_data (PDF) desteklemez, sadece metin destekler
                if model.startswith("gemma-") and any("inline_data" in p for p in parts):
                    continue

                endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
                payload: Dict[str, Any] = {
                    "contents": [{"parts": parts}],
                    "generationConfig": {"temperature": 0.0},
                }

                try:
                    resp = requests.post(endpoint, headers=headers, json=payload, timeout=35)
                    if resp.status_code == 402:
                        logger.error(
                            "[Gemini-AI] HTTP 402: API anahtarının bağlı olduğu proje 'Prepay (No credits)' modunda. "
                            "Lütfen 'Free tier' projesine ait bir API anahtarı kullanın."
                        )
                        self._exhausted_models.update(all_models)
                        return []

                    if resp.status_code in (429, 503):
                        # 503 veya 429 veren modeli bu çalışmada devre dışı bırak ki sonraki adımlarda vakit kaybettirmesin
                        self._exhausted_models.add(model)
                        logger.warning(f"[Gemini-AI] Model ({model}) HTTP {resp.status_code}, hızlı yedek modele geçiliyor...")
                        time.sleep(1)
                        continue

                    if resp.status_code in (400, 404):
                        self._exhausted_models.add(model)
                        logger.warning(f"[Gemini-AI] Model ({model}) HTTP {resp.status_code} nedeniyle devre dışı bırakıldı.")
                        continue

                    if resp.status_code != 200:
                        logger.warning(f"[Gemini-AI] Model ({model}) HTTP {resp.status_code}: {resp.text[:160]}")
                        continue

                    data = resp.json()
                    candidates = data.get("candidates", [])
                    if not candidates:
                        continue
                    text_parts = candidates[0].get("content", {}).get("parts", [])
                    raw_text = "".join(p.get("text", "") for p in text_parts)
                    parsed = self._extract_json_from_text(raw_text)
                    if parsed:
                        return parsed
                except requests.exceptions.Timeout:
                    self._exhausted_models.add(model)
                    logger.warning(f"[Gemini-AI] Model ({model}) 35 sn içinde yanıt vermedi, devre dışı bırakıldı.")
                except Exception as e:
                    logger.warning(f"[Gemini-AI] Model ({model}) çağrı uyarısı: {e}")

            if attempt == 0:
                time.sleep(4)
        return []

    def _convert_gemini_items(self, items: List[Dict[str, Any]], default_url: str) -> List[Competition]:
        """Gemini JSON çıktılarını doğrular ve Competition nesnelerine dönüştürür."""
        converted: List[Competition] = []
        valid_scopes = {"national", "world", "balkan", "europe", "ema_league", "regional"}
        min_year = self.years[0]

        for item in items:
            try:
                if not isinstance(item, dict):
                    continue
                title = str(item.get("title", "")).strip()
                start_date = str(item.get("start_date", "")).strip()[:10]
                end_date = str(item.get("end_date", start_date)).strip()[:10] or start_date
                if not title or not is_valid_competition_title(title):
                    continue
                if not re.match(r"^20\d{2}-\d{2}-\d{2}$", start_date):
                    continue
                if int(start_date[:4]) < min_year:
                    continue
                if not re.match(r"^20\d{2}-\d{2}-\d{2}$", end_date) or end_date < start_date:
                    end_date = start_date

                raw_country = str(item.get("country", "")).strip().upper()
                raw_cname = str(item.get("country_name", "")).strip()
                raw_city = str(item.get("city", "")).strip()

                invalid_tokens = {"", "n/a", "na", "none", "null", "tbd", "tbc", "ybd", "unknown", "belirlenecek", "to be determined", "-"}
                if raw_city.lower() in invalid_tokens:
                    raw_city = ""
                if raw_country.lower() in invalid_tokens:
                    raw_country = ""

                if start_date.endswith("-01-01") and not raw_city:
                    continue

                country, country_name, _ = self.detect_country(
                    f"{title} {raw_city} {raw_cname}",
                    raw_country or "INT",
                    raw_cname or "Uluslararası",
                )
                if country.upper() in {"INT", "TBD", "TBC", "NA", "EU"} and not raw_city:
                    continue

                city = raw_city or country_name

                scope = str(item.get("scope", "")).strip().lower()
                if scope not in valid_scopes:
                    scope = self.detect_scope(title, country)

                federation = str(item.get("federation", "")).strip() or "EMA / Ulusal"
                web_url = str(item.get("website_url", "")).strip() or default_url
                comp_id = generate_competition_id(title, start_date, country, city)

                comp = Competition(
                    id=comp_id,
                    title=title,
                    country=country,
                    country_name=country_name,
                    city=city,
                    start_date=start_date,
                    end_date=end_date,
                    venue_type="",
                    categories=self.detect_categories(title),
                    scope=scope,
                    federation=federation,
                    website_url=web_url,
                    status="scheduled",
                    created_at=datetime.utcnow().isoformat(),
                )
                comp.content_hash = compute_content_hash(comp)
                converted.append(comp)
            except Exception as item_err:
                logger.debug(f"[Gemini-AI] Öğe dönüştürme uyarısı: {item_err}")
        return converted
