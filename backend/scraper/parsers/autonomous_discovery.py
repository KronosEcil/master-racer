"""
Master Racer - Autonomous Dynamic Discovery Engine
Herhangi bir statik listeye bağımlı kalmadan; federasyonların dinamik arama motorlarını,
yıl bazlı takvim filtrelerini (2026, 2027, 2028) ve açık atletizm API/veri beslemelerini
otonom olarak tarayan ve yeni yarışmaları keşfeden dinamik tarayıcı.
"""

import re
import json
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
    bölgesel salon meetinglerini ve uluslararası master takvimlerini otonom tarayan motor.
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

        # 5. Türkiye ve Balkan Federasyonları Dinamik Portalları
        discovered.extend(self._scrape_balkan_and_turkey())

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
                                scope="regional",
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
        Fransa Atletizm Federasyonu (FFA) ve Salon Meetingleri dinamik arama motoru.
        """
        results: List[Competition] = []
        for year in self.years:
            urls = [
                f"https://bases.athle.fr/asp.net/listeepreuves.aspx?annee={year}&famille=salle",
                f"https://bases.athle.fr/asp.net/listeepreuves.aspx?annee={year}&categorie=MA",
                f"https://www.athle.fr/calendrier?year={year}"
            ]
            for url in urls:
                try:
                    html = self.fetch_html(url, timeout=10)
                    if not html:
                        continue
                    soup = BeautifulSoup(html, "html.parser")
                    rows = soup.select("table tr, .epreuve-item, article")
                    for row in rows:
                        text = row.get_text(separator=" ", strip=True)
                        text_lower = text.lower()
                        # Fransa için sıkı Master (35+) filtreleme:
                        has_french_master = any(kw in text_lower for kw in ["master", "vétéran", "veteran", "35+", "ema league", "européen masters"])
                        if has_french_master:
                            title_elem = row.select_one("a, .nom, strong, h3")
                            title = title_elem.get_text(strip=True) if title_elem else text[:60]
                            start_date, end_date = self._extract_dates_from_text(text, year)
                            if not start_date:
                                continue
                            comp_id = generate_competition_id(title, start_date, "FR")
                            comp = Competition(
                                id=comp_id,
                                title=title,
                                country="FR",
                                country_name="Fransa",
                                city=self._extract_city(text) or "Fransa",
                                start_date=start_date,
                                end_date=end_date,
                                venue_type=self.detect_venue_type(title + " " + text),
                                categories=self.detect_categories(title + " " + text, self.detect_venue_type(title + " " + text)),
                                scope="regional",
                                federation="FFA",
                                website_url="https://bases.athle.fr",
                                status="scheduled",
                                created_at=datetime.utcnow().isoformat()
                            )
                            comp.content_hash = compute_content_hash(comp)
                            results.append(comp)
                except Exception as e:
                    logger.debug(f"[FFA-France] Hata ({url}): {e}")
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
                            scope="europe",
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

                                country, country_name, _ = self.detect_country(title, raw_country, raw_cname)
                                if country == "JP" and not city:
                                    city = "Kansai"

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
                                    scope="world" if "world" in title.lower() else "europe",
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
                                        scope="europe",
                                        federation="EMA",
                                        website_url=web_url,
                                        status="scheduled",
                                        created_at=datetime.utcnow().isoformat()
                                    )
                                    comp.content_hash = compute_content_hash(comp)
                                    results.append(comp)
                except Exception as e:
                    logger.debug(f"[EMA-Intl] Hata ({url}): {e}")
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

        # 2. Türkiye Atletizm Federasyonu (TAF) Dinamik Faaliyet Takvimi
        for year in self.years:
            taf_url = f"https://www.taf.org.tr/faaliyet-takvimi/?yil={year}"
            try:
                resp = self.session.get(taf_url, timeout=12)
                if resp.status_code == 200:
                    soup = BeautifulSoup(resp.text, "html.parser")
                    rows = soup.select("table tr, .event-item, article")
                    for row in rows:
                        text = row.get_text(separator=" ", strip=True)
                        if any(kw in text.lower() for kw in ["master", "veteran", "kulüpler ligi"]):
                            title_elem = row.select_one("a, strong, h3, h4")
                            title = title_elem.get_text(strip=True) if title_elem else text[:60]
                            if not is_valid_competition_title(title):
                                continue
                            start_date, end_date = self._extract_dates_from_text(text, year)
                            if not start_date:
                                continue
                            city = self._extract_city(text) or self._extract_city(title)
                            country, country_name, _ = self.detect_country(title + " " + text, "TR", "Türkiye")
                            venue = self.detect_venue_type(title + " " + text)
                            comp_id = generate_competition_id(title, start_date, country)
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
                                scope="turkey",
                                federation="TAF",
                                website_url=taf_url,
                                status="scheduled",
                                created_at=datetime.utcnow().isoformat()
                            )
                            comp.content_hash = compute_content_hash(comp)
                            results.append(comp)
            except Exception as e:
                logger.debug(f"[TAF] Hata ({taf_url}): {e}")

        return results

    def _extract_dates_from_text(self, text: str, fallback_year: int) -> tuple[Optional[str], Optional[str]]:
        """
        Metin içinden GG.AA.YYYY veya YYYY-AA-GG veya metinsel tarih formatlarını çıkarır.
        """
        # 0. Yıl belirleme: Metinde geçen açık 4 haneli yıl (Örn: 2027, EMACS2027, EMAMRC2027)
        # veya şampiyona kısaltmasındaki 2 haneli yıl (Örn: EMACNS28 -> 2028, EMACI28 -> 2028)
        explicit_year = fallback_year
        four_digit_match = re.search(r"202[5-9]", text)
        if four_digit_match:
            explicit_year = int(four_digit_match.group(0))
        else:
            two_digit_match = re.search(r"\b[A-Za-z]+(2[5-9])\b", text)
            if two_digit_match:
                explicit_year = 2000 + int(two_digit_match.group(1))

        # 1. YYYY-MM-DD
        iso_match = re.search(r"(\d{4})-(\d{1,2})-(\d{1,2})", text)
        if iso_match:
            y, m, d = iso_match.groups()
            dt = f"{y}-{int(m):02d}-{int(d):02d}"
            return dt, dt

        # 2. DD.MM.YYYY veya DD/MM/YYYY
        dot_match = re.search(r"(\d{1,2})[./](\d{1,2})[./](\d{4})", text)
        if dot_match:
            d, m, y = dot_match.groups()
            dt = f"{y}-{int(m):02d}-{int(d):02d}"
            return dt, dt

        # 3. Yıl ve ay isimleri (Ocak, Şubat, Januar, Février, January, April, August vb.)
        months = {
            "jan": 1, "ocak": 1, "januar": 1, "janvier": 1, "ene": 1,
            "feb": 2, "şubat": 2, "subat": 2, "februar": 2, "février": 2, "fev": 2,
            "mar": 3, "mart": 3, "märz": 3, "mars": 3, "marzo": 3,
            "apr": 4, "nisan": 4, "april": 4, "avril": 4, "abr": 4,
            "may": 5, "mayıs": 5, "mai": 5, "mayo": 5,
            "jun": 6, "haziran": 6, "juni": 6, "juin": 6, "junio": 6,
            "jul": 7, "temmuz": 7, "juli": 7, "juillet": 7, "julio": 7,
            "aug": 8, "ağustos": 8, "agustos": 8, "août": 8, "agosto": 8,
            "sep": 9, "eylül": 9, "eylul": 9, "sept": 9, "septiembre": 9,
            "oct": 10, "ekim": 10, "oktober": 10, "octobre": 10, "octubre": 10,
            "nov": 11, "kasım": 11, "kasim": 11, "novembre": 11, "noviembre": 11,
            "dec": 12, "aralık": 12, "aralik": 12, "dezember": 12, "décembre": 12, "diciembre": 12
        }

        # İki tarih arası aralık: Örn: "11 August/22 August", "18 May/21 May", "15 April/18 Spril", "11 - 22 August"
        range_match = re.search(r"(\d{1,2})\.?\s*([a-zA-Zçğıöşü]+)?\s*(?:/|-|–|\s+to\s+)\s*(\d{1,2})\.?\s*([a-zA-Zçğıöşü]+)", text)
        if range_match:
            d1 = int(range_match.group(1))
            m1_str = (range_match.group(2) or "").lower()[:3]
            d2 = int(range_match.group(3))
            m2_str = (range_match.group(4) or "").lower()[:3]
            
            m2_num = months.get(m2_str)
            # Eğer m1 belirtilmemişse veya typo ise (ör. 18 Spril) m2_num al
            m1_num = months.get(m1_str) if m1_str in months else m2_num

            if m1_num and m2_num:
                y = explicit_year
                return f"{y}-{m1_num:02d}-{d1:02d}", f"{y}-{m2_num:02d}-{d2:02d}"

        # Tek tarih veya genel ay tarama: Örn: 15. April veya 31. October
        for m_name, m_num in months.items():
            pattern = rf"(\d{{1,2}})\s*(?:-|–|\.|/|\s*to\s*)?\s*(\d{{1,2}})?\s*\b{m_name}[a-z]*\b\s*(\d{{4}})?"
            match = re.search(pattern, text.lower())
            if match:
                d1 = int(match.group(1))
                d2 = int(match.group(2)) if match.group(2) else d1
                
                if match.group(3):
                    y = int(match.group(3))
                else:
                    y = explicit_year
                return f"{y}-{m_num:02d}-{d1:02d}", f"{y}-{m_num:02d}-{d2:02d}"

        return None, None

    def _extract_city(self, text: str) -> str:
        """Metin içinden bilinen atletizm şehirlerini filtreler."""
        common_cities = [
            "İstanbul", "İzmir", "Ankara", "Bursa",
            "Dortmund", "Chemnitz", "Halle", "Erfurt", "München", "Frankfurt", "Berlin",
            "Miramas", "Nantes", "Lyon", "Paris", "Tours",
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
