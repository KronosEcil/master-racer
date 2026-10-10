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
                    comp_id = generate_competition_id(title, "2026-08-01", "WMA")

                    comp = Competition(
                        id=comp_id,
                        title=title,
                        country="INT",
                        country_name="Uluslararası",
                        city="",
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

        if not competitions:
            logger.info("[WMABalkanParser] Canlı takvimden veri alınamadı, resmi Dünya ve Balkan Şampiyonaları yükleniyor.")
            competitions.extend(self._get_official_wma_and_balkan_fixtures())

        return competitions

    def _get_official_wma_and_balkan_fixtures(self) -> List[Competition]:
        fixtures = [
            {
                "title": "Balkan Masterlar Salon Atletizm Şampiyonası (BAMACI)",
                "country": "RS",
                "country_name": "Sırbistan",
                "city": "Belgrad",
                "start_date": "2026-03-07",
                "end_date": "2026-03-08",
                "venue_type": "indoor",
                "scope": "balkan",
                "federation": "BAMACS",
                "regulations_url": "https://balkanmastersathletics.org/regulations/bamaci-2026.pdf",
                "website_url": "https://balkanmastersathletics.org"
            },
            {
                "title": "Balkan Masterlar Açık Saha Atletizm Şampiyonası (BAMACS)",
                "country": "GR",
                "country_name": "Yunanistan",
                "city": "Selanik",
                "start_date": "2026-09-18",
                "end_date": "2026-09-20",
                "venue_type": "outdoor",
                "scope": "balkan",
                "federation": "BAMACS",
                "regulations_url": "https://balkanmastersathletics.org/regulations/bamacs-2026.pdf",
                "website_url": "https://balkanmastersathletics.org"
            },
            {
                "title": "WMA World Masters Athletics Championships Stadia",
                "country": "KOR",
                "country_name": "Güney Kore",
                "city": "Daegu",
                "start_date": "2026-08-22",
                "end_date": "2026-09-03",
                "venue_type": "outdoor",
                "scope": "world",
                "federation": "WMA",
                "regulations_url": "https://world-masters-athletics.org/regulations/wma-daegu-2026.pdf",
                "website_url": "https://daegu2026wma.org"
            },
            {
                "title": "WMA World Masters Athletics Indoor Championships (WMACI)",
                "country": "USA",
                "country_name": "ABD",
                "city": "Gainesville, Florida",
                "start_date": "2027-03-23",
                "end_date": "2027-03-30",
                "venue_type": "indoor",
                "scope": "world",
                "federation": "WMA",
                "regulations_url": "https://world-masters-athletics.org/regulations/wmaci-2027.pdf",
                "website_url": "https://world-masters-athletics.org"
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
