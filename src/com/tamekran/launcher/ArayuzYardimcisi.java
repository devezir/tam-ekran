package com.tamekran.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Tam Ekran - Merkezi Arayüz ve Sistem Yardımcısı (UI Helpers)
 * Ekran yoğunluğu (dp to px), sistem intentleri, pencereler ve ortak uyarıları
 * tek noktada toplayarak kod tekrarlarını önler (DRY Prensibi).
 */
public final class ArayuzYardimcisi {

    private ArayuzYardimcisi() {}

    public static final String UYARI_MASAUSTU_KILITLI = "Masaüstü kilitli. Değişiklik yapmak için Ayarlar'dan kilidi açın.";

    /**
     * Kullanıcıya ana iş parçacığı ve null güvencesiyle kısa süreli Toast bildirimi gösterir (DRY SSOT).
     */
    public static void toast(final Context context, final String mesaj) {
        if (context == null || mesaj == null || mesaj.trim().isEmpty()) return;
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            try {
                Toast.makeText(context, mesaj, Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                LauncherLog.d("ArayuzYardimcisi", "Toast gosterilemedi: " + t.getMessage());
            }
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(context, mesaj, Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        LauncherLog.d("ArayuzYardimcisi", "Async Toast gosterilemedi: " + t.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Kullanıcıya ana iş parçacığı ve null güvencesiyle uzun süreli Toast bildirimi gösterir (DRY SSOT).
     */
    public static void toastUzun(final Context context, final String mesaj) {
        if (context == null || mesaj == null || mesaj.trim().isEmpty()) return;
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            try {
                Toast.makeText(context, mesaj, Toast.LENGTH_LONG).show();
            } catch (Throwable t) {
                LauncherLog.d("ArayuzYardimcisi", "Toast gosterilemedi: " + t.getMessage());
            }
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(context, mesaj, Toast.LENGTH_LONG).show();
                    } catch (Throwable t) {
                        LauncherLog.d("ArayuzYardimcisi", "Async Toast gosterilemedi: " + t.getMessage());
                    }
                }
            });
        }
    }

    /**
     * DP değerini cihazın piksel (PX) değerine dönüştürür.
     */
    public static int dpToPx(Context context, float dp) {
        if (context == null) return (int) dp;
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * DP değerini Resources nesnesi üzerinden piksel (PX) değerine dönüştürür.
     */
    public static int dpToPx(Resources res, float dp) {
        if (res == null) return (int) dp;
        float density = res.getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public static int dpToPx(Context context, int dp) {
        return dpToPx(context, (float) dp);
    }

    public static int pxToDp(Context context, int px) {
        if (context == null) return px;
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (px / (density > 0 ? density : 1.0f));
    }

    /**
     * Cihazın sistem durum çubuğu (status bar) yüksekliğini piksel (PX) olarak döner.
     * Cihaz kaynaklarından "status_bar_height" değerini güvenli şekilde okur.
     * Bulunamazsa varsayılan olarak 24dp döner.
     */
    public static int getDurumCubuguYuksekligi(Context context) {
        if (context == null) return 0;
        int sonuc = 0;
        try {
            int kaynakId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (kaynakId > 0) {
                sonuc = context.getResources().getDimensionPixelSize(kaynakId);
            }
        } catch (Throwable t) {
            LauncherLog.d("ArayuzYardimcisi", "status_bar_height okunamadi: " + t.getMessage());
        }
        if (sonuc <= 0) {
            sonuc = dpToPx(context, 24f);
        }
        return sonuc;
    }

    /**
     * Masaüstünün kilitli olup olmadığını denetler.
     * Eğer kilitli ise kullanıcıya standart Toast uyarısı gösterip true döndürür.
     */
    public static boolean kilitliMiVeUyar(Context context) {
        if (context == null) return false;
        if (AyarBankasi.isMasaustuKilitli(context)) {
            toast(context, context.getString(R.string.uyari_masaustu_kilitli));
            return true;
        }
        return false;
    }

    /**
     * İlgili paketin Google Play Store sayfasını açar.
     * Cihazda Play Store uygulaması yoksa güvenli bir şekilde web tarayıcısına yönlendirir.
     */
    public static void playStoreAc(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return;
        try {
            Intent niyet = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + paketAdi));
            if (!(context instanceof Activity)) {
                niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(niyet);
        } catch (Exception e) {
            try {
                Intent webNiyet = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + paketAdi));
                if (!(context instanceof Activity)) {
                    webNiyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(webNiyet);
            } catch (Exception e2) {
                LauncherLog.w("ArayuzYardimcisi", "Play Store ve Web market acilamadi: " + paketAdi, e2);
                toast(context, context.getString(R.string.magaza_acilamadi));
            }
        }
    }

    /**
     * Verilen paket adına ait uygulamanın varsayılan etkinliğini başlatır.
     */
    public static boolean uygulamaBaslat(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return false;
        try {
            Intent niyet = context.getPackageManager().getLaunchIntentForPackage(paketAdi);
            if (niyet != null) {
                niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(niyet);
                return true;
            }
        } catch (Throwable t) {
            LauncherLog.w("ArayuzYardimcisi", "Uygulama baslatilamadi: " + paketAdi, t);
        }
        return false;
    }

    private static boolean kalibratorKonumAltta = true;

    public static boolean isKalibratorKonumAltta() {
        return kalibratorKonumAltta;
    }

    public static void setKalibratorKonumAltta(boolean altta) {
        kalibratorKonumAltta = altta;
    }

    /**
     * Kalibratör ve canlı panel diyalogları için saydam arka plan, dim oranı ve alt/üst hizalama uygular.
     */
    public static void kalibratorPenceresiAyarla(Dialog diyalog, boolean konumAltta, float dimAmount) {
        kalibratorKonumAltta = konumAltta;
        if (diyalog == null) return;
        try {
            diyalog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception e) {
            LauncherLog.d("ArayuzYardimcisi", "requestWindowFeature atlandi: " + e.getMessage());
        }
        Window window = diyalog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(dimAmount);
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = konumAltta ? Gravity.BOTTOM : Gravity.TOP;
            wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(wlp);
        }
    }

    /**
     * Kalibratör diyalogunun yerleşimini (üst / alt) dinamik olarak günceller.
     */
    public static void kalibratorKonumGuncelle(Dialog diyalog, boolean konumAltta) {
        kalibratorKonumAltta = konumAltta;
        if (diyalog == null) return;
        Window w = diyalog.getWindow();
        if (w != null) {
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.gravity = konumAltta ? Gravity.BOTTOM : Gravity.TOP;
            w.setAttributes(lp);
        }
    }

    /**
     * Kalibratör penceresi konum düğmesini (Üste Al / Alta Al) bağlar (DRY).
     */
    public static void baglaKalibratorKonumDugmesi(final Dialog dialog, final Button dugmeKonum) {
        if (dugmeKonum == null || dialog == null) return;
        dugmeKonum.setText(kalibratorKonumAltta ? "📌 Üste Al" : "📌 Alta Al");
        dugmeKonum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kalibratorKonumAltta = !kalibratorKonumAltta;
                kalibratorKonumGuncelle(dialog, kalibratorKonumAltta);
                dugmeKonum.setText(kalibratorKonumAltta ? "📌 Üste Al" : "📌 Alta Al");
            }
        });
    }

    /**
     * Kalibratörlerdeki SeekBar ve +/- adımlama butonlarını tek merkezden bağlar (DRY).
     */
    public static void baglaAyarAdimlayici(
            final SeekBar seekBar,
            final Button dugmeEksi,
            final Button dugmeArti,
            final int[] deger,
            final int min,
            final int max,
            final Runnable onChange) {
        baglaAyarAdimlayici(seekBar, dugmeEksi, dugmeArti, deger, min, max, 1, onChange);
    }

    /**
     * Kalibratörlerdeki SeekBar ve +/- adımlama butonlarını adım boyutuyla bağlar (DRY).
     */
    public static void baglaAyarAdimlayici(
            final SeekBar seekBar,
            final Button dugmeEksi,
            final Button dugmeArti,
            final int[] deger,
            final int min,
            final int max,
            final int adim,
            final Runnable onChange) {
        final int adimBoyutu = adim <= 0 ? 1 : adim;
        if (seekBar != null) {
            seekBar.setMax(max - min);
            seekBar.setProgress(Math.max(0, Math.min(max - min, deger[0] - min)));
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean user) {
                    if (user) {
                        deger[0] = progress + min;
                        if (onChange != null) onChange.run();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });
        }
        if (dugmeEksi != null) {
            dugmeEksi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (deger[0] - adimBoyutu >= min) {
                        deger[0] -= adimBoyutu;
                    } else {
                        deger[0] = min;
                    }
                    if (seekBar != null) seekBar.setProgress(deger[0] - min);
                    if (onChange != null) onChange.run();
                }
            });
        }
        if (dugmeArti != null) {
            dugmeArti.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (deger[0] + adimBoyutu <= max) {
                        deger[0] += adimBoyutu;
                    } else {
                        deger[0] = max;
                    }
                    if (seekBar != null) seekBar.setProgress(deger[0] - min);
                    if (onChange != null) onChange.run();
                }
            });
        }
    }

    /**
     * Uygulamayı güvenli bir şekilde PIN sorgusu, kullanım sayacı ve hata yönetimi ile başlatır (DRY SSOT).
     */
    public static void guvenliUygulamaBaslat(final Activity activity, final UygulamaBilgisi app, final Runnable onBaslatildi) {
        if (activity == null || app == null) return;

        if (app.paketAdi != null && app.paketAdi.startsWith("kisayol:")) {
            final String hedefPaket = KisayolYoneticisi.kisayolHedefPaketGetir(activity, app.paketAdi);
            final Runnable kisayolGorevi = new Runnable() {
                @Override
                public void run() {
                    if (KisayolYoneticisi.kisayolBaslat(activity, app.paketAdi)) {
                        if (onBaslatildi != null) onBaslatildi.run();
                    } else {
                        toast(activity, activity.getString(R.string.baslatici_bulunamadi));
                    }
                }
            };
            if (hedefPaket != null && UygulamaKilidiYoneticisi.kilitliMi(activity, hedefPaket)) {
                UygulamaKilidiYoneticisi.pinSorgulaVeCalistir(activity, kisayolGorevi);
            } else {
                kisayolGorevi.run();
            }
            return;
        }

        Intent niyet = app.baslatmaNiyeti;
        if (niyet == null && app.paketAdi != null) {
            try {
                niyet = activity.getPackageManager().getLaunchIntentForPackage(app.paketAdi);
                if (niyet != null) {
                    niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.baslatmaNiyeti = niyet;
                }
            } catch (Throwable t) {
                LauncherLog.d("ArayuzYardimcisi", "Launch intent alinamadi: " + t.getMessage());
            }
        }

        if (niyet == null) {
            toast(activity, activity.getString(R.string.baslatici_bulunamadi));
            return;
        }

        final Intent calistirilacakNiyet = niyet;
        final Runnable baslatmaGorevi = new Runnable() {
            @Override
            public void run() {
                try {
                    KullanimSayaci.uygulamayiBaslatti(activity, app.paketAdi);
                    activity.startActivity(calistirilacakNiyet);
                    if (onBaslatildi != null) {
                        onBaslatildi.run();
                    }
                } catch (Exception e) {
                    try {
                        Intent fallback = activity.getPackageManager().getLaunchIntentForPackage(app.paketAdi);
                        if (fallback != null) {
                            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(fallback);
                            if (onBaslatildi != null) onBaslatildi.run();
                            return;
                        }
                    } catch (Exception e2) {
                        LauncherLog.w("ArayuzYardimcisi", "Fallback intent basarisiz", e2);
                    }
                    toast(activity, activity.getString(R.string.acilamadi_hata, e.getMessage()));
                }
            }
        };

        if (UygulamaKilidiYoneticisi.kilitliMi(activity, app.paketAdi)) {
            UygulamaKilidiYoneticisi.pinSorgulaVeCalistir(activity, baslatmaGorevi);
        } else {
            baslatmaGorevi.run();
        }
    }

    /**
     * Standart oge_uygulama görünümünü etiket, simge normalizasyonu, yazı stili,
     * boyutlandırma ve bildirim rozetleriyle bağlar (DRY View Binding).
     */
    public static void uygulamaGorunumuBagla(Context context, View view, UygulamaBilgisi app, boolean etiketGoster, int simgeBoyutuOlcek) {
        if (context == null || view == null || app == null) return;

        ImageView simge = (ImageView) view.findViewById(R.id.oge_simge);
        TextView etiket = (TextView) view.findViewById(R.id.oge_etiket);
        TextView rozet = (TextView) view.findViewById(R.id.oge_rozet);
        FrameLayout kapsayiciSimge = (FrameLayout) view.findViewById(R.id.kapsayici_simge);

        if (etiket != null) {
            etiket.setText(app.etiket);
            etiket.setVisibility(etiketGoster ? View.VISIBLE : View.GONE);
            YaziStiliYoneticisi.stiliUygula(etiket, context);
        }

        if (simge != null) {
            if (app.simge != null) {
                simge.setImageDrawable(SimgeNormalizasyonYoneticisi.normalizeEt(context, app.simge));
            } else {
                simge.setImageResource(R.drawable.simge_varsayilan);
            }
        }

        if (kapsayiciSimge != null && simgeBoyutuOlcek > 0) {
            int dp56 = dpToPx(context, 56 * (simgeBoyutuOlcek / 100.0f));
            ViewGroup.LayoutParams lp = kapsayiciSimge.getLayoutParams();
            if (lp != null && lp.width != dp56) {
                lp.width = dp56;
                lp.height = dp56;
                kapsayiciSimge.setLayoutParams(lp);
            }
        }

        rozetGorunumuBagla(context, rozet, app.paketAdi);
    }

    /**
     * Bildirim rozetini (nokta veya sayı) tek merkezden görünümle bağlar (DRY SSOT).
     */
    public static void rozetGorunumuBagla(Context context, TextView rozet, String paketAdi) {
        if (context == null || rozet == null || paketAdi == null) return;
        int bildirimSayisi = BildirimYoneticisi.getBildirimSayisi(context, paketAdi);
        int rozetTuru = BildirimYoneticisi.getRozetTuru(context);
        if (bildirimSayisi > 0 && rozetTuru != BildirimYoneticisi.ROZET_KAPALI) {
            rozet.setVisibility(View.VISIBLE);
            if (rozetTuru == BildirimYoneticisi.ROZET_NOKTA) {
                rozet.setText("");
                int dp10 = dpToPx(context, 10);
                FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(dp10, dp10);
                rlp.gravity = Gravity.TOP | Gravity.RIGHT;
                rozet.setLayoutParams(rlp);
            } else {
                rozet.setText(bildirimSayisi > 99 ? "99+" : String.valueOf(bildirimSayisi));
                int dp18 = dpToPx(context, 18);
                FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp18);
                rlp.gravity = Gravity.TOP | Gravity.RIGHT;
                rozet.setLayoutParams(rlp);
                rozet.setMinWidth(dp18);
                rozet.setPadding(4, 0, 4, 0);
            }
        } else {
            rozet.setVisibility(View.GONE);
        }
    }

    public static void uygulamaGorunumuBagla(Context context, View view, UygulamaBilgisi app) {
        uygulamaGorunumuBagla(context, view, app, true, 100);
    }
}
