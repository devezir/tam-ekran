package com.tamekran.launcher;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tam Ekran - Masaustu Gorunum ve Canli Izgara Kontrolcusu
 * Cok sayfali masaustu sayfa olusturma, sayfa noktalari, rozet guncelleme
 * ve Duzen Studyosu canli onizleme metodlarini AnaEkranActivity disinda yonetir (SRP).
 */
public class MasaustuGorunumKontrolcusu {

    private final AnaEkranActivity aktivite;
    private final SayfaliMasaustuGorunumu masaustuSayfaliGorunum;
    private final LinearLayout kapsayiciSayfaNoktalari;
    private final List<UygulamaBilgisi> tumUygulamalarListesi;
    private final List<CellLayout> sayfaCellLayoutlari = new ArrayList<CellLayout>();

    public MasaustuGorunumKontrolcusu(
            AnaEkranActivity aktivite,
            SayfaliMasaustuGorunumu masaustuSayfaliGorunum,
            LinearLayout kapsayiciSayfaNoktalari,
            List<UygulamaBilgisi> tumUygulamalarListesi) {
        this.aktivite = aktivite;
        this.masaustuSayfaliGorunum = masaustuSayfaliGorunum;
        this.kapsayiciSayfaNoktalari = kapsayiciSayfaNoktalari;
        this.tumUygulamalarListesi = tumUygulamalarListesi;
    }

    public CellLayout getSayfaCellLayout(int sayfa) {
        if (sayfa < 0 || sayfa >= sayfaCellLayoutlari.size()) return null;
        return sayfaCellLayoutlari.get(sayfa);
    }

    public void setIzgaraBoyutuTumSayfalar(int sutun, int satir) {
        for (CellLayout cl : sayfaCellLayoutlari) {
            if (cl != null) cl.setGridBoyutu(sutun, satir);
        }
    }

    public void masaustuIzgaraGuncelle(int sutun, int satir, boolean kilavuz) {
        for (CellLayout cl : sayfaCellLayoutlari) {
            if (cl != null) {
                cl.setGridBoyutu(sutun, satir);
                cl.setKilavuzGoster(kilavuz);
            }
        }
    }

    public void canliMasaustuSimgeBoyutuGuncelle(int olcek) {
        int dp56 = ArayuzYardimcisi.dpToPx(aktivite, 56 * (olcek / 100.0f));
        for (CellLayout cl : sayfaCellLayoutlari) {
            if (cl != null) {
                for (int i = 0; i < cl.getChildCount(); i++) {
                    View child = cl.getChildAt(i);
                    View box = child.findViewById(R.id.kapsayici_simge);
                    if (box != null) {
                        ViewGroup.LayoutParams lp = box.getLayoutParams();
                        if (lp != null) {
                            lp.width = dp56;
                            lp.height = dp56;
                            box.setLayoutParams(lp);
                        }
                    }
                }
            }
        }
    }

    public void canliEtiketGosterGuncelle(boolean goster) {
        for (CellLayout cl : sayfaCellLayoutlari) {
            if (cl != null) {
                for (int i = 0; i < cl.getChildCount(); i++) {
                    View child = cl.getChildAt(i);
                    View etiket = child.findViewById(R.id.oge_etiket);
                    if (etiket != null) {
                        etiket.setVisibility(goster ? View.VISIBLE : View.GONE);
                    }
                }
            }
        }
    }

    public void canliMasaustuDolguGuncelle(int dolguModu) {
        int dolguDp = (dolguModu == 0) ? 0 : (dolguModu == 2 ? 18 : 8);
        int px = ArayuzYardimcisi.dpToPx(aktivite, dolguDp);
        for (CellLayout cl : sayfaCellLayoutlari) {
            if (cl != null) {
                cl.setPadding(px, 0, px, 0);
            }
        }
    }

    public void canliKlasorSekliGuncelle(int sekil) {
        KlasorSimgeUretici.setKlasorSekli(aktivite, sekil);
        KlasorSimgeUretici.onbellegiTemizle();
        masaustuSayfalariniTazele();
        aktivite.dockYukle();
    }

    public void canliGecisEfektiGuncelle(int efekt) {
        if (masaustuSayfaliGorunum != null) {
            masaustuSayfaliGorunum.setGecisEfekti(efekt);
        }
    }

    public void masaustuRozetleriniGuncelle() {
        for (CellLayout cl : sayfaCellLayoutlari) {
            if (cl == null) continue;
            int childCount = cl.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = cl.getChildAt(i);
                if (child != null && child.getTag() instanceof UygulamaBilgisi) {
                    UygulamaBilgisi app = (UygulamaBilgisi) child.getTag();
                    TextView rozet = (TextView) child.findViewById(R.id.oge_rozet);
                    rozetGuncelle(rozet, app.paketAdi);
                }
            }
        }
    }

    private void rozetGuncelle(TextView rozet, String paketAdi) {
        ArayuzYardimcisi.rozetGorunumuBagla(aktivite, rozet, paketAdi);
    }

    public void sayfaNoktalariniGuncelle(int aktifSayfa, int toplamSayfa) {
        if (kapsayiciSayfaNoktalari == null) return;
        kapsayiciSayfaNoktalari.removeAllViews();

        if (toplamSayfa <= 1) {
            kapsayiciSayfaNoktalari.setVisibility(View.GONE);
            return;
        }

        kapsayiciSayfaNoktalari.setVisibility(View.VISIBLE);
        int dp8 = ArayuzYardimcisi.dpToPx(aktivite, 8);
        int vurgu = TemaYoneticisi.getVurguRengi(aktivite);

        for (int i = 0; i < toplamSayfa; i++) {
            final int sayfaIndeksi = i;
            TextView nokta = new TextView(aktivite);
            nokta.setText(i == aktifSayfa ? "●" : "○");
            nokta.setTextSize(i == aktifSayfa ? 13 : 11);
            nokta.setTextColor(i == aktifSayfa ? vurgu : Color.parseColor("#88FFFFFF"));
            nokta.setGravity(Gravity.CENTER);
            nokta.setPadding(dp8, 0, dp8, 0);
            nokta.setClickable(true);
            nokta.setFocusable(true);
            nokta.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (masaustuSayfaliGorunum != null) {
                        masaustuSayfaliGorunum.sayfayaGit(sayfaIndeksi, true);
                    }
                }
            });
            kapsayiciSayfaNoktalari.addView(nokta);
        }
    }

    public void sayfayiArtirimliTazele(int sayfaNo) {
        ikiSayfayiArtirimliTazele(sayfaNo, sayfaNo);
    }

    /**
     * Sürükle-bırak sonrası tüm masaüstü ağacını yıkmak yerine yalnızca etkilenen
     * kaynak ve hedef sayfaların CellLayout görünümlerini artımlı (incremental) olarak günceller.
     */
    public void ikiSayfayiArtirimliTazele(int kaynakSayfa, int hedefSayfa) {
        if (masaustuSayfaliGorunum == null) {
            masaustuSayfalariniTazele();
            return;
        }
        MasaustuModelDeposu.ModelDurumu model = MasaustuModelDeposu.yukle(aktivite);
        int toplamSayfa = MasaustuYoneticisi.getSayfaSayisi(aktivite);
        if (sayfaCellLayoutlari.size() != toplamSayfa) {
            masaustuSayfalariniTazele();
            return;
        }

        List<KlasorYoneticisi.KlasorBilgisi> tumKlasorler = KlasorYoneticisi.klasorleriYukle(aktivite);
        int sutun = AyarBankasi.getMasaustuSutun(aktivite);
        int satir = AyarBankasi.getMasaustuSatir(aktivite);

        if (kaynakSayfa >= 0 && kaynakSayfa < sayfaCellLayoutlari.size()) {
            CellLayout kaynakCl = sayfaCellLayoutlari.get(kaynakSayfa);
            if (kaynakCl != null) {
                sayfaCellLayoutDoldur(kaynakCl, kaynakSayfa, model.sayfaOgeleri(kaynakSayfa), tumKlasorler, sutun, satir);
            }
        }
        if (hedefSayfa != kaynakSayfa && hedefSayfa >= 0 && hedefSayfa < sayfaCellLayoutlari.size()) {
            CellLayout hedefCl = sayfaCellLayoutlari.get(hedefSayfa);
            if (hedefCl != null) {
                sayfaCellLayoutDoldur(hedefCl, hedefSayfa, model.sayfaOgeleri(hedefSayfa), tumKlasorler, sutun, satir);
            }
        }
    }

    public void masaustuSayfalariniTazele() {
        if (masaustuSayfaliGorunum == null) return;

        int toplamSayfa = MasaustuYoneticisi.getSayfaSayisi(aktivite);
        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> kaydedilmisSayfalar = MasaustuYoneticisi.sayfalariYukle(aktivite);
        List<KlasorYoneticisi.KlasorBilgisi> tumKlasorler = KlasorYoneticisi.klasorleriYukle(aktivite);

        int sutun = AyarBankasi.getMasaustuSutun(aktivite);
        int satir = AyarBankasi.getMasaustuSatir(aktivite);

        boolean hicUygulamaYok = true;
        for (List<MasaustuYoneticisi.MasaustuOgesi> liste : kaydedilmisSayfalar.values()) {
            if (liste != null && !liste.isEmpty()) {
                hicUygulamaYok = false;
                break;
            }
        }

        if (hicUygulamaYok && !tumUygulamalarListesi.isEmpty()) {
            if (MasaustuYoneticisi.sonVeriBozukMu()) {
                LauncherLog.e("MasaustuGorunumKontrolcusu", "Bozuk masaustu JSON tespit edildi; otomatik ilk kurulum ezmesi engellendi.");
                ArayuzYardimcisi.toastUzun(aktivite, aktivite.getString(R.string.veri_karantina_uyarisi));
            } else if (MasaustuYoneticisi.isTekKatmanModu(aktivite)) {
                MasaustuYoneticisi.tumUygulamalariMasaustuneSerp(aktivite, tumUygulamalarListesi);
                kaydedilmisSayfalar = MasaustuYoneticisi.sayfalariYukle(aktivite);
                toplamSayfa = MasaustuYoneticisi.getSayfaSayisi(aktivite);
            } else {
                int limit = Math.min(8, tumUygulamalarListesi.size());
                List<MasaustuYoneticisi.MasaustuOgesi> ilkSayfa = kaydedilmisSayfalar.get(0);
                if (ilkSayfa == null) {
                    ilkSayfa = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
                    kaydedilmisSayfalar.put(0, ilkSayfa);
                }
                for (int i = 0; i < limit; i++) {
                    int cx = i % sutun;
                    int cy = i / sutun;
                    ilkSayfa.add(new MasaustuYoneticisi.MasaustuOgesi(tumUygulamalarListesi.get(i).paketAdi, cx, cy));
                }
                MasaustuYoneticisi.sayfalariKaydet(aktivite, kaydedilmisSayfalar);
            }
        }

        masaustuSayfaliGorunum.removeAllViews();
        sayfaCellLayoutlari.clear();

        WidgetYoneticisi widgetYoneticisi = aktivite.getWidgetYoneticisi();

        for (int i = 0; i < toplamSayfa; i++) {
            final int sayfaNo = i;
            List<MasaustuYoneticisi.MasaustuOgesi> ogeler = kaydedilmisSayfalar.get(i);
            if (ogeler == null) {
                ogeler = new ArrayList<MasaustuYoneticisi.MasaustuOgesi>();
            }

            LinearLayout sayfaKapsayici = new LinearLayout(aktivite);
            sayfaKapsayici.setOrientation(LinearLayout.VERTICAL);
            sayfaKapsayici.setClipChildren(false);
            sayfaKapsayici.setClipToPadding(false);
            sayfaKapsayici.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));

            LinearLayout widgetKapsayici = new LinearLayout(aktivite);
            widgetKapsayici.setOrientation(LinearLayout.VERTICAL);
            widgetKapsayici.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            if (widgetYoneticisi != null) {
                List<WidgetYoneticisi.WidgetKaydi> widgetler = widgetYoneticisi.getSayfaWidgetleri(sayfaNo);
                if (!widgetler.isEmpty()) {
                    for (WidgetYoneticisi.WidgetKaydi wk : widgetler) {
                        View wView = widgetYoneticisi.widgetGorunumuOlustur(aktivite, wk.widgetId, new WidgetYoneticisi.WidgetIslemDinleyici() {
                            @Override
                            public void onWidgetlerGuncellendi() {
                                masaustuSayfalariniTazele();
                            }
                        });
                        if (wView != null) {
                            widgetKapsayici.addView(wView);
                        }
                    }
                }
            }

            if (widgetKapsayici.getChildCount() > 0) {
                sayfaKapsayici.addView(widgetKapsayici);
            }

            CellLayout cl = new CellLayout(aktivite);
            cl.setGridBoyutu(sutun, satir);
            cl.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            ));
            int dp6 = ArayuzYardimcisi.dpToPx(aktivite, 6);
            cl.setPadding(dp6, dp6, dp6, dp6);

            MasaustuModelDeposu.ModelDurumu model = MasaustuModelDeposu.yukle(aktivite);
            sayfaCellLayoutDoldur(cl, sayfaNo, model.sayfaOgeleri(sayfaNo), tumKlasorler, sutun, satir);

            sayfaCellLayoutlari.add(cl);
            sayfaKapsayici.addView(cl);
            masaustuSayfaliGorunum.addView(sayfaKapsayici);
        }

        sayfaNoktalariniGuncelle(masaustuSayfaliGorunum.getAktifSayfa(), toplamSayfa);
    }

    private void sayfaCellLayoutDoldur(
            CellLayout cl,
            final int sayfaNo,
            List<MasaustuKaydi> ogeler,
            List<KlasorYoneticisi.KlasorBilgisi> tumKlasorler,
            int sutun,
            int satir) {
        if (cl == null) return;
        cl.removeAllViews();
        cl.setGridBoyutu(sutun, satir);

        List<UygulamaBilgisi> kisayollar = KisayolYoneticisi.sayfadakiKisayollariAl(aktivite, sayfaNo);

        if (ogeler != null) {
            for (final MasaustuKaydi oge : ogeler) {
                if (oge == null || oge.bilesenKimligi == null) continue;

                if (HizliAraclarYoneticisi.ARAC_SERIT_GUC.equals(oge.bilesenKimligi)
                        || HizliAraclarYoneticisi.ARAC_SERIT_ARAMA.equals(oge.bilesenKimligi)) {
                    seritGorunumuBagla(cl, sayfaNo, oge);
                    continue;
                }

                UygulamaBilgisi app = UygulamaBilgisi.kimliktenCozumle(
                        aktivite, oge.bilesenKimligi, tumUygulamalarListesi, tumKlasorler);
                if (app != null) {
                    View iconView = masaustuOgeGorunumuOlustur(app, sayfaNo, oge.cellX, oge.cellY, oge.id);
                    CellLayout.LayoutParams lp = new CellLayout.LayoutParams(oge.cellX, oge.cellY, oge.spanX, oge.spanY);
                    cl.addView(iconView, lp);
                }
            }
        }

        for (UygulamaBilgisi k : kisayollar) {
            boolean zatenVar = false;
            if (ogeler != null) {
                for (MasaustuKaydi o : ogeler) {
                    if (DockYoneticisi.bilesenEslesiyorMu(o.bilesenKimligi, k.getBilesenKimligi())) {
                        zatenVar = true;
                        break;
                    }
                }
            }
            if (!zatenVar) {
                int kX = 0, kY = 0;
                boolean b = false;
                for (int r = 0; r < satir; r++) {
                    for (int c = 0; c < sutun; c++) {
                        if (!cl.isCellOccupied(c, r)) {
                            kX = c; kY = r; b = true; break;
                        }
                    }
                    if (b) break;
                }
                MasaustuYoneticisi.sayfayaOgeYerlestir(aktivite, sayfaNo, k.paketAdi, kX, kY);
                View iconView = masaustuOgeGorunumuOlustur(k, sayfaNo, kX, kY, null);
                CellLayout.LayoutParams lp = new CellLayout.LayoutParams(kX, kY, 1, 1);
                cl.addView(iconView, lp);
            }
        }
    }

    private void seritGorunumuBagla(CellLayout cl, final int sayfaNo, final MasaustuKaydi oge) {
        final MasaustuYoneticisi.MasaustuOgesi buOge = new MasaustuYoneticisi.MasaustuOgesi(
                oge.bilesenKimligi, oge.cellX, oge.cellY, oge.spanX, oge.spanY);
        final boolean gucSeridi = HizliAraclarYoneticisi.ARAC_SERIT_GUC.equals(oge.bilesenKimligi);
        String baslik = gucSeridi ? "⚡ Güç Denetimi Şeridi" : "🔍 Hızlı Arama & Ayarlar";
        Drawable seritSimge = HizliAraclarYoneticisi.cizAracSimgesi(aktivite, oge.bilesenKimligi);
        final UygulamaBilgisi seritApp = new UygulamaBilgisi(baslik, oge.bilesenKimligi, "", seritSimge);

        View.OnLongClickListener uzunBasma = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return aktivite.suruklemeBaslat(
                        v,
                        new SuruklemeVerisi(seritApp, SuruklemeVerisi.KAYNAK_MASAUSTU,
                                sayfaNo, buOge.cellX, buOge.cellY, buOge.spanX, buOge.spanY).withItemId(oge.id)
                );
            }
        };

        View seritView = gucSeridi
                ? HizliAraclarYoneticisi.seritGorunumuOlustur(aktivite, sayfaNo, buOge, uzunBasma, new Runnable() {
                    @Override
                    public void run() {
                        sayfayiArtirimliTazele(sayfaNo);
                    }
                })
                : HizliAraclarYoneticisi.aramaSeritGorunumuOlustur(aktivite, sayfaNo, buOge, uzunBasma, new Runnable() {
                    @Override
                    public void run() {
                        aktivite.cekmeceyiAc();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        aktivite.sesliAramaBaslat();
                    }
                });

        if (seritView != null) {
            seritView.setTag(seritApp);
            CellLayout.LayoutParams lp = new CellLayout.LayoutParams(oge.cellX, oge.cellY, oge.spanX, oge.spanY);
            cl.addView(seritView, lp);
        }
    }

    private View masaustuOgeGorunumuOlustur(final UygulamaBilgisi app, final int sayfaNo, final int cellX, final int cellY, final String itemId) {
        View view = aktivite.getLayoutInflater().inflate(R.layout.oge_uygulama, null, false);
        view.setTag(app);

        boolean etiketGoster = AyarBankasi.isEtiketlerGoster(aktivite);
        int olcek = AyarBankasi.getMasaustuSimgeBoyutu(aktivite);
        ArayuzYardimcisi.uygulamaGorunumuBagla(aktivite, view, app, etiketGoster, olcek);

        final SuruklemeVerisi suruklemeVerisi = new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_MASAUSTU, sayfaNo, cellX, cellY).withItemId(itemId);
        Runnable yukariKaydirma = (app.paketAdi != null && app.paketAdi.startsWith("klasor:"))
                ? new Runnable() {
                    @Override
                    public void run() {
                        aktivite.klasoruAc(app.paketAdi, true);
                    }
                }
                : null;
        NovaUzunBasmaYardimcisi.bagla(aktivite, view, new NovaUzunBasmaYardimcisi.UzunBasmaAksiyonu() {
            @Override
            public void onPopupGoster(View kaynakView) {
                HizliEylemlerDiyalogu.gosterNovaPopup(
                        aktivite,
                        kaynakView,
                        app,
                        suruklemeVerisi,
                        new HizliEylemlerDiyalogu.IslemDinleyici() {
                            @Override
                            public void listeYenile() {
                                aktivite.uygulamalariYukle();
                            }
                        },
                        null
                );
            }

            @Override
            public boolean onSuruklemeBaslat(View kaynakView) {
                return aktivite.suruklemeBaslat(kaynakView, suruklemeVerisi);
            }

            @Override
            public void onKisaTiklama(View kaynakView) {
                if (app.paketAdi != null && app.paketAdi.startsWith("arac:")) {
                    HizliAraclarYoneticisi.araciCalistir(aktivite, app.paketAdi, new Runnable() {
                        @Override
                        public void run() {
                            masaustuSayfalariniTazele();
                            aktivite.dockYukle();
                        }
                    });
                    return;
                }
                if (app.paketAdi != null && app.paketAdi.startsWith("klasor:")) {
                    aktivite.klasoruAc(app.paketAdi);
                } else {
                    aktivite.uygulamaBaslat(app);
                }
            }
        }, yukariKaydirma);

        return view;
    }
}
