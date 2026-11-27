package com.tamekran.launcher;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tam Ekran - Doğrudan APK Çıkarıcı ve Paylaşım Motoru
 * Cihazda yüklü herhangi bir uygulamanın APK kurulum dosyasını harici hiçbir
 * üçüncü parti araca ihtiyaç duymadan çıkarır ve sistem paylaşım sayfası üzerinden gönderir.
 */
public class ApkPaylasimYardimcisi {

    public static void kendiniApkOlarakPaylasSecimli(final Activity activity) {
        if (activity == null) return;
        CharSequence[] secenekler = new CharSequence[]{
            "📤 Paylaş (WhatsApp, Bluetooth, Drive...)",
            "💾 İndirilenler (Downloads) Klasörüne Kaydet"
        };
        AyarDiyalogYardimcisi.listeSecimDiyalogu(activity, activity.getString(R.string.ayar_kendini_apk_paylas_baslik), secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
            @Override
            public void onSecildi(int which) {
                String surum = "1.0";
                try {
                    PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                    if (pi != null && pi.versionName != null) surum = pi.versionName;
                } catch (Throwable t) {
                    LauncherLog.d("ApkPaylasimYardimcisi", "Surum bilgisi alinamadi: " + t.getMessage());
                }
                String dosyaEtiketi = "TamEkran_v" + surum;
                if (which == 0) {
                    UygulamaBilgisi selfApp = new UygulamaBilgisi(
                            dosyaEtiketi,
                            activity.getPackageName(),
                            (String) null,
                            (android.graphics.drawable.Drawable) null);
                    apkPaylas(activity, selfApp);
                } else if (which == 1) {
                    apkIndirilenlereKaydet(activity, activity.getPackageName(), dosyaEtiketi + ".apk");
                }
            }
        });
    }

    private static void apkIndirilenlereKaydet(final Activity activity, final String paketAdi, final String hedefDosyaAdi) {
        ArayuzYardimcisi.toast(activity, "💾 " + hedefDosyaAdi + " kaydediliyor...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream in = null;
                OutputStream out = null;
                try {
                    ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(paketAdi, 0);
                    File srcApk = new File(ai.sourceDir);
                    if (!srcApk.exists()) {
                        toastUi(activity, "APK kaynak dosyası bulunamadı.");
                        return;
                    }
                    in = new FileInputStream(srcApk);
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues cv = new ContentValues();
                        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, hedefDosyaAdi);
                        cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive");
                        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                        Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                        if (uri == null) {
                            toastUi(activity, "İndirilenler klasörüne yazılamadı.");
                            return;
                        }
                        out = activity.getContentResolver().openOutputStream(uri);
                    } else {
                        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloads.exists()) downloads.mkdirs();
                        File hedef = new File(downloads, hedefDosyaAdi);
                        out = new FileOutputStream(hedef);
                    }
                    if (out == null) {
                        toastUi(activity, "Çıktı akışı açılamadı.");
                        return;
                    }
                    byte[] buffer = new byte[16384];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                    toastUi(activity, "✅ İndirilenler'e kaydedildi: " + hedefDosyaAdi);
                } catch (final Throwable t) {
                    toastUi(activity, "APK kaydedilemedi: " + t.getMessage());
                } finally {
                    try { if (in != null) in.close(); } catch (Exception e) { LauncherLog.d("ApkPaylasimYardimcisi", "Girdi akisi kapatilamadi: " + e.getMessage()); }
                    try { if (out != null) out.close(); } catch (Exception e) { LauncherLog.d("ApkPaylasimYardimcisi", "Cikti akisi kapatilamadi: " + e.getMessage()); }
                }
            }
        }).start();
    }

    private static void toastUi(final Activity activity, final String mesaj) {
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArayuzYardimcisi.toastUzun(activity, mesaj);
            }
        });
    }

    public static void apkPaylas(final Activity activity, final UygulamaBilgisi app) {
        if (activity == null || app == null || app.paketAdi == null) return;

        ArayuzYardimcisi.toast(activity, "📦 " + app.etiket + " APK hazırlanıyor...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(app.paketAdi, 0);
                    File srcApk = new File(ai.sourceDir);
                    if (!srcApk.exists()) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ArayuzYardimcisi.toast(activity, "APK kaynak dosyası bulunamadı.");
                            }
                        });
                        return;
                    }

                    // Güvenli ve okunabilir bir dosya adı oluştur
                    String temizEtiket = (app.etiket != null ? app.etiket : app.paketAdi)
                        .replaceAll("[^a-zA-Z0-9çÇğĞıİöÖşŞüÜ_.-]", "_");
                    String hedefDosyaAdi = temizEtiket + ".apk";

                    File hedefDizin = activity.getExternalCacheDir();
                    if (hedefDizin == null) hedefDizin = activity.getCacheDir();
                    final File hedefApk = new File(hedefDizin, hedefDosyaAdi);

                    // Arka planda kopyala
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = new FileInputStream(srcApk);
                        out = new FileOutputStream(hedefApk);
                        byte[] buffer = new byte[16384];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                        out.flush();
                    } finally {
                        try { if (in != null) in.close(); } catch (Exception e) { LauncherLog.d("ApkPaylasimYardimcisi", "Paylasim girdi akisi kapatilamadi: " + e.getMessage()); }
                        try { if (out != null) out.close(); } catch (Exception e) { LauncherLog.d("ApkPaylasimYardimcisi", "Paylasim cikti akisi kapatilamadi: " + e.getMessage()); }
                    }

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Uri uri = DosyaSaglayici.getUriForFile(activity, hedefApk);
                                Intent paylasIntent = new Intent(Intent.ACTION_SEND);
                                paylasIntent.setType("application/vnd.android.package-archive");
                                paylasIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                paylasIntent.putExtra(Intent.EXTRA_SUBJECT, (app.etiket != null ? app.etiket : "Uygulama") + " APK");
                                paylasIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                activity.startActivity(Intent.createChooser(paylasIntent, app.etiket + " APK Dosyasını Paylaş"));
                            } catch (Throwable t) {
                                ArayuzYardimcisi.toast(activity, "Paylaşım başlatılamadı: " + t.getMessage());
                            }
                        }
                    });

                } catch (final Throwable t) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArayuzYardimcisi.toast(activity, "APK çıkarılamadı: " + t.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
}
