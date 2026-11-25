package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tam Ekran - Uygulama Kullanım İstatistikleri & Süre Analitiği (Dijital Denge)
 * Hangi uygulamanın kaç kez açıldığını, kullanım yüzdelerini ve toplam açılışları
 * görsel çubuklarla sunar.
 */
public class KullanimRaporuDiyalogu {

    public static void goster(final Activity activity, List<UygulamaBilgisi> tumUygulamalar, final Runnable sifirlamaGorevi) {
        if (activity == null || tumUygulamalar == null) return;

        // Kullanım sayılarını topla
        int toplamAcilis = 0;
        final List<UygulamaBilgisi> siraliListe = new ArrayList<UygulamaBilgisi>();

        for (UygulamaBilgisi app : tumUygulamalar) {
            int sayi = KullanimSayaci.getKullanimSayisi(activity, app.paketAdi);
            if (sayi > 0) {
                toplamAcilis += sayi;
                siraliListe.add(app);
            }
        }

        Collections.sort(siraliListe, new Comparator<UygulamaBilgisi>() {
            @Override
            public int compare(UygulamaBilgisi o1, UygulamaBilgisi o2) {
                int c1 = KullanimSayaci.getKullanimSayisi(activity, o1.paketAdi);
                int c2 = KullanimSayaci.getKullanimSayisi(activity, o2.paketAdi);
                return c2 - c1;
            }
        });

        int pad = ArayuzYardimcisi.dpToPx(activity, 16);

        LinearLayout anaLayout = new LinearLayout(activity);
        anaLayout.setOrientation(LinearLayout.VERTICAL);
        anaLayout.setPadding(pad, pad, pad, pad);
        anaLayout.setBackgroundColor(0xFF1A1A1A);

        // Üst Başlık & İstatistik Kartı
        LinearLayout kartOzet = new LinearLayout(activity);
        kartOzet.setOrientation(LinearLayout.VERTICAL);
        kartOzet.setBackgroundColor(0xFF262626);
        kartOzet.setPadding(pad, pad, pad, pad);

        TextView yaziToplam = new TextView(activity);
        yaziToplam.setText("📊 Toplam Başlatma: " + toplamAcilis + " kez");
        yaziToplam.setTextSize(16);
        yaziToplam.setTypeface(null, android.graphics.Typeface.BOLD);
        yaziToplam.setTextColor(activity.getResources().getColor(R.color.renk_vurgu));
        kartOzet.addView(yaziToplam);

        TextView yaziAciklama = new TextView(activity);
        yaziAciklama.setText(siraliListe.size() + " farklı uygulama başlatıldı. %100 çevrimdışı, sıfır telemetri.");
        yaziAciklama.setTextSize(12);
        yaziAciklama.setTextColor(Color.LTGRAY);
        kartOzet.addView(yaziAciklama);

        anaLayout.addView(kartOzet);

        // Liste Kaydırma Alanı
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout.LayoutParams lpScroll = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ArayuzYardimcisi.dpToPx(activity, 320));
        lpScroll.setMargins(0, pad / 2, 0, pad / 2);
        scrollView.setLayoutParams(lpScroll);

        LinearLayout listeKapsayici = new LinearLayout(activity);
        listeKapsayici.setOrientation(LinearLayout.VERTICAL);

        if (siraliListe.isEmpty()) {
            TextView bosYazi = new TextView(activity);
            bosYazi.setText("Henüz kullanım verisi oluşmadı.\nUygulama açtıkça burada listelenecektir.");
            bosYazi.setGravity(Gravity.CENTER);
            bosYazi.setPadding(0, pad * 2, 0, pad * 2);
            bosYazi.setTextColor(Color.GRAY);
            listeKapsayici.addView(bosYazi);
        } else {
            int sira = 1;
            int pad8 = ArayuzYardimcisi.dpToPx(activity, 8);
            int dp26 = ArayuzYardimcisi.dpToPx(activity, 26);
            int dp32 = ArayuzYardimcisi.dpToPx(activity, 32);
            int dp6 = ArayuzYardimcisi.dpToPx(activity, 6);
            int dp4 = ArayuzYardimcisi.dpToPx(activity, 4);

            for (UygulamaBilgisi app : siraliListe) {
                int sayi = KullanimSayaci.getKullanimSayisi(activity, app.paketAdi);
                int yuzde = toplamAcilis > 0 ? (sayi * 100 / toplamAcilis) : 0;

                LinearLayout satir = new LinearLayout(activity);
                satir.setOrientation(LinearLayout.HORIZONTAL);
                satir.setGravity(Gravity.CENTER_VERTICAL);
                satir.setPadding(0, pad8, 0, pad8);

                // Sıra no
                TextView yaziSira = new TextView(activity);
                yaziSira.setText("#" + (sira++));
                yaziSira.setTextSize(12);
                yaziSira.setTextColor(Color.GRAY);
                yaziSira.setWidth(dp26);
                satir.addView(yaziSira);

                // Simge
                ImageView simge = new ImageView(activity);
                simge.setImageDrawable(app.simge);
                LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dp32, dp32);
                lpImg.setMargins(0, 0, pad8, 0);
                simge.setLayoutParams(lpImg);
                satir.addView(simge);

                // Bilgi ve Bar
                LinearLayout colBilgi = new LinearLayout(activity);
                colBilgi.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                colBilgi.setLayoutParams(lpCol);

                LinearLayout baslikSatiri = new LinearLayout(activity);
                baslikSatiri.setOrientation(LinearLayout.HORIZONTAL);

                TextView ad = new TextView(activity);
                ad.setText(app.etiket);
                ad.setTextSize(14);
                ad.setTextColor(Color.WHITE);
                ad.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                baslikSatiri.addView(ad);

                TextView sayac = new TextView(activity);
                sayac.setText(sayi + " kez (%" + yuzde + ")");
                sayac.setTextSize(12);
                sayac.setTextColor(Color.LTGRAY);
                baslikSatiri.addView(sayac);
                colBilgi.addView(baslikSatiri);

                // İlerleme çubuğu
                ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
                bar.setMax(100);
                bar.setProgress(yuzde);
                LinearLayout.LayoutParams lpBar = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp6);
                lpBar.setMargins(0, dp4, 0, 0);
                bar.setLayoutParams(lpBar);
                colBilgi.addView(bar);

                satir.addView(colBilgi);
                listeKapsayici.addView(satir);
            }
        }

        scrollView.addView(listeKapsayici);
        anaLayout.addView(scrollView);

        // Alt Butonlar
        LinearLayout altBar = new LinearLayout(activity);
        altBar.setOrientation(LinearLayout.HORIZONTAL);

        int dp40 = ArayuzYardimcisi.dpToPx(activity, 40);

        Button btnSifirla = new Button(activity);
        btnSifirla.setText("🗑️ Sıfırla");
        btnSifirla.setBackgroundResource(R.drawable.kart_arkaplan);
        btnSifirla.setTextColor(0xFFE57373);
        LinearLayout.LayoutParams lpReset = new LinearLayout.LayoutParams(0, dp40, 1.0f);
        lpReset.setMargins(0, 0, 8, 0);
        btnSifirla.setLayoutParams(lpReset);
        altBar.addView(btnSifirla);

        Button btnKapat = new Button(activity);
        btnKapat.setText("Kapat");
        btnKapat.setBackgroundResource(R.drawable.kart_arkaplan);
        btnKapat.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(0, dp40, 1.0f);
        btnKapat.setLayoutParams(lpClose);
        altBar.addView(btnKapat);

        anaLayout.addView(altBar);

        final AlertDialog diyalog = new AlertDialog.Builder(activity)
            .setView(anaLayout)
            .create();

        btnKapat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diyalog.dismiss();
            }
        });

        btnSifirla.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(activity)
                    .setTitle("İstatistikleri Sıfırla")
                    .setMessage("Tüm uygulama başlatma sayaçları sıfırlanacak. Emin misiniz?")
                    .setPositiveButton("Evet, Sıfırla", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            SharedPreferences sp = AyarBankasi.getOzelAyarlar(activity, AyarBankasi.PREF_SAYAC_DOSYA);
                            if (sp != null) AyarBankasi.editorKaydet(sp.edit().clear());
                            ArayuzYardimcisi.toast(activity, "Kullanım istatistikleri sıfırlandı.");
                            diyalog.dismiss();
                            if (sifirlamaGorevi != null) sifirlamaGorevi.run();
                        }
                    })
                    .setNegativeButton("İptal", null)
                    .show();
            }
        });

        diyalog.show();
    }
}
