package com.tamekran.launcher;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Vibrator;
import android.view.View;

/**
 * Tam Ekran - Sürükle ve Bırak (Drag & Drop) Motoru
 * Android 2.2'den Android 17'ye kadar kesintisiz dokunsal geri bildirim ve sürükleme sağlar.
 */
public class SurukleBirakYardimcisi {

    public static class SimgeGolgesi extends View.DragShadowBuilder {
        private Drawable simge;

        public SimgeGolgesi(View gorunum, Drawable simge) {
            super(gorunum);
            this.simge = simge;
        }

        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {
            int genislik = getView() != null && getView().getWidth() > 0 ? getView().getWidth() : 120;
            int yukseklik = getView() != null && getView().getHeight() > 0 ? getView().getHeight() : 120;
            size.set(genislik, yukseklik);
            touch.set(genislik / 2, yukseklik / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            try {
                if (getView() != null) {
                    getView().draw(canvas);
                    return;
                }
            } catch (Throwable t) {
                LauncherLog.d("SurukleBirak", "onDrawShadow view draw basarisiz: " + t.getMessage());
            }
            if (simge != null && getView() != null) {
                int w = getView().getWidth();
                int h = getView().getHeight();
                int size = Math.min(w, h);
                int l = (w - size) / 2;
                int t = (h - size) / 2;
                simge.setBounds(l, t, l + size, t + size);
                simge.draw(canvas);
            }
        }
    }

    public static void titresim(Context context, long milisaniye) {
        if (context == null) return;
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && (Build.VERSION.SDK_INT < 11 || v.hasVibrator())) {
                v.vibrate(milisaniye);
            }
        } catch (Throwable t) {
            LauncherLog.d("SurukleBirak", "Titresim hatasi: " + t.getMessage());
        }
    }

    public static boolean suruklemeyiBaslat(View gorunum, UygulamaBilgisi app) {
        if (gorunum == null || app == null) return false;
        return suruklemeyiBaslat(gorunum, new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_CEKMECE));
    }

    public static boolean suruklemeyiBaslat(View gorunum, SuruklemeVerisi veri) {
        if (gorunum == null || veri == null || veri.app == null) return false;

        titresim(gorunum.getContext(), 40);

        if (Build.VERSION.SDK_INT >= 11) {
            ClipData clip = ClipData.newPlainText("tamekran_pkg", veri.app.paketAdi);
            SimgeGolgesi golge = new SimgeGolgesi(gorunum, veri.app.simge);

            if (Build.VERSION.SDK_INT >= 24) {
                return gorunum.startDragAndDrop(clip, golge, veri, 0);
            } else {
                return gorunum.startDrag(clip, golge, veri, 0);
            }
        }
        return false;
    }
}
