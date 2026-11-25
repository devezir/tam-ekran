# Tam Ekran — Mimari ve Davranış Spesifikasyonu (SPEC)

**Sürüm Hedefi:** `v1.0.0` (Saf Java, sıfır harici kütüphane bağımlılığı)  
**Ana Dil:** Türkçe (`res/values/strings.xml`) — İngilizce yerelleştirme (`res/values-en/strings.xml`) tam pariteyle korunur  
**Temel İlke:** Gradle olmadan hızlı (`aapt2` + `javac` + `d8` + `uber-apk-signer`), tüy gibi hafif (~486 KB), modüler ve veri kaybına kapalı (fail-closed) Android başlatıcısı.

---

## 1. Mimari Katmanlaşma (4 Katmanlı Tek Yönlü Bağımlılık)

1. **Katman 1 — Yaprak Veri ve Ayar Katmanı (UI & Activity Bağımlılıksız)**
   - `AyarBankasi`: Tek doğruluk kaynağı (SSOT). Tüm `SharedPreferences` dosya adları (`tamekran_ayarlar`, `tamekran_ozel_etiketler`, `tamekran_kullanim_sayaci`, `tamekran_uygulama_kilidi`), tüm ayar anahtarları ve sınırlandırılmış (örn. ızgara 3–12) erişim metotları buradadır.
   - Veri Yöneticileri: `MasaustuYoneticisi` (bozuk JSON tespiti `VERI_DURUMU_BOZUK` ve `karantina_` koruması dahil), `DockYoneticisi`, `KlasorYoneticisi`, `Kategoriler / KategoriYoneticisi`, `OzelEtiketYoneticisi`, `KullanimSayaci`, `UygulamaKilidiYoneticisi`.

2. **Katman 2 — İş Mantığı, Önbellek ve Güvenlik Katmanı**
   - `MasaustuButunlukDenetleyici`: Kurulu uygulamaları hem `CATEGORY_LAUNCHER` hem `getInstalledApplications(0)` ile denetler (devre dışı/dondurulmuş paketleri korur); çakışma ve sınır taşmasını onarır; bozuk veri durumunda üzerine yazmaz.
   - `SimgeOnbellegi`: `MAX_ONBELLEK = 160` üst sınırlı, senkronize `LinkedHashMap` tabanlı gerçek LRU önbellek.
   - `DosyaSaglayici`: Kanonik yol ve `..` / `/` / `\` / `\0` engelli güvenli dosya sunucusu.
   - `YedeklemeYoneticisi`: Geçici `.tmp` dosya üzerinden atomik yazma ve geri yükleme öncesi JSON şema/içerik doğrulaması.
   - `GuncellemeYoneticisi`: Yalnızca HTTPS + resmi GitHub alan adları (`github.com`, `githubusercontent.com`), yönlendirme başlıklarında tekrar alan adı kontrolü, SHA-256 doğrulaması (`version.json` `apkSha256`) ve kurulu paketle APK imza sertifikası (`GET_SIGNING_CERTIFICATES` / `GET_SIGNATURES`) eşleşmesi.

3. **Katman 3 — Sunum, Kontrolcü ve Düzen Stüdyosu Katmanı**
   - `IzgaraKalibratoru`: Masaüstü, Çekmece ve Alt Bar (Dock) sekmelerini tek yüzen Canlı Düzen Stüdyosu'nda birleştirir; tüm düzenlemeler kaydet/iptal yaşam döngüsüyle canlı önizlenir.
   - `GuvenliAlanKalibratoru` & `GuvenliAlanYoneticisi`: API 21+ gerçek `WindowInsets` / `DisplayCutout` desteği yanında 4 yönlü (Üst, Alt, Sol, Sağ) kullanıcı tanımlı güvenli alan kırpması sunar.
   - `MasaustuGorunumKontrolcusu`: Masaüstü sayfaları, widget kapsayıcıları, `CellLayout`, sayfa noktaları, rozetler ve canlı önizleme güncellemeleri.
   - `SistemOlayYoneticisi`: Paket ekleme/kaldırma `BroadcastReceiver` (API 33+ `RECEIVER_EXPORTED` uyumlu), `LauncherApps.Callback` ve otomatik döndürme `ContentObserver`.
   - `DockArayuzKontrolcusu`, `AramaVeCekmeceKontrolcusu`, `SuruklemeKontrolcusu`, `GeriAlKontrolcusu` (7 sn zaman aşımı).

4. **Katman 4 — Activity ve Sistem Servisleri**
   - `AnaEkranActivity`: `ExecutorService` (tek iş parçacıklı) + `AtomicInteger` nesil jetonu + 80 ms debounce ile yarış koşulsuz yükleme; `WidgetYoneticisi` `Bundle` durum koruma; `LauncherDurumu` durum makinesi yönlendirmesi.
   - `AyarlarActivity`, `BildirimDinleyiciServisi`, `ErisilebilirlikServisi`, `CihazYoneticisiAlicisi`, `PaketDegisimAlicisi`.

---

## 2. Başlatıcı Durum Makinesi (`LauncherDurumu`)

1. `LauncherDurumu.NORMAL (0)`: Varsayılan çok sayfalı masaüstü ve dock gezinme modu.
2. `LauncherDurumu.CEKMECE (1)`: Uygulama çekmecesi açık (kategoriler, sık kullanılanlar ve A–Z indeks).
3. `LauncherDurumu.ARAMA (2)`: Çekmece içinde T9 / metin / anlık hesap makinesi filtresi etkin; Geri tuşu önce aramayı temizleyip `CEKMECE` durumuna döner.
4. `LauncherDurumu.KLASOR (3)`: Klasör penceresi açık; kapanışta önceki durumuna (`CEKMECE` veya `NORMAL`) döner.
5. `LauncherDurumu.SURUKLEME (4)`: `SuruklemeKontrolcusu` aktif sürükleme/düzenleme oturumu; Geri tuşu sürüklemeyi sonlandırır ve boş alan uzun basma menüsü engellenir.

---

## 3. Hata Yönetimi, Güvenlik ve Derleme Kuralları

- **Kör `catch` Yasağı:** `catch (Throwable ignored) {}` kullanılmaz; beklenen platform atlamaları `LauncherLog.d` ile, iş/I/O hataları `LauncherLog.w`/`e` ile kaydedilir ve kullanıcıya Türkçe `strings.xml` mesajı sunulur.
- **Veri Kaybı Koruması:** Masaüstü JSON bozuksa (`VERI_DURUMU_BOZUK`) ilk kurulum verisiyle veya boş listeyle üzerine yazılmaz; ham veri `karantina_` anahtarında saklanır.
- **Sır Güvenliği ve Doğrulama:** `build.py` hiçbir sürüm takip edilen dosyada parola bulundurmaz (`TAMEKRAN_KS_PASS` ortam değişkeni veya gitignore'lı `keystore/signing.properties`); `test.py` taze klonda `gen/R.java` dosyasını `aapt2` ile otomatik üretip güncelliğini denetleyerek sıfır harici test bağımlılığıyla çalışır. `.github/workflows/build.yml` otomatik `push`/`pull_request` tetiklemeleri için **bilinçli olarak devre dışı bırakılmıştır** (yalnızca manuel `workflow_dispatch` açıktır); derleme ve testler yerel `build.py` / `test.py` kapısı üzerinden yürütülür.
