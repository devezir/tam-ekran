package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * Tam Ekran - Tema ve Vurgu Rengi Motoru
 * Nova Launcher benzeri arayüz renk teması seçimi sağlar.
 */
public class TemaYoneticisi {

    public static final String PREF_VURGU_RENGI = "tema_vurgu_rengi";

    public static final int RENK_KIRMIZI = Color.parseColor("#E53935");
    public static final int RENK_MAVI = Color.parseColor("#1976D2");
    public static final int RENK_YESIL = Color.parseColor("#388E3C");
    public static final int RENK_MOR = Color.parseColor("#7B1FA2");
    public static final int RENK_SARI = Color.parseColor("#FBC02D");
    public static final int RENK_PEMBE = Color.parseColor("#C2185B");
    public static final int RENK_KARBON = Color.parseColor("#78909C");

    public static final String[] RENK_ISIMLERI = new String[]{
        "Mercan Kırmızısı (Varsayılan)",
        "Okyanus Mavisi",
        "Zümrüt Yeşili",
        "Asil Mor",
        "Altın Sarısı",
        "Fuşya Pembe",
        "Karbon / Gri"
    };

    public static final int[] RENK_DEGERLERI = new int[]{
        RENK_KIRMIZI,
        RENK_MAVI,
        RENK_YESIL,
        RENK_MOR,
        RENK_SARI,
        RENK_PEMBE,
        RENK_KARBON
    };

    public static int getVurguRengi(Context context) {
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_VURGU_RENGI, RENK_KIRMIZI);
    }

    public static void setVurguRengi(Context context, int renk) {
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_VURGU_RENGI, renk));
    }
}

