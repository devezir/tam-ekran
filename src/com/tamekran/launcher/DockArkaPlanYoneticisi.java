package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Tam Ekran - Sabit Alt Çubuk (Dock) Arka Plan Stili Yöneticisi
 * Dock'ın görünümünü Şeffaf, Kayan Yuvarlak Hap, Standart Koyu veya Çizgili Platform
 * olarak Nova Launcher tarzında özelleştirir.
 */
public class DockArkaPlanYoneticisi {

    public static final String PREF_DOCK_STILI = "dock_arka_plan_stili";

    public static final int STIL_SEFFAF = 0;
    public static final int STIL_DIKDORTGEN = 1;
    public static final int STIL_KAYAN_HAP = 2;
    public static final int STIL_UST_CIZGI = 3;

    public static final String[] STIL_ISIMLERI = new String[]{
        "Minimalist (Tamamen Şeffaf / Görünmez)",
        "Standart Koyu (Dikdörtgen Plaka)",
        "Kayan Yuvarlak Hap (Floating Pill Dock)",
        "Üst Çizgili Modern Platform"
    };

    public static int getDockStili(Context context) {
        if (context == null) return STIL_KAYAN_HAP;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_DOCK_STILI, STIL_KAYAN_HAP);
    }

    public static int getStil(Context context) {
        return getDockStili(context);
    }

    public static void setDockStili(Context context, int stil) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_DOCK_STILI, stil));
    }

    public static void setStil(Context context, int stil) {
        setDockStili(context, stil);
    }

    private static final int RENK_DIKDORTGEN = 0x661A1A1A;
    private static final int RENK_KAYAN_HAP = 0x77212121;
    private static final int RENK_HAP_STROKE = 0x33FFFFFF;
    private static final int RENK_UST_CIZGI = 0x77121212;
    private static final int RENK_CIZGI_STROKE = 0x44FFFFFF;

    public static void stiliUygula(View dockView, Context context) {
        if (dockView == null || context == null) return;
        stiliUygula(dockView, context, getDockStili(context));
    }

    public static void stiliUygula(View dockView, Context context, int stil) {
        if (dockView == null || context == null) return;

        int pad12 = ArayuzYardimcisi.dpToPx(context, 12);
        int pad6 = ArayuzYardimcisi.dpToPx(context, 6);
        int pad16 = ArayuzYardimcisi.dpToPx(context, 16);

        if (stil == STIL_SEFFAF) {
            dockView.setBackgroundColor(Color.TRANSPARENT);
            dockView.setPadding(pad12, 0, pad12, pad6);
        } else if (stil == STIL_DIKDORTGEN) {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(RENK_DIKDORTGEN);
            dockView.setBackgroundDrawable(gd);
            dockView.setPadding(pad12, 0, pad12, pad6);
        } else if (stil == STIL_KAYAN_HAP) {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(RENK_KAYAN_HAP);
            gd.setCornerRadius(ArayuzYardimcisi.dpToPx(context, 26));
            gd.setStroke(ArayuzYardimcisi.dpToPx(context, 1.5f), RENK_HAP_STROKE);
            dockView.setBackgroundDrawable(gd);
            dockView.setPadding(pad16, 0, pad16, pad6);
        } else if (stil == STIL_UST_CIZGI) {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(RENK_UST_CIZGI);
            gd.setStroke(ArayuzYardimcisi.dpToPx(context, 1), RENK_CIZGI_STROKE);
            dockView.setBackgroundDrawable(gd);
            dockView.setPadding(pad12, 0, pad12, pad6);
        }
    }
}

