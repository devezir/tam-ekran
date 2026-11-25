package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

/**
 * Tam Ekran - Evrensel Ekran Kilitleyici
 * Android 2.2'den Android 17'ye (Xiaomi HyperOS / Samsung OneUI / Pixel) kadar çift tıkla kilitlemeyi çözer.
 */
public class EkranKilitleyici {

    /**
     * Cihazda herhangi bir kilitleme yönteminin aktif olup olmadığını denetler
     */
    public static boolean kilitYetkisiVarMi(Context context) {
        if (context == null) return false;

        // 1. Android 9+ Erişilebilirlik kontrolü
        if (Build.VERSION.SDK_INT >= 28 && KilitErisilebilirlikServisi.ornek != null) {
            return true;
        }

        // 2. Cihaz Yöneticisi kontrolü
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName bilesen = new ComponentName(context, CihazYoneticiAlicisi.class);
            if (dpm != null && dpm.isAdminActive(bilesen)) {
                return true;
            }
        } catch (Throwable t) {
            LauncherLog.d("EkranKilitleyici", "DPM admin kontrol hatasi: " + t.getMessage());
        }

        return false;
    }

    /**
     * Ekranı anında kilitler. İzin yoksa kullanıcıya seçenekli izin diyalogu açar.
     */
    public static void kilitle(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        // 1. YÖNTEM: Android 9.0 (API 28) - Android 17 Erişilebilirlik Servisi (Parmak izini bozmaz)
        if (Build.VERSION.SDK_INT >= 28) {
            boolean kilitlendi = KilitErisilebilirlikServisi.ekraniKilitle();
            if (kilitlendi) {
                return;
            }
        }

        // 2. YÖNTEM: Cihaz Yöneticisi (Device Admin) - Tüm Android sürümleri (API 8 - API 36+)
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName bilesen = new ComponentName(activity, CihazYoneticiAlicisi.class);
            if (dpm != null && dpm.isAdminActive(bilesen)) {
                dpm.lockNow();
                return;
            }
        } catch (Throwable t) {
            LauncherLog.w("EkranKilitleyici", "DPM lockNow hatasi", t);
        }

        // 3. İki yöntem de henüz yetkilendirilmemişse: Kullanıcıya rehberli izin diyalogu göster
        izinDiyaloguGoster(activity);
    }

    /**
     * Kullanıcıya çift tıklamayla ekran kilitleme iznini açması için seçenekler sunar
     */
    public static void izinDiyaloguGoster(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        String mesaj = "Çift dokunarak ekranı kilitleyebilmek için sistem izni gereklidir.\n\n"
            + "1. Erişilebilirlik İzni (Önerilen - Parmak izi okuyucuyu devre dışı bırakmaz)\n"
            + "2. Cihaz Yöneticisi (Klasik hızlı kilit)\n\n"
            + "Xiaomi / HyperOS Notu: Erişilebilirlik ekranında 'Kısıtlanmış Ayar' uyarısı alırsanız; "
            + "Ayarlar > Uygulamalar > Tam Ekran > Sağ üstteki 3 nokta > 'Kısıtlanmış ayarlara izin ver' seçeneğini açınız.";

        new AlertDialog.Builder(activity)
            .setTitle("Ekran Kilitleme İzni Gerekli")
            .setMessage(mesaj)
            .setPositiveButton("Erişilebilirlik İzni Ver", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent niyet = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        activity.startActivity(niyet);
                        ArayuzYardimcisi.toastUzun(activity, "Lütfen listeden 'Tam Ekran'ı bulun ve etkinleştirin.");
                    } catch (Throwable t) {
                        ArayuzYardimcisi.toast(activity, "Erişilebilirlik ayarları açılamadı.");
                    }
                }
            })
            .setNeutralButton("Cihaz Yöneticisi", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        ComponentName bilesen = new ComponentName(activity, CihazYoneticiAlicisi.class);
                        Intent niyet = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        niyet.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, bilesen);
                        niyet.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Tam Ekran: Çift dokunuşla ekranı kilitlemek için kullanılır.");
                        activity.startActivity(niyet);
                    } catch (Throwable t) {
                        ArayuzYardimcisi.toast(activity, "Cihaz yöneticisi açılamadı.");
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }
}
