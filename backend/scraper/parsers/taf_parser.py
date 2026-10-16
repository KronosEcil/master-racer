import logging
import re
from datetime import datetime
from typing import List
from bs4 import BeautifulSoup
from .base_parser import BaseParser
from ..config import Competition
from ..deduplication import generate_competition_id, compute_content_hash

logger = logging.getLogger(__name__)

class TAFParser(BaseParser):
    """
    Türkiye Atletizm Federasyonu (TAF) ve Türkiye Masterlar Takvim Ayrıştırıcısı
    """
    def __init__(self):
        super().__init__(name="TAF_Parser", base_url="https://taf.org.tr")
        self.calendar_urls = [
            "https://taf.org.tr/faaliyet-takvimi/",
            "https://www.taf.org.tr/faaliyet-takvimi/",
            "https://taf.org.tr/yarisma-programi/",
            "https://taf.org.tr/yarisma-takvimi/"
        ]

    def parse(self) -> List[Competition]:
        competitions: List[Competition] = []
        html = None
        for url in self.calendar_urls:
            html = self.fetch_html(url)
            if html:
                break

        if html:
            soup = BeautifulSoup(html, "html.parser")
            # TAF web sitesi takvim tablosu tarama
            rows = soup.select("table.table tr, .calendar-item, .yarisma-satir")
            for row in rows:
                try:
                    text = row.get_text(separator=" ", strip=True)
                    if not any(k in text.lower() for k in ["master", "veteran", "büyükler"]):
                        continue

                    cols = row.find_all("td")
                    if len(cols) >= 3:
                        title = cols[1].get_text(strip=True)
                        date_str = cols[0].get_text(strip=True)
                        city = cols[2].get_text(strip=True) if len(cols) > 2 else "Türkiye"
                    else:
                        title = text
                        date_str = ""
                        city = "Türkiye"

                    start_date, end_date = self._parse_dates(date_str)
                    venue = self.detect_venue_type(title)
                    comp_id = generate_competition_id(title, start_date, "TR")

                    comp = Competition(
                        id=comp_id,
                        title=title,
                        country="TR",
                        country_name="Türkiye",
                        city=city,
                        start_date=start_date,
                        end_date=end_date,
                        venue_type=venue,
                        categories=self.detect_categories(title),
                        scope="turkey",
                        federation="TAF",
                        website_url="https://taf.org.tr",
                        status="scheduled",
                        created_at=datetime.utcnow().isoformat()
                    )
                    comp.content_hash = compute_content_hash(comp)
                    competitions.append(comp)
                except Exception as e:
                    logger.debug(f"[TAFParser] Satır işlenirken hata: {e}")

        return competitions

    def _parse_dates(self, date_str: str) -> tuple[str, str]:
        months = {
            "ocak": 1, "subat": 2, "şubat": 2, "mart": 3, "nisan": 4, "mayis": 5, "mayıs": 5,
            "haziran": 6, "temmuz": 7, "agustos": 8, "ağustos": 8, "eylul": 9, "eylül": 9,
            "ekim": 10, "kasim": 11, "kasım": 11, "aralik": 12, "aralık": 12
        }

        # 1. Tam iki tarih aralığı (Örn: 11.07.2026 - 12.07.2026)
        range_full = re.search(r"(\d{1,2})[.-/](\d{1,2})[.-/](\d{4})\s*(?:-|–|/|\s+ila\s+|\s+to\s+)\s*(\d{1,2})[.-/](\d{1,2})[.-/](\d{4})", date_str)
        if range_full:
            d1, m1, y1, d2, m2, y2 = range_full.groups()
            return f"{y1}-{int(m1):02d}-{int(d1):02d}", f"{y2}-{int(m2):02d}-{int(d2):02d}"

        # 2. Gün aralığı ve nokta/çizgi (Örn: 11-12.07.2026 veya 11 - 12.07.2026)
        range_short = re.search(r"(\d{1,2})\s*(?:-|–)\s*(\d{1,2})[.-/](\d{1,2})[.-/](\d{4})", date_str)
        if range_short:
            d1, d2, m, y = range_short.groups()
            return f"{y}-{int(m):02d}-{int(d1):02d}", f"{y}-{int(m):02d}-{int(d2):02d}"

        # 3. Metinsel Türkçe ay aralıkları (Örn: 11-12 Temmuz 2026 veya 11 - 12 TEMMUZ 2026)
        text_lower = date_str.lower()
        for m_name, m_num in months.items():
            text_range = re.search(rf"(\d{{1,2}})\s*(?:-|–|\s+ila\s+)\s*(\d{{1,2}})\s+{m_name}\s*(\d{{4}})?", text_lower)
            if text_range:
                d1 = int(text_range.group(1))
                d2 = int(text_range.group(2))
                y = text_range.group(3) or datetime.utcnow().strftime("%Y")
                return f"{y}-{m_num:02d}-{d1:02d}", f"{y}-{m_num:02d}-{d2:02d}"

            text_single = re.search(rf"(\d{{1,2}})\s+{m_name}\s*(\d{{4}})?", text_lower)
            if text_single:
                d = int(text_single.group(1))
                y = text_single.group(2) or datetime.utcnow().strftime("%Y")
                dt = f"{y}-{m_num:02d}-{d:02d}"
                return dt, dt

        # 4. Tek sayısal tarih (Örn: 11.07.2026)
        match = re.search(r"(\d{1,2})[.-/](\d{1,2})[.-/](\d{4})", date_str)
        if match:
            day, month, year = match.groups()
            d = f"{year}-{int(month):02d}-{int(day):02d}"
            return d, d

        now = datetime.utcnow().strftime("%Y-%m-%d")
        return now, now
