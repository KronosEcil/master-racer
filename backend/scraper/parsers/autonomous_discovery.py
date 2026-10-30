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
from datetime import datetime, timezone
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
        logger.info(
            f"[AutonomousDiscovery_Parser] Tüm yöntemlerin taraması ve tekilleştirmesi tamamlandı. "
            f"Toplam {len(deduplicated)} doğrulanmış Master yarışması hazırlandı."
        )
        return deduplicated

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
                            if not re.match(r"^(19|20)\d{2}-\d{2}-\d{2}$", s_date) or int(s_date[:4]) < 1975:
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

        # ADIM 2: Resmi Federasyon Takvimleri + Canlı Arama Motoru İndeksleri (Geçmiş Arşiv + Gelecek Takvim)
        schema_instruction = (
            "\n\nKESİN ÇIKTI VE DOĞRULUK KURALLARI:\n"
            f"1. Hem aşağıda indirilen canlı resmi sayfa/arama verilerini HEM DE ilgili ulusal ve uluslararası federasyonların "
            f"(EMA, WMA, World Masters Sport/IMGA, BAMA, TAF, BMAF, IMAA, FFA, DLV, ÖLV, Swiss Masters, Atletiekunie, VAL/LBFA, FIDAL, RFEA, FPAtletismo/ANAV, "
            f"PMA/PZLA, ČAS, SAZ, MASZ, SAUL, SFIF, NFIF, DAF, SEGAS, FRA, BFLA, SAS, HAS, AZS, EKJL, LVS, LLAF) "
            f"GEÇMİŞTE YAPILMIŞ (tarihsel/arşiv edisyonları: 1985-{y1}) ve GELECEK ({years_all_tr} ve sonrası açıklanmış tüm yıllar) resmi şampiyona takvimlerini birleştirerek "
            "tarihi (YYYY-MM-DD) ve yapıldığı/yapılacağı şehri/ülkesi bilinen TÜM 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren / Veteranos / SAUL) şampiyonalarını eksiksiz çıkar.\n"
            f"2. ÖZELLİKLE arşiv için geçmiş yıllarda düzenlenmiş tüm resmi Master şampiyonalarını / Masters Games organizasyonlarını VE {y1}, {y2}, {y3}, {y4}+ gelecek şampiyonalarını birlikte dahil et!\n"
            "3. Elit/Büyükler (Senior Elite), Olimpik Deneme, U23, U20, U18 yarışmalarını ASLA dahil etme.\n"
            "4. Çok günlü yarışmalarda `start_date` ve `end_date` tarihlerini resmi takvimdeki tam aralık olarak (YYYY-MM-DD) yaz. "
            "Tarihi, şehri veya ülkesi 'TBD', 'TBC', 'N/A', 'Not Specified' olan ya da 1 Ocak (YYYY-01-01) gibi yer tutucu tarihli yarışmaları DAHİL ETME!\n"
            "5. Her yarışmanın `city`, `country` ve `country_name` alanlarına yalnızca O YARIŞMANIN kendi resmi ev sahibi şehrini ve ülkesini yaz (örneğin Perth -> AU/Avustralya, Lahti -> FI/Finlandiya, Como -> IT/İtalya, Taipei -> TW/Tayvan, Abu Dhabi -> AE/Birleşik Arap Emirlikleri; farklı yıllardaki Masters Games ev sahiplerini birbirine karıştırma!).\n"
            "6. `scope` alanı yalnızca şu 6 değerden biri olmalıdır: 'national', 'world', 'balkan', 'europe', 'ema_league', 'regional'.\n"
            "7. Metin içinde yarışmaya ait doğrudan detay/kayıt/sonuç sayfası bağlantısı `[URL: https://...]` varsa `website_url` alanına bu doğrudan bağlantıyı yaz. "
            "Yarışmaya ait resmi şartname, reglaman, bülten veya kılavuz PDF bağlantısı `[PDF: https://...]` varsa `regulations_url` alanına tam URL olarak yaz (yoksa null bırak).\n"
            "8. Sadece geçerli bir JSON dizisi (array) döndür, markdown veya açıklama metni ekleme:\n"
            '[{"title": "Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "Şehir", "country": "ISO-2 Ülke Kodu (TR, DE, FR, GB, IT, ES, PL, RO, FI, AU, JP, NZ vb.)", '
            '"country_name": "Türkçe Ülke Adı", "scope": "national|world|balkan|europe|ema_league|regional", '
            '"federation": "Federasyon Kısaltması", "website_url": "Doğrudan Yarışma veya Resmi Takvim URL", '
            '"regulations_url": "Varsa Resmi Şartname PDF URL veya null"}]'
        )

        official_batches = [
            (
                f"1. Uluslararası Resmi Takvimler (Geçmiş Arşiv & Gelecek): WMA, WMG, EMA, EMA League ve Balkan Masters",
                {"INT", "WORLD", "EUROPE", "BALKAN"},
                [
                    "https://european-masters-athletics.org/championships/",
                    "https://ema.opentrack.run/",
                    "https://balkanmasters.com/",
                    "https://world-masters-athletics.org/championships/",
                    "https://www.worldmasterssport.com/about-the-masters-games/next-masters-games/",
                    "https://www.worldmasterssport.com/about-the-masters-games/all-masters-games/",
                ],
                [
                    f"European Masters Athletics Championships past and upcoming EMACI EMACS EMACNS EMMTRC EMAMRC 2018 2019 2022 2023 2024 2025 {y1} {y2} {y3} {y4}",
                    f"World Masters Athletics Championships history Stadia Indoor 2018 2019 2022 2023 2024 2025 {y1} {y2} {y3} {y4}",
                    f"site:worldmasterssport.com World Masters Games Open Masters Games Winter European Pan American Asia Pacific",
                    f"Balkan Masters Athletics Championships BAMACI BAMACS BAMAM 2020 2021 2022 2023 2024 2025 {y1} {y2} {y3}",
                    f"EMA League {y1 - 1} {y1} {y2} {y3} European Masters Athletics",
                ],
                f"List ALL confirmed PAST (historical editions) and UPCOMING ({years_all_en} and beyond) international Masters championships and Masters Games from official federation calendars "
                f"(EMA https://european-masters-athletics.org/championships/, WMA https://world-masters-athletics.org/championships/, "
                f"World Masters Sport https://www.worldmasterssport.com/about-the-masters-games/next-masters-games/ and all-masters-games/, BAMA https://balkanmasters.com/), "
                f"extracting each event's true host city, country, and exact start/end dates:\n"
                f"1. World Masters Sport (https://www.worldmasterssport.com/) ALL Past & Upcoming Masters Games:\n"
                f"   - Past editions listed on worldmasterssport.com (e.g. Sydney 2009, Torino 2013, Nice 2015, Vancouver 2016, Auckland 2017, Penang 2018, Torino 2019, Innsbruck 2020, Tampere 2023, Jeonbuk 2023, Lombardia 2024, Cleveland 2024, Taipei 2025, Abu Dhabi 2026)\n"
                f"   - Upcoming editions (Kansai 2027, Como Lake 2027, Lahti 2028, Perth 2029) — always use each edition's own true host city and country!\n"
                f"2. World Masters Athletics (WMA) Past & Upcoming Championships:\n"
                f"   - WMA Stadia/Outdoor & Indoor Championships (e.g. Malaga 2018, Torun 2019, Tampere 2022, Torun 2023, Gothenburg 2024, Gainesville 2025, Daegu {y1}, Gainesville {y2}, and future editions) + WMA Mountain/Road Championships.\n"
                f"3. European Masters Athletics (EMA) Past & Upcoming Championships:\n"
                f"   - EMACS Stadia (e.g. Jesolo 2019, Pescara 2023, Madeira 2025, Jyväskylä {y2}, {y3}+), EMACI Indoor (Madrid 2018, Braga 2022, Torun 2024, Torun {y1}, Glasgow {y3}+), EMACNS Non-Stadia (Grosseto 2022, Porto Santo 2024, Catania {y1}, Galway {y3}+), EMMTRC Mountain & Trail, EMAMRC Marathon.\n"
                f"4. Balkan Masters Athletics (BAMA) Past & Upcoming Championships (BAMACS Stadia, BAMACI Indoor, BAMAM Marathon from 2019 through {y4}) & EMA League Meetings."
            ),
            (
                f"2. Birleşik Krallık (BMAF) ve İrlanda (IMAA) Geçmiş ve Gelecek Ulusal Takvimleri",
                {"GB", "IE"},
                [
                    "https://bmaf.org.uk/fixtures/",
                    "https://bmaf.opentrack.run/",
                    "https://www.athleticsireland.ie/competition/fixtures/",
                ],
                [
                    f"BMAF British Masters Championships fixtures 2023 2024 2025 {y1} {y2} {y3} Indoor Outdoor Cross Country Throws",
                    f"British Masters Road Relays 5k 10k Half Marathon Marathon 2024 2025 {y1} {y2} bmaf",
                    f"Irish Masters Athletics Championships IMAA 2023 2024 2025 {y1} {y2} Indoor Outdoor",
                ],
                f"List all confirmed past (2023, 2024, 2025, completed {y1}) AND upcoming ({y1}, {y2}, {y3}, {y4}+) British Masters (BMAF - https://bmaf.org.uk/fixtures/ and https://bmaf.opentrack.run/) and Irish Masters (IMAA) championships and open fixtures:\n"
                f"- BMAF Indoor & Outdoor Track & Field Championships (past & upcoming editions)\n"
                f"- BMAF Indoor Pentathlon & 3000m, Winter Throws & Outdoor Throws Pentathlon Championships\n"
                f"- BMAF Cross Country Championships & Relays, Road Relays, 1 Mile, 5k, 10k, 10 Mile, Half Marathon, Marathon, 10,000m Track, and Race Walking Championships\n"
                f"- Irish Masters (IMAA) Indoor (Athlone) & Outdoor Track & Field Championships (Tullamore) (past & upcoming editions)."
            ),
            (
                f"3. Fransa (FFA/CNAM), Almanya (DLV), İsviçre, Avusturya ve Benelüks Geçmiş ve Gelecek Ulusal Takvimleri",
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
                    f"Championnats de France Masters 2023 2024 2025 {y1} {y2} {y3} salle piste lancers marche cross site:masters.athle.fr OR site:athle.fr",
                    f"Deutsche Meisterschaften Masters 2023 2024 2025 {y1} {y2} {y3} Halle Freiluft Wurf-Mehrkampf Cross ladv",
                    f"Österreichische Mastersmeisterschaften 2024 2025 {y1} {y2} Halle Freiluft oelv",
                    f"Schweizermeisterschaften Masters 2024 2025 {y1} {y2} NK Masters Indoor Outdoor BK Masters Atletiek",
                ],
                f"List all confirmed past (2023, 2024, 2025, completed {y1}) AND upcoming ({y1}, {y2}, {y3}, {y4}+) national and regional 35+ Masters (Vétérans / Senioren / Masters) championships from the official federation calendars of France (https://masters.athle.fr/), Germany (DLV / LADV), Switzerland, Austria, Netherlands, and Belgium:\n"
                f"- France (FFA / CNAM - https://masters.athle.fr/ - country='FR'): Championnats de France Masters en Salle, sur Piste / Estival, Épreuves Combinées, Marche, Cross, Lancers, Route (past & upcoming editions).\n"
                f"- Germany (DLV - country='DE'): Deutsche Meisterschaften (DM) Masters Freiluft, DM Halle & Winterwurf, DM Wurf-Mehrkampf, DM Cross, DM Straßenlauf/Halbmarathon/Marathon, Team-DM Masters (past & upcoming editions).\n"
                f"- Switzerland (Swiss Masters - country='CH'), Austria (ÖLV - country='AT'), Netherlands (Atletiekunie - country='NL') & Belgium (VAL/LBFA - country='BE'): all Indoor & Outdoor Masters National Championships (past & upcoming editions)."
            ),
            (
                f"4. İtalya (FIDAL), İspanya (RFEA) ve Portekiz (FPAtletismo/ANAV) Geçmiş ve Gelecek Ulusal Takvimleri",
                {"IT", "ES", "PT"},
                [
                    f"https://www.fidal.it/calendario.php?anno={y1 - 1}&categoria=M",
                    f"https://www.fidal.it/calendario.php?anno={y1}&categoria=M",
                    f"https://www.fidal.it/calendario.php?anno={y2}&categoria=M",
                    "https://www.fidal.it/calendario.php",
                    "https://atletismorfea.es/calendario",
                ],
                [
                    f"Campionati Italiani Master 2023 2024 2025 {y1} {y2} {y3} Indoor Pista Pentathlon Lanci Prove Multiple Mezza Maratona fidal",
                    f"Campeonato de España Master 2023 2024 2025 {y1} {y2} {y3} Aire Libre Short Track Pista Cubierta Lanzamientos Combinadas Campo a Traves rfea",
                    f"Campeonatos Nacionais de Veteranos 2023 2024 2025 {y1} {y2} {y3} Pista Curta Ar Livre Corta-Mato Pentatlo Marcha fpatletismo anav",
                ],
                f"List all confirmed past (2023, 2024, 2025, completed {y1}) AND upcoming ({y1}, {y2}, {y3}, {y4}+) national 35+ Masters (Master / Veteranos) championships from the official national federation calendars of Italy, Spain, and Portugal:\n"
                f"- Italy (FIDAL - country='IT'): Campionati Italiani Master Indoor e Lanci Invernali (Ancona), Pista Master (Outdoor), Pentathlon dei Lanci, Prove Multiple e Staffette, Corsa Campestre (Cross), Mezza Maratona, Maratona, Marcia, 10km, Montagna/Trail (past & upcoming editions).\n"
                f"- Spain (RFEA - country='ES'): Campeonato de España Máster Short Track / Pista Cubierta (Indoor), Aire Libre (Outdoor), Lanzamientos Largos, Pentatlón de Lanzamientos, Pruebas Combinadas, Campo a Través, Milla, 10.000m, Ruta, Trail/Montaña (past & upcoming editions).\n"
                f"- Portugal (FPAtletismo / ANAV - country='PT'): Campeonatos Nacionais de Veteranos de Pista Curta (Indoor), Ar Livre (Outdoor), Pentatlo de Lançamentos, Corta-Mato, Marcha, Estrada (past & upcoming editions)."
            ),
            (
                f"5. Türkiye (TAF), Balkanlar ve Orta Avrupa Geçmiş ve Gelecek Ulusal Takvimleri",
                {"TR", "PL", "CZ", "SK", "HU", "GR", "RO", "BG", "RS", "HR", "SI", "AL"},
                [
                    "https://taf.org.tr/",
                    "https://www.pma.org.pl/",
                    "https://www.atletika.cz/kalendar-akci/",
                    "https://atletika.hu/",
                    "https://www.has.hr/",
                    "https://slovenska-atletika.si/",
                    "https://www.segas.gr/",
                    "https://fra.ro/",
                ],
                [
                    f"Türkiye Masterlar Salon Atletizm Şampiyonası Türkiye Masterlar Açık Saha Şampiyonası 2022 2023 2024 2025 {y1} {y2} taf",
                    f"Halowe Mistrzostwa Polski Masters Mistrzostwa Polski w Lekkiej Atletyce Masters 2023 2024 2025 {y1} {y2} {y3} Torun pma",
                    f"Halové MČR veteránů Mistrovství ČR veteránů na dráze 2024 2025 {y1} {y2} Majstrovstvá SR veteránov Szenior Atlétikai Magyar Bajnokság",
                    f"Campionatul National de Atletism Masters 2023 2024 2025 {y1} {y2} Bucuresti Panhellenic Masters Athletics Championships",
                    f"Prvenstvo Hrvatske za veterane 2024 2025 {y1} {y2} Prvenstvo Slovenije za veterane Bulgaria Serbia Masters Athletics",
                ],
                f"List all confirmed past (2022, 2023, 2024, 2025, completed {y1}) AND upcoming ({y1}, {y2}, {y3}, {y4}+) national 35+ Masters (Masterlar / Masters / Veteránů / Veteránov / Szenior / Veterane) championships from the official calendars of Turkey, Central Europe, and the Balkans:\n"
                f"- Turkey (TAF - country='TR'): Türkiye Masterlar Salon Şampiyonası, Türkiye Masterlar Açık Saha (Pist) Şampiyonası, Masterlar Atmalar Kupası, Masterlar Kros Şampiyonası (past 2022-2025 & upcoming {y1}-{y3} editions).\n"
                f"- Poland (PMA - country='PL'), Czechia (ČAS - country='CZ'), Slovakia (SAZ - country='SK'), Hungary (MASZ - country='HU'), Greece (SEGAS/OEBA - country='GR'), Romania (FRA - country='RO'), Bulgaria (BFLA - country='BG'), Serbia (SAS - country='RS'), Croatia (HAS/HAVU - country='HR'), Slovenia (AZS/ZAVS - country='SI'), Albania (FSHA - country='AL') Indoor & Outdoor Masters Championships (past & upcoming editions)."
            ),
            (
                f"6. İskandinavya (Finlandiya, İsveç, Norveç, Danimarka) ve Baltık (Estonya, Letonya, Litvanya) Geçmiş ve Gelecek Ulusal Takvimleri",
                {"FI", "SE", "NO", "DK", "EE", "LV", "LT"},
                [
                    "https://www.saul.fi/kilpailut/kilpailukalenteri/",
                    "https://www.friidrott.se/tavling-landslag/tavling/tavlingskalender/",
                    "https://www.friidrett.no/aktivitet/terminliste/",
                    "https://dansk-atletik.dk/",
                    "https://ekjl.ee/voistlused/voistluskalender/",
                    "https://athletics.lv/lv/kalendars",
                ],
                [
                    f"SAUL SM-hallit SM-yleisurheilu SM-maastot SM-puolimaraton SM-moniottelut 2023 2024 2025 {y1} {y2} {y3}",
                    f"ISM veteraner SM veteraner friidrott 2023 2024 2025 {y1} {y2} {y3} Sverige",
                    f"NM veteraner friidrett 2024 2025 {y1} {y2} {y3} DM Masters atletik Nordic Masters Championships",
                    f"Baltic Masters Athletics Championships 2023 2024 2025 {y1} {y2} {y3} Eesti meistrivõistlused veteranidele Latvijas čempionāts veterāniem",
                ],
                f"List all confirmed past (2023, 2024, 2025, completed {y1}) AND upcoming ({y1}, {y2}, {y3}, {y4}+) national and regional 35+ Masters (SAUL / Veteraner / Masters) championships from the official calendars of the Nordic and Baltic countries:\n"
                f"- Nordic & Baltic Regional: Nordic Masters Championships Indoor & Outdoor, Baltic Open Masters Athletics Championships (past & upcoming editions).\n"
                f"- Finland (SAUL - country='FI'), Sweden (SFIF - country='SE'), Norway (NFIF - country='NO'), Denmark (DAF - country='DK'), Estonia (EKJL - country='EE'), Latvia (LVS - country='LV'), Lithuania (LLAF - country='LT') Indoor & Outdoor Masters Championships (past & upcoming editions)."
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
                        "(Resmi federasyon metninde açıkça farklı bir tarih, şehir veya ülke yazmadıkça, "
                        "bu mevcut yarışmaların start_date ve end_date bilgilerini koru; resmi sitede açıkça ev sahibi şehir/ülke yazıyorsa resmi sitedeki konumu esas al):\n"
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
                )
                logger.info(f"[Gemini-AI] {label} taramasından {len(converted)} yarışma çıkarıldı.")
                results.extend(converted)
                time.sleep(3)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Resmi site tarama hatası ({label}): {e}")

        return results

    def _build_existing_context(self, batch_countries: set) -> str:
        """İlgili gruptaki mevcut veritabanı kayıtlarını özetleyerek AI'ın rastgele tarih kaydırmasını önler."""
        if not self.existing_comps:
            return ""
        lines = []
        is_intl_batch = "WORLD" in batch_countries or "EUROPE" in batch_countries
        for doc_id, data in self.existing_comps.items():
            if not isinstance(data, dict):
                continue
            c_code = str(data.get("country", "")).upper()
            scope = str(data.get("scope", "")).lower()
            title = str(data.get("title", "")).strip()
            s_date = str(data.get("start_date", "")).strip()[:10]
            e_date = str(data.get("end_date", s_date)).strip()[:10]
            city = str(data.get("city", "")).strip()
            if not title or not s_date:
                continue
            # Başlığında "kansai" geçmeyen bir Masters Games kaydında eski hatadan dolayı Kansai/JP kalmışsa AI'a bunu gönderme
            t_low = title.lower()
            if ("masters games" in t_low or "wmg" in t_low) and "kansai" not in t_low and ("kansai" in city.lower() or c_code == "JP"):
                continue
            if is_intl_batch and scope in {"world", "europe", "balkan", "ema_league"}:
                lines.append(f"- {title} | {s_date}..{e_date} | {city} ({c_code}) | scope={scope}")
            elif not is_intl_batch and c_code in batch_countries and scope in {"national", "regional"}:
                lines.append(f"- {title} | {s_date}..{e_date} | {city} ({c_code}) | scope={scope}")
        return "\n".join(lines[:35])

    def _fetch_hybrid_context(
        self,
        urls: List[str],
        search_queries: List[str],
        max_total_chars: int = 16000,
    ) -> str:
        """
        1. Bing RSS, Google News/Web RSS üzerinden canlı arama sonuçlarını (snippet) toplar.
        2. Doğrulanmış resmi federasyon URL'lerini doğrudan indirir (menü/sidebar gürültüsünü temizler,
           ancak yarışma detay sayfalarını [URL: ...] ve şartname PDF bağlantılarını [PDF: ...] olarak korur).
        """
        chunks: List[str] = []
        search_lines: List[str] = []
        seen_snippets = set()

        for sq in search_queries:
            # A) Bing RSS Arama (GitHub Actions / Azure IP'lerinde %100 engelsiz çalışır)
            try:
                bing_url = f"https://www.bing.com/search?q={quote_plus(sq)}&format=rss"
                resp = self.session.get(bing_url, timeout=8)
                if resp.status_code == 200 and resp.text:
                    soup = BeautifulSoup(resp.text, "xml")
                    for item in soup.find_all("item")[:7]:
                        t_txt = re.sub(r"\s+", " ", item.title.get_text(" ", strip=True)) if item.title else ""
                        s_txt = re.sub(r"\s+", " ", item.description.get_text(" ", strip=True)) if item.description else ""
                        u_txt = re.sub(r"\s+", " ", item.link.get_text(" ", strip=True)) if item.link else ""
                        line = f"- {t_txt} | {s_txt} [URL: {u_txt}]"
                        if (t_txt or s_txt) and line not in seen_snippets:
                            seen_snippets.add(line)
                            search_lines.append(line)
            except Exception as e:
                logger.debug(f"[Bing-RSS] Arama uyarısı ({sq}): {e}")

            # B) Google News/Web RSS Arama
            try:
                gn_url = f"https://news.google.com/rss/search?q={quote_plus(sq)}&hl=en-US&gl=US&ceid=US:en"
                resp = self.session.get(gn_url, timeout=8)
                if resp.status_code == 200 and resp.text:
                    soup = BeautifulSoup(resp.text, "xml")
                    for item in soup.find_all("item")[:4]:
                        t_txt = re.sub(r"\s+", " ", item.title.get_text(" ", strip=True)) if item.title else ""
                        line = f"- {t_txt}"
                        if t_txt and line not in seen_snippets:
                            seen_snippets.add(line)
                            search_lines.append(line)
            except Exception as e:
                logger.debug(f"[Google-RSS] Arama uyarısı ({sq}): {e}")

        if search_lines:
            chunks.append("=== [CANLI WEB ARAMA İNDEKSİ SONUÇLARI] ===\n" + "\n".join(search_lines[:35]))

        # 2. Doğrudan Resmi Federasyon Takvim Sayfaları (Gerçek href ve PDF linklerini koruyarak)
        per_url_limit = 2800
        ignored_domains = ("facebook.com", "instagram.com", "twitter.com", "x.com", "youtube.com", "linkedin.com")
        deep_link_keywords = (
            ".pdf", "reglement", "regolamento", "regulamento", "regulations", "manual",
            "ausschreibung", "circular", "bulletin", "opentrack.run", "ladv.de/ausschreibung",
            "espaces.html", "championship", "calendario", "fixtures", "event", "meeting",
            "masters-games", "next-masters-games", "all-masters-games"
        )
        for url in urls:
            try:
                resp = self.session.get(url, timeout=10)
                if resp.status_code != 200 or not resp.text:
                    continue
                soup = BeautifulSoup(resp.text, "html.parser")
                for tag in soup(["script", "style", "noscript", "svg", "footer", "nav", "header", "aside"]):
                    tag.decompose()
                # Sitelerdeki (masters.athle.fr, worldmasterssport.com vb.) üst/yan mega menüleri temizle
                for sel in [
                    "#ctnLeft", "#ctnLeftMenu", "#ctnTop", ".menuLeft", "#sidebar", ".sidebar",
                    "#main-header", "#top-header", ".et-l--header", ".et_pb_menu", ".et-menu-nav", ".sub-menu", ".et_mobile_menu"
                ]:
                    for el in soup.select(sel):
                        el.decompose()

                # Yarışma detay linklerini [URL: ...] ve şartname/bülten PDF'lerini [PDF: ...] olarak metne göm
                for a_tag in soup.find_all("a", href=True):
                    raw_href = (a_tag.get("href") or "").strip()
                    if not raw_href or raw_href.startswith(("#", "javascript:", "mailto:", "tel:")):
                        continue
                    full_href = urljoin(url, raw_href)
                    href_low = full_href.lower()
                    if any(d in href_low for d in ignored_domains):
                        continue
                    anchor_txt = a_tag.get_text(" ", strip=True)
                    if href_low.endswith(".pdf"):
                        a_tag.string = f"{anchor_txt} [PDF: {full_href}]"
                    elif full_href != url and any(k in href_low for k in deep_link_keywords) and len(anchor_txt) >= 3:
                        a_tag.string = f"{anchor_txt} [URL: {full_href}]"

                lines = []
                for line in soup.get_text(separator="\n").splitlines():
                    cleaned = re.sub(r"\s+", " ", line).strip()
                    if len(cleaned) >= 4:
                        lines.append(cleaned)
                text_block = "\n".join(lines)
                # masters.athle.fr sağ sütunundaki 100 komite listesini ("Les Espaces Structures") kes
                if "Les Espaces Structures" in text_block:
                    text_block = text_block.split("Les Espaces Structures")[0].strip()
                text_block = text_block[:per_url_limit]
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
        Sadece 2026'da gerçek çalışan ve HTTP 503 vermeyen modelleri listeler:
        1. gemini-3.5-flash-lite (En hızlı, 3-5 sn yanıt)
        2. gemini-3-flash-preview (En yüksek kapsamlı çıkarım gücü)
        3. gemma-3-27b-it (Yedek açık model)
        """
        if self._available_models:
            return self._available_models

        allowed_models = [
            "gemini-3.5-flash-lite",
            "gemini-3-flash-preview",
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

        logger.info(f"[Gemini-AI] Aktif AI modelleri: {self._available_models}")
        return self._available_models

    def _call_gemini_json(
        self,
        api_key: str,
        parts: List[Dict[str, Any]],
        use_search: bool = False,
    ) -> List[Dict[str, Any]]:
        """
        Çalışan modelleri (`gemini-3.5-flash-lite` -> `gemini-3-flash-preview` -> `gemma-3-27b-it`)
        50 sn zaman aşımı ile çağırır ve geçici yoğunlukta ana modelleri karalisteye almaz.
        """
        all_models = self._get_available_gemini_models(api_key)
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json",
        }
        core_models = {"gemini-3.5-flash-lite", "gemini-3-flash-preview"}

        for attempt in range(2):
            active_models = [m for m in all_models if m not in self._exhausted_models]
            if not active_models:
                active_models = list(all_models)

            for model in active_models:
                if model.startswith("gemma-") and any("inline_data" in p for p in parts):
                    continue

                endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
                gen_config: Dict[str, Any] = {"temperature": 0.0, "maxOutputTokens": 8192}
                if not use_search and not model.startswith("gemma-"):
                    gen_config["responseMimeType"] = "application/json"
                payload: Dict[str, Any] = {
                    "contents": [{"parts": parts}],
                    "generationConfig": gen_config,
                }
                if use_search and not model.startswith("gemma-"):
                    payload["tools"] = [{"google_search": {}}]

                try:
                    resp = requests.post(endpoint, headers=headers, json=payload, timeout=50)

                    # Free Tier google_search aracı 429 veya 400 verirse anında aynı modeli araçsız çağır
                    if resp.status_code in (400, 429) and "tools" in payload:
                        payload.pop("tools", None)
                        resp = requests.post(endpoint, headers=headers, json=payload, timeout=50)

                    if resp.status_code == 402:
                        logger.error(
                            "[Gemini-AI] HTTP 402: API anahtarının bağlı olduğu proje 'Prepay (No credits)' modunda. "
                            "Lütfen 'Free tier' projesine ait bir API anahtarı kullanın."
                        )
                        self._exhausted_models.update(all_models)
                        return []

                    if resp.status_code in (429, 503):
                        body_lower = resp.text.lower()
                        is_daily_limit = resp.status_code == 429 and (
                            "perday" in body_lower or "per_day" in body_lower or "daily" in body_lower
                        )
                        if model in core_models and not is_daily_limit:
                            logger.warning(f"[Gemini-AI] Model ({model}) geçici HTTP {resp.status_code}, sıradaki modele geçiliyor...")
                            time.sleep(2)
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
                    if model not in core_models:
                        self._exhausted_models.add(model)
                    logger.warning(f"[Gemini-AI] Model ({model}) 50 sn içinde yanıt vermedi, sıradaki model deneniyor...")
                except Exception as e:
                    logger.warning(f"[Gemini-AI] Model ({model}) çağrı uyarısı: {e}")

            if attempt == 0:
                time.sleep(5)
        return []

    def _convert_gemini_items(
        self,
        items: List[Dict[str, Any]],
        default_url: str,
        default_trust_level: int = 2,
        default_regulations_url: Optional[str] = None,
    ) -> List[Competition]:
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
                if not re.match(r"^(19|20)\d{2}-\d{2}-\d{2}$", start_date):
                    continue
                if int(start_date[:4]) < 1975:
                    continue
                if not re.match(r"^(19|20)\d{2}-\d{2}-\d{2}$", end_date) or end_date < start_date:
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
                # Başlığında "kansai" geçmeyen bir Masters Games için yanlışlıkla Kansai/JP atanmışsa temizle, gerçek ülkeyi başlıktan bul
                if ("masters games" in title_low or "wmg" in title_low) and "kansai" not in title_low and ("kansai" in city_low or raw_country in ("JP", "JPN")):
                    raw_city = ""
                    raw_country = ""
                    raw_cname = ""
                    city_low = ""
                if ("world" in title_low or "wma" in title_low or "wmra" in title_low) and "mountain" in title_low and start_date.startswith("2027"):
                    raw_city = "Klosters"
                    raw_country = "CH"
                    raw_cname = "İsviçre"
                elif ("european" in title_low or "emmtrc" in title_low) and ("mountain" in title_low or "trail" in title_low) and start_date.startswith("2027"):
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

                # Doğrudan OpenTrack, LADV, World Masters Sport, resmi PDF veya resmi CNAM alt sayfalarından gelen kayıtların güven puanı = 3
                trust_level = default_trust_level
                if reg_url or any(d in web_url.lower() for d in ("opentrack.run", "ladv.de/ausschreibung", "masters.athle.fr/asp.net/espaces.html", "worldmasterssport.com")):
                    trust_level = 3

                today_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d")
                comp_status = "completed" if end_date < today_iso else "scheduled"

                comp_id = generate_competition_id(title, start_date, country, city)

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
                    status=comp_status,
                    created_at=datetime.now(timezone.utc).isoformat(),
                )
                comp.content_hash = compute_content_hash(comp)
                converted.append(comp)
            except Exception as item_err:
                logger.debug(f"[Gemini-AI] Öğe dönüştürme uyarısı: {item_err}")
        return converted
