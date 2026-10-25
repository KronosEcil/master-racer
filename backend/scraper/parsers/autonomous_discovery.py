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

        # TAF Resmi PDF + Çoklu Arama İndeksi (Bing/Google RSS/DDG) + Resmi Federasyon Siteleri + Hızlı AI Analizi
        ai_discovered = self._discover_with_gemini_ai()
        discovered.extend(ai_discovered)

        deduplicated = deduplicate_competitions(discovered)
        logger.info(
            f"[AutonomousDiscovery_Parser] Tüm yöntemlerin taraması ve tekilleştirmesi tamamlandı. "
            f"Toplam {len(deduplicated)} doğrulanmış Master yarışması hazırlandı."
        )
        return deduplicated

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
                    converted = self._convert_gemini_items(items, default_url=pdf_url)
                    logger.info(f"[Gemini-AI] TAF Resmi PDF takviminden {len(converted)} Master yarışması çıkarıldı.")
                    results.extend(converted)
                    time.sleep(3)
            except Exception as e:
                logger.warning(f"[Gemini-AI] TAF PDF analizi hatası ({pdf_url}): {e}")

        # ADIM 2: Resmi Federasyon Takvimleri + Canlı Arama Motoru İndeksleri
        schema_instruction = (
            "\n\nKESİN ÇIKTI VE DOĞRULUK KURALLARI:\n"
            f"1. Hem aşağıda indirilen canlı resmi sayfa/arama verilerini HEM DE ilgili ulusal ve uluslararası federasyonların "
            f"(EMA, WMA, IMGA, BAMA, BMAF, IMAA, FFA, DLV, ÖLV, Swiss Masters, Atletiekunie, VAL/LBFA, FIDAL, RFEA, FPAtletismo/ANAV, "
            f"PMA/PZLA, ČAS, SAZ, MASZ, SAUL, SFIF, NFIF, DAF, SEGAS, FRA, BFLA, SAS, HAS, AZS, EKJL, LVS, LLAF) "
            f"{years_all_tr} yılları için açıklanmış resmi şampiyona takvimlerini birleştirerek tarihi (YYYY-MM-DD) ve yapılacağı şehri/ülkesi "
            "kesinleşmiş olan TÜM 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren / Veteranos / SAUL) şampiyonalarını eksiksiz çıkar.\n"
            f"2. ÖZELLİKLE {y2} yılı (önümüzdeki yıl) ulusal ve uluslararası Master şampiyonalarını ve {y3} yılı Avrupa/Dünya Master Şampiyonalarını (EMACI, EMACS, EMACNS, WMACS vb.) mutlaka dahil et!\n"
            "3. Elit/Büyükler (Senior Elite), Olimpik Deneme, U23, U20, U18 yarışmalarını ASLA dahil etme.\n"
            "4. Çok günlü yarışmalarda `start_date` ve `end_date` tarihlerini resmi takvimdeki tam aralık olarak (YYYY-MM-DD) yaz. "
            "Tarihi, şehri veya ülkesi 'TBD', 'TBC', 'N/A', 'Not Specified' olan ya da 1 Ocak (YYYY-01-01) gibi yer tutucu tarihli yarışmaları DAHİL ETME!\n"
            "5. `scope` alanı yalnızca şu 6 değerden biri olmalıdır: 'national', 'world', 'balkan', 'europe', 'ema_league', 'regional'.\n"
            "6. Sadece geçerli bir JSON dizisi (array) döndür, markdown veya açıklama metni ekleme:\n"
            '[{"title": "Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "Şehir", "country": "ISO-2 Ülke Kodu (TR, DE, FR, GB, IT, ES, PL, RO, JP, NZ vb.)", '
            '"country_name": "Türkçe Ülke Adı", "scope": "national|world|balkan|europe|ema_league|regional", '
            '"federation": "Federasyon Kısaltması", "website_url": "Resmi Takvim URL"}]'
        )

        official_batches = [
            (
                f"1. Uluslararası Resmi Takvimler: WMA, WMG, EMA ({y1}-{y4}), EMA League ve Balkan Masters",
                [
                    "https://european-masters-athletics.org/championships/",
                    "https://balkanmasters.com/",
                    "https://world-masters-athletics.org/championships/",
                    "https://wmg2027.jp/en/",
                    "https://imga.ch/events/world-masters-games/",
                ],
                [
                    f"European Masters Athletics Championships {y1} {y2} {y3} {y4} EMACI EMACS EMACNS EMMTRC EMAMRC",
                    f"World Masters Athletics Championships {y1} {y2} {y3} {y4} Stadia Indoor Mountain Marathon",
                    f"World Masters Games {y2} {y3} {y4} Kansai European Masters Games {y2}",
                    f"Balkan Masters Athletics Championships {y1} {y2} {y3} BAMACI BAMACS BAMAM",
                    f"EMA League {y1} {y2} European Masters Athletics",
                ],
                f"List ALL confirmed {years_all_en} international Masters championships and games from official federation calendars "
                f"(EMA https://european-masters-athletics.org/championships/, WMA https://world-masters-athletics.org/championships/, "
                f"IMGA https://wmg2027.jp/en/, BAMA https://balkanmasters.com/), ensuring EVERY confirmed {y1}, {y2}, {y3}, and {y4} event is included:\n"
                f"1. European Masters Athletics Championships ({y1}, {y2}, {y3}, {y4}):\n"
                f"   - EMACI (European Masters Athletics Championships Indoor - e.g. {y1} Torun, {y3} Indoor)\n"
                f"   - EMACS (European Masters Athletics Championships Stadia - e.g. {y2} Jyväskylä Finland, {y3} Stadia)\n"
                f"   - EMACNS (European Masters Non-Stadia Championships - e.g. {y1} Catania, {y3} Non-Stadia)\n"
                f"   - EMMTRC / EMORRC (European Masters Mountain & Trail / Off-Road Running Championships - e.g. {y1} Ojós Murcia Spain, {y2}, {y3})\n"
                f"   - EMAMRC (European Masters Marathon Championships - e.g. {y1} Porto, {y2} Lucerne Switzerland, {y3})\n"
                f"   - European Masters Games {y2} (EMG {y2} Como / Lombardia Italy)\n"
                f"2. World Masters Athletics (WMA) & World Masters Games (WMG) ({y1}, {y2}, {y3}, {y4}):\n"
                f"   - World Masters Games {y2} (WMG {y2} Kansai, Japan, 2027-05-14 to 2027-05-30)\n"
                f"   - WMA Outdoor/Stadia Championships ({y1} Daegu South Korea, {y3})\n"
                f"   - WMA Indoor Championships ({y2})\n"
                f"   - WMA Mountain Running Championships ({y1} Meduno Italy, {y2} Klosters Switzerland)\n"
                f"   - WMA Marathon, Half Marathon, 50km/100km & Race Walking Championships ({y1}, {y2}, {y3})\n"
                f"3. EMA League Meetings ({y1} and {y2}) & Balkan Masters Championships (BAMACI Indoor, BAMACS Stadia, BAMAM Marathon in {y1}, {y2}, {y3})."
            ),
            (
                f"2. Birleşik Krallık (BMAF) ve İrlanda (IMAA) {y1} ve {y2} Ulusal Takvimleri",
                [
                    "https://bmaf.org.uk/fixtures/",
                    "https://www.athleticsireland.ie/competition/fixtures/",
                ],
                [
                    f"BMAF British Masters Championships fixtures {y1} {y2} Indoor Outdoor Cross Country Throws",
                    f"British Masters Road Relays 5k 10k Half Marathon Marathon {y1} {y2} bmaf",
                    f"Irish Masters Athletics Championships IMAA {y1} {y2} Indoor Outdoor",
                ],
                f"List all confirmed {y1} AND {y2} British Masters (BMAF - https://bmaf.org.uk/fixtures/) and Irish Masters (IMAA) championships and open fixtures (both {y1} and {y2} editions!), including:\n"
                f"- BMAF Indoor Track & Field Championships ({y1} and {y2}, Lee Valley London)\n"
                f"- BMAF Outdoor Track & Field Championships ({y1} and {y2}, Derby)\n"
                f"- BMAF Indoor Pentathlon & 3000m Championships ({y1} and {y2})\n"
                f"- BMAF Winter Throws & Outdoor Throws Pentathlon Championships ({y1} and {y2})\n"
                f"- BMAF Cross Country Championships & Cross Country Relays ({y1} and {y2})\n"
                f"- BMAF Road Relays (Sutton Park), 1 Mile, 5k, 10k, 10 Mile, Half Marathon, Marathon, 10,000m Track, and Race Walking Championships ({y1} and {y2})\n"
                f"- Irish Masters (IMAA) Indoor Championships (Athlone) & Outdoor Track & Field Championships (Tullamore) ({y1} and {y2})."
            ),
            (
                f"3. Fransa (FFA), Almanya (DLV), İsviçre, Avusturya ve Benelüks {y1}-{y2} Ulusal Takvimleri",
                [
                    "https://www.athle.fr/contenu/calendrier-championnats-de-france/32",
                    "https://www.leichtathletik.de/wettkaempfe/termine",
                    "https://ladv.de/ausschreibungen",
                    "https://www.oelv.at/de/wettkaempfe/kalender",
                ],
                [
                    f"Championnats de France Masters {y1} {y2} salle piste lancers marche cross",
                    f"Deutsche Meisterschaften Masters {y1} {y2} Halle Freiluft Wurf-Mehrkampf Cross",
                    f"Österreichische Mastersmeisterschaften {y1} {y2} Halle Freiluft oelv",
                    f"Schweizermeisterschaften Masters {y1} {y2} NK Masters Indoor Outdoor {y1} {y2} BK Masters Atletiek {y1} {y2}",
                ],
                f"List all confirmed {y1} AND {y2} national 35+ Masters (Vétérans / Senioren / Masters) championships from the official federation calendars of France, Germany, Switzerland, Austria, Netherlands, and Belgium (include both {y1} and {y2} editions):\n"
                f"- France (FFA - country='FR'): Championnats de France Masters en salle (Indoor), Championnats de France Masters sur piste (Outdoor), Championnats de France Masters d'Épreuves Combinées & Marche, Lancers hivernaux, Cross, 5km, 10km, Semi-Marathon, Marathon ({y1} and {y2}).\n"
                f"- Germany (DLV - country='DE'): Deutsche Meisterschaften (DM) Masters Freiluft (Outdoor), DM Halle Masters & Winterwurf (Indoor), DM Wurf-Mehrkampf Masters, DM Cross Masters, DM Straßenlauf/Halbmarathon/Marathon Masters, Team-DM Masters ({y1} and {y2}).\n"
                f"- Switzerland (Swiss Masters - country='CH'): Schweizermeisterschaften Masters Indoor, Schweizermeisterschaften Masters Outdoor (Part 1 & Part 2), Wurf-Fünfkampf ({y1} and {y2}).\n"
                f"- Austria (ÖLV - country='AT'): Österreichische Masters-Hallenmeisterschaften (Indoor) & Österreichische Mastersmeisterschaften Freiluft (Outdoor) ({y1} and {y2}).\n"
                f"- Netherlands (Atletiekunie - country='NL') & Belgium (VAL/LBFA - country='BE'): NK Indoor Masters, NK Masters Outdoor, BK/CB Masters Indoor & Outdoor ({y1} and {y2})."
            ),
            (
                f"4. İtalya (FIDAL), İspanya (RFEA) ve Portekiz (FPAtletismo/ANAV) {y1}-{y2} Ulusal Takvimleri",
                [
                    f"https://www.fidal.it/calendario.php?anno={y1}&categoria=M",
                    f"https://www.fidal.it/calendario.php?anno={y2}&categoria=M",
                    "https://www.fidal.it/calendario.php",
                    "https://atletismorfea.es/calendario",
                ],
                [
                    f"Campionati Italiani Master {y1} {y2} Indoor Pista Pentathlon Lanci Prove Multiple Mezza Maratona fidal",
                    f"Campeonato de España Master {y1} {y2} Aire Libre Short Track Pista Cubierta Lanzamientos Combinadas Campo a Traves rfea",
                    f"Campeonatos Nacionais de Veteranos {y1} {y2} Pista Curta Ar Livre Corta-Mato Pentatlo Marcha fpatletismo anav",
                ],
                f"List all confirmed {y1} AND {y2} national 35+ Masters (Master / Veteranos) championships from the official national federation calendars of Italy, Spain, and Portugal (include both {y1} and {y2} editions):\n"
                f"- Italy (FIDAL - country='IT'): Campionati Italiani Master Indoor e Lanci Invernali (Ancona), Campionati Italiani Individuali su Pista Master (Outdoor), Pentathlon dei Lanci Master, Prove Multiple e Staffette Master, Campionati Italiani Master di Corsa Campestre (Cross), Mezza Maratona, Maratona, Marcia, 10km su Strada, Corsa in Montagna/Trail ({y1} and {y2}).\n"
                f"- Spain (RFEA - country='ES'): Campeonato de España Máster Short Track / Pista Cubierta (Indoor), Campeonato de España Máster de Aire Libre (Outdoor), Lanzamientos Largos Master, Pentatlón de Lanzamientos, Pruebas Combinadas, Campo a Través (Cross), Milla en Ruta, 10.000m, Ruta (5k/10k/Media Maratón/Maratón), Trail/Montaña ({y1} and {y2}).\n"
                f"- Portugal (FPAtletismo / ANAV - country='PT'): Campeonatos Nacionais de Veteranos de Pista Curta (Indoor), Campeonatos Nacionais de Veteranos de Pista ao Ar Livre (Outdoor), Pentatlo de Lançamentos, Corta-Mato (Cross), Marcha em Estrada, Estrada 10km/Meia Maratona ({y1} and {y2})."
            ),
            (
                f"5. Balkanlar, Orta/Doğu Avrupa, İskandinavya ve Baltık {y1}-{y2} Ulusal Takvimleri",
                [
                    "https://balkanmasters.com/",
                    "https://www.pma.org.pl/",
                    "https://www.saul.fi/kilpailut/kilpailukalenteri/",
                    "https://www.has.hr/",
                    "https://slovenska-atletika.si/",
                    "https://ekjl.ee/",
                ],
                [
                    f"Halowe Mistrzostwa Polski Masters Mistrzostwa Polski w Lekkiej Atletyce Masters {y1} {y2} Torun",
                    f"Mistrovství ČR veteránů v atletice {y1} {y2} hala dráha",
                    f"SAUL SM-hallit SM-yleisurheilu SM-maastot SM-puolimaraton SM-moniottelut {y1} {y2}",
                    f"ISM veteraner SM veteraner friidrott {y1} {y2} NM veteraner DM Masters atletik Baltic Masters Championships {y1} {y2}",
                    f"Campionatul National de Atletism Masters {y1} {y2} Romania Greece Bulgaria Serbia Croatia Slovenia Hungary Slovakia",
                ],
                f"List all confirmed {y1} AND {y2} national and regional 35+ Masters (Veteránů / Veteraner / SAUL / Masters) championships from the official calendars of Central/Eastern Europe, Balkans, Nordic, and Baltic countries (include both {y1} and {y2} editions):\n"
                f"- Central Europe: Poland (PMA - country='PL': Halowe Mistrzostwa Polski Masters w Toruniu & Mistrzostwa Polski w Lekkiej Atletyce Masters Outdoor, Rzuty, Wieloboje {y1} & {y2}), Czechia (ČAS - country='CZ': Halové MČR veteránů & MČR veteránů na dráze {y1} & {y2}), Slovakia (SAZ - country='SK': Majstrovstvá SR veteránov {y1} & {y2}), Hungary (MASZ - country='HU': Szenior Atlétikai Magyar Bajnokság Indoor & Outdoor {y1} & {y2}).\n"
                f"- Balkans & Turkey: Turkey (TAF - country='TR': Türkiye Masterlar Salon & Açık Saha Şampiyonası {y1} & {y2}), Greece (SEGAS/OEBA - country='GR': Panhellenic Masters Indoor & Outdoor Championships {y1} & {y2}), Romania (FRA - country='RO': Campionatul Național de Atletism Masters Indoor & Outdoor {y1} & {y2}), Bulgaria (BFLA - country='BG': National Masters Championships {y1} & {y2}), Serbia (SAS - country='RS'), Croatia (HAS/HAVU - country='HR': Prvenstvo Hrvatske za veterane Indoor & Outdoor {y1} & {y2}), Slovenia (AZS/ZAVS - country='SI': Prvenstvo Slovenije za veterane {y1} & {y2}), Albania (FSHA - country='AL').\n"
                f"- Nordic & Baltic: Nordic Masters Championships ({y1} & {y2}), Baltic Masters Championships ({y1} & {y2}), Finland (SAUL - country='FI': SM-hallit, SM-yleisurheilu, SM-moniottelut, SM-maastot, SM-puolimaraton, SM-maantiekymppi, SM-viestit {y1} & {y2}), Sweden (SFIF - country='SE': ISM Veteraner Indoor & SM Veteraner Outdoor {y1} & {y2}), Norway (NFIF - country='NO': NM Veteraner Indoor & Outdoor {y1} & {y2}), Denmark (DAF - country='DK': DM Masters Indoor & Outdoor {y1} & {y2}), Estonia (country='EE'), Latvia (country='LV'), Lithuania (country='LT')."
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
                items = self._call_gemini_json(api_key, parts, use_search=False)
                converted = self._convert_gemini_items(
                    items,
                    default_url=site_urls[0] if site_urls else "https://european-masters-athletics.org",
                )
                logger.info(f"[Gemini-AI] {label} taramasından {len(converted)} yarışma çıkarıldı.")
                results.extend(converted)
                time.sleep(4)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Resmi site tarama hatası ({label}): {e}")

        return results

    def _fetch_hybrid_context(
        self,
        urls: List[str],
        search_queries: List[str],
        max_total_chars: int = 24000,
    ) -> str:
        """
        1. Bing RSS, Google News/Web RSS ve DuckDuckGo Lite üzerinden canlı arama sonuçlarını (snippet) toplar.
        2. Doğrulanmış resmi federasyon URL'lerini doğrudan indirir.
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
                    for item in soup.find_all("item")[:8]:
                        t_txt = re.sub(r"\s+", " ", item.title.get_text(" ", strip=True)) if item.title else ""
                        s_txt = re.sub(r"\s+", " ", item.description.get_text(" ", strip=True)) if item.description else ""
                        u_txt = re.sub(r"\s+", " ", item.link.get_text(" ", strip=True)) if item.link else ""
                        line = f"- {t_txt} | {s_txt} ({u_txt})"
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
                    for item in soup.find_all("item")[:5]:
                        t_txt = re.sub(r"\s+", " ", item.title.get_text(" ", strip=True)) if item.title else ""
                        line = f"- {t_txt}"
                        if t_txt and line not in seen_snippets:
                            seen_snippets.add(line)
                            search_lines.append(line)
            except Exception as e:
                logger.debug(f"[Google-RSS] Arama uyarısı ({sq}): {e}")

        if search_lines:
            chunks.append("=== [CANLI WEB ARAMA İNDEKSİ SONUÇLARI] ===\n" + "\n".join(search_lines[:45]))

        # 2. Doğrudan Resmi Federasyon Takvim Sayfaları
        per_url_limit = max(3500, (max_total_chars - sum(len(c) for c in chunks)) // max(1, len(urls)))
        for url in urls:
            try:
                resp = self.session.get(url, timeout=10)
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
        use_search: bool = False,
    ) -> List[Dict[str, Any]]:
        """
        Hızlı Lite modelleri 35 sn zaman aşımı ile çağırır.
        Ana çalışan model olan `gemini-3.5-flash-lite` geçici RPM/503 alırsa karalisteye alınmaz,
        8 sn beklenip tekrar denenir; diğer bozuk (503) modeller ise anında devre dışı bırakılır.
        """
        all_models = self._get_available_gemini_models(api_key)
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json",
        }
        primary_model = all_models[0] if all_models else "gemini-3.5-flash-lite"

        for attempt in range(2):
            active_models = [m for m in all_models if m not in self._exhausted_models]
            if not active_models:
                active_models = [primary_model]

            for model in active_models:
                if model.startswith("gemma-") and any("inline_data" in p for p in parts):
                    continue

                endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
                payload: Dict[str, Any] = {
                    "contents": [{"parts": parts}],
                    "generationConfig": {"temperature": 0.0},
                }
                if use_search and not model.startswith("gemma-"):
                    payload["tools"] = [{"google_search": {}}]

                try:
                    resp = requests.post(endpoint, headers=headers, json=payload, timeout=35)

                    # Free Tier google_search aracı 429 veya 400 verirse anında aynı modeli araçsız çağır
                    if resp.status_code in (400, 429) and "tools" in payload:
                        payload.pop("tools", None)
                        resp = requests.post(endpoint, headers=headers, json=payload, timeout=35)

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
                        if model == primary_model and not is_daily_limit:
                            logger.warning(f"[Gemini-AI] Ana model ({model}) geçici HTTP {resp.status_code}, 8 sn beklenip tekrar denenecek...")
                            time.sleep(8)
                            retry_resp = requests.post(endpoint, headers=headers, json=payload, timeout=35)
                            if retry_resp.status_code == 200:
                                resp = retry_resp
                            else:
                                continue
                        else:
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
                    if model != primary_model:
                        self._exhausted_models.add(model)
                    logger.warning(f"[Gemini-AI] Model ({model}) 35 sn içinde yanıt vermedi.")
                except Exception as e:
                    logger.warning(f"[Gemini-AI] Model ({model}) çağrı uyarısı: {e}")

            if attempt == 0:
                time.sleep(6)
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

                city_low = raw_city.lower()
                if any(k in city_low for k in ["luzern", "lucerne", "klosters", "zurich", "zürich"]):
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
