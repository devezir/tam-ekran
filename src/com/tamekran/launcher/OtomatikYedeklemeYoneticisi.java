package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tam Ekran - Döngüsel Otomatik Yedekleme Motoru (Rolling Auto-Backup)
 * Düzen değişikliklerinde son 3 otomatik yedeği rotasyonlu olarak saklar.
 * Kullanıcının yanlışlıkla masaüstünü bozması durumunda anında geri alma sağlar.
 */
public class OtomatikYedeklemeYoneticisi {

    private static final int MAKS_OTO_YEDEK = 3;
    private static long sonYedekZamani = 0;

    public static class OtoYedekBilgisi {
        public File dosya;
        public String tarihEtiketi;
        public int sira;
    }

    public static void kontrolEtVeYedekle(final Context context) {
        if (context == null) return;
        long simdi = System.currentTimeMillis();
        // Aynı dakikada gereksiz tekrar yazmayı engelle (en az 60 saniye ara)
        if (simdi - sonYedekZamani < 60000) return;
        sonYedekZamani = simdi;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File anaYedek = YedeklemeYoneticisi.yedekDosyasi(context);
                    if (!YedeklemeYoneticisi.yedekAl(context) || !anaYedek.exists()) return;

                    File dizin = anaYedek.getParentFile();
                    if (dizin == null) return;

                    // Rotasyon: 2 -> 3, 1 -> 2
                    for (int i = MAKS_OTO_YEDEK - 1; i >= 1; i--) {
                        File eski = new File(dizin, "TamEkran_OtoYedek_" + i + ".json");
                        File yeni = new File(dizin, "TamEkran_OtoYedek_" + (i + 1) + ".json");
                        if (eski.exists()) {
                            if (yeni.exists()) yeni.delete();
                            eski.renameTo(yeni);
                        }
                    }

                    // Ana yedeği 1 numaralı oto yedek olarak kopyala
                    File hedef = new File(dizin, "TamEkran_OtoYedek_1.json");
                    dosyaKopyala(anaYedek, hedef);
                } catch (Throwable t) {
                    LauncherLog.w("OtomatikYedeklemeYoneticisi", "otomatikYedekle hatasi", t);
                }
            }
        }).start();
    }

    public static List<OtoYedekBilgisi> otoYedekleriListele(Context context) {
        List<OtoYedekBilgisi> liste = new ArrayList<OtoYedekBilgisi>();
        if (context == null) return liste;

        File anaYedek = YedeklemeYoneticisi.yedekDosyasi(context);
        File dizin = anaYedek.getParentFile();
        if (dizin == null || !dizin.exists()) return liste;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

        for (int i = 1; i <= MAKS_OTO_YEDEK; i++) {
            File f = new File(dizin, "TamEkran_OtoYedek_" + i + ".json");
            if (f.exists() && f.length() > 0) {
                OtoYedekBilgisi info = new OtoYedekBilgisi();
                info.dosya = f;
                info.sira = i;
                info.tarihEtiketi = sdf.format(new Date(f.lastModified()));
                liste.add(info);
            }
        }
        return liste;
    }

    public static boolean otoYedektenGeriYukle(Context context, File otoYedekDosyasi) {
        if (context == null || otoYedekDosyasi == null || !otoYedekDosyasi.exists()) return false;
        try {
            File anaYedek = YedeklemeYoneticisi.yedekDosyasi(context);
            dosyaKopyala(otoYedekDosyasi, anaYedek);
            return YedeklemeYoneticisi.yedeğiYukle(context);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void dosyaKopyala(File kaynak, File hedef) throws Exception {
        FileInputStream fis = new FileInputStream(kaynak);
        FileOutputStream fos = new FileOutputStream(hedef);
        byte[] buf = new byte[4096];
        int len;
        while ((len = fis.read(buf)) > 0) {
            fos.write(buf, 0, len);
        }
        fis.close();
        fos.close();
    }
}
