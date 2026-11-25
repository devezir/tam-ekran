# Tam Ekran: Eksiksiz Ana Arayüz 🚀

[![Boyut](https://img.shields.io/badge/APK%20Boyutu-~486%20KB-00E5FF?style=flat-square)](releases/)
[![Sürüm](https://img.shields.io/badge/S%C3%BCr%C3%BCm-v1.0.0-00C853?style=flat-square)](version.json)
[![Uyumluluk](https://img.shields.io/badge/Android-2.2%20--%2017-brightgreen?style=flat-square)](releases/)
[![Bağımlılık](https://img.shields.io/badge/Bağımlılık-SIFIR%20(Pure%20Java)-orange?style=flat-square)](src/)
[![Reklam](https://img.shields.io/badge/Reklam-YOK%20(%250)-red?style=flat-square)](src/)
[![Lisans](https://img.shields.io/badge/Lisans-GPL--3.0-blue?style=flat-square)](LICENSE)

> **"Gereksiz her şeyden arındırılmış, Nova Launcher seviyesinde saf işlev odaklı, tüy gibi hafif Türkçe Android başlatıcısı."**

---

## ⚡ Neden Tam Ekran?

Piyasadaki popüler başlatıcılar (Nova Launcher, Microsoft Launcher vb.) onlarca megabaytlık reklam, analiz ve uyumluluk kütüphanesi barındırırken; **Tam Ekran**, modern masaüstü/çekmece/alt bar özelliklerini yalnızca **~486 KB** boyutunda, sıfır harici kütüphaneyle ve %100 saf Java ile sunar.

---

## 🛠️ Özellik Listesi (`v1.0.0`)

- **📐 Canlı Düzen Stüdyosu (`IzgaraKalibratoru`):** Masaüstü, Çekmece ve Alt Bar (Dock) kontrollerini tek 3 sekmeli yüzen pencerede birleştirir; satır/sütun (3–12), bağımsız simge boyutları (%50–%150), dock yüksekliği (48–96 dp), dock yuva sayısı (1–9), kaydırılabilir dock, dock arka plan stilleri, kenar boşluğu, klasör şekli, geçiş efektleri, çekmece saydamlığı, sık kullanılanlar ve sıralama anlık canlı önizlenir.
- **📱 Edge-to-Edge & 4 Yönlü Güvenli Alan (`GuvenliAlanYoneticisi`):** API 21+ şeffaf durum/gezinme çubuğu, API 28+ çentik (`DisplayCutout`) ve gerçek `WindowInsets` desteği yanında kullanıcının dilediği 4 yönden (Üst, Alt, Sol, Sağ) kırpma yapabilme kontrolü.
- **🖐️ Sürükle & Bırak ve 5 Saniye Geri Al (`SuruklemeKontrolcusu` & `GeriAlKontrolcusu`):** Çekmeceden masaüstüne veya dock'a sürükleme, şerit widget desteği, dock yeniden sıralama, masaüstü kilidi koruması ve yanlış silmeleri kurtaran Geri Al çubuğu.
- **🧩 Android Widget & Klasör Motoru (`WidgetYoneticisi` & `KlasorPenceresi`):** Saf Java `AppWidgetHost` + yapılandırma yeniden oluşturma (`Bundle` durum koruma), 12'li sayfalı klasör kartı, kapak klasörü modu ve 1 öğeye düşünce otomatik açılma.
- **🔎 T9 Akıllı Arama, Anlık Hesap Makinesi & Kategori Sekmeleri:** Türkçe karakter duyarlı T9 filtreleme, doğrudan arama kutusunda dört işlem / yüzde hesaplama ve özelleştirilebilir çekmece sekmeleri.
- **🔒 Uygulama Kilidi, Özel Etiket & Simge Paketleri:** 4 haneli yerel PIN ile uygulama kilitleme, uygulama adlarını ve simgelerini özelleştirme, Nova / ADW / Apex simge paketleri, bildirim noktası/sayacı ve çift dokunma / hareket kısayolları.
- **🛡️ Veri Bütünlüğü & Güvenli OTA Güncelleme:**
  - Bozuk masaüstü JSON verisini `karantina_` anahtarında koruyan ve boş veriyle ezilmesini engelleyen `MasaustuYoneticisi` + `MasaustuButunlukDenetleyici`.
  - Atomik `.tmp` yazma ve ön doğrulamalı JSON yedekleme/geri yükleme (`YedeklemeYoneticisi`), path-traversal korumalı `DosyaSaglayici`.
  - Yalnızca HTTPS + resmi GitHub alan adları, SHA-256 doğrulaması ve APK imza sertifikası eşleşmesiyle güncelleyen `GuncellemeYoneticisi`.

---

## 📊 Karşılaştırma

| Özellik | Ağır Başlatıcılar | **Tam Ekran (`v1.0.0`)** |
| :--- | :--- | :--- |
| **APK Boyutu** | 30–60 MB | **~486 KB** |
| **Harici Kütüphane / Reklam** | Çoklu SDK & İzleyici | **0 kütüphane, %0 reklam/telemetri** |
| **Canlı 3 Sekmeli Düzen Stüdyosu** | Kısıtlı veya Ücretli | **Tam Açık (Masaüstü + Çekmece + Dock)** |
| **Masaüstünü Kilitleme & Widget** | Var | **Var (Durum korumalı Widget Host)** |
| **İmza & SHA-256 Doğrulamalı OTA** | Mağaza bağımlı | **Dahili (HTTPS + SHA-256 + İmza)** |
| **Ana Dil & Yerelleştirme** | İngilizce ağırlıklı | **Ana dil Türkçe (`values`) + İngilizce (`values-en`)** |
| **Derleme Süresi** | 30–60 sn (Gradle) | **~2 saniye (`aapt2` + `javac` + `d8`)** |

---

## 🛠️ Nasıl Derlenir ve Test Edilir? (Gradle Olmadan)

Projede Gradle kullanılmaz; doğrudan `aapt2`, `javac`, `d8` ve `uber-apk-signer` ile derlenir ve imzalanır (`ANDROID_TOOLS_DIR`, `JDK_HOME_BIN`, `ANDROID_JAR` ortam değişkenleriyle özelleştirilebilir):

```bash
# Birim testlerini çalıştır (taze klonda gen/R.java otomatik üretilir):
python test.py

# Zorunlu birim testleriyle birlikte Release APK derle (imza için TAMEKRAN_KS_PASS veya gitignore'lı keystore/signing.properties gerekir):
python build.py

# Hızlı yerel debug derlemesi:
python build.py --debug
```

Windows'ta tek tıkla derleme için [`derle.bat`](derle.bat) de kullanılabilir.

---

## 📄 Lisans

Bu proje **GNU General Public License v3.0 (GPL-3.0)** altında lisanslanmıştır.  
Geliştirici: **Vezir** (GitHub: [@devezir](https://github.com/devezir))
