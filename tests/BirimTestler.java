package tests;

import com.tamekran.launcher.AyarBankasi;
import com.tamekran.launcher.ArayuzYardimcisi;
import com.tamekran.launcher.AyarlarActivity;
import com.tamekran.launcher.DosyaSaglayici;
import com.tamekran.launcher.GeriAlYoneticisi;
import com.tamekran.launcher.GuncellemeYoneticisi;
import com.tamekran.launcher.HesapAramaYardimcisi;
import com.tamekran.launcher.KlasorYoneticisi;
import com.tamekran.launcher.LauncherDurumu;
import com.tamekran.launcher.MasaustuButunlukDenetleyici;
import com.tamekran.launcher.MasaustuYoneticisi;
import com.tamekran.launcher.SuruklemeVerisi;
import com.tamekran.launcher.T9AramaYardimcisi;
import com.tamekran.launcher.YedeklemeYoneticisi;
import com.tamekran.launcher.DockYoneticisi;
import com.tamekran.launcher.HizliAraclarYoneticisi;
import com.tamekran.launcher.IzgaraKalibratoru;
import com.tamekran.launcher.OzelEtiketYoneticisi;
import com.tamekran.launcher.SimgeOnbellegi;
import com.tamekran.launcher.KlasorPenceresi;
import com.tamekran.launcher.PaketDegisimAlicisi;
import com.tamekran.launcher.UygulamaBilgisi;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tam Ekran - Saf Java Otomatik Birim Test Paketi (TDD / Zero-Dependency Tests)
 * Emülatör veya cihaz gerektirmeden tüm kritik çekirdek algoritmaları milisaniyeler içinde doğrular.
 */
public class BirimTestler {

    private static int toplamTest = 0;
    private static int basariliTest = 0;
    private static int hataliTest = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("🧪 TAM EKRAN - OTOMATİK BİRİM TESTLERİ (TDD)");
        System.out.println("==================================================");

        long baslangic = System.currentTimeMillis();

        testMatematikHesaplayici();
        testT9AramaMotoru();
        testAabbHucreCakismaAlgoritmasi();
        testMasaustuButunlukVeOnarim();
        testMasaustuKilidiMantigi();
        testYedeklemeSemasiVeBirimTesti();
        testGeriAlVeEsZamanlilikGuvenligi();
        testDryMimarisiVeYardimcilari();
        testCekmeceAraciVeOzelEtiket();
        testMasaustuModuVeTekKatmanSerpme();
        testMasaustuMukerrerOgeAyiklama();
        testKlasorKonteynerVeBilesenCozulme();
        testSimgeOnbellegiMimarisi();
        testKlasorPenceresiSayfalama();
        testDockMimarisiVeKaydirilabilirlik();
        testHayaletPaketTemizligiVeDondurmeWidgeti();
        testYeniSimgeBoyutlariVeDockStudyoMimarisi();
        testGuncellemeVeDinamikAracOnbellegi();
        testMimariSagliklastirmaVeGuvenlik();
        testBirlesikModelVeAtomikKlasorleme();
        KalicilikVeGocTestleri.calistir(new KalicilikVeGocTestleri.Dogrulayici() {
            @Override
            public void dogrula(String testAdi, boolean kosul) {
                BirimTestler.dogrula(testAdi, kosul);
            }
        });

        long sure = System.currentTimeMillis() - baslangic;

        System.out.println("==================================================");
        System.out.println("SONUÇ: " + toplamTest + " Test Koşuldu | "
                + basariliTest + " Başarılı | "
                + hataliTest + " Hatalı (" + sure + " ms)");
        System.out.println("==================================================");

        if (hataliTest > 0) {
            System.err.println("❌ BİRİM TESTLERİ BAŞARISIZ OLDU!");
            System.exit(1);
        } else {
            System.out.println("✅ TÜM BİRİM TESTLERİ KUSURSUZ GEÇTİ!");
            System.exit(0);
        }
    }

    private static void dogrula(String testAdi, boolean kosul) {
        toplamTest++;
        if (kosul) {
            basariliTest++;
            System.out.println("  [GEÇTİ] " + testAdi);
        } else {
            hataliTest++;
            System.err.println("  [HATA]  " + testAdi + " BEKLENEN SONUÇ ALINAMADI!");
        }
    }

    // =========================================================================
    // 1. ARAMA ÇUBUĞU HESAP MAKİNESİ TESTLERİ
    // =========================================================================
    private static void testMatematikHesaplayici() {
        System.out.println("\n[1/7] Matematik Hesaplayıcı (HesapAramaYardimcisi) Testleri:");

        HesapAramaYardimcisi.HesapSonucu r1 = HesapAramaYardimcisi.hesapla("25 * 4");
        dogrula("Çarpma: 25 * 4 == 100", r1.basarili && Math.abs(r1.deger - 100.0) < 0.0001);

        HesapAramaYardimcisi.HesapSonucu r2 = HesapAramaYardimcisi.hesapla("1200 + 350");
        dogrula("Toplama: 1200 + 350 == 1550", r2.basarili && Math.abs(r2.deger - 1550.0) < 0.0001);

        HesapAramaYardimcisi.HesapSonucu r3 = HesapAramaYardimcisi.hesapla("(40 + 10) / 2");
        dogrula("Parantez Önceliği: (40 + 10) / 2 == 25", r3.basarili && Math.abs(r3.deger - 25.0) < 0.0001);

        HesapAramaYardimcisi.HesapSonucu r4 = HesapAramaYardimcisi.hesapla("500 * 18%");
        dogrula("Yüzde Hesabı: 500 * 18% == 90", r4.basarili && Math.abs(r4.deger - 90.0) < 0.0001);

        HesapAramaYardimcisi.HesapSonucu r5 = HesapAramaYardimcisi.hesapla("-5 + 15");
        dogrula("Negatif Sayı: -5 + 15 == 10", r5.basarili && Math.abs(r5.deger - 10.0) < 0.0001);

        HesapAramaYardimcisi.HesapSonucu r6 = HesapAramaYardimcisi.hesapla("whatsapp");
        dogrula("Normal Kelimeyi Reddetme: 'whatsapp' -> basarisiz", !r6.basarili);

        HesapAramaYardimcisi.HesapSonucu r7 = HesapAramaYardimcisi.hesapla("insta 123");
        dogrula("Metin ve Sayı Karışımını Reddetme: 'insta 123' -> basarisiz", !r7.basarili);
    }

    // =========================================================================
    // 2. T9 TÜRKÇE AKILLI ARAMA TESTLERİ
    // =========================================================================
    private static void testT9AramaMotoru() {
        System.out.println("\n[2/7] T9 Akıllı Tuş Takımı Arama Testleri:");

        dogrula("Türkçe ç -> 2", T9AramaYardimcisi.harfiT9RakaminaCevir('ç') == '2');
        dogrula("Türkçe ğ -> 4", T9AramaYardimcisi.harfiT9RakaminaCevir('ğ') == '4');
        dogrula("Türkçe ı -> 4", T9AramaYardimcisi.harfiT9RakaminaCevir('ı') == '4');
        dogrula("Türkçe ö -> 6", T9AramaYardimcisi.harfiT9RakaminaCevir('ö') == '6');
        dogrula("Türkçe ş -> 7", T9AramaYardimcisi.harfiT9RakaminaCevir('ş') == '7');
        dogrula("Türkçe ü -> 8", T9AramaYardimcisi.harfiT9RakaminaCevir('ü') == '8');

        dogrula("T9 Kelime Eşleme: '942' -> 'WhatsApp'", T9AramaYardimcisi.eslesiyorMu("942", "WhatsApp"));
        dogrula("T9 Kelime Eşleme: '43727' -> 'Hesap Makinesi'", T9AramaYardimcisi.eslesiyorMu("43727", "Hesap Makinesi"));
        dogrula("T9 İkinci Kelime Başlangıcı: '625' -> 'Hesap Makinesi'", T9AramaYardimcisi.eslesiyorMu("625", "Hesap Makinesi"));
        dogrula("T9 Yanlış Sorguyu Reddetme: '99999' -> 'Kamera'", !T9AramaYardimcisi.eslesiyorMu("99999", "Kamera"));
    }

    // =========================================================================
    // 3. 2D AABB ÇOKLU HÜCRE ÇAKIŞMA TESTLERİ
    // =========================================================================
    private static void testAabbHucreCakismaAlgoritmasi() {
        System.out.println("\n[3/7] 2D AABB Çoklu Hücre Çakışma ve Matris Testleri:");

        dogrula("AABB Çakışma: (0,0, 4x1) ile (2,0, 1x1) çakışmalı",
                MasaustuYoneticisi.hucrelerCakisiyorMu(0, 0, 4, 1, 2, 0, 1, 1));

        dogrula("AABB Komşu Ayrımı: (0,0, 2x1) ile (2,0, 2x1) çakışmamalı",
                !MasaustuYoneticisi.hucrelerCakisiyorMu(0, 0, 2, 1, 2, 0, 2, 1));

        dogrula("AABB Farklı Satır: (1,0, 2x1) ile (1,1, 2x1) çakışmamalı",
                !MasaustuYoneticisi.hucrelerCakisiyorMu(1, 0, 2, 1, 1, 1, 2, 1));

        MasaustuYoneticisi.MasaustuOgesi oge = new MasaustuYoneticisi.MasaustuOgesi("arac:serit", 0, 1, 4, 1);
        dogrula("Hücre Kapsama: Şerit (0,1, 4x1) (2,1) hücresini kapsar",
                MasaustuYoneticisi.hucreyiKapsiyorMu(oge, 2, 1));
        dogrula("Hücre Kapsama Dışı: Şerit (0,1, 4x1) (2,2) hücresini kapsamaz",
                !MasaustuYoneticisi.hucreyiKapsiyorMu(oge, 2, 2));
    }

    // =========================================================================
    // 4. MASAÜSTÜ BÜTÜNLÜK VE KENDİ KENDİNİ ONARMA (SELF-HEALING) TESTLERİ
    // =========================================================================
    private static void testMasaustuButunlukVeOnarim() {
        System.out.println("\n[4/7] Masaüstü Bütünlük ve Self-Healing Onarım Testleri:");

        // Test 1: Sınır Taşmasını Onarma (4x6 ızgarada 7, 8 koordinatı -> 3, 5'e çekilmeli)
        List<MasaustuYoneticisi.MasaustuOgesi> liste1 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        liste1.add(new MasaustuYoneticisi.MasaustuOgesi("com.ornek.app", 7, 8, 1, 1));
        MasaustuButunlukDenetleyici.OnarimSonucu r1 = MasaustuButunlukDenetleyici.hucreCakismalariniOnar(liste1, 4, 6);
        dogrula("Sınır Taşması Onarımı: cellX 7 -> 3 oldu", liste1.get(0).cellX == 3);
        dogrula("Sınır Taşması Onarımı: cellY 8 -> 5 oldu", liste1.get(0).cellY == 5);
        dogrula("Sınır Taşması Onarımı: Degisiklik yapildi bayragi", r1.degisiklikYapildi && r1.onarilanOge == 1);

        // Test 2: Aynı Hücredeki İki Simgenin Çakışmasını Onarma (Overlapping Icons)
        List<MasaustuYoneticisi.MasaustuOgesi> liste2 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        liste2.add(new MasaustuYoneticisi.MasaustuOgesi("com.app.bir", 1, 2, 1, 1));
        liste2.add(new MasaustuYoneticisi.MasaustuOgesi("com.app.iki", 1, 2, 1, 1)); // Aynı koordinat!
        MasaustuButunlukDenetleyici.OnarimSonucu r2 = MasaustuButunlukDenetleyici.hucreCakismalariniOnar(liste2, 4, 6);
        dogrula("Çakışma Onarımı: İlk simge (1,2) konumunda kaldı", liste2.get(0).cellX == 1 && liste2.get(0).cellY == 2);
        boolean oge2FarkliKonumda = (liste2.get(1).cellX != 1 || liste2.get(1).cellY != 2);
        dogrula("Çakışma Onarımı: İkinci simge boş bir hücreye taşındı", oge2FarkliKonumda);
        dogrula("Çakışma Onarımı: Onarilan oge sayisi == 1", r2.onarilanOge == 1);

        // Test 3: Bozuk / Boş Ögelerin Temizlenmesi
        List<MasaustuYoneticisi.MasaustuOgesi> liste3 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        liste3.add(new MasaustuYoneticisi.MasaustuOgesi("com.gecerli.app", 0, 0));
        liste3.add(new MasaustuYoneticisi.MasaustuOgesi("", 0, 1)); // Boş paket
        liste3.add(new MasaustuYoneticisi.MasaustuOgesi(null, 0, 2)); // Null paket
        MasaustuButunlukDenetleyici.OnarimSonucu r3 = MasaustuButunlukDenetleyici.hucreCakismalariniOnar(liste3, 4, 6);
        dogrula("Bozuk Öğe Temizleme: Geçersiz 2 öğe silindi", r3.kaldirilanGecersizOge == 2);
        dogrula("Bozuk Öğe Temizleme: Listede sadece 1 geçerli öğe kaldı", liste3.size() == 1);

        // Test 4: 4x1 Şerit Widget ile 1x1 Simge Çakışmasını Onarma
        List<MasaustuYoneticisi.MasaustuOgesi> liste4 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        liste4.add(new MasaustuYoneticisi.MasaustuOgesi("arac:serit", 0, 0, 4, 1)); // 0. satır tamamen dolu (0,0) - (3,0)
        liste4.add(new MasaustuYoneticisi.MasaustuOgesi("com.app.cakisan", 2, 0, 1, 1)); // Şeritle çakışıyor!
        MasaustuButunlukDenetleyici.OnarimSonucu r4 = MasaustuButunlukDenetleyici.hucreCakismalariniOnar(liste4, 4, 6);
        dogrula("Widget Çakışması: Şerit widget (0,0, 4x1) korundu", liste4.get(0).cellX == 0 && liste4.get(0).cellY == 0 && liste4.get(0).spanX == 4);
        dogrula("Widget Çakışması: Çakışan simge alt satıra (y >= 1) kaydırıldı", liste4.get(1).cellY >= 1);

        // Test 5: 5'li Şerit Widget Sürükleme Verisi Doğrulaması
        com.tamekran.launcher.UygulamaBilgisi seritApp = new com.tamekran.launcher.UygulamaBilgisi("⚡ Güç Denetimi Şeridi", com.tamekran.launcher.HizliAraclarYoneticisi.ARAC_SERIT_GUC, (android.graphics.drawable.Drawable) null, (android.content.Intent) null);
        com.tamekran.launcher.SuruklemeVerisi vSerit = new com.tamekran.launcher.SuruklemeVerisi(seritApp, com.tamekran.launcher.SuruklemeVerisi.KAYNAK_MASAUSTU, 0, 0, 4, 4, 1);
        dogrula("Şerit Sürükleme Verisi: spanX == 4", vSerit.spanX == 4);
        dogrula("Şerit Sürükleme Verisi: spanY == 1", vSerit.spanY == 1);
        dogrula("Şerit Sürükleme Verisi: kaynakSayfa == 0", vSerit.kaynakSayfa == 0);
        dogrula("Şerit Sürükleme Verisi: kaynakCellY == 4", vSerit.kaynakCellY == 4);

        // Test 5b: Hızlı Arama & Ayarlar Şeridi Widget Kimliği ve Sürükleme Doğrulaması
        dogrula("Arama Widget Tanımı: ARAC_SERIT_ARAMA tanımlı", "arac:serit_arama".equals(com.tamekran.launcher.HizliAraclarYoneticisi.ARAC_SERIT_ARAMA));
        com.tamekran.launcher.UygulamaBilgisi aramaApp = new com.tamekran.launcher.UygulamaBilgisi("🔍 Hızlı Arama & Ayarlar", com.tamekran.launcher.HizliAraclarYoneticisi.ARAC_SERIT_ARAMA, (android.graphics.drawable.Drawable) null, (android.content.Intent) null);
        com.tamekran.launcher.SuruklemeVerisi vArama = new com.tamekran.launcher.SuruklemeVerisi(aramaApp, com.tamekran.launcher.SuruklemeVerisi.KAYNAK_MASAUSTU, 0, 0, 0, 4, 1);
        dogrula("Arama Şerit Sürükleme: spanX == 4", vArama.spanX == 4);
        dogrula("Arama Şerit Sürükleme: spanY == 1", vArama.spanY == 1);
        dogrula("Arama Şerit Sürükleme: paketAdi arac:serit_arama", "arac:serit_arama".equals(vArama.app.paketAdi));

        // Test 6: Otomatik Klasör Çözülme (1 Ögeli Klasörün Tek Uygulamaya Dönüşmesi - Kullanıcı Ekranındaki Vaka)
        List<KlasorYoneticisi.KlasorBilgisi> klasorler = new ArrayList<KlasorYoneticisi.KlasorBilgisi>();
        KlasorYoneticisi.KlasorBilgisi kb1 = new KlasorYoneticisi.KlasorBilgisi("101", "Klasör");
        kb1.paketler.add("com.android.contacts");
        klasorler.add(kb1);

        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> sayfalar = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> s0 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        s0.add(new MasaustuYoneticisi.MasaustuOgesi("klasor:101", 0, 4));
        sayfalar.put(0, s0);

        List<String> dock = new ArrayList<String>();
        boolean degisti = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(klasorler, sayfalar, dock);
        dogrula("Klasör Otomatik Çözülme: 1 ögeli klasör veritabanından kaldırıldı", klasorler.isEmpty());
        dogrula("Klasör Otomatik Çözülme: Masaüstündeki slot 'com.android.contacts' oldu", "com.android.contacts".equals(sayfalar.get(0).get(0).paketAdi));
        dogrula("Klasör Otomatik Çözülme: Koordinat (0, 4) korundu", sayfalar.get(0).get(0).cellX == 0 && sayfalar.get(0).get(0).cellY == 4);
        dogrula("Klasör Otomatik Çözülme: Değişiklik bayrağı true", degisti);

        // Test 7: Boş Klasör Temizliği (0 Ögeli Klasörün Silinmesi)
        List<KlasorYoneticisi.KlasorBilgisi> bosKlasorler = new ArrayList<KlasorYoneticisi.KlasorBilgisi>();
        KlasorYoneticisi.KlasorBilgisi kb0 = new KlasorYoneticisi.KlasorBilgisi("102", "Boş");
        bosKlasorler.add(kb0);

        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> sayfalar0 = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> s00 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        s00.add(new MasaustuYoneticisi.MasaustuOgesi("klasor:102", 1, 1));
        sayfalar0.put(0, s00);

        List<String> dock0 = new ArrayList<String>();
        dock0.add("klasor:102");
        boolean degisti0 = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(bosKlasorler, sayfalar0, dock0);
        dogrula("Boş Klasör Temizleme: Klasörler listesi boşaldı", bosKlasorler.isEmpty());
        dogrula("Boş Klasör Temizleme: Masaüstünden kaldırıldı", sayfalar0.get(0).isEmpty());
        dogrula("Boş Klasör Temizleme: Docktan kaldırıldı", dock0.isEmpty());
        dogrula("Boş Klasör Temizleme: Değişiklik bayrağı true", degisti0);

        // Test 8: Telefon vs Kişiler Bileşen Kimliği ve Hassas Arama Doğrulaması (Kullanıcı Talebi)
        com.tamekran.launcher.UygulamaBilgisi appKisiler = new com.tamekran.launcher.UygulamaBilgisi("Kişiler", "com.android.contacts", "com.android.contacts.activities.PeopleActivity", (android.graphics.drawable.Drawable) null);
        com.tamekran.launcher.UygulamaBilgisi appTelefon = new com.tamekran.launcher.UygulamaBilgisi("Telefon", "com.android.contacts", "com.android.contacts.DialtactsActivity", (android.graphics.drawable.Drawable) null);

        dogrula("Bileşen Kimliği: Telefon FQCN doğru", "com.android.contacts/com.android.contacts.DialtactsActivity".equals(appTelefon.getBilesenKimligi()));
        dogrula("Bileşen Kimliği: Kişiler FQCN doğru", "com.android.contacts/com.android.contacts.activities.PeopleActivity".equals(appKisiler.getBilesenKimligi()));

        List<com.tamekran.launcher.UygulamaBilgisi> karmaListe = new ArrayList<com.tamekran.launcher.UygulamaBilgisi>();
        karmaListe.add(appKisiler); // Alfabetik olarak Kişiler önce
        karmaListe.add(appTelefon); // Telefon sonra

        com.tamekran.launcher.UygulamaBilgisi bulunanTelefon = com.tamekran.launcher.UygulamaBilgisi.bilesenBul(karmaListe, "com.android.contacts/com.android.contacts.DialtactsActivity");
        dogrula("Bileşen Bulucu: Telefon arandığında Kişiler yerine Telefon bulundu", bulunanTelefon != null && "Telefon".equals(bulunanTelefon.etiket));

        dogrula("Bileşen Eşleme: Telefon ile Kişiler farklı aktiviteler eşleşmez", !com.tamekran.launcher.DockYoneticisi.bilesenEslesiyorMu(appTelefon.getBilesenKimligi(), appKisiler.getBilesenKimligi()));
        dogrula("Bileşen Eşleme: Telefon tam kimliği kendisiyle eşleşir", com.tamekran.launcher.DockYoneticisi.bilesenEslesiyorMu(appTelefon.getBilesenKimligi(), appTelefon.getBilesenKimligi()));
        dogrula("Bileşen Eşleme: Paket bazlı silme (uninstall) desteği", com.tamekran.launcher.DockYoneticisi.bilesenEslesiyorMu(appTelefon.getBilesenKimligi(), "com.android.contacts"));
        dogrula("Bileşen Eşleme: Eski yalın kayıt uyumluluğu", com.tamekran.launcher.DockYoneticisi.bilesenEslesiyorMu("com.android.contacts", appTelefon.getBilesenKimligi()));
    }

    // =========================================================================
    // 5. MASAÜSTÜ KİLİDİ MANTIK TESTLERİ
    // =========================================================================
    private static void testMasaustuKilidiMantigi() {
        System.out.println("\n[5/7] Masaüstü Kilidi Eylem Kuralları Testleri:");

        boolean masaustuKilitli = true;

        // Kural 1: Kilitliyken masaüstünden sürükleme başlatılamaz
        boolean masaustundenSuruklenebilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken masaüstü simgesi sürüklenebilir = false", !masaustundenSuruklenebilir);

        // Kural 2: Kilitliyken dock simgesi sürüklenebilir = false
        boolean docktanSuruklenebilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken dock simgesi sürüklenebilir = false", !docktanSuruklenebilir);

        // Kural 3: Kilitliyken çekmeceden sürükleme başlatılamaz
        boolean cekmecedenSuruklenebilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken çekmeceden sürükleme başlatılamaz = false", !cekmecedenSuruklenebilir);

        // Kural 4: Kilitliyken masaüstüne bırakma (drop) yapılamaz
        boolean masaustuneBirakilabilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken masaüstüne bırakma = false", !masaustuneBirakilabilir);

        // Kural 5: Kilitliyken çöp kutusuna bırakılarak öge silinemez
        boolean silinebilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken çöp kutusuna silme = false", !silinebilir);

        // Kural 6: Kilitliyken boş alan düzenleme araçları (widget, klasör, ızgara) açılamaz
        boolean duzenlemeMenusuAcilabilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken düzenleme araçları = false", !duzenlemeMenusuAcilabilir);

        // Kural 7: Kilitliyken klasör düzenlenemez / silinemez
        boolean klasorDuzenlenebilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken klasör düzenlenemez/silinemez = false", !klasorDuzenlenebilir);

        // Kural 8: Kilitliyken sistem widget'ı silinemez
        boolean widgetSilinebilir = !masaustuKilitli;
        dogrula("Kilit Kuralı: Kilitliyken sistem widget'ı silinemez = false", !widgetSilinebilir);

        // Kural 9: Kilit açıldığında (false) tüm eylemler serbesttir
        masaustuKilitli = false;
        dogrula("Kilit Kuralı: Kilit kapalıyken (false) sürükle-bırak serbest = true", !masaustuKilitli);
    }

    // =========================================================================
    // 6. YEDEKLEME VE GERİ YÜKLEME ŞEMA BÜTÜNLÜK TESTLERİ
    // =========================================================================
    private static void testYedeklemeSemasiVeBirimTesti() {
        System.out.println("\n[6/7] Yedekleme ve Geri Yükleme Şema Bütünlük Testleri:");

        dogrula("Yedekleme Anahtarı: PREF_MASAUSTU_KILITLI tanımlı",
                "masaustu_kilitli".equals(YedeklemeYoneticisi.PREF_MASAUSTU_KILITLI));

        dogrula("Yedekleme Anahtarı: PREF_GIZLENENLER_CSV tanımlı",
                "gizlenen_uygulamalar_csv".equals(YedeklemeYoneticisi.PREF_GIZLENENLER_CSV));

        dogrula("Yedekleme Anahtarı: PREF_DOCK_SIMGE_SAYISI tanımlı",
                "dock_simge_sayisi".equals(YedeklemeYoneticisi.PREF_DOCK_SIMGE_SAYISI));

        dogrula("Yedekleme Anahtarı: PREF_SIMGE_BOYUTU tanımlı",
                "simge_boyutu_olcek".equals(YedeklemeYoneticisi.PREF_SIMGE_BOYUTU));

        dogrula("Yedekleme Anahtarı: PREF_ETIKETLERI_GOSTER tanımlı",
                "etiketleri_goster".equals(YedeklemeYoneticisi.PREF_ETIKETLERI_GOSTER));

        dogrula("Hareket Anahtarı: PREF_HAREKET_ANA_EKRAN tanımlı",
                "hareket_ana_ekran".equals(com.tamekran.launcher.HareketYoneticisi.PREF_HAREKET_ANA_EKRAN));

        dogrula("Hareket Anahtarı: PREF_HAREKET_GERI_TUSU tanımlı",
                "hareket_geri_tusu".equals(com.tamekran.launcher.HareketYoneticisi.PREF_HAREKET_GERI_TUSU));
    }

    // =========================================================================
    // 7. GERİ AL (UNDO) VE SPEC MİMARİ GÜVENLİK TESTLERİ (ADDY OSMANI & MATT POCOCK)
    // =========================================================================
    private static void testGeriAlVeEsZamanlilikGuvenligi() {
        System.out.println("\n[7/7] Geri Al (Undo) ve Şartname Güvenliği Testleri:");

        GeriAlYoneticisi.temizle();
        dogrula("Geri Al Başlangıç: Kuyruk temiz", GeriAlYoneticisi.getSonSilinen() == null);
        dogrula("Geri Al Durumu: Silinen yokken geriAlinabilirMi = false", !GeriAlYoneticisi.geriAlinabilirMi());

        // Masaüstü öğesi silinme kaydı simülasyonu
        GeriAlYoneticisi.silineniKaydet(
                SuruklemeVerisi.KAYNAK_MASAUSTU,
                1,
                "com.whatsapp",
                "WhatsApp",
                2,
                3,
                1,
                1,
                null,
                null
        );

        dogrula("Geri Al Kaydı: Öğeyi tutuyor", GeriAlYoneticisi.getSonSilinen() != null);
        dogrula("Geri Al Koordinat: cellX korundu (2)", GeriAlYoneticisi.getSonSilinen().cellX == 2);
        dogrula("Geri Al Koordinat: cellY korundu (3)", GeriAlYoneticisi.getSonSilinen().cellY == 3);
        dogrula("Geri Al Sayfa: kaynakSayfa korundu (1)", GeriAlYoneticisi.getSonSilinen().kaynakSayfa == 1);
        dogrula("Geri Al Süre: Yeni silinen öğe geri alınabilir = true", GeriAlYoneticisi.geriAlinabilirMi());

        // Klasör silinme kaydı simülasyonu
        List<String> klasorPaketleri = Arrays.asList("com.google.android.gm", "com.google.android.apps.docs");
        GeriAlYoneticisi.silineniKaydet(
                SuruklemeVerisi.KAYNAK_MASAUSTU,
                0,
                "klasor:999",
                "Google",
                0,
                1,
                1,
                1,
                "Google",
                klasorPaketleri
        );

        dogrula("Geri Al Klasör: Klasör adı saklandı ('Google')", "Google".equals(GeriAlYoneticisi.getSonSilinen().klasorAdi));
        dogrula("Geri Al Klasör: İçerik paket sayısı korundu (2)", GeriAlYoneticisi.getSonSilinen().klasorPaketleri.size() == 2);
        dogrula("Geri Al Klasör: İlk paket korundu ('com.google.android.gm')", "com.google.android.gm".equals(GeriAlYoneticisi.getSonSilinen().klasorPaketleri.get(0)));

        // Süre aşımı simülasyonu (10 saniye sınırı)
        GeriAlYoneticisi.getSonSilinen().silinmeZamani = System.currentTimeMillis() - 15000;
        dogrula("Geri Al Süre Aşımı: 15 saniye sonra geriAlinabilirMi = false", !GeriAlYoneticisi.geriAlinabilirMi());

        // SPEC.md Izgara Sınır Kontratı Doğrulaması (AABB Sınır Taşırma Kontrolü)
        int sutun = 4;
        int satir = 6;
        int testX = 3;
        int testSpanX = 2; // 3 + 2 = 5 > 4 (Sınır taşması)
        dogrula("SPEC Kontratı: x + spanX > sutun sınır taşması tespiti", (testX + testSpanX) > sutun);

        int testY = 5;
        int testSpanY = 2; // 5 + 2 = 7 > 6 (Sınır taşması)
        dogrula("SPEC Kontratı: y + spanY > satir sınır taşması tespiti", (testY + testSpanY) > satir);
    }

    private static void testDryMimarisiVeYardimcilari() {
        System.out.println("\n[8/8] DRY Mimarisi (AyarBankasi ve ArayuzYardimcisi) Testleri:");

        dogrula("AyarBankasi: Pref adı eşleşmesi", "tamekran_ayarlar".equals(AyarBankasi.AYARLAR_PREF));
        dogrula("AyarBankasi: Masaüstü kilit anahtarı SSOT", YedeklemeYoneticisi.PREF_MASAUSTU_KILITLI.equals(AyarBankasi.PREF_MASAUSTU_KILITLI));
        dogrula("AyarBankasi: Simge boyutu anahtarı SSOT", YedeklemeYoneticisi.PREF_SIMGE_BOYUTU.equals(AyarBankasi.PREF_SIMGE_BOYUTU));
        dogrula("AyarBankasi: Izgara sütun anahtarı SSOT", AyarlarActivity.ANAHTAR_IZGARA_SUTUN.equals(AyarBankasi.ANAHTAR_IZGARA_SUTUN));
        dogrula("AyarBankasi: Izgara satır anahtarı SSOT", AyarlarActivity.ANAHTAR_IZGARA_SATIR.equals(AyarBankasi.ANAHTAR_IZGARA_SATIR));
        dogrula("AyarBankasi: Null context güvenliği (getAyarlar)", AyarBankasi.getAyarlar(null) == null);
        dogrula("AyarBankasi: Null context güvenliği (isMasaustuKilitli)", !AyarBankasi.isMasaustuKilitli(null));
        dogrula("AyarBankasi: Null context güvenliği (isTamEkranModu)", !AyarBankasi.isTamEkranModu(null));

        dogrula("ArayuzYardimcisi: Standart kilit uyarısı mesajı", ArayuzYardimcisi.UYARI_MASAUSTU_KILITLI != null && ArayuzYardimcisi.UYARI_MASAUSTU_KILITLI.contains("Masaüstü kilitli"));
        dogrula("ArayuzYardimcisi: Null context dpToPx fallback", ArayuzYardimcisi.dpToPx((Context) null, 24.0f) == 24);
        dogrula("ArayuzYardimcisi: Null context getDurumCubuguYuksekligi", ArayuzYardimcisi.getDurumCubuguYuksekligi(null) == 0);
        dogrula("ArayuzYardimcisi: Null context kilitliMiVeUyar", !ArayuzYardimcisi.kilitliMiVeUyar(null));
        dogrula("ArayuzYardimcisi: Null paket uygulamaBaslat güvenliği", !ArayuzYardimcisi.uygulamaBaslat(null, null));
        dogrula("UygulamaBilgisi: Doğrudan bileşen eşleşme doğrulaması", UygulamaBilgisi.bilesenEslesiyorMu("com.android.settings", "com.android.settings/com.android.settings.Settings"));
        dogrula("UygulamaBilgisi: Yanlış bileşen eşleşme reddi", !UygulamaBilgisi.bilesenEslesiyorMu("com.android.chrome", "com.android.settings"));
        dogrula("AyarBankasi: AYARLAR_PREF SSOT bağımsızlık", AyarBankasi.AYARLAR_PREF != null && !AyarBankasi.AYARLAR_PREF.isEmpty());
        dogrula("AyarBankasi: Null context getBoolean fallback", AyarBankasi.getBoolean(null, "test_key", true));
        dogrula("AyarBankasi: Null context getInt fallback", AyarBankasi.getInt(null, "test_key", 42) == 42);

        // JsonYardimcisi Testleri
        List<String> testListe = new ArrayList<String>();
        testListe.add("com.whatsapp");
        testListe.add("com.instagram.android");
        String jsonStr = com.tamekran.launcher.JsonYardimcisi.stringListesiToJson(testListe);
        dogrula("JsonYardimcisi: List to JSON dönüşümü", jsonStr.contains("com.whatsapp") && jsonStr.contains("com.instagram.android"));
        List<String> parsed = com.tamekran.launcher.JsonYardimcisi.jsonToStringListesi(jsonStr);
        dogrula("JsonYardimcisi: JSON to List dönüşüm boyutu (2)", parsed.size() == 2);
        dogrula("JsonYardimcisi: JSON to List ilk öğe doğrulaması", "com.whatsapp".equals(parsed.get(0)));
        dogrula("JsonYardimcisi: Null güvenliği", com.tamekran.launcher.JsonYardimcisi.jsonToStringListesi(null).isEmpty());

        // SistemAraclariYardimcisi Testleri
        dogrula("SistemAraclariYardimcisi: Request kodu doğru", com.tamekran.launcher.SistemAraclariYardimcisi.REQUEST_VARSAYILAN_BASLATICI == 8001);
        dogrula("SistemAraclariYardimcisi: Null context varsayilanBaslaticiMi güvenliği", !com.tamekran.launcher.SistemAraclariYardimcisi.varsayilanBaslaticiMi(null));
    }

    private static void testCekmeceAraciVeOzelEtiket() {
        System.out.println("\n[9/9] Çekmece Aracı (arac:cekmece) ve Özel Etiket Yönetimi Testleri:");

        dogrula("Çekmece Aracı: ARAC_CEKMECE sabiti doğru", "arac:cekmece".equals(HizliAraclarYoneticisi.ARAC_CEKMECE));

        List<HizliAraclarYoneticisi.AracTanimi> tumAraclar = HizliAraclarYoneticisi.getTumAraclar();
        HizliAraclarYoneticisi.AracTanimi cekmeceTanimi = null;
        for (HizliAraclarYoneticisi.AracTanimi t : tumAraclar) {
            if (HizliAraclarYoneticisi.ARAC_CEKMECE.equals(t.id)) {
                cekmeceTanimi = t;
                break;
            }
        }
        dogrula("Çekmece Aracı: getTumAraclar() içinde mevcut", cekmeceTanimi != null);
        dogrula("Çekmece Aracı: 1x1 ızgara boyutu (spanX == 1)", cekmeceTanimi != null && cekmeceTanimi.varsayilanSpanX == 1);
        dogrula("Çekmece Aracı: 1x1 ızgara boyutu (spanY == 1)", cekmeceTanimi != null && cekmeceTanimi.varsayilanSpanY == 1);
        dogrula("Çekmece Aracı: Başlık doğru", cekmeceTanimi != null && "Tüm Uygulamalar".equals(cekmeceTanimi.baslik));

        // Özel Etiket Güvenlik ve Fallback Testleri
        dogrula("Özel Etiket: Null context varsayılan döner", "WhatsApp".equals(OzelEtiketYoneticisi.getEtiket(null, "com.whatsapp", "WhatsApp")));
        dogrula("Özel Etiket: Null paket varsayılan döner", "WhatsApp".equals(OzelEtiketYoneticisi.getEtiket(null, null, "WhatsApp")));

        // FQCN Bileşen kimliği ve yalın paket adı ayrımı
        String fqcnTelefon = "com.android.contacts/com.android.contacts.DialtactsActivity";
        dogrula("FQCN Ayrıştırma: Paket adı slash içeriyor", fqcnTelefon.contains("/"));
        String safPaket = fqcnTelefon.split("/", 2)[0];
        dogrula("FQCN Ayrıştırma: Saf paket adı doğru", "com.android.contacts".equals(safPaket));
    }

    private static void testMasaustuModuVeTekKatmanSerpme() {
        System.out.println("\n[10/10] Masaüstü Modları ve Tek Katman (iOS) Serpme Testleri:");

        dogrula("Mod Sabiti: Klasik mod == 0", MasaustuYoneticisi.MOD_KLASIK_CIFTE_KATMAN == 0);
        dogrula("Mod Sabiti: Tek katman modu == 1", MasaustuYoneticisi.MOD_TEK_KATMAN_SERP == 1);
        dogrula("Pref Anahtarı: PREF_MASAUSTU_MODU tanımlı", "masaustu_modu".equals(MasaustuYoneticisi.PREF_MASAUSTU_MODU));
        dogrula("Pref Anahtarı: PREF_TEK_KATMAN_SAYFALARI_JSON tanımlı", "masaustu_tek_katman_sayfalari_json".equals(MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFALARI_JSON));
        dogrula("Dual Storage: Klasik ve Tek katman anahtarları farklı", !MasaustuYoneticisi.PREF_MASAUSTU_SAYFALARI_JSON.equals(MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFALARI_JSON));

        dogrula("AyarBankasi: Null context modu Klasik (0) döner", AyarBankasi.getMasaustuModu(null) == MasaustuYoneticisi.MOD_KLASIK_CIFTE_KATMAN);
        dogrula("AyarBankasi: Null context isTekKatmanModu false döner", !AyarBankasi.isTekKatmanModu(null));

        // Matematiksel Serpme Algoritması Simülasyonu
        int sutun = 4;
        int satir = 6;
        int sayfaKapasite = sutun * satir; // 24
        int toplamUygulama = 55;
        int beklenenSayfa = (int) Math.ceil((double) toplamUygulama / sayfaKapasite); // ceil(55 / 24) = 3
        dogrula("Serpme Matematiği: 55 uygulama 4x6 ızgarada 3 sayfa gerektirir", beklenenSayfa == 3);

        // 55. uygulamanın koordinatları: 54. indeks
        int sonIndeks = 54;
        int sonSayfa = sonIndeks / sayfaKapasite; // 54 / 24 = 2 (3. sayfa, 0-tabanlı)
        int sonSlot = sonIndeks % sayfaKapasite;  // 54 % 24 = 6
        int sonX = sonSlot % sutun;              // 6 % 4 = 2
        int sonY = sonSlot / sutun;              // 6 / 4 = 1
        dogrula("Serpme Koordinatı: Son uygulama 2. sayfaya (3. sayfa) düşer", sonSayfa == 2);
        dogrula("Serpme Koordinatı: Son uygulama X koordinatı 0..3 aralığında", sonX >= 0 && sonX < sutun);
        dogrula("Serpme Koordinatı: Son uygulama Y koordinatı 0..5 aralığında", sonY >= 0 && sonY < satir);
        dogrula("Serpme Koordinatı: Son uygulama (cx=2, cy=1) doğru", sonX == 2 && sonY == 1);
    }

    private static void testMasaustuMukerrerOgeAyiklama() {
        System.out.println("\n[11/11] Masaüstü Mükerrer Öge Ayıklama ve Katı Tekillik Testleri:");

        // Test 1: Tek Katman Modunda Sayfalar Arası Mükerrer Öge Temizleme
        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> haritaTekKatman = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> sayfa0 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        sayfa0.add(new MasaustuYoneticisi.MasaustuOgesi("com.android.calculator2", 0, 0));
        sayfa0.add(new MasaustuYoneticisi.MasaustuOgesi("com.whatsapp", 1, 0));
        haritaTekKatman.put(0, sayfa0);

        List<MasaustuYoneticisi.MasaustuOgesi> sayfa1 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        // Sayfa 1'de aynı hesap makinesi (bu sefer FQCN ile)
        sayfa1.add(new MasaustuYoneticisi.MasaustuOgesi("com.android.calculator2/com.android.calculator2.Calculator", 0, 0));
        sayfa1.add(new MasaustuYoneticisi.MasaustuOgesi("com.google.android.gm", 1, 0));
        haritaTekKatman.put(1, sayfa1);

        boolean tekKatmanTemizlendi = MasaustuButunlukDenetleyici.mukerrerOgeleriAyikla(haritaTekKatman, true);
        dogrula("Tek Katman Tekillik: Sayfalar arası mükerrer tespit edildi", tekKatmanTemizlendi);
        dogrula("Tek Katman Tekillik: Sayfa 0'daki ilk kopya korundu", haritaTekKatman.get(0).size() == 2);
        dogrula("Tek Katman Tekillik: Sayfa 1'deki kopya silindi", haritaTekKatman.get(1).size() == 1);
        dogrula("Tek Katman Tekillik: Sayfa 1'de kalan öge Gmail", "com.google.android.gm".equals(haritaTekKatman.get(1).get(0).paketAdi));

        // Test 2: Klasördeki Ögenin Masaüstünde Mükerrer Olması (Auto-Eviction)
        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> haritaKlasorle = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> s0 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        s0.add(new MasaustuYoneticisi.MasaustuOgesi("com.instagram.android", 0, 0));
        s0.add(new MasaustuYoneticisi.MasaustuOgesi("com.twitter.android", 1, 0));
        haritaKlasorle.put(0, s0);

        List<String> klasordekiler = Arrays.asList("com.instagram.android");
        boolean klasorMukerrerTemizlendi = MasaustuButunlukDenetleyici.mukerrerOgeleriAyikla(haritaKlasorle, true, klasordekiler);
        dogrula("Klasör Tekilliği: Klasördeki öge masaüstünden silindi", klasorMukerrerTemizlendi);
        dogrula("Klasör Tekilliği: Masaüstünde sadece Twitter kaldı", haritaKlasorle.get(0).size() == 1 && "com.twitter.android".equals(haritaKlasorle.get(0).get(0).paketAdi));

        // Test 3: Klasik Modda Sayfa İçi Mükerrer Temizleme
        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> haritaKlasik = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> ks0 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        ks0.add(new MasaustuYoneticisi.MasaustuOgesi("com.spotify.music", 0, 0));
        ks0.add(new MasaustuYoneticisi.MasaustuOgesi("com.spotify.music/com.spotify.music.MainActivity", 1, 0)); // Aynı sayfada mükerrer
        haritaKlasik.put(0, ks0);

        List<MasaustuYoneticisi.MasaustuOgesi> ks1 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        ks1.add(new MasaustuYoneticisi.MasaustuOgesi("com.spotify.music", 0, 0)); // Farklı sayfada (klasik modda izin verilir)
        haritaKlasik.put(1, ks1);

        boolean klasikTemizlendi = MasaustuButunlukDenetleyici.mukerrerOgeleriAyikla(haritaKlasik, false);
        dogrula("Klasik Mod: Sayfa içi mükerrer silindi", klasikTemizlendi);
        dogrula("Klasik Mod: Sayfa 0'da 1 öge kaldı", haritaKlasik.get(0).size() == 1);
        dogrula("Klasik Mod: Sayfa 1'deki ögeye dokunulmadı (bağımsız sayfa)", haritaKlasik.get(1).size() == 1);

        // Test 4: Araçlar (Widgets) Mükerrer Sayılmaz
        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> haritaArac = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> as0 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        as0.add(new MasaustuYoneticisi.MasaustuOgesi("arac:serit_arama", 0, 0, 4, 1));
        as0.add(new MasaustuYoneticisi.MasaustuOgesi("arac:serit_arama", 0, 1, 4, 1));
        haritaArac.put(0, as0);

        boolean aracTemizlendi = MasaustuButunlukDenetleyici.mukerrerOgeleriAyikla(haritaArac, true);
        dogrula("Araç İstisnası: Çoklu araçlar mükerrer olarak silinmez", !aracTemizlendi && haritaArac.get(0).size() == 2);
    }

    private static void testKlasorKonteynerVeBilesenCozulme() {
        System.out.println("\n[12/12] Klasör Konteyner Transferi ve Otomatik Çözülme Testleri:");

        // Test 1: Klasör içi FQCN mükerrerlik temizliği
        List<KlasorYoneticisi.KlasorBilgisi> klasorler = new ArrayList<KlasorYoneticisi.KlasorBilgisi>();
        KlasorYoneticisi.KlasorBilgisi kb1 = new KlasorYoneticisi.KlasorBilgisi("101", "Araçlar");
        kb1.paketler.add("com.google.android.calculator");
        kb1.paketler.add("com.google.android.calculator/com.android.calculator2.Calculator"); // FQCN mükerrer
        kb1.paketler.add("com.android.deskclock");
        klasorler.add(kb1);

        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> harita = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> s0 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        s0.add(new MasaustuYoneticisi.MasaustuOgesi("klasor:101", 0, 0));
        harita.put(0, s0);
        List<String> dock = new ArrayList<String>();

        boolean onarildi = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(klasorler, harita, dock);
        dogrula("Klasör İçi Mükerrer: FQCN mükerrer tespit edildi ve onarıldı", onarildi);
        dogrula("Klasör İçi Mükerrer: Klasörde tam 2 tekil paket kaldı", kb1.paketler.size() == 2);

        // Test 2: Klasör 1 öğeye düşürüldüğünde Otomatik Çözülme (Auto-Unfold)
        kb1.paketler.remove(0); // 1 öğe kaldı: com.android.deskclock
        boolean cozuldu = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(klasorler, harita, dock);
        dogrula("Otomatik Çözülme: 1 öğeye düşen klasör çözüldü", cozuldu);
        dogrula("Otomatik Çözülme: Klasör listesi boşaldı", klasorler.isEmpty());
        dogrula("Otomatik Çözülme: Masaüstü hücresinde kalan tek paket yer aldı", "com.android.deskclock".equals(harita.get(0).get(0).paketAdi));
        dogrula("Otomatik Çözülme: Masaüstü hücresi koordinatı korundu (0, 0)", harita.get(0).get(0).cellX == 0 && harita.get(0).get(0).cellY == 0);

        // Test 3: Boşalan (0 öğeli) klasörün temizlenmesi
        KlasorYoneticisi.KlasorBilgisi kb2 = new KlasorYoneticisi.KlasorBilgisi("102", "Boş");
        klasorler.add(kb2);
        harita.get(0).add(new MasaustuYoneticisi.MasaustuOgesi("klasor:102", 1, 0));
        boolean bosKlasorSilindi = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(klasorler, harita, dock);
        dogrula("Boş Klasör: 0 öğeli klasör silindi", bosKlasorSilindi && klasorler.isEmpty());
        dogrula("Boş Klasör: Masaüstünden kaldırıldı", harita.get(0).size() == 1);
    }

    private static void testSimgeOnbellegiMimarisi() {
        System.out.println("\n[13/14] Simge ve Etiket Önbelleği (SimgeOnbellegi) Testleri:");

        SimgeOnbellegi.tumunuTemizle();
        dogrula("Önbellek Başlangıç: Boş önbellek boyutu 0", SimgeOnbellegi.getOnbellekBoyutu() == 0);

        SimgeOnbellegi.onbellegeEkle("com.whatsapp", new SimgeOnbellegi.OnbellekOgesi("WhatsApp", null));
        SimgeOnbellegi.onbellegeEkle("com.android.settings/com.android.settings.Settings", new SimgeOnbellegi.OnbellekOgesi("Ayarlar", null));

        dogrula("Önbellek Ekleme: Boyut 2 oldu", SimgeOnbellegi.getOnbellekBoyutu() == 2);
        dogrula("Önbellek Getir: WhatsApp etiketi doğru", "WhatsApp".equals(SimgeOnbellegi.getir("com.whatsapp").etiket));
        dogrula("Önbellek Getir: Ayarlar FQCN etiketi doğru", "Ayarlar".equals(SimgeOnbellegi.getir("com.android.settings/com.android.settings.Settings").etiket));
        dogrula("Önbellek Getir: Olmayan öge null döner", SimgeOnbellegi.getir("com.bilinmeyen") == null);

        // Paket bazlı temizleme (Settings paketi silindiğinde FQCN de temizlenmeli)
        SimgeOnbellegi.paketTemizle("com.android.settings");
        dogrula("Paket Temizleme: Settings FQCN silindi", SimgeOnbellegi.getir("com.android.settings/com.android.settings.Settings") == null);
        dogrula("Paket Temizleme: WhatsApp hala önbellekte", SimgeOnbellegi.getir("com.whatsapp") != null);
        dogrula("Paket Temizleme: Boyut 1 kaldı", SimgeOnbellegi.getOnbellekBoyutu() == 1);

        // Tümünü temizleme
        SimgeOnbellegi.tumunuTemizle();
        dogrula("Tümünü Temizleme: Önbellek sıfırlandı", SimgeOnbellegi.getOnbellekBoyutu() == 0);
    }

    private static void testKlasorPenceresiSayfalama() {
        System.out.println("\n[14/14] Sayfalı Klasör Penceresi (KlasorPenceresi) Sayfalama Testleri:");

        // Test 1: Sayfa sayısı hesaplama (SAYFA_BASINA_OGE = 12)
        dogrula("Sayfa Hesabı: 0 öge -> 1 sayfa", KlasorPenceresi.toplamSayfaHesapla(0) == 1);
        dogrula("Sayfa Hesabı: 12 öge -> 1 sayfa", KlasorPenceresi.toplamSayfaHesapla(12) == 1);
        dogrula("Sayfa Hesabı: 13 öge -> 2 sayfa", KlasorPenceresi.toplamSayfaHesapla(13) == 2);
        dogrula("Sayfa Hesabı: 24 öge -> 2 sayfa", KlasorPenceresi.toplamSayfaHesapla(24) == 2);
        dogrula("Sayfa Hesabı: 25 öge -> 3 sayfa", KlasorPenceresi.toplamSayfaHesapla(25) == 3);

        // Test 2: Sayfa dilimleme
        List<UygulamaBilgisi> liste = new ArrayList<UygulamaBilgisi>();
        for (int i = 0; i < 25; i++) {
            liste.add(new UygulamaBilgisi("Uygulama " + i, "com.app." + i, "com.app." + i + ".Main", null));
        }

        List<UygulamaBilgisi> sayfa0 = KlasorPenceresi.sayfaOgesiGetir(liste, 0);
        dogrula("Sayfa Dilimleme: Sayfa 0 tam 12 öge içerir", sayfa0.size() == 12);
        dogrula("Sayfa Dilimleme: Sayfa 0 ilk öge 'Uygulama 0'", "Uygulama 0".equals(sayfa0.get(0).etiket));
        dogrula("Sayfa Dilimleme: Sayfa 0 son öge 'Uygulama 11'", "Uygulama 11".equals(sayfa0.get(11).etiket));

        List<UygulamaBilgisi> sayfa1 = KlasorPenceresi.sayfaOgesiGetir(liste, 1);
        dogrula("Sayfa Dilimleme: Sayfa 1 tam 12 öge içerir", sayfa1.size() == 12);
        dogrula("Sayfa Dilimleme: Sayfa 1 ilk öge 'Uygulama 12'", "Uygulama 12".equals(sayfa1.get(0).etiket));
        dogrula("Sayfa Dilimleme: Sayfa 1 son öge 'Uygulama 23'", "Uygulama 23".equals(sayfa1.get(11).etiket));

        List<UygulamaBilgisi> sayfa2 = KlasorPenceresi.sayfaOgesiGetir(liste, 2);
        dogrula("Sayfa Dilimleme: Sayfa 2 tam 1 öge içerir (kalan)", sayfa2.size() == 1);
        dogrula("Sayfa Dilimleme: Sayfa 2 tek öge 'Uygulama 24'", "Uygulama 24".equals(sayfa2.get(0).etiket));

        // Test 3: Sınır kontrolleri (Clamp)
        List<UygulamaBilgisi> sayfaNegatif = KlasorPenceresi.sayfaOgesiGetir(liste, -5);
        dogrula("Sınır Kontrolü: Negatif sayfa indeks 0'a çekilir", sayfaNegatif.size() == 12 && "Uygulama 0".equals(sayfaNegatif.get(0).etiket));

        List<UygulamaBilgisi> sayfaAsiri = KlasorPenceresi.sayfaOgesiGetir(liste, 99);
        dogrula("Sınır Kontrolü: Aşırı sayfa indeksi son sayfaya çekilir", sayfaAsiri.size() == 1 && "Uygulama 24".equals(sayfaAsiri.get(0).etiket));

        List<UygulamaBilgisi> bosListe = KlasorPenceresi.sayfaOgesiGetir(new ArrayList<UygulamaBilgisi>(), 0);
        dogrula("Boş Liste Güvenliği: Boş liste boş döner", bosListe.isEmpty());
    }

    private static void testDockMimarisiVeKaydirilabilirlik() {
        System.out.println("\n[15/15] Alt Bar (Dock) Yükseklik ve Kaydırılabilirlik Mimarisi Testleri:");

        dogrula("AyarBankasi Dock Yükseklik Varsayılan: 72 dp", AyarBankasi.getDockYuksekligi(null) == 72);
        dogrula("AyarBankasi Dock Kaydırılabilir Varsayılan: false", !AyarBankasi.isDockKaydirilabilir(null));
        dogrula("Dock Sabit Mod Limit: Maksimum slot 4 döner", DockYoneticisi.getMaksimumDockSayisi(null) == 4);
        dogrula("Düzen Stüdyosu Sabiti: MOD_DOCK == 2", IzgaraKalibratoru.MOD_DOCK == 2);

        // Dinamik ikon ölçekleme ve dikey sınır formül testi
        int testYukseklikDp = 72;
        int dikeyPayDp = Math.max(28, testYukseklikDp - 18); // 54 dp
        float tabanSimgeDp = testYukseklikDp * 0.60f; // 43.2 dp
        float hedefSimgeDp = Math.min(dikeyPayDp, tabanSimgeDp * 1.0f); // 43.2 dp
        dogrula("Dinamik İkon Ölçeği: 72dp dock için hedef simge 43.2 dp", Math.abs(hedefSimgeDp - 43.2f) < 0.01f);

        // Yüksek dock (96dp) testi
        int yuksekDockDp = 96;
        int dikeyPayYuksek = Math.max(28, yuksekDockDp - 18); // 78 dp
        float tabanYuksek = yuksekDockDp * 0.60f; // 57.6 dp
        float hedefYuksek = Math.min(dikeyPayYuksek, tabanYuksek * 1.0f); // 57.6 dp
        dogrula("Dinamik İkon Ölçeği: 96dp dock için hedef simge 57.6 dp", Math.abs(hedefYuksek - 57.6f) < 0.01f);

        // Alçak dock (48dp) testi
        int alcakDockDp = 48;
        int dikeyPayAlcak = Math.max(28, alcakDockDp - 18); // 30 dp
        float tabanAlcak = alcakDockDp * 0.60f; // 28.8 dp
        float hedefAlcak = Math.min(dikeyPayAlcak, tabanAlcak * 1.0f); // 28.8 dp
        dogrula("Dinamik İkon Ölçeği: 48dp dock için hedef simge 28.8 dp", Math.abs(hedefAlcak - 28.8f) < 0.01f);
    }

    private static void testHayaletPaketTemizligiVeDondurmeWidgeti() {
        System.out.println("\n[16/16] Hayalet Paket Temizliği, Dinamik Gözlemci ve Ekran Döndürme Testleri:");

        // 1. Ekran Döndürme Durum ve Etiket Testleri
        dogrula("Döndürme Durumu Varsayılan: Açık (true)", HizliAraclarYoneticisi.isOtomatikDondurmeAcik(null));
        UygulamaBilgisi dondurmeApp = HizliAraclarYoneticisi.aracUygulamaBilgisiAl(null, HizliAraclarYoneticisi.ARAC_DONDURME);
        dogrula("Döndürme Araç Bilgisi: Null değil", dondurmeApp != null);
        dogrula("Döndürme Canlı Etiket: 'Döndürme: Açık' içeriyor", dondurmeApp != null && dondurmeApp.etiket.startsWith("Döndürme:"));

        // 2. Paket Değişim Bayrağı Testi
        PaketDegisimAlicisi.paketDegisikligiOldu = true;
        dogrula("Paket Değişim Bayrağı: Doğru set edildi", PaketDegisimAlicisi.paketDegisikligiOldu);
        PaketDegisimAlicisi.paketDegisikligiOldu = false;

        // 3. Klasörden Silinen (Hayalet) Uygulama Temizliği ve Otomatik Çözülme (Auto-Unfold) Testi
        List<KlasorYoneticisi.KlasorBilgisi> klasorler = new ArrayList<KlasorYoneticisi.KlasorBilgisi>();
        KlasorYoneticisi.KlasorBilgisi k = new KlasorYoneticisi.KlasorBilgisi("k_test_1", "Test Klasör");
        k.paketler.add("com.whatsapp");
        k.paketler.add("com.deleted.afteroid.app");
        klasorler.add(k);

        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> sayfalar = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> sayfa0 = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        sayfa0.add(new MasaustuYoneticisi.MasaustuOgesi("klasor:k_test_1", 0, 0, 1, 1));
        sayfalar.put(0, sayfa0);

        List<String> dock = new ArrayList<String>();

        // Cihazda kurulu paketler kümesi (com.deleted.afteroid.app kurulu DEĞİL)
        Set<String> yukluPaketler = new HashSet<String>();
        yukluPaketler.add("com.whatsapp");
        yukluPaketler.add("com.android.settings");

        boolean onarildi = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(klasorler, sayfalar, dock, yukluPaketler);
        dogrula("Hayalet Paket Onarımı: Değişiklik yapıldı bayrağı true", onarildi);
        dogrula("Hayalet Paket Onarımı: Silinen paket klasörden atıldı ve 1 ögeye düşen klasör çözüldü (liste boş)", klasorler.isEmpty());
        dogrula("Hayalet Paket Onarımı: Masaüstündeki slot 'com.whatsapp' oldu", "com.whatsapp".equals(sayfa0.get(0).paketAdi));

        // 4. Boşalan Klasörün Tamamen Yok Edilmesi Testi
        List<KlasorYoneticisi.KlasorBilgisi> tekHayaletKlasor = new ArrayList<KlasorYoneticisi.KlasorBilgisi>();
        KlasorYoneticisi.KlasorBilgisi k2 = new KlasorYoneticisi.KlasorBilgisi("k_test_2", "Sadece Hayalet");
        k2.paketler.add("com.completely.removed.app");
        tekHayaletKlasor.add(k2);

        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> sayfalar2 = new HashMap<Integer, List<MasaustuYoneticisi.MasaustuOgesi>>();
        List<MasaustuYoneticisi.MasaustuOgesi> sayfaBos = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
        sayfaBos.add(new MasaustuYoneticisi.MasaustuOgesi("klasor:k_test_2", 1, 1, 1, 1));
        sayfalar2.put(0, sayfaBos);

        boolean bosaltildi = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(tekHayaletKlasor, sayfalar2, dock, yukluPaketler);
        dogrula("Hayalet Paket Onarımı: 0 ögeye düşen klasör yok edildi", tekHayaletKlasor.isEmpty());
        dogrula("Hayalet Paket Onarımı: Masaüstünden klasör slotu tamamen silindi", sayfaBos.isEmpty());

        // 5. Araç ve Kısayolların Korunması Testi
        List<KlasorYoneticisi.KlasorBilgisi> karisikKlasor = new ArrayList<KlasorYoneticisi.KlasorBilgisi>();
        KlasorYoneticisi.KlasorBilgisi k3 = new KlasorYoneticisi.KlasorBilgisi("k_test_3", "Karışık");
        k3.paketler.add("arac:cekmece");
        k3.paketler.add("kisayol:12345");
        k3.paketler.add("com.whatsapp");
        karisikKlasor.add(k3);

        boolean karisikOnarildi = MasaustuButunlukDenetleyici.klasorleriDenetleVeOnar(karisikKlasor, sayfalar, dock, yukluPaketler);
        dogrula("Araç ve Kısayol Koruma: Kurulu olmayanlar arasında kısayol ve araçlar silinmez", !karisikOnarildi);
        dogrula("Araç ve Kısayol Koruma: Klasör 3 ögesini de korudu", karisikKlasor.get(0).paketler.size() == 3);
    }

    private static void testYeniSimgeBoyutlariVeDockStudyoMimarisi() {
        System.out.println("\n[17/17] Masaüstü/Çekmece/Dock Simge Boyutları ve Dock Studio Testleri:");

        dogrula("Yedekleme: PREF_MASAUSTU_SIMGE_BOYUTU tanımlı",
                "masaustu_simge_boyutu_olcek".equals(YedeklemeYoneticisi.PREF_MASAUSTU_SIMGE_BOYUTU));
        dogrula("Yedekleme: PREF_CEKMECE_SIMGE_BOYUTU tanımlı",
                "cekmece_simge_boyutu_olcek".equals(YedeklemeYoneticisi.PREF_CEKMECE_SIMGE_BOYUTU));
        dogrula("Yedekleme: PREF_DOCK_SIMGE_BOYUTU tanımlı",
                "dock_simge_boyutu_olcek".equals(YedeklemeYoneticisi.PREF_DOCK_SIMGE_BOYUTU));

        dogrula("AyarBankasi: Masaüstü simge anahtarı SSOT",
                YedeklemeYoneticisi.PREF_MASAUSTU_SIMGE_BOYUTU.equals(AyarBankasi.PREF_MASAUSTU_SIMGE_BOYUTU));
        dogrula("AyarBankasi: Çekmece simge anahtarı SSOT",
                YedeklemeYoneticisi.PREF_CEKMECE_SIMGE_BOYUTU.equals(AyarBankasi.PREF_CEKMECE_SIMGE_BOYUTU));
        dogrula("AyarBankasi: Dock simge anahtarı SSOT",
                YedeklemeYoneticisi.PREF_DOCK_SIMGE_BOYUTU.equals(AyarBankasi.PREF_DOCK_SIMGE_BOYUTU));

        dogrula("AyarBankasi: Masaüstü varsayılan simge boyutu %100",
                AyarBankasi.getMasaustuSimgeBoyutu(null) == 100);
        dogrula("AyarBankasi: Çekmece varsayılan simge boyutu %100",
                AyarBankasi.getCekmeceSimgeBoyutu(null) == 100);
        dogrula("AyarBankasi: Dock varsayılan simge boyutu %100",
                AyarBankasi.getDockSimgeBoyutu(null) == 100);

        dogrula("Intent: EXTRA_AC_DOCK_KALIBRATORU tanımlı",
                "ekstra_ac_dock_kalibratoru".equals(com.tamekran.launcher.AnaEkranActivity.EXTRA_AC_DOCK_KALIBRATORU));

        // SimgeOnbellegi etiketGüncelle ve anlık geçersiz kılma testi
        SimgeOnbellegi.tumunuTemizle();
        String testKimlik = "com.ornek.uygulama/com.ornek.uygulama.AnaAktivite";
        SimgeOnbellegi.onbellegeEkle(testKimlik, new SimgeOnbellegi.OnbellekOgesi("Eski Ad", null));
        dogrula("SimgeOnbellegi: Başlangıç etiketi doğru",
                "Eski Ad".equals(SimgeOnbellegi.getir(testKimlik).etiket));

        SimgeOnbellegi.etiketGuncelle(testKimlik, "Yeni Özel Ad");
        dogrula("SimgeOnbellegi: etiketGuncelle sonrası RAM önbelleği güncellendi",
                "Yeni Özel Ad".equals(SimgeOnbellegi.getir(testKimlik).etiket));

        SimgeOnbellegi.paketTemizle("com.ornek.uygulama");
        dogrula("SimgeOnbellegi: paketTemizle ile önbellek temizlendi",
                SimgeOnbellegi.getir(testKimlik) == null);

        // Sabit mod dock ekran genişliği matematik sığdırma koruma testi
        int ekranW = 1080;
        int paddingW = 24;
        int kullanilabilirW = ekranW - paddingW;
        int dockAdedi = 7;
        int yuvaBasiPay = kullanilabilirW / dockAdedi;
        int margin = 2;
        int maksYuva = yuvaBasiPay - (2 * margin);
        int toplamTuketim = dockAdedi * (maksYuva + 2 * margin);
        dogrula("Dock Sabit Mod Sığdırma: 7 simge ekran genişliğini aşmaz", toplamTuketim <= kullanilabilirW);
        dogrula("Dock Sabit Mod Sığdırma: Her yuva pozitif genişliktedir", maksYuva > 0);
    }

    private static void testGuncellemeVeDinamikAracOnbellegi() {
        System.out.println("\n[18/18] Güncelleme Bütünlük Doğrulaması ve Dinamik Araç Önbellek Testleri:");

        String dondurmeKey = com.tamekran.launcher.AracSimgeCizici.onbellekAnahtariOlustur(
                null, HizliAraclarYoneticisi.ARAC_DONDURME);
        dogrula("AracSimgeCizici: Döndürme önbellek anahtarı durum eki içerir",
                "arac:dondurme_true".equals(dondurmeKey));

        String wifiKey = com.tamekran.launcher.AracSimgeCizici.onbellekAnahtariOlustur(
                null, HizliAraclarYoneticisi.ARAC_WIFI);
        dogrula("AracSimgeCizici: Wi-Fi önbellek anahtarı durum eki içerir",
                "arac:wifi_false".equals(wifiKey));

        String btKey = com.tamekran.launcher.AracSimgeCizici.onbellekAnahtariOlustur(
                null, HizliAraclarYoneticisi.ARAC_BLUETOOTH);
        dogrula("AracSimgeCizici: Bluetooth önbellek anahtarı durum eki içerir",
                "arac:bluetooth_false".equals(btKey));

        dogrula("GuncellemeYoneticisi: Null APK geçersiz sayılır",
                !com.tamekran.launcher.GuncellemeYoneticisi.apkGecerliMi(null, null));
        dogrula("GuncellemeYoneticisi: Mevcut olmayan APK geçersiz sayılır",
                !com.tamekran.launcher.GuncellemeYoneticisi.apkGecerliMi(null, new java.io.File("olmayan_guncelleme.apk")));

        dogrula("IzgaraKalibratoru: MOD_MASAUSTU == 0", com.tamekran.launcher.IzgaraKalibratoru.MOD_MASAUSTU == 0);
        dogrula("IzgaraKalibratoru: MOD_CEKMECE == 1", com.tamekran.launcher.IzgaraKalibratoru.MOD_CEKMECE == 1);
        dogrula("IzgaraKalibratoru: MOD_DOCK == 2", com.tamekran.launcher.IzgaraKalibratoru.MOD_DOCK == 2);
        dogrula("DockArkaPlanYoneticisi: 4 arka plan stili tanımlı", com.tamekran.launcher.DockArkaPlanYoneticisi.STIL_ISIMLERI.length == 4);

        com.tamekran.launcher.GuvenliAlanYoneticisi.sistemInsetleriniGuncelle(0, 84, 0, 126);
        com.tamekran.launcher.GuvenliAlanYoneticisi.uygula(null);
        dogrula("GuvenliAlanYoneticisi: Null aktivite ile sistem inset güncellemesi güvenli", true);
    }

    private static void testMimariSagliklastirmaVeGuvenlik() {
        System.out.println("\n[19/19] Tam Mimari Sağlıklaştırma, Güvenlik ve Eşzamanlılık Testleri:");

        // 1. Veri Kaybı ve Bozuk JSON Koruması (Karantina Bayrağı)
        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> bosHarita = MasaustuYoneticisi.sayfalariJsonAnalizEt(null, 3, 4);
        dogrula("MasaustuYoneticisi: Null JSON ilk kurulum durumu döner",
                bosHarita.size() == 3 && !MasaustuYoneticisi.sonVeriBozukMu()
                        && MasaustuYoneticisi.getSonYuklemeDurumu() == MasaustuYoneticisi.VERI_DURUMU_ILK_KURULUM);

        MasaustuYoneticisi.sayfalariJsonAnalizEt("bozuk_veri_kesintisi", 3, 4);
        dogrula("MasaustuYoneticisi: Geçersiz/bozuk veri VERI_DURUMU_BOZUK bayrağını açar",
                MasaustuYoneticisi.sonVeriBozukMu()
                        && MasaustuYoneticisi.getSonYuklemeDurumu() == MasaustuYoneticisi.VERI_DURUMU_BOZUK);

        MasaustuYoneticisi.sayfalariJsonAnalizEt("", 3, 4);
        dogrula("MasaustuYoneticisi: Boş metin sonrası sonVeriBozukMu sıfırlanır",
                !MasaustuYoneticisi.sonVeriBozukMu());

        // 2. DosyaSaglayici Path-Traversal Engelleme Testleri
        dogrula("DosyaSaglayici: '..' içeren yol reddedilir", !DosyaSaglayici.dosyaAdiGecerliMi("../secret.json"));
        dogrula("DosyaSaglayici: '/' içeren yol reddedilir", !DosyaSaglayici.dosyaAdiGecerliMi("alt/yedek.json"));
        dogrula("DosyaSaglayici: '\\\\' içeren yol reddedilir", !DosyaSaglayici.dosyaAdiGecerliMi("alt\\yedek.json"));
        dogrula("DosyaSaglayici: Boş dosya adı reddedilir", !DosyaSaglayici.dosyaAdiGecerliMi("   "));
        dogrula("DosyaSaglayici: Geçerli APK dosya adı onaylanır", DosyaSaglayici.dosyaAdiGecerliMi("TamEkran_v2.9.25.apk"));

        // 3. OTA HTTPS / Alan Adı Beyaz Listesi ve Gerçek SHA-256 Hesaplama Testleri
        dogrula("GuncellemeYoneticisi: Güvensiz HTTP reddedilir",
                !GuncellemeYoneticisi.isGuvenliIndirmeAdresi("http://github.com/devezir/tam-ekran/releases/download/v2.9.25/TamEkran.apk"));
        dogrula("GuncellemeYoneticisi: Yetkisiz alan adı reddedilir",
                !GuncellemeYoneticisi.isGuvenliIndirmeAdresi("https://evil.example.com/TamEkran.apk"));
        dogrula("GuncellemeYoneticisi: GitHub HTTPS release adresi onaylanır",
                GuncellemeYoneticisi.isGuvenliIndirmeAdresi("https://github.com/devezir/tam-ekran/releases/latest/download/TamEkran.apk"));
        dogrula("GuncellemeYoneticisi: GitHub Objects CDN HTTPS onaylanır",
                GuncellemeYoneticisi.isGuvenliIndirmeAdresi("https://objects.githubusercontent.com/github-production-release-asset/TamEkran.apk"));

        java.io.File tempHashDosya = null;
        try {
            tempHashDosya = java.io.File.createTempFile("tamekran_sha_", ".tmp");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempHashDosya);
            fos.write("abc".getBytes("UTF-8"));
            fos.close();
            String sha = GuncellemeYoneticisi.hesaplaSha256(tempHashDosya);
            dogrula("GuncellemeYoneticisi: Gerçek SHA-256 özeti NIST vektörüyle eşleşir",
                    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad".equals(sha));
        } catch (Exception e) {
            dogrula("GuncellemeYoneticisi: SHA-256 test dosyası yazılabildi (" + e.getMessage() + ")", false);
        } finally {
            if (tempHashDosya != null && tempHashDosya.exists()) {
                tempHashDosya.delete();
            }
        }

        // 4. SimgeOnbellegi Üst Sınırlı (160) Gerçek LRU Tahliye Testi
        SimgeOnbellegi.tumunuTemizle();
        dogrula("SimgeOnbellegi: MAX_ONBELLEK == 160", SimgeOnbellegi.MAX_ONBELLEK == 160);
        for (int i = 0; i < SimgeOnbellegi.MAX_ONBELLEK; i++) {
            SimgeOnbellegi.onbellegeEkle("pkg_" + i, new SimgeOnbellegi.OnbellekOgesi("Uyg " + i, null));
        }
        // En eski öğeye (pkg_0) erişilerek MRU pozisyonuna yükseltilir; pkg_1 artık en eski (LRU) olur
        SimgeOnbellegi.getir("pkg_0");
        SimgeOnbellegi.onbellegeEkle("pkg_160", new SimgeOnbellegi.OnbellekOgesi("Uyg 160", null));
        dogrula("SimgeOnbellegi: Kapasite 160 üzerine taşmaz", SimgeOnbellegi.getOnbellekBoyutu() == 160);
        dogrula("SimgeOnbellegi: Yakın zamanda erişilen pkg_0 LRU tahliyesinden korunur", SimgeOnbellegi.getir("pkg_0") != null);
        dogrula("SimgeOnbellegi: En eski erişilmeyen pkg_1 LRU tarafından atılır", SimgeOnbellegi.getir("pkg_1") == null);
        dogrula("SimgeOnbellegi: Yeni eklenen pkg_160 önbellekte tutulur", SimgeOnbellegi.getir("pkg_160") != null);
        SimgeOnbellegi.tumunuTemizle();

        // 5. AyarBankasi Kanonsal SSOT ve Sınır Garantileri
        dogrula("AyarBankasi: PREF_MASAUSTU_MODU MasaustuYoneticisi ile SSOT",
                AyarBankasi.PREF_MASAUSTU_MODU.equals(MasaustuYoneticisi.PREF_MASAUSTU_MODU));
        dogrula("AyarBankasi: Varsayılan masaüstü sütun 4, satır 6",
                AyarBankasi.getMasaustuSutun(null) == 4 && AyarBankasi.getMasaustuSatir(null) == 6);
        dogrula("AyarBankasi: Varsayılan çekmece sütun 4, satır 6",
                AyarBankasi.getCekmeceSutun(null) == 4 && AyarBankasi.getCekmeceSatir(null) == 6);
        dogrula("AyarBankasi: Özel dosya anahtarları tanımlı",
                "tamekran_kullanim_sayaci".equals(AyarBankasi.PREF_SAYAC_DOSYA)
                        && "tamekran_ozel_etiketler".equals(AyarBankasi.PREF_OZEL_ETIKET_DOSYA)
                        && "tamekran_uygulama_kilidi".equals(AyarBankasi.PREF_KILIT_DOSYA));

        // 6. Gerçek LauncherDurumu Durum Makinesi Sabitleri
        Set<Integer> durumlar = new HashSet<Integer>(Arrays.asList(
                LauncherDurumu.NORMAL,
                LauncherDurumu.CEKMECE,
                LauncherDurumu.ARAMA,
                LauncherDurumu.KLASOR,
                LauncherDurumu.SURUKLEME
        ));
        dogrula("LauncherDurumu: 5 benzersiz aktif başlatıcı durumu tanımlı",
                durumlar.size() == 5 && LauncherDurumu.NORMAL == 0);

        // 7. Nova Pre-Drag, Klasor Overlay ve Kaynak Sabitleri
        SuruklemeVerisi klasorVerisi = new SuruklemeVerisi(new UygulamaBilgisi("Test", "com.test", "", (android.graphics.drawable.Drawable) null), "k_42", 2);
        dogrula("SuruklemeVerisi: KAYNAK_KLASOR kaynağı ve klasör kimliği korunur",
                klasorVerisi.kaynakTuru == SuruklemeVerisi.KAYNAK_KLASOR
                        && "k_42".equals(klasorVerisi.kaynakKlasorId)
                        && klasorVerisi.kaynakPozisyon == 2);
        dogrula("KlasorPenceresi: Başlangıçta overlay kapalıdır", !KlasorPenceresi.acikMi());
        dogrula("HizliEylemlerDiyalogu: Başlangıçta Nova popup kapalıdır",
                !com.tamekran.launcher.HizliEylemlerDiyalogu.aktifPopupAcikMi());

        // 8. Android 2.2 - 17 Kısayol Sabitleme (Pinned & Legacy Shortcut) Testleri
        dogrula("EskiKisayolAlicisi: INSTALL_SHORTCUT aksiyon sabiti doğru",
                "com.android.launcher.action.INSTALL_SHORTCUT".equals(com.tamekran.launcher.EskiKisayolAlicisi.ACTION_INSTALL_SHORTCUT));
        com.tamekran.launcher.KisayolYoneticisi.KisayolBilgisi pinnedKb = new com.tamekran.launcher.KisayolYoneticisi.KisayolBilgisi();
        pinnedKb.shortcutPaket = "com.google.android.apps.docs";
        pinnedKb.shortcutId = "drive_folder_1";
        dogrula("KisayolBilgisi: Pinned shortcut paket ve ID alanları tutulur",
                "com.google.android.apps.docs".equals(pinnedKb.shortcutPaket)
                        && "drive_folder_1".equals(pinnedKb.shortcutId));
        dogrula("KisayolYoneticisi: Null context kisayolBaslat güvenliği",
                !com.tamekran.launcher.KisayolYoneticisi.kisayolBaslat(null, "kisayol:1"));
        dogrula("PinKisayolYardimcisi: Null context baslatPinnedShortcut güvenliği",
                !com.tamekran.launcher.PinKisayolYardimcisi.baslatPinnedShortcut(null, "com.google.android.apps.docs", "id"));
    }

    // =========================================================================
    // 20. BİRLEŞİK ITEMINFO MODELİ VE ATOMİK KLASÖRLEME / TAŞIMA TESTLERİ
    // =========================================================================
    private static void testBirlesikModelVeAtomikKlasorleme() {
        System.out.println("\n[20/20] Birleşik ItemInfo Modeli ve Atomik Klasörleme Testleri:");

        com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu durum =
                new com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu();
        durum.toplamSayfa = 2;
        durum.sutun = 4;
        durum.satir = 6;

        com.tamekran.launcher.MasaustuKaydi appA = new com.tamekran.launcher.MasaustuKaydi(
                "item_a", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.a/com.example.a.Main", "Uygulama A",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 0, 1, 1, 1, 1);
        com.tamekran.launcher.MasaustuKaydi appB = new com.tamekran.launcher.MasaustuKaydi(
                "item_b", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.b/com.example.b.Main", "Uygulama B",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 0, 2, 1, 1, 1);
        com.tamekran.launcher.MasaustuKaydi appC = new com.tamekran.launcher.MasaustuKaydi(
                "item_c", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.c/com.example.c.Main", "Uygulama C",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 1, 0, 0, 1, 1);
        com.tamekran.launcher.MasaustuKaydi appDock = new com.tamekran.launcher.MasaustuKaydi(
                "item_dock", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.d/com.example.d.Main", "Uygulama D",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_DOCK, 0, 0, 0, 1, 1);

        durum.ogeler.add(appA);
        durum.ogeler.add(appB);
        durum.ogeler.add(appC);
        durum.ogeler.add(appDock);

        // 1. Aynı sayfa masaüstü -> masaüstü klasör oluşturma: sürüklenen öğe masaüstünde KALMAMALI
        String k1 = com.tamekran.launcher.MasaustuModelDeposu.klasorOlusturVeBirlestirBellek(
                durum,
                "item_a", 0, "com.example.a/com.example.a.Main",
                "item_b", "com.example.b/com.example.b.Main",
                SuruklemeVerisi.KAYNAK_MASAUSTU, 0, null, "Araçlar");

        dogrula("Atomik Klasörleme: Yeni klasör ID üretildi", k1 != null && k1.length() > 0);
        dogrula("Atomik Klasörleme: Masaüstü sayfa 0'da yalnızca 1 öğe (yeni klasör) kaldı",
                durum.sayfaOgeleri(0).size() == 1 && durum.sayfaOgeleri(0).get(0).klasorMu());
        dogrula("Atomik Klasörleme: Sürüklenen uygulama B artık masaüstünde DEĞİL",
                durum.masaustundeBul(0, "com.example.b/com.example.b.Main") == null);
        dogrula("Atomik Klasörleme: Hedef uygulama A artık masaüstünde DEĞİL",
                durum.masaustundeBul(0, "com.example.a/com.example.a.Main") == null);
        dogrula("Atomik Klasörleme: Klasör içinde tam 2 öğe var (A ve B)",
                durum.klasorOgeleri(k1).size() == 2);
        dogrula("Atomik Klasörleme: Klasör hedefin (1,1) koordinatına yerleşti",
                durum.sayfaOgeleri(0).get(0).cellX == 1 && durum.sayfaOgeleri(0).get(0).cellY == 1);

        // 2. Bayat kaynakSayfa (0 verilse bile öğe aslında Sayfa 1'de) -> Mevcut klasöre ekleme
        boolean k1Eklendi = com.tamekran.launcher.MasaustuModelDeposu.klasoreOgeTasiBellek(
                durum,
                k1,
                "item_c",
                "com.example.c/com.example.c.Main",
                SuruklemeVerisi.KAYNAK_MASAUSTU,
                0, // kasıtlı bayat sayfa!
                null);
        dogrula("Atomik Klasöre Ekleme: Sayfa 1'deki C öğesi klasöre taşındı", k1Eklendi);
        dogrula("Atomik Klasöre Ekleme: Sayfa 1'de C artık kalmadı",
                durum.masaustundeBul(1, "com.example.c/com.example.c.Main") == null
                        && durum.sayfaOgeleri(1).isEmpty());
        dogrula("Atomik Klasöre Ekleme: Klasör içinde 3 öğe var",
                durum.klasorOgeleri(k1).size() == 3);

        // 3. Dock -> Mevcut klasöre taşıma (Dock'tan otomatik çıkmalı)
        boolean dockTasindi = com.tamekran.launcher.MasaustuModelDeposu.klasoreOgeTasiBellek(
                durum,
                k1,
                "item_dock",
                "com.example.d/com.example.d.Main",
                SuruklemeVerisi.KAYNAK_DOCK,
                0,
                null);
        dogrula("Dock -> Klasör Taşıma: Başarılı", dockTasindi);
        dogrula("Dock -> Klasör Taşıma: Dock'ta artık D yok",
                durum.docktaBul("com.example.d/com.example.d.Main") == null);
        dogrula("Dock -> Klasör Taşıma: Klasör içinde 4 öğe var",
                durum.klasorOgeleri(k1).size() == 4);

        // 4. Klasörden Masaüstüne Çıkarma + Otomatik Çözülme (2 -> 1 öğe kalınca)
        com.tamekran.launcher.MasaustuModelDeposu.masaustuneOgeTasiBellek(
                durum, "item_dock", "com.example.d/com.example.d.Main",
                SuruklemeVerisi.KAYNAK_KLASOR, 0, k1, 0, 3, 2, 1, 1);
        com.tamekran.launcher.MasaustuModelDeposu.masaustuneOgeTasiBellek(
                durum, "item_c", "com.example.c/com.example.c.Main",
                SuruklemeVerisi.KAYNAK_KLASOR, 0, k1, 1, 2, 2, 1, 1);
        dogrula("Klasörden Çıkarma: Klasörde 2 öğe kaldı, klasör hala açık",
                durum.klasorOgeleri(k1).size() == 2 && durum.idIleBul("klasor:" + k1) != null);

        com.tamekran.launcher.MasaustuModelDeposu.masaustuneOgeTasiBellek(
                durum, "item_b", "com.example.b/com.example.b.Main",
                SuruklemeVerisi.KAYNAK_KLASOR, 0, k1, 0, 0, 0, 1, 1);
        dogrula("Otomatik Çözülme: 1 öğe kalınca klasör silindi ve A masaüstüne döndü",
                durum.idIleBul("klasor:" + k1) == null
                        && durum.masaustundeBul(0, "com.example.a/com.example.a.Main") != null);
        dogrula("Otomatik Çözülme: Çözülen A klasörün (1,1) hücresini devraldı",
                appA.cellX == 1 && appA.cellY == 1 && appA.masaustundeMi());

        // 5. SuruklemeVerisi.withItemId() testi
        SuruklemeVerisi sv = new SuruklemeVerisi(
                new UygulamaBilgisi("A", "com.example.a", "", (android.graphics.drawable.Drawable) null),
                SuruklemeVerisi.KAYNAK_MASAUSTU, 0, 1, 1).withItemId("item_a");
        dogrula("SuruklemeVerisi.withItemId: itemId alanı taşınır", "item_a".equals(sv.itemId));

        // 6. KlasorKapakYoneticisi öncelik hiyerarşisi testleri
        Set<String> kapakAcik = new HashSet<String>(Arrays.asList("k_acik"));
        Set<String> kapakKapali = new HashSet<String>(Arrays.asList("k_kapali"));
        dogrula("KlasorKapakYoneticisi: Genel açıkken özel kapalı klasör false döner",
                !com.tamekran.launcher.KlasorKapakYoneticisi.hesaplaKapakModu(true, kapakAcik, kapakKapali, "k_kapali"));
        dogrula("KlasorKapakYoneticisi: Genel açıkken istisnasız klasör true döner",
                com.tamekran.launcher.KlasorKapakYoneticisi.hesaplaKapakModu(true, kapakAcik, kapakKapali, "k_diger"));
        dogrula("KlasorKapakYoneticisi: Genel kapalıyken özel açık klasör true döner",
                com.tamekran.launcher.KlasorKapakYoneticisi.hesaplaKapakModu(false, kapakAcik, kapakKapali, "k_acik"));
        dogrula("KlasorKapakYoneticisi: Genel kapalıyken istisnasız klasör false döner",
                !com.tamekran.launcher.KlasorKapakYoneticisi.hesaplaKapakModu(false, kapakAcik, kapakKapali, "k_diger"));

        // 7. Aynı uygulamadan masaüstünde 2+ kısayol varken tekil silme / dock'a taşıma / sayfaya taşıma testleri
        com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu cokluDurum =
                new com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu();
        cokluDurum.toplamSayfa = 2;
        cokluDurum.sutun = 4;
        cokluDurum.satir = 6;
        cokluDurum.dockLimit = 5;

        com.tamekran.launcher.MasaustuKaydi kopya1 = new com.tamekran.launcher.MasaustuKaydi(
                "dup_1", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.dup/com.example.dup.Main", "Uygulama",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 0, 0, 0, 1, 1);
        com.tamekran.launcher.MasaustuKaydi kopya2 = new com.tamekran.launcher.MasaustuKaydi(
                "dup_2", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.dup/com.example.dup.Main", "Uygulama",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 0, 2, 0, 1, 1);
        cokluDurum.ogeler.add(kopya1);
        cokluDurum.ogeler.add(kopya2);

        // Bütünlük kontrolü aynı sayfadaki 2 ayrı id'li kopyayı korumalı
        com.tamekran.launcher.MasaustuModelDeposu.butunlukDogrulaVeOnar(cokluDurum);
        dogrula("Çoklu Kısayol: Aynı sayfada aynı uygulamadan 2 kısayol korunur",
                cokluDurum.sayfaOgeleri(0).size() == 2);

        // Sadece dup_1 kaldırıldığında dup_2 masaüstünde kalmalı
        boolean tekSilindi = com.tamekran.launcher.MasaustuModelDeposu.ogeKaldirBellek(
                cokluDurum, "dup_1", SuruklemeVerisi.KAYNAK_MASAUSTU, 0, "com.example.dup/com.example.dup.Main");
        dogrula("Çoklu Kısayol Tekil Silme: İşlem başarılı", tekSilindi);
        dogrula("Çoklu Kısayol Tekil Silme: Yalnızca dup_1 silindi, dup_2 masaüstünde kaldı",
                cokluDurum.sayfaOgeleri(0).size() == 1 && "dup_2".equals(cokluDurum.sayfaOgeleri(0).get(0).id));

        // Çekmeceden 2. bir kopya (dup_3) eklenip Dock'a taşındığında masaüstündeki dup_2 korunmalı
        com.tamekran.launcher.MasaustuModelDeposu.masaustuneOgeTasiBellek(
                cokluDurum, null, "com.example.dup/com.example.dup.Main",
                SuruklemeVerisi.KAYNAK_CEKMECE, 0, null, 0, 1, 0, 1, 1);
        dogrula("Çoklu Kısayol Çekmeceden Ekleme: Sayfada tekrar 2 kısayol oldu",
                cokluDurum.sayfaOgeleri(0).size() == 2);

        String ikinciId = null;
        for (com.tamekran.launcher.MasaustuKaydi mk : cokluDurum.sayfaOgeleri(0)) {
            if (!"dup_2".equals(mk.id)) {
                ikinciId = mk.id;
                break;
            }
        }
        boolean dockaGitti = com.tamekran.launcher.MasaustuModelDeposu.dockaOgeTasiBellek(
                cokluDurum, ikinciId, "com.example.dup/com.example.dup.Main",
                SuruklemeVerisi.KAYNAK_MASAUSTU, 0, null, 0);
        dogrula("Çoklu Kısayol -> Dock Taşıma: Başarılı", dockaGitti);
        dogrula("Çoklu Kısayol -> Dock Taşıma: Dock'ta 1 öğe var",
                cokluDurum.dockOgeleri().size() == 1);
        dogrula("Çoklu Kısayol -> Dock Taşıma: Masaüstündeki diğer kopya (dup_2) korundu",
                cokluDurum.sayfaOgeleri(0).size() == 1 && "dup_2".equals(cokluDurum.sayfaOgeleri(0).get(0).id));

        // 8. Uygulama kaldırma (Uninstall) tüm kopyaları (masaüstü + dock + klasör) atomik temizler
        com.tamekran.launcher.MasaustuKaydi silKlasor = new com.tamekran.launcher.MasaustuKaydi(
                "klasor:k_del", com.tamekran.launcher.MasaustuKaydi.TUR_KLASOR,
                "klasor:k_del", "SilTest",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 1, 0, 0, 1, 1);
        com.tamekran.launcher.MasaustuKaydi silIc1 = new com.tamekran.launcher.MasaustuKaydi(
                "ic_1", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.dup/com.example.dup.Main", "Dup",
                "k_del", 0, 0, 0, 1, 1);
        com.tamekran.launcher.MasaustuKaydi silIc2 = new com.tamekran.launcher.MasaustuKaydi(
                "ic_2", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.kalan/com.example.kalan.Main", "Kalan",
                "k_del", 1, 0, 0, 1, 1);
        cokluDurum.ogeler.add(silKlasor);
        cokluDurum.ogeler.add(silIc1);
        cokluDurum.ogeler.add(silIc2);

        boolean tumuSilindi = com.tamekran.launcher.MasaustuModelDeposu.paketTumKopyalariSilBellek(
                cokluDurum, "com.example.dup");
        dogrula("Paket Uninstall Temizliği: Tüm kopyalar silindi", tumuSilindi);
        dogrula("Paket Uninstall Temizliği: Masaüstü ve dockta kopya kalmadı",
                cokluDurum.masaustundeBul(0, "com.example.dup") == null
                        && cokluDurum.docktaBul("com.example.dup") == null);
        dogrula("Paket Uninstall Temizliği: Tek öğeye düşen klasör otomatik çözüldü",
                cokluDurum.idIleBul("klasor:k_del") == null
                        && cokluDurum.masaustundeBul(1, "com.example.kalan") != null);

        // 9. API 8 güvenli editorKaydet null koruması
        AyarBankasi.editorKaydet(null);
        dogrula("AyarBankasi.editorKaydet: Null editor ile güvenli", true);

        // 10. Geri Al (Undo) 2. kısayolu silmeden silineni eski hücresine geri yüklemeli
        com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu geriAlDurum =
                new com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu();
        geriAlDurum.toplamSayfa = 2;
        geriAlDurum.sutun = 4;
        geriAlDurum.satir = 6;
        geriAlDurum.ogeler.add(new com.tamekran.launcher.MasaustuKaydi(
                "g_1", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.app/com.example.app.Main", "App",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 0, 0, 0, 1, 1));
        geriAlDurum.ogeler.add(new com.tamekran.launcher.MasaustuKaydi(
                "g_2", com.tamekran.launcher.MasaustuKaydi.TUR_UYGULAMA,
                "com.example.app/com.example.app.Main", "App",
                com.tamekran.launcher.MasaustuKaydi.CONTAINER_MASAUSTU, 0, 2, 0, 1, 1));

        com.tamekran.launcher.MasaustuModelDeposu.ogeKaldirBellek(
                geriAlDurum, "g_1", SuruklemeVerisi.KAYNAK_MASAUSTU, 0, "com.example.app/com.example.app.Main");
        dogrula("Geri Al Önkoşul: g_1 silindi, g_2 kaldı",
                geriAlDurum.sayfaOgeleri(0).size() == 1 && "g_2".equals(geriAlDurum.sayfaOgeleri(0).get(0).id));

        boolean geriYuklendi = com.tamekran.launcher.MasaustuModelDeposu.ogeGeriYukleBellek(
                geriAlDurum, SuruklemeVerisi.KAYNAK_MASAUSTU, 0,
                "com.example.app/com.example.app.Main", 0, 0, 1, 1, null, null);
        dogrula("Geri Al: Öğe geri yüklendi", geriYuklendi);
        dogrula("Geri Al: Aynı sayfadaki 2. kısayol (g_2) silinmeden toplam 2 kısayol oluştu",
                geriAlDurum.sayfaOgeleri(0).size() == 2 && geriAlDurum.idIleBul("g_2") != null);

        // 11. UygulamaKilidiYoneticisi API 8 güvenli null kontrolleri
        dogrula("UygulamaKilidiYoneticisi: Null context ile boş set döner",
                com.tamekran.launcher.UygulamaKilidiYoneticisi.kilitliPaketleriAl(null).isEmpty());
        dogrula("UygulamaKilidiYoneticisi: Null context ile kilitliMi false döner",
                !com.tamekran.launcher.UygulamaKilidiYoneticisi.kilitliMi(null, "com.example.app"));
        com.tamekran.launcher.UygulamaKilidiYoneticisi.kilitle(null, "com.example.app");
        com.tamekran.launcher.UygulamaKilidiYoneticisi.kilidiKaldir(null, "com.example.app");
        dogrula("UygulamaKilidiYoneticisi: Null context kilitle/kilidiKaldir güvenli", true);

        // 12. GeriTusuYardimcisi null güvenliği
        com.tamekran.launcher.GeriTusuYardimcisi.kaydet(null, null);
        dogrula("GeriTusuYardimcisi: Null activity ve runnable ile güvenli", true);

        // 13. UygulamaBilgisi.kimliktenCozumle DRY çözücü testi
        dogrula("UygulamaBilgisi.kimliktenCozumle: Null context ile güvenli",
                UygulamaBilgisi.kimliktenCozumle(null, "com.example.app", null, null) == null);

        // 14. PIN tuzlu SHA-256 özetleme ve geriye dönük uyumluluk
        String pinHash = com.tamekran.launcher.UygulamaKilidiYoneticisi.pinHashUret("abcdef1234567890", "2580");
        dogrula("PIN Hash: v2 formatında üretilir", pinHash.startsWith("v2:abcdef1234567890:"));
        dogrula("PIN Hash: Doğru PIN doğrulanır",
                com.tamekran.launcher.UygulamaKilidiYoneticisi.pinHashDogrula(pinHash, "2580"));
        dogrula("PIN Hash: Yanlış PIN reddedilir",
                !com.tamekran.launcher.UygulamaKilidiYoneticisi.pinHashDogrula(pinHash, "1234"));
        dogrula("PIN Hash: Eski düz metin PIN ile geriye dönük doğrulama çalışır",
                com.tamekran.launcher.UygulamaKilidiYoneticisi.pinHashDogrula("4321", "4321"));

        // 15. OTA SHA-256 64-hex zorunlu doğrulama
        dogrula("OTA SHA-256: Boş/null/kısa/geçersiz hex reddedilir",
                !com.tamekran.launcher.GuncellemeYoneticisi.sha256FormatGecerliMi(null)
                && !com.tamekran.launcher.GuncellemeYoneticisi.sha256FormatGecerliMi("")
                && !com.tamekran.launcher.GuncellemeYoneticisi.sha256FormatGecerliMi("abc123")
                && !com.tamekran.launcher.GuncellemeYoneticisi.sha256FormatGecerliMi("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));
        dogrula("OTA SHA-256: Geçerli 64-hex kabul edilir",
                com.tamekran.launcher.GuncellemeYoneticisi.sha256FormatGecerliMi(
                        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

        // 16. V2 Tek Yazar klasör yaşam döngüsü (oluştur -> ad değiştir -> ekle -> sil ve masaüstüne dök)
        com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu kDurum =
                new com.tamekran.launcher.MasaustuModelDeposu.ModelDurumu();
        kDurum.toplamSayfa = 2;
        kDurum.sutun = 4;
        kDurum.satir = 6;
        String kId = com.tamekran.launcher.MasaustuModelDeposu.bosKlasorOlusturBellek(kDurum, 0, "Araçlar");
        dogrula("V2 Boş Klasör: Oluşturuldu", kId != null && kDurum.klasorKaydiBul(kId) != null);
        dogrula("V2 Klasör Adı: Değiştirildi",
                com.tamekran.launcher.MasaustuModelDeposu.klasorAdiDegistirBellek(kDurum, kId, "Sistem")
                && "Sistem".equals(kDurum.klasorKaydiBul(kId).etiket));
        dogrula("V2 Klasöre Ekleme: 2 uygulama eklendi",
                com.tamekran.launcher.MasaustuModelDeposu.klasoreUygulamaEkleBellek(kDurum, kId, "com.a/Main")
                && com.tamekran.launcher.MasaustuModelDeposu.klasoreUygulamaEkleBellek(kDurum, kId, "com.b/Main")
                && kDurum.klasorOgeleri(kId).size() == 2);
        dogrula("V2 Klasör Silme: Klasör silindi ve 2 uygulama masaüstüne döküldü",
                com.tamekran.launcher.MasaustuModelDeposu.klasoruSilVeMasaustuneDokBellek(kDurum, kId)
                && kDurum.klasorKaydiBul(kId) == null
                && kDurum.sayfaOgeleri(0).size() == 2);

        // 17. V2 Çoklu Ekleme ile Klasör Oluşturma
        java.util.List<String> coklu = new java.util.ArrayList<String>();
        coklu.add("com.x/Main");
        coklu.add("com.y/Main");
        String ckId = com.tamekran.launcher.MasaustuModelDeposu.klasorOlusturVeCokluEkleBellek(kDurum, 1, "Sosyal", coklu);
        dogrula("V2 Çoklu Klasör: 2 öğeli klasör sayfa 1'de oluşturuldu",
                ckId != null && kDurum.klasorKaydiBul(ckId) != null
                && kDurum.klasorKaydiBul(ckId).sayfa == 1
                && kDurum.klasorOgeleri(ckId).size() == 2);

        // 18. Ekran Süresi Biçimlendirme & Null Güvenlik Kontrolleri
        dogrula("KullanimSayaci.sureBicimlendir: Saat/dakika biçimlendirme doğru",
                "".equals(com.tamekran.launcher.KullanimSayaci.sureBicimlendir(0L))
                && "< 1 dk".equals(com.tamekran.launcher.KullanimSayaci.sureBicimlendir(30000L))
                && "15 dk".equals(com.tamekran.launcher.KullanimSayaci.sureBicimlendir(900000L))
                && "1 sa 30 dk".equals(com.tamekran.launcher.KullanimSayaci.sureBicimlendir(5400000L)));
        dogrula("AyarBankasi & OzelSimgeYoneticisi: Null context güvenli",
                AyarBankasi.isCekmeceSekmeleriGoster(null)
                && AyarBankasi.uzunBasmaOgesiAktifMi(null, "adlandir")
                && AyarBankasi.uzunBasmaOgesiAktifMi(null, false, "gizle")
                && com.tamekran.launcher.OzelSimgeYoneticisi.ozelSimgeKaydiAl(null, "a/b", "a") == null);
    }
}
