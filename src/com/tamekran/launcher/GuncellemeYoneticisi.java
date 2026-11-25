package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Tam Ekran - Otomatik Güncelleme Yöneticisi
 * - Bilinmeyen kaynaklar kontrolü (Android 2.2 - Android 17 uyumlu)
 * - GitHub / JSON sürüm sorgulama
 * - Saf Java asenkron APK indirme ve doğrudan kurulum
 */
public class GuncellemeYoneticisi {

    private static final String SURUM_KONTROL_URL = "https://raw.githubusercontent.com/devezir/tam-ekran/main/version.json";
    private static Handler anaKanal;

    private static Handler getAnaKanal() {
        if (anaKanal == null) {
            anaKanal = new Handler(Looper.getMainLooper());
        }
        return anaKanal;
    }

    public interface DurumDinleyici {
        void durumGuncelle(String mesaj);
    }

    /**
     * Android sürümüne göre 'Bilinmeyen Kaynaklar / Uygulama Yükleme' iznini denetler.
     */
    public static boolean bilinmeyenKaynaklarIzniVarMi(Context context) {
        if (context == null) return false;

        // Android 8.0 (API 26) ve üzeri: Paket bazlı izin
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                return context.getPackageManager().canRequestPackageInstalls();
            } catch (Throwable t) {
                return false;
            }
        }

        // Android 4.2 - 7.1 (API 17 - 25)
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                return Settings.Global.getInt(context.getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS, 0) == 1;
            } catch (Throwable t) {
                LauncherLog.d("GuncellemeYoneticisi", "Settings.Global kontrol basarisiz: " + t.getMessage());
            }
        }

        // Android 2.2 - 4.1 (API 8 - 16)
        try {
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1;
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * Kullanıcıyı sistemin Bilinmeyen Kaynaklar / Yükleme İzni ayar ekranına yönlendirir.
     */
    public static void bilinmeyenKaynaklarAyariniAc(Activity activity) {
        if (activity == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                Intent niyet = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                niyet.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(niyet);
            } else {
                Intent niyet = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                activity.startActivity(niyet);
            }
        } catch (Throwable t) {
            ArayuzYardimcisi.toastUzun(activity, "Güvenlik ayarları açılamadı. Lütfen Ayarlar > Güvenlik menüsünü kontrol edin.");
        }
    }

    /**
     * Kullanıcının talebiyle güncelleme sürecini başlatır.
     * Kural: Önce bilinmeyen kaynaklar kontrol edilir, izin yoksa indirme başlatılmaz.
     */
    public static void guncellemeleriDenetle(final Activity activity, final DurumDinleyici dinleyici) {
        if (activity == null || activity.isFinishing()) return;

        // 1. ADIM: Bilinmeyen Kaynaklar İzni Kontrolü
        if (!bilinmeyenKaynaklarIzniVarMi(activity)) {
            new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.bilinmeyen_kaynak_baslik))
                .setMessage(activity.getString(R.string.bilinmeyen_kaynak_mesaj))
                .setPositiveButton(activity.getString(R.string.izin_ver), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bilinmeyenKaynaklarAyariniAc(activity);
                    }
                })
                .setNegativeButton(activity.getString(R.string.iptal), null)
                .show();

            if (dinleyici != null) {
                dinleyici.durumGuncelle("Güncelleme için yükleme izni gerekiyor.");
            }
            return;
        }

        // 2. ADIM: İzin tamam, sunucudan yeni sürüm kontrolü yap
        if (dinleyici != null) {
            dinleyici.durumGuncelle(activity.getString(R.string.guncelleme_denetleniyor));
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int yerelVersionCode = 1;
                    String yerelVersionName = "1.0.0";
                    try {
                        PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                        yerelVersionCode = pInfo.versionCode;
                        yerelVersionName = pInfo.versionName;
                    } catch (Throwable t) {
                        LauncherLog.w("GuncellemeYoneticisi", "PackageInfo alinamadi", t);
                    }

                    URL url = new URL(SURUM_KONTROL_URL + "?t=" + System.currentTimeMillis());
                    HttpURLConnection baglanti = (HttpURLConnection) url.openConnection();
                    baglanti.setRequestMethod("GET");
                    baglanti.setConnectTimeout(8000);
                    baglanti.setReadTimeout(8000);
                    baglanti.setRequestProperty("User-Agent", "TamEkran-Android");
                    baglanti.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
                    baglanti.setRequestProperty("Pragma", "no-cache");
                    baglanti.setUseCaches(false);

                    int cevapKodu = baglanti.getResponseCode();
                    if (cevapKodu != 200) {
                        bildirHata(activity, dinleyici, "Sunucuya bağlanılamadı (Kod: " + cevapKodu + ")");
                        return;
                    }

                    BufferedReader okuyucu = new BufferedReader(new InputStreamReader(baglanti.getInputStream(), "UTF-8"));
                    StringBuilder yanit = new StringBuilder();
                    String satir;
                    while ((satir = okuyucu.readLine()) != null) {
                        yanit.append(satir);
                    }
                    okuyucu.close();

                    JSONObject json = new JSONObject(yanit.toString());
                    final int yeniVersionCode = json.optInt("versionCode", 1);
                    final String yeniVersionName = json.optString("versionName", "1.0.0");
                    final String aciklama = json.optString("changelog", "Performans ve kararlılık iyileştirmeleri.");
                    final String indirmeAdresi = json.optString("downloadUrl", "");
                    final String beklenenSha256 = json.optString("apkSha256", json.optString("sha256", ""));

                    final int fYerelVc = yerelVersionCode;
                    final String fYerelVn = yerelVersionName;

                    getAnaKanal().post(new Runnable() {
                        @Override
                        public void run() {
                            if (activity.isFinishing()) return;

                            if (yeniVersionCode > fYerelVc) {
                                // Yeni sürüm bulundu!
                                if (dinleyici != null) {
                                    dinleyici.durumGuncelle("Yeni sürüm hazır: v" + yeniVersionName);
                                }
                                new AlertDialog.Builder(activity)
                                    .setTitle(activity.getString(R.string.guncelleme_yeni_surum_baslik, yeniVersionName))
                                    .setMessage(aciklama + "\n\nŞimdi indirilip kurulsun mu?")
                                    .setPositiveButton(activity.getString(R.string.guncelleme_indir_ve_kur), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            apkIndirVeKur(activity, indirmeAdresi, yeniVersionName, beklenenSha256);
                                        }
                                    })
                                    .setNegativeButton(activity.getString(R.string.iptal), null)
                                    .show();
                            } else {
                                // Zaten en güncel sürüm
                                if (dinleyici != null) {
                                    dinleyici.durumGuncelle("En güncel sürümü (v" + fYerelVn + ") kullanıyorsunuz.");
                                }
                                ArayuzYardimcisi.toast(activity, String.format(activity.getString(R.string.guncelleme_guncel), fYerelVn));
                            }
                        }
                    });

                } catch (final Throwable t) {
                    bildirHata(activity, dinleyici, t.getMessage());
                }
            }
        }).start();
    }

    private static void bildirHata(final Activity activity, final DurumDinleyici dinleyici, final String hata) {
        getAnaKanal().post(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) return;
                String mesaj = String.format(activity.getString(R.string.guncelleme_hata), hata != null ? hata : "Bilinmeyen hata");
                if (dinleyici != null) {
                    dinleyici.durumGuncelle(mesaj);
                }
                ArayuzYardimcisi.toastUzun(activity, mesaj);
            }
        });
    }

    private static boolean aktiviteCanliMi(Activity activity) {
        if (activity == null || activity.isFinishing()) return false;
        if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) return false;
        return true;
    }

    private static void guvenliKapat(Activity activity, ProgressDialog dialog) {
        if (dialog == null) return;
        try {
            if (aktiviteCanliMi(activity) && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            LauncherLog.d("GuncellemeYoneticisi", "ProgressDialog kapatma atlandi: " + e.getMessage());
        }
    }

    private static void dizindekiEskiApklariSil(File dizin) {
        if (dizin == null || !dizin.exists() || !dizin.isDirectory()) return;
        File[] dosyalar = dizin.listFiles();
        if (dosyalar == null) return;
        for (File f : dosyalar) {
            if (f != null && f.isFile()) {
                String ad = f.getName();
                if (ad.startsWith("TamEkran_") && (ad.endsWith(".apk") || ad.endsWith(".tmp"))) {
                    f.delete();
                }
            }
        }
    }

    /**
     * Yalnızca HTTPS ve resmi GitHub barındırma alan adlarına izin verir (Host Whitelist).
     */
    public static boolean isGuvenliIndirmeAdresi(String url) {
        if (url == null) return false;
        try {
            URL parsed = new URL(url.trim());
            if (!"https".equalsIgnoreCase(parsed.getProtocol())) return false;
            String host = parsed.getHost() != null ? parsed.getHost().toLowerCase(java.util.Locale.US) : "";
            return "github.com".equals(host)
                    || host.endsWith(".github.com")
                    || "githubusercontent.com".equals(host)
                    || host.endsWith(".githubusercontent.com");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dosyanın SHA-256 özetini küçük harfli 64 karakterlik hex olarak döndürür.
     */
    public static boolean sha256FormatGecerliMi(String hex) {
        if (hex == null) return false;
        String t = hex.trim();
        if (t.length() != 64) return false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            boolean gecerli = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!gecerli) return false;
        }
        return true;
    }

    public static String hesaplaSha256(File dosya) {
        if (dosya == null || !dosya.exists() || !dosya.isFile()) return null;
        java.io.FileInputStream fis = null;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            fis = new java.io.FileInputStream(dosya);
            byte[] buf = new byte[8192];
            int okunan;
            while ((okunan = fis.read(buf)) != -1) {
                digest.update(buf, 0, okunan);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xFF & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            LauncherLog.w("GuncellemeYoneticisi", "SHA-256 hesaplama hatasi", e);
            return null;
        } finally {
            try { if (fis != null) fis.close(); } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("deprecation")
    private static byte[] ilkImzayiAl(PackageInfo pi) {
        if (pi == null) return null;
        if (Build.VERSION.SDK_INT >= 28 && pi.signingInfo != null) {
            android.content.pm.Signature[] imzalar = pi.signingInfo.getApkContentsSigners();
            if (imzalar != null && imzalar.length > 0 && imzalar[0] != null) {
                return imzalar[0].toByteArray();
            }
        }
        if (pi.signatures != null && pi.signatures.length > 0 && pi.signatures[0] != null) {
            return pi.signatures[0].toByteArray();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static boolean apkImzasiEslesiyorMu(Context context, File apkDosyasi) {
        if (context == null || apkDosyasi == null) return false;
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            int flag = Build.VERSION.SDK_INT >= 28
                    ? android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                    : android.content.pm.PackageManager.GET_SIGNATURES;
            PackageInfo kurulu = pm.getPackageInfo(context.getPackageName(), flag);
            PackageInfo arsiv = pm.getPackageArchiveInfo(apkDosyasi.getAbsolutePath(), flag);
            byte[] kuruluImza = ilkImzayiAl(kurulu);
            byte[] arsivImza = ilkImzayiAl(arsiv);
            if (kuruluImza == null || arsivImza == null) return false;
            return java.util.Arrays.equals(kuruluImza, arsivImza);
        } catch (Exception e) {
            LauncherLog.w("GuncellemeYoneticisi", "Imza karsilastirma hatasi", e);
            return false;
        }
    }

    /**
     * İndirilen dosyanın sağlam bir APK arşivi, bu pakete ve aynı imza sertifikasına ait olduğunu doğrular.
     */
    public static boolean apkGecerliMi(Context context, File apkDosyasi) {
        if (context == null || apkDosyasi == null || !apkDosyasi.exists() || apkDosyasi.length() < 50000L) {
            return false;
        }
        try {
            PackageInfo info = context.getPackageManager().getPackageArchiveInfo(apkDosyasi.getAbsolutePath(), 0);
            if (info == null || !context.getPackageName().equals(info.packageName)) {
                return false;
            }
            return apkImzasiEslesiyorMu(context, apkDosyasi);
        } catch (Exception e) {
            return false;
        }
    }

    private static File tekSeferIndir(
            final Activity activity,
            final String indirmeUrl,
            final String beklenenSha256,
            final File hedefApk,
            final ProgressDialog ilerlemeKutusu) throws Exception {

        if (!isGuvenliIndirmeAdresi(indirmeUrl)) {
            throw new java.io.IOException(activity.getString(R.string.guncelleme_guvensiz_adres));
        }

        File geciciDosya = new File(hedefApk.getParentFile(), hedefApk.getName() + ".tmp");
        if (geciciDosya.exists()) geciciDosya.delete();
        if (hedefApk.exists()) hedefApk.delete();

        URL url = new URL(indirmeUrl);
        HttpURLConnection baglanti = null;
        int yonlendirmeSayisi = 0;
        while (yonlendirmeSayisi < 5) {
            if (!isGuvenliIndirmeAdresi(url.toString())) {
                throw new java.io.IOException(activity.getString(R.string.guncelleme_guvensiz_adres));
            }
            baglanti = (HttpURLConnection) url.openConnection();
            baglanti.setInstanceFollowRedirects(false);
            baglanti.setConnectTimeout(15000);
            baglanti.setReadTimeout(20000);
            baglanti.setRequestProperty("User-Agent", "TamEkran-Android");
            baglanti.setRequestProperty("Accept-Encoding", "identity");
            baglanti.connect();

            int cevapKodu = baglanti.getResponseCode();
            if (cevapKodu == HttpURLConnection.HTTP_MOVED_PERM
                    || cevapKodu == HttpURLConnection.HTTP_MOVED_TEMP
                    || cevapKodu == HttpURLConnection.HTTP_SEE_OTHER
                    || cevapKodu == 307
                    || cevapKodu == 308) {
                String yeniKonum = baglanti.getHeaderField("Location");
                if (yeniKonum != null && yeniKonum.length() > 0) {
                    URL sonrakiUrl = new URL(url, yeniKonum);
                    if (!isGuvenliIndirmeAdresi(sonrakiUrl.toString())) {
                        baglanti.disconnect();
                        throw new java.io.IOException(activity.getString(R.string.guncelleme_guvensiz_adres));
                    }
                    url = sonrakiUrl;
                    yonlendirmeSayisi++;
                    baglanti.disconnect();
                    continue;
                }
            }
            if (cevapKodu != HttpURLConnection.HTTP_OK) {
                baglanti.disconnect();
                throw new java.io.IOException("Sunucu hatası (HTTP " + cevapKodu + ")");
            }
            break;
        }

        if (baglanti == null) {
            throw new java.io.IOException("Bağlantı kurulamadı.");
        }

        int dosyaBoyutu = baglanti.getContentLength();
        InputStream girdi = null;
        FileOutputStream cikti = null;
        long toplam = 0;
        try {
            girdi = baglanti.getInputStream();
            cikti = new FileOutputStream(geciciDosya);

            byte[] veri = new byte[8192];
            int sonYuzde = -1;
            int sayac;
            while ((sayac = girdi.read(veri)) != -1) {
                toplam += sayac;
                cikti.write(veri, 0, sayac);
                if (dosyaBoyutu > 0) {
                    final int yuzde = (int) ((toplam * 100L) / dosyaBoyutu);
                    if (yuzde != sonYuzde) {
                        sonYuzde = yuzde;
                        getAnaKanal().post(new Runnable() {
                            @Override
                            public void run() {
                                if (aktiviteCanliMi(activity) && ilerlemeKutusu.isShowing()) {
                                    ilerlemeKutusu.setProgress(yuzde);
                                }
                            }
                        });
                    }
                }
            }
            cikti.flush();
        } finally {
            try { if (cikti != null) cikti.close(); } catch (Exception e) { LauncherLog.d("GuncellemeYoneticisi", "cikti close: " + e.getMessage()); }
            try { if (girdi != null) girdi.close(); } catch (Exception e) { LauncherLog.d("GuncellemeYoneticisi", "girdi close: " + e.getMessage()); }
            try { baglanti.disconnect(); } catch (Exception e) { LauncherLog.d("GuncellemeYoneticisi", "disconnect: " + e.getMessage()); }
        }

        if (dosyaBoyutu > 0 && toplam != dosyaBoyutu) {
            geciciDosya.delete();
            throw new java.io.IOException("İndirme yarım kaldı (" + toplam + "/" + dosyaBoyutu + " bayt)");
        }
        if (toplam < 50000L) {
            geciciDosya.delete();
            throw new java.io.IOException("İndirilen dosya geçersiz (" + toplam + " bayt)");
        }

        String temizBeklenen = beklenenSha256 != null ? beklenenSha256.trim() : "";
        if (!sha256FormatGecerliMi(temizBeklenen)) {
            geciciDosya.delete();
            throw new java.io.IOException(activity.getString(R.string.guncelleme_hash_uyumsuz));
        }
        String gercekHash = hesaplaSha256(geciciDosya);
        if (gercekHash == null || !gercekHash.equalsIgnoreCase(temizBeklenen)) {
            geciciDosya.delete();
            throw new java.io.IOException(activity.getString(R.string.guncelleme_hash_uyumsuz));
        }

        if (!geciciDosya.renameTo(hedefApk)) {
            geciciDosya.delete();
            throw new java.io.IOException("APK dosyası hazırlanamadı.");
        }

        if (!apkGecerliMi(activity, hedefApk)) {
            hedefApk.delete();
            throw new java.io.IOException(activity.getString(R.string.guncelleme_imza_uyumsuz));
        }

        return hedefApk;
    }

    /**
     * APK dosyasını arka planda indirir, hash + sertifika bütünlüğünü doğrular ve kurulumu başlatır.
     */
    private static void apkIndirVeKur(final Activity activity, final String indirmeUrl, final String surumAdi, final String beklenenSha256) {
        if (!aktiviteCanliMi(activity)) return;
        if (indirmeUrl == null || indirmeUrl.trim().length() == 0) {
            ArayuzYardimcisi.toast(activity, activity.getString(R.string.guncelleme_adres_yok));
            return;
        }
        if (!isGuvenliIndirmeAdresi(indirmeUrl)) {
            ArayuzYardimcisi.toastUzun(activity, activity.getString(R.string.guncelleme_guvensiz_adres));
            return;
        }

        final ProgressDialog ilerlemeKutusu = new ProgressDialog(activity);
        ilerlemeKutusu.setTitle(activity.getString(R.string.guncelleme_indiriliyor_baslik));
        ilerlemeKutusu.setMessage(activity.getString(R.string.guncelleme_indiriliyor));
        ilerlemeKutusu.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        ilerlemeKutusu.setIndeterminate(false);
        ilerlemeKutusu.setMax(100);
        ilerlemeKutusu.setCancelable(false);
        ilerlemeKutusu.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dizindekiEskiApklariSil(activity.getExternalCacheDir());
                    dizindekiEskiApklariSil(activity.getCacheDir());

                    File hedefDizin = activity.getExternalCacheDir();
                    if (hedefDizin == null) {
                        hedefDizin = activity.getCacheDir();
                    }

                    String temizSurum = (surumAdi != null && surumAdi.trim().length() > 0)
                            ? surumAdi.trim().replaceAll("[^a-zA-Z0-9._-]", "_")
                            : String.valueOf(System.currentTimeMillis());
                    File hedefApk = new File(hedefDizin, "TamEkran_v" + temizSurum + ".apk");

                    File indirilenApk = null;
                    Exception sonHata = null;
                    for (int deneme = 0; deneme < 2; deneme++) {
                        try {
                            indirilenApk = tekSeferIndir(activity, indirmeUrl, beklenenSha256, hedefApk, ilerlemeKutusu);
                            sonHata = null;
                            break;
                        } catch (Exception e) {
                            sonHata = e;
                            if (deneme == 0) {
                                getAnaKanal().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (aktiviteCanliMi(activity) && ilerlemeKutusu.isShowing()) {
                                            ilerlemeKutusu.setProgress(0);
                                        }
                                    }
                                });
                            }
                        }
                    }

                    if (indirilenApk == null || sonHata != null) {
                        throw (sonHata != null ? sonHata : new java.io.IOException("İndirme başarısız."));
                    }

                    final File hazirApk = indirilenApk;
                    getAnaKanal().post(new Runnable() {
                        @Override
                        public void run() {
                            guvenliKapat(activity, ilerlemeKutusu);
                            if (aktiviteCanliMi(activity)) {
                                kurulumuBaslat(activity, hazirApk);
                            }
                        }
                    });

                } catch (final Exception e) {
                    getAnaKanal().post(new Runnable() {
                        @Override
                        public void run() {
                            guvenliKapat(activity, ilerlemeKutusu);
                            if (aktiviteCanliMi(activity)) {
                                ArayuzYardimcisi.toastUzun(activity, activity.getString(R.string.guncelleme_basarisiz, e.getMessage()));
                            }
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * İndirilen APK dosyasını sisteme kurmak için Paket Yükleyiciyi açar.
     */
    public static void kurulumuBaslat(Context context, File apkDosyasi) {
        if (context == null || !apkGecerliMi(context, apkDosyasi)) {
            if (context != null) {
                ArayuzYardimcisi.toastUzun(context, "Kurulum paketi geçersiz veya eksik, lütfen tekrar deneyin.");
            }
            return;
        }

        String eylem = (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 29)
                ? Intent.ACTION_INSTALL_PACKAGE
                : Intent.ACTION_VIEW;
        Intent niyet = new Intent(eylem);
        if (!(context instanceof Activity)) {
            niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        if (Build.VERSION.SDK_INT >= 24) {
            // Android 7.0+ (API 24+) ContentProvider üzerinden güvenli paylaşım
            Uri contentUri = Uri.parse("content://" + DosyaSaglayici.OTORITE + "/" + apkDosyasi.getName());
            niyet.setDataAndType(contentUri, "application/vnd.android.package-archive");
            niyet.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            // Android 2.2 - 6.0 doğrudan dosya URI'si
            niyet.setDataAndType(Uri.fromFile(apkDosyasi), "application/vnd.android.package-archive");
        }

        try {
            context.startActivity(niyet);
        } catch (Throwable t) {
            ArayuzYardimcisi.toastUzun(context, "Paket yükleyici açılamadı: " + t.getMessage());
        }
    }
}
