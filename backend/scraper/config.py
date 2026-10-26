import os
from typing import List, Optional
from pydantic import BaseModel, Field

class Competition(BaseModel):
    """
    Masterlar Atletizm Yarışması Veri Modeli
    """
    id: str = Field(default="", description="Deterministik benzersiz kimlik (slug)")
    title: str = Field(default="", description="Yarışmanın resmi adı")
    country: str = Field(default="INT", description="Ülke kodu (TR, GR, SWE vb.)")
    country_name: str = Field(default="Uluslararası", description="Ülke adı (Türkiye, Yunanistan vb.)")
    city: str = Field(default="", description="Yarışmanın yapılacağı şehir")
    start_date: str = Field(default="", description="Başlangıç tarihi (YYYY-MM-DD)")
    end_date: str = Field(default="", description="Bitiş tarihi (YYYY-MM-DD)")
    venue_type: str = Field(default="outdoor", description="Tesis türü: outdoor, indoor, road, cross")
    categories: List[str] = Field(default_factory=list, description="Kapsanan branşlar (Sprint, Atlamalar vb.)")
    scope: str = Field(default="regional", description="Kapsam: turkey, balkan, europe, world, regional")
    federation: str = Field(default="", description="Düzenleyen federasyon/birlik (TAF, EMA, WMA, BAMACS)")
    regulations_url: Optional[str] = Field(default=None, description="Reglaman / Şartname PDF bağlantısı")
    website_url: Optional[str] = Field(default=None, description="Resmi etkinlik web sitesi")
    min_age: int = Field(default=35, description="Minimum yarışma yaşı (Standart: 35+)")
    age_category: str = Field(default="35+", description="Yaş kategorisi: 35+ (M35/W35+)")
    trust_level: int = Field(default=2, description="Kaynak güven seviyesi: 3=Resmi API/PDF/OpenTrack, 2=Resmi Federasyon Sayfası, 1=Web Arama")
    content_hash: str = Field(default="", description="Veri değişiklik takibi için SHA-256 özeti")
    status: str = Field(default="scheduled", description="Durum: scheduled, postponed, cancelled, completed")
    is_new: bool = Field(default=False, description="Yarışmanın sisteme yeni eklendiğini belirtir")
    date_changed: bool = Field(default=False, description="Yarışmanın tarihinin değiştiğini belirtir")
    location_changed: bool = Field(default=False, description="Yarışmanın yerinin/şehrinin değiştiğini belirtir")
    old_start_date: Optional[str] = Field(default=None, description="Tarih değiştiyse eski başlangıç tarihi")
    old_location: Optional[str] = Field(default=None, description="Konum değiştiyse eski konum/şehir bilgisi")
    created_at: str = Field(default="", description="Kayıt oluşturulma zamanı (ISO 8601)")
    updated_at: str = Field(default="", description="Son güncelleme zamanı (ISO 8601)")

class ScraperConfig:
    FIRESTORE_COLLECTION = os.getenv("FIRESTORE_COLLECTION", "competitions")
    FIREBASE_CREDENTIALS_JSON = os.getenv("FIREBASE_CREDENTIALS_JSON", "")
    FIREBASE_CREDENTIALS_PATH = os.getenv("FIREBASE_CREDENTIALS_PATH", "serviceAccountKey.json")
    GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
    FCM_TOPIC_ALL = "competitions_all"
    FCM_TOPIC_PREFIX_COUNTRY = "country_"
    USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 MasterRacerBot/1.0"
