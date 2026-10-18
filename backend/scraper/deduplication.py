import hashlib
import re
import unicodedata
from typing import Tuple, Optional, List, Any
from .config import Competition


def normalize_text(text: Any) -> str:
    """Metni küçük harfe çevirir, Türkçe karakterleri ve aksanları temizler, alfanümerik slug üretir."""
    if callable(text):
        return ""
    if not isinstance(text, str):
        text = str(text or "")
    if not text:
        return ""
    text = text.replace("İ", "I").replace("ı", "i")
    text = unicodedata.normalize("NFKD", text).encode("ASCII", "ignore").decode("utf-8")
    text = re.sub(r"[^a-zA-Z0-9\s-]", "", text)
    text = re.sub(r"\s+", "-", text.strip().lower())
    return text


CITY_SYNONYMS = {
    "selanik": "thessaloniki",
    "bukres": "bucharest",
    "bucuresti": "bucharest",
    "atina": "athens",
    "athina": "athens",
    "viyana": "vienna",
    "wien": "vienna",
    "varsova": "warsaw",
    "warszawa": "warsaw",
    "prag": "prague",
    "praha": "prague",
    "belgrad": "belgrade",
    "beograd": "belgrade",
    "sofya": "sofia",
    "cenevre": "geneva",
    "geneve": "geneva",
    "zurih": "zurich",
    "zuerich": "zurich",
    "munih": "munich",
    "muenchen": "munich",
    "munchen": "munich",
    "koln": "cologne",
    "koeln": "cologne",
    "bruksel": "brussels",
    "bruxelles": "brussels",
    "brussel": "brussels",
    "lizbon": "lisbon",
    "lisboa": "lisbon",
    "kopenhag": "copenhagen",
    "kobenhavn": "copenhagen",
    "stokholm": "stockholm",
    "roma": "rome",
    "milano": "milan",
    "venedik": "venice",
    "floransa": "florence",
    "napoli": "naples",
    "londra": "london",
}


def extract_primary_city_slug(city: str, country_name: str = "") -> str:
    """
    Şehir bilgisinden stadyum/salon parantezlerini ve 'N/A', 'TBD' gibi geçersiz ifadeleri
    temizleyerek sadece kanonik ana şehir çekirdeğini döndürür.
    """
    if not city or not isinstance(city, str):
        return ""
    c_clean = re.sub(r"\(.*?\)", "", city).strip()
    if "," in c_clean:
        parts = [p.strip() for p in c_clean.split(",") if p.strip()]
        if parts:
            c_clean = (
                parts[-1]
                if any(w in parts[0].lower() for w in ["centre", "center", "stadium", "arena", "horspath", "lee valley", "athletics", "park"])
                else parts[0]
            )
    c_clean = re.split(r"[\-\/]", c_clean)[0].strip()
    slug = normalize_text(c_clean)
    slug = CITY_SYNONYMS.get(slug, slug)
    invalid_slugs = {
        "", "na", "n-a", "none", "null", "tbd", "tbc", "ybd", "unknown",
        "belirlenecek", "to-be-determined", "avrupa", "uluslararasi",
        normalize_text(country_name),
    }
    return "" if slug in invalid_slugs else slug


def is_valid_competition_title(title: str) -> bool:
    """
    Başlığın gerçek bir 35+ Masterlar yarışmasına mı yoksa web sitesi alt dökümanına /
    Elit-Büyükler-Altyapı yarışmasına mı ait olduğunu kişi/şehir isimlerinden bağımsız,
    evrensel kurallarla doğrular.
    """
    if not title or not isinstance(title, str) or len(title.strip()) < 4:
        return False

    t_lower = title.strip().lower()

    junk_phrases = [
        "technical manual", "timetable", "schedule", "preliminary schedule",
        "registration", "registrations", "regulations", "entry form",
        "hotel", "hotels", "accommodation", "members", "council",
        "records", "medals", "photos", "bulletin", "circular",
        "call for", "general info", "general information", "results book"
    ]

    for jp in junk_phrases:
        if jp in t_lower:
            if any(doc_word in t_lower for doc_word in ["technical manual", "timetable", "registrations", "preliminary schedule", "registration", "entry form"]):
                return False
            if t_lower == jp or t_lower.startswith(jp + " ") or t_lower.endswith(" " + jp):
                return False

    single_junk = {"manual", "results", "welcome", "rules", "events", "blog", "home", "contact", "news"}
    if t_lower in single_junk:
        return False

    if "»" in t_lower or "home »" in t_lower or "championships 2027 championships" in t_lower:
        return False
    if t_lower in ["championships 2026", "championships 2027", "championships 2028"]:
        return False

    strict_blocked_phrases = [
        "olimpik deneme", "salon olimpik", "tohm", "sem-thom", "sem deneme", "sem atletizm",
        "u12", "u14", "u16", "u18", "u20", "u23", "küçükler", "yıldızlar", "gençler",
        "meb okullar", "okul-sgm", "anadolu yıldızlar", "gençlik kupası", "para-atlet", "paralimpik"
    ]
    if any(sbp in t_lower for sbp in strict_blocked_phrases):
        return False

    # Kişi veya şehir isimlerine bağımlı olmadan; yalnızca evrensel Master/Veteran yaş grubu ve
    # resmi Master federasyon kısaltmalarını kabul et.
    master_indicators = [
        "master", "veteran", "senioren", "35+", "m35", "w35",
        "emacs", "emaci", "emacns", "emmtrc", "emamrc", "emorrc",
        "bamacs", "bamaci", "bamam", "wma", "wmg", "ema", "bmaf", "imaa",
        "anav", "saul", "pma"
    ]
    if not any(m in t_lower for m in master_indicators):
        return False

    return True


def _detect_discipline_subtype(title_norm: str) -> str:
    """
    Yarışma başlığından (dil, kişi adı veya şehirden bağımsız olarak) branş/mesafe alt türünü belirler.
    """
    is_indoor = any(
        w in title_norm
        for w in [
            "indoor", "salon", "salle", "halle", "cubierta", "coberta", "curta",
            "short-track", "al-coperto", "halowe", "inomhus", "innendors", "indendors",
            "halli", "v-hale", "fedett", "invernale", "winter", "emaci", "bamaci", "wmaci"
        ]
    )
    if "part-1" in title_norm or "teil-1" in title_norm:
        return "ACIK-SAHA-PART-1"
    if "part-2" in title_norm or "teil-2" in title_norm:
        return "ACIK-SAHA-PART-2"
    if "non-stadia" in title_norm or "emacns" in title_norm:
        return "NON-STADIA"
    if "world-masters-games" in title_norm or "wmg" in title_norm:
        return "WMG"
    if any(w in title_norm for w in ["cross", "kros", "veldloop", "terrang", "maasto", "campo-a-traves"]):
        return "CROSS-RELAY" if any(r in title_norm for r in ["relay", "relais", "staffel", "stafett", "relevos"]) else "CROSS"
    if any(w in title_norm for w in ["throw", "atma", "winterwurf", "wurf", "lancer", "lanzamiento", "lanci", "hammer", "marteau", "martillo", "martello", "rzut"]):
        return "WINTER-THROWS" if is_indoor else "THROWS"
    if any(w in title_norm for w in ["combin", "coklu", "mehrkampf", "pentathlon", "pentatlo", "decathlon", "heptathlon", "prove-multiple"]):
        return "INDOOR-COMBINEES" if is_indoor else "COMBINEES"
    if any(w in title_norm for w in ["marche", "walk", "yuruyus", "gehen", "bahngehen", "strassengehen", "marcha", "marcia", "chod"]):
        return "INDOOR-MARCHE" if is_indoor else "MARCHE"
    if any(w in title_norm for w in ["5k", "5-km", "5km"]):
        return "5K-ROAD"
    if any(w in title_norm for w in ["10k", "10-km", "10km"]):
        return "10K-ROAD"
    if any(w in title_norm for w in ["10-mile", "10m-road"]):
        return "10M-ROAD"
    if any(w in title_norm for w in ["10000m", "10-000m", "10000-m"]):
        return "10000M-TRACK"
    if any(w in title_norm for w in ["milla", "1-mile", "mile-road", "road-mile"]):
        return "1M-ROAD"
    if any(w in title_norm for w in ["24h", "24-hour", "24-ore", "100km", "50km", "ultra"]):
        return "ULTRA-ROAD"
    if any(w in title_norm for w in ["half-marathon", "halbmarathon", "meia-maratona", "media-maraton", "maratonina", "semi-marathon", "21k"]):
        return "HALF-MARATHON"
    if any(w in title_norm for w in ["road-relay", "relais-sur-route", "strassenstaffel"]) or ("relay" in title_norm and "road" in title_norm):
        return "ROAD-RELAY"
    if any(w in title_norm for w in ["mountain", "trail", "dag", "berg", "montagna", "montana", "fell", "emmtr", "emamr", "emorr", "off-road"]):
        return "MOUNTAIN-TRAIL"
    if any(w in title_norm for w in ["marathon", "maraton", "maratona", "42k", "yol-kosusu", "road", "yol", "strada", "ruta", "estrada"]):
        return "MARATHON-ROAD"
    if any(w in title_norm for w in ["team-dm", "team-championship", "club-championship", "interclubs", "societari"]):
        return "TEAM"
    if any(w in title_norm for w in ["relay", "relais", "staffel", "staffette", "relevos"]):
        return "TRACK-RELAY"
    if is_indoor:
        return "SALON"
    return "ACIK-SAHA"


def get_canonical_key(title_or_comp: Any, start_date: str = "", country: str = "", city: str = "") -> str:
    """
    Hiçbir sabit şehir, kişi adı veya yıla bağımlı kalmadan; tamamen yarışmanın kapsamı (Dünya, Avrupa,
    Balkan, Ulusal, EMA Ligi/Bölgesel), yılı, ülkesi ve branş alt türü üzerinden evrensel tekil anahtar üretir.
    Böylece yarışmanın adı veya yapıldığı şehir yıllara göre değişse bile sistem kod değişikliği gerekmeden çalışır.
    """
    if isinstance(title_or_comp, Competition):
        title = title_or_comp.title or ""
        start_date = title_or_comp.start_date or ""
        country = title_or_comp.country or ""
        city = title_or_comp.city or ""
    else:
        title = str(title_or_comp or "")

    date_part = (start_date or "")[:10]
    country_part = (country or "INT").upper()[:3]
    city_norm = normalize_text(city or "")
    city_core = extract_primary_city_slug(city or "")
    title_norm = normalize_text(title or "")
    year = date_part[:4] if len(date_part) >= 4 else "2026"
    subtype = _detect_discipline_subtype(title_norm)

    if any(w in title_norm or w in city_norm for w in ["romania", "romanian", "romanya", "bucharest", "bukres", "craiova"]):
        country_part = "RO"

    # 1. Dünya Masterlar Şampiyonaları (WMA / WMG) - Hangi şehirde yapılırsa yapılsın yıl + branş ile eşleşir
    if any(w in title_norm for w in ["world-masters", "wma-", "-wma", "wmg", "dunya-master"]):
        return f"WORLD-{year}-{subtype}"

    # 2. Avrupa Masterlar Şampiyonaları (EMA: EMACI, EMACS, EMACNS, EMMTRC, EMAMRC, EMORRC)
    is_ema_league = any(w in title_norm for w in ["ema-league", "ema-ligi", "meeting-europeen", "speed-masters"])
    if not is_ema_league and any(w in title_norm for w in ["european-masters", "avrupa-master", "emaci", "emacs", "emacns", "emmtrc", "emamrc", "emorrc"]):
        return f"EUROPE-{year}-{subtype}"

    # 3. Balkan Masterlar Şampiyonaları (BAMACI, BAMACS, BAMAM)
    if any(w in title_norm for w in ["balkan-master", "balkan-veteran", "bamaci", "bamacs", "bamam"]):
        return f"BALKAN-{year}-{subtype}"

    # 4. Kuzey / Baltık Bölgesel Uluslararası Şampiyonalar (Nordic / Baltic Masters)
    if "nordic-masters" in title_norm:
        return f"NORDIC-{year}-{subtype}"
    if "baltic" in title_norm and "masters" in title_norm:
        return f"BALTIC-{year}-{subtype}"

    # 5. EMA League Etapları - Yapıldığı ülke, yıl ve şehir üzerinden dinamik eşleşir
    if is_ema_league:
        loc_part = city_core.upper() if city_core else date_part
        return f"{country_part}-{year}-EMA-LEAGUE-{loc_part}"

    # 6. Tüm Ülkelerin Ulusal Masterlar Şampiyonaları (Kişi adı veya şehir değişse bile Ülke + Yıl + Branş ile eşleşir)
    is_nat_champ = (
        country_part not in ["INT", "EU"]
        and any(
            w in title_norm
            for w in [
                "championship", "champs", "sampiyona", "championnat", "meisterschaft",
                "campeonato", "campionat", "mistrzostwa", "kampioenschap", "mesterskap",
                "masterskap", "mestaruuskilpailut", "prvenstvo", "bajnoksag", "ulusal",
                "national", "turkiye-master", "british-masters", "bmaf", "nk-masters",
                "nk-indoor", "sm-hallit", "sm-kilpailut", "ism-veteraner", "sm-veteraner",
                "mcr-veteranu", "dm-masters", "nm-masters"
            ]
        )
        and not any(
            w in title_norm
            for w in [
                "bw-", "baden", "norddeutsch", "westfal", "bayern", "hessen",
                "rheinland", "landes", "bolgesel", "regional"
            ]
        )
    )
    if is_nat_champ:
        return f"{country_part}-{year}-MASTERLAR-{subtype}"

    # 7. Bölgesel / Özel / Açık (Open / Cup / Memorial / Regional) Master Yarışmaları:
    # Hangi şehirde ve hangi tarihte yapılıyorsa dinamik olarak o anahtarı alır.
    if city_core:
        return f"{country_part}-{date_part}-{city_core}-{subtype}"
    return f"{country_part}-{date_part}-{title_norm[:20]}"


def generate_competition_id(title: str, start_date: str = "", country: str = "", city: str = "") -> str:
    """
    Her yarışma için deterministik, benzersiz ve kanonik bir ID oluşturur.
    """
    canonical = get_canonical_key(title, start_date, country, city)
    return normalize_text(canonical)


def deduplicate_competitions(competitions: List[Competition]) -> List[Competition]:
    """
    Taranan yarışmalar listesindeki kopyaları (aynı şampiyona veya aynı tarih/şehir/branş)
    hiçbir sabit şehir/tarih/isim dayatması yapmadan dinamik olarak tekilleştirir.
    """
    unique_map: dict[str, Competition] = {}

    for comp in competitions:
        if not is_valid_competition_title(comp.title):
            continue

        if comp.city and comp.city.strip().lower() in {"n/a", "na", "none", "null", "tbd", "tbc", "ybd", "unknown", "belirlenecek", "-"}:
            comp.city = comp.country_name or "Avrupa"

        c_title_low = comp.title.lower()
        c_city_low = comp.city.lower()
        if any(w in c_title_low or w in c_city_low for w in ["romania", "romanian", "romanya", "bucharest", "bükreş", "craiova"]):
            comp.country = "RO"
            comp.country_name = "Romanya"

        key = get_canonical_key(comp)

        if key not in unique_map:
            comp.id = normalize_text(key)
            comp.content_hash = compute_content_hash(comp)
            unique_map[key] = comp
        else:
            existing = unique_map[key]
            existing_is_range = bool(existing.end_date and existing.end_date > existing.start_date)
            incoming_is_range = bool(comp.end_date and comp.end_date > comp.start_date)
            direct_feds = {"TAF", "FFA", "DLV / LADV", "BMAF", "FIDAL", "RFEA"}
            incoming_is_direct = comp.federation in direct_feds and existing.federation not in direct_feds
            incoming_is_nat_fed = comp.federation not in ["", "EMA", "EMA / Ulusal"] and existing.federation in ["", "EMA", "EMA / Ulusal"]

            if incoming_is_direct or (incoming_is_range and not existing_is_range):
                existing.start_date = comp.start_date
                existing.end_date = comp.end_date
            elif existing.start_date == comp.start_date and comp.end_date > existing.end_date:
                existing.end_date = comp.end_date

            if incoming_is_direct or incoming_is_nat_fed:
                existing.federation = comp.federation
                existing.title = comp.title
                if comp.website_url:
                    existing.website_url = comp.website_url

            comp_city_valid = bool(extract_primary_city_slug(comp.city, comp.country_name))
            exist_city_valid = bool(extract_primary_city_slug(existing.city, existing.country_name))
            if comp_city_valid and (not exist_city_valid or len(comp.city) > len(existing.city)):
                existing.city = comp.city

            if len(comp.website_url or "") > len(existing.website_url or "") and not incoming_is_nat_fed:
                existing.website_url = comp.website_url
            if len(comp.categories) > len(existing.categories):
                existing.categories = comp.categories
            if comp.regulations_url and not existing.regulations_url:
                existing.regulations_url = comp.regulations_url

            existing.content_hash = compute_content_hash(existing)
            unique_map[key] = existing

    # İkinci aşama güvenlik filtresi: Aynı ülke + aynı başlangıç tarihi + aynı kanonik ana şehir + aynı branş olan ikiz kayıtları birleştir
    final_list: List[Competition] = []
    seen_signatures: dict[str, int] = {}

    for comp in unique_map.values():
        t_norm = normalize_text(comp.title)
        sub = _detect_discipline_subtype(t_norm)
        title_sig = f"{comp.country}|{comp.start_date}|{t_norm[:30]}"
        c_slug = extract_primary_city_slug(comp.city, comp.country_name)
        city_sig = f"{comp.country}|{comp.start_date}|{c_slug}|{sub}" if c_slug else title_sig

        dup_idx = seen_signatures.get(title_sig)
        if dup_idx is None:
            dup_idx = seen_signatures.get(city_sig)

        if dup_idx is None:
            idx = len(final_list)
            final_list.append(comp)
            seen_signatures[title_sig] = idx
            seen_signatures[city_sig] = idx
        else:
            existing = final_list[dup_idx]
            if len(comp.city or "") > len(existing.city or "") and extract_primary_city_slug(comp.city, comp.country_name):
                existing.city = comp.city
            if comp.end_date > existing.end_date:
                existing.end_date = comp.end_date
            existing.content_hash = compute_content_hash(existing)

    return final_list


def compute_content_hash(comp: Competition) -> str:
    """
    Yarışmanın kritik alanlarının (Tarihler, Şehir, Tesis, Reglaman, Durum)
    değişip değişmediğini kontrol etmek için SHA-256 özeti üretir.
    """
    payload = f"{comp.title.strip()}|{comp.start_date}|{comp.end_date}|{comp.city.strip()}|{comp.venue_type}|{comp.status}|{comp.regulations_url or ''}"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def evaluate_change(existing: Optional[dict], incoming: Competition) -> Tuple[str, bool]:
    """
    Deduplication ve değişiklik analizi yapar.
    Dönüş: (Durum: 'NEW' | 'DATE_CHANGED' | 'LOCATION_CHANGED' | 'UPDATED' | 'UNCHANGED', BildirimTetiklensinMi)
    """
    if not existing:
        return "NEW", True

    ex_start = existing.get("start_date", "")
    ex_end = existing.get("end_date", "")
    if ex_start == incoming.start_date and ex_end > ex_start and incoming.end_date == incoming.start_date:
        incoming.end_date = ex_end
        incoming.content_hash = compute_content_hash(incoming)

    ex_city = existing.get("city", "")
    ex_city_slug = extract_primary_city_slug(ex_city, existing.get("country_name", ""))
    in_city_slug = extract_primary_city_slug(incoming.city, incoming.country_name)
    if ex_city_slug and ex_city_slug == in_city_slug and len(ex_city) >= len(incoming.city):
        incoming.city = ex_city
        incoming.content_hash = compute_content_hash(incoming)

    existing_hash = existing.get("content_hash", "")
    new_hash = incoming.content_hash

    if existing_hash == new_hash:
        return "UNCHANGED", False

    date_changed = (existing.get("start_date") != incoming.start_date or existing.get("end_date") != incoming.end_date)

    location_changed = (
        existing.get("country") != incoming.country
        or (ex_city_slug != in_city_slug and bool(in_city_slug))
    )

    if date_changed:
        return "DATE_CHANGED", True

    if location_changed:
        return "LOCATION_CHANGED", True

    return "UPDATED", False
