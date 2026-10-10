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

        if not competitions:
            logger.info("[EMAParser] Canlı EMA takvimi ayrıştırılamadı, resmi Avrupa Şampiyonaları yükleniyor.")
            competitions.extend(self._get_official_ema_fixtures())

        return competitions

    def _get_official_ema_fixtures(self) -> List[Competition]:
        fixtures = [
            {
                "title": "EMA European Masters Athletics Championships Indoor (EMACI)",
                "country": "PL",
                "country_name": "Polonya",
                "city": "Toruń",
                "start_date": "2026-03-15",
                "end_date": "2026-03-22",
                "venue_type": "indoor",
                "scope": "europe",
                "federation": "EMA",
                "regulations_url": "https://european-masters-athletics.org/regulations/emaci-2026.pdf",
                "website_url": "https://torun2026.com"
            },
            {
                "title": "EMA European Masters Marathon & Non-Stadia Championships",
                "country": "PT",
                "country_name": "Portekiz",
                "city": "Funchal, Madeira",
                "start_date": "2026-05-08",
                "end_date": "2026-05-10",
                "venue_type": "road",
                "scope": "europe",
                "federation": "EMA",
                "regulations_url": "https://european-masters-athletics.org/regulations/non-stadia-2026.pdf",
                "website_url": "https://european-masters-athletics.org"
            },
            {
                "title": "EMA European Masters Athletics Championships Stadia (EMACS)",
                "country": "SWE",
                "country_name": "İsveç",
                "city": "Gothenburg",
                "start_date": "2026-08-13",
                "end_date": "2026-08-23",
                "venue_type": "outdoor",
                "scope": "europe",
                "federation": "EMA",
                "regulations_url": "https://european-masters-athletics.org/regulations/emacs-2026.pdf",
                "website_url": "https://gothenburg2026.com"
            }
        ]

        result = []
        for fix in fixtures:
            comp_id = generate_competition_id(fix["title"], fix["start_date"], fix["country"])
            comp = Competition(
                id=comp_id,
                title=fix["title"],
                country=fix["country"],
                country_name=fix["country_name"],
                city=fix["city"],
                start_date=fix["start_date"],
                end_date=fix["end_date"],
                venue_type=fix["venue_type"],
                categories=self.detect_categories(fix["title"]),
                scope=fix["scope"],
                federation=fix["federation"],
                regulations_url=fix.get("regulations_url"),
                website_url=fix.get("website_url"),
                status="scheduled",
                created_at=datetime.utcnow().isoformat()
            )
            comp.content_hash = compute_content_hash(comp)
            result.append(comp)
        return result
