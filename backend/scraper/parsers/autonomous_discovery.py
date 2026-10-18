"""
Master Racer - Autonomous AI Discovery Engine (100% Gemini AI + Search Grounding + Multimodal PDF)
Statik HTML/CSS seçicilere veya kırılgan federasyon URL'lerine bağımlı kalmadan;
yalnızca Google Gemini API (Google Search Grounding + Multimodal Resmi PDF Analizi)
kullanarak Türkiye, Balkanlar, Avrupa ve Dünya genelindeki tüm 35+ Masterlar atletizm
yarışmalarını derinlemesine keşfeden tek katmanlı otonom yapay zeka motoru.
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
    2026, 2027 ve 2028 yıllarındaki 35+ Masterlar (Masters / Veteran / Senioren)
    atletizm yarışmalarını otonom olarak tarayan ve standartlaştıran motor.
    """

    def __init__(self):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://european-masters-athletics.org")
        self.years = [2026, 2027, 2028]

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
        1. TAF'ın faaliyet takvimi sayfasındaki resmi PDF dosyasını doğrudan Gemini'ye okutur.
        2. Dünya (WMA/WMG), Avrupa (EMA Şampiyonaları ve EMA League) ve Balkan (BAMACI/BAMACS)
           ana şampiyona takvimlerini tarar.
        3. Tüm Avrupa ülkelerinin Ulusal Salon, Açık Saha, Atmalar, Çoklu Branşlar, Kros, Bayrak (Relays)
           ve Yol (5k, 10k, Yarı Maraton, Maraton, Yürüyüş) Masterlar Şampiyonalarını odaklanmış
           bölge sorgularıyla eksiksiz keşfeder.
        """
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not api_key:
            logger.warning("[Gemini-AI] GEMINI_API_KEY tanımlı değil! Tarama yapılamadı.")
            return []

        results: List[Competition] = []
        logger.info("[Gemini-AI] Yapay zeka destekli derinlemesine keşif motoru başlatılıyor...")

        # -------------------------------------------------------------------------
        # ADIM 1: TAF Resmi Faaliyet Takvimi PDF Analizi (Multimodal)
        # -------------------------------------------------------------------------
        taf_pdf_urls = self._find_taf_calendar_pdf_urls()
        for pdf_url in taf_pdf_urls[:1]:
            try:
                resp = self.session.get(pdf_url, timeout=25)
                if resp.status_code == 200 and len(resp.content) > 1000:
                    pdf_b64 = base64.b64encode(resp.content).decode("utf-8")
                    prompt = (
                        "Bu Türkiye Atletizm Federasyonu (TAF) resmi faaliyet takvimi PDF belgesini satır satır incele.\n"
                        "KURAL 1: SADECE 'Yaş Grubu' sütununda 'Master' veya 'Masterlar' yazan VEYA yarışma adında açıkça "
                        "'Master', 'Masterlar', 'Masterler', 'BAMACI', 'BAMACS', 'EMACI', 'EMACS', 'WMA' geçen "
                        "35+ yaş Masterlar yarışmalarını çıkar.\n"
                        "KURAL 2: Yaş Grubu 'B' (Büyükler), 'U23', 'U20', 'U18', 'U16', 'U14' olan yarışmaları "
                        "(örneğin Büyükler Balkan Salon Şampiyonası, Salon Olimpik Deneme, Olimpik Deneme, TOHM, SEM, "
                        "Okullar, Kulüpler Süper Lig vb.) KESİNLİKLE DAHİL ETME!\n"
                        "KURAL 3: Yarışma adında 'Masterlar' kelimesi eksikse ama Yaş Grubu 'Master' ise başlığa mutlaka "
                        "'Masterlar' ibaresini ekle (Örn: 'Balkan Masterlar Salon Atletizm Şampiyonası (BAMACI)').\n"
                        "Sadece geçerli bir JSON dizisi (array) döndür, başka hiçbir açıklama yazma:\n"
                        '[{"title": "Resmi Yarışma Adı (Masterlar)", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
                        '"city": "Şehir", "country": "ISO-2 Ülke Kodu (TR, SI, RO, PL vb.)", '
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
        # ADIM 2: Odaklanmış ve Güçlendirilmiş Google Search Grounding Sorguları
        # -------------------------------------------------------------------------
        schema_instruction = (
            "\n\nÇIKTI KURALLARI:\n"
            "1. SADECE 35+ yaş Masterlar (Masters / Veteran / Vétérans / Senioren / Veterani / Veteranos) "
            "atletizm yarışmalarını dahil et. Elit/Büyükler (Senior Elite), Olimpik Deneme, U23, U20, U18 yarışmalarını ASLA dahil etme!\n"
            "2. Her yarışmanın `title` (başlık) alanında mutlaka 'Masters', 'Masterlar', 'Master', 'Veteran', 'Senioren' veya "
            "resmi kısaltması ('EMACI', 'EMACS', 'BAMACI', 'BAMACS', 'WMA', 'WMG', 'BMAF', 'EMA League') yer almalıdır.\n"
            "3. Çok günlü yarışmalarda `start_date` (başlangıç) ve `end_date` (bitiş) tarihlerini tam aralık olarak (YYYY-MM-DD) yaz. "
            "Kesin tarihi belli olmayan (TBD/TBC) yarışmaları dahil etme.\n"
            "4. `city` alanına yalnızca gerçek şehir adını (varsa parantez içinde stadyum/salon adını, örn: 'Bursa (Osmangazi Atletizm Salonu)' "
            "veya 'Toruń') yaz. Asla 'N/A', 'TBD', 'Unknown' yazma.\n"
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
                "Dünya (WMA/WMG), Avrupa (EMA & EMA League) ve Balkan (BAMACI/BAMACS) Şampiyonaları",
                "Search official calendars of World Masters Athletics (world-masters-athletics.org), "
                "European Masters Athletics (european-masters-athletics.org / data.opentrack.run), and Balkan Masters Athletics "
                "for ALL 2026, 2027, and 2028 international Masters athletics championships and cups. "
                "Specifically include:\n"
                "- European Masters Indoor Championships (EMACI 2026 Toruń, EMACI 2028 Glasgow)\n"
                "- European Masters Stadia / Outdoor Championships (EMACS 2027 Jyväskylä)\n"
                "- European Masters Non-Stadia Championships (EMACNS 2026, EMACNS 2028 Galway)\n"
                "- European Masters Marathon, Off-Road, Trail & Mountain Running Championships (EMORRC 2026 Porto Santo, EMMTRC 2027 Ojós, EMAMRC 2027 Lucerne)\n"
                "- All EMA League Meetings (Meeting Européen Masters en Salle Nantes 2027, Catania 2026, Speed Masters Thessaloniki 2026, Christina Ziemann Trophy, etc.)\n"
                "- World Masters Athletics Championships (WMA Stadia Daegu 2026, WMA Marathon Zagreb 2026, WMA Indoor/Road/Mountain 2026-2028) & World Masters Games (WMG Kansai 2027)\n"
                "- Balkan Masters Indoor Championships (BAMACI 2026 Novo Mesto, BAMACI 2027) and Balkan Masters Stadia Championships (BAMACS 2026 Craiova, BAMACS 2027)."
            ),
            (
                "Türkiye & Doğu Balkanlar (TR, GR, BG, RO)",
                "2026 ve 2027 yıllarında Türkiye (TAF - taf.org.tr), Yunanistan (SEGAS / OEBA), Bulgaristan (BFLA) ve Romanya (FRA) "
                "tarafından düzenlenen TÜM 35+ yaş Masterlar (Veteran) atletizm yarışmalarını detaylıca bul:\n"
                "- Türkiye: Alkan Öral Türkiye Masterlar Salon Şampiyonası (Bursa), Murat Akman Türkiye Masterlar Açık Saha Şampiyonası (İzmir), "
                "Türkiye Masterlar Kros Şampiyonası, Türkiye Masterlar Atmalar / Uzun Mesafeler Şampiyonası, İsmail Akçay Yol Koşusu, "
                "Fahir Özgüden Masterlar Kupası ve diğer TAF Masterlar yarışmaları.\n"
                "- Yunanistan, Bulgaristan ve Romanya (country='RO'): 2026 ve 2027 Ulusal Masterlar Salon (Indoor), Açık Saha (Outdoor/Stadia), "
                "Open Romanian Outdoor Masters (Bucharest, country='RO') ve Balkan Masterlar Şampiyonası (BAMACS Craiova)."
            ),
            (
                "Batı Balkanlar & Adriyatik (RS, HR, SI, BA, AL, ME, MK)",
                "Find all official 2026 and 2027 National Masters (Veteran) Indoor Championships, National Masters Outdoor (Stadia) "
                "Championships, Masters Throws/Road/Cross Championships, and Balkan Masters Championships (BAMACI Novo Mesto, WMA Marathon Zagreb) "
                "in Serbia (SAS), Croatia (HAS / HAVU), Slovenia (AZS / ZAVS), Bosnia and Herzegovina, Albania, Montenegro, and North Macedonia."
            ),
            (
                "Birleşik Krallık ve İrlanda (GB - BMAF & IE - IMAA Tüm Pist, Salon, Kros ve Yol Şampiyonaları)",
                "Search the official British Masters Athletic Federation fixture calendar (bmaf.org.uk/fixtures/ and bmaf.opentrack.run) "
                "and Irish Masters Athletic Association (irishmastersathletics.com) for ALL 2026 and 2027 championships and open events. "
                "Make sure to include EVERY single one of these BMAF and IMAA championships with their exact dates and venues:\n"
                "- BMAF / British Masters Indoor Track & Field Championships (Lee Valley)\n"
                "- BMAF / British Masters Outdoor Track & Field Championships (Derby)\n"
                "- BMAF Indoor Pentathlon, 3000m and Open Events (Cardiff)\n"
                "- BMAF Winter Throws Championships (Horspath, Oxford) & BMAF Throws Pentathlon / 10,000m Championships\n"
                "- British Masters Cross Country Championships & British Masters Cross Country Relay Championships (Long Eaton / West Park)\n"
                "- British Masters Road Relay Championships (Mallory Park)\n"
                "- British Masters 5k Road Championships (Battersea Park / Horwich)\n"
                "- British Masters 10k Road Championships, 10 Mile Road Championships, Half Marathon Championships, and Marathon Championships\n"
                "- British Masters Race Walking Championships (Indoor, Track & Road)\n"
                "- Irish Masters (IMAA) Indoor Championships (Athlone) and Irish Masters Outdoor Track & Field Championships (Tullamore)."
            ),
            (
                "Fransa ve Almanya (FR - FFA & DE - DLV/LADV Tüm Salon, Pist, Atmalar ve Yol Şampiyonaları)",
                "Find all official 2026 and 2027 Masters (Senioren / Vétérans) athletics championships in France and Germany:\n"
                "- France (FFA - athle.fr): Championnats de France Masters en Salle (Saint-Brieuc 2026, Nantes 2027), "
                "Championnats de France Masters sur Piste / Stadia (Épinal 2026, Tours 2027), Championnats de France Masters d'Épreuves Combinées, "
                "Championnats de France Masters de Marche, Championnats de France Masters Lancers Longs (Hiver), 5km / 10km / Semi-Marathon / Cross Masters, "
                "and Meeting Européen Masters en Salle / EMA League Nantes.\n"
                "- Germany (DLV / LADV - leichtathletik.de): Deutsche Masters Hallenmeisterschaften & Winterwurf (Düsseldorf 2026, Frankfurt), "
                "Deutsche Masters Freiluftmeisterschaften (Mönchengladbach 2026, Gotha), Deutsche Masters Mehrkampfmeisterschaften, "
                "Team DM Masters / DM Masters Bahngehen (Troisdorf), Deutsche Straßenlauf-/Halbmarathon-/Crosslauf-Meisterschaften Masters, "
                "and BW-Meisterschaften Halbmarathon Bernhausen."
            ),
            (
                "Benelüks, İsviçre ve Avusturya (NL, BE, LU, CH, AT)",
                "Find all official 2026 and 2027 Masters athletics championships in the Netherlands, Belgium, Switzerland, and Austria:\n"
                "- Netherlands (Atletiekunie): NK Masters Indoor (Apeldoorn Omnisport), NK Masters Outdoor Track & Field, NK Cross / 10km / Marathon Masters.\n"
                "- Belgium (VAL / LBFA / KBAB): Belgian Masters Indoor Championships, Belgian Masters Outdoor Track & Field & Throws Championships.\n"
                "- Switzerland (Swiss Athletics / Swiss Masters - swiss-masters.ch): Swiss Masters Indoor Championships, "
                "Swiss Masters Outdoor Championships (Part 1 & Part 2), Swiss Winter Throws & Throwing Pentathlon Championships Masters, "
                "Swiss Masters Hammer / Pentathlon Championships.\n"
                "- Austria (ÖLV - oelv.at): Int. Österreichische Hallenmeisterschaften der Masters & Österreichische Meisterschaften der Masters (Outdoor / Straße)."
            ),
            (
                "Güney Avrupa - İtalya, İspanya, Portekiz (IT, ES, PT)",
                "Find all official 2026 and 2027 National Masters (Veteranos / Master) Indoor, Outdoor, Throws, Combined Events, Road, and Cross Championships in:\n"
                "- Italy (FIDAL - fidal.it): Campionati Italiani Master Indoor e Pentathlon Lanci Invernale (Ancona), "
                "Campionati Italiani Individuali su Pista Master (Outdoor), Prove Multiple e Staffette Master, "
                "Campionati Italiani Master 5km / 10km / Mezza Maratona / Maratona / 24h su Strada / Cross.\n"
                "- Spain (RFEA - rfea.es): Campeonato de España Máster Short Track / Pista Cubierta (Antequera), "
                "Campeonato de España Máster al Aire Libre, Lanzamientos Largos Máster, Pruebas Combinadas, Milla / 10km / Media Maratón / Campo a Través Máster.\n"
                "- Portugal (FPAtletismo / ANAV): Campeonatos Nacionais de Veteranos em Pista Curta / Coberta (Indoor), "
                "Campeonatos Nacionais de Veteranos ao Ar Livre (Outdoor), Pentatlo / Lançamentos / 10km em Estrada Veteranos."
            ),
            (
                "Orta, Kuzey ve Baltık Avrupa (PL, CZ, SK, HU, SE, NO, FI, DK, EE, LV, LT)",
                "Find all official 2026 and 2027 National Masters (Veteránů / Veteraner / SAUL / Masters) Indoor, Outdoor, Throws, and Road/Cross Championships in:\n"
                "- Poland (PZLA / PMA - pma.org.pl): Halowe Mistrzostwa Polski Masters (Toruń) & Mistrzostwa Polski Masters na otwartym stadionie, Rzuty (Throws).\n"
                "- Czechia (ČAS / Sdružení veteránů): MČR veteránů v hale (Ostrava) & Czech Masters Open Outdoor Championships (MČR veteránů na dráze).\n"
                "- Slovakia (SAZ) & Hungary (MASZ): National Masters Indoor and Outdoor Athletics Championships.\n"
                "- Nordic & Baltic: Nordic Masters Indoor & Outdoor Championships, Sweden (ISM Veteraner & SM Veteraner), "
                "Norway (NM Masters Innendørs & Utendørs), Finland (SAUL SM-hallit, SM-kilpailut, SM-maastot, SM-maantie), "
                "Denmark (DM Masters Indendørs & Udendørs), and Baltic Open / Estonian, Latvian, Lithuanian Masters Championships."
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
        """TAF faaliyet takvimi sayfasındaki güncel PDF bağlantısını bulur."""
        pdf_urls: List[str] = []
        try:
            html = self.fetch_html("https://taf.org.tr/ova_doc/faaliyet-takvimi/", timeout=12)
            if html:
                soup = BeautifulSoup(html, "html.parser")
                for a_tag in soup.find_all("a", href=True):
                    href = a_tag["href"]
                    if href.lower().endswith(".pdf") and any(y in href for y in ["2026", "2027"]):
                        if href not in pdf_urls:
                            pdf_urls.append(href)
                            break
        except Exception as e:
            logger.debug(f"[TAF-PDF] ova_doc tarama hatası: {e}")

        return pdf_urls[:1]

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

        for item in items:
            try:
                if not isinstance(item, dict):
                    continue
                title = str(item.get("title", "")).strip()
                start_date = str(item.get("start_date", "")).strip()[:10]
                end_date = str(item.get("end_date", start_date)).strip()[:10] or start_date
                if not title or not is_valid_competition_title(title):
                    continue
                if not re.match(r"^202[6-9]-\d{2}-\d{2}$", start_date):
                    continue
                if not re.match(r"^202[6-9]-\d{2}-\d{2}$", end_date) or end_date < start_date:
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
