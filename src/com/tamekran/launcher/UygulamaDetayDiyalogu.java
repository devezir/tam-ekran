package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tam Ekran - Zengin Uygulama Bilgisi & Boyut Detay Kartı
 * Uygulamanın sürümü, dosya boyutu, kurulum/güncelleme tarihi, hedef SDK'sı
 * ve başlatma sayısını Nova tarzı zarif bir kartta gösterir.
 */
public class UygulamaDetayDiyalogu {

    public static void goster(final Activity activity, final UygulamaBilgisi app) {
        if (activity == null || app == null) return;

        PackageManager pm = activity.getPackageManager();
        PackageInfo pi = null;
        ApplicationInfo ai = null;
        try {
            pi = pm.getPackageInfo(app.paketAdi, 0);
            ai = pm.getApplicationInfo(app.paketAdi, 0);
        } catch (Throwable t) {
            LauncherLog.d("UygulamaDetayDiyalogu", "PackageInfo alinamadi: " + t.getMessage());
        }

        int pad = ArayuzYardimcisi.dpToPx(activity, 16);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundColor(0xFF1E1E1E);

        // Üst Başlık (Simge + İsim + Paket Adı)
        LinearLayout ustKart = new LinearLayout(activity);
        ustKart.setOrientation(LinearLayout.HORIZONTAL);
        ustKart.setGravity(Gravity.CENTER_VERTICAL);
        ustKart.setPadding(0, 0, 0, pad);

        ImageView simge = new ImageView(activity);
        simge.setImageDrawable(app.simge);
        int simgeBoyut = ArayuzYardimcisi.dpToPx(activity, 48);
        LinearLayout.LayoutParams lpSimge = new LinearLayout.LayoutParams(simgeBoyut, simgeBoyut);
        lpSimge.setMargins(0, 0, pad, 0);
        simge.setLayoutParams(lpSimge);
        ustKart.addView(simge);

        LinearLayout baslikCol = new LinearLayout(activity);
        baslikCol.setOrientation(LinearLayout.VERTICAL);

        TextView yaziAd = new TextView(activity);
        yaziAd.setText(app.etiket);
        yaziAd.setTextSize(18);
        yaziAd.setTypeface(null, android.graphics.Typeface.BOLD);
        yaziAd.setTextColor(Color.WHITE);
        baslikCol.addView(yaziAd);

        TextView yaziPaket = new TextView(activity);
        yaziPaket.setText(app.paketAdi);
        yaziPaket.setTextSize(11);
        yaziPaket.setTextColor(Color.GRAY);
        baslikCol.addView(yaziPaket);

        ustKart.addView(baslikCol);
        layout.addView(ustKart);

        // Detay Bilgileri
        ScrollView scrollView = new ScrollView(activity);
        LinearLayout detayKapsayici = new LinearLayout(activity);
        detayKapsayici.setOrientation(LinearLayout.VERTICAL);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

        // 1. Sürüm Bilgisi
        String surumAdi = pi != null ? pi.versionName : "Bilinmiyor";
        long surumKodu = pi != null ? pi.versionCode : 0;
        detaySatiriEkle(activity, detayKapsayici, "📦 Sürüm", surumAdi + " (v" + surumKodu + ")");

        // 2. APK Dosya Boyutu
        if (ai != null && ai.sourceDir != null) {
            File f = new File(ai.sourceDir);
            if (f.exists()) {
                double mb = f.length() / (1024.0 * 1024.0);
                detaySatiriEkle(activity, detayKapsayici, "💾 APK Boyutu", String.format(Locale.US, "%.2f MB", mb));
            }
        }

        // 3. Hedef Android Sürümü (Target SDK)
        if (ai != null) {
            detaySatiriEkle(activity, detayKapsayici, "🎯 Hedef SDK", "API " + ai.targetSdkVersion);
        }

        // 4. Kurulum ve Son Güncelleme Tarihi
        if (pi != null) {
            detaySatiriEkle(activity, detayKapsayici, "📅 İlk Kurulum", sdf.format(new Date(pi.firstInstallTime)));
            detaySatiriEkle(activity, detayKapsayici, "🔄 Son Güncelleme", sdf.format(new Date(pi.lastUpdateTime)));
        }

        // 5. Başlatma Sayısı
        int acilisSayisi = KullanimSayaci.getKullanimSayisi(activity, app.paketAdi);
        detaySatiriEkle(activity, detayKapsayici, "📊 Başlatma Sayısı", acilisSayisi + " kez açıldı");

        // 6. Kilit Durumu
        boolean kilitli = UygulamaKilidiYoneticisi.kilitliMi(activity, app.paketAdi);
        detaySatiriEkle(activity, detayKapsayici, "🔒 PIN Güvenliği", kilitli ? "Korumalı (PIN Gerekli)" : "Açık (Korumasız)");

        scrollView.addView(detayKapsayici);
        layout.addView(scrollView);

        // Alt Eylemler (Tasarım Felsefesi: AkisSatiriLayout + Sabit Yükseklik Yok)
        AkisSatiriLayout altBar = new AkisSatiriLayout(activity);
        altBar.setPadding(0, pad, 0, 0);

        Button btnAyarlar = ArayuzYardimcisi.dugmeOlustur(activity, "⚙️ Sistem", R.color.renk_vurgu, false);
        altBar.addView(btnAyarlar);

        Button btnPlay = ArayuzYardimcisi.dugmeOlustur(activity, "🛍️ Play Store", R.color.renk_yazi_ana, false);
        altBar.addView(btnPlay);

        Button btnApkPaylas = ArayuzYardimcisi.dugmeOlustur(activity, "📦 APK Paylaş", R.color.renk_yazi_ana, false);
        altBar.addView(btnApkPaylas);

        layout.addView(altBar);

        Button btnKapat = ArayuzYardimcisi.dugmeOlustur(activity, "Kapat", R.color.renk_yazi_ikincil, false);
        LinearLayout.LayoutParams lpKapat = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lpKapat.setMargins(0, ArayuzYardimcisi.dpToPx(activity, 8), 0, 0);
        btnKapat.setLayoutParams(lpKapat);
        layout.addView(btnKapat);

        final AlertDialog diyalog = new AlertDialog.Builder(activity)
            .setView(layout)
            .create();

        btnKapat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                diyalog.dismiss();
            }
        });

        btnAyarlar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SistemAraclariYardimcisi.uygulamaBilgisiAc(activity, app.paketAdi);
                diyalog.dismiss();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArayuzYardimcisi.playStoreAc(activity, app.paketAdi);
                diyalog.dismiss();
            }
        });

        btnApkPaylas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApkPaylasimYardimcisi.apkPaylas(activity, app);
                diyalog.dismiss();
            }
        });

        diyalog.show();
    }

    private static void detaySatiriEkle(Context context, LinearLayout parent, String baslik, String deger) {
        int pad6 = ArayuzYardimcisi.dpToPx(context, 6);
        LinearLayout satir = new LinearLayout(context);
        satir.setOrientation(LinearLayout.HORIZONTAL);
        satir.setPadding(0, pad6, 0, pad6);

        TextView yaziBaslik = new TextView(context);
        yaziBaslik.setText(baslik);
        yaziBaslik.setTextSize(13);
        yaziBaslik.setTextColor(Color.LTGRAY);
        yaziBaslik.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        satir.addView(yaziBaslik);

        TextView yaziDeger = new TextView(context);
        yaziDeger.setText(deger);
        yaziDeger.setTextSize(13);
        yaziDeger.setTypeface(null, android.graphics.Typeface.BOLD);
        yaziDeger.setTextColor(Color.WHITE);
        satir.addView(yaziDeger);

        parent.addView(satir);
    }
}
