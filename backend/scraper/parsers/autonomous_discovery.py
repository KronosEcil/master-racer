"""
Master Racer - Official Federation Calendar AI Discovery Engine
Gemini'yi rastgele Google aramasına bırakmak yerine; her uluslararası birliğin (WMA, EMA, BAMA)
ve her ülkenin resmi ulusal atletizm/masterlar federasyonunun resmi takvim sitelerini (`site:`)
hedefleyerek sıfır halüsinasyonla (temperature=0.0) tarayan otonom yapay zeka motoru.
"""

import re
import time
import json
import base64
import logging
from datetime import datetime
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
    Her ülkenin ve uluslararası federasyonun resmi takvim web sitelerini ayrı ayrı tanımlayarak,
    Gemini AI'ya yalnızca bu resmi kaynakları analiz ettiren sıfır-halüsinasyonlu keşif motoru.
    """

    def __init__(self, existing_comps: Optional[Dict[str, dict]] = None):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://european-masters-athletics.org")
        current_year = datetime.utcnow().year
        self.years = [current_year, current_year + 1, current_year + 2, current_year + 3]
        self.existing_comps: Dict[str, dict] = existing_comps or {}
        self._exhausted_models = set()
        self._available_models: List[str] = []
        self._search_tool_disabled: bool = False

    def parse(self) -> List[Competition]:
        logger.info(f"Resmi Federasyon Takvimleri Yapay Zeka Tarayıcısı başlatılıyor (Yıllar: {self.years})...")
        discovered = self._discover_with_gemini_ai()
        deduplicated = deduplicate_competitions(discovered)
        logger.info(
            f"[AutonomousDiscovery_Parser] Resmi site taraması ve tekilleştirmesi tamamlandı. "
            f"Toplam {len(deduplicated)} doğrulanmış Master yarışması hazırlandı."
        )
        return deduplicated

    def _discover_with_gemini_ai(self) -> List[Competition]:
        """
        Her bölgenin ve ülkenin resmi ulusal takvim sitelerini ayrı ayrı hedefleyerek:
        1. Türkiye (TAF) resmi faaliyet takvimi PDF'ini doğrudan okur.
        2. Uluslararası (WMA, EMA, BAMA) ve Ulusal Federasyonların resmi takvim sitelerini (`site:...`)
           Gemini ile hafif ve hızlı isteklerle araştırır (ücretsiz katman TPM/RPM sınırlarına tam uyumlu).
        """
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not api_key:
            logger.warning("[Gemini-AI] GEMINI_API_KEY tanımlı değil! Tarama yapılamadı.")
            return []

        results: List[Competition] = []
        y1, y2, y3, y4 = self.years[0], self.years[1], self.years[2], self.years[3]
        years_all_tr = f"{y1}, {y2}, {y3} ve {y4}"
        years_all_en = f"{y1}, {y2}, {y3}, and {y4}"

        # -------------------------------------------------------------------------
        # ADIM 1: Türkiye Atletizm Federasyonu (TAF) Resmi Takvim PDF'i (`taf.org.tr`)
        # -------------------------------------------------------------------------
        taf_pdf_urls = self._find_taf_calendar_pdf_urls()
        for pdf_url in taf_pdf_urls[:1]:
            try:
                resp = self.session.get(pdf_url, timeout=25)
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
                    items = self._call_gemini_json(api_key, parts, use_search=False)
                    converted = self._convert_gemini_items(items, default_url=pdf_url)
                    logger.info(f"[Gemini-AI] TAF Resmi PDF takviminden {len(converted)} Master yarışması çıkarıldı.")
                    results.extend(converted)
                    time.sleep(12)
            except Exception as e:
                logger.warning(f"[Gemini-AI] TAF PDF analizi hatası ({pdf_url}): {e}")

        # -------------------------------------------------------------------------
        # ADIM 2: Resmi Federasyon Takvim Siteleri Sorguları (Her Ülke İçin Resmi Kaynak)
        # -------------------------------------------------------------------------
        schema_instruction = (
            "\n\nKESİN ÇIKTI VE DOĞRULUK KURALLARI:\n"
            f"1. SADECE yukarıda belirtilen resmi federasyon takvim sitelerinde yer alan ve {years_all_tr} yıllarında "
            "tarihi (YYYY-MM-DD) ile yapılacağı şehri kesinleşmiş olan 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren) "
            "yarışmalarını çıkar. Resmi takvimde yer almayan hiçbir yarışmayı, tarihi veya şehri ASLA TAHMİN ETME / UYDURMA!\n"
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

        official_queries = [
            (
                f"1. Uluslararası Resmi Takvimler: WMA, EMA, OpenTrack ve Balkan Masters ({years_all_tr})",
                [
                    "https://european-masters-athletics.org/championships/",
                    "https://balkanmasters.com/",
                    "https://world-masters-athletics.org/championships/",
                    "https://wmg2027.jp/",
                ],
                True,
                f"Analyze ONLY the official calendars of:\n"
                f"- European Masters Athletics: https://european-masters-athletics.org/championships/ and https://data.opentrack.run/\n"
                f"- Balkan Masters Athletics: https://balkanmasters.com/\n"
                f"- World Masters Athletics: https://world-masters-athletics.org/championships/ and https://wmg2027.jp/\n"
                f"Extract all confirmed {years_all_en} European Masters Championships (EMACI, EMACS, EMACNS, EMMTRC, EMAMRC, EMORRC), "
                f"EMA League meetings, Balkan Masters Championships (BAMACI, BAMACS, BAMAM), World Masters Championships (WMA), "
                f"and World Masters Games (WMG) with their exact official dates and host cities."
            ),
            (
                f"2. Birleşik Krallık (BMAF) ve İrlanda (IMAA) Resmi Takvim Siteleri ({years_all_tr})",
                [
                    "https://bmaf.org.uk/fixtures/",
                    "https://bmaf.opentrack.run/",
                    "https://irishmastersathletics.com/",
                    "https://www.athleticsireland.ie/competition/fixtures/",
                ],
                False,
                f"Analyze ONLY the official UK and Ireland Masters athletics fixture calendars:\n"
                f"- United Kingdom (BMAF): https://bmaf.org.uk/fixtures/ and https://bmaf.opentrack.run/\n"
                f"- Ireland (IMAA): https://irishmastersathletics.com/ and https://www.athleticsireland.ie/competition/fixtures/\n"
                f"Extract all confirmed {years_all_en} BMAF / British Masters and Irish Masters (IMAA) championships and open fixtures, including:\n"
                f"Indoor & Outdoor Track & Field Championships, Indoor Pentathlon & 3000m, Winter Throws & Throws Pentathlon, 10,000m, "
                f"Cross Country Championships, Cross Country Relay Championships, Road Relay Championships, "
                f"1 Mile, 5k Road, 10k Road, 10 Mile, Half Marathon, Marathon, Ultra, and Race Walking Championships."
            ),
            (
                f"3. Fransa (FFA), Almanya (DLV/LADV), İsviçre, Avusturya ve Benelüks Resmi Siteleri ({years_all_tr})",
                [
                    "https://www.athle.fr/contenu/calendrier-championnats-de-france/32",
                    "https://www.leichtathletik.de/wettkaempfe/termine",
                    "https://ladv.de/ausschreibungen",
                    "https://www.swiss-masters.ch/",
                    "https://www.oelv.at/de/wettkaempfe/kalender",
                    "https://www.atletiek.nl/wedstrijden/",
                    "https://www.atletiek.be/wedstrijden/kalender",
                ],
                False,
                f"Analyze ONLY the official national federation calendars for {years_all_en} 35+ Masters (Vétérans / Senioren / Masters) championships:\n"
                f"- France (FFA): https://www.athle.fr/contenu/calendrier-championnats-de-france/32\n"
                f"- Germany (DLV / LADV): https://www.leichtathletik.de/wettkaempfe/termine and https://ladv.de/ausschreibungen\n"
                f"- Switzerland (Swiss Masters): https://www.swiss-masters.ch/ and https://www.swiss-athletics.ch/\n"
                f"- Austria (ÖLV): https://www.oelv.at/de/wettkaempfe/kalender\n"
                f"- Netherlands (Atletiekunie): https://www.atletiek.nl/wedstrijden/\n"
                f"- Belgium (VAL / LBFA): https://www.atletiek.be/wedstrijden/kalender and https://www.lbfa.be/fr/calendrier"
            ),
            (
                f"4. İtalya (FIDAL), İspanya (RFEA) ve Portekiz (FPAtletismo/ANAV) Resmi Siteleri ({years_all_tr})",
                [
                    "https://www.fidal.it/calendario.php",
                    "https://www.fidal.it/categoria/Master/25",
                    "https://atletismorfea.es/calendario",
                    "https://fpatletismo.pt/atletismo/calendario-nacional/",
                    "https://www.anavportugal.com/",
                ],
                False,
                f"Analyze ONLY the official national federation calendars for {years_all_en} 35+ Masters (Master / Veteranos) championships:\n"
                f"- Italy (FIDAL): https://www.fidal.it/calendario.php and https://www.fidal.it/categoria/Master/25\n"
                f"- Spain (RFEA): https://atletismorfea.es/calendario and https://www.rfeacontent.es/calendario/\n"
                f"- Portugal (FPAtletismo / ANAV): https://fpatletismo.pt/atletismo/calendario-nacional/ and https://www.anavportugal.com/"
            ),
            (
                f"5. Balkanlar, Orta/Doğu Avrupa, İskandinavya ve Baltık Resmi Federasyon Siteleri ({years_all_tr})",
                [
                    "https://balkanmasters.com/",
                    "https://atletismveterani.ro/",
                    "https://www.pma.org.pl/",
                    "https://www.veteraniatletika.cz/",
                    "https://www.saul.fi/kilpailut/kilpailukalenteri/",
                    "https://www.has.hr/",
                    "https://slovenska-atletika.si/",
                    "https://ekjl.ee/",
                ],
                False,
                f"Analyze ONLY the official national federation calendars for {years_all_en} 35+ Masters (Veteránů / Veteraner / SAUL / Masters) championships:\n"
                f"- Balkans: https://balkanmasters.com/ , Greece (https://www.segas.gr/ , https://www.oeba.gr/), Bulgaria (https://www.bfla.org/), "
                f"Romania (https://www.fra.ro/ , https://atletismveterani.ro/ - country='RO'), Serbia (https://sas.org.rs/), Croatia (https://www.has.hr/ , https://www.havu.hr/), Slovenia (https://slovenska-atletika.si/ , https://zavs.si/)\n"
                f"- Central Europe: Poland (https://www.pma.org.pl/ , https://pzla.pl/), Czechia (https://www.atletika.cz/ , https://www.veteraniatletika.cz/), Slovakia (https://www.atletika.sk/), Hungary (https://atletika.hu/)\n"
                f"- Nordic & Baltic: Finland (https://www.saul.fi/kilpailut/kilpailukalenteri/), Sweden (https://www.friidrott.se/), Norway (https://www.friidrett.no/), Denmark (https://dansk-atletik.dk/), Estonia (https://ekjl.ee/), Latvia (https://athletics.lv/), Lithuania (https://lengvoji.lt/)"
            ),
        ]

        for label, site_urls, include_ema_api, query_text in official_queries:
            try:
                official_digest = self._fetch_official_sites_digest(site_urls, include_ema_api=include_ema_api)
                full_prompt = query_text
                if official_digest:
                    full_prompt += (
                        "\n\nAŞAĞIDA BU RESMİ FEDERASYON SİTELERİNDEN DOĞRUDAN ÇEKİLEN GÜNCEL TAKVİM VERİLERİ YER ALMAKTADIR:\n"
                        f"{official_digest}\n"
                    )
                full_prompt += schema_instruction
                parts = [{"text": full_prompt}]
                items = self._call_gemini_json(api_key, parts, use_search=not self._search_tool_disabled)
                converted = self._convert_gemini_items(items, default_url=site_urls[0] if site_urls else "https://european-masters-athletics.org")
                logger.info(f"[Gemini-AI] {label} taramasından {len(converted)} yarışma çıkarıldı.")
                results.extend(converted)
                time.sleep(8)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Resmi site tarama hatası ({label}): {e}")

        return results

    def _fetch_official_sites_digest(self, urls: List[str], include_ema_api: bool = False, max_total_chars: int = 14000) -> str:
        """
        Resmi federasyon takvim sayfalarını (ve uluslararası grup için EMA resmi AWS API akışını)
        doğrudan Python ile indirip temiz metin özetine dönüştürür. Böylece Free Tier'da Google Search
        aracı (google_search) kota engeline (HTTP 429) takılsa bile resmi takvimler %100 okunur.
        """
        chunks: List[str] = []
        min_year = self.years[0]

        if include_ema_api:
            try:
                ema_resp = self.session.get("https://s831118207.onlinehome.us/ema_cham_aw/api/v1/events", timeout=12)
                if ema_resp.status_code == 200:
                    payload = ema_resp.json()
                    data_list = payload.get("data", []) if isinstance(payload, dict) else (payload if isinstance(payload, list) else [])
                    ema_lines = ["=== [RESMİ EMA AWS API VERİ AKIŞI] ==="]
                    for ev in data_list:
                        if not isinstance(ev, dict):
                            continue
                        s_date = str(ev.get("startDate") or ev.get("start_date") or ev.get("date") or "")[:10]
                        if s_date and re.match(r"^20\d{2}", s_date) and int(s_date[:4]) >= min_year:
                            e_date = str(ev.get("endDate") or ev.get("end_date") or s_date)[:10]
                            name = str(ev.get("name") or ev.get("title") or "").strip()
                            loc = str(ev.get("location") or ev.get("city") or "").strip()
                            country = str(ev.get("country") or "").strip()
                            web = str(ev.get("website") or ev.get("url") or "https://european-masters-athletics.org/championships/").strip()
                            ema_lines.append(f"- {name} | {s_date}..{e_date} | Location: {loc} ({country}) | URL: {web}")
                    if len(ema_lines) > 1:
                        chunks.append("\n".join(ema_lines))
            except Exception as e:
                logger.debug(f"[EMA-API] Resmi veri akışı okuma uyarısı: {e}")

        per_url_limit = max(1800, max_total_chars // max(1, len(urls)))
        for url in urls:
            try:
                html = self.fetch_html(url, timeout=10)
                if not html:
                    continue
                soup = BeautifulSoup(html, "html.parser")
                for tag in soup(["script", "style", "noscript", "svg", "footer", "nav"]):
                    tag.decompose()
                lines = []
                for line in soup.get_text(separator="\n").splitlines():
                    cleaned = re.sub(r"\s+", " ", line).strip()
                    if len(cleaned) >= 4:
                        lines.append(cleaned)
                text_block = "\n".join(lines)[:per_url_limit]
                if text_block:
                    chunks.append(f"=== [RESMİ TAKVİM KAYNAĞI: {url}] ===\n{text_block}")
            except Exception as e:
                logger.debug(f"[Official-Fetch] {url} okuma uyarısı: {e}")

        combined = "\n\n".join(chunks)
        return combined[:max_total_chars]

    def _find_taf_calendar_pdf_urls(self) -> List[str]:
        """TAF faaliyet takvimi sayfasındaki güncel PDF bağlantılarını bulur."""
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

        return pdf_urls[:2]

    @staticmethod
    def _extract_json_from_text(raw_text: str) -> List[Dict[str, Any]]:
        """
        Gemini yanıtındaki JSON dizisini `raw_decode` kullanarak ayrıştırır.
        Yanıtın sonundaki Google Search atıfları (`[1]`, `[2]`) veya ek açıklamalar yüzünden
        `Extra data` hatası oluşmasını %100 engeller.
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
        Google Generative Language API'de aktif olan temel Flash modellerini döndürür.
        Deneysel/transcribe/image modellerine gereksiz istek atılmasını engeller.
        """
        if self._available_models:
            return self._available_models

        allowed_models = [
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3-flash-preview",
            "gemini-3.1-flash-lite-preview",
        ]
        discovered_set = set()
        try:
            url = f"https://generativelanguage.googleapis.com/v1beta/models?key={api_key}"
            resp = requests.get(url, timeout=15)
            if resp.status_code == 200:
                for m in resp.json().get("models", []):
                    methods = m.get("supportedGenerationMethods", [])
                    if "generateContent" in methods:
                        name = str(m.get("name", "")).replace("models/", "").strip()
                        discovered_set.add(name)
        except Exception as e:
            logger.debug(f"[Gemini-AI] Model listesi sorgulama uyarısı: {e}")

        if discovered_set:
            self._available_models = [m for m in allowed_models if m in discovered_set] or allowed_models[:3]
        else:
            self._available_models = allowed_models[:3]

        logger.info(f"[Gemini-AI] Kullanılabilir aktif Gemini modelleri: {self._available_models}")
        return self._available_models

    def _call_gemini_json(
        self,
        api_key: str,
        parts: List[Dict[str, Any]],
        use_search: bool = True,
    ) -> List[Dict[str, Any]]:
        """
        Gemini REST API'sini hafif token konfigürasyonu ve temperature=0.0 (sıfır tahmin/halüsinasyon)
        ile çağırır; Free Tier'da google_search aracı 429 verirse anında doğrudan analiz moduna (use_search=False) geçer.
        """
        all_models = self._get_available_gemini_models(api_key)
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json",
        }
        effective_search = use_search and not self._search_tool_disabled

        for attempt in range(2):
            active_models = [m for m in all_models if m not in self._exhausted_models]
            if not active_models:
                return []

            for model in active_models:
                endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
                gen_config: Dict[str, Any] = {"temperature": 0.0}
                payload: Dict[str, Any] = {
                    "contents": [{"parts": parts}],
                    "generationConfig": gen_config,
                }
                if effective_search:
                    payload["tools"] = [{"google_search": {}}]

                try:
                    resp = requests.post(endpoint, headers=headers, json=payload, timeout=90)

                    # Free Tier'da google_search aracı kota engeline (HTTP 429) takılırsa anında araçsız (doğrudan resmi veri) modda dene
                    if resp.status_code == 429 and effective_search:
                        logger.info(
                            f"[Gemini-AI] Free Tier google_search kotası ({model}) dolu; doğrudan indirilen resmi takvim verisiyle işleniyor..."
                        )
                        self._search_tool_disabled = True
                        effective_search = False
                        payload.pop("tools", None)
                        resp = requests.post(endpoint, headers=headers, json=payload, timeout=90)

                    if resp.status_code == 402:
                        logger.error(
                            "[Gemini-AI] HTTP 402 (Prepayment credits depleted): API anahtarının bağlı olduğu Google AI Studio "
                            "projesi 'Tier 1 - Prepay (No credits)' modunda olduğu için istekler reddediliyor. "
                            "Ücretsiz günlük kotadan yararlanmak için AI Studio'da 'Free tier' (Ücretsiz Katman) projesine ait bir API anahtarı kullanın."
                        )
                        self._exhausted_models.update(all_models)
                        return []
                    if resp.status_code in (429, 503):
                        body_lower = resp.text.lower()
                        if resp.status_code == 429 and ("perday" in body_lower or "per_day" in body_lower or "daily" in body_lower):
                            self._exhausted_models.add(model)
                            logger.warning(f"[Gemini-AI] Model ({model}) günlük ücretsiz kota sınırına ulaştı, yedek modele geçiliyor...")
                        else:
                            logger.warning(f"[Gemini-AI] Model ({model}) HTTP {resp.status_code}, yedek model deneniyor...")
                        time.sleep(2)
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
                except Exception as e:
                    logger.warning(f"[Gemini-AI] Model ({model}) çağrı uyarısı: {e}")

            if attempt == 0 and len(self._exhausted_models) < len(all_models):
                time.sleep(8)
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

                country, country_name, _ = self.detect_country(f"{title} {raw_city} {raw_cname}", raw_country or "INT", raw_cname or "Uluslararası")
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
