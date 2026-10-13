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

    def detect_country(self, title: str, default_country: str = "INT", default_name: str = "Uluslararası") -> tuple[str, str, str]:
        """
        Yarışma başlığından ve metninden ev sahibi ülkeyi, ülke adını ve bayrağını belirler.
        Tüm tespitler GitHub scraper motorunda otomatik yapılır.
        """
        def_upper = default_country.upper()
        def_name_lower = default_name.lower()
        if def_upper in ["AL", "ALB"] or def_name_lower in ["albania", "arnavutluk"]:
            return "AL", "Arnavutluk", "🇦🇱"

        if any(k in title_lower for k in ["kansai", "world masters games", "wmg", "japan", "japonya"]):
            return "JP", "Japonya", "🇯🇵"
        if any(k in title_lower for k in ["türkiye", "turkey", "istanbul", "izmir", "bursa", "ankara", "taf"]):
            return "TR", "Türkiye", "🇹🇷"
        if any(k in title_lower for k in ["almanya", "germany", "deutschland", "dlv", "ladv"]):
            return "DE", "Almanya", "🇩🇪"
        if any(k in title_lower for k in ["fransa", "france", "ffa", "miramas"]):
            return "FR", "Fransa", "🇫🇷"
        if any(k in title_lower for k in ["yunanistan", "greece", "hellas", "athens"]):
            return "GR", "Yunanistan", "🇬🇷"
        if any(k in title_lower for k in ["italya", "italy", "italia", "catania", "fidal"]):
            return "IT", "İtalya", "🇮🇹"
        if any(k in title_lower for k in ["polonya", "poland", "polska", "torun", "toruń"]):
            return "PL", "Polonya", "🇵🇱"
        if any(k in title_lower for k in ["ispanya", "spain", "españa", "ojós", "ojos"]):
            return "ES", "İspanya", "🇪🇸"
        if any(k in title_lower for k in ["finlandiya", "finland", "jyväskylä", "jyvaskyla"]):
            return "FIN", "Finlandiya", "🇫🇮"
        if any(k in title_lower for k in ["isviçre", "switzerland", "schweiz", "lucerne"]):
            return "CH", "İsviçre", "🇨🇭"
        if any(k in title_lower for k in ["romanya", "romania", "craiova", "rasnov", "râșnov"]):
            return "RO", "Romanya", "🇷🇴"
        if any(k in title_lower for k in ["ingiltere", "uk", "gbr", "glasgow", "great britain"]):
            return "GB", "Birleşik Krallık", "🇬🇧"
        if any(k in title_lower for k in ["irlanda", "ireland", "galway"]):
            return "IE", "İrlanda", "🇮🇪"
        if any(k in title_lower for k in ["slovenya", "slovenia", "novo mesto", "domzale"]):
            return "SI", "Slovenya", "🇸🇮"
        if any(k in title_lower for k in ["arnavutluk", "albania", "shkodër", "shkoder", "tirana", "tiran"]):
            return "AL", "Arnavutluk", "🇦🇱"
        if any(k in title_lower for k in ["çekya", "czech", "rumburk", "lovosice", "janske lazne"]):
            return "CZ", "Çekya", "🇨🇿"
        if any(k in title_lower for k in ["portekiz", "portugal", "lisboa", "lisbon"]):
            return "PT", "Portekiz", "🇵🇹"
        if any(k in title_lower for k in ["norveç", "norway", "oslo"]):
            return "NO", "Norveç", "🇳🇴"
        if any(k in title_lower for k in ["danimarka", "denmark", "dansk", "hvidovre", "aabenraa"]):
            return "DK", "Danimarka", "🇩🇰"
        if any(k in title_lower for k in ["izlanda", "iceland", "selfoss"]):
            return "IS", "İzlanda", "🇮🇸"
        if any(k in title_lower for k in ["güney kore", "korea", "daegu"]):
            return "KOR", "Güney Kore", "🇰🇷"
        if any(k in title_lower for k in ["letonya", "latvia", "liepaja"]):
            return "LV", "Letonya", "🇱🇻"
        if any(k in title_lower for k in ["litvanya", "lithuania", "marijampole", "klaipeda"]):
            return "LT", "Litvanya", "🇱🇹"
        if any(k in title_lower for k in ["hırvatistan", "croatia", "zagreb", "varaždin", "varazdin"]):
            return "HR", "Hırvatistan", "🇭🇷"
        if any(k in title_lower for k in ["avusturya", "austria", "götzis", "gotzis"]):
            return "AT", "Avusturya", "🇦🇹"
        if any(k in title_lower for k in ["belçika", "belgium", "merksem"]):
            return "BE", "Belçika", "🇧🇪"
        if any(k in title_lower for k in ["isveç", "sweden", "östersund", "ostersund"]):
            return "SE", "İsveç", "🇸🇪"
        if any(k in title_lower for k in ["estonya", "estonia", "pärnu", "parnu"]):
            return "EE", "Estonya", "🇪🇪"
        if any(k in title_lower for k in ["sırbistan", "serbia", "belgrad", "kruševac"]):
            return "RS", "Sırbistan", "🇷🇸"
        if any(k in title_lower for k in ["bulgaristan", "bulgaria", "sofya", "plovdiv"]):
            return "BG", "Bulgaristan", "🇧🇬"
        if any(k in title_lower for k in ["balkan"]):
            return "EU", "Avrupa", "🇪🇺"
        
        country_flags = {
            "TR": "🇹🇷", "DE": "🇩🇪", "FR": "🇫🇷", "GR": "🇬🇷", "IT": "🇮🇹",
            "JP": "🇯🇵", "ES": "🇪🇸", "GB": "🇬🇧", "PL": "🇵🇱", "SE": "🇸🇪",
            "RO": "🇷🇴", "BG": "🇧🇬", "RS": "🇷🇸", "KOR": "🇰🇷", "USA": "🇺🇸",
            "FIN": "🇫🇮", "CH": "🇨🇭", "IE": "🇮🇪", "SI": "🇸🇮", "AL": "🇦🇱",
            "CZ": "🇨🇿", "PT": "🇵🇹", "NO": "🇳🇴", "DK": "🇩🇰", "IS": "🇮🇸",
            "LV": "🇱🇻", "LT": "🇱🇹", "HR": "🇭🇷", "AT": "🇦🇹", "BE": "🇧🇪",
            "EE": "🇪🇪", "EU": "🇪🇺", "INT": "🌍"
        }
        flag = country_flags.get(default_country.upper(), "🏃")
        return default_country, default_name, flag

    def detect_venue_type(self, text: str) -> str:
        """Metin içerisindeki anahtar kelimelerden salon/açık/yol ayrımını yapar."""
        text_lower = text.lower()
        if any(w in text_lower for w in ["salon", "indoor", "kapalı", "halle", "en salle", "hallenmeeting", "pista cubierta"]):
            return "indoor"
        if any(w in text_lower for w in ["yol", "maraton", "yarı maraton", "road", "10k", "21k", "42k"]):
            return "road"
        if any(w in text_lower for w in ["kros", "cross", "cross country", "arazi"]):
            return "cross"
        return "outdoor"

    def detect_categories(self, text: str, venue_type: str = "") -> List[str]:
        """Metne ve tesis türüne göre branş gruplarını sınıflandırır."""
        text_lower = text.lower()
        categories = []

        is_road = venue_type == "road" or any(w in text_lower for w in ["yol", "maraton", "yarı maraton", "road", "10k", "21k", "42k"])
        is_cross = venue_type == "cross" or any(w in text_lower for w in ["kros", "cross", "cross country", "arazi"])

        # Yol veya kros koşularında sadece uzun mesafe branşı bulunur; sprint/atlama/atma pist branşları yer almaz!
        if is_road or is_cross:
            return ["Uzun Mesafe"]

        # Pist / Salon / Genel şampiyonalar için branş tespiti
        if any(w in text_lower for w in ["sprint", "koşu", "100m", "200m", "400m", "bayrak", "60m"]):
            categories.append("Sprint")
        if any(w in text_lower for w in ["engel", "hurdle", "engelli", "60m h"]):
            categories.append("Engelli")
        if any(w in text_lower for w in ["atlama", "uzun atlama", "üçadım", "yüksek atlama", "sırık", "jump"]):
            categories.append("Atlamalar")
        if any(w in text_lower for w in ["atma", "gülle", "cirit", "disk", "çekiç", "throw", "shot put", "winterwurf", "wurf", "lancers"]):
            categories.append("Atmalar")
        if any(w in text_lower for w in ["uzun mesafe", "800m", "1500m", "3000m", "5000m", "10000m", "distance"]):
            categories.append("Uzun Mesafe")
        
        if not categories:
            # Standart tam pist/salon şampiyonası ise tüm branşları içerir
            categories = ["Sprint", "Engelli", "Atlamalar", "Atmalar", "Uzun Mesafe"]
        return categories

    def is_master_athletics(self, text: str) -> bool:
        """
        Sadece 35+ yaş kategorisini kapsayan Master/Veteran yarışmalarını doğrular.
        Sadece Gençler (U14-U23) veya salt elit yarışmaları eler.
        """
        text_lower = text.lower()
        # Eğer gençlik/yıldız kategorisiyse ve master kelimesi yoksa ele
        if any(k in text_lower for k in ["u14", "u16", "u18", "u20", "u23", "gençler", "yıldızlar", "jugend", "cadet"]):
            if not any(k in text_lower for k in ["master", "veteran", "senioren", "35+"]):
                return False

        # Resmi Master/Veteran belirteçleri
        master_keywords = [
            "master", "veteran", "senioren", "35+", "m35", "w35",
            "bamacs", "bamaci", "emaci", "emacs", "wmaci", "wma", "world masters"
        ]
        return any(k in text_lower for k in master_keywords)
