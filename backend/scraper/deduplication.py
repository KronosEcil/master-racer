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


def extract_primary_city_slug(city: str, country_name: str = "") -> str:
    """Şehir bilgisinden parantez içi tesis adlarını ve geçersiz yer tutucuları temizler."""
    if not city or not isinstance(city, str):
        return ""
    c_clean = re.sub(r"\(.*?\)", "", city).strip()
    if "," in c_clean:
        parts = [p.strip() for p in c_clean.split(",") if p.strip()]
        if parts:
            c_clean = (
                parts[-1]
                if any(w in parts[0].lower() for w in ["centre", "center", "stadium", "arena", "athletics", "park"])
                else parts[0]
            )
    c_clean = re.split(r"[\-\/]", c_clean)[0].strip()
    slug = normalize_text(c_clean)
    invalid_slugs = {
        "", "na", "n-a", "none", "null", "tbd", "tbc", "tba", "ybd", "unknown",
        "belirlenecek", "belirsiz", "to-be-determined", "to-be-announced", "to-be-confirmed",
        "not-specified", "unspecified", "not-announced", "various", "multiple",
        "a-determiner", "por-determinar", "da-definire", "avrupa", "uluslararasi",
        normalize_text(country_name),
    }
    return "" if slug in invalid_slugs else slug


def is_valid_competition_title(title: str) -> bool:
    """
    Başlığın gerçek bir 35+ Masterlar yarışmasına ait olduğunu doğrular.
    Elit/Büyükler, U14-U23, Olimpik Deneme, TOHM, SEM ve alt döküman başlıklarını reddeder.
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

    strict_blocked_phrases = [
        "olimpik deneme", "salon olimpik", "tohm", "sem-thom", "sem deneme", "sem atletizm",
        "u12", "u14", "u16", "u18", "u20", "u23", "küçükler", "yıldızlar", "gençler",
        "meb okullar", "okul-sgm", "anadolu yıldızlar", "gençlik kupası", "para-atlet", "paralimpik"
    ]
    if any(sbp in t_lower for sbp in strict_blocked_phrases):
        return False

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
    """Yarışma başlığından (kişi veya şehir adına bağımlı olmadan) branş/mesafe alt türünü belirler."""
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
    if any(w in title_norm for w in ["cross", "kros", "veldloop", "terrang", "maasto", "campo-a-traves", "corta-mato"]):
        return "CROSS-RELAY" if any(r in title_norm for r in ["relay", "relais", "staffel", "stafett", "relevos"]) else "CROSS"
    if any(w in title_norm for w in ["throw", "atma", "winterwurf", "wurf", "lancer", "lanzamiento", "lanci", "hammer", "marteau", "martillo", "martello", "rzut", "lancamentos"]):
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
    if any(w in title_norm for w in ["half-marathon", "halbmarathon", "meia-maratona", "media-maraton", "maratonina", "semi-marathon", "mezza-maratona", "21k"]):
        return "HALF-MARATHON"
    if any(w in title_norm for w in ["road-relay", "relais-sur-route", "strassenstaffel"]) or ("relay" in title_norm and "road" in title_norm):
        return "ROAD-RELAY"
    if any(w in title_norm for w in ["mountain", "trail", "dag", "berg", "montagna", "montana", "fell", "emmtr", "emamr", "emorr", "off-road"]):
        return "MOUNTAIN-TRAIL"
    if any(w in title_norm for w in ["marathon", "maraton", "maratona", "42k", "yol-kosusu", "road", "yol", "strada", "ruta", "estrada"]):
        return "MARATHON-ROAD"
    if any(w in title_norm for w in ["team-dm", "team-championship", "club-championship", "interclubs", "societari", "clubes"]):
        return "TEAM"
    if any(w in title_norm for w in ["relay", "relais", "staffel", "staffette", "relevos"]):
        return "TRACK-RELAY"
    if is_indoor:
        return "SALON"
    return "ACIK-SAHA"


def get_canonical_key(title_or_comp: Any, start_date: str = "", country: str = "", city: str = "") -> str:
    """
    Sabit şehir veya kişi adına bağımlı olmadan; yarışmanın kapsamı (Dünya, Avrupa, Balkan, Ulusal,
    EMA Ligi/Bölgesel), yılı, ülkesi ve branş alt türü üzerinden deterministik tekil anahtar üretir.
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
    city_core = extract_primary_city_slug(city or "")
    title_norm = normalize_text(title or "")
    year = date_part[:4] if len(date_part) >= 4 else "2026"
    subtype = _detect_discipline_subtype(title_norm)

    if any(w in title_norm for w in ["world-masters", "wma-", "-wma", "wmg", "dunya-master"]):
        return f"WORLD-{year}-{subtype}"

    is_ema_league = any(w in title_norm for w in ["ema-league", "ema-ligi", "meeting-europeen", "speed-masters"])
    if not is_ema_league and any(
        w in title_norm
        for w in [
            "european-masters", "european-stadia", "avrupa-master",
            "emaci", "emacs", "emacns", "emmtrc", "emamrc", "emorrc",
            "ema-marathon", "ema-mountain", "ema-indoor", "ema-stadia", "ema-non-stadia", "ema-off-road", "ema-championship"
        ]
    ):
        return f"EUROPE-{year}-{subtype}"

    if any(w in title_norm for w in ["balkan-master", "balkan-veteran", "bamaci", "bamacs", "bamam"]):
        return f"BALKAN-{year}-{subtype}"

    if "nordic-masters" in title_norm:
        return f"NORDIC-{year}-{subtype}"
    if "baltic" in title_norm and "masters" in title_norm:
        return f"BALTIC-{year}-{subtype}"

    if is_ema_league:
        loc_part = city_core.upper() if city_core else date_part
        return f"{country_part}-{year}-EMA-LEAGUE-{loc_part}"

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

    if city_core:
        return f"{country_part}-{date_part}-{city_core}-{subtype}"
    return f"{country_part}-{date_part}-{title_norm[:25]}"


def generate_competition_id(title: str, start_date: str = "", country: str = "", city: str = "") -> str:
    """Her yarışma için deterministik, benzersiz ve kanonik bir ID oluşturur."""
    canonical = get_canonical_key(title, start_date, country, city)
    return normalize_text(canonical)


def deduplicate_competitions(competitions: List[Competition]) -> List[Competition]:
    """Taranan yarışmalar listesindeki kopyaları API kotası harcamadan deterministik olarak tekilleştirir."""
    unique_map: dict[str, Competition] = {}

    for comp in competitions:
        if not is_valid_competition_title(comp.title):
            continue

        key = get_canonical_key(comp)
        if key not in unique_map:
            comp.id = normalize_text(key)
            comp.content_hash = compute_content_hash(comp)
            unique_map[key] = comp
        else:
            existing = unique_map[key]
            existing_is_range = bool(existing.end_date and existing.end_date > existing.start_date)
            incoming_is_range = bool(comp.end_date and comp.end_date > comp.start_date)

            if incoming_is_range and not existing_is_range:
                existing.start_date = comp.start_date
                existing.end_date = comp.end_date
            elif existing.start_date == comp.start_date and comp.end_date > existing.end_date:
                existing.end_date = comp.end_date

            ex_valid_city = extract_primary_city_slug(existing.city, existing.country_name)
            in_valid_city = extract_primary_city_slug(comp.city, comp.country_name)
            if in_valid_city and (not ex_valid_city or len(comp.city or "") > len(existing.city or "")):
                existing.city = comp.city
                if comp.country and comp.country not in ("INT", "EU"):
                    existing.country = comp.country
                    existing.country_name = comp.country_name
            if len(comp.website_url or "") > len(existing.website_url or ""):
                existing.website_url = comp.website_url

            existing.content_hash = compute_content_hash(existing)
            unique_map[key] = existing

    # İkinci Aşama: Aynı başlangıç tarihine (start_date) ve aynı kapsama sahip olup,
    # bir tanesi başlıkta branş detayı ("Indoor" vb.) eksik yazıldığı için genel "-ACIK-SAHA"
    # anahtarına düşen ikiz kayıtları (örn. WORLD-2027-SALON ve WORLD-2027-ACIK-SAHA, 2027-03-18) birleştir.
    keys_to_remove = set()
    all_keys = list(unique_map.keys())
    for k in all_keys:
        if k.endswith("-ACIK-SAHA"):
            prefix = k[: -len("-ACIK-SAHA")]
            generic_comp = unique_map[k]
            for other_k in all_keys:
                if other_k != k and other_k.startswith(prefix) and not other_k.endswith("-ACIK-SAHA"):
                    specific_comp = unique_map[other_k]
                    if specific_comp.start_date == generic_comp.start_date:
                        if generic_comp.end_date > specific_comp.end_date:
                            specific_comp.end_date = generic_comp.end_date
                        if extract_primary_city_slug(generic_comp.city, generic_comp.country_name) and not extract_primary_city_slug(specific_comp.city, specific_comp.country_name):
                            specific_comp.city = generic_comp.city
                        specific_comp.content_hash = compute_content_hash(specific_comp)
                        keys_to_remove.add(k)
                        break

    for rk in keys_to_remove:
        unique_map.pop(rk, None)

    return list(unique_map.values())


def compute_content_hash(comp: Competition) -> str:
    """Yarışmanın kritik alanlarının (Başlık, Tarihler, Şehir, Ülke, Kapsam, Durum) SHA-256 özetini üretir."""
    payload = f"{comp.title.strip()}|{comp.start_date}|{comp.end_date}|{comp.city.strip()}|{comp.country.strip()}|{comp.scope}|{comp.status}"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def evaluate_change(existing: Optional[dict], incoming: Competition) -> Tuple[str, bool]:
    """Değişiklik analizi yapar."""
    if not existing:
        return "NEW", True

    ex_start = existing.get("start_date", "")
    ex_end = existing.get("end_date", "")
    if ex_start == incoming.start_date and ex_end > ex_start and incoming.end_date == incoming.start_date:
        incoming.end_date = ex_end
        incoming.content_hash = compute_content_hash(incoming)

    ex_city = extract_primary_city_slug(existing.get("city", ""), existing.get("country_name", ""))
    in_city = extract_primary_city_slug(incoming.city, incoming.country_name)

    # Mevcut kayıtta geçerli bir şehir varken yeni gelen kayıtta şehir belirtilmemişse mevcut konumu koru
    if ex_city and not in_city:
        incoming.city = existing.get("city", incoming.city)
        incoming.country = existing.get("country", incoming.country)
        incoming.country_name = existing.get("country_name", incoming.country_name)
        incoming.content_hash = compute_content_hash(incoming)
        in_city = ex_city

    existing_hash = existing.get("content_hash", "")
    new_hash = incoming.content_hash

    if existing_hash == new_hash:
        return "UNCHANGED", False

    date_changed = (
        existing.get("start_date") != incoming.start_date
        or existing.get("end_date") != incoming.end_date
    )

    location_changed = (
        existing.get("country") != incoming.country
        or (ex_city != in_city and bool(in_city))
    )

    if date_changed:
        return "DATE_CHANGED", True

    if location_changed:
        return "LOCATION_CHANGED", True

    return "UPDATED", False
