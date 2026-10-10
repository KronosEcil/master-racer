# Master Racer - Masterlar Atletizm Takip Sistemi
## Mimari Tasarım ve Gereksinim Eşleme

### 1. Sistem Mimarisi Şeması
- **Web Kaynakları:** TAF (taf.org.tr), EMA (european-masters-athletics.org), WMA (worldmastersathletics.org), Balkan Masters (BAMACS).
- **Tarama & Ayrıştırma (Scraper):** Python 3.11 tabanlı modüler parser motoru.
- **Otomasyon & Cron:** GitHub Actions (Günde 2 kez otonom çalışma: 06:00 & 18:00 UTC).
- **Veri Tekilleştirme & Değişiklik Tespiti:** SHA-256 tabanlı hashing algoritması.
- **Veritabanı:** Google Firebase Cloud Firestore (NoSQL, Serverless, Free-Tier, Offline Caching).
- **Bildirim Servisi:** Firebase Cloud Messaging (FCM) - Topic-based (`competitions_all`, `country_{code}`).
- **İstemci (Mobil):** Android Native (Kotlin + Jetpack Compose + Material 3 + MVVM).

### 2. Maliyet Analizi (0 TL Güvencesi)
| Servis | Sağlayıcı | Ücretsiz Katman (Free-Tier) Kotası | Projenin Tahmini Tüketimi | Maliyet |
| :--- | :--- | :--- | :--- | :--- |
| **Cron & Execution** | GitHub Actions | 2000 dk/ay (Private repo), Sınırsız (Public repo) | ~60 dk/ay (2 dk/gün) | **0 TL** |
| **Veritabanı** | Google Cloud Firestore | 50.000 okuma/gün, 20.000 yazma/gün, 1 GB alan | ~50 yazma/gün, ~200 okuma/gün | **0 TL** |
| **Push Notification** | Firebase Cloud Messaging | Sınırsız bildirim | Günde 1-5 bildirim | **0 TL** |
| **Admin Yönetimi** | Firebase Console Web UI | Dahili Firestore Studio veri editörü | Sınırsız | **0 TL** |
