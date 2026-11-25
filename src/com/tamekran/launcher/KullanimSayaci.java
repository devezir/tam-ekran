package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tam Ekran - Sık Kullanılan Uygulamalar Sayacı
 * Kullanıcının en çok açtığı uygulamaları yerel olarak takip eder.
 * %100 çevrimdışı, sıfır telemetri.
 */
public class KullanimSayaci {

    public static void uygulamayiBaslatti(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_SAYAC_DOSYA);
        if (sp == null) return;
        int sayi = sp.getInt(paketAdi, 0);
        AyarBankasi.editorKaydet(sp.edit().putInt(paketAdi, sayi + 1));
    }

    public static int getKullanimSayisi(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return 0;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_SAYAC_DOSYA);
        return sp != null ? sp.getInt(paketAdi, 0) : 0;
    }

    public static List<UygulamaBilgisi> sikKullanilanlariSec(final Context context, List<UygulamaBilgisi> tumu, int adet) {
        List<UygulamaBilgisi> kopya = new ArrayList<UygulamaBilgisi>(tumu);
        Collections.sort(kopya, new Comparator<UygulamaBilgisi>() {
            @Override
            public int compare(UygulamaBilgisi o1, UygulamaBilgisi o2) {
                int c1 = getKullanimSayisi(context, o1.paketAdi);
                int c2 = getKullanimSayisi(context, o2.paketAdi);
                return c2 - c1; // En çok kullanılan en başa
            }
        });

        List<UygulamaBilgisi> sonuc = new ArrayList<UygulamaBilgisi>();
        for (UygulamaBilgisi app : kopya) {
            if (getKullanimSayisi(context, app.paketAdi) > 0) {
                sonuc.add(app);
                if (sonuc.size() >= adet) break;
            }
        }
        return sonuc;
    }
}
