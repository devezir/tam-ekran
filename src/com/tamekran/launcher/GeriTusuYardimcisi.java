package com.tamekran.launcher;

import android.app.Activity;
import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/**
 * Tam Ekran - Android 13+ (API 33+) OnBackInvokedCallback Yardımcısı
 * Android 2.2 - 12 cihazlarda VerifyError oluşmaması için ayrı sınıfa izole edilmiştir.
 */
public final class GeriTusuYardimcisi {

    private GeriTusuYardimcisi() {}

    public static void kaydet(Activity activity, final Runnable geriAksiyonu) {
        if (Build.VERSION.SDK_INT < 33 || activity == null || geriAksiyonu == null) {
            return;
        }
        try {
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            geriAksiyonu.run();
                        }
                    }
            );
        } catch (Throwable t) {
            LauncherLog.d("GeriTusuYardimcisi", "OnBackInvokedCallback kaydedilemedi: " + t.getMessage());
        }
    }
}
