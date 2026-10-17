import os
import re
import sys
import json
import logging
import argparse
from datetime import datetime
from typing import Dict, List, Optional

import firebase_admin
from firebase_admin import credentials, firestore

from .config import Competition, ScraperConfig
from .deduplication import evaluate_change, deduplicate_competitions, is_valid_competition_title, get_canonical_key
from .notification import NotificationManager
from .parsers.taf_parser import TAFParser
from .parsers.ema_parser import EMAParser
from .parsers.wma_balkan_parser import WMABalkanParser
from .parsers.national_parser import NationalFederationsParser
from .parsers.autonomous_discovery import AutonomousDiscoveryParser

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger("MasterRacerRunner")

def init_firebase() -> Optional[firestore.firestore.Client]:
    """
    Firebase Admin SDK'yı başlatır.
    Ortam değişkeninden (GitHub Secrets) veya yerel serviceAccountKey.json dosyasından okur.
    """
    if firebase_admin._apps:
        return firestore.client()

    cred_json_str = ScraperConfig.FIREBASE_CREDENTIALS_JSON
    cred_file_path = ScraperConfig.FIREBASE_CREDENTIALS_PATH

    cred = None
    if cred_json_str and cred_json_str.strip():
        try:
            logger.info("Firebase kimlik bilgileri ortam değişkeninden okunuyor...")
            cred_dict = json.loads(cred_json_str)
            cred = credentials.Certificate(cred_dict)
        except Exception as e:
            logger.error(f"FIREBASE_CREDENTIALS_JSON parse hatası: {e}")

    if not cred and os.path.exists(cred_file_path):
        try:
            logger.info(f"Firebase kimlik bilgileri yerel dosyadan okunuyor: {cred_file_path}")
            cred = credentials.Certificate(cred_file_path)
        except Exception as e:
            logger.error(f"Yerel credential dosyası yükleme hatası: {e}")

    if cred:
        try:
            firebase_admin.initialize_app(cred)
            logger.info("Firebase Admin SDK başarıyla ilklendirildi.")
            return firestore.client()
        except Exception as e:
            logger.error(f"Firebase initialize_app hatası: {e}")
            return None
    else:
        logger.warning("Firebase kimlik bilgisi bulunamadı! [DRY-RUN / SİMÜLASYON MODUNDA ÇALIŞIYOR]")
        return None

def fetch_existing_competitions(db: Optional[firestore.firestore.Client]) -> Dict[str, dict]:
    """Firestore'dan mevcut tüm yarışma kayıtlarını ID indeksli olarak çeker."""
    existing_map = {}
    if not db:
        return existing_map

    try:
        col_ref = db.collection(ScraperConfig.FIRESTORE_COLLECTION)
        docs = col_ref.stream()
        for doc in docs:
            data = doc.to_dict()
            existing_map[doc.id] = data
        logger.info(f"Firestore'dan {len(existing_map)} adet mevcut yarışma kaydı çekildi.")
    except Exception as e:
        logger.error(f"Firestore okuma hatası: {e}")
    return existing_map

def run_scraper(dry_run: bool = False, export_path: Optional[str] = None):
    logger.info("=== Master Racer Otomasyon & Tarama Motoru Başlatılıyor ===")
    
    db = init_firebase() if not dry_run else None
    notification_mgr = NotificationManager(is_initialized=(db is not None and not dry_run))

    existing_comps = fetch_existing_competitions(db)

    # Sadece tamamen canlı ve otonom keşif motoru (AutonomousDiscoveryParser) çalışır.
    # Statik varsayımlar veya yapay tarihler üreten eski ayrıştırıcılar devre dışıdır.
    parsers = [
        AutonomousDiscoveryParser()
    ]

    all_scraped: List[Competition] = []
    for parser in parsers:
        logger.info(f"[{parser.name}] Taraması başlatılıyor...")
        try:
            comps = parser.parse()
            logger.info(f"[{parser.name}] {len(comps)} yarışma bulundu.")
            all_scraped.extend(comps)
        except Exception as e:
            logger.error(f"[{parser.name}] Çalışırken hata: {e}")

    logger.info(f"Toplam ham taranan yarışma sayısı: {len(all_scraped)}")

    # Tekilleştirme motorunu çalıştır (Duplicate ve döküman başlıklarını eler)
    all_scraped = deduplicate_competitions(all_scraped)
    logger.info(f"Tekilleştirme sonrası aktif yarışma sayısı: {len(all_scraped)}")

    # Tekilleştirme, Değişiklik Tespiti ve Veritabanı Güncelleme
    stats = {"NEW": 0, "DATE_CHANGED": 0, "LOCATION_CHANGED": 0, "UPDATED": 0, "UNCHANGED": 0}

    for comp in all_scraped:
        existing = existing_comps.get(comp.id)
        change_type, should_notify = evaluate_change(existing, comp)
        stats[change_type] += 1

        if change_type == "UNCHANGED":
            continue

        comp.updated_at = datetime.utcnow().isoformat()
        if change_type == "NEW":
            comp.is_new = True
            comp.date_changed = False
            comp.location_changed = False
            logger.info(f"✨ [YENİ YARIŞMA] {comp.title} ({comp.start_date}) - ID: {comp.id}")
        elif change_type == "DATE_CHANGED":
            comp.date_changed = True
            comp.old_start_date = existing.get("start_date") if existing else None
            comp.is_new = existing.get("is_new", False) if existing else False
            comp.location_changed = existing.get("location_changed", False) if existing else False
            logger.warning(f"⚠️ [TARİH GÜNCELLENDİ] {comp.title} -> Yeni Tarih: {comp.start_date} (Eski: {comp.old_start_date})")
        elif change_type == "LOCATION_CHANGED":
            comp.location_changed = True
            comp.old_location = f"{existing.get('city', '')}, {existing.get('country_name', '')}".strip(", ") if existing else None
            comp.is_new = existing.get("is_new", False) if existing else False
            comp.date_changed = existing.get("date_changed", False) if existing else False
            logger.warning(f"📍 [KONUM GÜNCELLENDİ] {comp.title} -> Yeni: {comp.city}, {comp.country_name} (Eski: {comp.old_location})")
        else:
            comp.is_new = existing.get("is_new", False) if existing else False
            comp.date_changed = existing.get("date_changed", False) if existing else False
            comp.location_changed = existing.get("location_changed", False) if existing else False
            logger.info(f"🔄 [GÜNCELLENDİ] {comp.title}")

        # Firestore'a Yaz
        if db and not dry_run:
            try:
                comp_data = comp.model_dump()
                db.collection(ScraperConfig.FIRESTORE_COLLECTION).document(comp.id).set(comp_data, merge=True)
            except Exception as e:
                logger.error(f"Firestore yazma hatası ({comp.id}): {e}")

        # FCM Bildirimi Gönder
        if should_notify:
            notification_mgr.send_competition_notification(comp, change_type)

    # Firestore Eski / Hatalı / Kopya Kayıt Temizliği (Garbage Collection)
    scraped_ids = {c.id for c in all_scraped}
    scraped_canonical_keys = {get_canonical_key(c) for c in all_scraped}
    deleted_count = 0

    if db and not dry_run:
        for doc_id, doc_data in existing_comps.items():
            title = doc_data.get("title", "")
            s_date = str(doc_data.get("start_date", ""))
            c_name = str(doc_data.get("country_name", ""))
            country = str(doc_data.get("country", ""))
            city = str(doc_data.get("city", ""))

            # 1. Başlık geçerlilik kontrolü (Technical Manual, Timetable, Registrations, vb.)
            is_invalid_title = not is_valid_competition_title(title)
            
            # 2. Geçersiz veya sahte tarihler (2006 veya 2026'dan eski)
            is_bad_date = s_date.startswith("2006") or (len(s_date) >= 4 and s_date[:4] < "2026")
            
            # 3. Craiova / BAMACS 2026 yarışmasının 2027 tarihli hatalı kopyası
            is_wrong_bamacs_year = ("craiova" in title.lower() or "bamacs" in title.lower()) and s_date.startswith("2027")

            # 4. Hatalı ülke/şehir kayıtları (Balkan ülkesi, Selanik'te BAMACS, Uluslararası Kansai, Albania vb.)
            is_wrong_country = (
                (country == "GR" and "bamacs" in title.lower()) or 
                (country == "Balkan" or c_name == "Balkan" or city == "Balkan") or
                (("kansai" in doc_id.lower() or "kansai" in title.lower() or "wmg" in doc_id.lower() or "world-masters-games" in doc_id.lower()) and (country != "JP" or "uluslararası" in c_name.lower())) or
                (c_name.lower() == "albania" or (country.upper() == "ALB" and c_name != "Arnavutluk"))
            )

            # 5. Sahte mock fixture artıkları
            is_mock_artifact = any(kw in doc_id.lower() or kw in title.lower() for kw in [
                "sonbahar", "bursa-kros", "naili moran", "mock", "sample", "demo",
                "registrations", "timetable", "schedule", "manual"
            ])

            # 6. Yeni taranan kanonik yarışmalarla çakışan eski kopya ID'ler
            old_canonical_key = get_canonical_key(title, s_date, country, city)
            is_duplicate_of_new = (doc_id not in scraped_ids and old_canonical_key in scraped_canonical_keys)

            # 7. Başlığında açıkça yıl belirtilip tarihi farklı olan (Örn: EMACNS28 -> 2028 ama tarih 2027) tutarsız eski kayıtlar
            explicit_year_str = ""
            four_d = re.search(r"202[5-9]", title)
            two_d = re.search(r"\b[A-Za-z]+(2[5-9])\b", title)
            if four_d:
                explicit_year_str = four_d.group(0)
            elif two_d:
                explicit_year_str = f"20{two_d.group(1)}"
            is_year_mismatch = bool(explicit_year_str and len(s_date) >= 4 and explicit_year_str != s_date[:4])

            # 8. Büyük EMA şampiyonalarında kesin takvim yılı kontrolleri
            t_low = title.lower()
            if ("lucerne" in t_low or "emamrc" in t_low) and not s_date.startswith("2027"):
                is_year_mismatch = True
            if ("galway" in t_low or "emacns" in t_low) and not s_date.startswith("2028"):
                is_year_mismatch = True
            if ("jyvaskyla" in t_low or "jyväskylä" in t_low or "emacs" in t_low) and not s_date.startswith("2027"):
                is_year_mismatch = True
            if ("ojos" in t_low or "ojós" in t_low or "emmtr" in t_low) and not s_date.startswith("2027"):
                is_year_mismatch = True

            # 9. Bozuk karakterli (\ufffd) veya artık canlı FFA takviminde yer almayan eski statik Fransa kayıtları
            has_corrupt_encoding = ("\ufffd" in title or "\ufffd" in city)
            is_stale_france = (country == "FR" and doc_id not in scraped_ids)

            should_delete = (
                is_invalid_title or is_bad_date or is_wrong_bamacs_year or
                is_wrong_country or is_mock_artifact or is_duplicate_of_new or
                is_year_mismatch or has_corrupt_encoding or is_stale_france
            )

            if should_delete and doc_id not in scraped_ids:
                try:
                    db.collection(ScraperConfig.FIRESTORE_COLLECTION).document(doc_id).delete()
                    logger.info(f"🗑️ [ESKİ/HATALI/KOPYA VERİ SİLİNDİ] {title} (ID: {doc_id})")
                    deleted_count += 1
                except Exception as e:
                    logger.error(f"Firestore silme hatası ({doc_id}): {e}")

    logger.info("=== Tarama Özeti ===")
    logger.info(f"Yeni Eklenen     : {stats['NEW']}")
    logger.info(f"Tarihi Değişen   : {stats['DATE_CHANGED']}")
    logger.info(f"Detayı Değişen   : {stats['UPDATED']}")
    logger.info(f"Değişmeyen       : {stats['UNCHANGED']}")
    logger.info(f"Silinen Eski Kayıt: {deleted_count}")
    logger.info(f"Toplam Aktif     : {len(all_scraped)}")

    if export_path:
        with open(export_path, "w", encoding="utf-8") as f:
            json.dump([c.model_dump() for c in all_scraped], f, ensure_ascii=False, indent=2)
        logger.info(f"Sonuçlar JSON olarak dışa aktarıldı: {export_path}")

    return stats

def main():
    parser = argparse.ArgumentParser(description="Master Racer Otomasyon & Tarama Motoru")
    parser.add_argument("--dry-run", action="store_true", help="Veritabanına yazmadan ve bildirim atmadan simüle et")
    parser.add_argument("--export", type=str, default=None, help="Taranan verileri JSON dosyasına kaydet")
    args = parser.parse_args()

    try:
        run_scraper(dry_run=args.dry_run, export_path=args.export)
    except Exception as e:
        logger.exception(f"Tarama motoru beklenmeyen bir hata ile karşılaştı: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
