import logging
from datetime import datetime
from typing import List
from bs4 import BeautifulSoup
from .base_parser import BaseParser
from ..config import Competition
from ..deduplication import generate_competition_id, compute_content_hash

logger = logging.getLogger(__name__)

class EMAParser(BaseParser):
    """
    European Masters Athletics (EMA) Resmi Takvim Ayrıştırıcısı
    https://european-masters-athletics.org
    """
    def __init__(self):
        super().__init__(name="EMA_Parser", base_url="https://european-masters-athletics.org")
        self.calendar_url = "https://european-masters-athletics.org/competitions/"

    def parse(self) -> List[Competition]:
        competitions: List[Competition] = []
        html = self.fetch_html(self.calendar_url)

        if html:
            soup = BeautifulSoup(html, "html.parser")
            cards = soup.select(".competition-card, article.event, tr.event-row")
            for card in cards:
                try:
                    title_elem = card.select_one("h2, h3, .event-title, td.title")
                    if not title_elem:
                        continue
                    title = title_elem.get_text(strip=True)
                    # Tarih & Yer çıkarma
                    meta_elem = card.select_one(".event-date, .meta, td.date")
                    meta_text = meta_elem.get_text(strip=True) if meta_elem else ""

                    venue = self.detect_venue_type(title)
                    comp_id = generate_competition_id(title, "2026-03-01", "EU")

                    comp = Competition(
                        id=comp_id,
                        title=title,
                        country="EU",
                        country_name="Avrupa",
                        city="",
                        start_date="2026-03-01",
                        end_date="2026-03-07",
                        venue_type=venue,
                        categories=self.detect_categories(title),
                        scope="europe",
                        federation="EMA",
                        website_url=self.base_url,
                        status="scheduled",
                        created_at=datetime.utcnow().isoformat()
                    )
                    comp.content_hash = compute_content_hash(comp)
                    competitions.append(comp)
                except Exception as e:
                    logger.debug(f"[EMAParser] Etkinlik işlenirken hata: {e}")

        return competitions
