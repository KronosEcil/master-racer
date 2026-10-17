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
        def_upper = (default_country or "INT").strip().upper()
        def_name_lower = (default_name or "").strip().lower()
        title_lower = f"{title or ''} {default_name or ''} {default_country or ''}".lower()

        # 1. Doğrudan ISO ülke kodları veya ülke isimleri eşleşmesi (Avrupa ve Dünya)
        iso_map = {
            "TR": ("TR", "Türkiye", "🇹🇷"), "TUR": ("TR", "Türkiye", "🇹🇷"),
            "DE": ("DE", "Almanya", "🇩🇪"), "GER": ("DE", "Almanya", "🇩🇪"), "DEU": ("DE", "Almanya", "🇩🇪"),
            "FR": ("FR", "Fransa", "🇫🇷"), "FRA": ("FR", "Fransa", "🇫🇷"),
            "GR": ("GR", "Yunanistan", "🇬🇷"), "GRE": ("GR", "Yunanistan", "🇬🇷"), "GRC": ("GR", "Yunanistan", "🇬🇷"),
            "IT": ("IT", "İtalya", "🇮🇹"), "ITA": ("IT", "İtalya", "🇮🇹"),
            "ES": ("ES", "İspanya", "🇪🇸"), "ESP": ("ES", "İspanya", "🇪🇸"),
            "PT": ("PT", "Portekiz", "🇵🇹"), "POR": ("PT", "Portekiz", "🇵🇹"), "PRT": ("PT", "Portekiz", "🇵🇹"),
            "GB": ("GB", "Birleşik Krallık", "🇬🇧"), "GBR": ("GB", "Birleşik Krallık", "🇬🇧"), "UK": ("GB", "Birleşik Krallık", "🇬🇧"),
            "IE": ("IE", "İrlanda", "🇮🇪"), "IRL": ("IE", "İrlanda", "🇮🇪"),
            "NL": ("NL", "Hollanda", "🇳🇱"), "NED": ("NL", "Hollanda", "🇳🇱"), "NLD": ("NL", "Hollanda", "🇳🇱"),
            "BE": ("BE", "Belçika", "🇧🇪"), "BEL": ("BE", "Belçika", "🇧🇪"),
            "LU": ("LU", "Lüksemburg", "🇱🇺"), "LUX": ("LU", "Lüksemburg", "🇱🇺"),
            "CH": ("CH", "İsviçre", "🇨🇭"), "SUI": ("CH", "İsviçre", "🇨🇭"), "CHE": ("CH", "İsviçre", "🇨🇭"),
            "AT": ("AT", "Avusturya", "🇦🇹"), "AUT": ("AT", "Avusturya", "🇦🇹"),
            "PL": ("PL", "Polonya", "🇵🇱"), "POL": ("PL", "Polonya", "🇵🇱"),
            "CZ": ("CZ", "Çekya", "🇨🇿"), "CZE": ("CZ", "Çekya", "🇨🇿"),
            "SK": ("SK", "Slovakya", "🇸🇰"), "SVK": ("SK", "Slovakya", "🇸🇰"),
            "HU": ("HU", "Macaristan", "🇭🇺"), "HUN": ("HU", "Macaristan", "🇭🇺"),
            "RO": ("RO", "Romanya", "🇷🇴"), "ROU": ("RO", "Romanya", "🇷🇴"), "ROM": ("RO", "Romanya", "🇷🇴"),
            "BG": ("BG", "Bulgaristan", "🇧🇬"), "BUL": ("BG", "Bulgaristan", "🇧🇬"), "BGR": ("BG", "Bulgaristan", "🇧🇬"),
            "RS": ("RS", "Sırbistan", "🇷🇸"), "SRB": ("RS", "Sırbistan", "🇷🇸"),
            "HR": ("HR", "Hırvatistan", "🇭🇷"), "CRO": ("HR", "Hırvatistan", "🇭🇷"), "HRV": ("HR", "Hırvatistan", "🇭🇷"),
            "SI": ("SI", "Slovenya", "🇸🇮"), "SLO": ("SI", "Slovenya", "🇸🇮"), "SVN": ("SI", "Slovenya", "🇸🇮"),
            "BA": ("BA", "Bosna-Hersek", "🇧🇦"), "BIH": ("BA", "Bosna-Hersek", "🇧🇦"),
            "ME": ("ME", "Karadağ", "🇲🇪"), "MNE": ("ME", "Karadağ", "🇲🇪"),
            "MK": ("MK", "Kuzey Makedonya", "🇲🇰"), "MKD": ("MK", "Kuzey Makedonya", "🇲🇰"),
            "AL": ("AL", "Arnavutluk", "🇦🇱"), "ALB": ("AL", "Arnavutluk", "🇦🇱"),
            "SE": ("SE", "İsveç", "🇸🇪"), "SWE": ("SE", "İsveç", "🇸🇪"),
            "NO": ("NO", "Norveç", "🇳🇴"), "NOR": ("NO", "Norveç", "🇳🇴"),
            "FI": ("FIN", "Finlandiya", "🇫🇮"), "FIN": ("FIN", "Finlandiya", "🇫🇮"),
            "DK": ("DK", "Danimarka", "🇩🇰"), "DEN": ("DK", "Danimarka", "🇩🇰"), "DNK": ("DK", "Danimarka", "🇩🇰"),
            "IS": ("IS", "İzlanda", "🇮🇸"), "ISL": ("IS", "İzlanda", "🇮🇸"),
            "EE": ("EE", "Estonya", "🇪🇪"), "EST": ("EE", "Estonya", "🇪🇪"),
            "LV": ("LV", "Letonya", "🇱🇻"), "LAT": ("LV", "Letonya", "🇱🇻"), "LVA": ("LV", "Letonya", "🇱🇻"),
            "LT": ("LT", "Litvanya", "🇱🇹"), "LTU": ("LT", "Litvanya", "🇱🇹"),
            "UA": ("UA", "Ukrayna", "🇺🇦"), "UKR": ("UA", "Ukrayna", "🇺🇦"),
            "CY": ("CY", "Kıbrıs", "🇨🇾"), "CYP": ("CY", "Kıbrıs", "🇨🇾"),
            "MT": ("MT", "Malta", "🇲🇹"), "MLT": ("MT", "Malta", "🇲🇹"),
            "JP": ("JP", "Japonya", "🇯🇵"), "JPN": ("JP", "Japonya", "🇯🇵"),
            "KR": ("KOR", "Güney Kore", "🇰🇷"), "KOR": ("KOR", "Güney Kore", "🇰🇷"),
            "US": ("USA", "ABD", "🇺🇸"), "USA": ("USA", "ABD", "🇺🇸")
        }

        if any(k in title_lower for k in ["kansai", "world masters games", "wmg", "japan", "japonya"]):
            return "JP", "Japonya", "🇯🇵"
        if any(k in title_lower for k in ["türkiye", "turkey", "turkish", "istanbul", "izmir", "bursa", "ankara", "taf"]):
            return "TR", "Türkiye", "🇹🇷"
        if any(k in title_lower for k in ["almanya", "germany", "german", "deutschland", "deutsche", "dlv", "ladv"]):
            return "DE", "Almanya", "🇩🇪"
        if any(k in title_lower for k in ["fransa", "france", "french", "ffa", "miramas", "nantes", "saint-brieuc", "épinal", "epinal", "châteauroux", "belfort"]):
            return "FR", "Fransa", "🇫🇷"
        if any(k in title_lower for k in ["yunanistan", "greece", "greek", "hellas", "athens", "thessaloniki", "selanik"]):
            return "GR", "Yunanistan", "🇬🇷"
        if any(k in title_lower for k in ["italya", "italy", "italian", "italia", "catania", "roma", "ancona", "fidal"]):
            return "IT", "İtalya", "🇮🇹"
        if any(k in title_lower for k in ["polonya", "poland", "polish", "polska", "torun", "toruń", "warszawa", "włocławek"]):
            return "PL", "Polonya", "🇵🇱"
        if any(k in title_lower for k in ["ispanya", "spain", "spanish", "españa", "ojós", "ojos", "madrid", "barcelona", "valencia", "malaga"]):
            return "ES", "İspanya", "🇪🇸"
        if any(k in title_lower for k in ["finlandiya", "finland", "finnish", "jyväskylä", "jyvaskyla", "tampere", "helsinki"]):
            return "FIN", "Finlandiya", "🇫🇮"
        if any(k in title_lower for k in ["isviçre", "switzerland", "swiss", "schweiz", "lucerne", "luzern", "kreuzlingen", "bellinzona", "zürich"]):
            return "CH", "İsviçre", "🇨🇭"
        if any(k in title_lower for k in ["romanya", "romania", "romanian", "craiova", "rasnov", "râșnov", "bucharest", "bükreş"]):
            return "RO", "Romanya", "🇷🇴"
        if any(k in title_lower for k in ["ingiltere", "uk", "gbr", "glasgow", "great britain", "british", "scotland", "england", "london", "sheffield", "birmingham", "derby"]):
            return "GB", "Birleşik Krallık", "🇬🇧"
        if any(k in title_lower for k in ["irlanda", "ireland", "irish", "galway", "dublin", "athlone"]):
            return "IE", "İrlanda", "🇮🇪"
        if any(k in title_lower for k in ["slovenya", "slovenia", "slovenian", "novo mesto", "domzale", "ljubljana", "celje"]):
            return "SI", "Slovenya", "🇸🇮"
        if any(k in title_lower for k in ["arnavutluk", "albania", "albanian", "shkodër", "shkoder", "tirana", "tiran"]):
            return "AL", "Arnavutluk", "🇦🇱"
        if any(k in title_lower for k in ["çekya", "czech", "rumburk", "lovosice", "janske lazne", "prague", "praha", "olomouc"]):
            return "CZ", "Çekya", "🇨🇿"
        if any(k in title_lower for k in ["portekiz", "portugal", "portuguese", "lisboa", "lisbon", "porto", "braga", "pombal"]):
            return "PT", "Portekiz", "🇵🇹"
        if any(k in title_lower for k in ["norveç", "norway", "norwegian", "oslo", "bergen", "moss"]):
            return "NO", "Norveç", "🇳🇴"
        if any(k in title_lower for k in ["danimarka", "denmark", "danish", "dansk", "hvidovre", "aabenraa", "copenhagen", "odense"]):
            return "DK", "Danimarka", "🇩🇰"
        if any(k in title_lower for k in ["izlanda", "iceland", "icelandic", "selfoss", "reykjavik"]):
            return "IS", "İzlanda", "🇮🇸"
        if any(k in title_lower for k in ["güney kore", "korea", "daegu"]):
            return "KOR", "Güney Kore", "🇰🇷"
        if any(k in title_lower for k in ["letonya", "latvia", "latvian", "liepaja", "riga", "valmiera"]):
            return "LV", "Letonya", "🇱🇻"
        if any(k in title_lower for k in ["litvanya", "lithuania", "lithuanian", "marijampole", "klaipeda", "vilnius", "kaunas"]):
            return "LT", "Litvanya", "🇱🇹"
        if any(k in title_lower for k in ["hırvatistan", "croatia", "croatian", "zagreb", "varaždin", "varazdin", "split"]):
            return "HR", "Hırvatistan", "🇭🇷"
        if any(k in title_lower for k in ["avusturya", "austria", "austrian", "götzis", "gotzis", "wien", "vienna", "linz"]):
            return "AT", "Avusturya", "🇦🇹"
        if any(k in title_lower for k in ["belçika", "belgium", "belgian", "merksem", "brussels", "gent", "louvain"]):
            return "BE", "Belçika", "🇧🇪"
        if any(k in title_lower for k in ["hollanda", "netherlands", "dutch", "amsterdam", "apeldoorn", "hilversum"]):
            return "NL", "Hollanda", "🇳🇱"
        if any(k in title_lower for k in ["isveç", "sweden", "swedish", "östersund", "ostersund", "gothenburg", "göteborg", "stockholm", "malmö"]):
            return "SE", "İsveç", "🇸🇪"
        if any(k in title_lower for k in ["estonya", "estonia", "estonian", "pärnu", "parnu", "tartu", "tallinn"]):
            return "EE", "Estonya", "🇪🇪"
        if any(k in title_lower for k in ["sırbistan", "serbia", "serbian", "belgrad", "belgrade", "kruševac"]):
            return "RS", "Sırbistan", "🇷🇸"
        if any(k in title_lower for k in ["bulgaristan", "bulgaria", "bulgarian", "sofya", "sofia", "plovdiv", "stara zagora"]):
            return "BG", "Bulgaristan", "🇧🇬"
        if any(k in title_lower for k in ["macaristan", "hungary", "hungarian", "budapest", "nyíregyháza"]):
            return "HU", "Macaristan", "🇭🇺"
        if any(k in title_lower for k in ["slovakya", "slovakia", "slovak", "bratislava", "banská bystrica"]):
            return "SK", "Slovakya", "🇸🇰"

        if def_upper in iso_map:
            return iso_map[def_upper]

        if any(k in title_lower for k in ["balkan"]):
            return "EU", "Avrupa", "🇪🇺"

        return default_country, default_name, "🏃"

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
