"""
Master Racer - Autonomous AI Discovery Engine (100% Dynamic Gemini AI + Search Grounding + Multimodal PDF)
Hiçbir sabit şehir, sabit yıl, sabit kişi/memorial ismi veya karmaşık kural tablosuna bağımlı kalmadan;
yalnızca Google Gemini API (Google Search Grounding + Multimodal Resmi PDF Analizi + AI Tekilleştirme)
kullanarak Türkiye, Balkanlar, Avrupa ve Dünya genelindeki tüm 35+ Masterlar atletizm
yarışmalarını 4 yıllık dinamik takvim penceresiyle keşfeden ve tekilleştiren otonom yapay zeka motoru.
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
    Yalnızca Google Gemini AI (Multimodal PDF + Google Search Grounding + AI Konsolidasyon/Tekilleştirme)
    kullanarak içinde bulunulan yıl ve takip eden 3 yıldaki (toplam 4 yıllık pencere: ör. 2026, 2027, 2028, 2029)
    tüm 35+ Masterlar (Masters / Veteran / Senioren) atletizm yarışmalarını otonom tarayan motor.
    """

    def __init__(self, existing_comps: Optional[Dict[str, dict]] = None):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://european-masters-athletics.org")
        current_year = datetime.utcnow().year
        self.years = [current_year, current_year + 1, current_year + 2, current_year + 3]
        self.existing_comps: Dict[str, dict] = existing_comps or {}
        self.ids_to_delete: List[str] = []

    def parse(self) -> List[Competition]:
        logger.info(f"Otonom Yapay Zeka (Gemini AI) Tarayıcısı başlatılıyor (Yıllar: {self.years})...")
        discovered = self._discover_with_gemini_ai()
        logger.info(
            f"[AutonomousDiscovery_Parser] Yapay zeka taraması ve tekilleştirmesi tamamlandı. "
            f"Toplam {len(discovered)} benzersiz Master yarışması hazırlandı."
        )
        return discovered

    def _discover_with_gemini_ai(self) -> List[Competition]:
        """
        Google Gemini API kullanarak (ücretsiz katman 5 RPM kota sınırına tam uyumlu aralıklarla):
        1. TAF'ın faaliyet takvimi sayfasındaki resmi PDF dosyasını doğrudan Gemini'ye okutur.
        2. Dünya (WMA/WMG), Avrupa (EMA & EMA League), Balkan (balkanmasters.com) ve
           tüm Avrupa ülkelerinin Ulusal/Bölgesel Masterlar takvimlerini 4 odaklı sorguda 4 yıllık pencere için tarar.
        3. Toplanan tüm ham yarışmaları ve mevcut Firestore kayıtlarını son aşamada Gemini Denetçisine
           vererek Master doğrulamasını, tekilleştirmeyi ve kopya temizliğini doğrudan yapay zekaya yaptırır.
        """
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not api_key:
            logger.warning("[Gemini-AI] GEMINI_API_KEY tanımlı değil! Tarama yapılamadı.")
            return []

        raw_items: List[Dict[str, Any]] = []
        y1, y2, y3, y4 = self.years[0], self.years[1], self.years[2], self.years[3]
        years_all_en = f"{y1}, {y2}, {y3}, and {y4}"
        years_all_tr = f"{y1}, {y2}, {y3} ve {y4}"
        logger.info(f"[Gemini-AI] Yapay zeka destekli derinlemesine keşif motoru başlatılıyor ({years_all_tr})...")

        # -------------------------------------------------------------------------
        # ADIM 1: TAF Resmi Faaliyet Takvimi PDF Analizi (Multimodal - Yaş Grubu Sütunu Bazlı)
        # -------------------------------------------------------------------------
        taf_pdf_urls = self._find_taf_calendar_pdf_urls()
        for pdf_url in taf_pdf_urls[:1]:
            try:
                resp = self.session.get(pdf_url, timeout=25)
                if resp.status_code == 200 and len(resp.content) > 1000:
                    pdf_b64 = base64.b64encode(resp.content).decode("utf-8")
                    prompt = (
                        "Bu Türkiye Atletizm Federasyonu (TAF) resmi faaliyet takvimi PDF belgesini satır satır incele.\n"
                        "KURAL 1: Tablodaki 'Yaş Grubu' sütununu kontrol et. SADECE Yaş Grubu sütununda 'Master' veya 'Masterlar' "
                        "yazan VE Türkiye sınırları içinde (Yurt İçi, country='TR') düzenlenen yarışmaları çıkar. "
                        "(Yurt dışında düzenlenen Balkan, Avrupa ve Dünya şampiyonalarını uluslararası sorgu zaten çektiği için buraya dahil etme).\n"
                        "KURAL 2: Yaş Grubu 'B' (Büyükler), 'U23', 'U20', 'U18', 'U16', 'U14' olan yarışmaları "
                        "(örneğin Büyükler Balkan Şampiyonası, Salon Olimpik Deneme, Olimpik Deneme, TOHM, SEM, "
                        "Okullar, Kulüpler Ligi vb.) KESİNLİKLE DAHİL ETME!\n"
                        "KURAL 3: Eğer Yaş Grubu sütununda 'Master' yazıyor ancak yarışma adında 'Masterlar' kelimesi geçmiyorsa "
                        "(örneğin bir anma/yol koşusu veya kupa ise), `title` alanının sonuna mutlaka '(Masterlar)' ibaresini ekle.\n"
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
        # ADIM 2: 4 Yıllık (2026-2029) Odaklanmış Google Search Grounding Sorguları
        # (Ücretsiz katman 5 İstek/Dakika [5 RPM] sınırına takılmamak için 4 kapsamlı sorgu + 12 sn bekleme)
        # -------------------------------------------------------------------------
        schema_instruction = (
            "\n\nÇIKTI KURALLARI:\n"
            f"1. {years_all_tr} yılları için SADECE 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren / Veterani / Veteranos) "
            "kategorisindeki yarışmaları dahil et. Elit/Büyükler (Senior Elite), Olimpik Deneme, U23, U20, U18 yarışmalarını ASLA dahil etme!\n"
            "2. Yarışmanın ilgili yıldaki resmi adı ve yapılacağı şehir ne ise takvimden doğrudan onu al. "
            "Eğer yarışma başlığında 'Masters / Masterlar / Veteran / Senioren / BMAF / EMA / WMA / BAMACI / BAMACS' ibaresi yoksa "
            "ama yarışma resmi bir Masterlar yarışması ise başlığa '(Masters)' ekle.\n"
            "3. Çok günlü yarışmalarda `start_date` (başlangıç) ve `end_date` (bitiş) tarihlerini tam aralık olarak (YYYY-MM-DD) yaz. "
            "Kesin tarihi veya ülkesi belli olmayan (TBD/TBC veya 2029-01-01 gibi yer tutucu tarihli) yarışmaları KESİNLİKLE DAHİL ETME!\n"
            "4. `city` ve `country` alanlarına gerçek şehir ve 2 harfli ISO ülke kodunu yaz. Asla 'N/A', 'TBD', 'Unknown' yazma.\n"
            "5. `scope` alanı yalnızca şu 6 değerden biri olmalıdır: 'national', 'world', 'balkan', 'europe', 'ema_league', 'regional'.\n"
            "6. Sadece geçerli bir JSON dizisi (array) döndür, markdown veya açıklama metni ekleme:\n"
            '[{"title": "Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "Şehir", "country": "ISO-2 Ülke Kodu (TR, DE, FR, GB, IT, ES, PL, RO vb.)", '
            '"country_name": "Türkçe Ülke Adı", "scope": "national|world|balkan|europe|ema_league|regional", '
            '"federation": "Federasyon Kısaltması", "website_url": "Resmi Web Sitesi URL"}]'
        )

        discovery_queries = [
            (
                f"1. Küresel (WMA/WMG), Avrupa (EMA & EMA League) ve Balkan (balkanmasters.com) Şampiyonaları ({years_all_tr})",
                f"Search the official calendars of World Masters Athletics (world-masters-athletics.org), "
                f"European Masters Athletics (european-masters-athletics.org / data.opentrack.run), and Balkan Masters Athletics "
                f"(https://balkanmasters.com/) for ALL {years_all_en} (including all confirmed {y2}, {y3}, and {y4} future events) "
                f"international Masters (35+) athletics championships, cups, and league meetings:\n"
                f"- European Masters Indoor (EMACI), Stadia/Outdoor (EMACS), Non-Stadia (EMACNS), Marathon/Mountain/Trail/Off-Road (EMORRC, EMMTRC, EMAMRC)\n"
                f"- All European Masters Athletics League (EMA League) indoor and outdoor meetings across Europe\n"
                f"- World Masters Athletics Championships (WMA Indoor, WMA Stadia, WMA Marathon, WMA Road, WMA Mountain/Trail) & World Masters Games (WMG)\n"
                f"- Balkan Masters Athletics Championships on https://balkanmasters.com/ (BAMACI Indoor, BAMACS Stadia, Balkan Masters Marathon/Cross/Road)."
            ),
            (
                f"2. Türkiye, Balkanlar ve Orta/Doğu Avrupa Ulusal & Açık Masterlar ({years_all_tr})",
                f"Search official national federation calendars and https://balkanmasters.com/ for ALL {years_all_en} "
                f"35+ Masters (Masterlar / Veteran / Veteránů / Masters) National Indoor Championships, National Outdoor (Stadia) Championships, "
                f"Cross Country, Throws, Combined Events, Road Running, and Open/Regional Masters competitions in:\n"
                f"- Turkey (TAF - taf.org.tr), Greece (SEGAS/OEBA), Bulgaria (BFLA), Romania (FRA - country='RO')\n"
                f"- Serbia (SAS), Croatia (HAS/HAVU), Slovenia (AZS/ZAVS), Bosnia and Herzegovina, Albania, Montenegro, North Macedonia\n"
                f"- Poland (PZLA/PMA - pma.org.pl), Czechia (ČAS), Slovakia (SAZ), and Hungary (MASZ)."
            ),
            (
                f"3. Birleşik Krallık (BMAF), İrlanda (IMAA), İskandinavya ve Baltık Avrupa ({years_all_tr})",
                f"Search the official British Masters Athletic Federation fixture calendar (bmaf.org.uk/fixtures/ and bmaf.opentrack.run), "
                f"Irish Masters Athletic Association (irishmastersathletics.com), and Nordic/Baltic federations for ALL {years_all_en} scheduled 35+ Masters events:\n"
                f"- UK (BMAF): British Masters Indoor & Outdoor Track & Field Championships, BMAF Indoor Pentathlon & 3000m, Outdoor Combined Events, "
                f"BMAF Winter Throws & Throws Pentathlon, 10,000m, British Masters Cross Country Championships, British Masters Cross Country Relay Championships, "
                f"British Masters Road Relay Championships, British Masters 1 Mile / 5k Road / 10k Road / 10 Mile / Half Marathon / Marathon / Ultra Championships, and Race Walking.\n"
                f"- Ireland (IMAA): Irish Masters Indoor, Outdoor, Cross Country, and Road Championships.\n"
                f"- Nordic & Baltic: Nordic Masters & Baltic Masters Championships, Sweden (ISM & SM Veteraner), Norway (NM Masters), "
                f"Finland (SAUL SM-hallit, SM-kilpailut, SM-maastot, SM-maantie), Denmark (DM Masters), Estonia, Latvia, and Lithuania."
            ),
            (
                f"4. Batı ve Güney Avrupa - FR, DE, NL, BE, LU, CH, AT, IT, ES, PT ({years_all_tr})",
                f"Search official national federation calendars for ALL {years_all_en} 35+ Masters (Senioren / Vétérans / Master / Veteranos) "
                f"Indoor, Outdoor (Stadia), Winter Throws, Combined Events, Race Walk, Cross Country, and Road Running (5k, 10k, Half Marathon, Marathon) Championships in:\n"
                f"- France (FFA - athle.fr/contenu/calendrier-championnats-de-france/32) & Germany (DLV / LADV - leichtathletik.de)\n"
                f"- Netherlands (Atletiekunie - NK Masters), Belgium (VAL/LBFA/KBAB), Luxembourg, Switzerland (Swiss Masters - Part 1 & Part 2, Throws, Pentathlon), Austria (ÖLV)\n"
                f"- Italy (FIDAL - fidal.it), Spain (RFEA - rfea.es), and Portugal (FPAtletismo / ANAV)."
            ),
        ]

        for region_label, query_text in discovery_queries:
            try:
                parts = [{"text": query_text + schema_instruction}]
                items = self._call_gemini_json(api_key, parts, use_search=True)
                if isinstance(items, list):
                    logger.info(f"[Gemini-AI] {region_label} taramasından {len(items)} yarışma keşfedildi.")
                    raw_items.extend(items)
                # Ücretsiz katman 5 RPM (dakikada 5 istek) kotasını aşmamak için 12 saniye bekle
                time.sleep(12)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Bölgesel arama hatası ({region_label}): {e}")

        # -------------------------------------------------------------------------
        # ADIM 3: Gemini AI ile Akıllı Doğrulama, Tekilleştirme ve Mevcut Veritabanı Eşleştirmesi
        # -------------------------------------------------------------------------
        consolidated_items = self._consolidate_with_gemini(api_key, raw_items)
        converted = self._convert_gemini_items(consolidated_items, default_url="https://european-masters-athletics.org")
        self._ensure_duplicate_cleanup(converted)
        return converted

    def _consolidate_with_gemini(self, api_key: str, raw_items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Toplanan tüm ham yarışmaları ve mevcut Firestore veritabanı özetini doğrudan Gemini'ye göndererek:
        1. Master (35+) olmayan yarışmaları ve 'TBD' ülkeli/tarihli taslakları eler.
        2. Farklı sorgulardan veya farklı dillerden gelen tekrarları (kopyaları) birleştirir.
        3. Mevcut veritabanında zaten var olan yarışmaların `id` değerini korur (şehir veya isim değişse bile).
        4. Veritabanında eskiden kalmış kopya veya Master dışı kayıtların ID'lerini `ids_to_delete` olarak belirler
           (ancak geçmişte yapılmış geçerli Master yarışmalarını asla silmez).
        """
        if not raw_items:
            return []

        existing_summary = [
            {
                "id": doc_id,
                "title": d.get("title", ""),
                "start_date": d.get("start_date", ""),
                "end_date": d.get("end_date", ""),
                "city": d.get("city", ""),
                "country": d.get("country", ""),
                "scope": d.get("scope", ""),
            }
            for doc_id, d in self.existing_comps.items()
        ]

        prompt = (
            "Sen Master Racer uygulamasının baş veri denetçisisin. Sana iki JSON listesi veriyorum:\n"
            "1. `mevcut_veritabani`: Firestore veritabanında halihazırda kayıtlı olan yarışmalar.\n"
            "2. `yeni_tarananlar`: Bugün farklı bölge sorgularından toplanan ham yarışma listesi.\n\n"
            "GÖREVLERİN:\n"
            "1. SADECE 35+ YAŞ MASTERLAR VE KESİN VERİ KONTROLÜ: `yeni_tarananlar` veya `mevcut_veritabani` içinde 35+ Masterlar olmayan "
            "(Elit/Büyükler, U14-U23, Salon Olimpik Deneme, TOHM, SEM, Okul) VEYA ülkesi/tarihi belirsiz ('TBD', 'N/A', '2029-01-01') olan yarışmaları kesinlikle çıkar.\n"
            "2. TEKİLLEŞTİRME (DEDUPLICATION): Hem `yeni_tarananlar` içinde hem de `mevcut_veritabani` ile karşılaştırmada aynı yarışma farklı dillerde "
            "(ör. 'Dünya Masterler Pist Şampiyonası' ve 'World Masters Athletics Championships Daegu 2026', 'Avrupa Masterler Salon Şampiyonası' ve 'European Masters Indoor Championships (EMACI)', "
            "'Balkan Masterler Pist Şampiyonası' ve 'Balkan Masters Athletics Championship Stadia (BAMACS)', 'Masterlar Salon Türkiye Şampiyonası' ve 'Alkan Öral Türkiye Masterlar Salon Şampiyonası') "
            "veya farklı şehir yazılışlarıyla ('Selanik' ve 'Thessaloniki', 'Bükreş' ve 'Bucharest') birden fazla kez yer alıyorsa bunları TEK BİR yarışma kaydında birleştir! "
            "Çok günlü tarih aralığı (end_date > start_date) olanı ve daha detaylı şehir bilgisini koru.\n"
            "3. MEVCUT ID EŞLEŞTİRME: Bir yarışma `mevcut_veritabani` içindeki bir yarışmayla aynı şampiyona ise (tarihi, şehri veya başlığı güncellenmiş olsa bile), "
            "`mevcut_veritabani` içindeki `id` değerini AYNEN koru! Yalnızca veritabanında hiç olmayan yeni bir yarışma ise `ülkekodu-yıl-kısa-ad` formatında benzersiz küçük harf bir `id` üret.\n"
            "4. KOPYA VE HATALI KAYIT TEMİZLİĞİ (`ids_to_delete`): `mevcut_veritabani` içinde aynı yarışmanın iki farklı ID ile kopyası varsa (fazlalık olan kopya ID'lerin tamamını), "
            "ülke kodu hatalı olanları (ör. Romanya yarışması olup `it-` ile başlayanları veya `tbd-` ile başlayanları) veya Master olmayan kayıtları `ids_to_delete` listesine ekle. "
            "ÇOK ÖNEMLİ: Geçmiş tarihte yapılmış veya önceki yıllara (ör. 2026) ait TEKİL ve geçerli Master yarışmalarını kullanıcının kendi derecelerini takip edebilmesi için ASLA `ids_to_delete` listesine ekleme!\n\n"
            f"mevcut_veritabani:\n{json.dumps(existing_summary, ensure_ascii=False)}\n\n"
            f"yeni_tarananlar:\n{json.dumps(raw_items, ensure_ascii=False)}\n\n"
            "Sadece aşağıdaki JSON nesnesi formatında yanıt döndür, başka hiçbir açıklama yazma:\n"
            '{"competitions": [{"id": "...", "title": "...", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "...", "country": "ISO-2", "country_name": "Türkçe Ülke Adı", '
            '"scope": "national|world|balkan|europe|ema_league|regional", "federation": "...", "website_url": "..."}], '
            '"ids_to_delete": ["silinecek_kopya_veya_gecersiz_id_1"]}'
        )

        logger.info(f"[Gemini-AI] {len(raw_items)} ham yarışma Gemini AI ile doğrulanıyor ve tekilleştiriliyor...")
        parsed = self._call_gemini_json(api_key, [{"text": prompt}], use_search=False, expect_object=True)
        if isinstance(parsed, dict):
            comps = parsed.get("competitions", [])
            to_del = parsed.get("ids_to_delete", [])
            if isinstance(to_del, list):
                self.ids_to_delete = [str(x).strip() for x in to_del if x]
            if isinstance(comps, list) and len(comps) > 0:
                logger.info(
                    f"[Gemini-AI] AI Tekilleştirme tamamlandı: {len(comps)} benzersiz yarışma, "
                    f"{len(self.ids_to_delete)} silinecek kopya/geçersiz kayıt belirlendi."
                )
                return comps

        logger.warning("[Gemini-AI] AI konsolidasyon yanıtı alınamadı, mevcut ID eşleştirmeli yedek tekilleştirme uygulanıyor.")
        return raw_items

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

        # Liste (array) bekleniyorsa: metindeki tüm geçerli JSON listelerini veya nesnelerini topla
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

        # Eğer köşeli parantez kesilmişse metin içindeki tekil `{...}` nesnelerini kurtar
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
        Gemini REST API'sini ücretsiz katman kotası yüksek olan modellerle (gemini-2.5-flash)
        çağırır; HTTP 429/503 durumunda otomatik bekleme ve yeniden deneme (retry) uygular.
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
                    "temperature": 0.1,
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
        """Gemini JSON çıktılarını doğrular, mevcut veritabanı ID'leriyle eşleştirir ve Competition nesnelerine dönüştürür."""
        converted: List[Competition] = []
        seen_ids = set()
        seen_date_country_title = set()
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

                # Gelecek yıllarda 1 Ocak (YYYY-01-01) yer tutucu tarihli veya ülkesi TBD olan belirsiz kayıtları reddet
                if start_date.endswith("-01-01") and not raw_city:
                    continue

                country, country_name, _ = self.detect_country(f"{title} {raw_city} {raw_cname}", raw_country or "INT", raw_cname or "Uluslararası")
                if country.upper() in {"INT", "TBD", "TBC", "NA"} and not raw_city:
                    continue

                city = raw_city or country_name

                scope = str(item.get("scope", "")).strip().lower()
                if scope not in valid_scopes:
                    scope = self.detect_scope(title, country)

                federation = str(item.get("federation", "")).strip() or "EMA / Ulusal"
                web_url = str(item.get("website_url", "")).strip() or default_url

                raw_id = str(item.get("id", "")).strip()
                comp_id = normalize_text(raw_id) if raw_id else ""

                # Mevcut veritabanında aynı yarışma varsa (en kısa/kanonik mevcut ID öncelikli) onun ID'sini koru
                if not comp_id or comp_id not in self.existing_comps:
                    for ex_id in sorted(self.existing_comps.keys(), key=len):
                        if ex_id in seen_ids or ex_id.startswith("tbd-"):
                            continue
                        ex_data = self.existing_comps[ex_id]
                        ex_start = str(ex_data.get("start_date", ""))[:10]
                        ex_city_slug = normalize_text(ex_data.get("city", ""))[:6]
                        in_city_slug = normalize_text(city)[:6]
                        if ex_data.get("country") == country and (
                            ex_start == start_date
                            or (ex_start[:7] == start_date[:7] and ex_data.get("scope") == scope and scope in {"world", "europe", "balkan"})
                            or (ex_start[:7] == start_date[:7] and ex_city_slug and ex_city_slug == in_city_slug and ex_data.get("scope") == scope)
                        ):
                            comp_id = ex_id
                            break

                if not comp_id:
                    comp_id = generate_competition_id(title, start_date, country, city)

                sig = f"{country}|{start_date}|{normalize_text(title)[:22]}"
                if not comp_id or comp_id in seen_ids or sig in seen_date_country_title:
                    continue
                seen_ids.add(comp_id)
                seen_date_country_title.add(sig)

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

    def _ensure_duplicate_cleanup(self, active_comps: List[Competition]) -> None:
        """
        Veritabanında önceki çalışmalardan kalmış `tbd-` uzantılı, `it-...-bukres` gibi hatalı ülke kodlu
        veya aynı ülke + aynı ay/şehir içinde birden fazla kez yazılmış ikiz kayıtların silinmesini garanti altına alır.
        Geçmiş yıllarda (ör. 2026) yapılmış tekil ve geçerli Master yarışmalarına ASLA dokunmaz.
        """
        active_ids = {c.id for c in active_comps}
        del_set = set(self.ids_to_delete)

        combined_pool: Dict[str, Dict[str, Any]] = dict(self.existing_comps)
        for c in active_comps:
            combined_pool[c.id] = c.model_dump()

        # 1. Bozuk ('tbd-', '-na', Romanya olup 'it-' ile başlayan, veya Elit Balkan Salon) kayıtları işaretle
        for ex_id, ex_data in self.existing_comps.items():
            ex_title = str(ex_data.get("title", "")).lower()
            ex_city = str(ex_data.get("city", "")).lower()
            ex_country = str(ex_data.get("country", "")).upper()
            if (
                ex_id.startswith("tbd-")
                or ex_id.endswith("-na")
                or ex_country in {"TBD", "TBC", "NA", "INT"}
                or (ex_id.startswith("it-") and any(w in ex_title or w in ex_city for w in ["romania", "bucharest", "bükreş", "bukres"]))
                or (ex_id == "rs-2027-02-20-belgrad")
            ):
                del_set.add(ex_id)

        # 2. Aynı ülke + aynı ay (YYYY-MM) + aynı kapsam (veya aynı şehir) içinde birden fazla ikiz kayıt varsa:
        by_country_month_scope: Dict[str, List[str]] = {}
        for rec_id, rec_data in combined_pool.items():
            if rec_id in del_set:
                continue
            c_code = str(rec_data.get("country", "")).upper()
            s_month = str(rec_data.get("start_date", ""))[:7]
            scope = str(rec_data.get("scope", "national")).lower()
            c_city = normalize_text(rec_data.get("city", ""))[:8]
            if scope in {"world", "europe", "balkan", "ema_league"}:
                group_key = f"{c_code}|{s_month}|{scope}"
            else:
                group_key = f"{c_code}|{s_month}|{c_city}|{scope}"
            by_country_month_scope.setdefault(group_key, []).append(rec_id)

        for _, group_ids in by_country_month_scope.items():
            if len(group_ids) > 1:
                group_ids_sorted = sorted(
                    group_ids,
                    key=lambda gid: (
                        gid in active_ids,
                        -len(gid),
                        str(combined_pool[gid].get("end_date", "")) > str(combined_pool[gid].get("start_date", "")),
                        len(str(combined_pool[gid].get("city", ""))),
                    ),
                    reverse=True,
                )
                winner_id = group_ids_sorted[0]
                for dup_id in group_ids_sorted[1:]:
                    if dup_id != winner_id:
                        del_set.add(dup_id)

        active_comps[:] = [c for c in active_comps if c.id not in del_set]
        self.ids_to_delete = [did for did in del_set if did in self.existing_comps and did not in {c.id for c in active_comps}]
