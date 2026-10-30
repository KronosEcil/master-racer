import os
import sys
import json
import logging
import argparse
from datetime import datetime, timezone
from typing import Dict, List, Optional

import firebase_admin
from firebase_admin import credentials, firestore

from .config import Competition, ScraperConfig
from .deduplication import evaluate_change, generate_competition_id
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
    clean_reset_done = False
    badges_baseline_reset = False
    obsolete_fields_cleaned = False
    state_ref = None
    if db and not dry_run:
        state_ref = db.collection("app_metadata").document("scraper_state")
        try:
            state_doc = state_ref.get()
            if state_doc.exists:
                s_dict = state_doc.to_dict() or {}
                if s_dict.get("clean_reset_done_v1") is True:
                    clean_reset_done = True
                if s_dict.get("badges_baseline_reset_v4") is True:
                    badges_baseline_reset = True
                if s_dict.get("obsolete_fields_cleaned_v1") is True:
                    obsolete_fields_cleaned = True
        except Exception as e:
            logger.warning(f"scraper_state okuma uyarısı: {e}")

        if not clean_reset_done:
            logger.info("🧹 [TEK SEFERLİK TEMİZ VERİTABANI GEÇİŞİ] Eski testlerden kalan hatalı/kopya kayıtlar temizleniyor...")
            for doc_id, doc_data in list(existing_comps.items()):
                if doc_id not in scraped_ids:
                    title = str(doc_data.get("title", "")).strip()
                    try:
                        db.collection(ScraperConfig.FIRESTORE_COLLECTION).document(doc_id).delete()
                        existing_comps.pop(doc_id, None)
                        logger.info(f"🗑️ [ESKİ/HATALI KAYIT TEMİZLENDİ] {title} (ID: {doc_id})")
                        deleted_count += 1
                    except Exception as e:
                        logger.error(f"Firestore silme hatası ({doc_id}): {e}")
            try:
                state_ref.set(
                    {
                        "clean_reset_done_v1": True,
                        "reset_completed_at": datetime.now(timezone.utc).isoformat(),
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
        else:
            # Mevcut veritabanındaki tüm geçerli yarışmalar kalıcı olarak korunur;
            # Yalnızca kanonik ID kuralı güncellenmiş veya aynı gün başka bir spesifik branş kaydının
            # "-acik-saha" kopyası olan mükerrer ID'ler temizlenir.
            all_dates_by_prefix = {}
            for doc_id, doc_data in existing_comps.items():
                if not doc_id.endswith("-acik-saha"):
                    parts = doc_id.split("-")
                    if len(parts) >= 2:
                        prefix = f"{parts[0]}-{parts[1]}-"
                        all_dates_by_prefix.setdefault(prefix, set()).add(str(doc_data.get("start_date", ""))[:10])
            for c in all_scraped:
                if not c.id.endswith("-acik-saha"):
                    parts = c.id.split("-")
                    if len(parts) >= 2:
                        prefix = f"{parts[0]}-{parts[1]}-"
                        all_dates_by_prefix.setdefault(prefix, set()).add(c.start_date[:10])

            for doc_id, doc_data in list(existing_comps.items()):
                title = str(doc_data.get("title", "")).strip()
                s_date = str(doc_data.get("start_date", "")).strip()[:10]
                country = str(doc_data.get("country", "")).strip()
                city = str(doc_data.get("city", "")).strip()
                canon_id = generate_competition_id(title, s_date, country, city)

                is_same_date_generic_dup = False
                if doc_id.endswith("-acik-saha"):
                    prefix = doc_id[: -len("acik-saha")]
                    if s_date and s_date in all_dates_by_prefix.get(prefix, set()):
                        is_same_date_generic_dup = True

                if canon_id != doc_id or is_same_date_generic_dup:
                    try:
                        # Geçmiş/arşiv yarışmalarının kaybolmaması için: eğer yeni kanonik ID taramada veya
                        # veritabanında yoksa, eski kaydı silmeden önce yeni kanonik ID'ye taşı.
                        if not is_same_date_generic_dup and canon_id not in scraped_ids and canon_id not in existing_comps:
                            migrated_data = dict(doc_data)
                            migrated_data["id"] = canon_id
                            db.collection(ScraperConfig.FIRESTORE_COLLECTION).document(canon_id).set(migrated_data, merge=True)
                            existing_comps[canon_id] = migrated_data
                            logger.info(f"📦 [ARŞİV KAYDI TAŞINDI] {title} ({doc_id} -> {canon_id})")
                        db.collection(ScraperConfig.FIRESTORE_COLLECTION).document(doc_id).delete()
                        existing_comps.pop(doc_id, None)
                        logger.info(f"🗑️ [MÜKERRER/ESKİ ID TEMİZLENDİ] {title} (Eski ID: {doc_id} -> Yeni: {canon_id})")
                        deleted_count += 1
                    except Exception as e:
                        logger.error(f"Firestore mükerrer silme hatası ({doc_id}): {e}")

    # Toplu veritabanı yenilemesinde veya ilk temel rozet sıfırlamasında bildirim gönderme
    new_candidates = sum(1 for c in all_scraped if c.id not in existing_comps and c.status != "completed")
    allow_push_notifications = (new_candidates <= 10) and badges_baseline_reset

    # 2. DOĞRULANMIŞ RESMİ YARIŞMALARI VERİTABANINA YAZ
    for comp in all_scraped:
        existing = existing_comps.get(comp.id)
        change_type, should_notify = evaluate_change(existing, comp)
        stats[change_type] += 1

        if change_type == "UNCHANGED":
            continue

        is_past_archive = comp.status == "completed"
        comp.updated_at = datetime.now(timezone.utc).isoformat()
        if not badges_baseline_reset or is_past_archive:
            # Temel referans (baseline) kurulumunda veya geçmiş arşiv yarışmalarında Yeni / Güncellendi etiketi gösterme
            comp.is_new = False
            comp.date_changed = False
            comp.location_changed = False
            comp.old_start_date = None
            comp.old_location = None
            should_notify = False
            logger.info(f"🔄 [{'ARŞİV EKLENDİ' if is_past_archive else 'TEMEL KAYIT GÜNCELLENDİ'}] {comp.title} ({comp.start_date}) - {comp.city}, {comp.country_name}")
        elif change_type == "NEW":
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

    # 3. TEK SEFERLİK ROZET SIFIRLAMA (badges_baseline_reset_v1):
    # Şu an veritabanında var olan tüm yarışmaların "Yeni", "Tarih Güncellendi", "Konum Güncellendi"
    # etiketlerini temizler; bundan sonraki taramalarda yalnızca gerçekten yeni eklenen veya değişenlerde görünür.
    if db and not dry_run and not badges_baseline_reset:
        logger.info("🧹 [ROZET SIFIRLAMA] Mevcut tüm yarışmaların 'Yeni / Konum Güncellendi / Tarih Güncellendi' etiketleri sıfırlanıyor...")
        reset_badge_count = 0
        try:
            col_ref = db.collection(ScraperConfig.FIRESTORE_COLLECTION)
            for doc in col_ref.stream():
                d = doc.to_dict() or {}
                updates = {}
                # World Masters Mountain 2027 konum düzeltmesi (Ojós -> Klosters, İsviçre)
                if doc.id == "world-2027-mountain-trail" and "oj" in str(d.get("city", "")).lower():
                    updates["city"] = "Klosters"
                    updates["country"] = "CH"
                    updates["country_name"] = "İsviçre"
                if (
                    d.get("is_new")
                    or d.get("date_changed")
                    or d.get("location_changed")
                    or d.get("old_start_date")
                    or d.get("old_location")
                ):
                    updates.update(
                        {
                            "is_new": False,
                            "date_changed": False,
                            "location_changed": False,
                            "old_start_date": None,
                            "old_location": None,
                        }
                    )
                if updates:
                    col_ref.document(doc.id).update(updates)
                    reset_badge_count += 1
            if state_ref:
                state_ref.set(
                    {
                        "badges_baseline_reset_v4": True,
                        "badges_reset_at": datetime.now(timezone.utc).isoformat(),
                    },
                    merge=True,
                )
            logger.info(
                f"✅ [ROZET SIFIRLAMA TAMAMLANDI] {reset_badge_count} yarışmanın bildirim etiketi temizlendi. "
                "Bundan sonra yalnızca yeni eklenen veya değişen yarışmalarda etiket görünecektir."
            )
        except Exception as e:
            logger.error(f"Rozet sıfırlama hatası: {e}")

    # 4. KULLANILMAYAN ESKİ ALANLARI (categories, venue_type, min_age, age_category) FIRESTORE'DAN SİL
    if db and not dry_run and not obsolete_fields_cleaned:
        logger.info("🧹 [ALAN TEMİZLİĞİ] Kullanılmayan 'categories', 'venue_type', 'min_age', 'age_category' alanları Firestore'dan siliniyor...")
        cleaned_docs = 0
        obsolete_keys = ("categories", "venue_type", "min_age", "age_category")
        try:
            col_ref = db.collection(ScraperConfig.FIRESTORE_COLLECTION)
            for doc in col_ref.stream():
                d = doc.to_dict() or {}
                del_updates = {k: firestore.DELETE_FIELD for k in obsolete_keys if k in d}
                if del_updates:
                    col_ref.document(doc.id).update(del_updates)
                    cleaned_docs += 1
            if state_ref:
                state_ref.set({"obsolete_fields_cleaned_v1": True}, merge=True)
            logger.info(f"✅ [ALAN TEMİZLİĞİ TAMAMLANDI] {cleaned_docs} kayıttan gereksiz alanlar silindi.")
        except Exception as e:
            logger.error(f"Eski alan temizleme hatası: {e}")

    # 5. GEÇMİŞ YARIŞMALAR ARŞİVİ KORUMASI & TARİHİ GEÇENLERİ 'completed' OLARAK İŞARETLEME
    if db and not dry_run:
        today_iso = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        archived_updates = 0
        try:
            col_ref = db.collection(ScraperConfig.FIRESTORE_COLLECTION)
            for doc in col_ref.stream():
                d = doc.to_dict() or {}
                e_date = str(d.get("end_date") or d.get("start_date") or "")[:10]
                t_str = str(d.get("title", "")).strip()
                t_low = t_str.lower()
                c_str = str(d.get("city", "")).strip()
                country_str = str(d.get("country", "")).strip().upper()
                doc_updates = {}

                # Tarihi geçmiş yarışmaları silmeden kalıcı arşiv ('completed') statüsüne geçir
                if e_date and e_date < today_iso and d.get("status") != "completed":
                    doc_updates["status"] = "completed"
                    doc_updates["is_new"] = False

                # Başlığında "kansai" geçmeyen bir Masters Games kaydında eski hatadan Kansai/JP kalmışsa başlıktan gerçek ülkesini/şehrini bul
                if ("masters games" in t_low or "wmg" in t_low) and "kansai" not in t_low and ("kansai" in c_str.lower() or country_str == "JP"):
                    det_c, det_cname, _ = ai_parser.detect_country(t_str, "INT", "Uluslararası")
                    if det_c not in ("INT", "JP"):
                        doc_updates["country"] = det_c
                        doc_updates["country_name"] = det_cname
                        # Başlıktaki şehir adını çıkar (örn. "World Masters Games Perth 2029" -> "Perth")
                        cleaned_title = t_str
                        for rm in ("Winter World Masters Games", "World Masters Games", "Open Masters Games", "European Masters Games", "Lake"):
                            cleaned_title = cleaned_title.replace(rm, "").replace(rm.lower(), "")
                        city_tokens = [tok for tok in cleaned_title.split() if not tok.isdigit() and len(tok) >= 3]
                        doc_updates["city"] = " ".join(city_tokens).strip() or det_cname
                        doc_updates["location_changed"] = False
                        doc_updates["old_location"] = None

                if doc_updates:
                    col_ref.document(doc.id).update(doc_updates)
                    archived_updates += 1
            if archived_updates:
                logger.info(f"📚 [ARŞİV & KONUM GÜNCELLEMESİ] {archived_updates} yarışma kaydı arşiv/konum açısından güncellendi.")
        except Exception as e:
            logger.warning(f"Arşiv statü güncelleme uyarısı: {e}")

    total_in_db = len(set(existing_comps.keys()) | scraped_ids)
    logger.info("=== Tarama Özeti ===")
    logger.info(f"Yeni Eklenen     : {stats['NEW']}")
    logger.info(f"Tarihi Değişen   : {stats['DATE_CHANGED']}")
    logger.info(f"Detayı Değişen   : {stats['UPDATED']}")
    logger.info(f"Değişmeyen       : {stats['UNCHANGED']}")
    logger.info(f"Temizlenen Eski  : {deleted_count}")
    logger.info(f"Toplam Aktif     : {total_in_db}")

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
