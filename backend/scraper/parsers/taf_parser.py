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
        match = re.search(r"(\d{1,2})[.-/](\d{1,2})[.-/](\d{4})", date_str)
        if match:
            day, month, year = match.groups()
            d = f"{year}-{int(month):02d}-{int(day):02d}"
            return d, d
        now = datetime.utcnow().strftime("%Y-%m-%d")
        return now, now
