package com.tamekran.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tam Ekran - Klasör Simge Önizleme Motoru (2x2 Mini Grid Renderer)
 * Nova Launcher ve Pixel Launcher benzeri, klasörün içindeki ilk 4 uygulamanın
 * gerçek simgelerini şık bir 2x2 ızgara şeklinde kompozit simge olarak çizer.
 * Saf Java & Canvas, sıfır kütüphane bağımlılığı.
 */
public class KlasorSimgeUretici {

    public static final String PREF_KLASOR_SEKLI = "klasor_arka_plan_sekli";
    public static final int SEKIL_YUVARLATILMIS = 0;
    public static final int SEKIL_DAIRE = 1;
    public static final int SEKIL_KARE = 2;

    public static final String[] SEKIL_ISIMLERI = new String[]{
        "Squircle (Yuvarlatılmış Kart)",
        "Circle (Tam Daire)",
        "Square (Modern Kare)"
    };

    private static final Map<String, Drawable> simgeOnbellegi = new HashMap<String, Drawable>();

    public static void onbellegiTemizle() {
        simgeOnbellegi.clear();
    }

    public static int getKlasorSekli(Context context) {
        if (context == null) return SEKIL_YUVARLATILMIS;
        return AyarBankasi.getAyarlar(context).getInt(PREF_KLASOR_SEKLI, SEKIL_YUVARLATILMIS);
    }

    public static void setKlasorSekli(Context context, int sekil) {
        if (context == null) return;
        AyarBankasi.editorKaydet(AyarBankasi.getAyarlar(context).edit().putInt(PREF_KLASOR_SEKLI, sekil));
        onbellegiTemizle();
    }

    public static Drawable klasorSimgesiOlustur(
        Context context,
        KlasorYoneticisi.KlasorBilgisi klasor,
        List<UygulamaBilgisi> tumUygulamalar
    ) {
        if (context == null || klasor == null) {
            return context != null ? context.getResources().getDrawable(R.drawable.simge_varsayilan) : null;
        }

        int sekil = getKlasorSekli(context);

        // Önbellek anahtarı: Klasör ID'si + şekil + paket hash'i
        String anahtar = klasor.id + "_s" + sekil + "_" + klasor.paketler.size() + "_" + (klasor.paketler.isEmpty() ? 0 : klasor.paketler.get(0).hashCode());
        if (simgeOnbellegi.containsKey(anahtar)) {
            return simgeOnbellegi.get(anahtar);
        }

        int boyut = ArayuzYardimcisi.dpToPx(context, 56);
        if (boyut <= 0) boyut = 144;

        Bitmap bitmap = Bitmap.createBitmap(boyut, boyut, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. Arka Plan Plakası (Nova Tarzı - Ortak DRY Plaka Çizici)
        RectF alan = new RectF(2, 2, boyut - 2, boyut - 2);
        float kose = (sekil == SEKIL_KARE) ? 0f : ArayuzYardimcisi.dpToPx(context, 14);
        AracSimgeCizici.plakaCiz(
                canvas,
                alan,
                kose,
                0x55263238,
                0x44FFFFFF,
                ArayuzYardimcisi.dpToPx(context, 1.5f),
                sekil == SEKIL_DAIRE
        );

        // 2. Klasör içindeki ilk 4 uygulamanın simgelerini topla
        List<Drawable> miniSimgeler = new java.util.ArrayList<Drawable>();
        if (klasor.paketler != null && tumUygulamalar != null) {
            for (String pkg : klasor.paketler) {
                if (miniSimgeler.size() >= 4) break;
                UygulamaBilgisi app = UygulamaBilgisi.bilesenBul(tumUygulamalar, pkg);
                if (app != null && app.simge != null) {
                    miniSimgeler.add(app.simge);
                }
            }
        }

        // 3. Mini Simgeleri 2x2 Izgaraya Çiz
        if (miniSimgeler.isEmpty()) {
            // Boş klasör ise varsayılan simgeyi merkeze çiz
            Drawable def = context.getResources().getDrawable(R.drawable.simge_varsayilan);
            if (def != null) {
                int pad = boyut / 4;
                def.setBounds(pad, pad, boyut - pad, boyut - pad);
                def.draw(canvas);
            }
        } else if (miniSimgeler.size() == 1) {
            // Tek uygulama varsa ortala
            Drawable s = miniSimgeler.get(0);
            int pad = boyut / 5;
            s.setBounds(pad, pad, boyut - pad, boyut - pad);
            s.draw(canvas);
        } else {
            // 2, 3 veya 4 uygulama: 2x2 ızgara
            int miniBoyut = (int) (boyut * 0.38f);
            int kenarBosluk = (int) (boyut * 0.09f);
            int araBosluk = boyut - (2 * kenarBosluk) - (2 * miniBoyut);

            // Pozisyon 1: Sol Üst
            Drawable s1 = miniSimgeler.get(0);
            int l1 = kenarBosluk;
            int t1 = kenarBosluk;
            s1.setBounds(l1, t1, l1 + miniBoyut, t1 + miniBoyut);
            s1.draw(canvas);

            // Pozisyon 2: Sağ Üst
            if (miniSimgeler.size() > 1) {
                Drawable s2 = miniSimgeler.get(1);
                int l2 = l1 + miniBoyut + araBosluk;
                int t2 = t1;
                s2.setBounds(l2, t2, l2 + miniBoyut, t2 + miniBoyut);
                s2.draw(canvas);
            }

            // Pozisyon 3: Sol Alt
            if (miniSimgeler.size() > 2) {
                Drawable s3 = miniSimgeler.get(2);
                int l3 = l1;
                int t3 = t1 + miniBoyut + araBosluk;
                s3.setBounds(l3, t3, l3 + miniBoyut, t3 + miniBoyut);
                s3.draw(canvas);
            }

            // Pozisyon 4: Sağ Alt
            if (miniSimgeler.size() > 3) {
                Drawable s4 = miniSimgeler.get(3);
                int l4 = l1 + miniBoyut + araBosluk;
                int t4 = t1 + miniBoyut + araBosluk;
                s4.setBounds(l4, t4, l4 + miniBoyut, t4 + miniBoyut);
                s4.draw(canvas);
            }
        }

        BitmapDrawable sonuc = new BitmapDrawable(context.getResources(), bitmap);
        simgeOnbellegi.put(anahtar, sonuc);
        return sonuc;
    }
}

