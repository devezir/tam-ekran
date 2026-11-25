package com.tamekran.launcher;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.HashMap;
import java.util.Map;

/**
 * Tam Ekran - Bildirim Rozetleri Dinleyici Servisi (Notification Badges Engine)
 * Nova Launcher / TeslaUnread benzeri dinamik bildirim sayaçları sağlar.
 * Android 4.3 (API 18) - Android 17 (API 36+) destekli.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BildirimDinleyiciServisi extends NotificationListenerService {

    public interface BildirimGuncellemeDinleyicisi {
        void onBildirimlerDegisti();
    }

    private static final Map<String, Integer> bildirimSayilari = new HashMap<String, Integer>();
    private static BildirimGuncellemeDinleyicisi dinleyici;
    private static boolean servisBagli = false;

    public static void setGuncellemeDinleyicisi(BildirimGuncellemeDinleyicisi d) {
        dinleyici = d;
    }

    public static boolean isServisAktif() {
        return servisBagli;
    }

    public static int getBildirimSayisi(String paketAdi) {
        if (paketAdi == null) return 0;
        synchronized (bildirimSayilari) {
            Integer sayi = bildirimSayilari.get(paketAdi);
            return sayi != null ? sayi : 0;
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        servisBagli = true;
        sayilariYenile();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        servisBagli = false;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        sayilariYenile();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        sayilariYenile();
    }

    private void sayilariYenile() {
        if (Build.VERSION.SDK_INT < 18) return;

        try {
            StatusBarNotification[] aktifler = getActiveNotifications();
            Map<String, Integer> yeniHarita = new HashMap<String, Integer>();

            if (aktifler != null) {
                for (StatusBarNotification sbn : aktifler) {
                    if (sbn == null) continue;
                    // Kalıcı sistem bildirimlerini (Ongoing / Foreground) rozet sayısına dahil etme
                    if (sbn.isOngoing()) continue;

                    String paket = sbn.getPackageName();
                    if (paket != null) {
                        int mevcut = yeniHarita.containsKey(paket) ? yeniHarita.get(paket) : 0;
                        yeniHarita.put(paket, mevcut + 1);
                    }
                }
            }

            synchronized (bildirimSayilari) {
                bildirimSayilari.clear();
                bildirimSayilari.putAll(yeniHarita);
            }

            if (dinleyici != null) {
                dinleyici.onBildirimlerDegisti();
            }
        } catch (Throwable t) {
            LauncherLog.w("BildirimDinleyiciServisi", "tazele hatasi", t);
        }
    }
}
