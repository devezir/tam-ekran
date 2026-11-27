package com.tamekran.launcher;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tam Ekran - Sık Kullanılan Uygulamalar Sayacı
 * Kullanıcının en çok açtığı uygulamaları yerel olarak takip eder.
 * %100 çevrimdışı, sıfır telemetri.
 */
public class KullanimSayaci {

    public static void uygulamayiBaslatti(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_SAYAC_DOSYA);
        if (sp == null) return;
        int sayi = sp.getInt(paketAdi, 0);
        AyarBankasi.editorKaydet(sp.edit().putInt(paketAdi, sayi + 1));
    }

    public static int getKullanimSayisi(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return 0;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_SAYAC_DOSYA);
        return sp != null ? sp.getInt(paketAdi, 0) : 0;
    }

    public static List<UygulamaBilgisi> sikKullanilanlariSec(final Context context, List<UygulamaBilgisi> tumu, int adet) {
        List<UygulamaBilgisi> kopya = new ArrayList<UygulamaBilgisi>(tumu);
        Collections.sort(kopya, new Comparator<UygulamaBilgisi>() {
            @Override
            public int compare(UygulamaBilgisi o1, UygulamaBilgisi o2) {
                int c1 = getKullanimSayisi(context, o1.paketAdi);
                int c2 = getKullanimSayisi(context, o2.paketAdi);
                return c2 - c1; // En çok kullanılan en başa
            }
        });

        List<UygulamaBilgisi> sonuc = new ArrayList<UygulamaBilgisi>();
        for (UygulamaBilgisi app : kopya) {
            if (getKullanimSayisi(context, app.paketAdi) > 0) {
                sonuc.add(app);
                if (sonuc.size() >= adet) break;
            }
        }
        return sonuc;
    }

    public static boolean ekranSuresiIzniVarMi(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 21) return false;
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Map<String, Long> son7GunEkranSureleriniAl(Context context) {
        Map<String, Long> sonuc = new HashMap<String, Long>();
        if (context == null || Build.VERSION.SDK_INT < 21 || !ekranSuresiIzniVarMi(context)) {
            return sonuc;
        }
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return sonuc;
            long simdi = System.currentTimeMillis();
            long yediGunOnce = simdi - (7L * 24L * 60L * 60L * 1000L);
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, yediGunOnce, simdi);
            if (stats != null) {
                for (UsageStats u : stats) {
                    if (u == null || u.getPackageName() == null) continue;
                    long sure = u.getTotalTimeInForeground();
                    if (sure > 0L) {
                        Long mevcut = sonuc.get(u.getPackageName());
                        sonuc.put(u.getPackageName(), (mevcut != null ? mevcut : 0L) + sure);
                    }
                }
            }
        } catch (Throwable t) {
            LauncherLog.d("KullanimSayaci", "UsageStats okunamadi: " + t.getMessage());
        }
        return sonuc;
    }

    public static String sureBicimlendir(long ms) {
        if (ms <= 0L) return "";
        long toplamDk = ms / 60000L;
        if (toplamDk <= 0L) return "< 1 dk";
        long saat = toplamDk / 60L;
        long dk = toplamDk % 60L;
        if (saat > 0L) {
            return dk > 0L ? (saat + " sa " + dk + " dk") : (saat + " sa");
        }
        return dk + " dk";
    }
}
