package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tam Ekran - Uygulama Çekmecesi Sıralama Motoru
 * Uygulamaları A-Z, Z-A, En Çok Açılanlar ve En Yeni Yüklenenler şeklinde sıralar.
 */
public class CekmeceSiralamasi {

    public static final String PREF_SIRALAMA = "cekmece_siralama_turu";

    public static final int SIRALAMA_A_TO_Z = 0;
    public static final int SIRALAMA_Z_TO_A = 1;
    public static final int SIRALAMA_EN_COK_KULLANILAN = 2;
    public static final int SIRALAMA_EN_YENI_YUKLENEN = 3;

    public static final String[] SIRALAMA_ISIMLERI = new String[]{
        "🔤 A'dan Z'ye (Alfabetik)",
        "🔠 Z'den A'ya (Ters Alfabetik)",
        "🔥 En Çok Kullanılanlar (Sıklık)",
        "✨ En Yeni Yüklenenler / Güncellenenler"
    };

    public static int getSiralamaTuru(Context context) {
        if (context == null) return SIRALAMA_A_TO_Z;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_SIRALAMA, SIRALAMA_A_TO_Z);
    }

    public static void setSiralamaTuru(Context context, int tur) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_SIRALAMA, tur));
    }

    public static void sirala(final Context context, List<UygulamaBilgisi> liste) {
        if (context == null || liste == null || liste.size() <= 1) return;

        int tur = getSiralamaTuru(context);
        final Collator trCollator = Collator.getInstance(new Locale("tr", "TR"));

        if (tur == SIRALAMA_A_TO_Z) {
            Collections.sort(liste, new Comparator<UygulamaBilgisi>() {
                @Override
                public int compare(UygulamaBilgisi o1, UygulamaBilgisi o2) {
                    if (o1 == null || o1.etiket == null) return 1;
                    if (o2 == null || o2.etiket == null) return -1;
                    return trCollator.compare(o1.etiket, o2.etiket);
                }
            });
        } else if (tur == SIRALAMA_Z_TO_A) {
            Collections.sort(liste, new Comparator<UygulamaBilgisi>() {
                @Override
                public int compare(UygulamaBilgisi o1, UygulamaBilgisi o2) {
                    if (o1 == null || o1.etiket == null) return 1;
                    if (o2 == null || o2.etiket == null) return -1;
                    return trCollator.compare(o2.etiket, o1.etiket);
                }
            });
        } else if (tur == SIRALAMA_EN_COK_KULLANILAN) {
            Collections.sort(liste, new Comparator<UygulamaBilgisi>() {
                @Override
                public int compare(UygulamaBilgisi o1, UygulamaBilgisi o2) {
                    int c1 = KullanimSayaci.getKullanimSayisi(context, o1.paketAdi);
                    int c2 = KullanimSayaci.getKullanimSayisi(context, o2.paketAdi);
                    if (c1 != c2) return c2 - c1;
                    return trCollator.compare(o1.etiket, o2.etiket);
                }
            });
        } else if (tur == SIRALAMA_EN_YENI_YUKLENEN) {
            PackageManager pm = context.getPackageManager();
            final Map<String, Long> zamanHaritasi = new HashMap<String, Long>();
            for (UygulamaBilgisi app : liste) {
                try {
                    PackageInfo pi = pm.getPackageInfo(app.paketAdi, 0);
                    zamanHaritasi.put(app.paketAdi, Math.max(pi.firstInstallTime, pi.lastUpdateTime));
                } catch (Throwable t) {
                    LauncherLog.d("CekmeceSiralamasi", "PackageInfo firstInstallTime alinamadi: " + app.paketAdi);
                    zamanHaritasi.put(app.paketAdi, 0L);
                }
            }

            Collections.sort(liste, new Comparator<UygulamaBilgisi>() {
                @Override
                public int compare(UygulamaBilgisi o1, UygulamaBilgisi o2) {
                    Long t1 = zamanHaritasi.get(o1.paketAdi);
                    Long t2 = zamanHaritasi.get(o2.paketAdi);
                    long z1 = t1 != null ? t1 : 0L;
                    long z2 = t2 != null ? t2 : 0L;
                    if (z1 != z2) {
                        return z2 > z1 ? 1 : -1;
                    }
                    return trCollator.compare(o1.etiket, o2.etiket);
                }
            });
        }
    }
}

