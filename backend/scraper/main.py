import os
import sys
import json
import logging
import argparse
from datetime import datetime
from typing import Dict, List, Optional

import firebase_admin
from firebase_admin import credentials, firestore

from .config import Competition, ScraperConfig
from .deduplication import evaluate_change
from .notification import NotificationManager
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

    # Resmi Federasyon Siteleri + Gemini AI Keşif ve Tekilleştirme Motoru
    ai_parser = AutonomousDiscoveryParser(existing_comps=existing_comps)
    all_scraped: List[Competition] = []

    logger.info(f"[{ai_parser.name}] Taraması başlatılıyor...")
    try:
        all_scraped = ai_parser.parse()
        logger.info(f"[{ai_parser.name}] {len(all_scraped)} doğrulanmış yarışma hazırlandı.")
    except Exception as e:
        logger.error(f"[{ai_parser.name}] Çalışırken hata: {e}")

    stats = {"NEW": 0, "DATE_CHANGED": 0, "LOCATION_CHANGED": 0, "UPDATED": 0, "UNCHANGED": 0}
    scraped_ids = {c.id for c in all_scraped}
    deleted_count = 0

    # 1. TEK SEFERLİK TEMİZ VERİTABANI GEÇİŞİ (clean_reset_done_v1):
    # Eski testlerden kalan hatalı/kopya/elit yarışmaları SADECE BİR KERELİĞİNE temizler.
    # Bu temizlik bir kez tamamlandıktan sonra Firestore'daki bayrak `clean_reset_done_v1 = True` olur
    # ve bundan sonraki hiçbir taramada mevcut yarışmalar (2026, 2027, 2028, 2029) silinmez; kalıcı olarak korunur!
    if db and not dry_run:
        state_ref = db.collection("app_metadata").document("scraper_state")
        clean_reset_done = False
        try:
            state_doc = state_ref.get()
            if state_doc.exists and state_doc.to_dict().get("clean_reset_done_v1") is True:
                clean_reset_done = True
        except Exception as e:
            logger.warning(f"scraper_state okuma uyarısı: {e}")

        if not clean_reset_done and len(scraped_ids) >= 5:
            logger.info("🧹 [TEK SEFERLİK TEMİZ VERİTABANI GEÇİŞİ] Eski testlerden kalan hatalı/kopya kayıtlar temizleniyor...")
            for doc_id, doc_data in existing_comps.items():
                if doc_id not in scraped_ids:
                    title = str(doc_data.get("title", "")).strip()
                    try:
                        db.collection(ScraperConfig.FIRESTORE_COLLECTION).document(doc_id).delete()
                        logger.info(f"🗑️ [ESKİ/HATALI KAYIT TEMİZLENDİ] {title} (ID: {doc_id})")
                        deleted_count += 1
                    except Exception as e:
                        logger.error(f"Firestore silme hatası ({doc_id}): {e}")
            try:
                state_ref.set(
                    {
                        "clean_reset_done_v1": True,
                        "reset_completed_at": datetime.utcnow().isoformat(),
                        "cleaned_records_count": deleted_count,
                    },
                    merge=True,
                )
                logger.info(
                    f"✅ [TEK SEFERLİK TEMİZLİK TAMAMLANDI] {deleted_count} eski kayıt silindi. "
                    "Bundan sonraki taramalarda hiçbir yarışma silinmeyecek, kalıcı olarak korunacaktır."
                )
            except Exception as e:
                logger.error(f"scraper_state yazma hatası: {e}")

    # Toplu veritabanı yenilemesinde (10'dan fazla yeni kayıt eklendiğinde) bildirim yağmuru oluşmasını engelle
    new_candidates = sum(1 for c in all_scraped if c.id not in existing_comps)
    allow_push_notifications = new_candidates <= 10

    # 2. DOĞRULANMIŞ RESMİ YARIŞMALARI VERİTABANINA YAZ
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
            old_end = existing.get("end_date") if existing else None
            comp.is_new = existing.get("is_new", False) if existing else False
            comp.location_changed = existing.get("location_changed", False) if existing else False
            logger.warning(f"⚠️ [TARİH GÜNCELLENDİ] {comp.title} -> Yeni: {comp.start_date}..{comp.end_date} (Eski: {comp.old_start_date}..{old_end})")
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

        # FCM Bildirimi Gönder (Toplu sıfırlama/kurulum haricinde)
        if should_notify and allow_push_notifications:
            notification_mgr.send_competition_notification(comp, change_type)

    logger.info("=== Tarama Özeti ===")
    logger.info(f"Yeni Eklenen     : {stats['NEW']}")
    logger.info(f"Tarihi Değişen   : {stats['DATE_CHANGED']}")
    logger.info(f"Detayı Değişen   : {stats['UPDATED']}")
    logger.info(f"Değişmeyen       : {stats['UNCHANGED']}")
    logger.info(f"Temizlenen Eski  : {deleted_count}")
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
