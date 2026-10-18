import hashlib
import re
import unicodedata
from typing import Tuple, Optional, Any
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


def generate_competition_id(title: str, start_date: str = "", country: str = "", city: str = "") -> str:
    """Yedek olarak basit deterministik slug üretir (Ana ID ataması Gemini AI tarafından yapılır)."""
    country_part = normalize_text(country or "int")[:3]
    date_part = (start_date or "")[:10]
    title_part = normalize_text(title or "")[:35].strip("-")
    return f"{country_part}-{date_part}-{title_part}".strip("-")


def compute_content_hash(comp: Competition) -> str:
    """
    Yarışmanın kritik alanlarının (Başlık, Tarihler, Şehir, Ülke, Kapsam, Durum)
    değişip değişmediğini kontrol etmek için SHA-256 özeti üretir (tesis türü hariçtir).
    """
    payload = f"{comp.title.strip()}|{comp.start_date}|{comp.end_date}|{comp.city.strip()}|{comp.country.strip()}|{comp.scope}|{comp.status}"
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def evaluate_change(existing: Optional[dict], incoming: Competition) -> Tuple[str, bool]:
    """
    Değişiklik analizi yapar.
    Dönüş: (Durum: 'NEW' | 'DATE_CHANGED' | 'LOCATION_CHANGED' | 'UPDATED' | 'UNCHANGED', BildirimTetiklensinMi)
    """
    if not existing:
        return "NEW", True

    # Başlangıç günü aynıysa ve mevcut kayıtta çok günlü bitiş tarihi varken yeni veride tek gün gelmişse mevcut aralığı koru
    ex_start = existing.get("start_date", "")
    ex_end = existing.get("end_date", "")
    if ex_start == incoming.start_date and ex_end > ex_start and incoming.end_date == incoming.start_date:
        incoming.end_date = ex_end
        incoming.content_hash = compute_content_hash(incoming)

    existing_hash = existing.get("content_hash", "")
    new_hash = incoming.content_hash

    if existing_hash == new_hash:
        return "UNCHANGED", False

    date_changed = (
        existing.get("start_date") != incoming.start_date
        or existing.get("end_date") != incoming.end_date
    )

    ex_city = normalize_text(existing.get("city", ""))
    in_city = normalize_text(incoming.city)
    location_changed = (
        existing.get("country") != incoming.country
        or (ex_city != in_city and bool(in_city) and ex_city not in in_city and in_city not in ex_city)
    )

    if date_changed:
        return "DATE_CHANGED", True

    if location_changed:
        return "LOCATION_CHANGED", True

    return "UPDATED", False
