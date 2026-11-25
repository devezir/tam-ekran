package tests;

import com.tamekran.launcher.AyarBankasi;
import com.tamekran.launcher.DockYoneticisi;
import com.tamekran.launcher.KlasorKapakYoneticisi;
import com.tamekran.launcher.MasaustuModelDeposu;
import com.tamekran.launcher.UygulamaKilidiYoneticisi;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * SahteBaglam (AyarBankasi.TestDepoSaglayici) kullanarak SharedPreferences bağımlı
 * Dock, Klasör Kapak Modu, Uygulama Kilidi ve AyarBankasi kalıcılık akışlarını JVM'de doğrular.
 */
public final class KalicilikVeGocTestleri {

    public interface Dogrulayici {
        void dogrula(String testAdi, boolean kosul);
    }

    private KalicilikVeGocTestleri() {}

    public static void calistir(Dogrulayici d) {
        System.out.println("\n[21/21] SahteBaglam ile Kalıcılık ve Depo Testleri:");
        File geciciKlasor = new File(System.getProperty("java.io.tmpdir"), "tamekran_test_" + System.currentTimeMillis());
        SahteBaglam sahte = new SahteBaglam(geciciKlasor);
        AyarBankasi.setTestSaglayici(sahte);
        try {
            // 1. DockYoneticisi kalıcı kayıt, ekleme ve silme
            DockYoneticisi.dockKaydet(null, Arrays.asList("com.app.bir/Main", "com.app.iki/Main"));
            List<String> yuklenenDock = DockYoneticisi.dockYukle(null);
            d.dogrula("Kalıcılık Dock: 2 öğe kaydedilip geri okundu", yuklenenDock.size() == 2);
            DockYoneticisi.docktanKaldir(null, "com.app.bir/Main");
            d.dogrula("Kalıcılık Dock: Kaldırma sonrası 1 öğe kaldı", DockYoneticisi.dockYukle(null).size() == 1);

            // 2. KlasorKapakYoneticisi kalıcı istisna önceliği
            KlasorKapakYoneticisi.setGenelKapakModu(null, true);
            KlasorKapakYoneticisi.setKlasorKapakModu(null, "k_99", false);
            d.dogrula("Kalıcılık Kapak Modu: Genel açıkken klasöre özel kapatma kalıcı olur",
                    !KlasorKapakYoneticisi.isKlasorKapakModu(null, "k_99"));
            d.dogrula("Kalıcılık Kapak Modu: İstisnasız klasör genel ayarı izler",
                    KlasorKapakYoneticisi.isKlasorKapakModu(null, "k_100"));

            // 3. V2 model önbelleği sıfırlama (v2ModeliniSifirla)
            AyarBankasi.putString(null, MasaustuModelDeposu.PREF_MODEL_V2_KLASIK, "[{\"pkg\":\"x\"}]");
            MasaustuModelDeposu.v2ModeliniSifirla(null);
            d.dogrula("Kalıcılık V2 Sıfırlama: PREF_MODEL_V2_KLASIK temizlendi",
                    AyarBankasi.getString(null, MasaustuModelDeposu.PREF_MODEL_V2_KLASIK, null) == null);

            // 4. UygulamaKilidiYoneticisi kalıcı JSON kaydı
            UygulamaKilidiYoneticisi.pinBelirle(null, "1234");
            d.dogrula("UygulamaKilidiYoneticisi: PIN ayarlandı ve doğrulandı",
                    UygulamaKilidiYoneticisi.pinAyarliMi(null) && UygulamaKilidiYoneticisi.pinDogrula(null, "1234"));
            UygulamaKilidiYoneticisi.kilitle(null, "com.gizli.app");
            d.dogrula("UygulamaKilidiYoneticisi: Kilitlenen paket kalıcı olarak kilitli görünür",
                    UygulamaKilidiYoneticisi.kilitliMi(null, "com.gizli.app"));
            UygulamaKilidiYoneticisi.kilidiKaldir(null, "com.gizli.app");
            d.dogrula("UygulamaKilidiYoneticisi: Kilidi kaldırılan paket açılır",
                    !UygulamaKilidiYoneticisi.kilitliMi(null, "com.gizli.app"));
        } finally {
            AyarBankasi.setTestSaglayici(null);
            diziniSil(geciciKlasor);
        }
    }

    private static void diziniSil(File f) {
        if (f == null || !f.exists()) return;
        File[] cocuklar = f.listFiles();
        if (cocuklar != null) {
            for (File c : cocuklar) {
                diziniSil(c);
            }
        }
        f.delete();
    }
}
