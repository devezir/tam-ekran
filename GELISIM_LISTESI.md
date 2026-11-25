# 📋 Tam Ekran - Gelecek Planlar ve Gelişim Listesi

Bu liste, ileride projeye kazandırılması planlanan ancak mevcut performansı ve hafifliği korumak amacıyla bekletilen özellikleri içerir.

---

### ⏳ Bekleyen Özellikler ve İyileştirmeler

- [ ] **GitHub Actions Bulut CI/CD Otomasyonu (Seçenek 4):**
  - **Açıklama:** GitHub üzerinden otomatik APK derleme, test çalıştırma ve release oluşturma hattı.
  - **Bekletilme Nedeni:** GitHub Actions ortamında önbellek sorunları, uzak anahtar yönetimi ve ağ gecikmeleri nedeniyle yerel derlemeye göre daha hantal kalması. Yerel ortamda `build.py` ve `derle.bat` ile 1.5 saniyede anında imzalı APK üretme hızı şimdilik tercih edilmektedir.

- [ ] **Android 14/15 Tahmini Geri Tuşu (Predictive Back Gesture):**
  - **Açıklama:** `OnBackInvokedDispatcher` ile modern cihazlarda yumuşak geri animasyon desteği.

- [ ] **Android 15 Edge-to-Edge Sistem Çubuğu Dinleyicisi:**
  - **Açıklama:** Mevcut interaktif güvenli alan kalibratörüne ek olarak yerel `WindowInsets` entegrasyonu.
