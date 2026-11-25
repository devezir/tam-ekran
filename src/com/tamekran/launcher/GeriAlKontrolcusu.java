package com.tamekran.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

/**
 * Masaüstünden silinen öğeler için Geri Al (Undo) Çubuğu Kontrolcüsü (SRP & DRY).
 */
public class GeriAlKontrolcusu {

    private final Activity activity;
    private final View barGeriAl;
    private final TextView yaziGeriAlBilgi;
    private Runnable geriAlKapatRunnable;

    public GeriAlKontrolcusu(Activity activity, View barGeriAl, TextView yaziGeriAlBilgi) {
        this.activity = activity;
        this.barGeriAl = barGeriAl;
        this.yaziGeriAlBilgi = yaziGeriAlBilgi;
    }

    public void goster(String ogeEtiketi) {
        if (barGeriAl == null) return;
        if (yaziGeriAlBilgi != null) {
            yaziGeriAlBilgi.setText(ogeEtiketi + " kaldırıldı");
        }
        barGeriAl.bringToFront();
        barGeriAl.setVisibility(View.VISIBLE);
        if (Build.VERSION.SDK_INT >= 11) {
            barGeriAl.setAlpha(0.0f);
            barGeriAl.animate().alpha(1.0f).setDuration(200).start();
        }
        if (geriAlKapatRunnable != null) {
            barGeriAl.removeCallbacks(geriAlKapatRunnable);
        }
        geriAlKapatRunnable = new Runnable() {
            @Override
            public void run() {
                gizle();
            }
        };
        barGeriAl.postDelayed(geriAlKapatRunnable, 5000);
    }

    public void gizle() {
        if (barGeriAl == null || barGeriAl.getVisibility() != View.VISIBLE) return;
        if (Build.VERSION.SDK_INT >= 11) {
            barGeriAl.animate().alpha(0.0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (barGeriAl != null) barGeriAl.setVisibility(View.GONE);
                }
            }).start();
        } else {
            barGeriAl.setVisibility(View.GONE);
        }
    }

    public void geriAlYap(Runnable onGeriAlindi) {
        if (activity != null && GeriAlYoneticisi.geriAl(activity)) {
            ArayuzYardimcisi.toast(activity, "Öğe geri yüklendi.");
            if (onGeriAlindi != null) {
                onGeriAlindi.run();
            }
        }
        gizle();
    }
}
