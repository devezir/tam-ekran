package com.tamekran.launcher;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Tüm Uygulamalar Çekmecesi için A-Z Hızlı Alfabe İndeks Çubuğu Yöneticisi (SRP & DRY).
 */
public final class AlfabeIndeksYardimcisi {

    private AlfabeIndeksYardimcisi() {}

    public static final String[] ALFABE = new String[]{
        "A", "B", "C", "Ç", "D", "E", "F", "G", "H", "I", "İ",
        "J", "K", "L", "M", "N", "O", "Ö", "P", "R", "S", "Ş",
        "T", "U", "Ü", "V", "Y", "Z", "#"
    };

    /**
     * A-Z Hızlı Alfabe İndeks Çubuğunu Doldurur ve Dinleyicilerini Bağlar
     */
    public static void olustur(
            final Context context,
            final LinearLayout cubukAlfabe,
            final GridView izgaraTumUygulamalar,
            final List<UygulamaBilgisi> filtrelenmisListe) {

        if (context == null || cubukAlfabe == null) return;
        cubukAlfabe.removeAllViews();

        for (final String harf : ALFABE) {
            TextView t = new TextView(context);
            t.setText(harf);
            t.setTextSize(9);
            t.setTextColor(Color.parseColor("#99FFFFFF"));
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, 1, 0, 1);
            t.setClickable(true);
            t.setFocusable(true);

            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    harfeAtla(context, harf, izgaraTumUygulamalar, filtrelenmisListe);
                }
            });

            cubukAlfabe.addView(t);
        }
    }

    /**
     * Seçilen harfe karşılık gelen uygulamaya ızgara üzerinde kaydırır
     */
    public static void harfeAtla(
            final Context context,
            final String harf,
            final GridView izgaraTumUygulamalar,
            final List<UygulamaBilgisi> filtrelenmisListe) {

        if (context == null || filtrelenmisListe == null || filtrelenmisListe.isEmpty()) return;
        SurukleBirakYardimcisi.titresim(context, 20);

        int hedef = -1;
        if ("#".equals(harf)) {
            hedef = filtrelenmisListe.size() - 1;
        } else {
            Locale trLocale = new Locale("tr", "TR");
            for (int i = 0; i < filtrelenmisListe.size(); i++) {
                String etiket = filtrelenmisListe.get(i).etiket;
                if (etiket != null && etiket.toUpperCase(trLocale).startsWith(harf)) {
                    hedef = i;
                    break;
                }
            }
        }

        if (hedef != -1 && izgaraTumUygulamalar != null) {
            izgaraTumUygulamalar.setSelection(hedef);
        }
    }
}
