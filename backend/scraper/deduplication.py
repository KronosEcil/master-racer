import hashlib
import re
import unicodedata
from typing import Tuple, Optional
from .config import Competition

def normalize_text(text: str) -> str:
    """Metni küçük harfe çevirir, Türkçe karakterleri ve aksanları temizler, alfanümerik slug üretir."""
    if not text:
        return ""
    text = text.replace("İ", "I").replace("ı", "i")
    text = unicodedata.normalize("NFKD", text).encode("ASCII", "ignore").decode("utf-8")
    text = re.sub(r"[^a-zA-Z0-9\s-]", "", text)
    text = re.sub(r"\s+", "-", text.strip().lower())
    return text

def generate_competition_id(title: str, start_date: str, country: str) -> str:
    """
    Her yarışma için deterministik, benzersiz bir ID oluşturur.
    Örnek: 'tur-2025-07-05-turkiye-masterlar-atletizm-sampiyonasi'
    """
    country_slug = normalize_text(country)[:3] or "gen"
    title_slug = normalize_text(title)[:40]
    return f"{country_slug}-{start_date}-{title_slug}".strip("-")

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
