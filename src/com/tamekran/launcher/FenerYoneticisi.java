package com.tamekran.launcher;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.widget.Toast;

/**
 * Tam Ekran - Hızlı Fener (El Feneri) Yöneticisi
 * Ekran hareketleri, sallama veya simge kaydırmayla cihazın flaşını açıp kapatır.
 * Saf Java, harici kütüphanesiz, Android 6+ CameraManager ve eski cihaz fallback destekli.
 */
public class FenerYoneticisi {

    private static boolean fenerAcik = false;

    public static boolean isFenerAcik() {
        return fenerAcik;
    }

    public static void toggle(Context context) {
        if (context == null) return;
        setFener(context, !fenerAcik);
    }

    public static void setFener(Context context, boolean acik) {
        if (context == null) return;

        try {
            if (Build.VERSION.SDK_INT >= 23) {
                CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (cm != null) {
                    String[] ids = cm.getCameraIdList();
                    if (ids != null && ids.length > 0) {
                        for (String id : ids) {
                            try {
                                cm.setTorchMode(id, acik);
                                fenerAcik = acik;
                                ArayuzYardimcisi.toast(context, acik ? "🔦 Fener Açıldı" : "🔦 Fener Kapatıldı");
                                return;
                            } catch (Throwable t) {
                                LauncherLog.d("FenerYoneticisi", "Kamera ID torch basarisiz: " + t.getMessage());
                            }
                        }
                    }
                }
            } else {
                // Android 5.1 ve altı cihazlar için klasik Camera fallback
                try {
                    android.hardware.Camera cam = android.hardware.Camera.open();
                    android.hardware.Camera.Parameters p = cam.getParameters();
                    p.setFlashMode(acik ? android.hardware.Camera.Parameters.FLASH_MODE_TORCH : android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                    cam.setParameters(p);
                    if (acik) {
                        cam.startPreview();
                    } else {
                        cam.stopPreview();
                        cam.release();
                    }
                    fenerAcik = acik;
                    ArayuzYardimcisi.toast(context, acik ? "🔦 Fener Açıldı" : "🔦 Fener Kapatıldı");
                    return;
                } catch (Throwable t) {
                    LauncherLog.d("FenerYoneticisi", "Legacy camera torch basarisiz: " + t.getMessage());
                }
            }
            ArayuzYardimcisi.toast(context, "Cihazda fener/flaş donanımı bulunamadı.");
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(context, "Fener çalıştırılamadı: " + t.getMessage());
        }
    }
}
