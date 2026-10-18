"""
Master Racer - Autonomous AI Discovery Engine (100% Dynamic Gemini AI + Search Grounding + Multimodal PDF)
Hiçbir sabit şehir, sabit yıl veya sabit kişi/memorial ismine bağımlı kalmadan;
yalnızca Google Gemini API (Google Search Grounding + Multimodal Resmi PDF Analizi)
kullanarak Türkiye, Balkanlar, Avrupa ve Dünya genelindeki tüm 35+ Masterlar atletizm
yarışmalarını 4 yıllık dinamik takvim penceresiyle (ör. 2026, 2027, 2028, 2029) keşfeden otonom yapay zeka motoru.
"""

import re
import time
import json
import base64
import logging
from datetime import datetime
from typing import List, Dict, Any
from bs4 import BeautifulSoup
import requests

from .base_parser import BaseParser
from ..config import Competition, ScraperConfig
from ..deduplication import (
    generate_competition_id,
    compute_content_hash,
    is_valid_competition_title,
)

logger = logging.getLogger(__name__)


class AutonomousDiscoveryParser(BaseParser):
    """
    Yalnızca Google Gemini AI (Multimodal PDF + Google Search Grounding) katmanını kullanarak
    içinde bulunulan yıl ve takip eden 3 yıldaki (toplam 4 yıllık pencere: ör. 2026, 2027, 2028, 2029)
    tüm 35+ Masterlar (Masters / Veteran / Senioren) atletizm yarışmalarını otonom tarayan motor.
    """

    def __init__(self):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://european-masters-athletics.org")
        current_year = datetime.utcnow().year
        # İçinde bulunulan yıl + gelecek 3 yıl (Örn: 2026, 2027, 2028, 2029)
        self.years = [current_year, current_year + 1, current_year + 2, current_year + 3]

    def parse(self) -> List[Competition]:
        logger.info(f"Otonom Yapay Zeka (Gemini AI) Tarayıcısı başlatılıyor (Yıllar: {self.years})...")
        discovered = self._discover_with_gemini_ai()
        logger.info(
            f"[AutonomousDiscovery_Parser] Yapay zeka taraması tamamlandı. "
            f"Toplam {len(discovered)} ham Master yarışması keşfedildi."
        )
        return discovered

    def _discover_with_gemini_ai(self) -> List[Competition]:
        """
        Google Gemini API (Search Grounding + Multimodal PDF okuma) kullanarak:
        1. TAF'ın faaliyet takvimi sayfasındaki resmi PDF dosyalarını doğrudan Gemini'ye okutur;
           sadece 'Yaş Grubu' sütununda 'Master / Masterlar' yazan satırları alır.
        2. Dünya (WMA/WMG), Avrupa (EMA Şampiyonaları ve EMA League) ve Balkan (BAMA)
           ana şampiyona takvimlerini 4 yıllık pencere (2026, 2027, 2028, 2029) için tarar.
        3. Özellikle 2027, 2028 ve 2029 ileri tarihli açıklanmış şampiyonalar için odaklanmış
           bir gelecek yıllar sorgusu çalıştırır (2026 yoğunluğu içinde kaybolmamaları için).
        4. Tüm Avrupa ülkelerinin Ulusal Salon, Açık Saha, Atmalar, Çoklu Branşlar, Kros, Bayrak (Relays),
           Yürüyüş ve Yol Masterlar Şampiyonalarını 2026, 2027, 2028 ve 2029 yılları için canlı keşfeder.
        """
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not api_key:
            logger.warning("[Gemini-AI] GEMINI_API_KEY tanımlı değil! Tarama yapılamadı.")
            return []

        results: List[Competition] = []
        y1, y2, y3, y4 = self.years[0], self.years[1], self.years[2], self.years[3]
        years_all_en = f"{y1}, {y2}, {y3}, and {y4}"
        years_all_tr = f"{y1}, {y2}, {y3} ve {y4}"
        future_years_en = f"{y2}, {y3}, and {y4}"
        logger.info(f"[Gemini-AI] Yapay zeka destekli derinlemesine keşif motoru başlatılıyor ({years_all_tr})...")

        # -------------------------------------------------------------------------
        # ADIM 1: TAF Resmi Faaliyet Takvimi PDF Analizi (Multimodal - Yaş Grubu Sütunu Bazlı)
        # -------------------------------------------------------------------------
        taf_pdf_urls = self._find_taf_calendar_pdf_urls()
        for pdf_url in taf_pdf_urls:
            try:
                resp = self.session.get(pdf_url, timeout=25)
                if resp.status_code == 200 and len(resp.content) > 1000:
                    pdf_b64 = base64.b64encode(resp.content).decode("utf-8")
                    prompt = (
                        "Bu Türkiye Atletizm Federasyonu (TAF) resmi faaliyet takvimi PDF belgesini satır satır incele.\n"
                        "KURAL 1: Tablodaki 'Yaş Grubu' sütununu kontrol et. SADECE Yaş Grubu sütununda 'Master' veya 'Masterlar' "
                        "yazan yarışmaları çıkar. Yarışmaya o yıl kimin adı verilmiş olursa olsun veya hangi şehirde yapılırsa yapılsın, "
                        "kriter yalnızca Yaş Grubu'nun 35+ Master/Masterlar olmasıdır!\n"
                        "KURAL 2: Yaş Grubu 'B' (Büyükler), 'U23', 'U20', 'U18', 'U16', 'U14' olan yarışmaları "
                        "(örneğin Büyükler Balkan Şampiyonası, Salon Olimpik Deneme, Olimpik Deneme, TOHM, SEM, "
                        "Okullar, Kulüpler Ligi vb.) KESİNLİKLE DAHİL ETME!\n"
                        "KURAL 3: Eğer Yaş Grubu sütununda 'Master' yazıyor ancak yarışma adında 'Masterlar' kelimesi geçmiyorsa "
                        "(örneğin bir anma/yol koşusu veya kupa ise), `title` alanının sonuna mutlaka '(Masterlar)' ibaresini ekle.\n"
                        "Sadece geçerli bir JSON dizisi (array) döndür, başka hiçbir açıklama yazma:\n"
                        '[{"title": "Takvimdeki Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
                        '"city": "Takvimdeki Şehir", "country": "ISO-2 Ülke Kodu (TR, SI, RO, PL vb.)", '
                        '"country_name": "Türkçe Ülke Adı", "venue_type": "indoor|outdoor|road|cross", '
                        '"scope": "national|balkan|europe|world|regional", '
                        '"federation": "TAF", "website_url": "https://taf.org.tr"}]'
                    )
                    parts = [
                        {"inline_data": {"mime_type": "application/pdf", "data": pdf_b64}},
                        {"text": prompt},
                    ]
                    items = self._call_gemini_json(api_key, parts, use_search=False)
                    converted = self._convert_gemini_items(items, default_url=pdf_url)
                    logger.info(f"[Gemini-AI] TAF Resmi PDF takviminden {len(converted)} Master yarışması çıkarıldı.")
                    results.extend(converted)
                    time.sleep(2)
            except Exception as e:
                logger.warning(f"[Gemini-AI] TAF PDF analizi hatası ({pdf_url}): {e}")

        # -------------------------------------------------------------------------
        # ADIM 2: 4 Yıllık (2026, 2027, 2028, 2029) Dinamik Google Search Grounding Sorguları
        # -------------------------------------------------------------------------
        schema_instruction = (
            "\n\nÇIKTI KURALLARI:\n"
            f"1. {years_all_tr} yılları için SADECE 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren / Veterani / Veteranos) "
            "kategorisindeki yarışmaları dahil et. Elit/Büyükler (Senior Elite), Olimpik Deneme, U23, U20, U18 yarışmalarını ASLA dahil etme!\n"
            "2. Yarışmanın ilgili yıldaki resmi adı ve yapılacağı şehir/stadyum ne ise takvimden doğrudan onu al. "
            "Eğer yarışma başlığında 'Masters / Masterlar / Veteran / Senioren / BMAF / EMA / WMA / BAMACI / BAMACS' ibaresi yoksa "
            "ama yarışma resmi bir Masterlar yarışması ise başlığa '(Masters)' ekle.\n"
            "3. Çok günlü yarışmalarda `start_date` (başlangıç) ve `end_date` (bitiş) tarihlerini tam aralık olarak (YYYY-MM-DD) yaz. "
            "Kesin tarihi belli olmayan (TBD/TBC) yarışmaları dahil etme.\n"
            "4. `city` alanına yarışmanın düzenleneceği gerçek şehir adını (varsa parantez içinde tesis adını) yaz. "
            "Asla 'N/A', 'TBD', 'Unknown' yazma.\n"
            "5. `scope` alanı yalnızca şu 6 değerden biri olmalıdır: 'national', 'world', 'balkan', 'europe', 'ema_league', 'regional'.\n"
            "6. Sadece geçerli bir JSON dizisi (array) döndür, markdown veya açıklama metni ekleme:\n"
            '[{"title": "Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "Şehir", "country": "ISO-2 Ülke Kodu (TR, DE, FR, GB, IT, ES, PL, RO vb.)", '
            '"country_name": "Türkçe Ülke Adı", "venue_type": "indoor|outdoor|road|cross", '
            '"scope": "national|world|balkan|europe|ema_league|regional", '
            '"federation": "Federasyon Kısaltması", "website_url": "Resmi Web Sitesi URL"}]'
        )

        discovery_queries = [
            (
                f"Dünya (WMA/WMG), Avrupa (EMA & EMA League) ve Balkan (BAMA) Şampiyonaları ({years_all_tr})",
                f"Search the official calendars of World Masters Athletics (world-masters-athletics.org), "
                f"European Masters Athletics (european-masters-athletics.org / data.opentrack.run), and Balkan Masters Athletics "
                f"for ALL {years_all_en} international Masters (35+) athletics championships, cups, and league meetings, "
                f"with their official host cities and exact dates:\n"
                f"- European Masters Indoor Championships (EMACI), Stadia/Outdoor Championships (EMACS), and Non-Stadia Championships (EMACNS)\n"
                f"- European Masters Marathon, Off-Road, Mountain & Trail Running Championships (EMORRC, EMMTRC, EMAMRC) and Winter Throws\n"
                f"- All European Masters Athletics League (EMA League) indoor and outdoor meetings across Europe\n"
                f"- World Masters Athletics Championships (WMA Indoor, WMA Outdoor/Stadia, WMA Marathon, WMA Road, WMA Mountain/Trail) & World Masters Games (WMG)\n"
                f"- Balkan Masters Athletics Championships: Balkan Masters Indoor (BAMACI), Balkan Masters Outdoor/Stadia (BAMACS), and Balkan Masters Marathon/Cross."
            ),
            (
                f"İleri Tarihli ({future_years_en}) Açıklanmış Tüm Dünya, Avrupa, Balkan ve Ulusal Master Şampiyonaları",
                f"Search specifically for future scheduled {future_years_en} (especially {y3} and {y4}) Masters (35+) athletics championships "
                f"across Europe and the World that already have confirmed dates on European Masters Athletics (european-masters-athletics.org), "
                f"World Masters Athletics (world-masters-athletics.org), OpenTrack, FFA (athle.fr), BMAF (bmaf.org.uk), DLV, FIDAL, RFEA, and Balkan calendars:\n"
                f"- All {y2}, {y3}, and {y4} EMACI (European Masters Indoor), EMACS (European Masters Stadia), EMACNS (European Masters Non-Stadia), "
                f"EMMTRC (Mountain & Trail), EMAMRC (Marathon), EMORRC (Off-Road), and EMA League meetings\n"
                f"- All {y2}, {y3}, and {y4} WMA (World Masters Indoor, Outdoor, Road, Marathon, Mountain) & World Masters Games (WMG)\n"
                f"- All {y2}, {y3}, and {y4} National Masters Indoor, Outdoor, Combined Events, Race Walk, Throws, Cross, and Road Championships "
                f"already published on national federation multi-year calendars (such as France FFA {y2}-{y3}, UK BMAF, Germany, Italy, Spain, Turkey, Balkans, etc.)."
            ),
            (
                f"Türkiye & Doğu Balkanlar - TR, GR, BG, RO ({years_all_tr})",
                f"{years_all_tr} yıllarında Türkiye (TAF - taf.org.tr), Yunanistan (SEGAS / OEBA), Bulgaristan (BFLA) ve Romanya (FRA - country='RO') "
                f"resmi federasyon takvimlerinde yer alan TÜM 35+ yaş Masterlar (Veteran) atletizm yarışmalarını "
                f"(ilgili yıldaki resmi isimleri ve düzenleneceği şehirlerle birlikte) detaylıca bul:\n"
                f"- Ulusal Masterlar Salon (Indoor) ve Açık Saha (Outdoor/Stadia) Atletizm Şampiyonaları\n"
                f"- Ulusal Masterlar Kros (Cross Country), Atmalar (Throws), Çoklu Branşlar ve Yol Koşusu Şampiyonaları\n"
                f"- Federasyon takvimindeki resmi Masterlar Kupa, Bayrak ve Açık (Open) Master yarışmaları."
            ),
            (
                f"Batı Balkanlar & Adriyatik - RS, HR, SI, BA, AL, ME, MK ({years_all_tr})",
                f"Find all official {years_all_en} National Masters (35+ Veteran) Indoor Championships, National Masters Outdoor (Stadia) "
                f"Championships, Masters Throws, Road, Cross Country, and Open/Balkan Masters Championships in "
                f"Serbia (SAS), Croatia (HAS / HAVU), Slovenia (AZS / ZAVS), Bosnia and Herzegovina, Albania, Montenegro, and North Macedonia, "
                f"with their scheduled host cities and exact dates."
            ),
            (
                f"Birleşik Krallık ve İrlanda - GB (BMAF) & IE (IMAA) ({years_all_tr})",
                f"Search the official British Masters Athletic Federation fixture calendar (bmaf.org.uk/fixtures/ and bmaf.opentrack.run) "
                f"and Irish Masters Athletic Association (irishmastersathletics.com) for ALL {years_all_en} scheduled 35+ Masters championships "
                f"and open fixtures, with whatever UK/Irish city hosts them in that year:\n"
                f"- BMAF / British Masters Indoor & Outdoor Track & Field Championships\n"
                f"- BMAF Indoor Pentathlon & 3000m Championships, Outdoor Combined Events Championships\n"
                f"- BMAF Winter Throws Championships, Throws Pentathlon, and 10,000m Track Championships\n"
                f"- British Masters Cross Country Championships & British Masters Cross Country Relay Championships\n"
                f"- British Masters Road Relay Championships\n"
                f"- British Masters Road Running Championships: 1 Mile, 5k Road, 10k Road, 10 Mile Road, Half Marathon, Marathon, and Ultra/24h\n"
                f"- British Masters Race Walking Championships (Indoor, Track & Road)\n"
                f"- Irish Masters (IMAA) Indoor, Outdoor Track & Field, Cross Country, and Road Championships."
            ),
            (
                f"Fransa ve Almanya - FR (FFA) & DE (DLV/LADV) ({years_all_tr})",
                f"Find all official {years_all_en} National and Regional 35+ Masters (Senioren / Vétérans) athletics championships in France and Germany, "
                f"with their scheduled host cities and dates for each year ({y1}, {y2}, {y3}, {y4}):\n"
                f"- France (FFA - athle.fr/contenu/calendrier-championnats-de-france/32): Championnats de France Masters en Salle (Indoor), "
                f"Championnats de France Masters sur Piste (Outdoor/Stadia), Épreuves Combinées, Marche, Lancers Longs, Cross, 5km, 10km, Semi-Marathon, Marathon, and EMA League / Open Masters meetings.\n"
                f"- Germany (DLV / LADV - leichtathletik.de): Deutsche Masters Hallenmeisterschaften & Winterwurf, "
                f"Deutsche Masters Freiluftmeisterschaften (Stadia), Mehrkampf, Team DM Masters, Bahngehen / Straßengehen, "
                f"Straßenlauf (5km, 10km, Halbmarathon, Marathon), Crosslauf, and major Landes/Regional Masters Championships."
            ),
            (
                f"Benelüks, İsviçre ve Avusturya - NL, BE, LU, CH, AT ({years_all_tr})",
                f"Find all official {years_all_en} National 35+ Masters athletics championships in the Netherlands, Belgium, Luxembourg, Switzerland, and Austria, "
                f"with their scheduled host cities and dates:\n"
                f"- Netherlands (Atletiekunie): NK Masters Indoor, NK Masters Outdoor Track & Field, NK Cross / Road / Throws Masters.\n"
                f"- Belgium (VAL / LBFA / KBAB): Belgian Masters Indoor, Outdoor Track & Field, Throws, Combined Events, and Road/Cross Championships.\n"
                f"- Switzerland (Swiss Athletics / Swiss Masters - swiss-masters.ch): Swiss Masters Indoor & Outdoor Championships (including Part 1 & Part 2), "
                f"Winter Throws & Throwing Pentathlon, Hammer, Combined Events, and Road/Mountain Masters.\n"
                f"- Austria (ÖLV - oelv.at): Österreichische Hallenmeisterschaften der Masters & Österreichische Meisterschaften der Masters (Outdoor, Throws, Road)."
            ),
            (
                f"Güney Avrupa - İtalya, İspanya, Portekiz - IT, ES, PT ({years_all_tr})",
                f"Find all official {years_all_en} National 35+ Masters (Veteranos / Master) athletics championships in Italy, Spain, and Portugal, "
                f"with their scheduled host cities and dates:\n"
                f"- Italy (FIDAL - fidal.it): Campionati Italiani Master Indoor & Pentathlon Lanci Invernale, Campionati Italiani Individuali su Pista Master (Outdoor), "
                f"Prove Multiple e Staffette Master, Campionati Italiani Master 5km / 10km / Mezza Maratona / Maratona / 24h su Strada / Cross / Montagna.\n"
                f"- Spain (RFEA - rfea.es): Campeonato de España Máster Short Track / Pista Cubierta, Aire Libre, Lanzamientos Largos, "
                f"Pruebas Combinadas, Clubes, Milla, 10km, Media Maratón, Maratón, and Campo a Través Máster.\n"
                f"- Portugal (FPAtletismo / ANAV): Campeonatos Nacionais de Veteranos em Pista Curta / Coberta (Indoor), "
                f"Ar Livre (Outdoor), Pentatlo, Lançamentos, Corta-Mato (Cross), and Estrada (10km / Meia Maratona / Maratona)."
            ),
            (
                f"Orta, Kuzey ve Baltık Avrupa - PL, CZ, SK, HU, SE, NO, FI, DK, EE, LV, LT ({years_all_tr})",
                f"Find all official {years_all_en} National and Regional 35+ Masters (Veteránů / Veteraner / SAUL / Masters) Indoor, Outdoor, Throws, Combined, "
                f"Road, and Cross Championships (with their scheduled host cities and dates) in:\n"
                f"- Poland (PZLA / PMA - pma.org.pl), Czechia (ČAS), Slovakia (SAZ), and Hungary (MASZ)\n"
                f"- Sweden (Friidrott - ISM & SM Veteraner), Norway (NM Masters), Finland (SAUL SM-hallit, SM-kilpailut, SM-maastot, SM-maantie), and Denmark (DM Masters)\n"
                f"- Estonia, Latvia, Lithuania, Nordic Masters Championships, and Baltic Masters Championships."
            ),
        ]

        for region_label, query_text in discovery_queries:
            try:
                parts = [{"text": query_text + schema_instruction}]
                items = self._call_gemini_json(api_key, parts, use_search=True)
                converted = self._convert_gemini_items(items, default_url="https://european-masters-athletics.org")
                logger.info(f"[Gemini-AI] {region_label} taramasından {len(converted)} yarışma keşfedildi.")
                results.extend(converted)
                time.sleep(2)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Bölgesel arama hatası ({region_label}): {e}")

        return results

    def _find_taf_calendar_pdf_urls(self) -> List[str]:
        """TAF faaliyet takvimi sayfasındaki güncel PDF bağlantılarını 4 yıllık pencereye göre bulur."""
        pdf_urls: List[str] = []
        year_strs = [str(y) for y in self.years]
        try:
            html = self.fetch_html("https://taf.org.tr/ova_doc/faaliyet-takvimi/", timeout=12)
            if html:
                soup = BeautifulSoup(html, "html.parser")
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

        return pdf_urls[:3]

    def _call_gemini_json(self, api_key: str, parts: List[Dict[str, Any]], use_search: bool = True) -> List[Dict[str, Any]]:
        """
        Gemini REST API'sini ücretsiz katman kotası yüksek olan modellerle (gemini-2.5-flash)
        çağırır ve JSON dizisini ayrıştırır.
        """
        models = ["gemini-2.5-flash", "gemini-2.5-flash-lite"]
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json",
        }
        for model in models:
            endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
            gen_config: Dict[str, Any] = {"temperature": 0.1}
            if "2.5" in model:
                gen_config["thinkingConfig"] = {"thinkingBudget": 0}
            payload: Dict[str, Any] = {
                "contents": [{"parts": parts}],
                "generationConfig": gen_config,
            }
            if use_search:
                payload["tools"] = [{"google_search": {}}]

            try:
                resp = requests.post(endpoint, headers=headers, json=payload, timeout=90)
                if resp.status_code == 429:
                    logger.warning(f"[Gemini-AI] Model ({model}) kota sınırı (HTTP 429), 5 sn bekleniyor...")
                    time.sleep(5)
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
                cleaned = re.sub(r"```(?:json)?", "", raw_text).strip("` \n\r\t")
                bracket_match = re.search(r"\[.*\]", cleaned, flags=re.DOTALL)
                if bracket_match:
                    parsed = json.loads(bracket_match.group(0))
                    if isinstance(parsed, list):
                        return parsed
            except Exception as e:
                logger.warning(f"[Gemini-AI] Model ({model}) çağrı uyarısı: {e}")
        return []

    def _convert_gemini_items(self, items: List[Dict[str, Any]], default_url: str) -> List[Competition]:
        """Gemini JSON çıktılarını öğe bazında hata korumalı olarak doğrular ve Competition nesnelerine dönüştürür."""
        converted: List[Competition] = []
        valid_scopes = {"national", "world", "balkan", "europe", "ema_league", "regional"}
        valid_venues = {"indoor", "outdoor", "road", "cross"}
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

                raw_country = str(item.get("country", "EU")).strip().upper()
                raw_cname = str(item.get("country_name", "Avrupa")).strip()
                raw_city = str(item.get("city", "")).strip()

                if raw_city.lower() in {"n/a", "na", "none", "null", "tbd", "tbc", "ybd", "unknown", "belirlenecek", "to be determined", "-"}:
                    raw_city = ""

                country, country_name, _ = self.detect_country(f"{title} {raw_city} {raw_cname}", raw_country, raw_cname)
                city = raw_city or country_name

                venue = str(item.get("venue_type", "")).strip().lower()
                if venue not in valid_venues:
                    venue = self.detect_venue_type(title)

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
                    venue_type=venue,
                    categories=self.detect_categories(title, venue),
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
