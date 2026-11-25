package com.tamekran.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;

/**
 * AnaEkranActivity için Kalibratör Intentlerini Yönlendirici (SRP & DRY).
 */
public final class KalibrasyonIntentYoneticisi {

    private KalibrasyonIntentYoneticisi() {}

    public static void kontrolEt(
            final AnaEkranActivity activity,
            final Handler zamanlayici,
            final Intent intent) {

        if (activity == null || intent == null || zamanlayici == null) return;

        if (intent.getBooleanExtra(AnaEkranActivity.EXTRA_AC_GUVENLI_ALAN, false)) {
            intent.removeExtra(AnaEkranActivity.EXTRA_AC_GUVENLI_ALAN);
            zamanlayici.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GuvenliAlanKalibratoru.goster(activity);
                }
            }, 300);
        } else if (intent.getBooleanExtra(AnaEkranActivity.EXTRA_AC_IZGARA_KALIBRATORU, false)) {
            final int mod = intent.getIntExtra(AnaEkranActivity.EXTRA_IZGARA_MODU, IzgaraKalibratoru.MOD_MASAUSTU);
            intent.removeExtra(AnaEkranActivity.EXTRA_AC_IZGARA_KALIBRATORU);
            zamanlayici.postDelayed(new Runnable() {
                @Override
                public void run() {
                    IzgaraKalibratoru.goster(activity, mod);
                }
            }, 300);
        } else if (intent.getBooleanExtra(AnaEkranActivity.EXTRA_AC_GORSEL_AYAR, false)) {
            final int tur = intent.getIntExtra(AnaEkranActivity.EXTRA_GORSEL_AYAR_TURU, 1);
            intent.removeExtra(AnaEkranActivity.EXTRA_AC_GORSEL_AYAR);
            final int hedefMod = (tur == 2) ? IzgaraKalibratoru.MOD_CEKMECE
                    : ((tur == 3 || tur == 4) ? IzgaraKalibratoru.MOD_DOCK : IzgaraKalibratoru.MOD_MASAUSTU);
            zamanlayici.postDelayed(new Runnable() {
                @Override
                public void run() {
                    IzgaraKalibratoru.goster(activity, hedefMod);
                }
            }, 300);
        } else if (intent.getBooleanExtra(AnaEkranActivity.EXTRA_AC_DOCK_KALIBRATORU, false)) {
            intent.removeExtra(AnaEkranActivity.EXTRA_AC_DOCK_KALIBRATORU);
            zamanlayici.postDelayed(new Runnable() {
                @Override
                public void run() {
                    IzgaraKalibratoru.goster(activity, IzgaraKalibratoru.MOD_DOCK);
                }
            }, 300);
        }
    }
}
