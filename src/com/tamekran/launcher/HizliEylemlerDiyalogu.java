package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Hızlı Eylemler Açılır Menüsü (Nova Tarzı Sabitlenebilir Popup Menü)
 * Uzun basışta simgenin hemen üstünde/altında yüzen kart olarak açılır;
 * parmak oynatılmazsa açık kalır, sürükleme başlarsa anında kapanır.
 */
public class HizliEylemlerDiyalogu {

    public interface IslemDinleyici {
        void listeYenile();
    }

    private static PopupWindow aktifPopup = null;

    public static boolean aktifPopupAcikMi() {
        return aktifPopup != null && aktifPopup.isShowing();
    }

    public static void kapatAktifPopup() {
        if (aktifPopup != null) {
            try {
                if (aktifPopup.isShowing()) {
                    aktifPopup.dismiss();
                }
            } catch (Exception e) {
                LauncherLog.d("HizliEylemler", "Popup kapatma atlandi: " + e.getMessage());
            }
            aktifPopup = null;
        }
    }

    public static void popupEtkilesimeAc() {
        if (aktifPopup != null && aktifPopup.isShowing() && !aktifPopup.isFocusable()) {
            try {
                aktifPopup.setFocusable(true);
                aktifPopup.update();
            } catch (Exception e) {
                LauncherLog.d("HizliEylemler", "Popup odak guncelleme atlandi: " + e.getMessage());
            }
        }
    }

    /**
     * Simgeye bağlı (anchored) Nova tarzı yüzen eylem kartını açar.
     */
    public static void gosterNovaPopup(
            final Activity aktivite,
            final View kaynakGorunum,
            final UygulamaBilgisi app,
            final SuruklemeVerisi veri,
            final IslemDinleyici dinleyici,
            final Runnable klasordenCikarGorevi
    ) {
        if (aktivite == null || app == null || kaynakGorunum == null) return;
        if (aktivite.isFinishing()) return;
        kapatAktifPopup();

        final int kaynakTuru = (veri != null) ? veri.kaynakTuru : SuruklemeVerisi.KAYNAK_MASAUSTU;
        final boolean masaustundeMi = (kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU);
        final boolean docktaMi = (kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK);
        final boolean klasorIcindeMi = (kaynakTuru == SuruklemeVerisi.KAYNAK_KLASOR);
        final int masaustuSayfasi = (veri != null) ? veri.kaynakSayfa : 0;

        SharedPreferences ayarlar = AyarBankasi.getAyarlar(aktivite);
        final boolean masaustuKilitli = ayarlar != null && ayarlar.getBoolean(YedeklemeYoneticisi.PREF_MASAUSTU_KILITLI, false);

        int dp12 = ArayuzYardimcisi.dpToPx(aktivite, 12);
        int dp10 = ArayuzYardimcisi.dpToPx(aktivite, 10);
        int dp8 = ArayuzYardimcisi.dpToPx(aktivite, 8);
        int dp24 = ArayuzYardimcisi.dpToPx(aktivite, 24);
        int kartGenislik = ArayuzYardimcisi.dpToPx(aktivite, 240);
        int maksYukseklik = ArayuzYardimcisi.dpToPx(aktivite, 320);

        LinearLayout kart = new LinearLayout(aktivite);
        kart.setOrientation(LinearLayout.VERTICAL);
        kart.setPadding(dp10, dp10, dp10, dp8);

        GradientDrawable arkaPlan = new GradientDrawable();
        arkaPlan.setColor(0xF01C1F26);
        arkaPlan.setCornerRadius(ArayuzYardimcisi.dpToPx(aktivite, 16));
        arkaPlan.setStroke(ArayuzYardimcisi.dpToPx(aktivite, 1), 0x33FFFFFF);
        kart.setBackgroundDrawable(arkaPlan);
        if (Build.VERSION.SDK_INT >= 21) {
            kart.setElevation(ArayuzYardimcisi.dpToPx(aktivite, 10));
        }

        // Üst Başlık Satırı (Simge + Başlık)
        LinearLayout baslikSatiri = new LinearLayout(aktivite);
        baslikSatiri.setOrientation(LinearLayout.HORIZONTAL);
        baslikSatiri.setGravity(Gravity.CENTER_VERTICAL);
        baslikSatiri.setPadding(dp8 / 2, 0, dp8 / 2, dp8);

        if (app.simge != null) {
            ImageView img = new ImageView(aktivite);
            img.setImageDrawable(app.simge);
            LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(dp24, dp24);
            imgLp.setMargins(0, 0, dp8, 0);
            img.setLayoutParams(imgLp);
            baslikSatiri.addView(img);
        }

        TextView baslik = new TextView(aktivite);
        baslik.setText(app.etiket);
        baslik.setTextColor(Color.WHITE);
        baslik.setTextSize(14);
        baslik.setTypeface(null, Typeface.BOLD);
        baslik.setSingleLine(true);
        baslik.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        baslikSatiri.addView(baslik);
        kart.addView(baslikSatiri);

        View ayirici = new View(aktivite);
        ayirici.setBackgroundColor(0x26FFFFFF);
        LinearLayout.LayoutParams ayrLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        ayrLp.setMargins(0, 0, 0, dp8 / 2);
        ayirici.setLayoutParams(ayrLp);
        kart.addView(ayirici);

        ScrollView kaydirici = new ScrollView(aktivite);
        kaydirici.setVerticalScrollBarEnabled(false);
        LinearLayout listeKutusu = new LinearLayout(aktivite);
        listeKutusu.setOrientation(LinearLayout.VERTICAL);
        kaydirici.addView(listeKutusu);
        kart.addView(kaydirici, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final PopupWindow popup = new PopupWindow(kart, kartGenislik, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setTouchable(true);
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                if (aktifPopup == popup) {
                    aktifPopup = null;
                }
            }
        });
        aktifPopup = popup;

        final boolean[] eylemEklendi = new boolean[1];

        if ((masaustundeMi || docktaMi) && masaustuKilitli) {
            novaSatiriEkle(aktivite, listeKutusu, popup, "🔒 Masaüstü kilitli", 0xFFFFB74D, new Runnable() {
                @Override
                public void run() {
                    ArayuzYardimcisi.toast(aktivite, aktivite.getString(R.string.uyari_masaustu_kilitli));
                }
            });
            eylemEklendi[0] = true;
        }

        if (!masaustuKilitli && app.paketAdi != null && app.paketAdi.startsWith("klasor:")) {
            final String klasorId = app.paketAdi.substring("klasor:".length());
            final boolean kapakModu = KlasorKapakYoneticisi.isKlasorKapakModu(aktivite, klasorId);
            if (aktivite instanceof AnaEkranActivity) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "📂 Klasörü Aç", Color.WHITE, new Runnable() {
                    @Override
                    public void run() {
                        ((AnaEkranActivity) aktivite).klasoruAc(app.paketAdi, true);
                    }
                });
            }
            novaSatiriEkle(aktivite, listeKutusu, popup, "✏️ Klasör Adını Değiştir", Color.WHITE, new Runnable() {
                @Override
                public void run() {
                    klasorAdDegistirIsle(aktivite, klasorId, app.etiket, dinleyici);
                }
            });
            novaSatiriEkle(aktivite, listeKutusu, popup, kapakModu ? "📁 Kapak Modu: Açık" : "📁 Kapak Modu: Kapalı", Color.WHITE, new Runnable() {
                @Override
                public void run() {
                    kapakModuDegistirIsle(aktivite, klasorId, kapakModu, dinleyici);
                }
            });
            if (masaustundeMi) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "📄 Başka Sayfaya Taşı", Color.WHITE, new Runnable() {
                    @Override public void run() { sayfaSecimDiyalogu(aktivite, app, veri, dinleyici); }
                });
            }
            novaSatiriEkle(aktivite, listeKutusu, popup, docktaMi ? "🗑️ Alt Bardan Kaldır" : "🗑️ Masaüstünden Kaldır", 0xFFFF8A80, new Runnable() {
                @Override
                public void run() {
                    konumdanKaldirVeGeriAlKaydet(aktivite, app, veri, dinleyici);
                }
            });
            novaSatiriEkle(aktivite, listeKutusu, popup, "❌ Klasörü Tamamen Sil", 0xFFFF5252, new Runnable() {
                @Override
                public void run() {
                    KlasorYoneticisi.klasorUygulamalariniMasaustuneDokVeSil(aktivite, klasorId);
                    if (dinleyici != null) dinleyici.listeYenile();
                    ArayuzYardimcisi.toast(aktivite, aktivite.getString(R.string.klasor_silindi_mesaj));
                }
            });
            eylemEklendi[0] = true;
        } else if (!masaustuKilitli && app.paketAdi != null && app.paketAdi.startsWith("kisayol:")) {
            final String kisayolId = app.paketAdi.substring("kisayol:".length());
            novaSatiriEkle(aktivite, listeKutusu, popup, "✏️ Kısayol Adını Değiştir", Color.WHITE, new Runnable() {
                @Override
                public void run() {
                    AyarDiyalogYardimcisi.metinGirdiDiyalogu(aktivite, "Kısayol Adı", app.etiket, null, new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
                        @Override
                        public void onMetinGirildi(String yeniAd) {
                            if (yeniAd != null && yeniAd.length() > 0) {
                                List<KisayolYoneticisi.KisayolBilgisi> list = KisayolYoneticisi.tumKisayollariYukle(aktivite);
                                for (KisayolYoneticisi.KisayolBilgisi kb : list) {
                                    if (kb.id.equals(kisayolId)) {
                                        kb.ad = yeniAd;
                                        break;
                                    }
                                }
                                KisayolYoneticisi.kisayollariKaydet(aktivite, list);
                                if (dinleyici != null) dinleyici.listeYenile();
                            }
                        }
                    });
                }
            });
            novaSatiriEkle(aktivite, listeKutusu, popup, "📄 Başka Sayfaya Taşı", Color.WHITE, new Runnable() {
                @Override public void run() { sayfaSecimDiyalogu(aktivite, app, dinleyici); }
            });
            novaSatiriEkle(aktivite, listeKutusu, popup, "🗑️ Kaldır", 0xFFFF8A80, new Runnable() {
                @Override
                public void run() {
                    konumdanKaldirVeGeriAlKaydet(aktivite, app, veri, dinleyici);
                }
            });
            eylemEklendi[0] = true;
        } else if (!masaustuKilitli && app.paketAdi != null && app.paketAdi.startsWith("arac:")) {
            if (masaustundeMi) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "📄 Başka Sayfaya Taşı", Color.WHITE, new Runnable() {
                    @Override public void run() { sayfaSecimDiyalogu(aktivite, app, veri, dinleyici); }
                });
            }
            novaSatiriEkle(aktivite, listeKutusu, popup, docktaMi ? "🗑️ Alt Bardan Kaldır" : "🗑️ Masaüstünden Kaldır", 0xFFFF8A80, new Runnable() {
                @Override
                public void run() {
                    konumdanKaldirVeGeriAlKaydet(aktivite, app, veri, dinleyici);
                }
            });
            eylemEklendi[0] = true;
        }

        if (!eylemEklendi[0]) {
            if (klasorIcindeMi && klasordenCikarGorevi != null && !masaustuKilitli) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "📤 Klasörden Çıkar", 0xFF80D8FF, klasordenCikarGorevi);
            }

            if (!masaustuKilitli) {
                if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "adlandir")) {
                    novaSatiriEkle(aktivite, listeKutusu, popup, "✏️ Yeniden Adlandır", Color.WHITE, new Runnable() {
                        @Override public void run() { adlandirDiyalogu(aktivite, app, dinleyici); }
                    });
                }
                if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "ozel_simge")) {
                    novaSatiriEkle(aktivite, listeKutusu, popup, "🖼️ Özel Simge Seç", Color.WHITE, new Runnable() {
                        @Override public void run() { ozelSimgeSecimDiyalogu(aktivite, app, dinleyici); }
                    });
                }
                if ((masaustundeMi || docktaMi) && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kaydirma")) {
                    String eylemOzeti = SimgeKaydirmaYoneticisi.getEylemOzeti(aktivite, app.paketAdi);
                    novaSatiriEkle(aktivite, listeKutusu, popup, "👆 Yukarı Kaydırma (" + eylemOzeti + ")", Color.WHITE, new Runnable() {
                        @Override
                        public void run() {
                            List<UygulamaBilgisi> apps = (aktivite instanceof AnaEkranActivity)
                                    ? ((AnaEkranActivity) aktivite).getTumUygulamalarListesi() : null;
                            SimgeKaydirmaYoneticisi.secimDiyalogu(aktivite, app.paketAdi, app.etiket, apps, new Runnable() {
                                @Override public void run() { if (dinleyici != null) dinleyici.listeYenile(); }
                            });
                        }
                    });
                }
            }

            if ((masaustundeMi || docktaMi) && !masaustuKilitli) {
                novaSatiriEkle(aktivite, listeKutusu, popup, docktaMi ? "🗑️ Alt Bardan Kaldır" : "🗑️ Masaüstünden Kaldır", 0xFFFF8A80, new Runnable() {
                    @Override public void run() { konumdanKaldirVeGeriAlKaydet(aktivite, app, veri, dinleyici); }
                });
            }

            if (!masaustundeMi && !docktaMi && !klasorIcindeMi) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "➕ Masaüstüne Ekle", Color.WHITE, new Runnable() {
                    @Override public void run() { sayfaSecimDiyalogu(aktivite, app, veri, dinleyici); }
                });
            }

            if (!docktaMi && !masaustuKilitli && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "dock")) {
                final boolean docktaVar = DockYoneticisi.docktaVarMi(aktivite, app.getBilesenKimligi());
                novaSatiriEkle(aktivite, listeKutusu, popup, docktaVar ? "📌 Alt Bardan Kaldır" : "📌 Alt Bara Sabitle", Color.WHITE, new Runnable() {
                    @Override
                    public void run() {
                        dockSabitlemeDegistirIsle(aktivite, app, docktaVar, dinleyici);
                    }
                });
            }

            if (!klasorIcindeMi && !masaustuKilitli && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "klasor")) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "📁 Klasöre Ekle", Color.WHITE, new Runnable() {
                    @Override public void run() { klasorSecimDiyalogu(aktivite, app, veri, dinleyici); }
                });
            }

            final boolean kilitli = UygulamaKilidiYoneticisi.kilitliMi(aktivite, app.paketAdi);
            if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kaydirma") && (masaustundeMi || docktaMi)) {
                // Yukarı kaydırma satırı yukarıda eklendi
            }
            if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kilit")) {
                novaSatiriEkle(aktivite, listeKutusu, popup, kilitli ? "🔓 Kilidi Kaldır" : "🔒 Uygulamayı Kilitle", Color.WHITE, new Runnable() {
                    @Override
                    public void run() {
                        uygulamaKilitDegistirIsle(aktivite, app, kilitli, dinleyici);
                    }
                });
            }

            if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "uygulama_bilgisi")) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "ℹ️ Uygulama Bilgisi", Color.WHITE, new Runnable() {
                    @Override public void run() { UygulamaDetayDiyalogu.goster(aktivite, app); }
                });
            }

            if (!masaustundeMi && !docktaMi && !klasorIcindeMi && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "gizle")) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "👁️ Menüde Gizle", Color.WHITE, new Runnable() {
                    @Override
                    public void run() {
                        menudeGizleIsle(aktivite, app, dinleyici);
                    }
                });
            }

            if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kaldir")) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "📲 Uygulamayı Sil (Uninstall)", 0xFFFF8A80, new Runnable() {
                    @Override public void run() { uygulamayiSil(aktivite, app.paketAdi); }
                });
            }

            if (!masaustundeMi && !docktaMi && !klasorIcindeMi) {
                novaSatiriEkle(aktivite, listeKutusu, popup, "⋯ Tüm Seçenekler", 0xFFB0BEC5, new Runnable() {
                    @Override
                    public void run() {
                        goster(aktivite, kaynakGorunum, app, masaustundeMi, masaustuSayfasi, dinleyici);
                    }
                });
            }
        }

        // Boyutu ölç ve ikonun hemen üstüne (yer yoksa altına) hizala
        int wSpec = View.MeasureSpec.makeMeasureSpec(kartGenislik, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(maksYukseklik, View.MeasureSpec.AT_MOST);
        kart.measure(wSpec, hSpec);
        int popW = kart.getMeasuredWidth();
        int popH = Math.min(maksYukseklik, kart.getMeasuredHeight());
        if (popH <= 0) popH = maksYukseklik;
        popup.setHeight(popH);

        int[] loc = new int[2];
        kaynakGorunum.getLocationInWindow(loc);
        int ekranW = aktivite.getResources().getDisplayMetrics().widthPixels;
        int ekranH = aktivite.getResources().getDisplayMetrics().heightPixels;
        int ustSinir = ArayuzYardimcisi.getDurumCubuguYuksekligi(aktivite) + dp8;

        int x = loc[0] + (kaynakGorunum.getWidth() - popW) / 2;
        if (x < dp12) x = dp12;
        if (x + popW > ekranW - dp12) x = Math.max(dp12, ekranW - popW - dp12);

        int yUst = loc[1] - popH - dp8;
        int yAlt = loc[1] + kaynakGorunum.getHeight() + dp8;
        int y = (yUst >= ustSinir) ? yUst : Math.min(yAlt, Math.max(ustSinir, ekranH - popH - dp12));

        try {
            View kok = aktivite.getWindow().getDecorView();
            popup.showAtLocation(kok, Gravity.NO_GRAVITY, x, y);
        } catch (Exception e) {
            LauncherLog.w("HizliEylemler", "Nova popup gosterilemedi", e);
            aktifPopup = null;
        }
    }

    private static void novaSatiriEkle(
            Activity aktivite,
            LinearLayout kapsayici,
            final PopupWindow popup,
            String metin,
            int renk,
            final Runnable eylem) {
        TextView tv = new TextView(aktivite);
        tv.setText(metin);
        tv.setTextColor(renk);
        tv.setTextSize(13);
        int padH = ArayuzYardimcisi.dpToPx(aktivite, 10);
        int padV = ArayuzYardimcisi.dpToPx(aktivite, 9);
        tv.setPadding(padH, padV, padH, padV);
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kapatAktifPopup();
                if (eylem != null) eylem.run();
            }
        });
        kapsayici.addView(tv);
    }

    private static void konumdanKaldirVeGeriAlKaydet(
            Activity aktivite,
            UygulamaBilgisi app,
            SuruklemeVerisi veri,
            IslemDinleyici dinleyici) {
        if (aktivite == null || app == null || veri == null) return;

        String klasorAdi = null;
        List<String> klasorPaketleri = null;
        if (app.paketAdi != null && app.paketAdi.startsWith("kisayol:")) {
            String kisayolId = app.paketAdi.substring("kisayol:".length());
            KisayolYoneticisi.kisayolSil(aktivite, kisayolId);
        } else if (app.paketAdi != null && app.paketAdi.startsWith("klasor:")) {
            String kId = app.paketAdi.substring("klasor:".length());
            List<KlasorYoneticisi.KlasorBilgisi> kList = KlasorYoneticisi.klasorleriYukle(aktivite);
            for (KlasorYoneticisi.KlasorBilgisi kb : kList) {
                if (kb.id.equals(kId)) {
                    klasorAdi = kb.ad;
                    klasorPaketleri = new ArrayList<String>(kb.paketler);
                    break;
                }
            }
            KlasorYoneticisi.klasorSil(aktivite, kId);
        }

        int kaynakSayfa = (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) ? veri.kaynakSayfa : 0;
        GeriAlYoneticisi.silineniKaydet(
                veri.kaynakTuru,
                kaynakSayfa,
                app.paketAdi,
                app.etiket,
                veri.kaynakCellX,
                veri.kaynakCellY,
                veri.spanX,
                veri.spanY,
                klasorAdi,
                klasorPaketleri
        );

        String kimlik = app.getBilesenKimligi();
        if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU || veri.kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
            MasaustuModelDeposu.ogeKaldir(aktivite, veri.itemId, veri.kaynakTuru, veri.kaynakSayfa, kimlik);
        }

        if (dinleyici != null) dinleyici.listeYenile();
        if (aktivite instanceof AnaEkranActivity) {
            ((AnaEkranActivity) aktivite).geriAlCubugunuGoster(app.etiket);
        } else {
            ArayuzYardimcisi.toast(aktivite, app.etiket + " kaldırıldı.");
        }
    }

    public static void goster(
        final Activity aktivite,
        final View kaynakGorunum,
        final UygulamaBilgisi app,
        final boolean masaustundeMi,
        final int masaustuSayfasi,
        final IslemDinleyici dinleyici
    ) {
        if (aktivite == null || app == null) return;

        SharedPreferences ayarlar = AyarBankasi.getAyarlar(aktivite);
        boolean masaustuKilitli = ayarlar.getBoolean(YedeklemeYoneticisi.PREF_MASAUSTU_KILITLI, false);

        if (masaustundeMi && masaustuKilitli) {
            ArayuzYardimcisi.toast(aktivite, "Masaüstü kilitli. Değişiklik yapmak için Ayarlar'dan kilidi kaldırın.");
            return;
        }

        // Klasör kısayolu için özel menü
        if (app.paketAdi != null && app.paketAdi.startsWith("klasor:")) {
            final String klasorId = app.paketAdi.substring("klasor:".length());
            final boolean kapakModu = KlasorKapakYoneticisi.isKlasorKapakModu(aktivite, klasorId);
            CharSequence[] klasorSecenekleri = new CharSequence[]{
                "📂 Klasörü Aç",
                "✏️ Klasör Adını Değiştir",
                kapakModu ? "📁 Kapak Modu (İlk Uygulamayı Aç): Açık" : "📁 Kapak Modu (İlk Uygulamayı Aç): Kapalı",
                "📄 Başka Sayfaya Taşı",
                "🗑️ Masaüstünden Kaldır",
                "❌ Klasörü Tamamen Sil"
            };

            new AlertDialog.Builder(aktivite)
                .setTitle("📁 " + app.etiket)
                .setItems(klasorSecenekleri, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            if (aktivite instanceof AnaEkranActivity) {
                                ((AnaEkranActivity) aktivite).klasoruAc(app.paketAdi, true);
                            }
                        } else if (which == 1) {
                            klasorAdDegistirIsle(aktivite, klasorId, app.etiket, dinleyici);
                        } else if (which == 2) {
                            kapakModuDegistirIsle(aktivite, klasorId, kapakModu, dinleyici);
                        } else if (which == 3) {
                            sayfaSecimDiyalogu(aktivite, app, dinleyici);
                        } else if (which == 4) {
                            MasaustuYoneticisi.masaustundenKaldir(aktivite, app.paketAdi);
                            if (dinleyici != null) dinleyici.listeYenile();
                        } else if (which == 5) {
                            KlasorYoneticisi.klasorUygulamalariniMasaustuneDokVeSil(aktivite, klasorId);
                            if (dinleyici != null) dinleyici.listeYenile();
                            ArayuzYardimcisi.toast(aktivite, aktivite.getString(R.string.klasor_silindi_mesaj));
                        }
                    }
                })
                .setNegativeButton(R.string.iptal, null)
                .show();
            return;
        }

        // Sistem Kısayolu için özel menü
        if (app.paketAdi != null && app.paketAdi.startsWith("kisayol:")) {
            final String kisayolId = app.paketAdi.substring("kisayol:".length());
            CharSequence[] ksSecenekleri = new CharSequence[]{
                "✏️ Kısayol Adını Değiştir",
                "📄 Başka Sayfaya Taşı",
                "🗑️ Masaüstünden Kaldır"
            };

            new AlertDialog.Builder(aktivite)
                .setTitle("⚡ " + app.etiket)
                .setItems(ksSecenekleri, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            AyarDiyalogYardimcisi.metinGirdiDiyalogu(aktivite, "Kısayol Adı", app.etiket, null, new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
                                @Override
                                public void onMetinGirildi(String yeniAd) {
                                    if (yeniAd != null && yeniAd.length() > 0) {
                                        List<KisayolYoneticisi.KisayolBilgisi> list = KisayolYoneticisi.tumKisayollariYukle(aktivite);
                                        for (KisayolYoneticisi.KisayolBilgisi kb : list) {
                                            if (kb.id.equals(kisayolId)) {
                                                kb.ad = yeniAd;
                                                break;
                                            }
                                        }
                                        KisayolYoneticisi.kisayollariKaydet(aktivite, list);
                                        if (dinleyici != null) dinleyici.listeYenile();
                                    }
                                }
                            });
                        } else if (which == 1) {
                            sayfaSecimDiyalogu(aktivite, app, dinleyici);
                        } else if (which == 2) {
                            KisayolYoneticisi.kisayolSil(aktivite, kisayolId);
                            if (dinleyici != null) dinleyici.listeYenile();
                            ArayuzYardimcisi.toast(aktivite, app.etiket + " kaldırıldı.");
                        }
                    }
                })
                .setNegativeButton(R.string.iptal, null)
                .show();
            return;
        }

        List<String> secenekler = new ArrayList<String>();
        final List<Integer> eylemKodlari = new ArrayList<Integer>();

        // 2. Yeniden Adlandır
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "adlandir")) {
            secenekler.add("✏️ Yeniden Adlandır (" + app.etiket + ")");
            eylemKodlari.add(2);
        }

        // 2b. Özel Simge Seç
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "ozel_simge")) {
            secenekler.add("🖼️ Özel Simge Seç");
            eylemKodlari.add(12);
        }

        // 2c. Uygulama Kilidi & PIN Koruması
        final boolean kilitli = UygulamaKilidiYoneticisi.kilitliMi(aktivite, app.paketAdi);
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kilit")) {
            secenekler.add(kilitli ? "🔓 Uygulama Kilidini Kaldır" : "🔒 Uygulamayı Kilitle (PIN)");
            eylemKodlari.add(13);
        }

        // 2d. Nova Swipe Action: Yukarı Kaydırma Eylemi
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kaydirma")) {
            String eylemOzeti = SimgeKaydirmaYoneticisi.getEylemOzeti(aktivite, app.paketAdi);
            secenekler.add("👆 Yukarı Kaydırma Eylemi (" + eylemOzeti + ")");
            eylemKodlari.add(14);
        }

        // 3. Masaüstü İşlemleri
        if (masaustundeMi) {
            secenekler.add("🗑️ Masaüstünden Kaldır");
            eylemKodlari.add(3);

            secenekler.add("📄 Başka Sayfaya Taşı");
            eylemKodlari.add(4);
        } else {
            secenekler.add("➕ Masaüstüne Ekle (Sayfa Seç)");
            eylemKodlari.add(5);
        }

        // 3b. Alt Bar (Dock) İşlemleri
        final boolean dockta = DockYoneticisi.docktaVarMi(aktivite, app.getBilesenKimligi());
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "dock")) {
            secenekler.add(dockta ? "📌 Alt Bardan Kaldır" : "📌 Alt Bara Sabitle");
            eylemKodlari.add(16);
        }

        // 4. Klasöre Ekle
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "klasor")) {
            secenekler.add("📁 Klasöre Ekle");
            eylemKodlari.add(6);
        }

        // 5. Kategori Ata (Yalnızca Çekmece)
        if (!masaustundeMi && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kategori")) {
            secenekler.add("🏷️ Menü Kategorisi Belirle");
            eylemKodlari.add(7);
        }

        // 6. Uygulama Bilgisi & Boyut
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "uygulama_bilgisi")) {
            secenekler.add("ℹ️ Uygulama Bilgisi & Boyutu");
            eylemKodlari.add(8);
        }

        // 7. Mağaza / Google Play (Yalnızca Çekmece)
        if (!masaustundeMi && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "play_store")) {
            secenekler.add("🛍️ Google Play'de Göster");
            eylemKodlari.add(9);
        }

        // 7b. APK Paylaşımı (Yalnızca Çekmece)
        if (!masaustundeMi && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "apk_paylas")) {
            secenekler.add("📦 APK Dosyasını Paylaş");
            eylemKodlari.add(15);
        }

        // 8. Menüde Gizle (Yalnızca Çekmece)
        if (!masaustundeMi && AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "gizle")) {
            secenekler.add("👁️ Menüde Gizle");
            eylemKodlari.add(10);
        }

        // 9. Kaldır (Uninstall)
        if (AyarBankasi.uzunBasmaOgesiAktifMi(aktivite, "kaldir")) {
            secenekler.add("📲 Uygulamayı Kaldır (Uninstall)");
            eylemKodlari.add(11);
        }

        new AlertDialog.Builder(aktivite)
            .setTitle(app.etiket)
            .setIcon(app.simge)
            .setItems(secenekler.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int eylem = eylemKodlari.get(which);
                    switch (eylem) {
                        case 2: // Yeniden adlandır
                            adlandirDiyalogu(aktivite, app, dinleyici);
                            break;

                        case 3: // Masaüstünden kaldır
                            MasaustuModelDeposu.ogeKaldir(
                                    aktivite,
                                    null,
                                    SuruklemeVerisi.KAYNAK_MASAUSTU,
                                    masaustuSayfasi,
                                    app.getBilesenKimligi());
                            ArayuzYardimcisi.toast(aktivite, app.etiket + " masaüstünden kaldırıldı.");
                            if (dinleyici != null) dinleyici.listeYenile();
                            break;

                        case 16: // Alt Bara Ekle / Kaldır
                            dockSabitlemeDegistirIsle(aktivite, app, dockta, dinleyici);
                            break;

                        case 4: // Sayfa taşı
                        case 5: // Sayfa seçerek masaüstüne ekle
                            sayfaSecimDiyalogu(aktivite, app, dinleyici);
                            break;

                        case 6: // Klasöre ekle
                            klasorSecimDiyalogu(aktivite, app, dinleyici);
                            break;

                        case 7: // Kategori seç
                            kategoriSecimDiyalogu(aktivite, app, dinleyici);
                            break;

                        case 8: // Uygulama bilgisi & boyut kartı
                            UygulamaDetayDiyalogu.goster(aktivite, app);
                            break;

                        case 9: // Play store
                            playStoreAc(aktivite, app.paketAdi);
                            break;

                        case 10: // Menüde gizle
                            menudeGizleIsle(aktivite, app, dinleyici);
                            break;

                        case 11: // Uygulama sil
                            uygulamayiSil(aktivite, app.paketAdi);
                            break;

                        case 12: // Özel Simge Seç
                            ozelSimgeSecimDiyalogu(aktivite, app, dinleyici);
                            break;

                        case 13: // Uygulama Kilidi & PIN
                            uygulamaKilitDegistirIsle(aktivite, app, kilitli, dinleyici);
                            break;

                        case 14: // Yukarı Kaydırma Eylemi
                            kaydirmaEylemiSecIsle(aktivite, app, dinleyici);
                            break;

                        case 15: // APK Paylaş
                            ApkPaylasimYardimcisi.apkPaylas(aktivite, app);
                            break;
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private static void ozelSimgeSecimDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final IslemDinleyici dinleyici) {
        final SimgePaketiYoneticisi motor = new SimgePaketiYoneticisi(aktivite);
        final List<SimgePaketiYoneticisi.SimgePaketiBilgisi> paketler = motor.yukluPaketleriBul();

        List<String> basliklar = new ArrayList<String>();
        basliklar.add("Varsayılan (Ana Temaya Uy)");
        basliklar.add("🖼️ Galeriden Resim Seç (PNG/JPG)");
        for (SimgePaketiYoneticisi.SimgePaketiBilgisi p : paketler) {
            basliklar.add("🎨 " + p.baslik);
        }

        new AlertDialog.Builder(aktivite)
            .setTitle("Özel Simge Seç: " + app.etiket)
            .setItems(basliklar.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 1) {
                        OzelSimgeYoneticisi.galeriSeciminiBaslat(aktivite, app.getBilesenKimligi(), app.paketAdi);
                        return;
                    }
                    if (which == 0) {
                        OzelSimgeYoneticisi.setOzelSimge(aktivite, app.getBilesenKimligi(), app.paketAdi, null);
                    } else {
                        String secilen = paketler.get(which - 2).paketAdi;
                        OzelSimgeYoneticisi.setOzelSimge(aktivite, app.getBilesenKimligi(), app.paketAdi, secilen);
                    }
                    SimgeOnbellegi.paketTemizle(app.paketAdi);
                    KlasorSimgeUretici.onbellegiTemizle();
                    try {
                        android.content.pm.PackageManager pm = aktivite.getPackageManager();
                        android.graphics.drawable.Drawable ham = null;
                        if (app.etkinlikAdi != null && app.etkinlikAdi.length() > 0) {
                            ham = pm.getActivityIcon(new android.content.ComponentName(app.paketAdi, app.etkinlikAdi));
                        } else {
                            ham = pm.getApplicationIcon(app.paketAdi);
                        }
                        app.simge = motor.simgeyiGetir(app.paketAdi, app.etkinlikAdi, ham);
                    } catch (Throwable t) {
                        LauncherLog.d("HizliEylemler", "Anlik simge yenileme uyarisi: " + t.getMessage());
                    }
                    ArayuzYardimcisi.toast(aktivite, aktivite.getString(R.string.simge_guncellendi));
                    if (aktivite instanceof AnaEkranActivity) {
                        ((AnaEkranActivity) aktivite).uygulamalariYukle();
                        ((AnaEkranActivity) aktivite).masaustuSayfalariniTazele();
                    }
                    if (dinleyici != null) dinleyici.listeYenile();
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private static void adlandirDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final IslemDinleyici dinleyici) {
        AyarDiyalogYardimcisi.metinGirdiDiyalogu(aktivite, aktivite.getString(R.string.yeniden_adlandir), app.etiket, null, new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
            @Override
            public void onMetinGirildi(String yeniAd) {
                String kimlik = app.getBilesenKimligi();
                if (yeniAd.length() > 0) {
                    app.etiket = yeniAd;
                    OzelEtiketYoneticisi.setEtiket(aktivite, kimlik, yeniAd);
                } else {
                    // Boş bırakılırsa orijinal sistem adına geri döndür
                    OzelEtiketYoneticisi.setEtiket(aktivite, kimlik, null);
                    try {
                        PackageManager pm = aktivite.getPackageManager();
                        if (app.etkinlikAdi != null && !app.etkinlikAdi.isEmpty()) {
                            android.content.ComponentName cn = new android.content.ComponentName(app.paketAdi, app.etkinlikAdi);
                            android.content.pm.ActivityInfo aInfo = pm.getActivityInfo(cn, 0);
                            app.etiket = aInfo.loadLabel(pm).toString();
                        } else {
                            android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(app.paketAdi, 0);
                            app.etiket = pm.getApplicationLabel(ai).toString();
                        }
                    } catch (Throwable t) {
                        // sessizce devam et
                    }
                }
                ArayuzYardimcisi.toast(aktivite, "Uygulama adı güncellendi.");
                SimgeOnbellegi.etiketGuncelle(kimlik, app.etiket);
                if (yeniAd.length() == 0) {
                    SimgeOnbellegi.paketTemizle(app.paketAdi);
                }
                if (dinleyici != null) {
                    dinleyici.listeYenile();
                }
                if (aktivite instanceof AnaEkranActivity) {
                    AnaEkranActivity ana = (AnaEkranActivity) aktivite;
                    ana.uygulamalariYukle();
                    ana.masaustuSayfalariniTazele();
                    ana.dockYukle();
                }
            }
        });
    }

    private static void klasorAdDegistirIsle(
            final Activity aktivite,
            final String klasorId,
            final String mevcutAd,
            final IslemDinleyici dinleyici) {
        AyarDiyalogYardimcisi.metinGirdiDiyalogu(aktivite, "Klasör Adı", mevcutAd, null, new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
            @Override
            public void onMetinGirildi(String yeniAd) {
                if (yeniAd != null && yeniAd.length() > 0) {
                    List<KlasorYoneticisi.KlasorBilgisi> list = KlasorYoneticisi.klasorleriYukle(aktivite);
                    for (KlasorYoneticisi.KlasorBilgisi k : list) {
                        if (k.id.equals(klasorId)) {
                            k.ad = yeniAd;
                            break;
                        }
                    }
                    KlasorYoneticisi.klasorleriKaydet(aktivite, list);
                    if (dinleyici != null) dinleyici.listeYenile();
                }
            }
        });
    }

    private static void kapakModuDegistirIsle(
            Activity aktivite,
            String klasorId,
            boolean mevcutKapakModu,
            IslemDinleyici dinleyici) {
        KlasorKapakYoneticisi.setKlasorKapakModu(aktivite, klasorId, !mevcutKapakModu);
        ArayuzYardimcisi.toast(aktivite, "Kapak modu " + (!mevcutKapakModu ? "etkinleştirildi (Tek tık ilk uygulamayı açar)." : "kapatıldı."));
        if (dinleyici != null) dinleyici.listeYenile();
    }

    private static void dockSabitlemeDegistirIsle(
            Activity aktivite,
            UygulamaBilgisi app,
            boolean docktaVar,
            IslemDinleyici dinleyici) {
        if (docktaVar) {
            DockYoneticisi.docktanKaldir(aktivite, app.getBilesenKimligi());
            ArayuzYardimcisi.toast(aktivite, app.etiket + " alt bardan kaldırıldı.");
        } else {
            DockYoneticisi.dockaEkle(aktivite, app.getBilesenKimligi());
            ArayuzYardimcisi.toast(aktivite, app.etiket + " alt bara sabitlendi.");
        }
        if (dinleyici != null) dinleyici.listeYenile();
    }

    private static void sayfaSecimDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final IslemDinleyici dinleyici) {
        sayfaSecimDiyalogu(aktivite, app, null, dinleyici);
    }

    private static void sayfaSecimDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final SuruklemeVerisi veri, final IslemDinleyici dinleyici) {
        int toplamSayfa = MasaustuYoneticisi.getSayfaSayisi(aktivite);
        String[] sayfalar = new String[toplamSayfa];
        for (int i = 0; i < toplamSayfa; i++) {
            sayfalar[i] = "Sayfa " + (i + 1);
        }

        new AlertDialog.Builder(aktivite)
            .setTitle("Masaüstü Sayfası Seçin")
            .setItems(sayfalar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (veri != null && veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) {
                        MasaustuModelDeposu.masaustuneOgeTasi(aktivite, veri, which, 0, 0);
                        ArayuzYardimcisi.toast(aktivite, app.etiket + " Sayfa " + (which + 1) + "'e taşındı.");
                    } else {
                        MasaustuModelDeposu.masaustuneOgeTasi(
                                aktivite,
                                new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_CEKMECE),
                                which, 0, 0);
                        ArayuzYardimcisi.toast(aktivite, app.etiket + " Sayfa " + (which + 1) + "'e eklendi.");
                    }
                    if (dinleyici != null) dinleyici.listeYenile();
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private static void kategoriSecimDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final IslemDinleyici dinleyici) {
        final List<String> kategoriler = KategoriYoneticisi.getKategoriler(aktivite);
        final List<String> secenekListesi = new ArrayList<String>(kategoriler);
        secenekListesi.add("+ Yeni Kategori Oluştur");

        new AlertDialog.Builder(aktivite)
            .setTitle("Kategori Belirle: " + app.etiket)
            .setItems(secenekListesi.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == secenekListesi.size() - 1) {
                        yeniKategoriDiyalogu(aktivite, app, dinleyici);
                    } else {
                        String secilen = kategoriler.get(which);
                        KategoriYoneticisi.kategoriAta(aktivite, app.paketAdi, secilen);
                        ArayuzYardimcisi.toast(aktivite, app.etiket + " -> " + secilen + " kategorisine atandı.");
                        if (dinleyici != null) dinleyici.listeYenile();
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private static void yeniKategoriDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final IslemDinleyici dinleyici) {
        AyarDiyalogYardimcisi.metinGirdiDiyalogu(aktivite, "Yeni Kategori Oluştur", null, "Kategori Adı (Örn: Finans)", new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
            @Override
            public void onMetinGirildi(String ad) {
                if (ad.length() > 0) {
                    KategoriYoneticisi.kategoriEkle(aktivite, ad);
                    KategoriYoneticisi.kategoriAta(aktivite, app.paketAdi, ad);
                    ArayuzYardimcisi.toast(aktivite, ad + " kategorisi oluşturuldu ve atandı.");
                    if (dinleyici != null) dinleyici.listeYenile();
                }
            }
        });
    }

    private static void klasorSecimDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final IslemDinleyici dinleyici) {
        klasorSecimDiyalogu(aktivite, app, null, dinleyici);
    }

    private static void klasorSecimDiyalogu(final Activity aktivite, final UygulamaBilgisi app, final SuruklemeVerisi veri, final IslemDinleyici dinleyici) {
        final List<KlasorYoneticisi.KlasorBilgisi> klasorler = KlasorYoneticisi.klasorleriYukle(aktivite);
        List<String> basliklar = new ArrayList<String>();
        basliklar.add("+ Yeni Klasör Oluştur");
        for (KlasorYoneticisi.KlasorBilgisi k : klasorler) {
            basliklar.add("📁 " + k.ad + " (" + k.paketler.size() + " öğe)");
        }

        new AlertDialog.Builder(aktivite)
            .setTitle(R.string.klasore_ekle)
            .setItems(basliklar.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        AyarDiyalogYardimcisi.metinGirdiDiyalogu(aktivite, aktivite.getString(R.string.klasor_olustur), null, "Klasör Adı", new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
                            @Override
                            public void onMetinGirildi(final String ad) {
                                if (ad == null || ad.trim().length() == 0) return;
                                final String appKimlik = app.getBilesenKimligi();
                                final int aktifSayfa = (aktivite instanceof AnaEkranActivity)
                                        ? ((AnaEkranActivity) aktivite).getAktifSayfa() : 0;

                                List<UygulamaBilgisi> adaylar = new ArrayList<UygulamaBilgisi>();
                                if (aktivite instanceof AnaEkranActivity) {
                                    for (UygulamaBilgisi u : ((AnaEkranActivity) aktivite).getTumUygulamalarListesi()) {
                                        if (u != null && !DockYoneticisi.bilesenEslesiyorMu(u.getBilesenKimligi(), appKimlik)) {
                                            adaylar.add(u);
                                        }
                                    }
                                }
                                if (adaylar.isEmpty()) {
                                    List<String> tek = new ArrayList<String>();
                                    tek.add(appKimlik);
                                    MasaustuModelDeposu.klasorOlusturVeCokluEkle(aktivite, aktifSayfa, ad.trim(), tek);
                                    if (veri != null && (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU || veri.kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK)) {
                                        MasaustuModelDeposu.ogeKaldir(aktivite, veri.itemId, veri.kaynakTuru, veri.kaynakSayfa, appKimlik);
                                    }
                                    ArayuzYardimcisi.toast(aktivite, app.etiket + ", " + ad + " klasörüne eklendi.");
                                    if (dinleyici != null) dinleyici.listeYenile();
                                    return;
                                }

                                final List<UygulamaBilgisi> sonAdaylar = adaylar;
                                CharSequence[] adlar = new CharSequence[sonAdaylar.size()];
                                final boolean[] secimler = new boolean[sonAdaylar.size()];
                                for (int i = 0; i < sonAdaylar.size(); i++) {
                                    adlar[i] = sonAdaylar.get(i).etiket;
                                }
                                new AlertDialog.Builder(aktivite)
                                    .setTitle("En az 1 uygulama daha seçin (" + ad + ")")
                                    .setMultiChoiceItems(adlar, secimler, new DialogInterface.OnMultiChoiceClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int idx, boolean isChecked) {
                                            secimler[idx] = isChecked;
                                        }
                                    })
                                    .setPositiveButton(R.string.tamam, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            List<String> secilenKimlikler = new ArrayList<String>();
                                            secilenKimlikler.add(appKimlik);
                                            for (int i = 0; i < secimler.length; i++) {
                                                if (secimler[i]) {
                                                    secilenKimlikler.add(sonAdaylar.get(i).getBilesenKimligi());
                                                }
                                            }
                                            if (secilenKimlikler.size() < 2) {
                                                ArayuzYardimcisi.toast(aktivite, "Klasör için en az 2 uygulama gerekir.");
                                                return;
                                            }
                                            if (veri != null && (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU || veri.kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK)) {
                                                MasaustuModelDeposu.ogeKaldir(aktivite, veri.itemId, veri.kaynakTuru, veri.kaynakSayfa, appKimlik);
                                            }
                                            MasaustuModelDeposu.klasorOlusturVeCokluEkle(aktivite, aktifSayfa, ad.trim(), secilenKimlikler);
                                            ArayuzYardimcisi.toast(aktivite, ad + " klasörü oluşturuldu.");
                                            if (dinleyici != null) dinleyici.listeYenile();
                                        }
                                    })
                                    .setNegativeButton(R.string.iptal, null)
                                    .show();
                            }
                        });
                    } else {
                        KlasorYoneticisi.KlasorBilgisi secilen = klasorler.get(which - 1);
                        boolean eklendi;
                        if (veri != null) {
                            eklendi = MasaustuModelDeposu.klasoreOgeTasi(aktivite, secilen.id, veri);
                        } else {
                            eklendi = MasaustuModelDeposu.klasoreUygulamaEkle(aktivite, secilen.id, app.getBilesenKimligi());
                        }
                        if (eklendi) {
                            ArayuzYardimcisi.toast(aktivite, aktivite.getString(R.string.klasore_eklendi_mesaj, app.etiket, secilen.ad));
                            if (dinleyici != null) dinleyici.listeYenile();
                        } else {
                            ArayuzYardimcisi.toast(aktivite, app.etiket + " zaten bu klasörde var.");
                        }
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private static void uygulamaKilitDegistirIsle(final Activity aktivite, final UygulamaBilgisi app, boolean kilitli, final IslemDinleyici dinleyici) {
        if (!kilitli) {
            if (!UygulamaKilidiYoneticisi.pinAyarliMi(aktivite)) {
                UygulamaKilidiYoneticisi.yeniPinBelirleDiyalogu(aktivite, new Runnable() {
                    @Override
                    public void run() {
                        UygulamaKilidiYoneticisi.kilitle(aktivite, app.paketAdi);
                        ArayuzYardimcisi.toast(aktivite, "🔒 " + app.etiket + " kilitlendi.");
                        if (dinleyici != null) dinleyici.listeYenile();
                    }
                });
            } else {
                UygulamaKilidiYoneticisi.kilitle(aktivite, app.paketAdi);
                ArayuzYardimcisi.toast(aktivite, "🔒 " + app.etiket + " kilitlendi.");
                if (dinleyici != null) dinleyici.listeYenile();
            }
        } else {
            UygulamaKilidiYoneticisi.pinSorgulaVeCalistir(aktivite, new Runnable() {
                @Override
                public void run() {
                    UygulamaKilidiYoneticisi.kilidiKaldir(aktivite, app.paketAdi);
                    ArayuzYardimcisi.toast(aktivite, "🔓 " + app.etiket + " kilidi kaldırıldı.");
                    if (dinleyici != null) dinleyici.listeYenile();
                }
            });
        }
    }

    private static void menudeGizleIsle(Activity aktivite, UygulamaBilgisi app, IslemDinleyici dinleyici) {
        List<String> gizliler = YedeklemeYoneticisi.gizlenenleriAl(aktivite);
        if (!gizliler.contains(app.paketAdi)) {
            gizliler.add(app.paketAdi);
            YedeklemeYoneticisi.gizlenenleriKaydet(aktivite, gizliler);
            ArayuzYardimcisi.toast(aktivite, app.etiket + " menüden gizlendi.");
            if (dinleyici != null) dinleyici.listeYenile();
        }
    }

    private static void kaydirmaEylemiSecIsle(Activity aktivite, UygulamaBilgisi app, final IslemDinleyici dinleyici) {
        List<UygulamaBilgisi> apps = (aktivite instanceof AnaEkranActivity)
                ? ((AnaEkranActivity) aktivite).getTumUygulamalarListesi() : null;
        SimgeKaydirmaYoneticisi.secimDiyalogu(aktivite, app.paketAdi, app.etiket, apps, new Runnable() {
            @Override
            public void run() {
                if (dinleyici != null) dinleyici.listeYenile();
            }
        });
    }

    private static void uygulamaBilgisiAc(Context context, String paketAdi) {
        SistemAraclariYardimcisi.uygulamaBilgisiAc(context, paketAdi);
    }

    private static void playStoreAc(Context context, String paketAdi) {
        ArayuzYardimcisi.playStoreAc(context, paketAdi);
    }

    private static void uygulamayiSil(Context context, String paketAdi) {
        SistemAraclariYardimcisi.uygulamayiSil(context, paketAdi);
    }
}

