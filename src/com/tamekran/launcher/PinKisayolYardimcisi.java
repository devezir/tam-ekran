package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;

/**
 * Tam Ekran - Android 7.1+ / 8.0+ (API 25/26+) Pinned & Deep Shortcut Yardimcisi
 * Eski Android 2.2 - 7.0 cihazlarda VerifyError olusmamasi icin izole edilmistir;
 * yalnizca Build.VERSION.SDK_INT >= 25 / 26 kontrolunden sonra cagrilir.
 */
public final class PinKisayolYardimcisi {

    private PinKisayolYardimcisi() {}

    public static boolean baslatPinnedShortcut(Context context, String paketAdi, String shortcutId) {
        if (Build.VERSION.SDK_INT < 25 || context == null || paketAdi == null || shortcutId == null) {
            return false;
        }
        try {
            LauncherApps la = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (la == null) return false;
            la.startShortcut(paketAdi, shortcutId, null, null, Process.myUserHandle());
            return true;
        } catch (Exception e) {
            LauncherLog.w("PinKisayolYardimcisi", "Pinned shortcut baslatilamadi: " + paketAdi + "/" + shortcutId, e);
            return false;
        }
    }

    public static class PinIstegiBilgisi {
        public LauncherApps.PinItemRequest request;
        public String paketAdi;
        public String shortcutId;
        public String baslik;
        public String yedekIntentUri;
        public Bitmap simge;
    }

    public static PinIstegiBilgisi pinIsteginiOku(Activity activity, Intent intent) {
        if (Build.VERSION.SDK_INT < 26 || activity == null || intent == null) {
            return null;
        }
        try {
            LauncherApps la = (LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (la == null) return null;
            LauncherApps.PinItemRequest req = la.getPinItemRequest(intent);
            if (req == null || !req.isValid()) return null;
            if (req.getRequestType() != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
                return null;
            }
            ShortcutInfo info = req.getShortcutInfo();
            if (info == null) return null;

            PinIstegiBilgisi sonuc = new PinIstegiBilgisi();
            sonuc.request = req;
            sonuc.paketAdi = info.getPackage();
            sonuc.shortcutId = info.getId();

            CharSequence label = info.getShortLabel();
            if (label == null || label.length() == 0) {
                label = info.getLongLabel();
            }
            sonuc.baslik = (label != null && label.length() > 0) ? label.toString().trim() : sonuc.paketAdi;

            try {
                Intent yedekNiyet = info.getIntent();
                if (yedekNiyet != null) {
                    sonuc.yedekIntentUri = yedekNiyet.toUri(Intent.URI_INTENT_SCHEME);
                }
            } catch (Exception e) {
                LauncherLog.d("PinKisayolYardimcisi", "Yedek intent okunamadi: " + e.getMessage());
            }

            try {
                int dpi = activity.getResources().getDisplayMetrics().densityDpi;
                Drawable iconDr = la.getShortcutIconDrawable(info, dpi);
                if (iconDr != null) {
                    sonuc.simge = drawableToBitmap(iconDr, 192);
                }
            } catch (Exception e) {
                LauncherLog.d("PinKisayolYardimcisi", "Shortcut simgesi alinamadi: " + e.getMessage());
            }
            return sonuc;
        } catch (Exception e) {
            LauncherLog.w("PinKisayolYardimcisi", "PinItemRequest okunamadi", e);
            return null;
        }
    }

    public static boolean pinIsteginiOnaylaVeKaydet(Context context, PinIstegiBilgisi bilgi, int hedefSayfa) {
        if (Build.VERSION.SDK_INT < 26 || context == null || bilgi == null || bilgi.request == null) {
            return false;
        }
        try {
            if (!bilgi.request.accept()) {
                return false;
            }
            return KisayolYoneticisi.pinnedKisayolKaydet(
                    context,
                    bilgi.paketAdi,
                    bilgi.shortcutId,
                    bilgi.baslik,
                    bilgi.yedekIntentUri,
                    bilgi.simge,
                    hedefSayfa
            );
        } catch (Exception e) {
            LauncherLog.w("PinKisayolYardimcisi", "PinItemRequest onaylanamadi", e);
            return false;
        }
    }

    private static Bitmap drawableToBitmap(Drawable drawable, int maxPx) {
        if (drawable instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
            if (bmp != null) return bmp;
        }
        int w = drawable.getIntrinsicWidth() > 0 ? Math.min(maxPx, drawable.getIntrinsicWidth()) : maxPx;
        int h = drawable.getIntrinsicHeight() > 0 ? Math.min(maxPx, drawable.getIntrinsicHeight()) : maxPx;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }
}
