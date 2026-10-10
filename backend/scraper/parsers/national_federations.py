"""
Ulusal Atletizm Federasyonları ve Master Birlikleri Kayıt Defteri (Registry)
Sadece federasyonların canlı web siteleri, takvim arama adresleri ve dil anahtar kelimeleri yer alır.
Hiçbir statik yarışma kaydı tutulmaz.
"""

from typing import Dict, List, Any

NATIONAL_FEDERATIONS: List[Dict[str, Any]] = [
    {
        "country_code": "TR",
        "country_name": "Türkiye",
        "federation_name": "TAF",
        "full_name": "Türkiye Atletizm Federasyonu",
        "base_url": "https://www.taf.org.tr",
        "calendar_url": "https://www.taf.org.tr/faaliyet-takvimi/",
        "keywords": ["master", "veteran", "büyükler", "şampiyonası"]
    },
    {
        "country_code": "GR",
        "country_name": "Yunanistan",
        "federation_name": "SEGAS / SEVAS",
        "full_name": "Hellenic Athletics Federation / Greek Masters Athletics",
        "base_url": "https://www.segas.gr",
        "calendar_url": "https://www.segas.gr/events/",
        "keywords": ["masters", "βετεράνων", "veteran", "masters championship", "πανελλήνιο"]
    },
    {
        "country_code": "DE",
        "country_name": "Almanya",
        "federation_name": "DLV",
        "full_name": "Deutscher Leichtathletik-Verband Masters",
        "base_url": "https://www.leichtathletik.de",
        "calendar_url": "https://www.leichtathletik.de/wettkaempfe/wettkampf-kalender",
        "keywords": ["senioren", "masters", "deutsche seniorenmeisterschaft", "dm masters", "winterwurf", "hallenmeeting"]
    },
    {
        "country_code": "IT",
        "country_name": "İtalya",
        "federation_name": "FIDAL",
        "full_name": "Federazione Italiana di Atletica Leggera (FIDAL Master)",
        "base_url": "https://www.fidal.it",
        "calendar_url": "https://www.fidal.it/calendario.php",
        "keywords": ["master", "campionati italiani master", "indoor master", "fidal master"]
    },
    {
        "country_code": "GB",
        "country_name": "Birleşik Krallık",
        "federation_name": "BMAF",
        "full_name": "British Masters Athletic Federation",
        "base_url": "https://bmaf.org.uk",
        "calendar_url": "https://bmaf.org.uk/fixtures/",
        "keywords": ["championships", "masters", "track & field", "indoor", "bmaf"]
    },
    {
        "country_code": "ES",
        "country_name": "İspanya",
        "federation_name": "RFEA",
        "full_name": "Real Federación Española de Atletismo (RFEA Master)",
        "base_url": "https://www.rfea.es",
        "calendar_url": "https://www.rfea.es/competi/calendario.asp",
        "keywords": ["master", "campeonato de españa master", "veteranos"]
    },
    {
        "country_code": "FR",
        "country_name": "Fransa",
        "federation_name": "FFA",
        "full_name": "Fédération Française d'Athlétisme (FFA Masters)",
        "base_url": "https://www.athle.fr",
        "calendar_url": "https://bases.athle.fr/asp.net/listeepreuves.aspx",
        "keywords": ["masters", "championnats de france masters", "vétérans", "miramas", "salle"]
    },
    {
        "country_code": "PL",
        "country_name": "Polonya",
        "federation_name": "PZLAM",
        "full_name": "Polski Związek Lekkiej Atletyki Masters",
        "base_url": "https://pzlam.pl",
        "calendar_url": "https://pzlam.pl/kalendarz-imprez/",
        "keywords": ["masters", "mistrzostwa polski masters", "halowe mistrzostwa"]
    },
    {
        "country_code": "BG",
        "country_name": "Bulgaristan",
        "federation_name": "BAF",
        "full_name": "Bulgarian Athletics Federation / Masters Club",
        "base_url": "https://baf.bg",
        "calendar_url": "https://baf.bg/calendar/",
        "keywords": ["masters", "ветерани", "държавно първенство"]
    },
    {
        "country_code": "RO",
        "country_name": "Romanya",
        "federation_name": "FRA",
        "full_name": "Federatia Romana de Atletism Masters",
        "base_url": "https://www.fra.ro",
        "calendar_url": "https://www.fra.ro/competitii/",
        "keywords": ["masters", "veterani", "campionatul national masters"]
    },
    {
        "country_code": "RS",
        "country_name": "Sırbistan",
        "federation_name": "SAS",
        "full_name": "Srpski Atletski Savez (SAS Masters)",
        "base_url": "https://www.sas.org.rs",
        "calendar_url": "https://www.sas.org.rs/kalendar/",
        "keywords": ["veterani", "masters", "prvenstvo srbije"]
    },
    {
        "country_code": "SE",
        "country_name": "İsveç",
        "federation_name": "SFIF",
        "full_name": "Svensk Friidrott (Veteran)",
        "base_url": "https://www.friidrott.se",
        "calendar_url": "https://www.friidrott.se/tavling-landslag/tavling/veteran-sm/",
        "keywords": ["veteran-sm", "masters", "veteran"]
    }
]
