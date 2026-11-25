package com.tamekran.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/**
 * Tam Ekran - Bildirim Rozetleri Yönetici Katmanı
 */
public class BildirimYoneticisi {

    public static final String PREF_BILDIRIM_ROZETI_TURU = "bildirim_rozeti_turu";
    public static final int ROZET_KAPALI = 0;
    public static final int ROZET_SAYI = 1;
    public static final int ROZET_NOKTA = 2;

    public static int getRozetTuru(Context context) {
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_BILDIRIM_ROZETI_TURU, ROZET_SAYI);
    }

    public static void setRozetTuru(Context context, int tur) {
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_BILDIRIM_ROZETI_TURU, tur));
    }

    public static boolean izinVerildiMi(Context context) {
        if (Build.VERSION.SDK_INT < 18) return false;

        try {
            String paket = context.getPackageName();
            String aktifDinleyiciler = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if (aktifDinleyiciler != null) {
                ComponentName cn = new ComponentName(context, BildirimDinleyiciServisi.class);
                return aktifDinleyiciler.contains(cn.flattenToString()) || aktifDinleyiciler.contains(paket);
            }
        } catch (Throwable t) {
            LauncherLog.d("BildirimYoneticisi", "izinVerildiMi kontrol basarisiz: " + t.getMessage());
        }
        return false;
    }

    public static void izinEkraniAc(Context context) {
        if (Build.VERSION.SDK_INT >= 18) {
            try {
                Intent niyet = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                context.startActivity(niyet);
                if (Build.VERSION.SDK_INT >= 33 && !izinVerildiMi(context)) {
                    ArayuzYardimcisi.toastUzun(context, "Kısıtlı ayar uyarısı alırsanız: Ayarlar > Sistem > Tam Ekran Sistem Ayarları üzerinden izin verin.");
                }
            } catch (Throwable t) {
                try {
                    Intent niyet = new Intent(Settings.ACTION_SETTINGS);
                    context.startActivity(niyet);
                } catch (Throwable t2) {
                    LauncherLog.w("BildirimYoneticisi", "Bildirim ayari acilamadi", t2);
                }
            }
        }
    }

    public static int getBildirimSayisi(Context context, String paketAdi) {
        if (getRozetTuru(context) == ROZET_KAPALI) return 0;
        if (Build.VERSION.SDK_INT < 18) return 0;
        return BildirimDinleyiciServisi.getBildirimSayisi(paketAdi);
    }
}

