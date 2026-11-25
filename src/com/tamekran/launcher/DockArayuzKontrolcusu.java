package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Sabit Alt Çubuk (Dock) Arayüz ve Yuva Kontrolcüsü
 * AnaEkranActivity'den dock yerleşimi, simge ölçeklemesi ve rozet yönetimini devralarak modülerliği sağlar.
 */
public class DockArayuzKontrolcusu {

    public interface DockEylemDinleyici {
        void onUygulamaTiklandi(UygulamaBilgisi app);
        boolean onUygulamaUzunBasildi(View view, SuruklemeVerisi veri);
    }

    /**
     * Alt bardaki yuvaları, simgeleri ve rozetleri baştan kurar
     */
    public static void dockYukle(
            final Context context,
            final LinearLayout dockYuvalari,
            final List<UygulamaBilgisi> tumUygulamalarListesi,
            final DockEylemDinleyici dinleyici) {
        dockYukle(context, dockYuvalari, null, tumUygulamalarListesi, dinleyici);
    }

    public static void dockYukle(
            final Context context,
            final LinearLayout dockYuvalari,
            final HorizontalScrollView dockKaydirici,
            final List<UygulamaBilgisi> tumUygulamalarListesi,
            final DockEylemDinleyici dinleyici) {
        int olcek = AyarBankasi.getDockSimgeBoyutu(context);
        int dockYukseklikDp = AyarBankasi.getDockYuksekligi(context);
        boolean kaydirilabilir = AyarBankasi.isDockKaydirilabilir(context);
        dockYukleDahili(context, dockYuvalari, dockKaydirici, tumUygulamalarListesi, dockYukseklikDp, olcek, -1, kaydirilabilir, dinleyici);
    }

    public static void dockYukleGecici(
            final Context context,
            final LinearLayout dockYuvalari,
            final HorizontalScrollView dockKaydirici,
            final List<UygulamaBilgisi> tumUygulamalarListesi,
            final int dockYukseklikDp,
            final int olcek,
            final int geciciYuvaSayisi,
            final boolean kaydirilabilir,
            final DockEylemDinleyici dinleyici) {
        dockYukleDahili(context, dockYuvalari, dockKaydirici, tumUygulamalarListesi, dockYukseklikDp, olcek, geciciYuvaSayisi, kaydirilabilir, dinleyici);
    }

    private static void dockYukleDahili(
            final Context context,
            final LinearLayout dockYuvalari,
            final HorizontalScrollView dockKaydirici,
            final List<UygulamaBilgisi> tumUygulamalarListesi,
            final int dockYukseklikDp,
            final int olcek,
            final int geciciYuvaSayisi,
            final boolean kaydirilabilir,
            final DockEylemDinleyici dinleyici) {

        if (context == null || dockYuvalari == null) return;
        dockYuvalari.removeAllViews();

        if (dockKaydirici != null) {
            if (!kaydirilabilir) {
                dockKaydirici.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return event.getAction() == MotionEvent.ACTION_MOVE;
                    }
                });
                dockKaydirici.scrollTo(0, 0);
            } else {
                dockKaydirici.setOnTouchListener(null);
            }
        }

        List<String> dockPaketleri = DockYoneticisi.dockYukle(context);
        if (!DockYoneticisi.isDockBaslatildi(context)) {
            dockPaketleri = DockYoneticisi.varsayilanlariKur(context, tumUygulamalarListesi);
        }
        if (geciciYuvaSayisi > 0 && !kaydirilabilir && dockPaketleri.size() > geciciYuvaSayisi) {
            dockPaketleri = new ArrayList<String>(dockPaketleri.subList(0, geciciYuvaSayisi));
        }

        int dockAdedi = Math.max(1, dockPaketleri.size());

        // Dinamik simge ve yuva hesabı (Dock yüksekliği ve yuva sayısına bağlı ölçekleme)
        int dikeyPayDp = Math.max(28, dockYukseklikDp - 18);
        float olcekCarpani = olcek / 100.0f;
        float hedefSimgeDp = Math.min((float) dikeyPayDp, (dockYukseklikDp * 0.60f) * olcekCarpani);
        hedefSimgeDp = Math.max(24.0f, hedefSimgeDp);

        int simgeBoyutuPx = ArayuzYardimcisi.dpToPx(context, (int) hedefSimgeDp);
        int yuvaBoyutuPx = simgeBoyutuPx + ArayuzYardimcisi.dpToPx(context, 10);
        int kenarBosluguPx = ArayuzYardimcisi.dpToPx(context, 4);

        // Sabit modda ekrana sığdırma koruması (Dar ekranlarda taşmayı ve kaymayı önler)
        if (!kaydirilabilir) {
            int ekranGenisligiPx = context.getResources().getDisplayMetrics().widthPixels;
            int kullanilabilirGenislikPx = Math.max(0, ekranGenisligiPx - ArayuzYardimcisi.dpToPx(context, 24));
            int yuvaBasiPayPx = kullanilabilirGenislikPx / dockAdedi;
            int hesaplananMarginPx = Math.max(1, ArayuzYardimcisi.dpToPx(context, 2));
            int maksYuvaPx = Math.max(ArayuzYardimcisi.dpToPx(context, 24), yuvaBasiPayPx - (2 * hesaplananMarginPx));

            if (yuvaBoyutuPx > maksYuvaPx) {
                yuvaBoyutuPx = maksYuvaPx;
                simgeBoyutuPx = Math.min(simgeBoyutuPx, Math.max(ArayuzYardimcisi.dpToPx(context, 20), yuvaBoyutuPx - ArayuzYardimcisi.dpToPx(context, 4)));
                kenarBosluguPx = hesaplananMarginPx;
            }
        }

        MasaustuModelDeposu.ModelDurumu model = MasaustuModelDeposu.yukle(context);
        List<MasaustuKaydi> dockKayitlari = model.dockOgeleri();

        for (int i = 0; i < dockPaketleri.size(); i++) {
            final int dockPozisyon = i;
            String pkg = dockPaketleri.get(i);
            String dockItemId = null;
            for (MasaustuKaydi dk : dockKayitlari) {
                if (DockYoneticisi.bilesenEslesiyorMu(dk.bilesenKimligi, pkg)) {
                    dockItemId = dk.id;
                    break;
                }
            }
            UygulamaBilgisi hedefApp = UygulamaBilgisi.kimliktenCozumle(context, pkg, tumUygulamalarListesi, null);

            if (hedefApp == null) continue;
            final UygulamaBilgisi app = hedefApp;

            FrameLayout yuva = new FrameLayout(context);
            yuva.setTag(app);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(yuvaBoyutuPx, yuvaBoyutuPx);
            lp.setMargins(kenarBosluguPx, 0, kenarBosluguPx, 0);
            yuva.setLayoutParams(lp);

            ImageView dockSimge = new ImageView(context);
            FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(simgeBoyutuPx, simgeBoyutuPx);
            imgLp.gravity = Gravity.CENTER;
            dockSimge.setLayoutParams(imgLp);
            dockSimge.setImageDrawable(SimgeNormalizasyonYoneticisi.normalizeEt(context, app.simge));
            dockSimge.setScaleType(ImageView.ScaleType.FIT_CENTER);
            dockSimge.setClickable(true);
            dockSimge.setFocusable(true);

            final SuruklemeVerisi dockVerisi = new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_DOCK, 0, dockPozisyon).withItemId(dockItemId);
            final Activity aktivite = (context instanceof Activity) ? (Activity) context : null;
            NovaUzunBasmaYardimcisi.bagla(aktivite, dockSimge, new NovaUzunBasmaYardimcisi.UzunBasmaAksiyonu() {
                @Override
                public void onPopupGoster(View kaynakView) {
                    if (aktivite == null) return;
                    HizliEylemlerDiyalogu.gosterNovaPopup(
                            aktivite,
                            kaynakView,
                            app,
                            dockVerisi,
                            new HizliEylemlerDiyalogu.IslemDinleyici() {
                                @Override
                                public void listeYenile() {
                                    if (aktivite instanceof AnaEkranActivity) {
                                        ((AnaEkranActivity) aktivite).uygulamalariYukle();
                                    }
                                }
                            },
                            null
                    );
                }

                @Override
                public boolean onSuruklemeBaslat(View kaynakView) {
                    return dinleyici != null && dinleyici.onUygulamaUzunBasildi(kaynakView, dockVerisi);
                }

                @Override
                public void onKisaTiklama(View kaynakView) {
                    if (dinleyici != null) dinleyici.onUygulamaTiklandi(app);
                }
            });

            yuva.addView(dockSimge);

            // Bildirim Rozeti
            int sayi = BildirimYoneticisi.getBildirimSayisi(context, app.paketAdi);
            int tur = BildirimYoneticisi.getRozetTuru(context);
            if (sayi > 0 && tur != BildirimYoneticisi.ROZET_KAPALI) {
                yuva.addView(olusturRozet(context, tur, sayi));
            }

            dockYuvalari.addView(yuva);
        }
    }

    private static TextView olusturRozet(Context context, int tur, int sayi) {
        TextView rozet = new TextView(context);
        rozet.setId(R.id.oge_rozet);
        rozet.setBackgroundResource(R.drawable.rozet_arkaplan);
        rozet.setTextColor(Color.WHITE);
        rozet.setTextSize(10);
        rozet.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams rozetLp;
        if (tur == BildirimYoneticisi.ROZET_NOKTA) {
            int dp10 = ArayuzYardimcisi.dpToPx(context, 10);
            rozetLp = new FrameLayout.LayoutParams(dp10, dp10);
        } else {
            int dp18 = ArayuzYardimcisi.dpToPx(context, 18);
            rozetLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp18);
            rozet.setMinWidth(dp18);
            rozet.setPadding(4, 0, 4, 0);
            rozet.setText(sayi > 99 ? "99+" : String.valueOf(sayi));
        }
        rozetLp.gravity = Gravity.TOP | Gravity.RIGHT;
        rozet.setLayoutParams(rozetLp);
        return rozet;
    }

    /**
     * Alt bardaki yuvaları yeniden oluşturmadan doğrudan var olan rozetleri yerinde günceller (Sıfır titreme)
     */
    public static void dockRozetleriniGuncelle(Context context, LinearLayout dockYuvalari) {
        if (context == null || dockYuvalari == null) return;
        int count = dockYuvalari.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = dockYuvalari.getChildAt(i);
            if (child instanceof ViewGroup) {
                ViewGroup yuva = (ViewGroup) child;
                Object tag = yuva.getTag();
                if (tag instanceof UygulamaBilgisi) {
                    UygulamaBilgisi app = (UygulamaBilgisi) tag;
                    TextView rozet = (TextView) yuva.findViewById(R.id.oge_rozet);
                    int sayi = BildirimYoneticisi.getBildirimSayisi(context, app.paketAdi);
                    int tur = BildirimYoneticisi.getRozetTuru(context);
                    if (sayi > 0 && tur != BildirimYoneticisi.ROZET_KAPALI) {
                        if (rozet == null) {
                            yuva.addView(olusturRozet(context, tur, sayi));
                        } else {
                            rozet.setVisibility(View.VISIBLE);
                            if (tur != BildirimYoneticisi.ROZET_NOKTA) {
                                rozet.setText(sayi > 99 ? "99+" : String.valueOf(sayi));
                            }
                        }
                    } else if (rozet != null) {
                        rozet.setVisibility(View.GONE);
                    }
                }
            }
        }
    }
}

