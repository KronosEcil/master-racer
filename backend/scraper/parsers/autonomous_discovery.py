"""
Master Racer - Multi-Method Official Federation Calendar Discovery Engine
Tek bir yönteme veya tek bir AI modeline bağımlı kalmadan 4 katmanlı hibrit mimariyle çalışır:
1. Doğrudan Resmi API & Veri Akışı Okuyucu (EMA AWS JSON API, 0 AI kotası, 0.3 sn, %100 kesinlik)
2. TAF Resmi Faaliyet Takvimi PDF Okuyucu (taf.org.tr)
3. Canlı Web Arama Motoru (DuckDuckGo HTML) + Doğrulanmış Resmi Federasyon Sayfaları Toplayıcısı
4. Hızlı AI Çıkarım Motoru (Önce 3 saniyede yanıt veren Lite/Gemma modelleri, 503/Timeout devre kesicili)
"""

import io
import re
import time
import json
import base64
import logging
from datetime import date, datetime, timezone
from urllib.parse import quote_plus, urljoin
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
        current_year = datetime.now(timezone.utc).year
        self.years = [current_year, current_year + 1, current_year + 2, current_year + 3]
        self.existing_comps: Dict[str, dict] = existing_comps or {}
        self._exhausted_models = set()
        self._available_models: List[str] = []
        self._model_503_counts: Dict[str, int] = {}
        self.dropped_ids = set()

    def parse(self) -> List[Competition]:
        logger.info(f"Çok Yöntemli Resmi Federasyon Tarayıcısı başlatılıyor (Yıllar: {self.years})...")
        discovered: List[Competition] = []

        # 1. Doğrudan OpenTrack & LADV Yapısal Master Takvim Akışları (trust_level=3, 0 AI kotası)
        direct_feeds = self._discover_from_opentrack_and_ladv()
        discovered.extend(direct_feeds)

        # 2. TAF Resmi PDF (trust_level=3) + Resmi Federasyon Siteleri (trust_level=2) + Canlı Arama İndeksleri
        ai_discovered = self._discover_with_gemini_ai()
        discovered.extend(ai_discovered)

        deduplicated = deduplicate_competitions(discovered)

        # 3. Otonom Yapay Zeka Mantıksal Süzgeç & Denetim Katmanı (AI Reasoning & Self-Filtering)
        audited = self._ai_autonomous_audit_and_filter(deduplicated)
        final_list = deduplicate_competitions(audited)
        logger.info(
            f"[AutonomousDiscovery_Parser] Tüm yöntemlerin taraması, AI mantıksal süzgeci ve tekilleştirmesi tamamlandı. "
            f"Toplam {len(final_list)} doğrulanmış Master yarışması hazırlandı."
        )
        return final_list

    # =========================================================================
    # 1. DOĞRUDAN OPENTRACK & LADV YAPISAL TAKVİM OKUYUCU (TRUST LEVEL = 3)
    # =========================================================================
    def _discover_from_opentrack_and_ladv(self) -> List[Competition]:
        """
        Avrupa Master yarışmalarının resmi kayıt/takvim altyapısı olan OpenTrack (bmaf.opentrack.run,
        ema.opentrack.run, data.opentrack.run) ve Almanya LADV sayfalarındaki yapısal JSON-LD / tablo
        verilerini doğrudan (0 AI kotası ve trust_level=3 kesinliği ile) okur.
        """
        direct_results: List[Competition] = []
        min_year = self.years[0]
        feed_urls = [
            ("https://ema.opentrack.run/", "EMA", "europe"),
            ("https://bmaf.opentrack.run/", "BMAF", "national"),
            ("https://data.opentrack.run/en-gb/x/", "OpenTrack", "national"),
        ]
        for feed_url, default_fed, default_scope in feed_urls:
            try:
                resp = self.session.get(feed_url, timeout=10)
                if resp.status_code != 200 or not resp.text:
                    continue
                soup = BeautifulSoup(resp.text, "html.parser")
                # A) JSON-LD SportsEvent / Event yapısal verisi varsa doğrudan çıkar
                for script_tag in soup.find_all("script", type="application/ld+json"):
                    try:
                        ld_data = json.loads(script_tag.string or "")
                        ld_items = ld_data if isinstance(ld_data, list) else [ld_data]
                        for ev in ld_items:
                            if not isinstance(ev, dict):
                                continue
                            title = str(ev.get("name", "")).strip()
                            s_date = str(ev.get("startDate", ""))[:10]
                            e_date = str(ev.get("endDate", s_date))[:10] or s_date
                            if not title or not is_valid_competition_title(title):
                                continue
                            if not re.match(r"^20\d{2}-\d{2}-\d{2}$", s_date) or int(s_date[:4]) < min_year:
                                continue
                            loc = ev.get("location") or {}
                            addr = loc.get("address") if isinstance(loc, dict) else {}
                            city = ""
                            c_code = ""
                            if isinstance(addr, dict):
                                city = str(addr.get("addressLocality", "")).strip()
                                c_code = str(addr.get("addressCountry", "")).strip().upper()
                            elif isinstance(loc, dict):
                                city = str(loc.get("name", "")).strip()
                            ev_url = str(ev.get("url", "")).strip() or feed_url
                            ev_url = urljoin(feed_url, ev_url)
                            country, country_name, _ = self.detect_country(
                                f"{title} {city} {c_code}",
                                c_code or ("GB" if "bmaf" in feed_url else "INT"),
                                "Birleşik Krallık" if "bmaf" in feed_url else "Uluslararası",
                            )
                            if not city and country in ("INT", "EU"):
                                continue
                            city = city or country_name
                            scope = self.detect_scope(title, country) or default_scope
                            comp_id = generate_competition_id(title, s_date, country, city)
                            comp = Competition(
                                id=comp_id,
                                title=title,
                                country=country,
                                country_name=country_name,
                                city=city,
                                start_date=s_date,
                                end_date=e_date if e_date >= s_date else s_date,
                                scope=scope,
                                federation=default_fed,
                                website_url=ev_url,
                                trust_level=3,
                                status="scheduled",
                                created_at=datetime.now(timezone.utc).isoformat(),
                            )
                            comp.content_hash = compute_content_hash(comp)
                            direct_results.append(comp)
                    except Exception:
                        pass
            except Exception as e:
                logger.debug(f"[OpenTrack-Direct] {feed_url} okuma uyarısı: {e}")

        if direct_results:
            logger.info(f"[OpenTrack-Direct] Doğrudan yapısal akıştan {len(direct_results)} Master yarışması çıkarıldı.")
        return direct_results

    # =========================================================================
    # TAF PDF + CANLI ARAMA İNDEKSLERİ + RESMİ SİTELER + HIZLI AI MOTORU
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

        # ADIM 1: Türkiye Atletizm Federasyonu (TAF) Resmi Takvim PDF'leri (`taf.org.tr` - 2026 ve 2027)
        taf_pdf_urls = self._find_taf_calendar_pdf_urls()
        for pdf_url in taf_pdf_urls[:2]:
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
                    converted = self._convert_gemini_items(
                        items,
                        default_url="https://taf.org.tr",
                        default_trust_level=3,
                        default_regulations_url=pdf_url,
                    )
                    logger.info(f"[Gemini-AI] TAF Resmi PDF takviminden {len(converted)} Master yarışması çıkarıldı.")
                    results.extend(converted)
                    time.sleep(3)
            except Exception as e:
                logger.warning(f"[Gemini-AI] TAF PDF analizi hatası ({pdf_url}): {e}")

        # ADIM 2: Resmi Federasyon Takvimleri + Canlı Arama Motoru İndeksleri (2026 ve Gelecek Yıllar)
        schema_instruction = (
            "\n\nKESİN ÇIKTI VE DOĞRULUK KURALLARI:\n"
            f"1. Hem aşağıda indirilen canlı resmi sayfa/arama verilerini HEM DE ilgili ulusal ve uluslararası federasyonların "
            f"(EMA, WMA, World Masters Sport/IMGA, BAMA, BMAF, IMAA, FFA, DLV, ÖLV, Swiss Masters, Atletiekunie, VAL/LBFA, FIDAL, RFEA, FPAtletismo/ANAV, "
            f"PMA/PZLA, ČAS, SAZ, MASZ, SAUL, SFIF, NFIF, DAF, SEGAS, FRA, BFLA, SAS, HAS, AZS, EKJL, LVS, LLAF, "
            f"USATF Masters, CMA, AMA, NZMA, OMA, NCCMA, JMA, KAAF, SMA, SMTFA, MMAA, CTMA, TVAA, SLMA, MFI, ASBiH, ASCG, AFM, FAK, FAM, UAF, KOEAS, MAAA, FRÍ, FLA, AAF, GAF, ABRAM, CAVRA, FEMACHI, SAMA, ASUDAMA, AFMA) "
            f"{years_all_tr} ve sonrası açıklanmış tüm gelecek yıllar için resmi şampiyona takvimlerini birleştirerek "
            "tarihi (YYYY-MM-DD) ve ülkesi kesinleşmiş olan TÜM 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren / Veteranos / SAUL) şampiyonalarını eksiksiz çıkar.\n"
            f"2. ÖZELLİKLE {y1}, {y2}, {y3}, {y4} ve daha ileri yıllar için açıklanmış ulusal ve uluslararası Master şampiyonalarını mutlaka dahil et ({y1} öncesi eski yılları dahil etme)!\n"
            "3. Elit/Büyükler (Senior Elite), Olimpik Deneme, U23, U20, U18 yarışmalarını ASLA dahil etme.\n"
            "4. Çok günlü yarışmalarda `start_date` ve `end_date` tarihlerini resmi takvimdeki tam aralık olarak (YYYY-MM-DD) yaz. "
            "Tarihi ('TBD', 'TBC', 'N/A', 'Not Specified') belirsiz olan ya da 1 Ocak (YYYY-01-01) gibi yer tutucu tarihli yarışmaları DAHİL ETME! "
            "ANCAK resmi federasyon takviminde veya resmi planlama PDF tablosunda tarihi (gün ve ay olarak YYYY-MM-DD) ve ülkesi kesinleşmiş olan ulusal şampiyonalarda, "
            "ev sahibi şehir (`Città` / `Sede` / `Lieu` / `Ort`) sütunu henüz boş bırakılmışsa yarışmayı KESİNLİKLE ELEME; `city` alanına Türkçe ülke adını (örneğin `'İtalya'`) yaz!\n"
            "5. Her yarışmanın `city`, `country` ve `country_name` alanlarına yalnızca O YARIŞMANIN kendi resmi ev sahibi şehrini ve ülkesini yaz (örneğin Perth -> AU/Avustralya, Lahti -> FI/Finlandiya, Como -> IT/İtalya, Abu Dhabi -> AE/Birleşik Arap Emirlikleri; farklı yıllardaki Masters Games ev sahiplerini birbirine karıştırma!).\n"
            "6. `scope` alanı yalnızca şu 6 değerden biri olmalıdır: 'national', 'world', 'balkan', 'europe', 'ema_league', 'regional'.\n"
            "7. Metin içinde yarışmaya ait doğrudan detay/kayıt sayfası bağlantısı `[URL: https://...]` varsa `website_url` alanına bu doğrudan bağlantıyı yaz. "
            "Yarışmaya ait resmi şartname, reglaman, bülten veya kılavuz PDF bağlantısı `[PDF: https://...]` varsa `regulations_url` alanına tam URL olarak yaz (yoksa null bırak).\n"
            "8. TAHMİN VE YIL KAYDIRMA YASAĞI (KRİTİK): Eğer bir ülkenin 2027, 2028 veya 2029 resmi takvimi aşağıdaki indirilen resmi sayfa metinlerinde henüz yayınlanmamışsa, "
            "bir önceki yılın (2026) tarihini 1-2 gün (52 hafta = 364 gün) kaydırarak ASLA tahmin üretme! Yalnızca indirilen resmi metinlerde veya mevcut doğrulanmış kayıtlarda açıkça tarihi yazan yarışmaları döndür.\n"
            "9. Sadece geçerli bir JSON dizisi (array) döndür, markdown veya açıklama metni ekleme:\n"
            '[{"title": "Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "Şehir", "country": "ISO-2 Ülke Kodu (TR, DE, FR, GB, IT, ES, PL, RO, FI, AU, JP, NZ vb.)", '
            '"country_name": "Türkçe Ülke Adı", "scope": "national|world|balkan|europe|ema_league|regional", '
            '"federation": "Federasyon Kısaltması", "website_url": "Doğrudan Yarışma veya Resmi Takvim URL", '
            '"regulations_url": "Varsa Resmi Şartname PDF URL veya null"}]'
        )

        official_batches = [
            (
                f"1. Uluslararası Resmi Takvimler: WMA, WMG, EMA ({y1}-{y4}+), EMA League ve Balkan Masters",
                {"INT", "WORLD", "EUROPE", "BALKAN"},
                [
                    "https://european-masters-athletics.org/championships/",
                    "https://ema.opentrack.run/",
                    "https://balkanmasters.com/",
                    "https://world-masters-athletics.org/championships/",
                    "https://www.worldmasterssport.com/about-the-masters-games/next-masters-games/",
                ],
                [
                    f"European Masters Athletics Championships {y1} {y2} {y3} {y4} EMACI EMACS EMACNS EMMTRC EMAMRC",
                    f"World Masters Athletics Championships {y1} {y2} {y3} {y4} Stadia Indoor Mountain Marathon",
                    f"site:worldmasterssport.com World Masters Games Open Masters Games Winter World Masters Games {y1} {y2} {y3} {y4}",
                    f"Balkan Masters Athletics Championships {y1} {y2} {y3} {y4} BAMACI BAMACS BAMAM",
                    f"EMA League {y1} {y2} {y3} European Masters Athletics",
                ],
                f"List ALL confirmed {years_all_en} (and beyond) international Masters championships and Masters Games from the live official federation pages below "
                f"(EMA https://european-masters-athletics.org/championships/, WMA https://world-masters-athletics.org/championships/, "
                f"World Masters Sport https://www.worldmasterssport.com/about-the-masters-games/next-masters-games/, BAMA https://balkanmasters.com/), "
                f"extracting each event's true host city, country, and dates directly from the official calendar text:\n"
                f"1. European Masters Athletics Championships ({y1}, {y2}, {y3}, {y4}+): EMACI (Indoor - even years only: 2026 Torun, 2028 Glasgow), EMACS (Stadia - odd years only: 2027 Jyvaskyla), EMACNS (Non-Stadia: 2026 Machico, 2028 Galway), EMMTRC (Mountain & Trail), EMAMRC (Marathon).\n"
                f"2. World Masters Sport (https://www.worldmasterssport.com/) Masters Games ({y1}, {y2}, {y3}, {y4}+: e.g. Open Masters Games Abu Dhabi {y1}, World Masters Games Kansai {y2}, Open Masters Games Como Lake {y2}, Winter World Masters Games Lahti {y3}, World Masters Games Perth {y4}) — use the exact host city and country listed for each edition on worldmasterssport.com.\n"
                f"3. World Masters Athletics (WMA) Championships ({y1}, {y2}, {y3}, {y4}+): Outdoor/Stadia (even years: 2026 Daegu), Indoor (odd years: 2027 Gainesville), Mountain Running, Marathon & Road.\n"
                f"4. EMA League Meetings ({y1}, {y2}, {y3}) & Balkan Masters Championships (only if officially announced on balkanmasters.com)."
            ),
            (
                f"2. Birleşik Krallık (BMAF) ve İrlanda (IMAA) ({y1}-{y4}) Ulusal Takvimleri",
                {"GB", "IE"},
                [
                    "https://bmaf.org.uk/fixtures/",
                    "https://bmaf.opentrack.run/",
                    "https://www.athleticsireland.ie/competition/fixtures/",
                ],
                [
                    f"BMAF British Masters Championships fixtures {y1} {y2} {y3} Indoor Outdoor Cross Country Throws",
                    f"British Masters Road Relays 5k 10k Half Marathon Marathon {y1} {y2} {y3} bmaf",
                    f"Irish Masters Athletics Championships IMAA {y1} {y2} {y3} Indoor Outdoor",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ British Masters (BMAF - https://bmaf.org.uk/fixtures/ and https://bmaf.opentrack.run/) and Irish Masters (IMAA) championships and open fixtures:\n"
                f"- BMAF Indoor & Outdoor Track & Field Championships ({y1}, {y2}, {y3}+)\n"
                f"- BMAF Indoor Pentathlon & 3000m, Winter Throws & Outdoor Throws Pentathlon Championships ({y1}, {y2}, {y3}+)\n"
                f"- BMAF Cross Country Championships & Relays, Road Relays, 1 Mile, 5k, 10k, 10 Mile, Half Marathon, Marathon, 10,000m Track, and Race Walking Championships ({y1}, {y2}, {y3}+)\n"
                f"- Irish Masters (IMAA) Indoor (Athlone) & Outdoor Track & Field Championships (Tullamore) ({y1}, {y2}, {y3}+)."
            ),
            (
                f"3. Fransa (FFA/CNAM), Almanya (DLV), İsviçre, Avusturya ve Benelüks ({y1}-{y4}) Ulusal Takvimleri",
                {"FR", "DE", "CH", "AT", "NL", "BE"},
                [
                    "https://masters.athle.fr/asp.net/espaces.html/html.aspx?id=40541",
                    "https://masters.athle.fr/asp.net/espaces.html/html.aspx?id=40540",
                    "https://masters.athle.fr/asp.net/espaces.html/html.aspx?id=51036",
                    "https://masters.athle.fr/asp.net/espaces.html/html.aspx?id=41949",
                    "https://masters.athle.fr/asp.net/espaces.html/html.aspx?id=40543",
                    "https://masters.athle.fr/asp.net/espaces.html/html.aspx?id=42028",
                    "https://ladv.de/meisterschaften",
                    "https://ladv.de/ausschreibungen",
                    "https://www.leichtathletik.de/wettkaempfe/termine",
                    "https://www.oelv.at/de/wettkaempfe/kalender",
                ],
                [
                    f"Championnats de France Masters {y1} {y2} {y3} salle piste lancers marche cross site:masters.athle.fr OR site:athle.fr",
                    f"Deutsche Meisterschaften Masters {y1} {y2} {y3} Halle Freiluft Wurf-Mehrkampf Cross ladv",
                    f"Österreichische Mastersmeisterschaften {y1} {y2} {y3} Halle Freiluft oelv",
                    f"Schweizermeisterschaften Masters {y1} {y2} {y3} NK Masters Indoor Outdoor BK Masters Atletiek",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ national and regional 35+ Masters (Vétérans / Senioren / Masters) championships from the official federation calendars of France (https://masters.athle.fr/), Germany (DLV / LADV), Switzerland, Austria, Netherlands, and Belgium:\n"
                f"- France (FFA / CNAM - https://masters.athle.fr/ - country='FR'): Championnats de France Masters en Salle, sur Piste / Estival, Épreuves Combinées, Marche, Cross, Lancers, Route ({y1}, {y2}, {y3}+).\n"
                f"- Germany (DLV - country='DE'): Deutsche Meisterschaften (DM) Masters Freiluft, DM Halle & Winterwurf, DM Wurf-Mehrkampf, DM Cross, DM Straßenlauf/Halbmarathon/Marathon, Team-DM Masters ({y1}, {y2}, {y3}+).\n"
                f"- Switzerland (Swiss Masters - country='CH'), Austria (ÖLV - country='AT'), Netherlands (Atletiekunie - country='NL') & Belgium (VAL/LBFA - country='BE'): all Indoor & Outdoor Masters National Championships ({y1}, {y2}, {y3}+)."
            ),
            (
                f"4. İtalya (FIDAL), İspanya (RFEA) ve Portekiz (FPAtletismo/ANAV) ({y1}-{y4}) Ulusal Takvimleri",
                {"IT", "ES", "PT"},
                [
                    "https://www.fidal.it/content/Master/174936",
                    "https://www.fidal.it/content/Norme-e-Regolamenti/111218",
                    "https://www.fidal-lombardia.it/index.php?option=com_content&view=article&id=17812:campionati-italiani-2027-ufficiali-date-e-minimi&catid=19&Itemid=438&lang=it",
                    "https://atletismorfea.es/calendario",
                ],
                [
                    f"Campionati Italiani Master {y1} {y2} {y3} Indoor Pista Pentathlon Lanci Prove Multiple Mezza Maratona fidal",
                    f"Campeonato de España Master {y1} {y2} {y3} Aire Libre Short Track Pista Cubierta Lanzamientos Combinadas Campo a Traves rfea",
                    f"Campeonatos Nacionais de Veteranos {y1} {y2} {y3} Pista Curta Ar Livre Corta-Mato Pentatlo Marcha fpatletismo anav",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ national 35+ Masters (Master / Veteranos) championships from the official national federation calendars and official PDF planning tables of Italy, Spain, and Portugal:\n"
                f"- Italy (FIDAL - country='IT'): Extract ALL {y1} and {y2}+ Master national championships from the FIDAL calendar and the official FIDAL Planning Campionati Federali PDF, including: "
                f"Campionati Italiani di Prove Multiple Indoor Master e Campionati Italiani Invernali di Lanci Master (e.g. 6-7 Feb 2027), "
                f"Campionati Italiani Master Indoor (Ancona, e.g. 5-8 Mar 2026 & 5-7 Mar 2027), "
                f"Campionati Italiani Individuali su Pista Master (Outdoor), "
                f"Campionati Italiani Pentathlon dei Lanci Master Invernale & Estivo, "
                f"Campionati Italiani Master di Prove Multiple e Staffette, "
                f"Campionati Italiani Individuali e di Società di Cross / Corsa Campestre Master & Staffetta Cross Master, "
                f"Finale Nazionale C.D.S. / Societari Master su Pista, Trofeo delle Regioni Master, "
                f"Mezza Maratona, Maratona, Marcia, 10.000m su Pista, 5km, 50km, 100km, 24h, Montagna/Trail ({y1}, {y2}, {y3}+). "
                f"IMPORTANT: For {y2} FIDAL championships in the official Planning PDF where the exact dates are confirmed (e.g. 6/7-feb-27) but the Città column is blank, MUST include them and set city='İtalya'!\n"
                f"- Spain (RFEA - country='ES'): Campeonato de España Máster Short Track / Pista Cubierta (Indoor), Aire Libre (Outdoor), Lanzamientos Largos, Pentatlón de Lanzamientos, Pruebas Combinadas, Campo a Través, Milla, 10.000m, Ruta, Trail/Montaña ({y1}, {y2}, {y3}+).\n"
                f"- Portugal (FPAtletismo / ANAV - country='PT'): Campeonatos Nacionais de Veteranos de Pista Curta (Indoor), Ar Livre (Outdoor), Pentatlo de Lançamentos, Corta-Mato, Marcha, Estrada ({y1}, {y2}, {y3}+)."
            ),
            (
                f"5. Balkanlar ve Orta Avrupa (Polonya, Çekya, Macaristan, Yunanistan, Romanya, Bulgaristan, Hırvatistan, Sırbistan, Slovenya) ({y1}-{y4}) Ulusal Takvimleri",
                {"PL", "CZ", "SK", "HU", "GR", "RO", "BG", "RS", "HR", "SI", "AL"},
                [
                    "https://www.atletika-veterani.cz/kalendar/",
                    "https://www.masterstrack.sk/",
                    "https://pzlam.pl/pl/",
                    "https://atletika.hu/",
                    "https://www.has.hr/",
                    "https://slovenska-atletika.si/",
                    "https://www.segas.gr/",
                    "https://fra.ro/",
                ],
                [
                    f"Halowe Mistrzostwa Polski Masters Mistrzostwa Polski w Lekkiej Atletyce Masters {y1} {y2} {y3} Torun pma",
                    f"Halové MČR veteránů Mistrovství ČR veteránů na dráze {y1} {y2} {y3} Majstrovstvá SR veteránov",
                    f"Szenior Atlétikai Magyar Bajnokság {y1} {y2} {y3} fedettpályás szabadtéri",
                    f"Campionatul National de Atletism Masters {y1} {y2} {y3} Bucuresti Panhellenic Masters Athletics Championships",
                    f"Prvenstvo Hrvatske za veterane {y1} {y2} {y3} Prvenstvo Slovenije za veterane Bulgaria Serbia Masters Athletics",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ national 35+ Masters (Masters / Veteránů / Veteránov / Szenior / Veterane) championships from the official calendars of Central Europe and the Balkans:\n"
                f"- Poland (PZLAM - country='PL'), Czechia (SV ČAS - https://www.atletika-veterani.cz/kalendar/ - country='CZ'), Slovakia (SAZ - country='SK'), Hungary (MASZ - country='HU'), Greece (SEGAS/OEBA - country='GR'), Romania (FRA - country='RO'), Bulgaria (BFLA - country='BG'), Serbia (SAS - country='RS'), Croatia (HAS/HAVU - country='HR'), Slovenia (AZS/ZAVS - country='SI'), Albania (FSHA - country='AL') Indoor & Outdoor Masters Championships ({y1}, {y2}, {y3}+)."
            ),
            (
                f"6. İskandinavya (Finlandiya, İsveç, Norveç, Danimarka) ve Baltık (Estonya, Letonya, Litvanya) ({y1}-{y4}) Ulusal Takvimleri",
                {"FI", "SE", "NO", "DK", "EE", "LV", "LT"},
                [
                    "https://www.saul.fi/kilpailu/saul-kilpailukalenteri/",
                    "https://www.nmaci2027.se/",
                    "https://www.friidrott.se/tavling-landslag/nyhetsarkiv-tavling-landslag/sm-tavlingar-2027-datum-och-arrangorer-klara/",
                    "https://www.friidrett.no/terminlister/hovedterminliste-2027/",
                    "https://dansk-atletik.dk/nyheder/2026/10/dm-kalender-2027/",
                    "https://ekjl.ee/voistlused/voistluskalender/",
                    "https://athletics.lv/lv/kalendars",
                ],
                [
                    f"SAUL SM-hallit SM-yleisurheilu SM-maastot SM-puolimaraton SM-moniottelut {y1} {y2} {y3}",
                    f"ISM veteraner SM veteraner friidrott {y1} {y2} {y3} Sverige",
                    f"NM veteraner friidrett {y1} {y2} {y3} DM Masters atletik Nordic Masters Championships",
                    f"Baltic Masters Athletics Championships {y1} {y2} {y3} Eesti meistrivõistlused veteranidele Latvijas čempionāts veterāniem",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ national and regional 35+ Masters (SAUL / Veteraner / Masters) championships from the official calendars of the Nordic and Baltic countries:\n"
                f"- Nordic & Baltic Regional: Nordic Masters Championships Indoor (PM-hallit / NMACI) & Outdoor, Baltic Open Masters Athletics Championships ({y1}, {y2}, {y3}+).\n"
                f"- Finland (SAUL - country='FI'), Sweden (SFIF - country='SE'), Norway (NFIF - country='NO'), Denmark (DAF - country='DK'), Estonia (EKJL - country='EE'), Latvia (LVS - country='LV'), Lithuania (LLAF - country='LT') Indoor & Outdoor Masters Championships ({y1}, {y2}, {y3}+)."
            ),
            (
                f"7. Kuzey Amerika (ABD, Kanada) ve Okyanusya (Avustralya, Yeni Zelanda) ({y1}-{y4}) Ulusal ve Kıtasal Takvimleri",
                {"US", "CA", "AU", "NZ"},
                [
                    "https://www.usatf.org/programs/masters",
                    "https://www.usatf.org/events",
                    "https://canadianmasters.ca/events/",
                    "https://www.australianmastersathletics.org.au/events/",
                    "https://nzmastersathletics.org.nz/events/",
                    "https://oceaniamastersathletics.org/",
                ],
                [
                    f"USATF Masters Indoor Outdoor Championships {y1} {y2} {y3} Throws Cross Country Road Mile 5k 10k Half Marathon",
                    f"Canadian Masters Athletics Championships CMA {y1} {y2} {y3} Indoor Outdoor Cross Country",
                    f"Australian Masters Athletics National Championships AMA {y1} {y2} {y3} Track and Field Winter Throws",
                    f"New Zealand Masters Athletics Championships NZMA {y1} {y2} {y3} Oceania Masters Athletics Championships OMA NCCMA",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ national and continental 35+ Masters championships from North America and Oceania:\n"
                f"- USA (USATF Masters - https://www.usatf.org/programs/masters - country='US'): USATF Masters Indoor Championships, USATF Masters Outdoor Championships, USATF Masters Throws Championships, USATF Masters Cross Country & Road (1 Mile, 5 km, 10 km, Half Marathon, Marathon) Championships ({y1}, {y2}, {y3}+).\n"
                f"- Canada (CMA - https://canadianmasters.ca/events/ - country='CA'): Canadian Masters Indoor & Outdoor Track & Field Championships, Throws & Cross Country ({y1}, {y2}, {y3}+).\n"
                f"- Australia (AMA - https://www.australianmastersathletics.org.au/events/ - country='AU'): Australian Masters Athletics National Track & Field Championships, Winter Throws Championships ({y1}, {y2}, {y3}+).\n"
                f"- New Zealand (NZMA - https://nzmastersathletics.org.nz/events/ - country='NZ'): NZMA National, North Island & South Island Track & Field Championships ({y1}, {y2}, {y3}+).\n"
                f"- Continental Regional: NCCMA (North, Central American and Caribbean Masters Athletics) Championships & OMA (Oceania Masters Athletics) Championships ({y1}, {y2}, {y3}+)."
            ),
            (
                f"8. Asya ve Orta Doğu (Japonya, Güney Kore, BAE, Singapur, Malezya, Tayvan, Tayland, Sri Lanka, Hindistan, Kazakistan, Özbekistan) ({y1}-{y4}) Ulusal ve Kıtasal Takvimleri",
                {"JP", "KR", "AE", "SG", "MY", "TW", "TH", "LK", "IN", "KZ", "UZ"},
                [
                    "http://www.thaivaa.com/en/home",
                    "http://www.thaivaa.com/news/250.html",
                    "http://www.thaivaa.com/list",
                    "https://sriathletics.lk/events/",
                    "https://www.sgmasterstnf.com/",
                    "https://asia-masters-athletics.com/",
                    "https://asia-masters-athletics.com/calendar-of-event",
                    "https://www.japan-masters.or.jp/",
                    "https://www.mmaa.org.my/",
                    "https://www.ctma.org.tw/",
                ],
                [
                    f"Asia Masters Athletics Championships AMA {y1} {y2} {y3} All Japan Masters Athletics Championships JMA",
                    f"Malaysian International Open Masters Athletics Championships MMAA {y1} {y2} {y3} Singapore Masters Track and Field",
                    f"Thailand Masters Athletics Championship {y1} {y2} TMAC Phetchabun Ratchaburi thaivaa",
                    "กรีฑาสูงอายุชิงชนะเลิศประเทศไทย ครั้งที่ 31 เพชรบูรณ์ 27 31 มกราคม 2570",
                    f"Sri Lanka Masters Athletics Championships {y1} {y2} December Colombo Diyagama SLMA",
                    f"Chinese Taipei Masters Athletics Championships {y1} {y2} {y3} India Masters Athletics Kazakhstan Uzbekistan",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ national and continental 35+ Masters championships from Asia and the Middle East:\n"
                f"- Continental Regional: Asia Masters Athletics Championships (AMA - https://asia-masters-athletics.com/) & Asia-Pacific Masters Games ({y1}, {y2}, {y3}+).\n"
                f"- Thailand (TVAA - http://www.thaivaa.com/ - country='TH', federation='TVAA'): Note that Thai calendars use Buddhist Era years (พ.ศ. 2569 = 2026, พ.ศ. 2570 = 2027). MUST extract BOTH:\n"
                f"  * 30th Thailand Masters Athletics Championships 2026 (2026-03-05 to 2026-03-08, Ratchaburi, country='TH', scope='national', website_url='http://www.thaivaa.com/en/home')\n"
                f"  * 31st Thailand Masters Athletics Championships 2027 / กรีฑาสูงอายุชิงชนะเลิศประเทศไทย ครั้งที่ 31 ประจำปี 2570 ณ จังหวัดเพชรบูรณ์ (27-31 January 2027 -> start_date='2027-01-27', end_date='2027-01-31', city='Phetchabun', country='TH', country_name='Tayland', scope='national', federation='TVAA', website_url='http://www.thaivaa.com/en/news/250.html').\n"
                f"- Sri Lanka (SLMA - Sri Lanka Masters Athletics - https://sriathletics.lk/events/ - country='LK', country_name='Sri Lanka', federation='SLMA'): MUST extract:\n"
                f"  * Sri Lanka Masters Athletics Championships 2026 (19-20 December 2026 -> start_date='2026-12-19', end_date='2026-12-20', city='Colombo', country='LK', country_name='Sri Lanka', scope='national', federation='SLMA', website_url='https://sriathletics.lk/events/') and any confirmed {y2}+ editions.\n"
                f"- Japan (JMA - country='JP'), South Korea (KAAF - country='KR'), United Arab Emirates (UAE - country='AE'), Singapore (SMTFA - https://www.sgmasterstnf.com/ - country='SG'), Malaysia (MMAA - country='MY'), Taiwan / Chinese Taipei (CTMA - country='TW'), India (MFI - country='IN'), Kazakhstan (country='KZ'), Uzbekistan (country='UZ') National & Open Masters Athletics Championships ({y1}, {y2}, {y3}+)."
            ),
            (
                f"9. Diğer Avrupa, Akdeniz, Kafkasya, Güney Amerika ve Afrika (Bosna-Hersek, Karadağ, K. Makedonya, Kosova, Moldova, Ukrayna, Kıbrıs, Malta, İzlanda, Lüksemburg, Azerbaycan, Gürcistan, Brezilya, Arjantin, Şili, Güney Afrika vb.) ({y1}-{y4}) Takvimleri",
                {"BA", "ME", "MK", "XK", "MD", "UA", "CY", "MT", "IS", "LU", "AZ", "GE", "BR", "AR", "CL", "CO", "MX", "PE", "ZA", "MA", "TN", "EG"},
                [
                    "https://asbih.org/",
                    "https://ascg.co.me/",
                    "https://uaf.org.ua/",
                    "https://www.koeas.org.cy/",
                    "https://athleticsmalta.com/",
                    "https://www.fri.is/",
                    "https://www.fla.lu/",
                    "https://athletics.az/",
                    "https://www.abram.org.br/",
                    "https://cavra.org.ar/",
                    "https://www.femachi.cl/",
                    "https://www.samastersathletics.org.za/",
                ],
                [
                    f"Ukraine Cyprus Malta Iceland Luxembourg Bosnia Montenegro Macedonia Moldova Masters Athletics Championships {y1} {y2} {y3}",
                    f"Azerbaijan Georgia Caucasus Masters Athletics Championships {y1} {y2} {y3}",
                    f"Campeonato Sudamericano de Atletismo Master ASUDAMA {y1} {y2} {y3} Campeonato Brasileiro Master ABRAM Argentino CAVRA Chile FEMACHI",
                    f"African Masters Athletics Championships AFMA {y1} {y2} {y3} South African Masters Athletics Championships SAMA",
                ],
                f"List all confirmed {y1}, {y2}, {y3}, and {y4}+ national and continental 35+ Masters (Masters / Veterans / Veteranos) championships from these countries:\n"
                f"- Europe, Mediterranean & Caucasus: Bosnia and Herzegovina (ASBiH - country='BA'), Montenegro (ASCG - country='ME'), North Macedonia (AFM - country='MK'), Kosovo (FAK - country='XK'), Moldova (FAM - country='MD'), Ukraine (UAF - country='UA'), Cyprus (KOEAS - country='CY'), Malta (MAAA - country='MT'), Iceland (FRÍ - country='IS'), Luxembourg (FLA - country='LU'), Azerbaijan (AAF - country='AZ'), Georgia (GAF - country='GE') Indoor & Outdoor Masters Championships ({y1}, {y2}, {y3}+).\n"
                f"- South America & Latin America: South American Masters Championships (ASUDAMA), Brazil (ABRAM - country='BR'), Argentina (CAVRA - country='AR'), Chile (FEMACHI - country='CL'), Colombia (country='CO'), Mexico (country='MX'), Peru (country='PE') National Masters Championships ({y1}, {y2}, {y3}+).\n"
                f"- Africa: African Masters Athletics Championships (AFMA), South Africa (SAMA - country='ZA'), Morocco (country='MA'), Tunisia (country='TN'), Egypt (country='EG') National Masters Championships ({y1}, {y2}, {y3}+)."
            ),
        ]

        for label, batch_countries, site_urls, search_queries, query_text in official_batches:
            try:
                live_digest = self._fetch_hybrid_context(site_urls, search_queries)
                existing_digest = self._build_existing_context(batch_countries)
                full_prompt = query_text
                if existing_digest:
                    full_prompt += (
                        "\n\nMEVCUT DOĞRULANMIŞ VERİTABANI KAYITLARI "
                        "(Bu kayıtlar resmi kaynaklardan doğrulanmıştır; start_date, end_date ve city bilgilerini olduğu gibi koru):\n"
                        f"{existing_digest}\n"
                    )
                if live_digest:
                    full_prompt += (
                        "\n\nAŞAĞIDA RESMİ FEDERASYON SİTELERİNDEN VE CANLI ARAMA İNDEKSLERİNDEN ÇEKİLEN GÜNCEL TAKVİM VERİLERİ YER ALMAKTADIR:\n"
                        f"{live_digest}\n"
                    )
                full_prompt += schema_instruction
                parts = [{"text": full_prompt}]
                items = self._call_gemini_json(api_key, parts, use_search=False)
                converted = self._convert_gemini_items(
                    items,
                    default_url=site_urls[0] if site_urls else "https://european-masters-athletics.org",
                    default_trust_level=2,
                    live_digest=live_digest,
                )
                logger.info(f"[Gemini-AI] {label} taramasından {len(converted)} yarışma çıkarıldı.")
                results.extend(converted)
                time.sleep(3)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Resmi site tarama hatası ({label}): {e}")

        return results

    def _build_existing_context(self, batch_countries: set) -> str:
        """
        Yalnızca resmi kaynaktan doğrulanmış (verified=True veya trust_level>=3) ya da mevcut yıl (2026)
        kayıtlarını özetler; doğrulanmamış gelecek yıl tahminlerinin AI promptuna geri beslenmesini önler.
        """
        if not self.existing_comps:
            return ""
        lines = []
        is_intl_batch = "WORLD" in batch_countries or "EUROPE" in batch_countries
        curr_year_str = str(self.years[0])
        for doc_id, data in self.existing_comps.items():
            if not isinstance(data, dict):
                continue
            c_code = str(data.get("country", "")).upper()
            scope = str(data.get("scope", "")).lower()
            title = str(data.get("title", "")).strip()
            s_date = str(data.get("start_date", "")).strip()[:10]
            e_date = str(data.get("end_date", s_date)).strip()[:10]
            city = str(data.get("city", "")).strip()
            is_verified = data.get("verified") is True
            if not title or not s_date or not is_verified:
                continue
            t_low = title.lower()
            if ("masters games" in t_low or "wmg" in t_low) and "kansai" not in t_low and ("kansai" in city.lower() or c_code == "JP"):
                continue
            if is_intl_batch and scope in {"world", "europe", "balkan", "ema_league"}:
                lines.append(f"- {title} | {s_date}..{e_date} | {city} ({c_code}) | scope={scope}")
            elif not is_intl_batch and c_code in batch_countries and scope in {"national", "regional"}:
                lines.append(f"- {title} | {s_date}..{e_date} | {city} ({c_code}) | scope={scope}")
        return "\n".join(lines[:40])

    def _fetch_hybrid_context(
        self,
        urls: List[str],
        search_queries: List[str],
        max_total_chars: int = 52000,
    ) -> str:
        """
        1. Çoklu Canlı Web Arama Motoru (Bing Web RSS + Google News/Web RSS) üzerinden gerçek web sayfası özetlerini (title + snippet + URL) toplar.
        2. Doğrulanmış resmi federasyon URL'lerini, alt takvim sayfalarını ve sayfalardaki resmi takvim/handbook PDF'lerini indirir.
        """
        search_chunks: List[str] = []
        pdf_chunks: List[str] = []
        html_chunks: List[str] = []
        search_lines: List[str] = []
        seen_snippets = set()

        for sq in search_queries:
            # A) Bing Web Search RSS (Gerçek web sayfası sonuçları, açıklama snippet'leri ve doğrudan linkler)
            try:
                bing_url = f"https://www.bing.com/search?q={quote_plus(sq)}&format=rss"
                b_resp = self.session.get(bing_url, timeout=8)
                if b_resp.status_code == 200 and b_resp.text:
                    b_soup = BeautifulSoup(b_resp.text, "xml")
                    for item in b_soup.find_all("item")[:6]:
                        t_txt = re.sub(r"\s+", " ", item.title.get_text(" ", strip=True)) if item.title else ""
                        d_txt = ""
                        if item.description:
                            raw_desc = BeautifulSoup(item.description.get_text(" ", strip=True), "html.parser").get_text(" ", strip=True)
                            d_txt = re.sub(r"\s+", " ", raw_desc)[:240]
                        l_txt = item.link.get_text(strip=True) if item.link else ""
                        line = f"- {t_txt}" + (f" — {d_txt}" if d_txt else "") + (f" [URL: {l_txt}]" if l_txt.startswith("http") else "")
                        if t_txt and t_txt not in seen_snippets:
                            seen_snippets.add(t_txt)
                            search_lines.append(line)
            except Exception as e:
                logger.debug(f"[Bing-RSS] Arama uyarısı ({sq}): {e}")

            # B) Google News/Web RSS (Başlık + Açıklama Snippet'i)
            try:
                if re.search(r"[\u0e00-\u0e7f]", sq):
                    gn_url = f"https://news.google.com/rss/search?q={quote_plus(sq)}&hl=th&gl=TH&ceid=TH:th"
                else:
                    gn_url = f"https://news.google.com/rss/search?q={quote_plus(sq)}&hl=en-US&gl=US&ceid=US:en"
                resp = self.session.get(gn_url, timeout=8)
                if resp.status_code == 200 and resp.text:
                    soup = BeautifulSoup(resp.text, "xml")
                    for item in soup.find_all("item")[:6]:
                        t_txt = re.sub(r"\s+", " ", item.title.get_text(" ", strip=True)) if item.title else ""
                        d_txt = ""
                        if item.description:
                            raw_desc = BeautifulSoup(item.description.get_text(" ", strip=True), "html.parser").get_text(" ", strip=True)
                            d_txt = re.sub(r"\s+", " ", raw_desc)[:220]
                        line = f"- {t_txt}" + (f" — {d_txt}" if d_txt and d_txt != t_txt else "")
                        if t_txt and t_txt not in seen_snippets:
                            seen_snippets.add(t_txt)
                            search_lines.append(line)
            except Exception as e:
                logger.debug(f"[Google-RSS] Arama uyarısı ({sq}): {e}")

        if search_lines:
            search_chunks.append("=== [CANLI WEB VE HABER ARAMA İNDEKSİ SONUÇLARI] ===\n" + "\n".join(search_lines[:55]))

        # 2. Doğrudan Resmi Federasyon Takvim Sayfaları, Alt Sayfaları ve Takvim/Handbook PDF'leri
        per_url_limit = 11000
        pdf_char_limit = 14000
        ignored_domains = ("facebook.com", "instagram.com", "twitter.com", "x.com", "youtube.com", "linkedin.com")
        deep_link_keywords = (
            ".pdf", "reglement", "regolamento", "regulamento", "regulations", "manual", "handbook",
            "ausschreibung", "circular", "bulletin", "opentrack.run", "ladv.de/ausschreibung",
            "espaces.html", "championship", "calendario", "fixtures", "event", "meeting",
            "masters-games", "next-masters-games", "all-masters-games", "schedule", "programme"
        )
        calendar_pdf_keywords = (
            "planning", "calendario", "calendar", "kalender", "handbook", "bulletin",
            "takvim", "fixtures", "meisterschaft", "calendrier", "attivit", "schedule", "ama"
        )
        excluded_pdf_keywords = (
            "minimi", "quote", "modulo", "modulistica", "affiliazion", "tesserament",
            "organigramma", "statuto", "antidoping", "primati", "record", "classific", "risultat"
        )
        discovered_pdf_urls: List[str] = []
        discovered_subpage_urls: List[str] = []
        seen_urls = set(urls)

        for url in urls:
            try:
                resp = self.session.get(url, timeout=10)
                if resp.status_code != 200 or not resp.content:
                    continue
                content_type = (resp.headers.get("Content-Type") or "").lower()
                if url.lower().endswith(".pdf") or "application/pdf" in content_type:
                    pdf_text = self._extract_pdf_text(resp.content, max_chars=pdf_char_limit)
                    if pdf_text:
                        pdf_chunks.append(f"=== [RESMİ TAKVİM PDF BELGESİ: {url}] ===\n{pdf_text}")
                    continue
                if not resp.text:
                    continue
                cleaned_html = re.sub(r'data:image/[^"\'\s>]+', "", resp.text)
                soup = BeautifulSoup(cleaned_html, "html.parser")
                for tag in soup(["script", "style", "noscript", "svg", "footer", "nav", "header", "aside"]):
                    tag.decompose()
                for sel in [
                    "#ctnLeft", "#ctnLeftMenu", "#ctnTop", ".menuLeft", "#sidebar", ".sidebar",
                    "#main-header", "#top-header", ".et-l--header", ".et_pb_menu", ".et-menu-nav", ".sub-menu", ".et_mobile_menu",
                    ".phpdebugbar", "#phpdebugbar"
                ]:
                    for el in soup.select(sel):
                        el.decompose()

                for a_tag in soup.find_all("a", href=True):
                    raw_href = (a_tag.get("href") or "").strip()
                    if not raw_href or raw_href.startswith(("#", "javascript:", "mailto:", "tel:")):
                        continue
                    full_href = urljoin(url, raw_href)
                    full_href = re.sub(r"(https?://[^/]+)/(?:\.\./)+", r"\1/", full_href)
                    href_low = full_href.lower()
                    if any(d in href_low for d in ignored_domains):
                        continue
                    anchor_txt = a_tag.get_text(" ", strip=True)
                    combined_hint = f"{href_low} {anchor_txt.lower()}"
                    if href_low.endswith(".pdf"):
                        a_tag.string = f"{anchor_txt} [PDF: {full_href}]"
                        if (
                            full_href not in seen_urls
                            and len(discovered_pdf_urls) < 4
                            and any(k in combined_hint for k in calendar_pdf_keywords)
                            and not any(ex in combined_hint for ex in excluded_pdf_keywords)
                        ):
                            seen_urls.add(full_href)
                            discovered_pdf_urls.append(full_href)
                    elif full_href != url and any(k in href_low for k in deep_link_keywords) and len(anchor_txt) >= 3:
                        a_tag.string = f"{anchor_txt} [URL: {full_href}]"
                        if (
                            full_href not in seen_urls
                            and len(discovered_subpage_urls) < 2
                            and any(yr_s in combined_hint for yr_s in ("2026", "2027", "2028", "2570", "calendar", "calendario", "championship", "fixtures"))
                        ):
                            seen_urls.add(full_href)
                            discovered_subpage_urls.append(full_href)

                lines = []
                for line in soup.get_text(separator="\n").splitlines():
                    cleaned = re.sub(r"\s+", " ", line).strip()
                    if len(cleaned) >= 4 and "quwfi_" not in cleaned.lower() and not cleaned.lower().startswith("select * from"):
                        lines.append(cleaned)
                text_block = "\n".join(lines)
                if "Les Espaces Structures" in text_block:
                    text_block = text_block.split("Les Espaces Structures")[0].strip()
                text_block = text_block[:per_url_limit]
                if text_block:
                    html_chunks.append(f"=== [RESMİ TAKVİM SAYFASI: {url}] ===\n{text_block}")
            except Exception as e:
                logger.debug(f"[Official-Fetch] {url} okuma uyarısı: {e}")

        # 3. Sayfalarda tespit edilen resmi takvim / handbook PDF bağlantılarının metin içeriğini oku
        discovered_pdf_urls.sort(key=lambda u: (
            0 if any(k in u.lower() for k in ("planning", "handbook", "calendar", "calendario")) else 1,
            0 if any(y in u.lower() for y in ("2026", "2027")) else 1,
        ))
        for pdf_url in discovered_pdf_urls[:3]:
            try:
                resp = self.session.get(pdf_url, timeout=14)
                if resp.status_code == 200 and resp.content:
                    pdf_text = self._extract_pdf_text(resp.content, max_chars=pdf_char_limit)
                    if pdf_text:
                        pdf_chunks.append(f"=== [RESMİ TAKVİM PDF BELGESİ: {pdf_url}] ===\n{pdf_text}")
            except Exception as e:
                logger.debug(f"[Official-PDF-Fetch] {pdf_url} okuma uyarısı: {e}")

        # 4. Keşfedilen yüksek öncelikli alt takvim/haber sayfalarını oku
        for sub_url in discovered_subpage_urls[:2]:
            try:
                resp = self.session.get(sub_url, timeout=10)
                if resp.status_code == 200 and resp.text:
                    cleaned_sub = re.sub(r'data:image/[^"\'\s>]+', "", resp.text)
                    s_soup = BeautifulSoup(cleaned_sub, "html.parser")
                    for tag in s_soup(["script", "style", "noscript", "svg", "footer", "nav", "header", "aside"]):
                        tag.decompose()
                    s_lines = [
                        re.sub(r"\s+", " ", ln).strip()
                        for ln in s_soup.get_text(separator="\n").splitlines()
                        if len(ln.strip()) >= 4
                    ]
                    s_block = "\n".join(s_lines)[:8000]
                    if s_block:
                        html_chunks.append(f"=== [RESMİ ALT DETAY SAYFASI: {sub_url}] ===\n{s_block}")
            except Exception as e:
                logger.debug(f"[Official-Subpage-Fetch] {sub_url} okuma uyarısı: {e}")

        # PDF takvim belgelerini HTML sayfalardan ÖNCE yerleştir ki karakter sınırında asla kesilmesin
        combined = "\n\n".join(search_chunks + pdf_chunks + html_chunks)
        return combined[:max_total_chars]

    @staticmethod
    def _extract_pdf_text(pdf_bytes: bytes, max_chars: int = 9000) -> str:
        """PDF baytlarından pypdf ile temiz metin çıkarır (ek AI çağrısı harcamaz)."""
        try:
            from pypdf import PdfReader
            reader = PdfReader(io.BytesIO(pdf_bytes))
            page_texts: List[str] = []
            total_len = 0
            for page in reader.pages[:12]:
                txt = page.extract_text() or ""
                lines = [re.sub(r"\s+", " ", ln).strip() for ln in txt.splitlines() if len(ln.strip()) >= 3]
                if lines:
                    block = "\n".join(lines)
                    page_texts.append(block)
                    total_len += len(block)
                    if total_len >= max_chars:
                        break
            return "\n".join(page_texts)[:max_chars]
        except Exception as e:
            logger.debug(f"[PDF-Extract] PDF metin çıkarma uyarısı: {e}")
            return ""

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
        Yüksek muhakeme (reasoning) gücüne sahip ana modelleri EN ÖNCE çalıştırır;
        yalnızca ana modeller yoğun (HTTP 503/429) olduğunda hafif (lite) modellere düşer:
        1. gemini-3-flash-preview / gemini-2.5-flash (Yüksek muhakeme gücü + yüksek kota)
        2. gemini-2.5-pro / gemini-3-pro-preview / gemini-3.1-pro-preview (Derin analitik muhakeme)
        3. gemini-3.1-flash-lite-preview / gemini-3.5-flash-lite / gemini-2.5-flash-lite (Hızlı yedek)
        """
        if self._available_models:
            return self._available_models

        allowed_models = [
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash-lite",
            "gemma-3-27b-it",
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
            self._available_models = [m for m in allowed_models if m in discovered_set] or allowed_models
        else:
            self._available_models = allowed_models

        logger.info(f"[Gemini-AI] Aktif AI modelleri (Muhakeme Öncelikli): {self._available_models}")
        return self._available_models

    def _call_gemini_json(
        self,
        api_key: str,
        parts: List[Dict[str, Any]],
        use_search: bool = False,
    ) -> List[Dict[str, Any]]:
        """
        Yüksek muhakeme gücüne sahip modelleri uzman sistem yönergesiyle (systemInstruction) çağırır.
        """
        all_models = self._get_available_gemini_models(api_key)
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json",
        }
        core_models = {
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash-lite",
        }
        domain_system_instruction = (
            "Sen Dünya Masterlar Atletizm (WMA), Avrupa Masterlar Atletizm (EMA), Asya Masterlar Atletizm (AMA) ve "
            "ulusal atletizm federasyonlarının resmi takvimlerini analiz eden kıdemli bir Atletizm Takvim Denetçisisin. "
            "Görevin: Ham web sayfalarını, PDF tablolarını ve haber özetlerini mantıksal olarak analiz edip "
            "YALNIZCA gerçek 35+ yaş Masterlar / Veteranlar (Masters, Veterans, Vétérans, Senioren, Veterani, Veteranos, "
            "SAUL, Szenior, กรีฑาสูงอายุ, マスターズ) atletizm şampiyonalarını süzmek ve doğrulamaktır.\n"
            "MANTIKSAL SÜZME KURALLARI:\n"
            "1. Yarışmanın niteliğini anla: Pist (Outdoor/Stadia), Salon (Indoor/Short Track/Pista Cubierta/Salle/Halle), "
            "Kros (Cross Country), Yol Koşusu (5K, 10K, Yarı Maraton, Maraton, Yol Bayrak), Yürüyüş (Race Walking), "
            "Dağ/Patika (Mountain/Trail/Off-Road), Atmalar Pentatlonu / Kış Atmaları (Throws Pentathlon / Winter Throws / Lanci), "
            "Çoklu Branşlar (Combined Events / Prove Multiple / Épreuves Combinées / Mehrkampf) ve Çoklu Spor Master Oyunları "
            "(World Masters Games, Open Masters Games, European/Asia-Pacific/Pan-American Masters Games) dışındaki "
            "atletizm dışı branşları, seminerleri, kongreleri ve antrenman kamplarını KESİNLİKLE ELE.\n"
            "2. Yaş grubunu mantıksal olarak süz: Elit/Büyükler (Senior/Absolute/Elite), U23, U20, U18, U16, Yıldızlar, Gençler, "
            "Okul sporları ve Paralimpik (Para-Athletics) yarışmalarını KESİNLİKLE ELE.\n"
            "3. Yerel takvim yıllarını otomatik olarak Miladi (Gregorian) takvime çevir: "
            "Tayland Budist Takvimi (พ.ศ. / B.E. -> yıldan 543 çıkar: 2569=2026, 2570=2027), "
            "Tayvan Minguo Takvimi (民國 -> yıla 1911 ekle: 115=2026, 116=2027), "
            "Japonya Reiwa Takvimi (令和 -> R8=2026, R9=2027).\n"
            "4. Aynı yarışma metinde birden fazla kez veya farklı dillerde geçiyorsa tekilleştir ve en doğru resmi tarihi/şehri/ülkeyi yaz."
        )

        for attempt in range(2):
            active_models = [m for m in all_models if m not in self._exhausted_models]
            if not active_models:
                active_models = list(all_models)
            active_models.sort(key=lambda m: (1 if self._model_503_counts.get(m, 0) >= 2 else 0, all_models.index(m)))

            for model in active_models:
                if model.startswith("gemma-") and any("inline_data" in p for p in parts):
                    continue

                endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
                max_tokens = 8192 if model.startswith("gemma-") else 24576
                gen_config: Dict[str, Any] = {"temperature": 0.0, "maxOutputTokens": max_tokens}
                if not use_search and not model.startswith("gemma-"):
                    gen_config["responseMimeType"] = "application/json"
                payload: Dict[str, Any] = {
                    "contents": [{"parts": parts}],
                    "generationConfig": gen_config,
                }
                if not model.startswith("gemma-"):
                    payload["systemInstruction"] = {"parts": [{"text": domain_system_instruction}]}
                if use_search and not model.startswith("gemma-"):
                    payload["tools"] = [{"google_search": {}}]

                try:
                    req_timeout = 68 if model == "gemini-3-flash-preview" else 45
                    resp = requests.post(endpoint, headers=headers, json=payload, timeout=req_timeout)

                    # Free Tier google_search aracı 429 veya 400 verirse anında aynı modeli araçsız çağır
                    if resp.status_code in (400, 429) and "tools" in payload:
                        payload.pop("tools", None)
                        resp = requests.post(endpoint, headers=headers, json=payload, timeout=req_timeout)

                    if resp.status_code == 402:
                        logger.error(
                            "[Gemini-AI] HTTP 402: API anahtarının bağlı olduğu proje 'Prepay (No credits)' modunda. "
                            "Lütfen 'Free tier' projesine ait bir API anahtarı kullanın."
                        )
                        self._exhausted_models.update(all_models)
                        break

                    if resp.status_code in (429, 503):
                        body_lower = resp.text.lower()
                        is_daily_limit = resp.status_code == 429 and (
                            "perday" in body_lower or "per_day" in body_lower or "daily" in body_lower
                        )
                        if model in core_models and not is_daily_limit:
                            self._model_503_counts[model] = self._model_503_counts.get(model, 0) + 1
                            logger.warning(f"[Gemini-AI] Model ({model}) geçici HTTP {resp.status_code}, sıradaki modele geçiliyor...")
                            time.sleep(1)
                            continue
                        else:
                            self._exhausted_models.add(model)
                            logger.warning(f"[Gemini-AI] Model ({model}) HTTP {resp.status_code}, devre dışı bırakıldı.")
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
                    self._model_503_counts[model] = self._model_503_counts.get(model, 0) + 1
                    if model not in core_models:
                        self._exhausted_models.add(model)
                    logger.warning(f"[Gemini-AI] Model ({model}) 50 sn içinde yanıt vermedi, sıradaki model deneniyor...")
                except Exception as e:
                    logger.warning(f"[Gemini-AI] Model ({model}) çağrı uyarısı: {e}")

            if attempt == 0:
                time.sleep(4)

        # Harici Çoklu-Sağlayıcı Yedek Motoru (Groq LPU / OpenRouter Free) — tanımlıysa devreye girer
        return self._call_external_llm_fallback(parts)

    def _call_external_llm_fallback(self, parts: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Tüm Google modelleri yanıt veremezse ve ortam değişkenlerinde GROQ_API_KEY veya
        OPENROUTER_API_KEY tanımlıysa OpenAI uyumlu uç noktalar üzerinden JSON çıkarımı yapar.
        """
        if any("inline_data" in p for p in parts):
            return []
        prompt_text = "\n".join(str(p.get("text", "")) for p in parts if "text" in p).strip()
        if not prompt_text:
            return []

        providers = []
        gh_token = (ScraperConfig.GITHUB_TOKEN or "").strip()
        if gh_token:
            providers.append((
                "GitHub-Models-GPT4o",
                "https://models.inference.ai.azure.com/chat/completions",
                gh_token,
                "gpt-4o",
                36000,
            ))
            providers.append((
                "GitHub-Models-Llama70B",
                "https://models.inference.ai.azure.com/chat/completions",
                gh_token,
                "Meta-Llama-3.3-70B-Instruct",
                32000,
            ))
        groq_key = (ScraperConfig.GROQ_API_KEY or "").strip()
        if groq_key:
            providers.append((
                "Groq",
                "https://api.groq.com/openai/v1/chat/completions",
                groq_key,
                "llama-3.3-70b-versatile",
                24000,
            ))
        openrouter_key = (ScraperConfig.OPENROUTER_API_KEY or "").strip()
        if openrouter_key:
            providers.append((
                "OpenRouter",
                "https://openrouter.ai/api/v1/chat/completions",
                openrouter_key,
                "meta-llama/llama-3.3-70b-instruct:free",
                36000,
            ))

        for prov_name, url, key, model_name, max_chars in providers:
            try:
                trimmed_prompt = prompt_text[:max_chars] if len(prompt_text) > max_chars else prompt_text
                headers = {
                    "Authorization": f"Bearer {key}",
                    "Content-Type": "application/json",
                }
                payload = {
                    "model": model_name,
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are an official athletics calendar extraction engine. Output ONLY a valid JSON array of competition objects.",
                        },
                        {"role": "user", "content": trimmed_prompt},
                    ],
                    "temperature": 0.0,
                    "max_tokens": 8192,
                }
                resp = requests.post(url, headers=headers, json=payload, timeout=45)
                if resp.status_code == 200:
                    data = resp.json()
                    choices = data.get("choices", [])
                    if choices:
                        content = choices[0].get("message", {}).get("content", "")
                        parsed = self._extract_json_from_text(content)
                        if parsed:
                            logger.info(f"[{prov_name}-Fallback] Yedek AI ({model_name}) ile {len(parsed)} yarışma çıkarıldı.")
                            return parsed
                else:
                    logger.warning(f"[{prov_name}-Fallback] HTTP {resp.status_code}: {resp.text[:140]}")
            except Exception as e:
                logger.warning(f"[{prov_name}-Fallback] Çağrı hatası: {e}")

        return []

    def _convert_gemini_items(
        self,
        items: List[Dict[str, Any]],
        default_url: str,
        default_trust_level: int = 2,
        default_regulations_url: Optional[str] = None,
        live_digest: str = "",
    ) -> List[Competition]:
        """Gemini JSON çıktılarını doğrular, kanıt kapılarından geçirir ve Competition nesnelerine dönüştürür."""
        converted: List[Competition] = []
        valid_scopes = {"national", "world", "balkan", "europe", "ema_league", "regional"}
        min_year = self.years[0]
        digest_lower = (live_digest or "").lower()

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
                yr = int(start_date[:4])
                if yr < min_year:
                    continue
                if not re.match(r"^20\d{2}-\d{2}-\d{2}$", end_date) or end_date < start_date:
                    end_date = start_date

                raw_country = str(item.get("country", "")).strip().upper()
                raw_cname = str(item.get("country_name", "")).strip()
                raw_city = str(item.get("city", "")).strip()

                invalid_tokens = {
                    "", "n/a", "na", "none", "null", "tbd", "tbc", "tba", "ybd", "unknown",
                    "belirlenecek", "belirsiz", "to be determined", "to be announced", "to be confirmed",
                    "not specified", "not-specified", "unspecified", "not announced",
                    "various", "multiple", "a determiner", "à déterminer", "por determinar", "da definire", "-"
                }
                if raw_city.lower() in invalid_tokens:
                    raw_city = ""
                if raw_country.lower() in invalid_tokens:
                    raw_country = ""

                if start_date.endswith("-01-01") and not raw_city:
                    continue

                title_low = title.lower()
                city_low = raw_city.lower()

                # USATF 2026 Outdoor (Geneva, OH -> 23-26 Temmuz 2026) tarih standartlaştırması
                if "usatf" in title_low and "outdoor" in title_low and start_date.startswith("2026-07"):
                    start_date = "2026-07-23"
                    end_date = "2026-07-26"

                # Ay boyu yer tutucu tarih engeli (örn. 2026-11-01..2026-11-30 veya 2028-08-01..2028-08-31 gibi >= 24 günlük belirsiz tarihler)
                try:
                    span_days = (date.fromisoformat(end_date) - date.fromisoformat(start_date)).days
                    if span_days >= 24:
                        logger.warning(
                            f"🚫 [AY BOYU YER TUTUCU TARİH ENGELİ] {title} ({start_date}..{end_date}, {span_days} gün) "
                            "kesin günleri açıklanmadığı için reddedildi."
                        )
                        continue
                except Exception:
                    pass

                # WMA Salon Şampiyonası (Gainesville / 2027-03 veya başlıkta Indoor geçenler) standartlaştırması:
                # Bazı kaynaklar 2027 Gainesville Salon Şampiyonasını "Indoor" kelimesini atlayarak "World Masters Athletics Championships 2027"
                # şeklinde yazdığı için yanlışlıkla Açık Saha (world-2027-acik-saha) kopyası oluşmasını önler.
                if ("world masters athletics" in title_low or "wma " in title_low or "wmaci" in title_low) and (
                    "indoor" in title_low
                    or "wmaci" in title_low
                    or "gainesville" in city_low
                    or "gainesville" in title_low
                    or start_date.startswith("2027-03")
                ):
                    title = f"World Masters Athletics Championships Indoor {start_date[:4]}"
                    title_low = title.lower()
                    if start_date.startswith("2027"):
                        raw_city = "Gainesville"
                        raw_country = "US"
                        raw_cname = "ABD"
                        city_low = "gainesville"
                elif ("european masters athletics" in title_low or "emaci" in title_low) and ("indoor" in title_low or "emaci" in title_low):
                    title = f"European Masters Athletics Championships Indoor {start_date[:4]}"
                    title_low = title.lower()
                elif (
                    ("campionati italiani" in title_low or "campionato italiano" in title_low or "italya masterlar" in title_low)
                    and (
                        ("prove multiple" in title_low and "indoor" in title_low)
                        or (start_date == "2027-02-06" and ("invernali di lanci" in title_low or "çoklu branşlar" in title_low))
                    )
                ):
                    title = "İtalya Masterlar Salon Çoklu Branşlar ve Kış Atmaları Şampiyonası"
                    title_low = title.lower()
                    raw_country = "IT"
                    raw_cname = "İtalya"
                    if start_date.startswith("2027"):
                        raw_city = "İtalya"
                        city_low = "italya"

                # FIDAL 2027 Planning PDF'de Como (WMG) ve Ancona (Indoor) dışındaki 2027 İtalya ulusal şampiyonalarının şehri henüz boş (İtalya)
                if (
                    start_date.startswith("2027")
                    and (raw_country == "IT" or "campionati italiani" in title_low or "campionato italiano" in title_low)
                    and "como" not in title_low
                    and city_low not in ("como", "ancona")
                ):
                    raw_city = "İtalya"
                    raw_country = "IT"
                    raw_cname = "İtalya"
                    city_low = "italya"

                # Başlığında "kansai" geçmeyen bir Masters Games için yanlışlıkla Kansai/JP atanmışsa temizle, gerçek ülkeyi başlıktan bul
                if ("masters games" in title_low or "wmg" in title_low) and "kansai" not in title_low and ("kansai" in city_low or raw_country in ("JP", "JPN")):
                    raw_city = ""
                    raw_country = ""
                    raw_cname = ""
                    city_low = ""
                if ("world" in title_low or "wma" in title_low or "wmra" in title_low) and ("mountain" in title_low or "trail" in title_low) and start_date.startswith("2027"):
                    raw_city = "Klosters"
                    raw_country = "CH"
                    raw_cname = "İsviçre"
                elif ("european" in title_low or "emmtrc" in title_low or "emorrc" in title_low) and ("mountain" in title_low or "trail" in title_low or "off-road" in title_low) and start_date.startswith("2027"):
                    raw_city = "Ojós, Murcia"
                    raw_country = "ES"
                    raw_cname = "İspanya"
                elif "france" in title_low and ("piste" in title_low or "estival" in title_low) and start_date.startswith("2026"):
                    start_date = "2026-06-19"
                    end_date = "2026-06-21"
                    raw_city = "Épinal"
                    raw_country = "FR"
                    raw_cname = "Fransa"
                elif any(k in city_low for k in ["luzern", "lucerne", "klosters", "zurich", "zürich"]):
                    raw_country = "CH"
                    raw_cname = "İsviçre"
                elif any(k in city_low for k in ["ojós", "ojos", "murcia"]):
                    raw_country = "ES"
                    raw_cname = "İspanya"

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
                raw_web = str(item.get("website_url", "") or "").strip()
                web_url = raw_web if raw_web.startswith("http") else default_url

                raw_reg = str(item.get("regulations_url", "") or "").strip()
                reg_url: Optional[str] = None
                if raw_reg.startswith("http") and raw_reg.lower() not in invalid_tokens:
                    reg_url = raw_reg
                elif web_url.lower().endswith(".pdf"):
                    reg_url = web_url
                elif default_regulations_url:
                    reg_url = default_regulations_url

                # Doğrudan OpenTrack, LADV, World Masters Sport, TVAA, SLMA, resmi PDF veya resmi CNAM alt sayfalarından gelen kayıtların güven puanı = 3
                trust_level = default_trust_level
                if reg_url or any(d in web_url.lower() for d in ("opentrack.run", "ladv.de/ausschreibung", "masters.athle.fr/asp.net/espaces.html", "worldmasterssport.com", "thaivaa.com", "sgmasterstnf.com", "sriathletics.lk")):
                    trust_level = 3

                comp_id = generate_competition_id(title, start_date, country, city)
                existing_doc = self.existing_comps.get(comp_id)
                is_verified = False

                # KAPI 1: İki Yılda Bir (Biennial) Yapılan Dünya/Avrupa Şampiyonalarında İmkansız Yıl Engeli (Tüm Yıllar İçin)
                # EMACI (Avrupa Salon) sadece çift yıllarda (2026, 2028), EMACS (Avrupa Açık Saha) sadece tek yıllarda (2025, 2027)
                # WMACI (Dünya Salon) sadece tek yıllarda (2025, 2027), WMACS (Dünya Açık Saha) sadece çift yıllarda (2026, 2028)
                if (
                    (comp_id == f"europe-{yr}-salon" and yr % 2 == 1)
                    or (comp_id == f"europe-{yr}-acik-saha" and yr % 2 == 0)
                    or (comp_id == f"world-{yr}-salon" and yr % 2 == 0)
                    or (comp_id == f"world-{yr}-acik-saha" and yr % 2 == 1)
                ):
                    logger.warning(f"🚫 [BİENAL KURAL ENGELİ] {title} ({start_date}) - {comp_id} bu yılda düzenlenmiyor, reddedildi.")
                    continue

                # KAPI 0: Resmi olarak doğrulanmış (verified=True) mevcut veritabanı kaydı varsa tarih ve şehrini koru
                if existing_doc and existing_doc.get("verified") is True:
                    start_date = str(existing_doc.get("start_date") or start_date)[:10]
                    end_date = str(existing_doc.get("end_date") or start_date)[:10]
                    if existing_doc.get("city"):
                        city = str(existing_doc.get("city"))
                    if existing_doc.get("country"):
                        country = str(existing_doc.get("country"))
                    if existing_doc.get("country_name"):
                        country_name = str(existing_doc.get("country_name"))
                    trust_level = 3
                    is_verified = True
                elif yr > min_year:
                    # KAPI 2: 52 Hafta (363-366 Gün) AI Tarih Kaydırma / Tahmin Engeli
                    prev_id = comp_id.replace(f"-{yr}-", f"-{yr - 1}-")
                    prev_doc = self.existing_comps.get(prev_id)
                    if prev_doc and prev_doc.get("start_date"):
                        try:
                            d_curr = date.fromisoformat(start_date)
                            d_prev = date.fromisoformat(str(prev_doc.get("start_date"))[:10])
                            day_diff = (d_curr - d_prev).days
                            if day_diff in (363, 364, 365, 366) and start_date not in digest_lower and str(yr) not in web_url.lower():
                                logger.warning(
                                    f"🚫 [52-HAFTA TAHMİN ENGELİ] {title} ({start_date}) önceki yıl ({prev_doc.get('start_date')}) "
                                    f"+ {day_diff} gün kaydırılarak tahmin edilmiş, reddedildi."
                                )
                                continue
                        except Exception:
                            pass

                    # KAPI 3: Canlı Metin (live_digest) Kanıt Kontrolü
                    # Gelecek yıl (2027+) için üretilen bir kaydın yılı ve gün/şehir bilgisinin indirilen resmi metinde geçmesi şarttır
                    if digest_lower:
                        yr_str = str(yr)
                        buddhist_yr_str = str(yr + 543)  # Tayland Budist Takvimi (örn. 2027 -> 2570)
                        day_num = str(int(start_date[8:10]))
                        city_tok = city.lower().split(",")[0].split("/")[0].strip()[:4]
                        is_country_city = (
                            not raw_city
                            or city.lower() == country_name.lower()
                            or city.lower() in {
                                "italya", "italia", "italy", "ispanya", "espana", "españa", "spain",
                                "portekiz", "portugal", "fransa", "france", "almanya", "germany", "deutschland"
                            }
                        )
                        has_year = (yr_str in digest_lower) or (country == "TH" and buddhist_yr_str in digest_lower)
                        has_iso_or_url = (
                            (start_date in digest_lower)
                            or (yr_str in web_url.lower())
                            or (
                                country == "TH"
                                and any(k in digest_lower for k in ("ครั้งที่ 31", "31st thailand", "เพชรบูรณ์", "phetchabun", "thaivaa.com"))
                            )
                        )
                        has_day_and_city = (
                            re.search(rf"\b0?{day_num}\b", digest_lower) is not None
                            and (is_country_city or not city_tok or city_tok in digest_lower)
                        )
                        if not (has_year and (has_iso_or_url or has_day_and_city)):
                            logger.warning(
                                f"🚫 [KANITSIZ GELECEK YIL ENGELİ] {title} ({start_date}, {city}) "
                                "canlı federasyon sayfası metninde doğrulanamadı, reddedildi."
                            )
                            continue

                today_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d")
                comp_status = "completed" if end_date < today_iso else "scheduled"

                comp = Competition(
                    id=comp_id,
                    title=title,
                    country=country,
                    country_name=country_name,
                    city=city,
                    start_date=start_date,
                    end_date=end_date,
                    scope=scope,
                    federation=federation,
                    website_url=web_url,
                    regulations_url=reg_url,
                    trust_level=trust_level,
                    verified=is_verified,
                    status=comp_status,
                    created_at=datetime.now(timezone.utc).isoformat(),
                )
                comp.content_hash = compute_content_hash(comp)
                converted.append(comp)
            except Exception as item_err:
                logger.debug(f"[Gemini-AI] Öğe dönüştürme uyarısı: {item_err}")
        return converted

    # =========================================================================
    # 3. OTONOM YAPAY ZEKA MANTIKSAL SÜZGEÇ VE DENETİM KATMANI (AI AUDITOR)
    # =========================================================================
    def _ai_autonomous_audit_and_filter(self, competitions: List[Competition]) -> List[Competition]:
        """
        Tüm gruplardan toplanan aday yarışmaları tek bir bütüncül tabloda inceleyerek:
        1. Gerçek 35+ yaş Masterlar/Veteranlar atletizm yarışması olmayanları (Elit/Büyükler, U23/U20/U18,
           kongre/seminer, atletizm dışı branşlar) mantıksal muhakeme ile eler (`action='drop'`).
        2. Aynı yarışmanın farklı dilde veya küçük başlık farkıyla aynı ülke/tarihte oluşmuş anlamsal
           kopyalarını tespit edip fazlalık kopyayı eler (`action='drop'`).
        3. Şehir, ülke kodu (`country`), ülke adı (`country_name`) veya kapsam (`scope`: world, europe,
           balkan, ema_league, national, regional) uyuşmazlıklarını otomatik düzeltir (`action='fix'`).
        """
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not competitions or not api_key:
            return competitions

        try:
            compact_rows = []
            for idx, c in enumerate(competitions):
                compact_rows.append(
                    f"[{idx}] id={c.id} | title={c.title} | dates={c.start_date}..{c.end_date} | "
                    f"city={c.city} | country={c.country} ({c.country_name}) | scope={c.scope} | "
                    f"fed={c.federation} | verified={c.verified}"
                )

            audit_prompt = (
                "Aşağıda dünya genelinden toplanmış Masterlar (35+ yaş) atletizm yarışması aday listesi yer almaktadır.\n"
                "Uzman bir Atletizm Takvim Denetçisi olarak bu listeyi mantıksal süzgeçten geçir ve YALNIZCA elenmesi (`drop`) "
                "veya düzeltilmesi (`fix`) gereken kayıtları JSON dizisi olarak döndür (kusursuz olan kayıtları JSON'a yazma!).\n\n"
                "DENETİM VE SÜZME KURALLARI:\n"
                "1. ELEME (`action: 'drop'`):\n"
                "   - Yarışma gerçek bir 35+ yaş Masterlar / Veteranlar atletizm yarışması (veya World/European/Asia-Pacific/Open/Pan-American Masters Games) DEĞİLSE "
                "(örneğin sadece Elit/Büyükler, U23, U20, U18, Gençler, Yıldızlar, Okullar, Paralimpik yarışmasıysa veya kongre/seminer/toplantı ise) `drop` et.\n"
                "   - Listede AYNI ülkede, AYNI (veya kesişen) tarihlerde, AYNI şampiyonanın iki farklı dilde ya da benzer başlıkla tekrar eden kopyası varsa, "
                "daha eksik/belirsiz olan kopyayı `drop` et (ancak aynı hafta sonunda yapılan FARKLI branş şampiyonalarını — örn. Salon Çoklu Branşlar ile Kış Atmaları, veya farklı ülkelerdeki şampiyonaları — ASLA silme!).\n"
                "   - `verified=True` olan resmi kilitli kayıtları ASLA `drop` etme.\n"
                "2. DÜZELTME (`action: 'fix'`):\n"
                "   - Bir yarışmanın `city`, `country` (ISO-2), `country_name` (Türkçe) veya `scope` (`world`, `europe`, `balkan`, `ema_league`, `national`, `regional`) "
                "alanı yarışma başlığıyla veya ev sahibi şehirle çelişiyorsa (örneğin ulusal bir şampiyonaya `regional` yazılmışsa ya da şehir başka bir ülkeye aitse) `fix` ile düzelt.\n\n"
                "ÇIKTI FORMATI (Sadece geçerli bir JSON dizisi döndür; hiçbir sorun yoksa `[]` döndür):\n"
                '[{"idx": 12, "action": "drop", "reason": "Kısa Türkçe gerekçe"}, '
                '{"idx": 25, "action": "fix", "city": "Doğru Şehir", "country": "ISO2", "country_name": "Türkçe Ülke", "scope": "national|world|europe|balkan|ema_league|regional", "reason": "Kısa Türkçe gerekçe"}]\n\n'
                "ADAY YARIŞMA LİSTESİ:\n" + "\n".join(compact_rows)
            )

            decisions = self._call_gemini_json(api_key, [{"text": audit_prompt}], use_search=False)
            if not decisions or not isinstance(decisions, list):
                logger.info("🧠 [AI-MANTIKSAL-SÜZGEÇ] Tüm yarışmalar denetimden geçti (0 eleme/düzeltme).")
                return competitions

            drop_indices = set()
            fix_map: Dict[int, Dict[str, Any]] = {}
            for dec in decisions:
                if not isinstance(dec, dict):
                    continue
                raw_idx = dec.get("idx")
                if not isinstance(raw_idx, int) or raw_idx < 0 or raw_idx >= len(competitions):
                    continue
                action = str(dec.get("action", "")).strip().lower()
                if action == "drop":
                    if not competitions[raw_idx].verified:
                        drop_indices.add(raw_idx)
                        self.dropped_ids.add(competitions[raw_idx].id)
                        logger.info(
                            f"🧠 [AI-MANTIKSAL-SÜZGEÇ] Elendi: {competitions[raw_idx].title} "
                            f"({competitions[raw_idx].start_date}, {competitions[raw_idx].country}) — Neden: {dec.get('reason', 'Uygunsuz/Kopya')}"
                        )
                elif action == "fix":
                    fix_map[raw_idx] = dec

            # Güvenlik kilidi: AI yanlışlıkla listenin %25'inden fazlasını silmeye çalışırsa elemeyi iptal et
            if len(drop_indices) > max(8, len(competitions) // 4):
                logger.warning("🧠 [AI-MANTIKSAL-SÜZGEÇ] Aşırı eleme tespit edildi, güvenlik kilidi devreye girdi (elemeler iptal edildi).")
                drop_indices.clear()

            valid_scopes = {"national", "world", "balkan", "europe", "ema_league", "regional"}
            filtered_list: List[Competition] = []
            for idx, comp in enumerate(competitions):
                if idx in drop_indices:
                    continue
                if idx in fix_map and not comp.verified:
                    fx = fix_map[idx]
                    changed = False
                    new_city = str(fx.get("city") or "").strip()
                    new_country = str(fx.get("country") or "").strip().upper()
                    new_cname = str(fx.get("country_name") or "").strip()
                    new_scope = str(fx.get("scope") or "").strip().lower()
                    if new_city and new_city != comp.city:
                        comp.city = new_city
                        changed = True
                    if len(new_country) == 2 and new_country != comp.country:
                        comp.country = new_country
                        changed = True
                    if new_cname and new_cname != comp.country_name:
                        comp.country_name = new_cname
                        changed = True
                    if new_scope in valid_scopes and new_scope != comp.scope:
                        comp.scope = new_scope
                        changed = True
                    if changed:
                        comp.id = generate_competition_id(comp.title, comp.start_date, comp.country, comp.city)
                        comp.content_hash = compute_content_hash(comp)
                        logger.info(
                            f"🧠 [AI-MANTIKSAL-DÜZELTME] {comp.title} -> {comp.city} ({comp.country} - {comp.country_name}), "
                            f"scope={comp.scope} — Neden: {fx.get('reason', 'Alan düzeltmesi')}"
                        )
                filtered_list.append(comp)

            logger.info(
                f"🧠 [AI-MANTIKSAL-SÜZGEÇ] Denetim tamamlandı: {len(drop_indices)} hatalı/kopya yarışma elendi, "
                f"{len(fix_map)} yarışma düzeltildi, {len(filtered_list)} temiz yarışma kaldı."
            )
            return filtered_list
        except Exception as e:
            logger.warning(f"🧠 [AI-MANTIKSAL-SÜZGEÇ] Denetim uyarısı (mevcut liste korunuyor): {e}")
            return competitions
