package com.tamekran.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/**
 * Tam Ekran - Android 8.0+ (API 26+) Aninda Pinned Shortcut Onay Ekrani
 * Google Drive, Chrome, WhatsApp, Dosyalar vb. uygulamalardan gelen "Ana ekrana ekle"
 * (ACTION_CONFIRM_PIN_SHORTCUT) istegini kullaniciya ikinci bir diyalog kacirma riski
 * yasatmadan aninda kabul eder ve masaustune ekler.
 */
public class PinKisayolOnayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        istegiIsle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        istegiIsle(intent);
    }

    private void istegiIsle(Intent intent) {
        if (Build.VERSION.SDK_INT >= 26 && intent != null) {
            PinKisayolYardimcisi.PinIstegiBilgisi bilgi = PinKisayolYardimcisi.pinIsteginiOku(this, intent);
            if (bilgi != null) {
                int hedefSayfa = MasaustuYoneticisi.getVarsayilanSayfa(this);
                if (PinKisayolYardimcisi.pinIsteginiOnaylaVeKaydet(this, bilgi, hedefSayfa)) {
                    ArayuzYardimcisi.toast(this, getString(R.string.kisayol_eklendi));
                }
            }
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
