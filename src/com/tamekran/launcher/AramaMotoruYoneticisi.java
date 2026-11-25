package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

/**
 * Tam Ekran - Arama Motoru ve Sağlayıcı Yöneticisi
 * Masaüstü arama çubuğunun hedefini belirler: Cihaz İçi Uygulamalar, Google, DuckDuckGo, Yandex veya Bing.
 * Android 2.2 - 17 tam uyumlu.
 */
public class AramaMotoruYoneticisi {

    public static final String PREF_ARAMA_MOTORU = "arama_motoru_secimi";

    public static final int MOTOR_YEREL = 0;
    public static final int MOTOR_GOOGLE = 1;
    public static final int MOTOR_DUCKDUCKGO = 2;
    public static final int MOTOR_YANDEX = 3;
    public static final int MOTOR_BING = 4;

    public static final String[] MOTOR_ISIMLERI = new String[]{
        "📱 Cihaz İçi Uygulama Menüsü",
        "🌐 Google Arama",
        "🦆 DuckDuckGo (Gizli & İzleyicisiz)",
        "🟡 Yandex Arama",
        "🔷 Microsoft Bing"
    };

    public static int getAktifMotor(Context context) {
        if (context == null) return MOTOR_YEREL;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_ARAMA_MOTORU, MOTOR_YEREL);
    }

    public static void setAktifMotor(Context context, int motor) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit().putInt(PREF_ARAMA_MOTORU, motor));
    }

    public static void webAramasiBaslat(Activity activity, String aranan) {
        if (activity == null || aranan == null || aranan.trim().length() == 0) return;
        int motor = getAktifMotor(activity);
        String url;

        switch (motor) {
            case MOTOR_DUCKDUCKGO:
                url = "https://duckduckgo.com/?q=" + Uri.encode(aranan.trim());
                break;
            case MOTOR_YANDEX:
                url = "https://yandex.com.tr/search/?text=" + Uri.encode(aranan.trim());
                break;
            case MOTOR_BING:
                url = "https://www.bing.com/search?q=" + Uri.encode(aranan.trim());
                break;
            case MOTOR_GOOGLE:
            default:
                url = "https://www.google.com/search?q=" + Uri.encode(aranan.trim());
                break;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(intent);
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(activity, "Tarayıcı açılamadı: " + t.getMessage());
        }
    }
}

