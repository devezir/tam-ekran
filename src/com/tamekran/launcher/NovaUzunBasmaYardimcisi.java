package com.tamekran.launcher;

import android.app.Activity;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

/**
 * Tam Ekran - Nova Launcher Tarzı İki Aşamalı Uzun Basma (Pre-Drag) Motoru
 * 1) Uzun basmada hemen sistem sürüklemesi yerine simge üstünde yüzen Popup menü açar.
 * 2) Kullanıcı parmağını oynatmadan kaldırırsa Popup menü açık kalır.
 * 3) Kullanıcı parmağını kaldırmadan touchSlop eşiğini aşarsa Popup anında kapanır ve sürükleme başlar.
 */
public final class NovaUzunBasmaYardimcisi {

    public interface UzunBasmaAksiyonu {
        void onPopupGoster(View kaynakView);
        boolean onSuruklemeBaslat(View kaynakView);
        void onKisaTiklama(View kaynakView);
    }

    private NovaUzunBasmaYardimcisi() {}

    public static void bagla(final Activity activity, final View hedefView, final UzunBasmaAksiyonu aksiyon) {
        bagla(activity, hedefView, aksiyon, null);
    }

    public static void bagla(
            final Activity activity,
            final View hedefView,
            final UzunBasmaAksiyonu aksiyon,
            final Runnable yukariKaydirmaAksiyonu) {
        if (hedefView == null || aksiyon == null) return;

        final int touchSlop = (activity != null)
                ? ViewConfiguration.get(activity).getScaledTouchSlop()
                : 24;
        final int yukariKaydirmaEsigi = Math.max(touchSlop * 2, 36);
        final float[] downXY = new float[2];
        final boolean[] durum = new boolean[3]; // [0]=uzunBasildi, [1]=suruklemeBasladi, [2]=yukariKaydirildi

        hedefView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (durum[0] || durum[1] || durum[2]) {
                    durum[0] = false;
                    durum[1] = false;
                    durum[2] = false;
                    return;
                }
                aksiyon.onKisaTiklama(v);
            }
        });

        hedefView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (durum[2]) return true;
                durum[0] = true;
                durum[1] = false;
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
                SurukleBirakYardimcisi.titresim(v.getContext(), 35);
                if (Build.VERSION.SDK_INT >= 11) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(110).start();
                }
                aksiyon.onPopupGoster(v);
                return true;
            }
        });

        hedefView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    downXY[0] = event.getRawX();
                    downXY[1] = event.getRawY();
                    durum[0] = false;
                    durum[1] = false;
                    durum[2] = false;
                    return false;
                }

                if (!durum[0] && !durum[1] && !durum[2] && yukariKaydirmaAksiyonu != null && action == MotionEvent.ACTION_MOVE) {
                    float dx = event.getRawX() - downXY[0];
                    float dy = event.getRawY() - downXY[1];
                    if (dy < -yukariKaydirmaEsigi && Math.abs(dy) > Math.abs(dx) * 1.4f) {
                        durum[2] = true;
                        v.cancelLongPress();
                        yukariKaydirmaAksiyonu.run();
                        return true;
                    }
                }

                if (durum[0] && !durum[1]) {
                    if (action == MotionEvent.ACTION_MOVE) {
                        float dx = event.getRawX() - downXY[0];
                        float dy = event.getRawY() - downXY[1];
                        if ((dx * dx + dy * dy) > (touchSlop * touchSlop)) {
                            durum[1] = true;
                            olcekSifirla(v);
                            HizliEylemlerDiyalogu.kapatAktifPopup();
                            aksiyon.onSuruklemeBaslat(v);
                            ViewParent parent = v.getParent();
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(false);
                            }
                            return true;
                        }
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        olcekSifirla(v);
                        ViewParent parent = v.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(false);
                        }
                        if (action == MotionEvent.ACTION_UP) {
                            HizliEylemlerDiyalogu.popupEtkilesimeAc();
                            return true;
                        } else {
                            HizliEylemlerDiyalogu.kapatAktifPopup();
                        }
                    }
                }
                return false;
            }
        });
    }

    private static void olcekSifirla(View v) {
        if (v != null && Build.VERSION.SDK_INT >= 11) {
            v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
        }
    }
}
