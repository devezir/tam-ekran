package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tam Ekran - Merkezi Ayar Bankası (Single Source of Truth)
 * Projedeki tüm SharedPreferences erişimlerini, anahtarlarını ve
 * sık kullanılan ayar sorgularını hiçbir yöneticiye veya Activity'ye
 * bağımlı kalmadan tek bir merkezde toplar (DRY & Temiz Mimari).
 */
public final class AyarBankasi {

    private AyarBankasi() {}

    public static final String AYARLAR_PREF = "tamekran_ayarlar";
    public static final String PREF_OZEL_ETIKET_DOSYA = "tamekran_ozel_etiketler";
    public static final String PREF_SAYAC_DOSYA = "tamekran_kullanim_sayaci";
    public static final String PREF_KILIT_DOSYA = "tamekran_uygulama_kilidi";

    // Anahtarlar (Kanonsal SSOT Tanımları — Bağımlılıksız Yaprak Katman)
    public static final String PREF_MASAUSTU_KILITLI = "masaustu_kilitli";
    public static final String PREF_SIMGE_BOYUTU = "simge_boyutu_olcek";
    public static final String PREF_MASAUSTU_SIMGE_BOYUTU = "masaustu_simge_boyutu_olcek";
    public static final String PREF_CEKMECE_SIMGE_BOYUTU = "cekmece_simge_boyutu_olcek";
    public static final String PREF_DOCK_SIMGE_BOYUTU = "dock_simge_boyutu_olcek";
    public static final String PREF_DOCK_SIMGE_SAYISI = "dock_simge_sayisi";
    public static final String PREF_ETIKETLERI_GOSTER = "etiketleri_goster";
    public static final String PREF_DOCK_GORUNUR = "dock_gorunur";
    public static final String PREF_DOCK_YUKSEKLIK = "dock_yuksekligi";
    public static final String PREF_DOCK_KAYDIRILABILIR = "dock_kaydirilabilir";
    public static final String PREF_CEKMECE_SAYDAMLIK = "cekmece_saydamlik";
    public static final String PREF_DUVAR_KAGIDI_KAYDIRMA = "duvar_kagidi_kaydirma";
    public static final String PREF_GECIS_EFEKTI = "masaustu_gecis_efekti";
    public static final String PREF_GIZLENENLER_CSV = "gizlenen_uygulamalar_csv";
    public static final String PREF_MASAUSTU_DOLGU = "masaustu_kenar_dolgusu";
    public static final String PREF_TAM_EKRAN_MODU = "tam_ekran_modu";
    public static final String PREF_SIK_KULLANILANLAR_MODU = "sik_kullanilanlar_modu";
    public static final String PREF_MASAUSTU_MODU = "masaustu_modu";
    public static final String PREF_DEGISTIRME_DAMGASI = "son_degisiklik_damgasi";
    public static final int MOD_KLASIK_CIFTE_KATMAN = 0;
    public static final int MOD_TEK_KATMAN_SERP = 1;

    public static final String ANAHTAR_IZGARA_SUTUN = "izgara_sutun";
    public static final String ANAHTAR_IZGARA_SATIR = "izgara_satir";
    public static final String ANAHTAR_CEKMECE_SUTUN = "cekmece_izgara_sutun";
    public static final String ANAHTAR_CEKMECE_SATIR = "cekmece_izgara_satir";

    public interface TestDepoSaglayici {
        SharedPreferences getAyarlar(String dosyaAdi);
        java.io.File getDosyalarDizini();
    }

    private static TestDepoSaglayici testSaglayici;

    public static void setTestSaglayici(TestDepoSaglayici saglayici) {
        testSaglayici = saglayici;
    }

    public static boolean baglamGecerliMi(Context context) {
        return context != null || testSaglayici != null;
    }

    public static java.io.File getDosyalarDizini(Context context) {
        if (testSaglayici != null) return testSaglayici.getDosyalarDizini();
        if (context == null) return null;
        java.io.File dizin = context.getExternalFilesDir(null);
        return (dizin != null) ? dizin : context.getFilesDir();
    }

    /**
     * Ortak SharedPreferences referansını döndürür.
     */
    public static SharedPreferences getAyarlar(Context context) {
        if (testSaglayici != null) return testSaglayici.getAyarlar(AYARLAR_PREF);
        if (context == null) return null;
        return context.getSharedPreferences(AYARLAR_PREF, Context.MODE_PRIVATE);
    }

    /**
     * Belirtilen isimde ayrı SharedPreferences dosyası döndürür.
     */
    public static SharedPreferences getOzelAyarlar(Context context, String dosyaAdi) {
        if (dosyaAdi == null) return null;
        if (testSaglayici != null) return testSaglayici.getAyarlar(dosyaAdi);
        if (context == null) return null;
        return context.getSharedPreferences(dosyaAdi, Context.MODE_PRIVATE);
    }

    public static long getDegisiklikDamgasi(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null ? sp.getLong(PREF_DEGISTIRME_DAMGASI, 0L) : 0L;
    }

    public static void editorKaydet(SharedPreferences.Editor editor) {
        if (editor == null) return;
        if (android.os.Build.VERSION.SDK_INT >= 9) {
            editor.apply();
        } else {
            editor.commit();
        }
    }

    public static void degisiklikDamgasiGuncelle(Context context) {
        SharedPreferences sp = getAyarlar(context);
        if (sp != null) {
            editorKaydet(sp.edit().putLong(PREF_DEGISTIRME_DAMGASI, System.currentTimeMillis()));
        }
    }

    /**
     * Masaüstünün kilitli olup olmadığını döndürür.
     */
    public static boolean isMasaustuKilitli(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null && sp.getBoolean(PREF_MASAUSTU_KILITLI, false);
    }

    /**
     * Masaüstü kilit durumunu değiştirir.
     */
    public static void setMasaustuKilitli(Context context, boolean kilitli) {
        SharedPreferences sp = getAyarlar(context);
        if (sp != null) {
            sp.edit().putBoolean(PREF_MASAUSTU_KILITLI, kilitli).commit();
        }
    }

    /**
     * Genel simge boyutunu yüzde ölçek cinsinden döndürür (varsayılan %100).
     */
    public static int getSimgeBoyutu(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null ? sp.getInt(PREF_SIMGE_BOYUTU, 100) : 100;
    }

    public static void setSimgeBoyutu(Context context, int olcek) {
        putInt(context, PREF_SIMGE_BOYUTU, olcek);
    }

    /**
     * Masaüstü simge boyutunu yüzde ölçek cinsinden döndürür (varsayılan genel simge boyutu veya %100).
     */
    public static int getMasaustuSimgeBoyutu(Context context) {
        return getInt(context, PREF_MASAUSTU_SIMGE_BOYUTU, getSimgeBoyutu(context));
    }

    public static void setMasaustuSimgeBoyutu(Context context, int olcek) {
        putInt(context, PREF_MASAUSTU_SIMGE_BOYUTU, olcek);
    }

    /**
     * Çekmece simge boyutunu yüzde ölçek cinsinden döndürür (varsayılan genel simge boyutu veya %100).
     */
    public static int getCekmeceSimgeBoyutu(Context context) {
        return getInt(context, PREF_CEKMECE_SIMGE_BOYUTU, getSimgeBoyutu(context));
    }

    public static void setCekmeceSimgeBoyutu(Context context, int olcek) {
        putInt(context, PREF_CEKMECE_SIMGE_BOYUTU, olcek);
    }

    /**
     * Alt bar (Dock) simge boyutunu yüzde ölçek cinsinden döndürür (varsayılan genel simge boyutu veya %100).
     */
    public static int getDockSimgeBoyutu(Context context) {
        return getInt(context, PREF_DOCK_SIMGE_BOYUTU, getSimgeBoyutu(context));
    }

    public static void setDockSimgeBoyutu(Context context, int olcek) {
        putInt(context, PREF_DOCK_SIMGE_BOYUTU, olcek);
    }

    /**
     * Dock'taki simge yuva sayısını döndürür (varsayılan 4).
     */
    public static int getDockSimgeSayisi(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null ? sp.getInt(PREF_DOCK_SIMGE_SAYISI, 4) : 4;
    }

    /**
     * Dock alanının görünür olup olmadığını döndürür (varsayılan true).
     */
    public static boolean isDockGorunur(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null && sp.getBoolean(PREF_DOCK_GORUNUR, true);
    }

    /**
     * Alt bar (Dock) yüksekliğini dp olarak döndürür (varsayılan 72 dp).
     */
    public static int getDockYuksekligi(Context context) {
        return getInt(context, PREF_DOCK_YUKSEKLIK, 72);
    }

    public static void setDockYuksekligi(Context context, int yukseklikDp) {
        putInt(context, PREF_DOCK_YUKSEKLIK, yukseklikDp);
    }

    /**
     * Alt barın yatay kaydırılabilir olup olmadığını döndürür (varsayılan false).
     */
    public static boolean isDockKaydirilabilir(Context context) {
        return getBoolean(context, PREF_DOCK_KAYDIRILABILIR, false);
    }

    public static void setDockKaydirilabilir(Context context, boolean kaydirilabilir) {
        putBoolean(context, PREF_DOCK_KAYDIRILABILIR, kaydirilabilir);
    }

    /**
     * Uygulama etiketlerinin gösterilip gösterilmeyeceğini döndürür.
     */
    public static boolean isEtiketlerGoster(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null && sp.getBoolean(PREF_ETIKETLERI_GOSTER, true);
    }

    /**
     * Masaüstü sütun sayısı (varsayılan 4, sınır 3-12).
     */
    public static int getMasaustuSutun(Context context) {
        SharedPreferences sp = getAyarlar(context);
        int deger = sp != null ? sp.getInt(ANAHTAR_IZGARA_SUTUN, 4) : 4;
        return Math.max(3, Math.min(12, deger));
    }

    /**
     * Masaüstü satır sayısı (varsayılan 6, sınır 3-12).
     */
    public static int getMasaustuSatir(Context context) {
        SharedPreferences sp = getAyarlar(context);
        int deger = sp != null ? sp.getInt(ANAHTAR_IZGARA_SATIR, 6) : 6;
        return Math.max(3, Math.min(12, deger));
    }

    /**
     * Çekmece sütun sayısı (varsayılan masaüstü sütun sayısı).
     */
    public static int getCekmeceSutun(Context context) {
        SharedPreferences sp = getAyarlar(context);
        int deger = sp != null ? sp.getInt(ANAHTAR_CEKMECE_SUTUN, getMasaustuSutun(context)) : 4;
        return Math.max(3, Math.min(12, deger));
    }

    /**
     * Çekmece satır sayısı (varsayılan masaüstü satır sayısı).
     */
    public static int getCekmeceSatir(Context context) {
        SharedPreferences sp = getAyarlar(context);
        int deger = sp != null ? sp.getInt(ANAHTAR_CEKMECE_SATIR, getMasaustuSatir(context)) : 6;
        return Math.max(3, Math.min(12, deger));
    }

    /**
     * Çekmece arka plan saydamlık yüzdesi (varsayılan 95).
     */
    public static int getCekmeceSaydamlik(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null ? sp.getInt(PREF_CEKMECE_SAYDAMLIK, 95) : 95;
    }

    /**
     * Duvar kâğıdı kaydırma (wallpaper parallax) açık mı (varsayılan true).
     */
    public static boolean isDuvarKagidiKaydirma(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null && sp.getBoolean(PREF_DUVAR_KAGIDI_KAYDIRMA, true);
    }

    /**
     * Gerçek Tam Ekran modunun (durum çubuğunu gizleme) açık olup olmadığını döndürür.
     */
    public static boolean isTamEkranModu(Context context) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null && sp.getBoolean(PREF_TAM_EKRAN_MODU, false);
    }

    /**
     * Masaüstü düzeni modu (Klasik Çekmeceli vs Tek Katman iOS Modu).
     */
    public static int getMasaustuModu(Context context) {
        return getInt(context, PREF_MASAUSTU_MODU, MOD_KLASIK_CIFTE_KATMAN);
    }

    public static boolean isTekKatmanModu(Context context) {
        return getMasaustuModu(context) == MOD_TEK_KATMAN_SERP;
    }

    public static void setMasaustuModu(Context context, int mod) {
        putInt(context, PREF_MASAUSTU_MODU, mod);
    }

    // =========================================================================
    // GENEL TİPLİ AYAR ERİŞİM VE KAYIT METOTLARI (DRY DATA ACCESS LAYER)
    // =========================================================================

    public static boolean getBoolean(Context context, String key, boolean def) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null ? sp.getBoolean(key, def) : def;
    }

    public static void putBoolean(Context context, String key, boolean val) {
        SharedPreferences sp = getAyarlar(context);
        if (sp != null) {
            editorKaydet(sp.edit().putBoolean(key, val));
        }
    }

    public static int getInt(Context context, String key, int def) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null ? sp.getInt(key, def) : def;
    }

    public static void putInt(Context context, String key, int val) {
        SharedPreferences sp = getAyarlar(context);
        if (sp != null) {
            editorKaydet(sp.edit().putInt(key, val));
        }
    }

    public static String getString(Context context, String key, String def) {
        SharedPreferences sp = getAyarlar(context);
        return sp != null ? sp.getString(key, def) : def;
    }

    public static void putString(Context context, String key, String val) {
        SharedPreferences sp = getAyarlar(context);
        if (sp != null) {
            editorKaydet(sp.edit().putString(key, val));
        }
    }

    public static java.util.List<String> getStringListesi(Context context, String key) {
        String hamJson = getString(context, key, null);
        return JsonYardimcisi.jsonToStringListesi(hamJson);
    }

    public static void putStringListesi(Context context, String key, java.util.List<String> liste) {
        String json = JsonYardimcisi.stringListesiToJson(liste);
        putString(context, key, json);
    }

    public static void remove(Context context, String key) {
        SharedPreferences sp = getAyarlar(context);
        if (sp != null) {
            editorKaydet(sp.edit().remove(key));
        }
    }
}
