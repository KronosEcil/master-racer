import logging
from datetime import datetime
from typing import List
from bs4 import BeautifulSoup
from .base_parser import BaseParser
from ..config import Competition
from ..deduplication import generate_competition_id, compute_content_hash

logger = logging.getLogger(__name__)

class WMABalkanParser(BaseParser):
    """
    World Masters Athletics (WMA) ve Balkan Masters Athletics (BAMACS) Takvim Ayrıştırıcısı
    """
    def __init__(self):
        super().__init__(name="WMA_Balkan_Parser", base_url="https://world-masters-athletics.org")
        self.wma_calendar_url = "https://world-masters-athletics.org/competitions/"

    def parse(self) -> List[Competition]:
        competitions: List[Competition] = []
        html = self.fetch_html(self.wma_calendar_url)

        if html:
            soup = BeautifulSoup(html, "html.parser")
            items = soup.select(".event-card, .tribe-events-calendar-list__event, tr")
            for item in items:
                try:
                    title_elem = item.select_one("h3, h4, .tribe-events-calendar-list__event-title, td.title")
                    if not title_elem:
                        continue
                    title = title_elem.get_text(strip=True)
                    if not any(k in title.lower() for k in ["championship", "masters", "balkan"]):
                        continue

                    venue = self.detect_venue_type(title)
                    c_code, c_name, _ = self.detect_country(title)
                    comp_id = generate_competition_id(title, "2026-08-01", c_code)

                    comp = Competition(
                        id=comp_id,
                        title=title,
                        country=c_code,
                        country_name=c_name,
                        city="Kansai" if c_code == "JP" else "",
                        start_date="2026-08-01",
                        end_date="2026-08-10",
                        venue_type=venue,
                        categories=self.detect_categories(title),
                        scope="world",
                        federation="WMA",
                        website_url=self.base_url,
                        status="scheduled",
                        created_at=datetime.utcnow().isoformat()
                    )
                    comp.content_hash = compute_content_hash(comp)
                    competitions.append(comp)
                except Exception as e:
                    logger.debug(f"[WMABalkanParser] Parse hatası: {e}")

        return competitions
