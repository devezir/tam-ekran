package com.tamekran.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Tam Ekran - Eski Android (API 8 - 25) INSTALL_SHORTCUT Alicisi
 * Android 2.2'den 7.1'e kadar (ve eski uygulamalar icin 8.0+ uzerinde)
 * com.android.launcher.action.INSTALL_SHORTCUT yayinlarini yakalar ve masaustune ekler.
 */
public class EskiKisayolAlicisi extends BroadcastReceiver {

    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        if (!ACTION_INSTALL_SHORTCUT.equals(intent.getAction())) return;

        int hedefSayfa = MasaustuYoneticisi.getVarsayilanSayfa(context);
        if (KisayolYoneticisi.kisayolSonucunuKaydet(context, intent, hedefSayfa)) {
            AyarBankasi.degisiklikDamgasiGuncelle(context);
            ArayuzYardimcisi.toast(context, context.getString(R.string.kisayol_eklendi));
        }
    }
}
