package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Sayfalı İnteraktif Klasör Penceresi (Nova Style Overlay Folder)
 * AnaEkranActivity kok_duzeni üzerinde yüzen kart olarak açılır;
 * hem simge üstü Nova Popup'ı hem de parmağı klasör dışına sürükleyerek masaüstüne/dock'a çıkarmayı destekler.
 */
public class KlasorPenceresi {

    public static final int SAYFA_BASINA_OGE = 12; // 4 sütun x 3 satır ızgara standardı

    private static ViewGroup aktifBackdrop = null;
    private static Runnable aktifOnKapatildi = null;
    private static Handler klasorHandler = null;
    private static Runnable bekleyenCikisKapatma = null;

    private static Handler getKlasorHandler() {
        if (klasorHandler == null) {
            klasorHandler = new Handler(Looper.getMainLooper());
        }
        return klasorHandler;
    }

    public static boolean acikMi() {
        return aktifBackdrop != null && aktifBackdrop.getParent() != null;
    }

    public static void kapat() {
        cikisZamanlayicisiniIptalEt();
        if (aktifBackdrop != null) {
            ViewGroup parent = (ViewGroup) aktifBackdrop.getParent();
            if (parent != null) {
                parent.removeView(aktifBackdrop);
            }
            aktifBackdrop = null;
        }
        if (aktifOnKapatildi != null) {
            Runnable cb = aktifOnKapatildi;
            aktifOnKapatildi = null;
            cb.run();
        }
    }

    private static void cikisZamanlayicisiniIptalEt() {
        if (bekleyenCikisKapatma != null && klasorHandler != null) {
            klasorHandler.removeCallbacks(bekleyenCikisKapatma);
            bekleyenCikisKapatma = null;
        }
    }

    public interface KlasorGuncellemeDinleyicisi {
        void onKlasorGuncellendi();
    }

    /**
     * Toplam öğe sayısına göre sayfa adedini hesaplar.
     */
    public static int toplamSayfaHesapla(int toplamOge) {
        if (toplamOge <= 0) return 1;
        return (toplamOge + SAYFA_BASINA_OGE - 1) / SAYFA_BASINA_OGE;
    }

    /**
     * Belirtilen sayfa indeksine (0-tabanlı) düşen öğeleri dilimler.
     */
    public static List<UygulamaBilgisi> sayfaOgesiGetir(List<UygulamaBilgisi> tumu, int sayfa) {
        List<UygulamaBilgisi> sonuc = new ArrayList<UygulamaBilgisi>();
        if (tumu == null || tumu.isEmpty()) return sonuc;

        int toplamSayfa = toplamSayfaHesapla(tumu.size());
        if (sayfa < 0) sayfa = 0;
        if (sayfa >= toplamSayfa) sayfa = toplamSayfa - 1;

        int baslangic = sayfa * SAYFA_BASINA_OGE;
        int bitis = Math.min(tumu.size(), baslangic + SAYFA_BASINA_OGE);
        for (int i = baslangic; i < bitis; i++) {
            sonuc.add(tumu.get(i));
        }
        return sonuc;
    }

    public static void ac(
        final Activity activity,
        final KlasorYoneticisi.KlasorBilgisi klasor,
        final List<UygulamaBilgisi> tumUygulamalar,
        final KlasorGuncellemeDinleyicisi dinleyici
    ) {
        ac(activity, klasor, tumUygulamalar, dinleyici, null);
    }

    public static void ac(
        final Activity activity,
        final KlasorYoneticisi.KlasorBilgisi klasor,
        final List<UygulamaBilgisi> tumUygulamalar,
        final KlasorGuncellemeDinleyicisi dinleyici,
        final Runnable onKapatildi
    ) {
        if (activity == null || klasor == null) return;
        kapat();

        final ViewGroup kokDuzeni = (ViewGroup) activity.findViewById(R.id.kok_duzeni);
        if (kokDuzeni == null) return;

        final List<UygulamaBilgisi> klasorUygulamalari = new ArrayList<UygulamaBilgisi>();
        for (String pkg : klasor.paketler) {
            UygulamaBilgisi app = UygulamaBilgisi.kimliktenCozumle(activity, pkg, tumUygulamalar, null);
            if (app != null) {
                klasorUygulamalari.add(app);
            }
        }

        final FrameLayout backdrop = new FrameLayout(activity);
        backdrop.setBackgroundColor(0x99000000);
        backdrop.setClickable(true);
        backdrop.setFocusable(true);
        backdrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HizliEylemlerDiyalogu.kapatAktifPopup();
                kapat();
            }
        });

        final LinearLayout kok = new LinearLayout(activity);
        kok.setOrientation(LinearLayout.VERTICAL);
        kok.setClickable(true);
        int pad = ArayuzYardimcisi.dpToPx(activity, 16);
        kok.setPadding(pad, pad, pad, pad);

        GradientDrawable kartBg = new GradientDrawable();
        kartBg.setColor(0xF01C1F26);
        kartBg.setCornerRadius(ArayuzYardimcisi.dpToPx(activity, 18));
        kartBg.setStroke(ArayuzYardimcisi.dpToPx(activity, 1), 0x33FFFFFF);
        kok.setBackgroundDrawable(kartBg);
        if (Build.VERSION.SDK_INT >= 21) {
            kok.setElevation(ArayuzYardimcisi.dpToPx(activity, 12));
        }

        // Üst Bar: Başlık ve Yeniden Adlandır
        LinearLayout ustBar = new LinearLayout(activity);
        ustBar.setOrientation(LinearLayout.HORIZONTAL);
        ustBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ustBar.setPadding(0, 0, 0, pad / 2);

        final TextView yaziBaslik = new TextView(activity);
        yaziBaslik.setText("📁 " + klasor.ad);
        yaziBaslik.setTextSize(18);
        yaziBaslik.setTextColor(Color.WHITE);
        yaziBaslik.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpBaslik = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        yaziBaslik.setLayoutParams(lpBaslik);
        ustBar.addView(yaziBaslik);

        Button dugmeDuzenle = new Button(activity);
        dugmeDuzenle.setText("✏️");
        dugmeDuzenle.setBackgroundResource(R.drawable.kart_arkaplan);
        dugmeDuzenle.setTextColor(Color.WHITE);
        int dp36 = ArayuzYardimcisi.dpToPx(activity, 36);
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(dp36, dp36);
        dugmeDuzenle.setLayoutParams(lpBtn);
        ustBar.addView(dugmeDuzenle);
        kok.addView(ustBar);

        // İçerik Izgarası
        final GridView izgara = new GridView(activity);
        izgara.setNumColumns(4);
        izgara.setGravity(android.view.Gravity.CENTER);
        izgara.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        int h = ArayuzYardimcisi.dpToPx(activity, 220);
        izgara.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));

        final List<UygulamaBilgisi> goruntulenenUygulamalar = new ArrayList<UygulamaBilgisi>();
        final int[] aktifSayfaRef = new int[]{0};

        final KlasorUygulamaAdaptoru adaptor = new KlasorUygulamaAdaptoru(activity, goruntulenenUygulamalar, klasor.id);
        izgara.setAdapter(adaptor);
        kok.addView(izgara);

        // Sayfa Gösterge Çubuğu (◀  ● ○ ○  ▶)
        final LinearLayout sayfaCubugu = new LinearLayout(activity);
        sayfaCubugu.setOrientation(LinearLayout.HORIZONTAL);
        sayfaCubugu.setGravity(android.view.Gravity.CENTER_VERTICAL);
        sayfaCubugu.setPadding(0, pad / 4, 0, pad / 4);

        final Button btnOnceki = new Button(activity);
        btnOnceki.setText("◀");
        btnOnceki.setBackgroundColor(Color.TRANSPARENT);
        btnOnceki.setTextColor(Color.WHITE);
        btnOnceki.setTextSize(13);
        btnOnceki.setLayoutParams(new LinearLayout.LayoutParams(dp36, dp36));
        sayfaCubugu.addView(btnOnceki);

        final TextView yaziSayfaGosterge = new TextView(activity);
        yaziSayfaGosterge.setGravity(android.view.Gravity.CENTER);
        yaziSayfaGosterge.setTextColor(Color.WHITE);
        yaziSayfaGosterge.setTextSize(13);
        LinearLayout.LayoutParams lpGosterge = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        yaziSayfaGosterge.setLayoutParams(lpGosterge);
        sayfaCubugu.addView(yaziSayfaGosterge);

        final Button btnSonraki = new Button(activity);
        btnSonraki.setText("▶");
        btnSonraki.setBackgroundColor(Color.TRANSPARENT);
        btnSonraki.setTextColor(Color.WHITE);
        btnSonraki.setTextSize(13);
        btnSonraki.setLayoutParams(new LinearLayout.LayoutParams(dp36, dp36));
        sayfaCubugu.addView(btnSonraki);

        kok.addView(sayfaCubugu);

        // Sayfa Güncelleme Koordinatörü
        final Runnable sayfaGuncelleyici = new Runnable() {
            @Override
            public void run() {
                int toplam = klasorUygulamalari.size();
                int toplamSayfa = toplamSayfaHesapla(toplam);
                if (aktifSayfaRef[0] >= toplamSayfa) {
                    aktifSayfaRef[0] = Math.max(0, toplamSayfa - 1);
                }
                if (aktifSayfaRef[0] < 0) {
                    aktifSayfaRef[0] = 0;
                }

                goruntulenenUygulamalar.clear();
                goruntulenenUygulamalar.addAll(sayfaOgesiGetir(klasorUygulamalari, aktifSayfaRef[0]));
                adaptor.notifyDataSetChanged();

                if (toplamSayfa <= 1) {
                    sayfaCubugu.setVisibility(View.GONE);
                } else {
                    sayfaCubugu.setVisibility(View.VISIBLE);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < toplamSayfa; i++) {
                        if (i > 0) sb.append("  ");
                        sb.append(i == aktifSayfaRef[0] ? "●" : "○");
                    }
                    yaziSayfaGosterge.setText(sb.toString());
                    btnOnceki.setEnabled(aktifSayfaRef[0] > 0);
                    btnOnceki.setAlpha(aktifSayfaRef[0] > 0 ? 1.0f : 0.3f);
                    btnSonraki.setEnabled(aktifSayfaRef[0] < toplamSayfa - 1);
                    btnSonraki.setAlpha(aktifSayfaRef[0] < toplamSayfa - 1 ? 1.0f : 0.3f);
                }
            }
        };

        // Sayfa Gezinme Butonları
        btnOnceki.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (aktifSayfaRef[0] > 0) {
                    aktifSayfaRef[0]--;
                    sayfaGuncelleyici.run();
                }
            }
        });

        btnSonraki.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int toplam = klasorUygulamalari.size();
                int toplamSayfa = toplamSayfaHesapla(toplam);
                if (aktifSayfaRef[0] < toplamSayfa - 1) {
                    aktifSayfaRef[0]++;
                    sayfaGuncelleyici.run();
                }
            }
        });

        // Dokunmatik Yatay Kaydırma (Swipe Gesture)
        final float[] dokunmaX = new float[1];
        final float[] dokunmaY = new float[1];
        izgara.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dokunmaX[0] = event.getX();
                        dokunmaY[0] = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        float farkX = event.getX() - dokunmaX[0];
                        float farkY = event.getY() - dokunmaY[0];
                        if (Math.abs(farkX) > 80 && Math.abs(farkX) > Math.abs(farkY) * 1.5f) {
                            int toplamSayfa = toplamSayfaHesapla(klasorUygulamalari.size());
                            if (farkX < 0 && aktifSayfaRef[0] < toplamSayfa - 1) {
                                // Sola kaydırma -> Sonraki Sayfa
                                aktifSayfaRef[0]++;
                                sayfaGuncelleyici.run();
                                return true;
                            } else if (farkX > 0 && aktifSayfaRef[0] > 0) {
                                // Sağa kaydırma -> Önceki Sayfa
                                aktifSayfaRef[0]--;
                                sayfaGuncelleyici.run();
                                return true;
                            }
                        }
                        break;
                }
                return false;
            }
        });

        // Adaptör Dinleyicisi
        adaptor.setDinleyici(new KlasorUygulamaAdaptoru.KlasorEtkilesimDinleyicisi() {
            @Override
            public void onUygulamaTiklandi(UygulamaBilgisi app) {
                uygulamayiBaslat(activity, app);
            }

            @Override
            public void onUygulamaPopup(UygulamaBilgisi app, View kaynakView, int pozisyon) {
                final SuruklemeVerisi klasorVerisi = new SuruklemeVerisi(app, klasor.id, pozisyon);
                HizliEylemlerDiyalogu.gosterNovaPopup(
                        activity,
                        kaynakView,
                        app,
                        klasorVerisi,
                        new HizliEylemlerDiyalogu.IslemDinleyici() {
                            @Override
                            public void listeYenile() {
                                if (activity instanceof AnaEkranActivity) {
                                    ((AnaEkranActivity) activity).uygulamalariYukle();
                                }
                                if (dinleyici != null) dinleyici.onKlasorGuncellendi();
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                klasordenCikarIsle(activity, klasor, app, klasorUygulamalari, sayfaGuncelleyici, dinleyici);
                            }
                        }
                );
            }

            @Override
            public boolean onUygulamaSurukle(UygulamaBilgisi app, View kaynakView, int pozisyon) {
                if (activity instanceof AnaEkranActivity) {
                    return ((AnaEkranActivity) activity).suruklemeBaslat(
                            kaynakView,
                            new SuruklemeVerisi(app, klasor.id, pozisyon)
                    );
                }
                return false;
            }
        });

        // İlk sayfa içeriklerini yükle
        sayfaGuncelleyici.run();

        // Alt Eylem Butonları
        LinearLayout altBar = new LinearLayout(activity);
        altBar.setOrientation(LinearLayout.HORIZONTAL);
        altBar.setPadding(0, pad / 2, 0, 0);

        Button dugmeEkle = new Button(activity);
        dugmeEkle.setText("+ Uygulama Ekle");
        dugmeEkle.setBackgroundResource(R.drawable.kart_arkaplan);
        dugmeEkle.setTextColor(activity.getResources().getColor(R.color.renk_vurgu));
        int dp40 = ArayuzYardimcisi.dpToPx(activity, 40);
        LinearLayout.LayoutParams lpEkle = new LinearLayout.LayoutParams(0, dp40, 1.0f);
        lpEkle.setMargins(0, 0, 8, 0);
        dugmeEkle.setLayoutParams(lpEkle);
        altBar.addView(dugmeEkle);

        Button dugmeSil = new Button(activity);
        dugmeSil.setText("🗑️ Klasörü Sil");
        dugmeSil.setBackgroundResource(R.drawable.kart_arkaplan);
        dugmeSil.setTextColor(0xFFE57373);
        LinearLayout.LayoutParams lpSil = new LinearLayout.LayoutParams(0, dp40, 1.0f);
        dugmeSil.setLayoutParams(lpSil);
        altBar.addView(dugmeSil);

        kok.addView(altBar);

        int ekranW = activity.getResources().getDisplayMetrics().widthPixels;
        int kartW = Math.min(ekranW - ArayuzYardimcisi.dpToPx(activity, 32), ArayuzYardimcisi.dpToPx(activity, 340));
        FrameLayout.LayoutParams kartLp = new FrameLayout.LayoutParams(kartW, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        backdrop.addView(kok, kartLp);

        if (Build.VERSION.SDK_INT >= 11) {
            kok.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    int act = event.getAction();
                    if (act == DragEvent.ACTION_DRAG_STARTED) return true;
                    if (act == DragEvent.ACTION_DRAG_ENTERED || act == DragEvent.ACTION_DRAG_LOCATION) {
                        cikisZamanlayicisiniIptalEt();
                        return true;
                    }
                    return false;
                }
            });

            backdrop.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    switch (event.getAction()) {
                        case DragEvent.ACTION_DRAG_STARTED:
                            return true;
                        case DragEvent.ACTION_DRAG_ENTERED:
                        case DragEvent.ACTION_DRAG_LOCATION:
                            Object state = event.getLocalState();
                            boolean klasorden = (state instanceof SuruklemeVerisi)
                                    && ((SuruklemeVerisi) state).kaynakTuru == SuruklemeVerisi.KAYNAK_KLASOR;
                            if (klasorden && bekleyenCikisKapatma == null) {
                                bekleyenCikisKapatma = new Runnable() {
                                    @Override
                                    public void run() {
                                        bekleyenCikisKapatma = null;
                                        kapat();
                                    }
                                };
                                getKlasorHandler().postDelayed(bekleyenCikisKapatma, 220);
                            }
                            return true;
                        case DragEvent.ACTION_DRAG_EXITED:
                        case DragEvent.ACTION_DROP:
                        case DragEvent.ACTION_DRAG_ENDED:
                            cikisZamanlayicisiniIptalEt();
                            return false;
                    }
                    return false;
                }
            });
        }

        // Klasör adını düzenle
        dugmeDuzenle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ArayuzYardimcisi.kilitliMiVeUyar(activity)) {
                    return;
                }
                AyarDiyalogYardimcisi.metinGirdiDiyalogu(activity, "Klasör Adını Değiştir", klasor.ad, null, new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
                    @Override
                    public void onMetinGirildi(String yeniAd) {
                        if (yeniAd.length() > 0) {
                            klasor.ad = yeniAd;
                            MasaustuModelDeposu.klasorAdiDegistir(activity, klasor.id, yeniAd);
                            yaziBaslik.setText("📁 " + yeniAd);
                            if (dinleyici != null) dinleyici.onKlasorGuncellendi();
                        }
                    }
                });
            }
        });

        // Klasöre uygulama ekle checklist
        dugmeEkle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ArayuzYardimcisi.kilitliMiVeUyar(activity)) {
                    return;
                }
                final List<UygulamaBilgisi> eklenebilirler = new ArrayList<UygulamaBilgisi>();
                for (UygulamaBilgisi app : tumUygulamalar) {
                    if (app == null) continue;
                    String kimlik = app.getBilesenKimligi();
                    boolean zatenVar = false;
                    for (String p : klasor.paketler) {
                        if (DockYoneticisi.bilesenEslesiyorMu(p, kimlik)) {
                            zatenVar = true;
                            break;
                        }
                    }
                    if (!zatenVar) {
                        eklenebilirler.add(app);
                    }
                }

                CharSequence[] basliklar = new CharSequence[eklenebilirler.size()];
                final boolean[] secimler = new boolean[eklenebilirler.size()];
                for (int i = 0; i < eklenebilirler.size(); i++) {
                    basliklar[i] = eklenebilirler.get(i).etiket;
                }

                new AlertDialog.Builder(activity)
                    .setTitle("Klasöre Ekle: " + klasor.ad)
                    .setMultiChoiceItems(basliklar, secimler, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which, boolean isChecked) {
                            secimler[which] = isChecked;
                        }
                    })
                    .setPositiveButton(R.string.tamam, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int which) {
                            boolean eklendi = false;
                            for (int i = 0; i < secimler.length; i++) {
                                if (secimler[i]) {
                                    UygulamaBilgisi app = eklenebilirler.get(i);
                                    String appKimlik = app.getBilesenKimligi();
                                    boolean eklendiSonuc = MasaustuModelDeposu.klasoreUygulamaEkle(activity, klasor.id, appKimlik);
                                    if (eklendiSonuc) {
                                        klasor.paketler.add(appKimlik);
                                        klasorUygulamalari.add(app);
                                        eklendi = true;
                                    }
                                }
                            }
                            if (eklendi) {
                                sayfaGuncelleyici.run();
                                if (dinleyici != null) dinleyici.onKlasorGuncellendi();
                                ArayuzYardimcisi.toast(activity, "Uygulamalar klasöre eklendi.");
                            }
                        }
                    })
                    .setNegativeButton(R.string.iptal, null)
                    .show();
            }
        });

        // Klasörü sil
        dugmeSil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ArayuzYardimcisi.kilitliMiVeUyar(activity)) {
                    return;
                }
                AyarDiyalogYardimcisi.onayDiyalogu(
                    activity,
                    "Klasörü Sil",
                    "'" + klasor.ad + "' klasörünü silmek istediğinize emin misiniz? İçindeki uygulamalar masaüstüne yerleştirilecektir.",
                    "Sil",
                    new Runnable() {
                        @Override
                        public void run() {
                            MasaustuModelDeposu.klasoruSilVeMasaustuneDok(activity, klasor.id);
                            kapat();
                            if (dinleyici != null) dinleyici.onKlasorGuncellendi();
                            ArayuzYardimcisi.toast(activity, klasor.ad + " klasörü silindi, uygulamalar masaüstüne çıkarıldı.");
                        }
                    }
                );
            }
        });

        aktifBackdrop = backdrop;
        aktifOnKapatildi = onKapatildi;
        kokDuzeni.addView(backdrop, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        backdrop.bringToFront();
    }

    private static void uygulamayiBaslat(final Activity activity, final UygulamaBilgisi app) {
        ArayuzYardimcisi.guvenliUygulamaBaslat(activity, app, new Runnable() {
            @Override
            public void run() {
                kapat();
            }
        });
    }

    private static void klasordenCikarIsle(
        final Activity activity,
        final KlasorYoneticisi.KlasorBilgisi klasor,
        final UygulamaBilgisi secilen,
        final List<UygulamaBilgisi> klasorUygulamalari,
        final Runnable sayfaGuncelleyici,
        final KlasorGuncellemeDinleyicisi dinleyici
    ) {
        if (activity == null || secilen == null || klasor == null) return;
        if (ArayuzYardimcisi.kilitliMiVeUyar(activity)) {
            return;
        }
        String secilenKimlik = secilen.getBilesenKimligi();
        int sonuc = KlasorYoneticisi.klasordenUygulamaCikar(activity, klasor.id, secilenKimlik);

        if (sonuc == KlasorYoneticisi.CIKARIM_BASARISIZ) {
            ArayuzYardimcisi.toast(activity, "Uygulama çıkarılamadı.");
            return;
        }

        klasorUygulamalari.remove(secilen);

        if (sonuc == KlasorYoneticisi.CIKARIM_VE_COZULDU) {
            kapat();
            ArayuzYardimcisi.toast(activity, secilen.etiket + " masaüstüne çıkarıldı. Klasör tek uygulama kaldığı için çözüldü.");
        } else if (sonuc == KlasorYoneticisi.CIKARIM_VE_SILINDI) {
            kapat();
            ArayuzYardimcisi.toast(activity, secilen.etiket + " masaüstüne çıkarıldı. Klasör silindi.");
        } else {
            for (int i = klasor.paketler.size() - 1; i >= 0; i--) {
                if (DockYoneticisi.bilesenEslesiyorMu(klasor.paketler.get(i), secilenKimlik)) {
                    klasor.paketler.remove(i);
                }
            }
            if (sayfaGuncelleyici != null) sayfaGuncelleyici.run();
            ArayuzYardimcisi.toast(activity, secilen.etiket + " masaüstüne çıkarıldı.");
        }

        if (dinleyici != null) dinleyici.onKlasorGuncellendi();
    }

    private static class KlasorUygulamaAdaptoru extends BaseAdapter {
        public interface KlasorEtkilesimDinleyicisi {
            void onUygulamaTiklandi(UygulamaBilgisi app);
            void onUygulamaPopup(UygulamaBilgisi app, View kaynakView, int pozisyon);
            boolean onUygulamaSurukle(UygulamaBilgisi app, View kaynakView, int pozisyon);
        }

        private Activity activity;
        private List<UygulamaBilgisi> liste;
        private String klasorId;
        private KlasorEtkilesimDinleyicisi dinleyici;

        public KlasorUygulamaAdaptoru(Activity activity, List<UygulamaBilgisi> liste, String klasorId) {
            this.activity = activity;
            this.liste = liste;
            this.klasorId = klasorId;
        }

        public void setDinleyici(KlasorEtkilesimDinleyicisi dinleyici) {
            this.dinleyici = dinleyici;
        }

        @Override public int getCount() { return liste != null ? liste.size() : 0; }
        @Override public Object getItem(int position) { return liste != null ? liste.get(position) : null; }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(R.layout.oge_uygulama, parent, false);
            }

            final UygulamaBilgisi app = (UygulamaBilgisi) getItem(position);
            if (app != null) {
                ArayuzYardimcisi.uygulamaGorunumuBagla(activity, convertView, app);

                NovaUzunBasmaYardimcisi.bagla(activity, convertView, new NovaUzunBasmaYardimcisi.UzunBasmaAksiyonu() {
                    @Override
                    public void onPopupGoster(View kaynakView) {
                        if (dinleyici != null) dinleyici.onUygulamaPopup(app, kaynakView, position);
                    }

                    @Override
                    public boolean onSuruklemeBaslat(View kaynakView) {
                        return dinleyici != null && dinleyici.onUygulamaSurukle(app, kaynakView, position);
                    }

                    @Override
                    public void onKisaTiklama(View kaynakView) {
                        if (dinleyici != null) dinleyici.onUygulamaTiklandi(app);
                    }
                });
            }

            return convertView;
        }
    }
}
