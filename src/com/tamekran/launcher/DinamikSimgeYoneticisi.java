package com.tamekran.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Tam Ekran - Dinamik Simge Motoru (Nova Style Dynamic Calendar Icon)
 * Takvim uygulamalarının simgelerinde o anki günün gerçek tarihini gösterir.
 */
public class DinamikSimgeYoneticisi {

    private static int sonCizilenGun = -1;
    private static BitmapDrawable onbellekTakvim;

    public static boolean takvimUygulamasiMi(String paketAdi) {
        if (paketAdi == null) return false;
        String p = paketAdi.toLowerCase(Locale.US);
        return p.contains("calendar") || p.contains("takvim") ||
               p.equals("com.google.android.calendar") ||
               p.equals("com.samsung.android.calendar") ||
               p.equals("com.miui.calendar");
    }

    public static Drawable dinamikTakvimSimgesi(Context context, Drawable temelSimge) {
        if (context == null) return temelSimge;

        Calendar takvim = Calendar.getInstance();
        int bugun = takvim.get(Calendar.DAY_OF_MONTH);

        if (bugun == sonCizilenGun && onbellekTakvim != null) {
            return onbellekTakvim;
        }

        int boyut = ArayuzYardimcisi.dpToPx(context, 56);
        if (boyut <= 0) boyut = 144;

        Bitmap bitmap = Bitmap.createBitmap(boyut, boyut, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Beyaz Takvim Sayfası
        Paint boyaKart = new Paint(Paint.ANTI_ALIAS_FLAG);
        boyaKart.setColor(Color.WHITE);
        boyaKart.setStyle(Paint.Style.FILL);

        RectF kartAlani = new RectF(4, 4, boyut - 4, boyut - 4);
        float r = ArayuzYardimcisi.dpToPx(context, 12);
        canvas.drawRoundRect(kartAlani, r, r, boyaKart);

        // Üst Renkli Başlık (Ayın Kısaltması)
        Paint boyaBaslik = new Paint(Paint.ANTI_ALIAS_FLAG);
        boyaBaslik.setColor(TemaYoneticisi.getVurguRengi(context));
        boyaBaslik.setStyle(Paint.Style.FILL);

        int baslikYukseklik = (int) (boyut * 0.32f);
        canvas.save();
        canvas.clipRect(4, 4, boyut - 4, 4 + baslikYukseklik);
        canvas.drawRoundRect(kartAlani, r, r, boyaBaslik);
        canvas.restore();

        // Başlık Metni (Örn: KASIM veya GÜN ADI)
        Paint boyaYaziBaslik = new Paint(Paint.ANTI_ALIAS_FLAG);
        boyaYaziBaslik.setColor(Color.WHITE);
        boyaYaziBaslik.setTextSize(boyut * 0.17f);
        boyaYaziBaslik.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        boyaYaziBaslik.setTextAlign(Paint.Align.CENTER);

        SimpleDateFormat ayBicimi = new SimpleDateFormat("MMM", new Locale("tr", "TR"));
        String ayAdi = ayBicimi.format(new Date()).toUpperCase(new Locale("tr", "TR"));
        canvas.drawText(ayAdi, boyut / 2.0f, baslikYukseklik * 0.72f, boyaYaziBaslik);

        // Gün Numarası (Örn: 21)
        Paint boyaYaziGun = new Paint(Paint.ANTI_ALIAS_FLAG);
        boyaYaziGun.setColor(Color.parseColor("#212121"));
        boyaYaziGun.setTextSize(boyut * 0.44f);
        boyaYaziGun.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        boyaYaziGun.setTextAlign(Paint.Align.CENTER);

        String gunMetin = String.valueOf(bugun);
        Rect bounds = new Rect();
        boyaYaziGun.getTextBounds(gunMetin, 0, gunMetin.length(), bounds);
        float yPos = ((boyut + baslikYukseklik) / 2.0f) + (bounds.height() / 2.0f);
        canvas.drawText(gunMetin, boyut / 2.0f, yPos, boyaYaziGun);

        onbellekTakvim = new BitmapDrawable(context.getResources(), bitmap);
        sonCizilenGun = bugun;
        return onbellekTakvim;
    }
}
