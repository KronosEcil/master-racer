# Master Racer - Kurulum ve Devreye Alma Rehberi

Bu rehber, **Masterlar Atletizm Takip Sistemi**'nin sıfır maliyetle (0 TL) Firebase, GitHub Actions ve Android üzerinde baştan sona nasıl kurulacağını ve çalıştırılacağını açıklar.

---

## 1. Firebase Kurulumu (0 TL - Spark Katmanı)

1. [Firebase Console](https://console.firebase.google.com/)'a gidin ve **"Yeni Proje Ekle"** butonuna tıklayın (Örn: `master-racer`).
2. **Cloud Firestore Veritabanı Oluşturma:**
   - Sol menüden **Build > Firestore Database** seçeneğine tıklayın.
   - **Create Database** deyin. Konum olarak `eur3 (europe-west)` veya size en yakın bölgeyi seçin.
   - Güvenlik kuralları (**Rules**) sekmesine gidin, mevcut içeriği silin ve aşağıdaki doğru kuralı yapıştırıp **Publish** butonuna basın:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       // Herkes yarışmaları okuyabilir, sadece servis anahtarı yazabilir
       match /competitions/{competitionId} {
         allow read: if true;
         allow write: if false; // Scraper Firebase Admin SDK ile doğrudan yazar
       }
     }
   }
   ```
   *(Not: `service cloud.firestore` ifadesindeki nokta ve boşluk önemlidir, alt çizgi `service_firestore` kullanılmaz).*

3. **Admin SDK Servis Hesabı Anahtarını Alma (Scraper için):**
   - Sol üstteki **Proje Ayarları** (Dişli çark simgesi) > **Service accounts (Hizmet hesapları)** sekmesine gidin.
   - Dil seçeneği olarak Python seçili kalsın, **"Generate new private key" (Yeni özel anahtar oluştur)** butonuna basın.
   - İndirilen `.json` dosyasını saklayın (bu dosya 2. adımda GitHub Secrets'a eklenecek).

4. **Android Uygulamasını Projeye Ekleme:**
   - Proje Ayarları > **Genel** sekmesine gelin ve sayfanın altındaki **Android simgesine** tıklayın.
   - **Android paket adı:** `com.ecil.masterracer` yazın.
   - **Uygulamayı kaydet** deyin ve ardından verilen **`google-services.json`** dosyasını indirin.
   - İndirdiğiniz bu dosyayı bilgisayarınızdaki `c:\Master Racer\android\app\` klasörünün içine kopyalayın (mevcut `google-services.json.example` dosyasının olduğu yere).

---

## 2. GitHub Actions Otomasyonunun Kurulumu (0 TL Cron) — Adım Adım Detaylı Anlatım

Bu adım, sunucu kiralamadan günde 2 kez yarışma sitelerini otomatik tarayıp telefonunuza bildirim gönderen ücretsiz cron motorunu devreye alır.

### Adım 2.1: GitHub'da Yeni Bir Repository Oluşturma
1. [GitHub.com](https://github.com)'a giriş yapın.
2. Sağ üst köşedeki **`+`** simgesine tıklayıp **"New repository"** seçin.
3. **Repository name:** `master-racer` yazın.
4. **Public** veya **Private** seçebilirsiniz (her ikisinde de GitHub Actions ücretsiz çalışır).
5. *"Add a README file"*, *"Add .gitignore"* seçeneklerini **İŞARETLEMEYİN** (çünkü kodlarımız zaten yerel bilgisayarınızda hazır).
6. **"Create repository"** butonuna tıklayın.
7. Karşınıza çıkan sayfadaki repo bağlantısını kopyalayın (Örn: `https://github.com/KULLANICI_ADINIZ/master-racer.git`).

### Adım 2.2: Bilgisayarınızdaki Projeyi GitHub'a Yükleme (Push)
Bilgisayarınızda PowerShell veya Terminal'i açıp şu komutları sırasıyla çalıştırın:

```powershell
# Proje ana klasörüne gidin
cd "c:\Master Racer"

# Git'i başlatın
git init

# Tüm dosyaları hazırlayın
git add .

# İlk commit'i oluşturun
git commit -m "feat: Master Racer tam altyapı ve Android uygulaması"

# Ana dalı main olarak ayarlayın
git branch -M main

# Kendi GitHub repo adresinizi buraya yapıştırın:
git remote add origin https://github.com/KULLANICI_ADINIZ/master-racer.git

# Kodları GitHub'a gönderin
git push -u origin main
```

*(Not: GitHub kullanıcı adı ve şifre/Personal Access Token isterse giriş yapın. Alternatif olarak **GitHub Desktop** uygulamasını kullanarak da klasörü repo olarak ekleyip "Publish repository" diyebilirsiniz).*

### Adım 2.3: Firebase Gizli Anahtarını GitHub Secrets'a Ekleme
GitHub Actions'ın veritabanına yarışmaları yazabilmesi ve FCM bildirimi atabilmesi için Firebase anahtarını güvenli şekilde tanımlamalıyız:

1. GitHub'da yeni açtığınız `master-racer` repo sayfasını açın.
2. Üstteki sekmelerden **Settings** (Ayarlar) sekmesine tıklayın.
3. Sol menüden **Secrets and variables > Actions** seçeneğine tıklayın.
4. Sağ üstteki yeşil **"New repository secret"** butonuna tıklayın.
5. Alanları şu şekilde doldurun:
   - **Name:** `FIREBASE_CREDENTIALS_JSON` *(harfi harfine aynı olmalıdır)*
   - **Secret:** 1. adımda Firebase'den indirdiğiniz servis hesabı `.json` dosyasını Not Defteri (Notepad) ile açın. İçindeki tüm metni (`{ ... }` dahil) kopyalayıp buraya yapıştırın.
6. **"Add secret"** butonuna tıklayın.

### Adım 2.4: Otomasyonu İlk Kez Test Etme
1. GitHub reponuzun üst menüsündeki **Actions** sekmesine tıklayın.
2. Sol tarafta **"Master Racer Scraper & Notification Cron"** iş akışını göreceksiniz, üzerine tıklayın.
3. Sağ tarafta beliren **"Run workflow"** butonuna tıklayıp yeşil **"Run workflow"** butonuna basın.
4. Birkaç saniye içinde bir görev başlayacak ve yeşil tik (başarılı) alacaktır.
5. Göreve tıklayarak TAF, EMA ve WMA tarayıcılarının bulduğu yarışmaları ve bildirim gönderim günlüklerini canlı olarak izleyebilirsiniz!
6. **Tebrikler!** Artık sistem her gün 06:00 UTC ve 18:00 UTC saatlerinde tamamen otonom ve ücretsiz olarak çalışacaktır.

---

## 3. Scraper'ı Bilgisayarda Yerel Olarak Test Etme (Opsiyonel)

Geliştirme yaparken bilgisayarınızda simülasyon testi yapmak isterseniz:
```bash
cd "c:\Master Racer"
pip install -r backend/requirements.txt

# Simülasyon modu (Firebase olmadan taranan yarışmaları JSON dosyasına kaydeder):
python -m backend.scraper.main --dry-run --export tarananlar.json
```

---

## 4. Android Uygulamasını Çalıştırma

1. **Android Studio**'yu açın.
2. **Open** diyerek `c:\Master Racer\android` klasörünü seçin.
3. `android/app/google-services.json` dosyanızın `c:\Master Racer\android\app\` altında olduğundan emin olun.
4. Paket adı `com.ecil.masterracer` olarak ayarlanmıştır.
5. Gradle Sync bittikten sonra telefonunuzu USB ile bağlayın veya bir emülatör seçip **Run 'app' (Shift + F10)** butonuna basın.
6. Uygulama açıldığında:
   - Bildirim iznini onaylayın (yeni yarışmalar için).
   - Yarışmaları tarihe, ülkeye, tesise veya branşa göre filtreleyin.
   - Herhangi bir yarışmaya tıklayıp **"Cihaz Takvimine Ekle"** butonunu test edin.

---

## 5. Sıfır Kod ile Admin Yönetimi (Doğrudan Web'den Düzenleme)

Özel bir web admin paneli kodlamanıza veya sunucu kiralamanıza gerek yoktur:
1. Firebase Console > **Firestore Database** ekranına gidin.
2. `competitions` koleksiyonundaki herhangi bir yarışmaya tıklayarak:
   - Tarihleri güncelleyebilir,
   - Reglaman PDF linkini (`regulations_url`) ekleyebilir veya değiştirebilir,
   - İptal edilen yarışmanın durumunu (`status: cancelled`) yapabilirsiniz.
3. Android uygulaması Firestore snapshot listener kullandığı için yapılan değişiklikler anında telefondaki ekranda güncellenecektir!
