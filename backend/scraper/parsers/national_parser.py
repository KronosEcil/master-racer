import logging
from datetime import datetime
from typing import List
from bs4 import BeautifulSoup

from .base_parser import BaseParser
from .national_federations import NATIONAL_FEDERATIONS
from ..config import Competition
from ..deduplication import generate_competition_id, compute_content_hash

logger = logging.getLogger(__name__)

class NationalFederationsParser(BaseParser):
    """
    Tüm Ulusal Atletizm Federasyonlarının (Yunanistan, Almanya, İtalya, İngiltere,
    İspanya, Fransa, Polonya, Balkan ülkeleri vb.) resmi web sayfalarını tarayan motor.
    """
    def __init__(self):
        super().__init__(name="NationalFederations_Parser", base_url="https://worldathletics.org")

    def parse(self) -> List[Competition]:
        all_competitions: List[Competition] = []

        for fed in NATIONAL_FEDERATIONS:
            country_code = fed["country_code"]
            country_name = fed["country_name"]
            fed_name = fed["federation_name"]
            calendar_url = fed["calendar_url"]
            keywords = fed["keywords"]

            logger.info(f"[{fed_name} - {country_name}] Resmi takvim taranıyor: {calendar_url}")
            fed_comps: List[Competition] = []

            # 1. Canlı Web Sayfasını Tara
            html = self.fetch_html(calendar_url)
            if html:
                try:
                    soup = BeautifulSoup(html, "html.parser")
                    # Genel tablo satırları, kartlar veya liste öğeleri
                    elements = soup.select("table tr, .event-item, .calendar-event, article, .competition")
                    for elem in elements:
                        text = elem.get_text(separator=" ", strip=True).lower()
                        if any(kw in text for kw in keywords):
                            title_elem = elem.select_one("h2, h3, h4, a, strong, td.title")
                            title = title_elem.get_text(strip=True) if title_elem else elem.get_text(strip=True)[:60]

                            venue = self.detect_venue_type(title)
                            comp_id = generate_competition_id(title, "2026-06-01", country_code)

                            comp = Competition(
                                id=comp_id,
                                title=title,
                                country=country_code,
                                country_name=country_name,
                                city="",
                                start_date="2026-06-01",
                                end_date="2026-06-02",
                                venue_type=venue,
                                categories=self.detect_categories(title),
                                scope=fed.get("scope", "regional"),
                                federation=fed_name,
                                website_url=fed["base_url"],
                                status="scheduled",
                                created_at=datetime.utcnow().isoformat()
                            )
                            comp.content_hash = compute_content_hash(comp)
                            fed_comps.append(comp)
                except Exception as e:
                    logger.debug(f"[{fed_name}] Canlı sayfa ayrıştırılırken hata: {e}")

            # Sadece canlı sayfadan taranan gerçek yarışmalar eklenir

            all_competitions.extend(fed_comps)
            logger.info(f"[{fed_name} - {country_name}] Toplam {len(fed_comps)} yarışma eklendi.")

        return all_competitions
