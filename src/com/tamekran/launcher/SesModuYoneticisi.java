package com.tamekran.launcher;

import android.content.Context;
import android.media.AudioManager;
import android.widget.Toast;

/**
 * Tam Ekran - Hızlı Ses Profili Yöneticisi
 * Ekran hareketleri, sallama veya simge kaydırmayla cihazın ses profilini
 * Normal -> Titreşim -> Sessiz döngüsünde anında değiştirir.
 */
public class SesModuYoneticisi {

    public static void sonrakiModaGec(Context context) {
        if (context == null) return;

        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return;

            int mevcut = am.getRingerMode();
            if (mevcut == AudioManager.RINGER_MODE_NORMAL) {
                am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                ArayuzYardimcisi.toast(context, "📳 Ses Profili: Titreşim");
            } else if (mevcut == AudioManager.RINGER_MODE_VIBRATE) {
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                ArayuzYardimcisi.toast(context, "🔇 Ses Profili: Sessiz");
            } else {
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                ArayuzYardimcisi.toast(context, "🔔 Ses Profili: Normal (Ses Açık)");
            }
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(context, "Ses modu değiştirilemedi (Rahatsız Etmeyin izni gerekebilir).");
        }
    }
}
