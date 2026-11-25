package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
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
                        try { if (in != null) in.close(); } catch (Exception ignored) {}
                        try { if (out != null) out.close(); } catch (Exception ignored) {}
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
