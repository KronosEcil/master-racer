import abc
import re
import logging
import requests
from typing import List, Optional
from datetime import datetime
from ..config import Competition, ScraperConfig

logger = logging.getLogger(__name__)


class BaseParser(abc.ABC):
    """
    Tüm federasyon ve yapay zeka takvim kazıyıcılarının türetildiği temel sınıf.
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

    @abc.abstractmethod
    def parse(self) -> List[Competition]:
        """Tüm yarışmaları tarayıp Competition listesi döndürür."""
        pass

    @staticmethod
    def iso2_to_flag_emoji(code: str) -> str:
        """Herhangi bir 2 harfli ISO ülke kodunu resmi Unicode Bayrak Emojisine dönüştürür."""
        c = (code or "").strip().upper()
        if c == "EU":
            return "🇪🇺"
        if c in ("INT", "WORLD"):
            return "🌍"
        if len(c) == 2 and "A" <= c[0] <= "Z" and "A" <= c[1] <= "Z":
            return chr(0x1F1E6 + ord(c[0]) - ord("A")) + chr(0x1F1E6 + ord(c[1]) - ord("A"))
        return "🌍"

    def detect_country(self, title: str, default_country: str = "INT", default_name: str = "Uluslararası") -> tuple[str, str, str]:
        """
        Hiçbir sabit şehir ismine bağımlı kalmadan; ISO ülke kodlarından, ülke isimlerinden
        ve ulusal federasyon kısaltmalarından ev sahibi ülkeyi, ülke adını ve bayrağını belirler.
        """
        def_upper = (default_country or "INT").strip().upper()
        title_lower = f"{title or ''} {default_name or ''} {default_country or ''}".lower()

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
            "XK": ("XK", "Kosova", "🇽🇰"), "KOS": ("XK", "Kosova", "🇽🇰"),
            "MD": ("MD", "Moldova", "🇲🇩"), "MDA": ("MD", "Moldova", "🇲🇩"),
            "AL": ("AL", "Arnavutluk", "🇦🇱"), "ALB": ("AL", "Arnavutluk", "🇦🇱"),
            "SE": ("SE", "İsveç", "🇸🇪"), "SWE": ("SE", "İsveç", "🇸🇪"),
            "NO": ("NO", "Norveç", "🇳🇴"), "NOR": ("NO", "Norveç", "🇳🇴"),
            "FI": ("FI", "Finlandiya", "🇫🇮"), "FIN": ("FI", "Finlandiya", "🇫🇮"),
            "DK": ("DK", "Danimarka", "🇩🇰"), "DEN": ("DK", "Danimarka", "🇩🇰"), "DNK": ("DK", "Danimarka", "🇩🇰"),
            "IS": ("IS", "İzlanda", "🇮🇸"), "ISL": ("IS", "İzlanda", "🇮🇸"),
            "EE": ("EE", "Estonya", "🇪🇪"), "EST": ("EE", "Estonya", "🇪🇪"),
            "LV": ("LV", "Letonya", "🇱🇻"), "LAT": ("LV", "Letonya", "🇱🇻"), "LVA": ("LV", "Letonya", "🇱🇻"),
            "LT": ("LT", "Litvanya", "🇱🇹"), "LTU": ("LT", "Litvanya", "🇱🇹"),
            "UA": ("UA", "Ukrayna", "🇺🇦"), "UKR": ("UA", "Ukrayna", "🇺🇦"),
            "CY": ("CY", "Kıbrıs", "🇨🇾"), "CYP": ("CY", "Kıbrıs", "🇨🇾"),
            "MT": ("MT", "Malta", "🇲🇹"), "MLT": ("MT", "Malta", "🇲🇹"),
            "JP": ("JP", "Japonya", "🇯🇵"), "JPN": ("JP", "Japonya", "🇯🇵"),
            "KR": ("KR", "Güney Kore", "🇰🇷"), "KOR": ("KR", "Güney Kore", "🇰🇷"),
            "US": ("US", "ABD", "🇺🇸"), "USA": ("US", "ABD", "🇺🇸"),
            "AU": ("AU", "Avustralya", "🇦🇺"), "AUS": ("AU", "Avustralya", "🇦🇺"),
            "NZ": ("NZ", "Yeni Zelanda", "🇳🇿"), "NZL": ("NZ", "Yeni Zelanda", "🇳🇿"),
            "CA": ("CA", "Kanada", "🇨🇦"), "CAN": ("CA", "Kanada", "🇨🇦"),
            "BR": ("BR", "Brezilya", "🇧🇷"), "BRA": ("BR", "Brezilya", "🇧🇷"),
            "AR": ("AR", "Arjantin", "🇦🇷"), "ARG": ("AR", "Arjantin", "🇦🇷"),
            "MX": ("MX", "Meksika", "🇲🇽"), "MEX": ("MX", "Meksika", "🇲🇽"),
            "PE": ("PE", "Peru", "🇵🇪"), "PER": ("PE", "Peru", "🇵🇪"),
            "CL": ("CL", "Şili", "🇨🇱"), "CHL": ("CL", "Şili", "🇨🇱"), "CHI": ("CL", "Şili", "🇨🇱"),
            "CO": ("CO", "Kolombiya", "🇨🇴"), "COL": ("CO", "Kolombiya", "🇨🇴"),
            "ZA": ("ZA", "Güney Afrika", "🇿🇦"), "RSA": ("ZA", "Güney Afrika", "🇿🇦"), "ZAF": ("ZA", "Güney Afrika", "🇿🇦"),
            "MA": ("MA", "Fas", "🇲🇦"), "MAR": ("MA", "Fas", "🇲🇦"),
            "TN": ("TN", "Tunus", "🇹🇳"), "TUN": ("TN", "Tunus", "🇹🇳"),
            "EG": ("EG", "Mısır", "🇪🇬"), "EGY": ("EG", "Mısır", "🇪🇬"),
            "CN": ("CN", "Çin", "🇨🇳"), "CHN": ("CN", "Çin", "🇨🇳"),
            "TW": ("TW", "Tayvan", "🇹🇼"), "TPE": ("TW", "Tayvan", "🇹🇼"),
            "AE": ("AE", "Birleşik Arap Emirlikleri", "🇦🇪"), "UAE": ("AE", "Birleşik Arap Emirlikleri", "🇦🇪"), "ARE": ("AE", "Birleşik Arap Emirlikleri", "🇦🇪"),
            "IN": ("IN", "Hindistan", "🇮🇳"), "IND": ("IN", "Hindistan", "🇮🇳"),
            "SG": ("SG", "Singapur", "🇸🇬"), "SGP": ("SG", "Singapur", "🇸🇬"),
            "MY": ("MY", "Malezya", "🇲🇾"), "MAS": ("MY", "Malezya", "🇲🇾"), "MYS": ("MY", "Malezya", "🇲🇾"),
            "TH": ("TH", "Tayland", "🇹🇭"), "THA": ("TH", "Tayland", "🇹🇭"),
            "KZ": ("KZ", "Kazakistan", "🇰🇿"), "KAZ": ("KZ", "Kazakistan", "🇰🇿"),
            "UZ": ("UZ", "Özbekistan", "🇺🇿"), "UZB": ("UZ", "Özbekistan", "🇺🇿"),
            "AZ": ("AZ", "Azerbaycan", "🇦🇿"), "AZE": ("AZ", "Azerbaycan", "🇦🇿"),
            "GE": ("GE", "Gürcistan", "🇬🇪"), "GEO": ("GE", "Gürcistan", "🇬🇪"),
        }

        if def_upper in iso_map:
            return iso_map[def_upper]

        if any(k in title_lower for k in ["yeni zelanda", "new zealand", "auckland"]):
            return "NZ", "Yeni Zelanda", "🇳🇿"
        if any(k in title_lower for k in ["avustralya", "australia", "australian", "perth", "sydney", "melbourne", "brisbane"]):
            return "AU", "Avustralya", "🇦🇺"
        if any(k in title_lower for k in ["abu dhabi", "united arab emirates", "birleşik arap emirlikleri", "uae"]):
            return "AE", "Birleşik Arap Emirlikleri", "🇦🇪"
        if any(k in title_lower for k in ["tayvan", "taiwan", "taipei"]):
            return "TW", "Tayvan", "🇹🇼"
        if any(k in title_lower for k in ["romanya", "romania", "romanian", "bucuresti", "bükreş", "craiova"]):
            return "RO", "Romanya", "🇷🇴"
        if any(k in title_lower for k in ["türkiye", "turkey", "turkish", "taf", "istanbul", "ankara", "izmir"]):
            return "TR", "Türkiye", "🇹🇷"
        if any(k in title_lower for k in ["almanya", "germany", "german", "deutschland", "deutsche", "dlv", "ladv"]):
            return "DE", "Almanya", "🇩🇪"
        if any(k in title_lower for k in ["fransa", "france", "french", "ffa", "miramas", "épinal", "epinal", "saint-brieuc"]):
            return "FR", "Fransa", "🇫🇷"
        if any(k in title_lower for k in ["yunanistan", "greece", "greek", "hellas", "segas"]):
            return "GR", "Yunanistan", "🇬🇷"
        if any(k in title_lower for k in ["italya", "italy", "italian", "italia", "fidal", "como", "lombardia", "torino", "catania", "ancona", "meduno"]):
            return "IT", "İtalya", "🇮🇹"
        if any(k in title_lower for k in ["polonya", "poland", "polish", "polska", "pzla", "torun", "toruń"]):
            return "PL", "Polonya", "🇵🇱"
        if any(k in title_lower for k in ["ispanya", "spain", "spanish", "españa", "rfea", "murcia", "ojós", "ojos"]):
            return "ES", "İspanya", "🇪🇸"
        if any(k in title_lower for k in ["finlandiya", "finland", "finnish", "saul", "lahti", "jyväskylä", "jyvaskyla", "tampere", "helsinki"]):
            return "FI", "Finlandiya", "🇫🇮"
        if any(k in title_lower for k in ["isviçre", "switzerland", "swiss", "schweiz", "klosters", "lucerne", "luzern"]):
            return "CH", "İsviçre", "🇨🇭"
        if any(k in title_lower for k in ["ingiltere", "birleşik krallık", "great britain", "british", "bmaf", "scotland", "england", "wales", "glasgow"]):
            return "GB", "Birleşik Krallık", "🇬🇧"
        if any(k in title_lower for k in ["irlanda", "ireland", "irish", "imaa", "galway", "athlone", "tullamore"]):
            return "IE", "İrlanda", "🇮🇪"
        if any(k in title_lower for k in ["slovenya", "slovenia", "slovenian"]):
            return "SI", "Slovenya", "🇸🇮"
        if any(k in title_lower for k in ["arnavutluk", "albania", "albanian"]):
            return "AL", "Arnavutluk", "🇦🇱"
        if any(k in title_lower for k in ["çekya", "czech"]):
            return "CZ", "Çekya", "🇨🇿"
        if any(k in title_lower for k in ["portekiz", "portugal", "portuguese", "fpatletismo", "anav", "porto", "lisboa"]):
            return "PT", "Portekiz", "🇵🇹"
        if any(k in title_lower for k in ["norveç", "norway", "norwegian"]):
            return "NO", "Norveç", "🇳🇴"
        if any(k in title_lower for k in ["danimarka", "denmark", "danish", "dansk", "aarhus"]):
            return "DK", "Danimarka", "🇩🇰"
        if any(k in title_lower for k in ["izlanda", "iceland", "icelandic"]):
            return "IS", "İzlanda", "🇮🇸"
        if any(k in title_lower for k in ["güney kore", "south korea", "korea", "daegu", "jeonbuk"]):
            return "KR", "Güney Kore", "🇰🇷"
        if any(k in title_lower for k in ["abd", "united states", "usa", "gainesville", "cleveland"]):
            return "US", "ABD", "🇺🇸"
        if any(k in title_lower for k in ["kanada", "canada", "vancouver", "toronto", "edmonton"]):
            return "CA", "Kanada", "🇨🇦"
        if any(k in title_lower for k in ["malezya", "malaysia", "penang"]):
            return "MY", "Malezya", "🇲🇾"
        if any(k in title_lower for k in ["japonya", "japan", "japanese", "kansai"]):
            return "JP", "Japonya", "🇯🇵"
        if any(k in title_lower for k in ["letonya", "latvia", "latvian"]):
            return "LV", "Letonya", "🇱🇻"
        if any(k in title_lower for k in ["litvanya", "lithuania", "lithuanian"]):
            return "LT", "Litvanya", "🇱🇹"
        if any(k in title_lower for k in ["hırvatistan", "croatia", "croatian"]):
            return "HR", "Hırvatistan", "🇭🇷"
        if any(k in title_lower for k in ["avusturya", "austria", "austrian", "ölv", "oelv", "innsbruck"]):
            return "AT", "Avusturya", "🇦🇹"
        if any(k in title_lower for k in ["belçika", "belgium", "belgian"]):
            return "BE", "Belçika", "🇧🇪"
        if any(k in title_lower for k in ["hollanda", "netherlands", "dutch", "atletiekunie"]):
            return "NL", "Hollanda", "🇳🇱"
        if any(k in title_lower for k in ["isveç", "sweden", "swedish"]):
            return "SE", "İsveç", "🇸🇪"
        if any(k in title_lower for k in ["estonya", "estonia", "estonian"]):
            return "EE", "Estonya", "🇪🇪"
        if any(k in title_lower for k in ["sırbistan", "serbia", "serbian"]):
            return "RS", "Sırbistan", "🇷🇸"
        if any(k in title_lower for k in ["bulgaristan", "bulgaria", "bulgarian"]):
            return "BG", "Bulgaristan", "🇧🇬"
        if any(k in title_lower for k in ["macaristan", "hungary", "hungarian"]):
            return "HU", "Macaristan", "🇭🇺"
        if any(k in title_lower for k in ["slovakya", "slovakia", "slovak"]):
            return "SK", "Slovakya", "🇸🇰"

        if any(k in title_lower for k in ["balkan"]):
            return "EU", "Avrupa", "🇪🇺"

        return default_country, default_name, self.iso2_to_flag_emoji(def_upper)

    def detect_scope(self, title: str, country: str = "") -> str:
        """
        Yarışma başlığından kapsamı (scope) belirler:
        'world' | 'europe' | 'balkan' | 'ema_league' | 'national' | 'regional'
        """
        t_low = (title or "").lower()
        if any(w in t_low for w in ["world", "dünya", "wma", "wmg", "open masters games"]):
            return "world"
        if any(w in t_low for w in ["ema league", "ema ligi", "meeting européen", "speed masters athletics challenge"]):
            return "ema_league"
        if any(w in t_low for w in ["balkan", "bamacs", "bamaci", "bamam"]):
            return "balkan"
        if any(w in t_low for w in ["european", "avrupa", "emacs", "emaci", "emacns", "emmtrc", "emamrc", "emorrc"]):
            return "europe"
        if any(w in t_low for w in [
            "national", "ulusal", "türkiye", "british masters", "bmaf", "deutsche", "championnats de france",
            "campionati italiani", "campeonato de españa", "campeonatos nacionais", "mistrzostwa polski",
            "nk masters", "nk indoor", "irish masters", "imaa", "swiss masters", "österr", "mčr",
            "sm-hallit", "sm-kilpailut", "ism veteraner", "sm veteraner", "nm masters", "dm masters"
        ]):
            return "national"
        return "regional"
