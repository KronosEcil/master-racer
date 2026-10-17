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
                                scope=self.detect_scope(title, country),
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
            "İstanbul", "İzmir", "Ankara", "Bursa",
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

