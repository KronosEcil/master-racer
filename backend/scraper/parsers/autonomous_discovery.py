"""
Master Racer - Official Federation Calendar AI Discovery Engine
Gemini'yi rastgele Google aramasına bırakmak yerine; her uluslararası birliğin (WMA, EMA, BAMA)
ve her ülkenin resmi ulusal atletizm/masterlar federasyonunun resmi takvim sitelerini
doğrudan indirip ve sadece o resmi alan adlarını (`site:`) hedefleyerek sıfır halüsinasyonla
(temperature=0.0) tarayan otonom yapay zeka motoru.
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
    normalize_text,
    generate_competition_id,
    compute_content_hash,
)

logger = logging.getLogger(__name__)


class AutonomousDiscoveryParser(BaseParser):
    """
    Her ülkenin ve uluslararası federasyonun resmi takvim web sitelerini ayrı ayrı tanımlayarak,
    önce resmi sayfa içeriğini doğrudan çeken ve Gemini AI'ya yalnızca bu resmi kaynakları
    analiz ettiren sıfır-halüsinasyonlu keşif motoru.
    """

    def __init__(self, existing_comps: Optional[Dict[str, dict]] = None):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://european-masters-athletics.org")
        current_year = datetime.utcnow().year
        self.years = [current_year, current_year + 1, current_year + 2, current_year + 3]
        self.existing_comps: Dict[str, dict] = existing_comps or {}
        self.ids_to_delete: List[str] = []

    def parse(self) -> List[Competition]:
        logger.info(f"Resmi Federasyon Takvimleri Yapay Zeka Tarayıcısı başlatılıyor (Yıllar: {self.years})...")
        discovered = self._discover_with_gemini_ai()
        logger.info(
            f"[AutonomousDiscovery_Parser] Resmi site taraması ve tekilleştirmesi tamamlandı. "
            f"Toplam {len(discovered)} doğrulanmış Master yarışması hazırlandı."
        )
        return discovered

    def _fetch_official_page_snapshot(self, urls: List[str], max_chars_per_url: int = 8000) -> str:
        """
        Verilen resmi federasyon takvim URL'lerine doğrudan bağlanır, tablo/liste metinlerini
        veya JSON verisini indirir ve Gemini'nin doğrudan okuması için temiz metin olarak birleştirir.
        """
        snapshots: List[str] = []
        for url in urls:
            try:
                resp = self.session.get(url, timeout=15)
                if resp.status_code != 200 or not resp.text:
                    continue
                content_type = resp.headers.get("Content-Type", "").lower()
                if "json" in content_type or url.endswith("/events"):
                    text_clean = resp.text[:max_chars_per_url]
                    snapshots.append(f"=== RESMİ KAYNAK VERİSİ ({url}) ===\n{text_clean}")
                else:
                    soup = BeautifulSoup(resp.text, "html.parser")
                    for tag in soup(["script", "style", "nav", "footer", "svg", "noscript"]):
                        tag.decompose()
                    lines = [
                        re.sub(r"\s+", " ", line).strip()
                        for line in soup.get_text(separator="\n").splitlines()
                    ]
                    lines = [ln for ln in lines if len(ln) > 3]
                    page_text = "\n".join(lines)[:max_chars_per_url]
                    if page_text:
                        snapshots.append(f"=== RESMİ TAKVİM SAYFASI ({url}) ===\n{page_text}")
            except Exception as e:
                logger.debug(f"[Resmi-Site-Önizleme] {url} okunamadı: {e}")
        return "\n\n".join(snapshots)

    def _discover_with_gemini_ai(self) -> List[Competition]:
        """
        Her bölgenin ve ülkenin resmi ulusal takvim sitelerini ayrı ayrı hedefleyerek:
        1. Türkiye (TAF) resmi faaliyet takvimi PDF'ini doğrudan okur.
        2. Uluslararası (WMA, EMA, BAMA) ve Ulusal Federasyonların resmi takvim sitelerini
           doğrudan indirir + sadece o resmi siteler (`site:...`) içinde Gemini ile araştırır.
        3. Son adımda tüm listeyi Gemini ile tekilleştirir.
        """
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not api_key:
            logger.warning("[Gemini-AI] GEMINI_API_KEY tanımlı değil! Tarama yapılamadı.")
            return []

        raw_items: List[Dict[str, Any]] = []
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
                        "Bu Türkiye Atletizm Federasyonu (TAF - https://taf.org.tr) resmi faaliyet takvimi PDF belgesini satır satır incele.\n"
                        "KURAL 1: Tablodaki 'Yaş Grubu' sütununu kontrol et. SADECE Yaş Grubu sütununda 'Master' veya 'Masterlar' "
                        "yazan VE Türkiye sınırları içinde (Yurt İçi, country='TR') düzenlenen yarışmaları çıkar.\n"
                        "KURAL 2: Yaş Grubu 'B' (Büyükler), 'U23', 'U20', 'U18', 'U16', 'U14' olan yarışmaları "
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
                    if isinstance(items, list):
                        logger.info(f"[Gemini-AI] TAF Resmi PDF takviminden {len(items)} Master yarışması çıkarıldı.")
                        raw_items.extend(items)
                    time.sleep(12)
            except Exception as e:
                logger.warning(f"[Gemini-AI] TAF PDF analizi hatası ({pdf_url}): {e}")

        # -------------------------------------------------------------------------
        # ADIM 2: Resmi Federasyon Takvim Siteleri Listesi (Her Ülke İçin Resmi Kaynak)
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

        official_federation_batches = [
            {
                "label": f"1. Uluslararası Resmi Takvimler: WMA, EMA, OpenTrack ve Balkan Masters ({years_all_tr})",
                "urls": [
                    "https://s831118207.onlinehome.us/ema_cham_aw/api/v1/events",
                    "https://european-masters-athletics.org/championships/",
                    "https://balkanmasters.com/",
                    "https://world-masters-athletics.org/championships/",
                ],
                "query": (
                    f"Use ONLY the official calendars of:\n"
                    f"- European Masters Athletics: https://european-masters-athletics.org/championships/ and https://data.opentrack.run/\n"
                    f"- Balkan Masters Athletics: https://balkanmasters.com/\n"
                    f"- World Masters Athletics: https://world-masters-athletics.org/championships/ and https://wmg2027.jp/\n"
                    f"Extract all confirmed {years_all_en} European Masters Championships (EMACI, EMACS, EMACNS, EMMTRC, EMAMRC, EMORRC), "
                    f"EMA League meetings, Balkan Masters Championships (BAMACI, BAMACS, BAMAM), World Masters Championships (WMA), "
                    f"and World Masters Games (WMG) with their exact official dates and host cities."
                ),
            },
            {
                "label": f"2. Birleşik Krallık (BMAF) ve İrlanda (IMAA) Resmi Takvim Siteleri ({years_all_tr})",
                "urls": [
                    "https://bmaf.org.uk/fixtures/",
                    "https://irishmastersathletics.com/",
                ],
                "query": (
                    f"Search ONLY the official UK and Ireland Masters athletics fixture calendars:\n"
                    f"- United Kingdom (BMAF): site:bmaf.org.uk (https://bmaf.org.uk/fixtures/) and site:bmaf.opentrack.run\n"
                    f"- Ireland (IMAA): site:irishmastersathletics.com (https://irishmastersathletics.com/) and site:athleticsireland.ie\n"
                    f"Extract all confirmed {years_all_en} BMAF / British Masters and Irish Masters (IMAA) championships and open fixtures, including:\n"
                    f"Indoor & Outdoor Track & Field Championships, Indoor Pentathlon & 3000m, Winter Throws & Throws Pentathlon, 10,000m, "
                    f"Cross Country Championships, Cross Country Relay Championships, Road Relay Championships, "
                    f"1 Mile, 5k Road, 10k Road, 10 Mile, Half Marathon, Marathon, Ultra, and Race Walking Championships."
                ),
            },
            {
                "label": f"3. Fransa (FFA), Almanya (DLV/LADV), İsviçre, Avusturya ve Benelüks Resmi Siteleri ({years_all_tr})",
                "urls": [
                    "https://www.athle.fr/contenu/calendrier-championnats-de-france/32",
                    "https://www.swiss-masters.ch/",
                    "https://www.oelv.at/de/wettkaempfe/kalender",
                ],
                "query": (
                    f"Search ONLY the official national federation calendars for {years_all_en} 35+ Masters (Vétérans / Senioren / Masters) championships:\n"
                    f"- France (FFA): https://www.athle.fr/contenu/calendrier-championnats-de-france/32 (site:athle.fr)\n"
                    f"- Germany (DLV / LADV): https://www.leichtathletik.de/wettkaempfe/termine and https://ladv.de/ausschreibungen (site:leichtathletik.de OR site:ladv.de)\n"
                    f"- Switzerland (Swiss Masters): https://www.swiss-masters.ch/ and https://www.swiss-athletics.ch/ (site:swiss-masters.ch OR site:swiss-athletics.ch)\n"
                    f"- Austria (ÖLV): https://www.oelv.at/de/wettkaempfe/kalender (site:oelv.at)\n"
                    f"- Netherlands (Atletiekunie): https://www.atletiek.nl/wedstrijden/ (site:atletiek.nl OR site:atletiekunie.nl)\n"
                    f"- Belgium (VAL / LBFA): https://www.atletiek.be/wedstrijden/kalender and https://www.lbfa.be/fr/calendrier (site:atletiek.be OR site:lbfa.be)"
                ),
            },
            {
                "label": f"4. İtalya (FIDAL), İspanya (RFEA) ve Portekiz (FPAtletismo/ANAV) Resmi Siteleri ({years_all_tr})",
                "urls": [
                    "https://www.fidal.it/calendario.php",
                    "https://www.anavportugal.com/",
                ],
                "query": (
                    f"Search ONLY the official national federation calendars for {years_all_en} 35+ Masters (Master / Veteranos) championships:\n"
                    f"- Italy (FIDAL): https://www.fidal.it/calendario.php and https://www.fidal.it/categoria/Master/25 (site:fidal.it)\n"
                    f"- Spain (RFEA): https://atletismorfea.es/calendario and https://www.rfeacontent.es/calendario/ (site:atletismorfea.es OR site:rfeacontent.es OR site:rfea.es)\n"
                    f"- Portugal (FPAtletismo / ANAV): https://fpatletismo.pt/atletismo/calendario-nacional/ and https://www.anavportugal.com/ (site:fpatletismo.pt OR site:anavportugal.com)"
                ),
            },
            {
                "label": f"5. Balkanlar, Orta/Doğu Avrupa, İskandinavya ve Baltık Resmi Federasyon Siteleri ({years_all_tr})",
                "urls": [
                    "https://balkanmasters.com/",
                    "https://www.pma.org.pl/",
                    "https://www.saul.fi/kilpailut/kilpailukalenteri/",
                ],
                "query": (
                    f"Search ONLY the official national federation calendars for {years_all_en} 35+ Masters (Veteránů / Veteraner / SAUL / Masters) championships:\n"
                    f"- Balkans: https://balkanmasters.com/ , Greece (https://www.segas.gr/ , https://www.oeba.gr/), Bulgaria (https://www.bfla.org/), "
                    f"Romania (https://www.fra.ro/ , https://atletismveterani.ro/ - country='RO'), Serbia (https://sas.org.rs/), Croatia (https://www.has.hr/ , https://www.havu.hr/), Slovenia (https://slovenska-atletika.si/ , https://zavs.si/)\n"
                    f"- Central Europe: Poland (https://www.pma.org.pl/ , https://pzla.pl/), Czechia (https://www.atletika.cz/ , https://www.veteraniatletika.cz/), Slovakia (https://www.atletika.sk/), Hungary (https://atletika.hu/)\n"
                    f"- Nordic & Baltic: Finland (https://www.saul.fi/kilpailut/kilpailukalenteri/), Sweden (https://www.friidrott.se/), Norway (https://www.friidrett.no/), Denmark (https://dansk-atletik.dk/), Estonia (https://ekjl.ee/), Latvia (https://athletics.lv/), Lithuania (https://lengvoji.lt/)"
                ),
            },
        ]

        for batch in official_federation_batches:
            label = batch["label"]
            urls = batch["urls"]
            query_text = batch["query"]
            try:
                snapshot_text = self._fetch_official_page_snapshot(urls)
                full_prompt = query_text
                if snapshot_text:
                    full_prompt += (
                        "\n\nAŞAĞIDA BU RESMİ SİTELERDEN DOĞRUDAN İNDİRİLEN GÜNCEL SAYFA/API İÇERİĞİ YER ALMAKTADIR "
                        "(Öncelikle bu resmi veriyi kullan ve aynı resmi sitelerin alt sayfalarını kontrol et):\n"
                        f"{snapshot_text}\n"
                    )
                full_prompt += schema_instruction

                parts = [{"text": full_prompt}]
                items = self._call_gemini_json(api_key, parts, use_search=True)
                if isinstance(items, list):
                    logger.info(f"[Gemini-AI] {label} taramasından {len(items)} yarışma çıkarıldı.")
                    raw_items.extend(items)
                time.sleep(12)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Resmi site tarama hatası ({label}): {e}")

        # -------------------------------------------------------------------------
        # ADIM 3: Gemini AI ile Doğrulama ve Tekilleştirme
        # -------------------------------------------------------------------------
        consolidated_items = self._consolidate_with_gemini(api_key, raw_items)
        return self._convert_gemini_items(consolidated_items, default_url="https://european-masters-athletics.org")

    def _consolidate_with_gemini(self, api_key: str, raw_items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Resmi sitelerden toplanan yarışma listesini doğrudan Gemini'ye göndererek:
        1. Master (35+) olmayan veya tarihi/ülkesi belirsiz ('TBD') olanları eler.
        2. Farklı kaynaklarda (ör. hem EMA hem ulusal federasyon sitesinde) iki kez geçen aynı yarışmaları
           tek bir kayıtta birleştirir.
        """
        if not raw_items:
            return []

        prompt = (
            "Sen Master Racer uygulamasının baş veri denetçisisin. Aşağıda resmi federasyon sitelerinden toplanan "
            "ham yarışma listesi (`yeni_tarananlar`) yer almaktadır.\n\n"
            "GÖREVLERİN:\n"
            "1. SADECE 35+ YAŞ MASTERLAR VE KESİN VERİ KONTROLÜ: Liste içinde 35+ Masterlar olmayan "
            "(Elit/Büyükler, U14-U23, Salon Olimpik Deneme, TOHM, SEM, Okul) VEYA ülkesi/tarihi belirsiz ('TBD', 'N/A', '2029-01-01') olan yarışmaları kesinlikle çıkar.\n"
            "2. TEKİLLEŞTİRME (DEDUPLICATION): Aynı yarışma listede iki kez yer alıyorsa (ör. hem uluslararası takvimde hem ulusal takvimde, "
            "veya farklı dil/şehir yazılışlarıyla) bunları TEK BİR yarışma kaydında birleştir! "
            "Çok günlü tarih aralığı (end_date > start_date) olanı ve daha detaylı şehir/stadyum bilgisini koru.\n"
            "3. STANDART ID ATAMASI: Her yarışma için `ülkekodu-başlangıçtarihi-kısa-ad` (ör. `gb-2026-06-14-british-masters-5k-road`, `tr-2026-01-31-alkan-oral-turkiye-masterlar-salon`) "
            "formatında deterministik, küçük harfli benzersiz bir `id` oluştur.\n\n"
            f"yeni_tarananlar:\n{json.dumps(raw_items, ensure_ascii=False)}\n\n"
            "Sadece geçerli bir JSON dizisi (array) döndür, başka hiçbir açıklama yazma:\n"
            '[{"id": "...", "title": "...", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "...", "country": "ISO-2", "country_name": "Türkçe Ülke Adı", '
            '"scope": "national|world|balkan|europe|ema_league|regional", "federation": "...", "website_url": "..."}]'
        )

        logger.info(f"[Gemini-AI] {len(raw_items)} resmi yarışma kaydı Gemini AI ile doğrulanıyor ve tekilleştiriliyor...")
        parsed = self._call_gemini_json(api_key, [{"text": prompt}], use_search=False, expect_object=False)
        if isinstance(parsed, list) and len(parsed) > 0:
            logger.info(f"[Gemini-AI] AI Tekilleştirme tamamlandı: {len(parsed)} benzersiz yarışma doğrulandı.")
            return parsed

        logger.warning("[Gemini-AI] AI konsolidasyon yanıtı alınamadı, ham liste kullanılıyor.")
        return raw_items

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
    def _extract_json_from_text(raw_text: str, expect_object: bool = False) -> Any:
        """
        Gemini yanıtındaki JSON dizisini veya nesnesini `raw_decode` kullanarak ayrıştırır.
        Yanıtın sonundaki Google Search atıfları (`[1]`, `[2]`) veya ek açıklamalar yüzünden
        `Extra data` hatası oluşmasını %100 engeller.
        """
        cleaned = re.sub(r"```(?:json)?", "", raw_text or "").strip("` \n\r\t")
        decoder = json.JSONDecoder()

        if expect_object:
            start_idx = cleaned.find("{")
            while start_idx != -1:
                try:
                    obj, _ = decoder.raw_decode(cleaned[start_idx:])
                    if isinstance(obj, dict):
                        return obj
                except Exception:
                    pass
                start_idx = cleaned.find("{", start_idx + 1)
            return {}

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

    def _call_gemini_json(
        self,
        api_key: str,
        parts: List[Dict[str, Any]],
        use_search: bool = True,
        expect_object: bool = False,
    ) -> Any:
        """
        Gemini REST API'sini temperature=0.0 (sıfır tahmin/halüsinasyon) ile çağırır;
        HTTP 429/503 durumunda otomatik bekleme ve yeniden deneme (retry) uygular.
        """
        models = ["gemini-2.5-flash", "gemini-2.5-flash-lite"]
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json",
        }
        for attempt in range(2):
            for model in models:
                endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
                gen_config: Dict[str, Any] = {
                    "temperature": 0.0,
                    "maxOutputTokens": 65536,
                }
                if "2.5" in model:
                    gen_config["thinkingConfig"] = {"thinkingBudget": 0}
                payload: Dict[str, Any] = {
                    "contents": [{"parts": parts}],
                    "generationConfig": gen_config,
                }
                if use_search:
                    payload["tools"] = [{"google_search": {}}]

                try:
                    resp = requests.post(endpoint, headers=headers, json=payload, timeout=120)
                    if resp.status_code in (429, 503):
                        wait_sec = 15 * (attempt + 1)
                        logger.warning(f"[Gemini-AI] Model ({model}) HTTP {resp.status_code}, {wait_sec} sn bekleniyor...")
                        time.sleep(wait_sec)
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
                    parsed = self._extract_json_from_text(raw_text, expect_object=expect_object)
                    if parsed:
                        return parsed
                except Exception as e:
                    logger.warning(f"[Gemini-AI] Model ({model}) çağrı uyarısı: {e}")
        return {} if expect_object else []

    def _convert_gemini_items(self, items: List[Dict[str, Any]], default_url: str) -> List[Competition]:
        """Gemini JSON çıktılarını doğrular ve Competition nesnelerine dönüştürür."""
        converted: List[Competition] = []
        seen_ids = set()
        seen_signatures = set()
        valid_scopes = {"national", "world", "balkan", "europe", "ema_league", "regional"}
        min_year = self.years[0]

        for item in items:
            try:
                if not isinstance(item, dict):
                    continue
                title = str(item.get("title", "")).strip()
                start_date = str(item.get("start_date", "")).strip()[:10]
                end_date = str(item.get("end_date", start_date)).strip()[:10] or start_date
                if not title or not re.match(r"^20\d{2}-\d{2}-\d{2}$", start_date):
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

                # Yer tutucu 1 Ocak tarihli veya ülkesi belirsiz kayıtları kesinlikle reddet
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
                sig = f"{country}|{start_date}|{normalize_text(title)[:22]}"
                if not comp_id or comp_id in seen_ids or sig in seen_signatures:
                    continue
                seen_ids.add(comp_id)
                seen_signatures.add(sig)

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
