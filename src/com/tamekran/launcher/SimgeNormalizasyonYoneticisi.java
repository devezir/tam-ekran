package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * Tam Ekran - Simge Boyut ve Şekil Normalizasyon Yöneticisi (Nova Icon Normalization)
 * Farklı boyutlardaki eski ve uyumsuz uygulama simgelerini standart tek tip geometrik tabana
 * (Squircle, Daire veya Yuvarlak Kare) yerleştirerek görsel simetri sağlar.
 */
public class SimgeNormalizasyonYoneticisi {

    public static final String PREF_SIMGE_NORMALIZASYONU = "simge_normalizasyonu";
    public static final String PREF_SIMGE_SEKLI = "simge_adaptif_sekli";

    public static final int SEKIL_ORIJINAL = 0;
    public static final int SEKIL_SQUIRCLE = 1;
    public static final int SEKIL_DAIRE = 2;
    public static final int SEKIL_KARE = 3;

    public static final String[] SEKIL_ISIMLERI = new String[]{
        "Orijinal (Serbest Şekil)",
        "Squircle (Nova Yuvarlatılmış)",
        "Daire (Yuvarlak)",
        "Yuvarlatılmış Kare"
    };

    // 120 FPS Kaydırma Akıcılığı ve Sıfır Bellek Tahsisi için Bounded LRU Önbellek
    private static final int MAX_ONBELLEK = 150;
    private static final java.util.Map<Integer, Drawable> onbellek = new java.util.LinkedHashMap<Integer, Drawable>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Drawable> eldest) {
            return size() > MAX_ONBELLEK;
        }
    };

    public static synchronized void onbellegiTemizle() {
        onbellek.clear();
    }

    public static boolean isNormalizasyonAktif(Context context) {
        if (context == null) return false;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getBoolean(PREF_SIMGE_NORMALIZASYONU, false);
    }

    public static void setNormalizasyonAktif(Context context, boolean aktif) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putBoolean(PREF_SIMGE_NORMALIZASYONU, aktif));
        onbellegiTemizle();
    }

    public static int getSimgeSekli(Context context) {
        if (context == null) return SEKIL_ORIJINAL;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_SIMGE_SEKLI, SEKIL_ORIJINAL);
    }

    public static void setSimgeSekli(Context context, int sekil) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_SIMGE_SEKLI, sekil));
        onbellegiTemizle();
    }

    /**
     * Verilen simgeyi normalizasyon ve şekil ayarlarına göre düzenler.
     * LRU önbellek sayesinde kaydırma sırasında sıfır Bitmap tahsisi yapar.
     */
    public static Drawable normalizeEt(Context context, Drawable orijinal) {
        if (context == null || orijinal == null) return orijinal;

        boolean norm = isNormalizasyonAktif(context);
        int sekil = getSimgeSekli(context);

        if (!norm && sekil == SEKIL_ORIJINAL) {
            return orijinal;
        }

        int anahtar = System.identityHashCode(orijinal) ^ (sekil << 8) ^ (norm ? 1 : 0);
        synchronized (onbellek) {
            Drawable onbellekteki = onbellek.get(anahtar);
            if (onbellekteki != null) {
                return onbellekteki;
            }
        }

        try {
            int boyut = ArayuzYardimcisi.dpToPx(context, 56);
            if (boyut <= 0) boyut = 96;

            Bitmap cikti = Bitmap.createBitmap(boyut, boyut, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(cikti);

            float r = boyut / 2.0f;
            Paint boya = new Paint(Paint.ANTI_ALIAS_FLAG);

            // Şekil Tabanı
            if (sekil != SEKIL_ORIJINAL) {
                boya.setColor(0xFF222831);
                RectF rect = new RectF(0, 0, boyut, boyut);

                if (sekil == SEKIL_DAIRE) {
                    canvas.drawCircle(r, r, r, boya);
                } else if (sekil == SEKIL_SQUIRCLE) {
                    canvas.drawRoundRect(rect, boyut * 0.28f, boyut * 0.28f, boya);
                } else if (sekil == SEKIL_KARE) {
                    canvas.drawRoundRect(rect, boyut * 0.16f, boyut * 0.16f, boya);
                }

                // Orijinal simgeyi şeklin ortasına yerleştir
                int icDolgu = (int) (boyut * 0.14f);
                orijinal.setBounds(icDolgu, icDolgu, boyut - icDolgu, boyut - icDolgu);
                orijinal.draw(canvas);
            } else if (norm) {
                // Sadece boyut dengeleme (Boyut normalizasyonu)
                int icDolgu = (int) (boyut * 0.05f);
                orijinal.setBounds(icDolgu, icDolgu, boyut - icDolgu, boyut - icDolgu);
                orijinal.draw(canvas);
            }

            Drawable sonuc = new BitmapDrawable(context.getResources(), cikti);
            synchronized (onbellek) {
                onbellek.put(anahtar, sonuc);
            }
            return sonuc;
        } catch (Throwable t) {
            return orijinal;
        }
    }
}

