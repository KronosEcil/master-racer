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

def is_valid_competition_title(title: str) -> bool:
    """
    Başlığın gerçek bir yarışmaya mı yoksa web sitesi alt sayfasına / dökümanına mı
    (Technical Manual, Registrations, Timetable, Schedule, vb.) ait olduğunu doğrular.
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
            # "championship" veya "trophy" veya "marathon" geçse bile "technical manual", "timetable", "registrations" bir alt dökümandır
            if any(doc_word in t_lower for doc_word in ["technical manual", "timetable", "registrations", "preliminary schedule", "registration", "entry form"]):
                return False
            if t_lower == jp or t_lower.startswith(jp + " ") or t_lower.endswith(" " + jp):
                return False

    # Tek başına "Manual", "Results", "Welcome", "Rules", "Events" gibi genel kelimeleri ele
    single_junk = {"manual", "results", "welcome", "rules", "events", "blog", "home", "contact", "news"}
    if t_lower in single_junk:
        return False

    return True

from typing import Tuple, Optional, List, Any

def get_canonical_key(title_or_comp: Any, start_date: str = "", country: str = "", city: str = "") -> str:
    """
    Farklı kaynaklardan (AWS API, OpenTrack, LADV, Balkan) taranan aynı yarışmaları
    eşleştirmek için standartlaştırılmış tekil anahtar üretir.
    Parametre olarak Competition nesnesi veya doğrudan başlık/tarih/ülke stringleri alabilir.
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
    title_norm = normalize_text(title or "")

    # 1. BAMACS Craiova
    if "craiova" in title_norm or "craiova" in city_norm:
        return "RO-2026-09-17-BAMACS-CRAIOVA"

    # 2. BAMACI Novo Mesto
    if "novo-mesto" in title_norm or "novo-mesto" in city_norm or "bamaci" in title_norm:
        return "SI-2026-03-14-BAMACI-NOVO-MESTO"

    # 3. WMA Marathon Zagreb
    if "zagreb" in title_norm or "zagreb" in city_norm or ("marathon" in title_norm and country_part == "HR"):
        return f"HR-{date_part}-WMA-MARATHON-ZAGREB"

    # 4. BW-Meisterschaften Bernhausen
    if "bernhausen" in city_norm or "bernhausen" in title_norm:
        return f"DE-{date_part}-BW-HALBMARATHON-BERNHAUSEN"

    # 5. EMA Büyük Şampiyonaları
    for em_type in ["emacs", "emaci", "emmtrc", "emmtr", "emamrc", "emacns"]:
        if em_type in title_norm:
            return f"EMA-{date_part[:4]}-{em_type.upper()}"

    # 6. Genel Tekilleştirme Anahtarı: Tarih + Ülke + Şehir (veya ana başlık çekirdeği)
    if city_norm:
        return f"{country_part}-{date_part}-{city_norm}"
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
    tekilleştirir ve en zengin veriye sahip olanı seçer.
    """
    unique_map: dict[str, Competition] = {}

    for comp in competitions:
        if not is_valid_competition_title(comp.title):
            continue

        key = get_canonical_key(comp)
        if key not in unique_map:
            # Kanonik ID ata
            comp.id = normalize_text(key)
            unique_map[key] = comp
        else:
            existing = unique_map[key]
            # Daha zengin metaveriyi koru
            if len(comp.website_url or "") > len(existing.website_url or ""):
                existing.website_url = comp.website_url
            if len(comp.categories) > len(existing.categories):
                existing.categories = comp.categories
            if comp.city and not existing.city:
                existing.city = comp.city
            if comp.regulations_url and not existing.regulations_url:
                existing.regulations_url = comp.regulations_url
            # Kanonik başlığı önceliklendir (Örn: BAMACS 2026 Craiova, WMA Masters Marathon Championships Zagreb)
            if any(kw in comp.title.lower() for kw in ["bamacs 2026", "bamaci 2026", "zagreb", "stadia"]):
                existing.title = comp.title
            unique_map[key] = existing

    return list(unique_map.values())

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

    existing_hash = existing.get("content_hash", "")
    new_hash = incoming.content_hash

    if existing_hash == new_hash:
        return "UNCHANGED", False

    date_changed = (existing.get("start_date") != incoming.start_date or existing.get("end_date") != incoming.end_date)
    
    # Şehir veya ülke değişikliği kontrolü
    existing_loc = f"{existing.get('city', '')} {existing.get('country', '')}".strip()
    incoming_loc = f"{incoming.city} {incoming.country}".strip()
    location_changed = (existing_loc != incoming_loc and (existing.get("city") != incoming.city))

    if date_changed:
        return "DATE_CHANGED", True

    if location_changed:
        return "LOCATION_CHANGED", True

    return "UPDATED", True

