package com.tamekran.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Tam Ekran - Merkezi Ayar Diyalogları Yardımcısı (Settings Dialog Helper)
 * Ayarlar ekranındaki tekrarlayan AlertDialog.Builder şablonlarını tek merkezde toplar (DRY Prensibi).
 */
public final class AyarDiyalogYardimcisi {

    private AyarDiyalogYardimcisi() {}

    public interface SecimDinleyicisi {
        void onSecildi(int which);
    }

    public interface MetinGirdiDinleyicisi {
        void onMetinGirildi(String metin);
    }

    /**
     * Tıklanabilir tek liste öğeli seçim diyalogu açar.
     */
    public static void listeSecimDiyalogu(
        Context context,
        CharSequence baslik,
        CharSequence[] secenekler,
        final SecimDinleyicisi dinleyici
    ) {
        if (context == null || secenekler == null) return;
        new AlertDialog.Builder(context)
            .setTitle(baslik)
            .setItems(secenekler, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dinleyici != null) {
                        dinleyici.onSecildi(which);
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    /**
     * Radyo butonlu (Single Choice) kalıcı seçim diyalogu açar.
     */
    public static void tekliSecimDiyalogu(
        Context context,
        String baslik,
        String[] secenekler,
        int aktifIndeks,
        final SecimDinleyicisi dinleyici
    ) {
        if (context == null || secenekler == null) return;
        new AlertDialog.Builder(context)
            .setTitle(baslik)
            .setSingleChoiceItems(secenekler, aktifIndeks, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dinleyici != null) {
                        dinleyici.onSecildi(which);
                    }
                    dialog.dismiss();
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    /**
     * Kullanıcıdan eylem onayı isteyen basit onay diyalogu açar.
     */
    public static void onayDiyalogu(
        Context context,
        String baslik,
        String mesaj,
        String onayYazisi,
        final Runnable onOnaylandi
    ) {
        if (context == null) return;
        new AlertDialog.Builder(context)
            .setTitle(baslik)
            .setMessage(mesaj)
            .setPositiveButton(onayYazisi, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (onOnaylandi != null) {
                        onOnaylandi.run();
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    /**
     * Kullanıcıdan metin girişi alan tek tip şık iletişim kutusu açar (DRY Prensibi).
     */
    public static void metinGirdiDiyalogu(
        Context context,
        CharSequence baslik,
        CharSequence varsayilanMetin,
        CharSequence ipucu,
        final MetinGirdiDinleyicisi dinleyici
    ) {
        if (context == null) return;
        final android.widget.EditText girdi = new android.widget.EditText(context);
        if (varsayilanMetin != null) {
            girdi.setText(varsayilanMetin);
            try {
                girdi.setSelection(varsayilanMetin.length());
            } catch (Exception e) {
                LauncherLog.d("AyarDiyalogYardimcisi", "Imlec konumu ayarlanamadi: " + e.getMessage());
            }
        }
        if (ipucu != null) {
            girdi.setHint(ipucu);
        }
        int pad = ArayuzYardimcisi.dpToPx(context, 16);
        android.widget.FrameLayout kapsayici = new android.widget.FrameLayout(context);
        kapsayici.setPadding(pad, pad / 2, pad, 0);
        kapsayici.addView(girdi);

        new AlertDialog.Builder(context)
            .setTitle(baslik)
            .setView(kapsayici)
            .setPositiveButton(R.string.tamam, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (dinleyici != null) {
                        String metin = girdi.getText() != null ? girdi.getText().toString().trim() : "";
                        dinleyici.onMetinGirildi(metin);
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }
}
