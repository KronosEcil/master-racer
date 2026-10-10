# Master Racer - Backend & Scraper Motoru

Bu modül, Masterlar Atletizm yarışmalarını otonom olarak tarar, veritabanına kaydeder ve yeni yarışmaları kullanıcılara FCM üzerinden bildirir.

## Özellikler
- **Web Scraping:** TAF, EMA, WMA ve Balkan Masters takvimlerini ayrıştırır.
- **Deduplication & Hashing:** SHA-256 tabanlı tekilleştirme ile sadece yeni veya tarihi değişmiş yarışmaları günceller.
- **Firebase Firestore Entegrasyonu:** Gerçek zamanlı NoSQL veritabanı.
- **FCM Bildirimleri:** `competitions_all` konusuna (topic) anlık bildirim iletir.
- **Sıfır Maliyet (0 TL):** GitHub Actions üzerinde günde 2 kez (06:00 & 18:00 UTC) ücretsiz çalışır.

## Yerel Çalıştırma
```bash
cd backend
pip install -r requirements.txt

# Simülasyon / Test modu (Firebase'e yazmadan)
python -m backend.scraper.main --dry-run --export taranan_yarisamalar.json

# Gerçek mod (serviceAccountKey.json mevcutken)
python -m backend.scraper.main
```
