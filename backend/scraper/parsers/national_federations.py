"""
Ulusal Atletizm Federasyonları ve Master Birlikleri Kayıt Defteri (Registry)
Her ülkenin resmi federasyon bilgileri, takvim sayfaları ve dil anahtar kelimeleri.
"""

from typing import Dict, List, Any

NATIONAL_FEDERATIONS: List[Dict[str, Any]] = [
    {
        "country_code": "GR",
        "country_name": "Yunanistan",
        "federation_name": "SEGAS / SEVAS",
        "full_name": "Hellenic Athletics Federation / Greek Masters Athletics",
        "base_url": "https://www.segas.gr",
        "calendar_url": "https://www.segas.gr/events/",
        "keywords": ["masters", "βετεράνων", "veteran", "masters championship", "πανελλήνιο"],
        "official_fixtures": [
            {
                "title": "Yunanistan Masterlar Salon Atletizm Şampiyonası (SEVAS)",
                "city": "Pire (SEF Salonu)",
                "start_date": "2026-02-14",
                "end_date": "2026-02-15",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.segas.gr/regulations/indoor-masters-2026.pdf",
                "website_url": "https://www.segas.gr"
            },
            {
                "title": "Yunanistan Masterlar Açık Saha Atletizm Şampiyonası",
                "city": "Volos",
                "start_date": "2026-06-20",
                "end_date": "2026-06-21",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://www.segas.gr/regulations/outdoor-masters-2026.pdf",
                "website_url": "https://www.segas.gr"
            }
        ]
    },
    {
        "country_code": "DE",
        "country_name": "Almanya",
        "federation_name": "DLV",
        "full_name": "Deutscher Leichtathletik-Verband Masters",
        "base_url": "https://www.leichtathletik.de",
        "calendar_url": "https://www.leichtathletik.de/wettkaempfe/wettkampf-kalender",
        "keywords": ["senioren", "masters", "deutsche seniorenmeisterschaft", "dm masters"],
        "official_fixtures": [
            {
                "title": "Almanya Masterlar Salon Şampiyonası (DM Halle Masters)",
                "city": "Dortmund (Helmut-Körnig-Halle)",
                "start_date": "2026-02-28",
                "end_date": "2026-03-01",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.leichtathletik.de/ausschreibung/dm-masters-halle-2026.pdf",
                "website_url": "https://www.leichtathletik.de"
            },
            {
                "title": "Almanya Masterlar Açık Saha Şampiyonası (DM Freiluft Masters)",
                "city": "Erfurt (Steigerwaldstadion)",
                "start_date": "2026-07-11",
                "end_date": "2026-07-12",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://www.leichtathletik.de/ausschreibung/dm-masters-2026.pdf",
                "website_url": "https://www.leichtathletik.de"
            },
            {
                "title": "Landeshallenmeisterschaften Masters + Winterwurf 2027",
                "city": "Chemnitz / Halle",
                "start_date": "2027-01-23",
                "end_date": "2027-01-24",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.leichtathletik.de/ausschreibungen/landeshallenmeisterschaften-masters-winterwurf-2027.pdf",
                "website_url": "https://www.leichtathletik.de"
            },
            {
                "title": "Almanya Masters Salon Şampiyonası (Deutsche Hallenmeisterschaften Masters 2027)",
                "city": "Dortmund (Helmut-Körnig-Halle)",
                "start_date": "2027-02-27",
                "end_date": "2027-02-28",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.leichtathletik.de/ausschreibungen/dm-masters-halle-2027.pdf",
                "website_url": "https://www.leichtathletik.de"
            },
            {
                "title": "28. Internationales Leichtathletik-Hallenmeeting (Masters & Aktive)",
                "city": "Erfurt / Chemnitz",
                "start_date": "2027-02-06",
                "end_date": "2027-02-06",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.leichtathletik.de/ausschreibungen/28-hallenmeeting-2027.pdf",
                "website_url": "https://www.leichtathletik.de"
            }
        ]
    },
    {
        "country_code": "IT",
        "country_name": "İtalya",
        "federation_name": "FIDAL",
        "full_name": "Federazione Italiana di Atletica Leggera (FIDAL Master)",
        "base_url": "https://www.fidal.it",
        "calendar_url": "https://www.fidal.it/calendario.php",
        "keywords": ["master", "campionati italiani master", "indoor master", "fidal master"],
        "official_fixtures": [
            {
                "title": "İtalya Masterlar Salon Atletizm Şampiyonası (Campionati Italiani Master Indoor)",
                "city": "Ancona (PalaCasali)",
                "start_date": "2026-02-27",
                "end_date": "2026-03-01",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.fidal.it/norme/campionati-master-indoor-2026.pdf",
                "website_url": "https://www.fidal.it"
            },
            {
                "title": "İtalya Masterlar Açık Saha Atletizm Şampiyonası (Campionati Italiani Master)",
                "city": "Rieti (Stadio Raul Guidobaldi)",
                "start_date": "2026-06-26",
                "end_date": "2026-06-28",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://www.fidal.it/norme/campionati-master-pista-2026.pdf",
                "website_url": "https://www.fidal.it"
            }
        ]
    },
    {
        "country_code": "GB",
        "country_name": "Birleşik Krallık",
        "federation_name": "BMAF",
        "full_name": "British Masters Athletic Federation",
        "base_url": "https://bmaf.org.uk",
        "calendar_url": "https://bmaf.org.uk/fixtures/",
        "keywords": ["championships", "masters", "track & field", "indoor", "bmaf"],
        "official_fixtures": [
            {
                "title": "BMAF İngiltere Masterlar Salon Şampiyonası (British Masters Indoor)",
                "city": "Sheffield (EIS Sheffield)",
                "start_date": "2026-02-14",
                "end_date": "2026-02-15",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://bmaf.org.uk/prospectus/indoor-2026.pdf",
                "website_url": "https://bmaf.org.uk"
            },
            {
                "title": "BMAF İngiltere Masterlar Açık Saha Şampiyonası (BMAF Track & Field)",
                "city": "Birmingham (Alexander Stadium)",
                "start_date": "2026-06-13",
                "end_date": "2026-06-14",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://bmaf.org.uk/prospectus/outdoor-2026.pdf",
                "website_url": "https://bmaf.org.uk"
            }
        ]
    },
    {
        "country_code": "ES",
        "country_name": "İspanya",
        "federation_name": "RFEA",
        "full_name": "Real Federación Española de Atletismo (RFEA Master)",
        "base_url": "https://www.rfea.es",
        "calendar_url": "https://www.rfea.es/competi/calendario.asp",
        "keywords": ["master", "campeonato de españa master", "veteranos"],
        "official_fixtures": [
            {
                "title": "İspanya Masterlar Salon Şampiyonası (Campeonato de España Master en Pista Cubierta)",
                "city": "Madrid (Gallur)",
                "start_date": "2026-03-06",
                "end_date": "2026-03-08",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.rfea.es/normas/pdf/ce_master_pc_2026.pdf",
                "website_url": "https://www.rfea.es"
            },
            {
                "title": "İspanya Masterlar Açık Saha Şampiyonası (Campeonato de España Master al Aire Libre)",
                "city": "Málaga (Estadio de Atletismo)",
                "start_date": "2026-06-19",
                "end_date": "2026-06-21",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://www.rfea.es/normas/pdf/ce_master_al_2026.pdf",
                "website_url": "https://www.rfea.es"
            }
        ]
    },
    {
        "country_code": "FR",
        "country_name": "Fransa",
        "federation_name": "FFA",
        "full_name": "Fédération Française d'Athlétisme (FFA Masters)",
        "base_url": "https://www.athle.fr",
        "calendar_url": "https://www.athle.fr/asp.net/main.html/html.aspx?htmlid=18",
        "keywords": ["masters", "championnats de france masters", "vétérans"],
        "official_fixtures": [
            {
                "title": "Fransa Masterlar Salon Atletizm Şampiyonası (France Masters en Salle)",
                "city": "Nantes (Pierre-Quinon)",
                "start_date": "2026-02-20",
                "end_date": "2026-02-22",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.athle.fr/reglements/masters-indoor-2026.pdf",
                "website_url": "https://www.athle.fr"
            },
            {
                "title": "Fransa Masterlar Açık Saha Şampiyonası (Championnats de France Masters)",
                "city": "Tours (Grand Stade)",
                "start_date": "2026-06-26",
                "end_date": "2026-06-28",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://www.athle.fr/reglements/masters-piste-2026.pdf",
                "website_url": "https://www.athle.fr"
            },
            {
                "title": "Meeting Miramas Métropole (Masters & Elite Indoor)",
                "city": "Miramas (Stadium Miramas Métropole)",
                "start_date": "2027-01-22",
                "end_date": "2027-01-23",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.athle.fr/reglements/meeting-miramas-2027.pdf",
                "website_url": "https://www.athle.fr"
            },
            {
                "title": "Meeting européen Masters en salle / EMA League",
                "city": "Lyon / Miramas",
                "start_date": "2027-01-30",
                "end_date": "2027-01-31",
                "venue_type": "indoor",
                "scope": "europe",
                "regulations_url": "https://european-masters-athletics.org/regulations/ema-league-indoor-2027.pdf",
                "website_url": "https://european-masters-athletics.org"
            },
            {
                "title": "Fransa Masters Salon Şampiyonası (Championnats de France Masters en Salle 2027)",
                "city": "Nantes (Pierre-Quinon)",
                "start_date": "2027-02-19",
                "end_date": "2027-02-21",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://www.athle.fr/reglements/masters-indoor-2027.pdf",
                "website_url": "https://www.athle.fr"
            }
        ]
    },
    {
        "country_code": "PL",
        "country_name": "Polonya",
        "federation_name": "PZLAM",
        "full_name": "Polski Związek Lekkiej Atletyki Masters",
        "base_url": "https://pzlam.pl",
        "calendar_url": "https://pzlam.pl/kalendarz-imprez/",
        "keywords": ["masters", "mistrzostwa polski masters", "halowe mistrzostwa"],
        "official_fixtures": [
            {
                "title": "Polonya Masterlar Salon Şampiyonası (Halowe Mistrzostwa Polski Masters)",
                "city": "Toruń (Arena Toruń)",
                "start_date": "2026-02-07",
                "end_date": "2026-02-08",
                "venue_type": "indoor",
                "scope": "regional",
                "regulations_url": "https://pzlam.pl/regulaminy/hmpm-2026.pdf",
                "website_url": "https://pzlam.pl"
            },
            {
                "title": "Polonya Masterlar Açık Saha Şampiyonası (Mistrzostwa Polski Masters)",
                "city": "Włocławek",
                "start_date": "2026-07-04",
                "end_date": "2026-07-05",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://pzlam.pl/regulaminy/mpm-2026.pdf",
                "website_url": "https://pzlam.pl"
            }
        ]
    },
    {
        "country_code": "BG",
        "country_name": "Bulgaristan",
        "federation_name": "BAF",
        "full_name": "Bulgarian Athletics Federation / Masters Club",
        "base_url": "https://baf.bg",
        "calendar_url": "https://baf.bg/calendar/",
        "keywords": ["masters", "ветерани", "държавно първенство"],
        "official_fixtures": [
            {
                "title": "Bulgaristan Masterlar Açık Saha Şampiyonası",
                "city": "Sofya (Vasil Levski)",
                "start_date": "2026-06-06",
                "end_date": "2026-06-07",
                "venue_type": "outdoor",
                "scope": "balkan",
                "regulations_url": "https://baf.bg/regulations/masters-2026.pdf",
                "website_url": "https://baf.bg"
            }
        ]
    },
    {
        "country_code": "RO",
        "country_name": "Romanya",
        "federation_name": "FRA",
        "full_name": "Federatia Romana de Atletism Masters",
        "base_url": "https://www.fra.ro",
        "calendar_url": "https://www.fra.ro/competitii/",
        "keywords": ["masters", "veterani", "campionatul national masters"],
        "official_fixtures": [
            {
                "title": "Romanya Masterlar Açık Saha Atletizm Şampiyonası",
                "city": "Bükreş (Stadionul Iolanda Balas)",
                "start_date": "2026-07-18",
                "end_date": "2026-07-19",
                "venue_type": "outdoor",
                "scope": "balkan",
                "regulations_url": "https://www.fra.ro/regulamente/masters-2026.pdf",
                "website_url": "https://www.fra.ro"
            }
        ]
    },
    {
        "country_code": "RS",
        "country_name": "Sırbistan",
        "federation_name": "SAS",
        "full_name": "Srpski Atletski Savez (SAS Masters)",
        "base_url": "https://www.sas.org.rs",
        "calendar_url": "https://www.sas.org.rs/kalendar/",
        "keywords": ["veterani", "masters", "prvenstvo srbije"],
        "official_fixtures": [
            {
                "title": "Sırbistan Masterlar Açık Saha Şampiyonası (Prvenstvo Srbije za Veterane)",
                "city": "Kruševac",
                "start_date": "2026-06-27",
                "end_date": "2026-06-28",
                "venue_type": "outdoor",
                "scope": "balkan",
                "regulations_url": "https://www.sas.org.rs/propisi/veterani-2026.pdf",
                "website_url": "https://www.sas.org.rs"
            }
        ]
    },
    {
        "country_code": "SE",
        "country_name": "İsveç",
        "federation_name": "SFIF",
        "full_name": "Svensk Friidrott (Veteran)",
        "base_url": "https://www.friidrott.se",
        "calendar_url": "https://www.friidrott.se/tavling-landslag/tavling/veteran-sm/",
        "keywords": ["veteran-sm", "masters", "veteran"],
        "official_fixtures": [
            {
                "title": "İsveç Masterlar Şampiyonası (Veteran-SM Friidrott)",
                "city": "Uppsala",
                "start_date": "2026-08-07",
                "end_date": "2026-08-09",
                "venue_type": "outdoor",
                "scope": "regional",
                "regulations_url": "https://www.friidrott.se/inbjudan/veteran-sm-2026.pdf",
                "website_url": "https://www.friidrott.se"
            }
        ]
    }
]
