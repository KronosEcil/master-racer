import abc
import logging
import requests
from typing import List, Optional
from datetime import datetime
from ..config import Competition, ScraperConfig

logger = logging.getLogger(__name__)

class BaseParser(abc.ABC):
    """
    Tüm federasyon takvim kazıyıcılarının türetildiği temel sınıf.
    """
    def __init__(self, name: str, base_url: str):
        self.name = name
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": ScraperConfig.USER_AGENT,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7"
        })

    def fetch_html(self, url: str, timeout: int = 15) -> Optional[str]:
        try:
            resp = self.session.get(url, timeout=timeout)
            if resp.status_code == 200:
                resp.encoding = resp.apparent_encoding or "utf-8"
                return resp.text
            logger.warning(f"[{self.name}] HTTP {resp.status_code} alındı: {url}")
        except Exception as e:
            logger.error(f"[{self.name}] İstek hatası ({url}): {e}")
        return None

    @abc.abstractmethod
    def parse(self) -> List[Competition]:
        """Tüm yarışmaları tarayıp Competition listesi döndürür."""
        pass

    def detect_venue_type(self, text: str) -> str:
        """Metin içerisindeki anahtar kelimelerden salon/açık/yol ayrımını yapar."""
        text_lower = text.lower()
        if any(w in text_lower for w in ["salon", "indoor", "kapalı"]):
            return "indoor"
        if any(w in text_lower for w in ["yol", "maraton", "yarı maraton", "road", "10k", "21k", "42k"]):
            return "road"
        if any(w in text_lower for w in ["kros", "cross", "cross country", "arazi"]):
            return "cross"
        return "outdoor"

    def detect_categories(self, text: str) -> List[str]:
        """Metne göre branş gruplarını sınıflandırır."""
        text_lower = text.lower()
        categories = []
        if any(w in text_lower for w in ["sprint", "koşu", "100m", "200m", "400m", "bayrak"]):
            categories.append("Sprint")
        if any(w in text_lower for w in ["engel", "hurdle", "engelli"]):
            categories.append("Engelli")
        if any(w in text_lower for w in ["atlama", "uzun atlama", "üçadım", "yüksek atlama", "sırık", "jump"]):
            categories.append("Atlamalar")
        if any(w in text_lower for w in ["atma", "gülle", "cirit", "disk", "çekiç", "throw", "shot put"]):
            categories.append("Atmalar")
        if any(w in text_lower for w in ["uzun mesafe", "5000m", "10000m", "maraton", "yol", "distance"]):
            categories.append("Uzun Mesafe")
        
        if not categories:
            # Standart tam şampiyona ise tüm branşları içerir
            categories = ["Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"]
        return categories
