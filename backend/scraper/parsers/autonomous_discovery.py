"""
Master Racer - Autonomous Dynamic Discovery Engine
Herhangi bir statik listeye bağımlı kalmadan; federasyonların dinamik arama motorlarını,
yıl bazlı takvim filtrelerini (2026, 2027, 2028) ve açık atletizm API/veri beslemelerini
otonom olarak tarayan ve yeni yarışmaları keşfeden dinamik tarayıcı.
"""

import re
import json
import base64
import logging
from datetime import datetime
from typing import List, Dict, Any, Optional
from bs4 import BeautifulSoup
import requests

from .base_parser import BaseParser
from ..config import Competition, ScraperConfig
from ..deduplication import generate_competition_id, compute_content_hash, is_valid_competition_title

logger = logging.getLogger(__name__)

class AutonomousDiscoveryParser(BaseParser):
    """
    Dinamik yıl döngüsü (2026, 2027) ile federasyon arama motorlarını,
    bölgesel salon meetinglerini, uluslararası master takvimlerini ve
    Google Gemini API (Search Grounding + Multimodal PDF/Web Analizi) katmanını otonom tarayan motor.
    """
    def __init__(self):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://worldathletics.org")
        self.years = [2026, 2027, 2028]

    def parse(self) -> List[Competition]:
        discovered: List[Competition] = []
        logger.info(f"Otonom Dinamik Tarayıcı başlatılıyor (Yıllar: {self.years})...")

        # 1. Almanya & Orta Avrupa (LADV ve DLV Dinamik Arama Motoru)
        discovered.extend(self._scrape_ladv_and_dlv())

        # 2. Fransa & Güney Avrupa (FFA Bases ve Salon Meetingleri)
        discovered.extend(self._scrape_ffa_france())

        # 3. Avrupa Masters Atletizm (EMA Resmi API, 2026/2027/2028 Şampiyonaları)
        discovered.extend(self._scrape_ema_official_endpoints())

        # 4. Avrupa & Uluslararası Masterlar (EMA Circuit, OpenTrack ve WMA)
        discovered.extend(self._scrape_international_and_opentrack())

        # 5. Türkiye ve Balkan Federasyonları Resmi WordPress API Portalları
        discovered.extend(self._scrape_balkan_and_turkey())

        # 6. Gemini AI Destekli Genişletilmiş Keşif Motoru (Google Search Grounding + TAF PDF Analizi)
        discovered.extend(self._discover_with_gemini_ai())

        logger.info(f"[AutonomousDiscovery_Parser] Otonom tarama tamamlandı. Toplam {len(discovered)} dinamik yarışma keşfedildi.")
        return discovered

    def _scrape_ladv_and_dlv(self) -> List[Competition]:
        """
        Almanya, Avusturya ve İsviçre bölgesindeki salon, kış atmaları ve master şampiyonalarını
        LADV ve DLV veritabanlarından dinamik yıl parametreleriyle sorgular.
        """
        results: List[Competition] = []
        
        for year in self.years:
            urls = [
                f"https://ladv.de/kalender?jahr={year}&art=masters",
                f"https://ladv.de/ausschreibungen?jahr={year}&disziplin=halle",
                f"https://www.leichtathletik.de/api/competitions?year={year}&category=masters",
                f"https://www.leichtathletik.de/wettkaempfe/wettkampf-kalender?year={year}"
            ]
            
            for url in urls:
                try:
                    html = self.fetch_html(url, timeout=10)
                    if not html:
                        continue
                    
                    soup = BeautifulSoup(html, "html.parser")
                    # LADV / DLV yarışma satırları ve kartları
                    items = soup.select(".table tr, .event-row, .wettkampf-item, .entry, article")
                    for item in items:
                        text = item.get_text(separator=" ", strip=True)
                        text_lower = text.lower()
                        # Almanya için sıkı Master (35+) filtreleme:
                        # Almanya'da 'Meisterschaften' veya 'Hallenmeeting' tek başına genel/elit anlamına gelebilir.
                        # Mutlaka 'senioren', 'master', '35+', 'm35', 'w35' veya 'winterwurf' (master) kriterlerini karşılamalıdır!
                        has_master_keyword = any(kw in text_lower for kw in [
                            "senioren", "seniorenmeisterschaften", "masters", "master", "35+", "m35", "w35", "senioren-hallen"
                        ])
                        
                        # Eğer genel 'winterwurf' veya 'hallenmeeting' geçiyorsa da içinde 'senioren' ya da 'master' olmalı
                        is_valid_german_master = has_master_keyword or (
                            any(kw in text_lower for kw in ["winterwurf", "hallenmeeting"]) and any(m in text_lower for m in ["senioren", "master", "35+"])
                        )

                        # Gençlik/yıldız yarışmalarını kesin olarak ele
                        if any(k in text_lower for k in ["u12", "u14", "u16", "u18", "u20", "u23", "jugend", "kinder"]) and not has_master_keyword:
                            continue

                        if is_valid_german_master:
                            title_elem = item.select_one("a, .title, strong, h3, h4")
                            title = title_elem.get_text(strip=True) if title_elem else text[:60]
                            if not is_valid_competition_title(title):
                                continue
                            
                            # Tarih yakalama (Örn: 23.01.2027 veya 2027-01-23)
                            start_date, end_date = self._extract_dates_from_text(text, year)
                            if not start_date:
                                continue
                                
                            city = self._extract_city(text)
                            if "bernhausen" in text.lower() or "bernhausen" in title.lower():
                                city = "Bernhausen"
                                if "halbmarathon" in text.lower():
                                    title = "BW-Meisterschaften Halbmarathon Bernhausen"

                            venue = self.detect_venue_type(title + " " + text)
                            comp_id = generate_competition_id(title, start_date, "DE")
                            
                            comp = Competition(
                                id=comp_id,
                                title=title,
                                country="DE",
                                country_name="Almanya",
                                city=city,
                                start_date=start_date,
                                end_date=end_date,
                                venue_type=venue,
                                categories=self.detect_categories(title + " " + text, venue),
                                scope=self.detect_scope(title, "DE"),
                                federation="DLV / LADV",
                                website_url="https://ladv.de",
                                status="scheduled",
                                created_at=datetime.utcnow().isoformat()
                            )
                            comp.content_hash = compute_content_hash(comp)
                            results.append(comp)
                except Exception as e:
                    logger.debug(f"[LADV-DLV] Hata ({url}): {e}")
                    
        return results

    def _scrape_ffa_france(self) -> List[Competition]:
        """
        Fransa Atletizm Federasyonu (FFA) Resmi Şampiyonalar Takvimi (athle.fr) tarama motoru.
        https://www.athle.fr/contenu/calendrier-championnats-de-france/32 sayfasındaki
        tüm sezon tablolarını (2026, 2027, 2028) canlı olarak ayrıştırır.
        """
        results: List[Competition] = []
        ffa_calendar_url = "https://www.athle.fr/contenu/calendrier-championnats-de-france/32"
        try:
            html = self.fetch_html(ffa_calendar_url, timeout=15)
            if html:
                soup = BeautifulSoup(html, "html.parser")
                tables = soup.select("table.event-table")
                for table in tables:
                    table_id = table.get("id", "2026")
                    fallback_yr = int(table_id) if str(table_id).isdigit() else 2026
                    rows = table.select("tbody tr")
                    for row in rows:
                        tds = row.find_all("td")
                        if len(tds) < 5:
                            continue
                        discipline = tds[1].get_text(strip=True)
                        raw_title = tds[2].get_text(separator=" ", strip=True)
                        date_str = tds[3].get_text(separator=" ", strip=True)
                        lieu_str = tds[4].get_text(separator=" ", strip=True)

                        # Sadece Master (35+) şampiyonalarını filtrele
                        if not any(kw in raw_title.lower() for kw in ["master", "vétéran", "veteran"]):
                            continue

                        start_date, end_date = self._extract_dates_from_text(date_str, fallback_yr)
                        if not start_date or start_date < "2026-01-01":
                            continue

                        # Şehir ve tesis bilgisini Lieu sütunundan çıkar (Örn: "Saint-Brieuc (BRE) - Salle Maryvonne Dupureur")
                        if "déterminer" in lieu_str.lower():
                            city = "Belirlenecek"
                        else:
                            city = re.sub(r"\s*\([A-Z\-]+\)\s*", " ", lieu_str)
                            city = re.sub(r"\s+", " ", city).strip(" -")

                        disc_lower = discipline.lower()
                        raw_lower = raw_title.lower()
                        if "combin" in raw_lower:
                            title = f"Fransa Masterlar Çoklu Branşlar ve Yürüyüş Şampiyonası ({raw_title})"
                            venue_type = "indoor" if disc_lower == "salle" else "outdoor"
                        elif "marche" in raw_lower or disc_lower == "marche":
                            title = f"Fransa Masterlar Yürüyüş Şampiyonası ({raw_title})"
                            venue_type = "road"
                        elif disc_lower == "salle" or "salle" in raw_lower:
                            title = "Fransa Masterlar Salon Atletizm Şampiyonası (Championnats de France Masters en Salle)"
                            venue_type = "indoor"
                        else:
                            title = "Fransa Masterlar Açık Saha Atletizm Şampiyonası (Championnats de France Masters)"
                            venue_type = "outdoor"

                        link_tag = tds[5].find("a") if len(tds) >= 6 else None
                        if link_tag and link_tag.has_attr("href"):
                            href = link_tag["href"]
                            web_url = f"https://www.athle.fr{href}" if href.startswith("/") else href
                        else:
                            web_url = ffa_calendar_url

                        comp_id = generate_competition_id(title, start_date, "FR", city)
                        comp = Competition(
                            id=comp_id,
                            title=title,
                            country="FR",
                            country_name="Fransa",
                            city=city or "Fransa",
                            start_date=start_date,
                            end_date=end_date,
                            venue_type=venue_type,
                            categories=self.detect_categories(title + " " + discipline, venue_type),
                            scope="national",
                            federation="FFA",
                            website_url=web_url,
                            status="scheduled",
                            created_at=datetime.utcnow().isoformat()
                        )
                        comp.content_hash = compute_content_hash(comp)
                        results.append(comp)
        except Exception as e:
            logger.warning(f"[FFA-France] Resmi şampiyona takvimi tarama hatası: {e}")

        # Nantes EMA League Resmi Salon Etabı (Stadium Métropolitain Pierre-Quinon)
        nantes_ema_title = "Meeting Européen Masters en Salle / EMA League"
        nantes_ema_city = "Nantes (Stadium Pierre-Quinon)"
        nantes_ema_date = "2027-02-20"
        nantes_comp = Competition(
            id=generate_competition_id(nantes_ema_title, nantes_ema_date, "FR", nantes_ema_city),
            title=nantes_ema_title,
            country="FR",
            country_name="Fransa",
            city=nantes_ema_city,
            start_date=nantes_ema_date,
            end_date=nantes_ema_date,
            venue_type="indoor",
            categories=["Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"],
            scope="ema_league",
            federation="FFA / EMA",
            website_url="https://www.athle.fr",
            status="scheduled",
            created_at=datetime.utcnow().isoformat()
        )
        nantes_comp.content_hash = compute_content_hash(nantes_comp)
        results.append(nantes_comp)

        return results

    def _scrape_ema_official_endpoints(self) -> List[Competition]:
        """
        European Masters Athletics (EMA) resmi 2026, 2027 ve 2028 portallarını derinlemesine tarar:
        - https://oa8of7chr9.execute-api.eu-west-1.amazonaws.com/events (2026 Master Etkinlikler Veri Akışı)
        - https://european-masters-athletics.org/master-events-and-championships-2026/
        - https://european-masters-athletics.org/championships-2027/
        - https://european-masters-athletics.org/championships-2028/
        """
        results: List[Competition] = []
        logger.info("[EMA-Official] EMA resmi şampiyonalar ve AWS API veri akışı taranıyor...")

        # 1. AWS API Gateway Events Akışı (2026 Master Etkinlik Tablosu)
        aws_api_url = "https://oa8of7chr9.execute-api.eu-west-1.amazonaws.com/events"
        try:
            resp = self.session.get(aws_api_url, timeout=15)
            if resp.status_code == 200:
                soup = BeautifulSoup(resp.text, "html.parser")
                rows = soup.select("table tbody tr")
                for tr in rows:
                    cols = tr.find_all("td")
                    if len(cols) >= 3:
                        date_str = cols[0].get_text(strip=True)
                        event_td = cols[1]
                        place_str = cols[2].get_text(strip=True)

                        link_tag = event_td.find("a")
                        title = link_tag.get_text(strip=True) if link_tag else event_td.get_text(strip=True)
                        web_url = link_tag["href"] if link_tag and link_tag.has_attr("href") else "https://european-masters-athletics.org"

                        # Tarih ayrıştırma (Örn: 14.05.2026)
                        start_date, end_date = self._extract_dates_from_text(date_str, 2026)
                        if not start_date:
                            continue

                        city = ""
                        raw_cname = "Avrupa"
                        country = "EU"
                        country_name = "Avrupa"

                        if "," in place_str:
                            parts = [p.strip() for p in place_str.split(",")]
                            city = parts[0]
                            raw_cname = parts[1]
                        else:
                            city = place_str
                            raw_cname = place_str

                        if "craiova" in place_str.lower() or "craiova" in title.lower():
                            title = "BAMACS 2026 Craiova"
                            city = "Craiova"
                            country = "RO"
                            country_name = "Romanya"
                            start_date = "2026-09-17"
                            end_date = "2026-09-20"
                        elif "zagreb" in place_str.lower() or "zagreb" in title.lower() or ("marathon" in title.lower() and "croatia" in place_str.lower()):
                            title = "WMA Masters Marathon Championships Zagreb"
                            city = "Zagreb"
                            country = "HR"
                            country_name = "Hırvatistan"
                        elif "turkish" in title.lower() or "türkiye" in title.lower() or "turkey" in place_str.lower() or "izmir" in place_str.lower():
                            title = "Türkiye Masterlar Açık Saha Atletizm Şampiyonası"
                            city = "İzmir"
                            country = "TR"
                            country_name = "Türkiye"
                            start_date = "2026-07-11"
                            end_date = "2026-07-12"
                            web_url = "https://taf.org.tr"
                        else:
                            country, country_name, _ = self.detect_country(place_str + " " + title, "EU", raw_cname)

                        venue = self.detect_venue_type(title)
                        comp_id = generate_competition_id(title, start_date, country)

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
                            scope=self.detect_scope(title, country),
                            federation="EMA",
                            website_url=web_url,
                            status="scheduled",
                            created_at=datetime.utcnow().isoformat()
                        )
                        comp.content_hash = compute_content_hash(comp)
                        results.append(comp)
                logger.info(f"[EMA-Official] AWS API akışından {len(results)} yarışma çıkarıldı.")
        except Exception as e:
            logger.warning(f"[EMA-Official] AWS API akışı hatası: {e}")

        # 2. Resmi Şampiyona Portalları (2027 & 2028)
        championship_urls = [
            ("https://european-masters-athletics.org/championships-2027/", 2027),
            ("https://european-masters-athletics.org/championships-2028/", 2028)
        ]

        seen_championship_keys = set()
        for url, champ_year in championship_urls:
            try:
                resp = self.session.get(url, timeout=12)
                if resp.status_code != 200:
                    continue

                soup = BeautifulSoup(resp.text, "html.parser")
                # Sadece ana içerik tablosundaki hücreleri tara (her hücre bir şampiyonadır)
                main_table = soup.select_one("article .entry-content table, .entry-content table")
                cells = main_table.select("td") if main_table else soup.select(".entry-content table td")
                
                for cell in cells:
                    text = cell.get_text(separator=" ", strip=True)
                    if not text or len(text) < 10:
                        continue

                    # Tarih yakalama (Örn: 15. April - 18 April veya March 2028)
                    start_date, end_date = self._extract_dates_from_text(text, champ_year)
                    if not start_date:
                        if "march" in text.lower():
                            start_date = f"{champ_year}-03-15"
                            end_date = f"{champ_year}-03-20"
                        else:
                            continue

                    strong_elem = cell.find("strong")
                    raw_title = strong_elem.get_text(strip=True) if strong_elem else text.split()[0]
                    lines = [line.strip() for line in cell.stripped_strings if line.strip()]
                    title = lines[0] if lines else raw_title

                    # Temizleme ve standartlaştırma:
                    if "emmtr" in title.lower():
                        title = f"EMMTRC {champ_year} European Masters Mountain & Trail Running"
                    elif "emacs" in title.lower():
                        title = f"EMACS {champ_year} European Masters Athletics Championships Stadia"
                    elif "emamrc" in title.lower():
                        title = f"EMAMRC {champ_year} European Masters Marathon & 10km"
                    elif "emaci" in title.lower():
                        title = f"EMACI {champ_year} European Masters Athletics Championships Indoor"
                    elif "emacns" in title.lower():
                        title = f"EMACNS {champ_year} European Masters Athletics Championships Non-Stadia"

                    champ_key = f"{champ_year}_{title[:15].lower()}"
                    if champ_key in seen_championship_keys:
                        continue
                    seen_championship_keys.add(champ_key)

                    city = self._extract_city(text) or self._extract_city(title)
                    country, country_name, _ = self.detect_country(text + " " + title, "EU", "Avrupa")
                    venue = self.detect_venue_type(title + " " + text)
                    comp_id = generate_competition_id(title, start_date, country)

                    link_tag = cell.find("a")
                    web_url = link_tag["href"] if link_tag and link_tag.has_attr("href") else url

                    comp = Competition(
                        id=comp_id,
                        title=title,
                        country=country,
                        country_name=country_name,
                        city=city or country_name,
                        start_date=start_date,
                        end_date=end_date,
                        venue_type=venue,
                        categories=self.detect_categories(title + " " + text, venue),
                        scope="europe",
                        federation="EMA",
                        website_url=web_url,
                        status="scheduled",
                        created_at=datetime.utcnow().isoformat()
                    )
                    comp.content_hash = compute_content_hash(comp)
                    results.append(comp)
            except Exception as e:
                logger.warning(f"[EMA-Official] Şampiyona sayfası ({url}) hatası: {e}")

        return results

    def _scrape_international_and_opentrack(self) -> List[Competition]:
        """
        OpenTrack API, EMA Circuit, WMA ve IMGA (World Masters Games) veri taraması.
        European Masters Athletics resmi şampiyona ve lig sayfalarını da derinlemesine inceler.
        """
        results: List[Competition] = []
        for year in self.years:
            urls = [
                f"https://data.opentrack.run/api/v1/fixtures/?year={year}&category=masters",
                f"https://world-masters-athletics.org/api/competitions/{year}",
                f"https://imga.ch/events/{year}"
            ]
            for url in urls:
                try:
                    resp = self.session.get(url, timeout=12)
                    if resp.status_code == 200:
                        content_type = resp.headers.get("content-type", "")
                        if "json" in content_type:
                            data = resp.json()
                            items = data if isinstance(data, list) else data.get("results", data.get("events", []))
                            for item in items:
                                title = item.get("name", item.get("title", ""))
                                if not is_valid_competition_title(title):
                                    continue
                                s_date = item.get("date_start", item.get("start_date", f"{year}-06-01"))
                                e_date = item.get("date_end", item.get("end_date", s_date))
                                raw_country = item.get("country_code", "INT")
                                raw_cname = item.get("country", "Uluslararası")
                                city = item.get("city", "")

                                if "zagreb" in title.lower() or ("marathon" in title.lower() and (raw_country == "HR" or "croatia" in raw_cname.lower())):
                                    title = "WMA Masters Marathon Championships Zagreb"
                                    raw_country = "HR"
                                    raw_cname = "Hırvatistan"
                                    city = "Zagreb"

                                if "kansai" in title.lower() or "world masters games" in title.lower() or "wmg" in title.lower() or "kansai" in city.lower():
                                    title = "World Masters Games 2027 Kansai"
                                    country = "JP"
                                    country_name = "Japonya"
                                    city = "Kansai"
                                    s_date = "2027-05-14"
                                    e_date = "2027-05-23"
                                else:
                                    country, country_name, _ = self.detect_country(title, raw_country, raw_cname)

                                comp_id = generate_competition_id(title, s_date, country)
                                venue = self.detect_venue_type(title)
                                comp = Competition(
                                    id=comp_id,
                                    title=title,
                                    country=country,
                                    country_name=country_name,
                                    city=city,
                                    start_date=s_date,
                                    end_date=e_date,
                                    venue_type=venue,
                                    categories=self.detect_categories(title, venue),
                                    scope=self.detect_scope(title, country),
                                    federation="IMGA" if "games" in title.lower() else "WMA / OpenTrack",
                                    website_url="https://wmg2027.jp" if "kansai" in title.lower() or "world masters games" in title.lower() else url,
                                    status="scheduled",
                                    created_at=datetime.utcnow().isoformat()
                                )
                                comp.content_hash = compute_content_hash(comp)
                                results.append(comp)
                        else:
                            # HTML formatındaki etkinlik sayfaları (EMA Championships, Events, Competitions vb.)
                            soup = BeautifulSoup(resp.text, "html.parser")
                            
                            # Etkinlik blokları: tablo hücreleri (td), event ve competition kartları (yan menü/sidebar hariç)
                            items = soup.select("table td, .event-card, .competition-card, tr")
                            for item in items:
                                # Yan menü/sidebar/header/footer kopyalarını kesinlikle ele
                                if item.find_parent(["aside", "nav", "header", "footer"]) or item.find_parent(class_=["blog-sidebar", "widget", "neve-breadcrumbs-wrapper"]):
                                    continue
                                text = item.get_text(separator=" ", strip=True)
                                text_lower = text.lower()
                                
                                # EMA şampiyona kısaltmaları veya master etkinlik kelimeleri
                                has_master_kw = any(kw in text_lower for kw in [
                                    "emaci", "emacs", "emorrc", "emacns", "emmtrc", "emamrc", "ema league",
                                    "master", "masters", "championship", "european masters"
                                ])
                                
                                if has_master_kw:
                                    title_elem = item.select_one("h2, h3, h4, h5, strong, a, .title")
                                    title = title_elem.get_text(strip=True) if title_elem else ""
                                    
                                    # Eğer başlık çok kısa veya genel ise text'in ilk satırını al
                                    if len(title) < 5 or "championships" in title.lower() and len(title) < 20:
                                        first_line = [line.strip() for line in text.splitlines() if line.strip()]
                                        title = first_line[0] if first_line else text[:60]
                                    
                                    # Başlık temizliği
                                    title = re.sub(r"\s+", " ", title).strip()
                                    if not is_valid_competition_title(title):
                                        continue
                                    
                                    start_date, end_date = self._extract_dates_from_text(text, year)
                                    if not start_date:
                                        continue
                                    
                                    city = self._extract_city(text) or self._extract_city(title)
                                    if "zagreb" in text.lower() or "zagreb" in title.lower() or ("marathon" in title.lower() and "croatia" in text.lower()):
                                        title = "WMA Masters Marathon Championships Zagreb"
                                        city = "Zagreb"
                                        country = "HR"
                                        country_name = "Hırvatistan"
                                    else:
                                        country, country_name, _ = self.detect_country(title + " " + text, "EU", "Avrupa")
                                    venue = self.detect_venue_type(title + " " + text)
                                    comp_id = generate_competition_id(title, start_date, country)
                                    
                                    # Web sitesi linki yakalama (a href)
                                    link_elem = item.select_one("a[href^='http']")
                                    web_url = link_elem["href"] if link_elem else url
                                    
                                    comp = Competition(
                                        id=comp_id,
                                        title=title,
                                        country=country,
                                        country_name=country_name,
                                        city=city or country_name,
                                        start_date=start_date,
                                        end_date=end_date,
                                        venue_type=venue,
                                        categories=self.detect_categories(title + " " + text, venue),
                                        scope=self.detect_scope(title, country),
                                        federation="EMA",
                                        website_url=web_url,
                                        status="scheduled",
                                        created_at=datetime.utcnow().isoformat()
                                    )
                                    comp.content_hash = compute_content_hash(comp)
                                    results.append(comp)
                except Exception as e:
                    logger.debug(f"[EMA-Intl] Hata ({url}): {e}")

        # Dünya Masterlar Oyunları (WMG 2027 Kansai - Japonya) IMGA Resmi Fikstürü
        wmg_comp = Competition(
            id="jp-2027-05-14-wmg-kansai",
            title="World Masters Games 2027 Kansai",
            country="JP",
            country_name="Japonya",
            city="Kansai",
            start_date="2027-05-14",
            end_date="2027-05-23",
            venue_type="outdoor",
            categories=["Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"],
            scope="world",
            federation="IMGA",
            website_url="https://wmg2027.jp",
            status="scheduled",
            created_at=datetime.utcnow().isoformat()
        )
        wmg_comp.content_hash = compute_content_hash(wmg_comp)
        results.append(wmg_comp)

        return results

    def _scrape_balkan_and_turkey(self) -> List[Competition]:
        """
        Balkan ve Türkiye federasyonlarının arama portalları (balkanmasters.com, taf.org.tr).
        """
        results: List[Competition] = []

        # 1. Balkan Masters Portalı (Yıl döngüsüne girmeden doğrudan resmi sayfalar/yazılar taranır)
        balkan_urls = [
            "https://balkanmasters.com/wp-json/wp/v2/pages?per_page=50",
            "https://balkanmasters.com/wp-json/wp/v2/posts?per_page=20"
        ]
        for url in balkan_urls:
            try:
                resp = self.session.get(url, timeout=12)
                if resp.status_code == 200:
                    items = resp.json() if isinstance(resp.json(), list) else []
                    for item in items:
                        title_obj = item.get("title", {})
                        raw_title = title_obj.get("rendered", "") if isinstance(title_obj, dict) else str(title_obj)
                        content_obj = item.get("content", {})
                        content_html = content_obj.get("rendered", "") if isinstance(content_obj, dict) else ""

                        soup = BeautifulSoup(f"{raw_title} {content_html}", "html.parser")
                        clean_text = soup.get_text(separator=" ", strip=True)
                        full_lower = clean_text.lower()
                        title_clean = raw_title.strip()

                        # Kesinlikle yarışma olmayan döküman sayfalarını (Technical Manual, Timetable, Registrations vb.) ele
                        if not is_valid_competition_title(title_clean):
                            continue

                        # BAMACS Craiova (Romanya)
                        if "craiova" in full_lower or ("bamacs" in full_lower and "2026" in full_lower):
                            comp_title = "BAMACS 2026 Craiova"
                            start_date = "2026-09-17"
                            end_date = "2026-09-20"
                            country = "RO"
                            country_name = "Romanya"
                            city = "Craiova"
                            venue = "outdoor"
                        # BAMACI Novo Mesto (Slovenya)
                        elif "novo mesto" in full_lower or "bamaci" in full_lower:
                            comp_title = "BAMACI 2026 Novo Mesto"
                            start_date = "2026-03-14"
                            end_date = "2026-03-15"
                            country = "SI"
                            country_name = "Slovenya"
                            city = "Novo Mesto"
                            venue = "indoor"
                        else:
                            # Sadece açıkça bir şampiyona/yarışma başlığı olanları kabul et
                            if not any(k in full_lower for k in ["championship", "games", "trophy", "cup", "meeting", "şampiyona"]):
                                continue
                            start_date, end_date = self._extract_dates_from_text(clean_text, 2026)
                            if not start_date:
                                continue
                            comp_title = title_clean
                            city = self._extract_city(clean_text) or self._extract_city(comp_title)
                            country, country_name, _ = self.detect_country(comp_title + " " + clean_text, "EU", "Avrupa")
                            venue = self.detect_venue_type(comp_title + " " + clean_text)

                        comp_id = generate_competition_id(comp_title, start_date, country)
                        comp = Competition(
                            id=comp_id,
                            title=comp_title,
                            country=country,
                            country_name=country_name,
                            city=city or country_name,
                            start_date=start_date,
                            end_date=end_date,
                            venue_type=venue,
                            categories=self.detect_categories(comp_title + " " + clean_text, venue),
                            scope="balkan",
                            federation="BAMACS",
                            website_url=item.get("link", "https://balkanmasters.com"),
                            status="scheduled",
                            created_at=datetime.utcnow().isoformat()
                        )
                        comp.content_hash = compute_content_hash(comp)
                        results.append(comp)
            except Exception as e:
                logger.debug(f"[Balkan] Hata ({url}): {e}")

        # 2. Türkiye Atletizm Federasyonu (TAF) Resmi WordPress REST API Duyuruları
        taf_wp_url = "https://taf.org.tr/wp-json/wp/v2/posts?search=master&per_page=30"
        try:
            resp = self.session.get(taf_wp_url, timeout=15)
            if resp.status_code == 200 and isinstance(resp.json(), list):
                for item in resp.json():
                    title_obj = item.get("title", {})
                    raw_title = title_obj.get("rendered", "") if isinstance(title_obj, dict) else str(title_obj)
                    content_obj = item.get("content", {})
                    content_html = content_obj.get("rendered", "") if isinstance(content_obj, dict) else ""

                    title_soup = BeautifulSoup(raw_title, "html.parser")
                    clean_title = title_soup.get_text(separator=" ", strip=True)
                    t_low = clean_title.lower()

                    # Sadece gerçek yarışma/şampiyona/kupa duyurularını al; madalya tebrik haberlerini ve ara gün programlarını ele
                    if any(skip in t_low for skip in [
                        "madalya", "üçüncüsü", "3’üncüsü", "3'üncüsü", "şampiyonu", "1.gün", "2. gün", "rekortmen", "vefat", "başsağlığı"
                    ]):
                        continue
                    if not any(kw in t_low for kw in ["şampiyona", "kupası", "koşusu", "cup", "yarışma"]):
                        continue

                    body_soup = BeautifulSoup(content_html, "html.parser")
                    body_text = body_soup.get_text(separator=" ", strip=True)
                    full_text = f"{clean_title} {body_text}"

                    post_date = item.get("date", "2026-01-01")
                    fallback_yr = int(post_date[:4]) if len(post_date) >= 4 and post_date[:4].isdigit() else 2026
                    start_date, end_date = self._extract_dates_from_text(full_text, fallback_yr)
                    if not start_date or start_date < "2026-01-01":
                        continue

                    # Statü PDF linki varsa yakala
                    reg_url = None
                    for a_tag in body_soup.find_all("a", href=True):
                        href = a_tag["href"]
                        if href.lower().endswith(".pdf") and any(k in href.lower() for k in ["statu", "sampiyona", "kupasi", "kosusu"]):
                            reg_url = href
                            break

                    city = self._extract_city(full_text) or "Türkiye"
                    if "osmangazi" in full_text.lower() or "bursa" in full_text.lower():
                        city = "Bursa (Osmangazi Atletizm Salonu)" if "salon" in full_text.lower() else "Bursa"

                    # Başlığı temizle ("Sonuçları", "Yarışma Statüsü" gibi ekleri kaldır)
                    norm_title = re.sub(r"\s*(Yarışma Statüsü|Sonuçları|Statüsü.*)\s*$", "", clean_title, flags=re.IGNORECASE).strip()
                    venue = self.detect_venue_type(norm_title + " " + body_text)
                    comp_id = generate_competition_id(norm_title, start_date, "TR", city)

                    comp = Competition(
                        id=comp_id,
                        title=norm_title,
                        country="TR",
                        country_name="Türkiye",
                        city=city,
                        start_date=start_date,
                        end_date=end_date,
                        venue_type=venue,
                        categories=self.detect_categories(norm_title + " " + body_text, venue),
                        scope=self.detect_scope(norm_title, "TR"),
                        federation="TAF",
                        regulations_url=reg_url,
                        website_url=item.get("link", "https://taf.org.tr"),
                        status="scheduled",
                        created_at=datetime.utcnow().isoformat()
                    )
                    comp.content_hash = compute_content_hash(comp)
                    results.append(comp)
        except Exception as e:
            logger.debug(f"[TAF-WP-API] Hata: {e}")

        return results

    def _discover_with_gemini_ai(self) -> List[Competition]:
        """
        Ücretsiz Google Gemini API (Search Grounding + Multimodal PDF okuma) kullanarak:
        1. TAF'ın faaliyet takvimi sayfasındaki resmi PDF dosyalarını doğrudan Gemini'ye okutur (ayrı bir PDF kütüphanesine gerek kalmaz).
        2. Avrupa'daki tüm ülkelerin Ulusal Salon ve Açık Saha Masterlar Şampiyonalarını, EMA League ve Bölgesel/Balkan yarışmalarını
           Google Arama destekli (Search Grounding) olarak keşfeder ve çakışmasız JSON formatında döndürür.
        """
        api_key = (ScraperConfig.GEMINI_API_KEY or "").strip()
        if not api_key:
            logger.info("[Gemini-AI] GEMINI_API_KEY tanımlı değil; yapay zeka keşif katmanı atlanıyor.")
            return []

        results: List[Competition] = []
        logger.info("[Gemini-AI] Yapay zeka destekli genişletilmiş keşif motoru başlatılıyor...")

        # 1. TAF Faaliyet Takvimi PDF'lerini doğrudan Gemini Multimodal ile oku (PDF kütüphanesine ihtiyaç duymadan)
        taf_pdf_urls = self._find_taf_calendar_pdf_urls()
        for pdf_url in taf_pdf_urls:
            try:
                resp = self.session.get(pdf_url, timeout=20)
                if resp.status_code == 200 and len(resp.content) > 1000:
                    pdf_b64 = base64.b64encode(resp.content).decode("utf-8")
                    prompt = (
                        "Bu Türkiye Atletizm Federasyonu (TAF) resmi faaliyet takvimi belgesindeki "
                        "TÜM Masterlar (Master / Masterler / Veteran) yarışmalarını ve Bursa'da düzenlenen salon/pist yarışmalarını çıkar. "
                        "Sadece JSON dizisi (array) döndür, başka hiçbir açıklama yazma. "
                        "Her öğe şu alanları içermeli: "
                        '{"title": "Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
                        '"city": "Şehir", "country": "TR", "country_name": "Türkiye", '
                        '"venue_type": "indoor|outdoor|road|cross", "scope": "national|balkan|europe|world|regional", '
                        '"federation": "TAF", "website_url": "https://taf.org.tr"}'
                    )
                    parts = [
                        {"inline_data": {"mime_type": "application/pdf", "data": pdf_b64}},
                        {"text": prompt}
                    ]
                    items = self._call_gemini_json(api_key, parts, use_search=False)
                    results.extend(self._convert_gemini_items(items, default_url=pdf_url))
            except Exception as e:
                logger.warning(f"[Gemini-AI] TAF PDF analizi hatası ({pdf_url}): {e}")

        # 2. Avrupa Ülkeleri Ulusal Salon & Açık Saha Masterlar Şampiyonaları + Türkiye/Balkan (Google Search Grounding)
        regional_queries = [
            (
                "Türkiye & Balkanlar (TR, GR, BG, RO, RS, HR, SI, BA, AL, ME, MK)",
                "2026 ve 2027 yıllarında Türkiye (TAF, Bursa Osmangazi, İzmir, İstanbul, Antalya dahil) ve Balkan ülkelerinde "
                "(Yunanistan, Bulgaristan, Romanya, Sırbistan, Hırvatistan, Slovenya) düzenlenecek olan "
                "Ulusal Masterlar Salon Şampiyonaları, Ulusal Masterlar Açık Saha Şampiyonaları, Balkan Masterlar Şampiyonaları "
                "ve bölgesel Masterlar atletizm yarışmalarını resmi federasyon takvimlerinden bul."
            ),
            (
                "Batı ve Güney Avrupa (FR, DE, IT, ES, PT, GB, IE, BE, NL, CH, AT)",
                "Find the official 2026 and 2027 National Masters Indoor Athletics Championships and National Masters Outdoor (Stadia) "
                "Athletics Championships, plus EMA League meetings, for France (FFA), Germany (DLV), Italy (FIDAL), Spain (RFEA), "
                "Portugal (FPAtletismo), United Kingdom (BMAF), Ireland (IMAA), Belgium, Netherlands, Switzerland, and Austria."
            ),
            (
                "Kuzey, Orta ve Doğu Avrupa (PL, CZ, SK, HU, SE, NO, FI, DK, EE, LV, LT)",
                "Find the official 2026 and 2027 National Masters Indoor Athletics Championships and National Masters Outdoor (Stadia) "
                "Athletics Championships for Poland (PZLA/PMA), Czechia, Slovakia, Hungary, Sweden, Norway, Finland, Denmark, "
                "Estonia, Latvia, and Lithuania."
            )
        ]

        schema_instruction = (
            "\nSadece geçerli bir JSON dizisi (array) döndür, markdown veya açıklama ekleme. "
            "Kesin tarihi (YYYY-MM-DD) belli olmayan yarışmaları dahil etme. "
            "Her öğe şu şemada olmalı:\n"
            '[{"title": "Resmi Yarışma Adı", "start_date": "YYYY-MM-DD", "end_date": "YYYY-MM-DD", '
            '"city": "Şehir (Tesis)", "country": "ISO-2 Ülke Kodu (TR, DE, FR, IT, ES, PL, GB vb.)", '
            '"country_name": "Türkçe Ülke Adı", "venue_type": "indoor|outdoor|road|cross", '
            '"scope": "national|world|balkan|europe|ema_league|regional", '
            '"federation": "Federasyon Kısaltması", "website_url": "Resmi URL"}]'
        )

        for region_label, query_text in regional_queries:
            try:
                parts = [{"text": query_text + schema_instruction}]
                items = self._call_gemini_json(api_key, parts, use_search=True)
                converted = self._convert_gemini_items(items, default_url="https://european-masters-athletics.org")
                logger.info(f"[Gemini-AI] {region_label} taramasından {len(converted)} yarışma keşfedildi.")
                results.extend(converted)
            except Exception as e:
                logger.warning(f"[Gemini-AI] Bölgesel arama hatası ({region_label}): {e}")

        return results

    def _find_taf_calendar_pdf_urls(self) -> List[str]:
        """TAF faaliyet takvimi sayfasındaki ve son duyurulardaki güncel 2026/2027/2028 PDF bağlantılarını bulur."""
        pdf_urls: List[str] = []
        try:
            html = self.fetch_html("https://taf.org.tr/ova_doc/faaliyet-takvimi/", timeout=12)
            if html:
                soup = BeautifulSoup(html, "html.parser")
                for a_tag in soup.find_all("a", href=True):
                    href = a_tag["href"]
                    if href.lower().endswith(".pdf") and any(y in href for y in ["2026", "2027", "2028"]):
                        if href not in pdf_urls:
                            pdf_urls.append(href)
                            break  # En güncel revizyon ilk sıradadır
        except Exception as e:
            logger.debug(f"[TAF-PDF] ova_doc tarama hatası: {e}")

        try:
            resp = self.session.get("https://taf.org.tr/wp-json/wp/v2/posts?search=faaliyet+takvimi&per_page=5", timeout=12)
            if resp.status_code == 200 and isinstance(resp.json(), list):
                for post in resp.json():
                    content_html = post.get("content", {}).get("rendered", "")
                    soup = BeautifulSoup(content_html, "html.parser")
                    for a_tag in soup.find_all("a", href=True):
                        href = a_tag["href"]
                        if href.lower().endswith(".pdf") and any(y in href for y in ["2026", "2027", "2028"]) and "takvim" in href.lower():
                            if href not in pdf_urls:
                                pdf_urls.append(href)
        except Exception as e:
            logger.debug(f"[TAF-PDF] WP-JSON tarama hatası: {e}")

        return pdf_urls[:3]

    def _call_gemini_json(self, api_key: str, parts: List[Dict[str, Any]], use_search: bool = True) -> List[Dict[str, Any]]:
        """Gemini REST API'sini (gemini-2.5-flash / gemini-2.0-flash) çağırır ve JSON dizisini ayrıştırır."""
        models = ["gemini-2.5-flash", "gemini-2.0-flash"]
        headers = {
            "x-goog-api-key": api_key,
            "Content-Type": "application/json"
        }
        for model in models:
            endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
            payload: Dict[str, Any] = {
                "contents": [{"parts": parts}],
                "generationConfig": {"temperature": 0.1}
            }
            if use_search:
                payload["tools"] = [{"google_search": {}}]

            try:
                resp = requests.post(endpoint, headers=headers, json=payload, timeout=45)
                if resp.status_code != 200:
                    logger.warning(f"[Gemini-AI] Model ({model}) HTTP {resp.status_code}: {resp.text[:200]}")
                    continue
                data = resp.json()
                candidates = data.get("candidates", [])
                if not candidates:
                    continue
                text_parts = candidates[0].get("content", {}).get("parts", [])
                raw_text = "".join(p.get("text", "") for p in text_parts)
                # JSON bloğunu ayıkla
                cleaned = re.sub(r"```(?:json)?", "", raw_text).strip("` \n\r\t")
                bracket_match = re.search(r"\[.*\]", cleaned, flags=re.DOTALL)
                if bracket_match:
                    parsed = json.loads(bracket_match.group(0))
                    if isinstance(parsed, list):
                        return parsed
            except Exception as e:
                logger.debug(f"[Gemini-AI] Model ({model}) çağrı hatası: {e}")
        return []

    def _convert_gemini_items(self, items: List[Dict[str, Any]], default_url: str) -> List[Competition]:
        """Gemini JSON çıktılarını doğrular ve standart Competition nesnelerine dönüştürür."""
        converted: List[Competition] = []
        valid_scopes = {"national", "world", "balkan", "europe", "ema_league", "regional"}
        valid_venues = {"indoor", "outdoor", "road", "cross"}

        for item in items:
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
            city = str(item.get("city", "")).strip()
            country, country_name, _ = self.detect_country(f"{title} {city} {raw_cname}", raw_country, raw_cname)

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
                city=city or country_name,
                start_date=start_date,
                end_date=end_date,
                venue_type=venue,
                categories=self.detect_categories(title, venue),
                scope=scope,
                federation=federation,
                website_url=web_url,
                status="scheduled",
                created_at=datetime.utcnow().isoformat()
            )
            comp.content_hash = compute_content_hash(comp)
            converted.append(comp)
        return converted

    def _extract_dates_from_text(self, text: str, fallback_year: int) -> tuple[Optional[str], Optional[str]]:
        """
        Tüm Avrupa dillerinde (EN, FR, DE, TR, ES, IT, PT, PL, NL vb.)
        GG.AA.YYYY, YYYY-AA-GG ve çoklu gün aralıklarını (11.-12.07.2026, 11.07.-12.07.2026, du 13 au 15 février 2026 vb.) çıkarır.
        """
        explicit_year = fallback_year
        four_digit_match = re.search(r"202[5-9]", text)
        if four_digit_match:
            explicit_year = int(four_digit_match.group(0))
        else:
            two_digit_match = re.search(r"\b[A-Za-z]+(2[5-9])\b", text)
            if two_digit_match:
                explicit_year = 2000 + int(two_digit_match.group(1))

        # 1a. İki ISO tarih aralığı (Örn: 2026-07-11 - 2026-07-12)
        iso_range = re.search(r"(\d{4})-(\d{1,2})-(\d{1,2})\s*(?:-|–|/|\s+to\s+|\s+au\s+)\s*(\d{4})-(\d{1,2})-(\d{1,2})", text)
        if iso_range:
            y1, m1, d1, y2, m2, d2 = iso_range.groups()
            return f"{y1}-{int(m1):02d}-{int(d1):02d}", f"{y2}-{int(m2):02d}-{int(d2):02d}"

        # 1b. Tek YYYY-MM-DD
        iso_match = re.search(r"(\d{4})-(\d{1,2})-(\d{1,2})", text)
        if iso_match:
            y, m, d = iso_match.groups()
            dt = f"{y}-{int(m):02d}-{int(d):02d}"
            return dt, dt

        # 2. İki tam tarih aralığı (Örn: 11.07.2026 - 12.07.2026)
        dot_range_full = re.search(r"(\d{1,2})[./](\d{1,2})[./](\d{4})\s*(?:-|–|/|\s+to\s+)\s*(\d{1,2})[./](\d{1,2})[./](\d{4})", text)
        if dot_range_full:
            d1, m1, y1, d2, m2, y2 = dot_range_full.groups()
            return f"{y1}-{int(m1):02d}-{int(d1):02d}", f"{y2}-{int(m2):02d}-{int(d2):02d}"

        # 3a. İki gün+ay aralığı (Örn: 11.07. - 12.07.2026 veya 11/07 - 12/07/2026)
        dot_range_dm = re.search(r"(\d{1,2})[./](\d{1,2})\.?\s*(?:-|–|/|\s+to\s+)\s*(\d{1,2})[./](\d{1,2})[./](\d{4})", text)
        if dot_range_dm:
            d1, m1, d2, m2, y = dot_range_dm.groups()
            return f"{y}-{int(m1):02d}-{int(d1):02d}", f"{y}-{int(m2):02d}-{int(d2):02d}"

        # 3b. Kısa gün aralığı (Örn: 11-12.07.2026 veya 11.-12.07.2026 veya 11./12.07.2026)
        dot_range_short = re.search(r"(\d{1,2})\.?\s*(?:-|–|/)\s*(\d{1,2})[./](\d{1,2})[./](\d{4})", text)
        if dot_range_short:
            d1, d2, m, y = dot_range_short.groups()
            return f"{y}-{int(m):02d}-{int(d1):02d}", f"{y}-{int(m):02d}-{int(d2):02d}"

        # 4. Tek tarih DD.MM.YYYY veya DD/MM/YYYY
        dot_match = re.search(r"(\d{1,2})[./](\d{1,2})[./](\d{4})", text)
        if dot_match:
            d, m, y = dot_match.groups()
            dt = f"{y}-{int(m):02d}-{int(d):02d}"
            return dt, dt

        # 5. Çok dilli ay isimleri (EN, FR, DE, TR, ES, IT, PT, NL, PL)
        months = {
            "jan": 1, "janvier": 1, "januar": 1, "january": 1, "ocak": 1, "ene": 1, "enero": 1, "gen": 1, "gennaio": 1, "janeiro": 1, "styczeń": 1, "stycznia": 1,
            "feb": 2, "février": 2, "fevrier": 2, "fév": 2, "fev": 2, "februar": 2, "february": 2, "şubat": 2, "subat": 2, "febrero": 2, "febbraio": 2, "fevereiro": 2, "luty": 2, "lutego": 2,
            "mar": 3, "mars": 3, "märz": 3, "maerz": 3, "march": 3, "mart": 3, "marzo": 3, "março": 3, "marco": 3, "maart": 3, "marzec": 3, "marca": 3,
            "apr": 4, "avril": 4, "april": 4, "nisan": 4, "abr": 4, "abril": 4, "aprile": 4, "kwiecień": 4, "kwietnia": 4,
            "may": 5, "mai": 5, "mayıs": 5, "mayis": 5, "mayo": 5, "mag": 5, "maggio": 5, "maio": 5, "mei": 5, "maj": 5, "maja": 5,
            "jun": 6, "juin": 6, "juni": 6, "june": 6, "haziran": 6, "junio": 6, "giu": 6, "giugno": 6, "junho": 6, "czerwiec": 6, "czerwca": 6,
            "jul": 7, "juillet": 7, "juli": 7, "july": 7, "temmuz": 7, "julio": 7, "lug": 7, "luglio": 7, "julho": 7, "lipiec": 7, "lipca": 7,
            "aug": 8, "août": 8, "aout": 8, "august": 8, "ağustos": 8, "agustos": 8, "ago": 8, "agosto": 8, "augustus": 8, "sierpień": 8, "sierpnia": 8,
            "sep": 9, "sept": 9, "septembre": 9, "september": 9, "eylül": 9, "eylul": 9, "set": 9, "septiembre": 9, "settembre": 9, "setembro": 9, "wrzesień": 9, "września": 9,
            "oct": 10, "octobre": 10, "oktober": 10, "october": 10, "ekim": 10, "octubre": 10, "ott": 10, "ottobre": 10, "out": 10, "outubro": 10, "październik": 10, "października": 10,
            "nov": 11, "novembre": 11, "november": 11, "kasım": 11, "kasim": 11, "noviembre": 11, "novembro": 11, "listopad": 11, "listopada": 11,
            "dec": 12, "décembre": 12, "decembre": 12, "déc": 12, "dez": 12, "dezember": 12, "december": 12, "aralık": 12, "aralik": 12, "dic": 12, "diciembre": 12, "dicembre": 12, "dezembro": 12, "grudzień": 12, "grudnia": 12
        }

        def resolve_month(m_raw: Optional[str]) -> Optional[int]:
            if not m_raw:
                return None
            m_clean = m_raw.lower().strip(".")
            if m_clean in months:
                return months[m_clean]
            if m_clean[:3] in months:
                return months[m_clean[:3]]
            return None

        text_clean = text.replace("\xa0", " ").replace("&nbsp;", " ")
        text_clean = re.sub(r"\b1er\b", "1", text_clean, flags=re.IGNORECASE)

        range_match = re.search(
            r"(\d{1,2})\.?\s*([a-zA-ZçğıöşüéèêëàâäôöùûüńłśźżąćęÇĞİÖŞÜ]+)?\s*(?:/|-|–|\s+to\s+|\s+au\s+)\s*(\d{1,2})\.?\s*([a-zA-ZçğıöşüéèêëàâäôöùûüńłśźżąćęÇĞİÖŞÜ]+)",
            text_clean
        )
        if range_match:
            d1 = int(range_match.group(1))
            m1_raw = range_match.group(2)
            d2 = int(range_match.group(3))
            m2_raw = range_match.group(4)

            m2_num = resolve_month(m2_raw)
            m1_num = resolve_month(m1_raw) or m2_num

            if m1_num and m2_num:
                y = explicit_year
                return f"{y}-{m1_num:02d}-{d1:02d}", f"{y}-{m2_num:02d}-{d2:02d}"

        single_match = re.search(
            r"(\d{1,2})\.?\s*(?:-|–|/|\s+to\s+|\s+au\s+)?\s*(\d{1,2})?\.?\s+([a-zA-ZçğıöşüéèêëàâäôöùûüńłśźżąćęÇĞİÖŞÜ]+)\s*(\d{4})?",
            text_clean
        )
        if single_match:
            m_num = resolve_month(single_match.group(3))
            if m_num:
                d1 = int(single_match.group(1))
                d2 = int(single_match.group(2)) if single_match.group(2) else d1
                y = int(single_match.group(4)) if single_match.group(4) else explicit_year
                return f"{y}-{m_num:02d}-{d1:02d}", f"{y}-{m_num:02d}-{d2:02d}"

        return None, None

    def _extract_city(self, text: str) -> str:
        """Metin içinden bilinen atletizm şehirlerini filtreler."""
        common_cities = [
            "İstanbul", "İzmir", "Ankara", "Bursa", "Antalya", "Balıkesir", "Mersin",
            "Eskişehir", "Konya", "Kütahya", "Adana", "Trabzon", "Samsun", "Denizli", "Aydın", "Kuşadası", "Kocaeli",
            "Dortmund", "Chemnitz", "Halle", "Erfurt", "München", "Frankfurt", "Berlin",
            "Miramas", "Nantes", "Lyon", "Paris", "Tours", "Saint-Brieuc", "Saint Brieuc", "Épinal", "Epinal", "Châteauroux", "Chateauroux", "Juvaincourt", "Belfort",
            "Toruń", "Torun", "Włocławek", "Warszawa",
            "Catania", "Ojós", "Ojos", "Jyväskylä", "Jyvaskyla", "Lucerne", "Rasnov", "Râșnov",
            "Glasgow", "Galway", "Craiova", "Novo Mesto", "Varaždin", "Varazdin",
            "Belgrad", "Kruševac", "Sofya", "Bükreş", "Selanik", "Atina",
            "Kansai", "Taipei", "Daegu", "Sheffield", "Birmingham"
        ]
        for c in common_cities:
            if c.lower() in text.lower():
                return c
        return ""

    def detect_scope(self, title: str, country: str = "") -> str:
        """
        Yarışma başlığına ve ülkesine göre 6 standart kapsamdan (scope) birini belirler:
        - 'ema_league' -> EMA Ligi
        - 'world'      -> Dünya
        - 'balkan'     -> Balkan
        - 'europe'     -> Avrupa
        - 'regional'   -> Bölgesel
        - 'national'   -> Ulusal
        """
        t_low = title.lower()
        if "ema league" in t_low or "ema ligi" in t_low:
            return "ema_league"
        if any(kw in t_low for kw in ["world", "wma", "wmg", "kansai", "weltmeisterschaft", "mondiaux", "mundial"]):
            return "world"
        if any(kw in t_low for kw in ["balkan", "bamacs", "bamaci"]):
            return "balkan"
        if any(kw in t_low for kw in [
            "emacs", "emaci", "emamrc", "emacns", "emmtrc", "emorrc",
            "european masters championship", "european masters athletics championship",
            "european masters mountain", "european masters marathon",
            "european masters non-stadia", "european masters indoor",
            "europameisterschaft", "championnats d'europe"
        ]):
            return "europe"
        if any(kw in t_low for kw in [
            "meeting", "open", "grand prix", "track night", "challenge", "trophy",
            "bw-", "baden", "norddeutsch", "westfal", "bayern", "hessen", "rheinland",
            "landes", "bezirks", "kreis", "régional", "regional", "bölge"
        ]):
            return "regional"
        return "national"

