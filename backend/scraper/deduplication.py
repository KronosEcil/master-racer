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
    """
    Şehir bilgisinden stadyum/salon parantezlerini ve 'N/A', 'TBD' gibi geçersiz ifadeleri
    temizleyerek sadece ana şehir çekirdeğini döndürür.
    Örn: 'Apeldoorn (Omnisport)' -> 'apeldoorn'
         'National Indoor Athletics Centre, Cardiff' -> 'cardiff'
         'Horspath, Oxford' -> 'oxford'
    """
    if not city or not isinstance(city, str):
        return ""
    c_clean = re.sub(r"\(.*?\)", "", city).strip()
    if "," in c_clean:
        parts = [p.strip() for p in c_clean.split(",") if p.strip()]
        # Genellikle 'Stadyum/Bölge, Şehir' formatında son parça ana şehirdir
        if parts:
            c_clean = parts[-1] if any(w in parts[0].lower() for w in ["centre", "center", "stadium", "arena", "horspath", "lee valley", "athletics"]) else parts[0]
    c_clean = re.split(r"[\-\/]", c_clean)[0].strip()
    slug = normalize_text(c_clean)
    invalid_slugs = {
        "", "na", "n-a", "none", "null", "tbd", "tbc", "ybd", "unknown",
        "belirlenecek", "to-be-determined", "avrupa", "uluslararasi",
        normalize_text(country_name),
    }
    return "" if slug in invalid_slugs else slug


def is_valid_competition_title(title: str) -> bool:
    """
    Başlığın gerçek bir 35+ Masterlar yarışmasına mı yoksa web sitesi alt dökümanına /
    Elit-Büyükler-Altyapı yarışmasına mı ait olduğunu sıkı kurallarla doğrular.
    """
    if not title or not isinstance(title, str) or len(title.strip()) < 4:
        return False

    t_lower = title.strip().lower()

    # Kesinlikle yarışma olmayan teknik doküman ve menü kalıpları
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

    # Olimpik deneme, TOHM, SEM, okul ve gençlik yarışmalarını koşulsuz olarak ele
    strict_blocked_phrases = [
        "olimpik deneme", "salon olimpik", "tohm", "sem-thom", "sem deneme", "sem atletizm",
        "u12", "u14", "u16", "u18", "u20", "u23", "küçükler", "yıldızlar", "gençler",
        "meb okullar", "okul-sgm", "anadolu yıldızlar", "gençlik kupası", "para-atlet", "paralimpik"
    ]
    if any(sbp in t_lower for sbp in strict_blocked_phrases):
        return False

    # Her geçerli yarışma mutlaka bir Master / Veteran / Resmi Master Şampiyona göstergesi içermelidir!
    # Bu kural "Balkan Salon Atletizm Şampiyonası" (Büyükler/Elit) gibi Master olmayan yarışmaları %100 engeller.
    master_indicators = [
        "master", "veteran", "senioren", "35+", "m35", "w35",
        "emacs", "emaci", "emacns", "emmtrc", "emamrc", "emorrc",
        "bamacs", "bamaci", "wma", "wmg", "ema", "bmaf", "imaa",
        "anav", "saul", "pma", "alkan öral", "alkan oral", "murat akman",
        "ismail akçay", "ismail akcay", "fahir özgüden", "fahir ozguden",
        "christina ziemann", "bw-meisterschaften"
    ]
    if not any(m in t_lower for m in master_indicators):
        return False

    return True


def get_canonical_key(title_or_comp: Any, start_date: str = "", country: str = "", city: str = "") -> str:
    """
    Farklı sorgulardan ve kaynaklardan gelen aynı yarışmaları eşleştirmek için
    standartlaştırılmış, şehir/stadyum yazım farklarından etkilenmeyen tekil anahtar üretir.
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

    # Kansai / World Masters Games (Japonya)
    if "kansai" in title_norm or "kansai" in city_norm or "world-masters-games" in title_norm or "wmg" in title_norm:
        return "JP-2027-05-14-WMG-KANSAI"

    # 1. BAMACS Craiova
    if "craiova" in title_norm or "craiova" in city_norm or ("bamacs" in title_norm and year == "2026"):
        return "RO-2026-09-17-BAMACS-CRAIOVA"

    # 2. BAMACI Novo Mesto
    if "novo-mesto" in title_norm or "novo-mesto" in city_norm or ("bamaci" in title_norm and year == "2026"):
        return "SI-2026-03-14-BAMACI-NOVO-MESTO"

    # 3. WMA Marathon Zagreb
    if "zagreb" in title_norm or "zagreb" in city_norm or ("marathon" in title_norm and country_part == "HR"):
        return f"HR-{date_part}-WMA-MARATHON-ZAGREB"

    # 4. BW-Meisterschaften Bernhausen
    if "bernhausen" in city_norm or "bernhausen" in title_norm:
        return f"DE-{date_part}-BW-HALBMARATHON-BERNHAUSEN"

    # Türkiye Masterlar Şampiyonaları (TAF ve Gemini AI Eşleşmesi)
    is_tr = country_part == "TR" or any(w in title_norm for w in ["turkiye", "turkish", "turk", "alkan-oral", "murat-akman", "ismail-akcay", "fahir-ozguden"])
    if is_tr and any(w in title_norm for w in ["master", "alkan-oral", "murat-akman", "ismail-akcay", "fahir-ozguden"]):
        if any(w in title_norm for w in ["kros", "cross"]):
            return f"TR-{year}-MASTERLAR-CROSS"
        if any(w in title_norm for w in ["atma", "mesafeler-ve-atmalar", "throws"]):
            return f"TR-{year}-MASTERLAR-THROWS"
        if any(w in title_norm for w in ["ismail-akcay", "yol-kosusu"]):
            return f"TR-{year}-ISMAIL-AKCAY-YOL"
        if any(w in title_norm for w in ["fahir-ozguden", "hurdles-cup", "rekor-deneme"]) and "salon" not in title_norm:
            return f"TR-{year}-FAHIR-OZGUDEN-CUP"
        if any(w in title_norm for w in ["salon", "indoor", "alkan-oral"]):
            return f"TR-{year}-MASTERLAR-SALON"
        if any(w in title_norm for w in ["acik", "outdoor", "murat-akman", "izmir"]) or ("sampiyona" in title_norm or "champs" in title_norm):
            return f"TR-{year}-MASTERLAR-ACIK-SAHA"

    # Fransa Masterlar Şampiyonaları ve EMA Ligi
    is_fr = country_part == "FR" or "frans" in title_norm or "france" in title_norm
    if is_fr and ("ema-league" in title_norm or "meeting-europeen" in title_norm):
        return "FR-2027-EMA-LEAGUE-NANTES"
    if is_fr and "meeting" not in title_norm:
        if "combin" in title_norm or "coklu" in title_norm:
            return f"FR-{year}-MASTERLAR-COMBINEES"
        if "marche" in title_norm or "yuruyus" in title_norm:
            return f"FR-{year}-MASTERLAR-MARCHE"
        if "salon" in title_norm or "salle" in title_norm or "indoor" in title_norm or "saint-brieuc" in city_norm or "nantes" in city_norm:
            return f"FR-{year}-MASTERLAR-SALON"
        if "acik" in title_norm or "outdoor" in title_norm or "piste" in title_norm or "stadia" in title_norm or "epinal" in city_norm or "tours" in city_norm or "belfort" in city_norm or "championnats-de-france-masters" in title_norm:
            return f"FR-{year}-MASTERLAR-ACIK-SAHA"

    # İsviçre Part 1 & Part 2 ayrımı
    if country_part == "CH" and ("part-1" in title_norm or "teil-1" in title_norm):
        return f"CH-{year}-MASTERLAR-ACIK-SAHA-PART-1"
    if country_part == "CH" and ("part-2" in title_norm or "teil-2" in title_norm):
        return f"CH-{year}-MASTERLAR-ACIK-SAHA-PART-2"

    # EMA Büyük Şampiyonaları (Şehir ve Kısaltma Bazlı Kesin Eşleşme)
    if "ojos" in city_norm or "emmtr" in title_norm:
        return "EMA-2027-EMMTRC-OJOS"
    if "jyvaskyla" in city_norm or "emacs" in title_norm:
        return "EMA-2027-EMACS-JYVASKYLA"
    if "lucerne" in city_norm or "emamrc" in title_norm:
        return "EMA-2027-EMAMRC-LUCERNE"
    if "glasgow" in city_norm or ("emaci" in title_norm and year == "2028"):
        return "EMA-2028-EMACI-GLASGOW"
    if "galway" in city_norm or "emacns" in title_norm:
        return "EMA-2028-EMACNS-GALWAY"
    if ("torun" in city_norm or "emaci" in title_norm) and year == "2026" and ("europe" in title_norm or "avrupa" in title_norm or "emaci" in title_norm):
        return "EMA-2026-EMACI-TORUN"

    # 5. Evrensel Avrupa Ulusal Şampiyona Anahtarı (Tüm Ülkeler & BMAF/FIDAL/RFEA/DLV/PZLA vb.)
    is_nat_champ = (
        country_part not in ["INT", "EU"]
        and any(
            w in title_norm
            for w in [
                "championship", "champs", "sampiyona", "championnat", "meisterschaft",
                "campeonato", "campionat", "mistrzostwa", "kampioenschap", "mesterskap",
                "masterskap", "mestaruuskilpailut", "prvenstvo", "bajnoksag", "ulusal",
                "national", "bmaf", "nk-masters", "nk-indoor", "sm-hallit", "sm-kilpailut",
                "ism-veteraner", "sm-veteraner", "mcr-veteranu"
            ]
        )
        and not any(
            w in title_norm
            for w in [
                "europe", "avrupa", "world", "dunya", "wma", "wmg", "balkan", "bamacs", "bamaci",
                "ema-league", "ema-ligi", "meeting", "part-1", "part-2",
                "bw-", "baden", "norddeutsch", "westfal", "bayern", "hessen", "rheinland", "landes", "bolgesel", "regional"
            ]
        )
    )
    if is_nat_champ:
        if any(w in title_norm for w in ["throw", "atma", "winterwurf", "wurf", "lancer", "lanzamiento", "lanci", "hammer", "marteau", "martillo", "martello", "rzut"]):
            subtype = "THROWS"
        elif any(w in title_norm for w in ["combin", "coklu", "mehrkampf", "pentathlon", "decathlon", "heptathlon", "prove-multiple"]):
            subtype = "COMBINEES"
        elif any(w in title_norm for w in ["marche", "walk", "yuruyus", "gehen", "marcha", "marcia", "chod"]):
            subtype = "MARCHE"
        elif any(w in title_norm for w in ["marathon", "maraton", "half-marathon", "halbmarathon", "10k", "10-km", "5k", "milla", "road-relay", "road", "yol", "strada", "ruta", "estrada"]):
            subtype = "MARATHON"
        elif any(w in title_norm for w in ["cross", "kros", "veldloop", "terrang", "maasto"]):
            subtype = "CROSS"
        elif any(w in title_norm for w in ["mountain", "trail", "dag", "berg", "montagna", "montana"]):
            subtype = "MOUNTAIN"
        elif any(w in title_norm for w in ["indoor", "salon", "salle", "halle", "cubierta", "coberta", "curta", "short-track", "al-coperto", "halowe", "inomhus", "innendors", "indendors", "halli", "v-hale", "fedett"]):
            subtype = "SALON"
        else:
            subtype = "ACIK-SAHA"
        return f"{country_part}-{year}-MASTERLAR-{subtype}"

    # 6. Genel Tekilleştirme Anahtarı: Ülke + Başlangıç Tarihi + Ana Şehir Çekirdeği (Stadyum parantezlerinden bağımsız)
    if city_core:
        return f"{country_part}-{date_part}-{city_core}"
    return f"{country_part}-{date_part}-{title_norm[:20]}"


def generate_competition_id(title: str, start_date: str = "", country: str = "", city: str = "") -> str:
    """
    Her yarışma için deterministik, benzersiz ve kanonik bir ID oluşturur.
    Örnek: 'ro-2026-09-17-bamacs-craiova'
    """
    canonical = get_canonical_key(title, start_date, country, city)
    return normalize_text(canonical)


def deduplicate_competitions(competitions: List[Competition]) -> List[Competition]:
    """
    Taranan yarışmalar listesindeki kopyaları (aynı tarih, aynı şehir veya aynı şampiyona)
    tekilleştirir ve çoklu sorgulardan gelen verileri (tam tarih aralığı, tesis/şehir detayı,
    yetkili ulusal federasyon bilgisi) çakışmasız birleştirir.
    """
    unique_map: dict[str, Competition] = {}

    for comp in competitions:
        if not is_valid_competition_title(comp.title):
            continue

        # Geçersiz şehir yer tutucularını temizle
        if comp.city and comp.city.strip().lower() in {"n/a", "na", "none", "null", "tbd", "tbc", "ybd", "unknown", "belirlenecek", "-"}:
            comp.city = comp.country_name or "Avrupa"

        # Kansai / Japonya normalizasyonu
        c_title_low = comp.title.lower()
        c_city_low = comp.city.lower()
        if "kansai" in c_title_low or "world masters games" in c_title_low or "wmg" in c_title_low or "kansai" in c_city_low:
            comp.country = "JP"
            comp.country_name = "Japonya"
            comp.city = "Kansai"
            comp.start_date = "2027-05-14"
            comp.end_date = "2027-05-23"
            comp.venue_type = "outdoor"
            comp.scope = "world"
            comp.federation = "IMGA"
            comp.website_url = "https://wmg2027.jp"

        key = get_canonical_key(comp)
        if key.startswith("TR-") and "-MASTERLAR-ACIK-SAHA" in key:
            comp.title = "Murat Akman Türkiye Masterlar Açık Saha Atletizm Şampiyonası" if "2026" in key else "Türkiye Masterlar Açık Saha Atletizm Şampiyonası"
            comp.country = "TR"
            comp.country_name = "Türkiye"
            if "2026" in key:
                comp.city = "İzmir"
                comp.start_date = "2026-07-11"
                comp.end_date = "2026-07-12"
            comp.federation = "TAF"
            comp.venue_type = "outdoor"
            comp.scope = "national"
            if not comp.website_url or "taf.org.tr" not in comp.website_url:
                comp.website_url = "https://taf.org.tr"
        elif key.startswith("TR-") and "-MASTERLAR-SALON" in key:
            comp.title = "Alkan Öral Türkiye Masterlar Salon Atletizm Şampiyonası" if "2026" in key else "Türkiye Masterlar Salon Atletizm Şampiyonası"
            comp.country = "TR"
            comp.country_name = "Türkiye"
            if "2026" in key:
                comp.city = "Bursa (Osmangazi Atletizm Salonu)"
                comp.start_date = "2026-01-31"
                comp.end_date = "2026-02-01"
            comp.federation = "TAF"
            comp.venue_type = "indoor"
            comp.scope = "national"
            if not comp.website_url or "taf.org.tr" not in comp.website_url:
                comp.website_url = "https://taf.org.tr"
        elif key.startswith("FR-") and "-EMA-LEAGUE-NANTES" in key:
            comp.title = "Meeting Européen Masters en Salle / EMA League"
            comp.country = "FR"
            comp.country_name = "Fransa"
            comp.city = "Nantes (Stadium Pierre-Quinon)"
            comp.start_date = "2027-02-20"
            comp.end_date = "2027-02-20"
            comp.venue_type = "indoor"
            comp.scope = "ema_league"
            comp.federation = "FFA / EMA"
        elif key.startswith("FR-") and "-MASTERLAR-" in key:
            comp.country = "FR"
            comp.country_name = "Fransa"
            comp.federation = "FFA"
            comp.scope = "national"
            if not comp.website_url:
                comp.website_url = "https://www.athle.fr/contenu/calendrier-championnats-de-france/32"

        if key not in unique_map:
            comp.id = normalize_text(key)
            comp.content_hash = compute_content_hash(comp)
            unique_map[key] = comp
        else:
            existing = unique_map[key]
            existing_is_range = bool(existing.end_date and existing.end_date > existing.start_date)
            incoming_is_range = bool(comp.end_date and comp.end_date > comp.start_date)
            direct_feds = {"TAF", "FFA", "DLV / LADV", "BAMACS", "IMGA"}
            incoming_is_direct = comp.federation in direct_feds and existing.federation not in direct_feds
            incoming_is_nat_fed = comp.federation not in ["", "EMA"] and existing.federation == "EMA"

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

    # İkinci aşama güvenlik filtresi: Aynı ülke + aynı başlangıç tarihi + aynı ana şehir (veya başlık) olan ikiz kayıtları birleştir
    final_list: List[Competition] = []
    seen_signatures: dict[str, int] = {}

    for comp in unique_map.values():
        title_sig = f"{comp.country}|{comp.start_date}|{normalize_text(comp.title)[:30]}"
        c_slug = extract_primary_city_slug(comp.city, comp.country_name)
        city_sig = f"{comp.country}|{comp.start_date}|{c_slug}|{comp.scope}" if c_slug else title_sig

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

    # Eğer başlangıç tarihi aynıysa ve mevcut kayıtta çok günlü bitiş tarihi (end_date > start_date) varken
    # yeni taramada sadece tek gün (end_date == start_date) gelmişse, mevcut çok günlü bitiş tarihini koru!
    ex_start = existing.get("start_date", "")
    ex_end = existing.get("end_date", "")
    if ex_start == incoming.start_date and ex_end > ex_start and incoming.end_date == incoming.start_date:
        incoming.end_date = ex_end
        incoming.content_hash = compute_content_hash(incoming)

    # Eğer ana şehir çekirdeği aynıysa ve mevcut şehir bilgisi stadyum/salon detayı içeriyorsa (daha uzunsa),
    # daha kısa bir şehir metni yüzünden sahte LOCATION_CHANGED üretme!
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

    # Gerçek şehir veya ülke değişikliği kontrolü (sadece ana şehir çekirdeği veya ülke değiştiyse)
    location_changed = (
        existing.get("country") != incoming.country
        or (ex_city_slug != in_city_slug and bool(in_city_slug))
    )

    if date_changed:
        return "DATE_CHANGED", True

    if location_changed:
        return "LOCATION_CHANGED", True

    # Sadece başlık/URL revizyonu ise veritabanını sessizce güncelle, gereksiz bildirim fırtınası yaratma
    return "UPDATED", False
