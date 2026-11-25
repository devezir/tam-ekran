package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Tam Ekran - Simge Yazı Stili ve Metin Gölgesi Yöneticisi
 * Masaüstü, çekmece ve dock simgelerinin altındaki yazıların stilini, kalınlığını ve gölgesini
 * Nova Launcher benzeri kişiselleştirir.
 */
public class YaziStiliYoneticisi {

    public static final String PREF_YAZI_STILI = "simge_yazi_stili";
    public static final String PREF_METIN_GOLGESI = "simge_metin_golgesi";
    public static final String PREF_YAZI_BOYUTU = "simge_yazi_boyutu";

    public static final int STIL_NORMAL = 0;
    public static final int STIL_KALIN = 1;
    public static final int STIL_ITALIK = 2;
    public static final int STIL_KALIN_ITALIK = 3;

    public static final String[] STIL_ISIMLERI = new String[]{
        "Normal (Standart)",
        "Kalın (Bold)",
        "İtalik (Eğik)",
        "Kalın İtalik (Bold Italic)"
    };

    public static final int BOYUT_KUCUK = 10;
    public static final int BOYUT_STANDART = 12;
    public static final int BOYUT_BUYUK = 14;
    public static final int BOYUT_COK_BUYUK = 16;

    public static final int[] BOYUT_DEGERLERI = new int[]{10, 12, 14, 16};
    public static final String[] BOYUT_ISIMLERI = new String[]{
        "Küçük (10sp)",
        "Standart (12sp)",
        "Büyük (14sp)",
        "Çok Büyük (16sp)"
    };

    public static int getYaziBoyutu(Context context) {
        if (context == null) return BOYUT_STANDART;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_YAZI_BOYUTU, BOYUT_STANDART);
    }

    public static void setYaziBoyutu(Context context, int boyutSp) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_YAZI_BOYUTU, boyutSp));
    }

    public static int getYaziStili(Context context) {
        if (context == null) return STIL_NORMAL;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_YAZI_STILI, STIL_NORMAL);
    }

    public static void setYaziStili(Context context, int stil) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_YAZI_STILI, stil));
    }

    public static boolean getMetinGolgesi(Context context) {
        if (context == null) return true;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getBoolean(PREF_METIN_GOLGESI, true);
    }

    public static boolean isMetinGolgesi(Context context) {
        return getMetinGolgesi(context);
    }

    public static void setMetinGolgesi(Context context, boolean acik) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putBoolean(PREF_METIN_GOLGESI, acik));
    }

    public static void stiliUygula(TextView tv, Context context) {
        if (tv == null || context == null) return;

        int stil = getYaziStili(context);
        int tfStyle = Typeface.NORMAL;
        switch (stil) {
            case STIL_KALIN: tfStyle = Typeface.BOLD; break;
            case STIL_ITALIK: tfStyle = Typeface.ITALIC; break;
            case STIL_KALIN_ITALIK: tfStyle = Typeface.BOLD_ITALIC; break;
            case STIL_NORMAL: default: tfStyle = Typeface.NORMAL; break;
        }
        tv.setTypeface(null, tfStyle);

        int boyut = getYaziBoyutu(context);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, boyut);

        boolean golge = getMetinGolgesi(context);
        if (golge) {
            tv.setShadowLayer(3.0f, 1.0f, 1.0f, Color.parseColor("#CC000000"));
        } else {
            tv.setShadowLayer(0, 0, 0, 0);
        }
    }
}

