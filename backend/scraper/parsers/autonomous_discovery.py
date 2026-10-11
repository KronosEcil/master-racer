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
from ..deduplication import generate_competition_id, compute_content_hash

logger = logging.getLogger(__name__)

class AutonomousDiscoveryParser(BaseParser):
    """
    Dinamik yıl döngüsü (2026, 2027) ile federasyon arama motorlarını,
    bölgesel salon meetinglerini ve uluslararası master takvimlerini otonom tarayan motor.
    """
    def __init__(self):
        super().__init__(name="AutonomousDiscovery_Parser", base_url="https://worldathletics.org")
        self.years = [2026, 2027]

    def parse(self) -> List[Competition]:
        discovered: List[Competition] = []
        logger.info(f"Otonom Dinamik Tarayıcı başlatılıyor (Yıllar: {self.years})...")

        # 1. Almanya & Orta Avrupa (LADV ve DLV Dinamik Arama Motoru)
        discovered.extend(self._scrape_ladv_and_dlv())

        # 2. Fransa & Güney Avrupa (FFA Bases ve Salon Meetingleri)
        discovered.extend(self._scrape_ffa_france())

        # 3. Avrupa & Uluslararası Masterlar (EMA Circuit, OpenTrack ve WMA)
        discovered.extend(self._scrape_international_and_opentrack())

        # 4. Türkiye ve Balkan Federasyonları Dinamik Portalları
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
                            
                            # Tarih yakalama (Örn: 23.01.2027 veya 2027-01-23)
                            start_date, end_date = self._extract_dates_from_text(text, year)
                            if not start_date:
                                continue
                                
                            city = self._extract_city(text)
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
                f"https://imga.ch/events/{year}",
                f"https://european-masters-athletics.org/championships-{year}/",
                f"https://european-masters-athletics.org/events/",
                f"https://european-masters-athletics.org/competitions/",
                f"https://european-masters-athletics.org/ema-league-{year}/"
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
                                s_date = item.get("date_start", item.get("start_date", f"{year}-06-01"))
                                e_date = item.get("date_end", item.get("end_date", s_date))
                                raw_country = item.get("country_code", "INT")
                                raw_cname = item.get("country", "Uluslararası")
                                city = item.get("city", "")

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
                            
                            # EMA özel blokları: Gutenberg media-text, tablo hücreleri (td), article ve event kartları
                            items = soup.select(".wp-block-media-text, table td, .event-card, .competition-card, article, tr")
                            for item in items:
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
                                    
                                    start_date, end_date = self._extract_dates_from_text(text, year)
                                    if not start_date:
                                        continue
                                    
                                    city = self._extract_city(text) or self._extract_city(title)
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
        Balkan ve Türkiye federasyonlarının arama portalları.
        """
        results: List[Competition] = []
        for year in self.years:
            urls = [
                f"https://balkanmastersathletics.org/calendar-{year}/",
                f"https://www.taf.org.tr/faaliyet-takvimi/?yil={year}"
            ]
            for url in urls:
                html = self.fetch_html(url, timeout=10)
                if not html:
                    continue
                soup = BeautifulSoup(html, "html.parser")
                rows = soup.select("table tr, .event-item, article")
                for row in rows:
                    text = row.get_text(separator=" ", strip=True)
                    if any(kw in text.lower() for kw in ["master", "veteran", "bamacs", "bamaci"]):
                        title_elem = row.select_one("a, strong, h3, h4")
                        title = title_elem.get_text(strip=True) if title_elem else text[:60]
                        start_date, end_date = self._extract_dates_from_text(text, year)
                        if not start_date:
                            continue
                        comp_id = generate_competition_id(title, start_date, "TR" if "taf" in url else "BAMACS")
                        venue = self.detect_venue_type(title)
                        comp = Competition(
                            id=comp_id,
                            title=title,
                            country="TR" if "taf" in url else "EU",
                            country_name="Türkiye" if "taf" in url else "Balkan",
                            city=self._extract_city(text) or "Balkan",
                            start_date=start_date,
                            end_date=end_date,
                            venue_type=venue,
                            categories=self.detect_categories(title, venue),
                            scope="balkan" if "bamac" in title.lower() else "turkey",
                            federation="BAMACS" if "bamac" in title.lower() else "TAF",
                            website_url=url,
                            status="scheduled",
                            created_at=datetime.utcnow().isoformat()
                        )
                        comp.content_hash = compute_content_hash(comp)
                        results.append(comp)
        return results

    def _extract_dates_from_text(self, text: str, fallback_year: int) -> tuple[Optional[str], Optional[str]]:
        """
        Metin içinden GG.AA.YYYY veya YYYY-AA-GG veya metinsel tarih formatlarını çıkarır.
        """
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

        # İki farklı ay içeren aralık: Örn: 26. March – 2. April 2026 veya 15 April / 18 April
        # Önce ay bazlı tarama
        for m_name, m_num in months.items():
            pattern = rf"(\d{{1,2}})\s*(?:-|–|\.|/)?\s*(\d{{1,2}})?\s*{m_name}[a-z]*\s*({fallback_year})?"
            match = re.search(pattern, text.lower())
            if match:
                d1 = int(match.group(1))
                d2 = int(match.group(2)) if match.group(2) else d1
                y = int(match.group(3)) if match.group(3) else fallback_year
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
            "Glasgow", "Galway",
            "Belgrad", "Kruševac", "Sofya", "Bükreş", "Selanik", "Atina",
            "Kansai", "Taipei", "Daegu", "Sheffield", "Birmingham"
        ]
        for c in common_cities:
            if c.lower() in text.lower():
                return c
        return ""
