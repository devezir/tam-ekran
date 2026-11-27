package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tam Ekran - Ana Ekran (Home Launcher) Etkinliği
 * Nova Launcher Seviyesi Tam Fonksiyonel Güç Paketi:
 * - Android Widget Desteği (AppWidgetHost / AppWidgetManager)
 * - İnteraktif Masaüstü Klasörleri & Klasör Kart Penceresi
 * - A-Z Hızlı Alfabe Kaydırma İndeksi (Fast Scroller)
 * - Çok Sayfalı Masaüstü (Multi-Page Workspace) & Sonsuz Kaydırma
 * - %100 Güvenilir Ekran Hareketleri & Çift Tıkla Ekran Kilidi
 * - Bildirim Rozetleri & Dinamik Sayaçlar (TeslaUnread / Dots)
 * - Menü Sekmeleri & Akıllı Otomatik Kategoriler
 * - Zengin Hızlı Eylemler Açılır Menüsü & Simge Boyutlandırma
 * Saf Java, %100 Türkçe, sıfır harici kütüphane bağımlılığı.
 */
public class AnaEkranActivity extends Activity implements BildirimDinleyiciServisi.BildirimGuncellemeDinleyicisi {

    private View kokDuzeni;
    private SayfaliMasaustuGorunumu masaustuSayfaliGorunum;
    private LinearLayout kapsayiciSayfaNoktalari;
    private GridView izgaraTumUygulamalar;
    private LinearLayout panelTumUygulamalar;
    private LinearLayout dockYuvalari;
    private LinearLayout satirSikKullanilanlar;
    private LinearLayout kapsayiciSikKullanilanlar;
    private LinearLayout kapsayiciKategoriSekmeleri;
    private LinearLayout cubukAlfabe;
    private View barHizliAramaKisayollari;
    private View kartHesapMakinesi;
    private TextView yaziHesapSonuc;
    private Button dugmeHesapKopyala;
    private SuruklemeKontrolcusu suruklemeKontrolcusu;
    private EditText girdiArama;
    private LinearLayout barGeriAl;
    private TextView yaziGeriAlBilgi;
    private TextView dugmeGeriAlEylem;
    private GeriAlKontrolcusu geriAlKontrolcusu;

    private List<UygulamaBilgisi> tumUygulamalarListesi = new ArrayList<UygulamaBilgisi>();
    private List<UygulamaBilgisi> filtrelenmisListe = new ArrayList<UygulamaBilgisi>();

    public List<UygulamaBilgisi> getTumUygulamalarListesi() {
        return tumUygulamalarListesi;
    }

    private MasaustuGorunumKontrolcusu masaustuGorunumKontrolcusu;
    private int aktifDurum = LauncherDurumu.NORMAL;

    public static final int REQUEST_VARSAYILAN_BASLATICI = 8001;
    public static final String EXTRA_AC_GUVENLI_ALAN = "ekstra_ac_guvenli_alan";
    public static final String EXTRA_AC_IZGARA_KALIBRATORU = "ekstra_ac_izgara_kalibratoru";
    public static final String EXTRA_IZGARA_MODU = "ekstra_izgara_modu";
    public static final String EXTRA_AC_GORSEL_AYAR = "ekstra_ac_gorsel_ayar";
    public static final String EXTRA_GORSEL_AYAR_TURU = "ekstra_gorsel_ayar_turu";
    public static final String EXTRA_AC_DOCK_KALIBRATORU = "ekstra_ac_dock_kalibratoru";
    private View guvenliAlanKilavuzu;
    private long sonOgeUzunBasmaZamani = 0;

    private CekmeceAdaptoru menuAdaptoru;
    private String aktifKategori = KategoriYoneticisi.KAT_TUMU;

    private GestureDetector hareketAlgilayici;
    private float ikiParmakBaslangicY = 0;
    private Handler zamanlayici = new Handler(Looper.getMainLooper());

    private SimgePaketiYoneticisi simgeMotoru;
    private String oncekiSimgePaketi = "";
    private long sonSayfaDegisimZamani = 0;

    // Ayar durum takibi (Gereksiz onResume yeniden yüklemelerini ve donmaları engellemek için)
    private int oncekiMasaustuSutun = -1;
    private int oncekiMasaustuSatir = -1;
    private int oncekiCekmeceSutun = -1;
    private int oncekiCekmeceSatir = -1;
    private int oncekiSayfaSayisi = -1;
    private int oncekiSiralama = -1;
    private int oncekiGizliHash = 0;
    private boolean oncekiEtiketGoster = true;
    private int oncekiSimgeBoyutu = -1;
    private int oncekiDockSayisi = -1;
    private int oncekiMasaustuModu = -1;
    private int oncekiMasaustuVeriHash = 0;
    private int oncekiKuruluUygulamaSayisi = -1;
    private long oncekiDegisiklikDamgasi = -1L;

    // Dinamik Sistem Olaylari, Eszamanli Yukleme ve Widget Motoru
    private SistemOlayYoneticisi sistemOlayYoneticisi;
    private final ExecutorService yukleyiciHavuzu = Executors.newSingleThreadExecutor();
    private final AtomicInteger yuklemeNesli = new AtomicInteger(0);
    private final Runnable gecikmeliYuklemeGorevi = new Runnable() {
        @Override
        public void run() {
            uygulamalariYukleSimdi();
        }
    };

    // Android Widget Motoru
    private WidgetYoneticisi widgetYoneticisi;
    private SallamaAlgilayici sallamaAlgilayici;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ana_ekran);

        simgeMotoru = new SimgePaketiYoneticisi(this);
        oncekiSimgePaketi = simgeMotoru.getAktifPaketAdi();
        widgetYoneticisi = new WidgetYoneticisi(this);
        if (savedInstanceState != null) {
            widgetYoneticisi.yukleDurum(savedInstanceState);
        }

        edgeToEdgeSistemCubuklariniAyarla(AyarBankasi.getBoolean(this, AyarlarActivity.PREF_TAM_EKRAN_MODU, false));
        bilesenleriBaslat();
        hareketleriAyarla();
        uygulamalariYukle();
        varsayilanBaslaticiKontrolu();
        kontrolEtKalibrasyonIntentleri(getIntent());

        sistemOlayYoneticisi = new SistemOlayYoneticisi(this, new SistemOlayYoneticisi.OlayDinleyicisi() {
            @Override
            public void onUygulamalarYenile() {
                uygulamalariYukle();
            }

            @Override
            public void onSistemRotasyonuDegisti() {
                masaustuSayfalariniTazele();
                dockYukle();
            }
        });
        sistemOlayYoneticisi.baslat();

        sallamaAlgilayici = new SallamaAlgilayici(this, new SallamaAlgilayici.SallamaDinleyici() {
            @Override
            public void onSallandi() {
                sallamaEylemiUygula();
            }
        });

        BildirimDinleyiciServisi.setGuncellemeDinleyicisi(this);

        PaketDegisimAlicisi.setDinleyici(new PaketDegisimAlicisi.PaketDegisimDinleyici() {
            @Override
            public void onPaketlerDegisti() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uygulamalariYukle();
                    }
                });
            }
        });

        if (Build.VERSION.SDK_INT >= 33) {
            GeriTusuYardimcisi.kaydet(this, new Runnable() {
                @Override
                public void run() {
                    geriTusuIsle();
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        kontrolEtKalibrasyonIntentleri(intent);
        anaEkranTusuIsle(intent);
    }

    private void anaEkranTusuIsle(Intent intent) {
        if (intent == null || !Intent.ACTION_MAIN.equals(intent.getAction()) || !intent.hasCategory(Intent.CATEGORY_HOME)) {
            return;
        }
        boolean katmanKapatildi = false;
        if (HizliEylemlerDiyalogu.aktifPopupAcikMi()) {
            HizliEylemlerDiyalogu.kapatAktifPopup();
            katmanKapatildi = true;
        }
        if (KlasorPenceresi.acikMi()) {
            KlasorPenceresi.kapat();
            katmanKapatildi = true;
        }
        if (suruklemeKontrolcusu != null && suruklemeKontrolcusu.isSurukleniyor()) {
            suruklemeKontrolcusu.suruklemeBitti();
            setAktifDurum(LauncherDurumu.NORMAL);
            katmanKapatildi = true;
        }
        if (panelTumUygulamalar != null && panelTumUygulamalar.getVisibility() == View.VISIBLE) {
            if (girdiArama != null && girdiArama.getText().length() > 0) {
                girdiArama.setText("");
            }
            cekmeceyiKapat();
            katmanKapatildi = true;
        }
        if (masaustuSayfaliGorunum != null && masaustuSayfaliGorunum.getAktifSayfa() != masaustuSayfaliGorunum.getVarsayilanSayfa()) {
            masaustuSayfaliGorunum.sayfayaGit(masaustuSayfaliGorunum.getVarsayilanSayfa(), true);
            katmanKapatildi = true;
        }
        if (!katmanKapatildi) {
            int eylem = HareketYoneticisi.eylemiAl(this, HareketYoneticisi.PREF_HAREKET_ANA_EKRAN, HareketYoneticisi.EYLEM_YOK);
            if (eylem != HareketYoneticisi.EYLEM_YOK) {
                HareketYoneticisi.eylemiCalistir(this, eylem);
            }
        }
    }

    private void kontrolEtKalibrasyonIntentleri(Intent intent) {
        KalibrasyonIntentYoneticisi.kontrolEt(this, zamanlayici, intent);
    }

    public int getAktifDurum() {
        return aktifDurum;
    }

    private void setAktifDurum(int yeniDurum) {
        this.aktifDurum = yeniDurum;
        if (suruklemeKontrolcusu != null && !suruklemeKontrolcusu.isSurukleniyor()) {
            suruklemeKontrolcusu.setDurum(yeniDurum);
        }
    }

    public void masaustuIzgaraGuncelle(int sutun, int satir, boolean kilavuz) {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.masaustuIzgaraGuncelle(sutun, satir, kilavuz);
        }
    }

    public void cekmeceIzgaraGuncelle(int sutun, int satir, boolean kilavuz) {
        if (panelTumUygulamalar != null && panelTumUygulamalar.getVisibility() != View.VISIBLE) {
            cekmeceyiAc();
        }
        if (izgaraTumUygulamalar != null) {
            izgaraTumUygulamalar.setNumColumns(sutun);
        }
        if (menuAdaptoru != null) {
            menuAdaptoru.setIzgara(sutun, satir, kilavuz);
        }
    }

    public void canliMasaustuSimgeBoyutuGuncelle(int olcek) {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.canliMasaustuSimgeBoyutuGuncelle(olcek);
        }
    }

    public void canliCekmeceSimgeBoyutuGuncelle(int olcek) {
        if (menuAdaptoru != null) {
            menuAdaptoru.setGeciciSimgeBoyutu(olcek);
        }
    }

    public void canliDockSimgeBoyutuGuncelle(int olcek) {
        if (dockYuvalari != null) {
            int dockYukseklikDp = AyarBankasi.getDockYuksekligi(this);
            int dikeyPayDp = Math.max(28, dockYukseklikDp - 18);
            float hedefSimgeDp = Math.min((float) dikeyPayDp, (dockYukseklikDp * 0.60f) * (olcek / 100.0f));
            hedefSimgeDp = Math.max(24.0f, hedefSimgeDp);
            int simgePx = ArayuzYardimcisi.dpToPx(this, (int) hedefSimgeDp);

            for (int i = 0; i < dockYuvalari.getChildCount(); i++) {
                View slot = dockYuvalari.getChildAt(i);
                if (slot instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) slot;
                    for (int j = 0; j < vg.getChildCount(); j++) {
                        View sub = vg.getChildAt(j);
                        if (sub instanceof ImageView) {
                            ViewGroup.LayoutParams lp = sub.getLayoutParams();
                            if (lp != null) {
                                lp.width = simgePx;
                                lp.height = simgePx;
                                sub.setLayoutParams(lp);
                            }
                        }
                    }
                }
            }
        }
    }

    public void canliSimgeBoyutuGuncelle(int olcek) {
        canliMasaustuSimgeBoyutuGuncelle(olcek);
        canliCekmeceSimgeBoyutuGuncelle(olcek);
        canliDockSimgeBoyutuGuncelle(olcek);
    }

    public void canliCekmeceSaydamlikGuncelle(int saydamlikYuzdesi) {
        if (panelTumUygulamalar != null) {
            if (panelTumUygulamalar.getVisibility() != View.VISIBLE) {
                cekmeceyiAc();
            }
            int alpha = saydamlikYuzdesi * 255 / 100;
            panelTumUygulamalar.setBackgroundColor(Color.argb(alpha, 26, 26, 26));
        }
    }

    public void canliDockSayisiGuncelle(int yeniSayi) {
        SharedPreferences sp = AyarBankasi.getAyarlar(this);
        sp.edit().putInt(YedeklemeYoneticisi.PREF_DOCK_SIMGE_SAYISI, yeniSayi).commit();
        dockYukle();
    }

    public void canliDockYuksekligiGuncelle(int yeniYukseklikDp) {
        View dockBar = findViewById(R.id.sabit_alt_cubuk);
        if (dockBar != null) {
            ViewGroup.LayoutParams lp = dockBar.getLayoutParams();
            if (lp != null) {
                lp.height = ArayuzYardimcisi.dpToPx(this, yeniYukseklikDp);
                dockBar.setLayoutParams(lp);
            }
        }
        dockYukle();
        if (masaustuSayfaliGorunum != null) {
            masaustuSayfaliGorunum.requestLayout();
        }
    }

    public void canliEtiketGosterGuncelle(boolean goster) {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.canliEtiketGosterGuncelle(goster);
        }
        if (menuAdaptoru != null) {
            menuAdaptoru.setGeciciEtiketGoster(goster);
        }
    }

    public void canliMasaustuDolguGuncelle(int dolguModu) {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.canliMasaustuDolguGuncelle(dolguModu);
        }
    }

    public void canliKlasorSekliGuncelle(int sekil) {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.canliKlasorSekliGuncelle(sekil);
        }
    }

    public void canliGecisEfektiGuncelle(int efekt) {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.canliGecisEfektiGuncelle(efekt);
        }
    }

    public void canliSikKullanilanlarGuncelle(boolean acik) {
        if (satirSikKullanilanlar != null) {
            if (acik) {
                SharedPreferences sp = AyarBankasi.getAyarlar(this);
                boolean eski = sp.getBoolean(AyarlarActivity.PREF_SIK_KULLANILANLAR_MODU, true);
                AyarBankasi.editorKaydet(sp.edit().putBoolean(AyarlarActivity.PREF_SIK_KULLANILANLAR_MODU, true));
                sikKullanilanlariGoster();
                AyarBankasi.editorKaydet(sp.edit().putBoolean(AyarlarActivity.PREF_SIK_KULLANILANLAR_MODU, eski));
            } else {
                satirSikKullanilanlar.setVisibility(View.GONE);
            }
        }
    }

    public void canliSiralamaGuncelle(int siralama) {
        CekmeceSiralamasi.setSiralamaTuru(this, siralama);
        CekmeceSiralamasi.sirala(this, tumUygulamalarListesi);
        if (menuAdaptoru != null) {
            menuAdaptoru.notifyDataSetChanged();
        }
    }

    public void canliDockStiliGuncelle(int stil) {
        View dockBar = findViewById(R.id.sabit_alt_cubuk);
        if (dockBar != null) {
            DockArkaPlanYoneticisi.stiliUygula(dockBar, this, stil);
        }
    }

    public void canliDockGorunurlukGuncelle(boolean gorunur) {
        View dockBar = findViewById(R.id.sabit_alt_cubuk);
        if (dockBar != null) {
            dockBar.setVisibility(gorunur ? View.VISIBLE : View.GONE);
        }
        if (masaustuSayfaliGorunum != null) {
            masaustuSayfaliGorunum.requestLayout();
        }
    }

    public void guvenliAlanKilavuzunuGoster(int solDp, int ustDp, int sagDp, int altDp) {
        if (guvenliAlanKilavuzu == null) return;
        guvenliAlanKilavuzunuGuncelle(solDp, ustDp, sagDp, altDp);
        guvenliAlanKilavuzu.setVisibility(View.VISIBLE);
    }

    public void guvenliAlanKilavuzunuGuncelle(int solDp, int ustDp, int sagDp, int altDp) {
        if (guvenliAlanKilavuzu == null) return;
        int solPx = ArayuzYardimcisi.dpToPx(this, solDp);
        int ustPx = ArayuzYardimcisi.dpToPx(this, ustDp);
        int sagPx = ArayuzYardimcisi.dpToPx(this, sagDp);
        int altPx = ArayuzYardimcisi.dpToPx(this, altDp);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        lp.setMargins(solPx, ustPx, sagPx, altPx);
        guvenliAlanKilavuzu.setLayoutParams(lp);
    }

    public void guvenliAlanKilavuzunuGizle() {
        if (guvenliAlanKilavuzu != null) {
            guvenliAlanKilavuzu.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (widgetYoneticisi != null) {
            widgetYoneticisi.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (widgetYoneticisi != null) {
            widgetYoneticisi.stopListening();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (widgetYoneticisi != null) {
            widgetYoneticisi.kaydetDurum(outState);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        HizliEylemlerDiyalogu.kapatAktifPopup();
        if (sallamaAlgilayici != null) {
            sallamaAlgilayici.durdur();
        }
    }

    private void sallamaEylemiUygula() {
        SharedPreferences ayarlar = AyarBankasi.getAyarlar(this);
        int eylem = ayarlar.getInt(SallamaAlgilayici.PREF_SALLAMA_EYLEMI, SallamaAlgilayici.EYLEM_KAPALI);
        switch (eylem) {
            case SallamaAlgilayici.EYLEM_KILITLE:
                EkranKilitleyici.kilitle(this);
                break;
            case SallamaAlgilayici.EYLEM_ARAMA:
            case SallamaAlgilayici.EYLEM_CEKMECE:
                cekmeceyiAc();
                break;
            case SallamaAlgilayici.EYLEM_BILDIRIMLER:
                HareketYoneticisi.eylemiCalistir(this, HareketYoneticisi.EYLEM_BILDIRIMLER);
                break;
            case SallamaAlgilayici.EYLEM_FENER:
                FenerYoneticisi.toggle(this);
                break;
            case SallamaAlgilayici.EYLEM_SES_MODU:
                SesModuYoneticisi.sonrakiModaGec(this);
                break;
        }
    }

    public int getAktifSayfa() {
        return masaustuSayfaliGorunum != null ? masaustuSayfaliGorunum.getAktifSayfa() : 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VARSAYILAN_BASLATICI) {
            if (varsayilanBaslaticiMi(this)) {
                ArayuzYardimcisi.toast(this, "Tam Ekran varsayılan başlatıcı olarak ayarlandı!");
            }
            return;
        }

        if (requestCode == OzelSimgeYoneticisi.KOD_GALERI_SIMGE_SEC && resultCode == RESULT_OK && data != null) {
            String guncellenenPaket = OzelSimgeYoneticisi.bekleyenGaleriSonucunuIsle(this, data.getData());
            if (guncellenenPaket != null) {
                SimgeOnbellegi.paketTemizle(guncellenenPaket);
                KlasorSimgeUretici.onbellegiTemizle();
                uygulamalariYukle();
                masaustuSayfalariniTazele();
                ArayuzYardimcisi.toast(this, getString(R.string.simge_guncellendi));
            } else {
                ArayuzYardimcisi.toast(this, "Seçilen görsel yüklenemedi.");
            }
            return;
        }

        if (requestCode == KisayolYoneticisi.KOD_KISAYOL_SEC && resultCode == RESULT_OK && data != null) {
            int sayfa = getAktifSayfa();
            if (KisayolYoneticisi.kisayolSonucunuKaydet(this, data, sayfa)) {
                ArayuzYardimcisi.toast(this, "Kısayol masaüstüne eklendi.");
                masaustuSayfalariniTazele();
            }
            return;
        }

        if (widgetYoneticisi != null) {
            widgetYoneticisi.onActivityResult(this, requestCode, resultCode, data, new WidgetYoneticisi.WidgetIslemDinleyici() {
                @Override
                public void onWidgetlerGuncellendi() {
                    masaustuSayfalariniTazele();
                }
            });
        }
    }

    private void varsayilanBaslaticiKontrolu() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
                if (!varsayilanBaslaticiMi(AnaEkranActivity.this)) {
                    new AlertDialog.Builder(AnaEkranActivity.this)
                        .setTitle("Varsayılan Başlatıcı")
                        .setMessage("Tam Ekran'ı telefonunuzun varsayılan ana ekranı olarak ayarlamak ister misiniz?")
                        .setPositiveButton("Varsayılan Yap", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                varsayilanBaslaticiAyarla(AnaEkranActivity.this);
                            }
                        })
                        .setNegativeButton("Şimdi Değil", null)
                        .show();
                }
            }
        }, 800);
    }

    public static boolean varsayilanBaslaticiMi(Context context) {
        return SistemAraclariYardimcisi.varsayilanBaslaticiMi(context);
    }

    public static void varsayilanBaslaticiAyarla(Activity activity) {
        SistemAraclariYardimcisi.varsayilanBaslaticiAyarla(activity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences ayarlar = AyarBankasi.getAyarlar(this);

        // 1. Gerçek Tam Ekran ve Edge-to-Edge Kontrolü
        boolean tamEkran = ayarlar.getBoolean(AyarlarActivity.PREF_TAM_EKRAN_MODU, false);
        edgeToEdgeSistemCubuklariniAyarla(tamEkran);

        // 2. Sabit Alt Çubuk (Dock) Görünürlüğü ve Yüksekliği
        View dockBar = findViewById(R.id.sabit_alt_cubuk);
        if (dockBar != null) {
            boolean dockGorunur = ayarlar.getBoolean(YedeklemeYoneticisi.PREF_DOCK_GORUNUR, true);
            int yeniGorunurluk = dockGorunur ? View.VISIBLE : View.GONE;
            boolean gorunurlukDegisti = dockBar.getVisibility() != yeniGorunurluk;
            dockBar.setVisibility(yeniGorunurluk);

            int dockH = AyarBankasi.getDockYuksekligi(this);
            ViewGroup.LayoutParams lp = dockBar.getLayoutParams();
            int pxH = ArayuzYardimcisi.dpToPx(this, dockH);
            if (lp != null && lp.height != pxH) {
                lp.height = pxH;
                dockBar.setLayoutParams(lp);
            }

            if (gorunurlukDegisti && masaustuSayfaliGorunum != null) {
                masaustuSayfaliGorunum.requestLayout();
            }
        }

        // 3. Çok Sayfalı Masaüstü Yapılandırması ve Geçiş Efekti
        if (masaustuSayfaliGorunum != null) {
            masaustuSayfaliGorunum.setSonsuzKaydirma(MasaustuYoneticisi.getSonsuzKaydirma(this));
            masaustuSayfaliGorunum.setVarsayilanSayfa(MasaustuYoneticisi.getVarsayilanSayfa(this));
            int efekt = ayarlar.getInt(YedeklemeYoneticisi.PREF_GECIS_EFEKTI, SayfaliMasaustuGorunumu.EFEKT_STANDART);
            masaustuSayfaliGorunum.setGecisEfekti(efekt);
            boolean wpKaydirma = ayarlar.getBoolean(YedeklemeYoneticisi.PREF_DUVAR_KAGIDI_KAYDIRMA, true);
            masaustuSayfaliGorunum.setDuvarKagidiKaydirma(wpKaydirma);
        }

        // 4. Izgara Sütun ve Satır Sayısı (Masaüstü ve Çekmece)
        int mSutun = AyarlarActivity.getMasaustuSutunSayisi(this);
        int mSatir = AyarlarActivity.getMasaustuSatirSayisi(this);
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.setIzgaraBoyutuTumSayfalar(mSutun, mSatir);
        }
        int cSutun = AyarlarActivity.getCekmeceSutunSayisi(this);
        int cSatir = AyarlarActivity.getCekmeceSatirSayisi(this);
        if (izgaraTumUygulamalar != null) {
            izgaraTumUygulamalar.setNumColumns(cSutun);
        }
        if (menuAdaptoru != null) {
            menuAdaptoru.setIzgara(cSutun, cSatir, false);
        }

        // 5. Simge Paketi Kontrolü
        simgeMotoru.tazele();
        String yeniPaket = simgeMotoru.getAktifPaketAdi();

        // 6. Sallama Hareketi Algılayıcı (Pil Tasarruflu)
        if (sallamaAlgilayici != null) {
            sallamaAlgilayici.baslat();
        }

        // 7. Çekmece Arka Plan Şeffaflığı
        if (panelTumUygulamalar != null) {
            int saydamlik = ayarlar.getInt(YedeklemeYoneticisi.PREF_CEKMECE_SAYDAMLIK, 100);
            int alpha = saydamlik * 255 / 100;
            panelTumUygulamalar.setBackgroundColor(Color.argb(alpha, 26, 26, 26));
        }

        // 8. Dock Stili
        View dockBarView = findViewById(R.id.sabit_alt_cubuk);
        DockArkaPlanYoneticisi.stiliUygula(dockBarView, this);

        // 9. Güvenli Alan ve Masaüstü Kenar Kırpması
        GuvenliAlanYoneticisi.uygula(this);

        // 10. Otomatik Yedekleme Tetikle
        OtomatikYedeklemeYoneticisi.kontrolEtVeYedekle(this);

        // 11. Değişiklik Tespiti ve Akıllı Yenileme (Performans Darboğazı ve Donmayı Önleme)
        int sayfaSayisi = MasaustuYoneticisi.getSayfaSayisi(this);
        int siralama = CekmeceSiralamasi.getSiralamaTuru(this);
        int gizliHash = YedeklemeYoneticisi.gizlenenleriAl(this).hashCode();
        boolean etiketGoster = ayarlar.getBoolean(YedeklemeYoneticisi.PREF_ETIKETLERI_GOSTER, true);
        int simgeBoyutu = ayarlar.getInt(YedeklemeYoneticisi.PREF_SIMGE_BOYUTU, 100);
        int dockSayisi = ayarlar.getInt(YedeklemeYoneticisi.PREF_DOCK_SIMGE_SAYISI, 4);
        int simdikiMod = MasaustuYoneticisi.getMasaustuModu(this);
        String jsonKey = MasaustuYoneticisi.isTekKatmanModu(this) ? MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFALARI_JSON : MasaustuYoneticisi.PREF_MASAUSTU_SAYFALARI_JSON;
        String simdikiMasaustuJson = ayarlar.getString(jsonKey, "{}");
        int simdikiMasaustuHash = simdikiMasaustuJson.hashCode();

        // Akıllı Paket Mutabakatı (Afteroid / F-Droid veya Sistemden silinen / eklenen uygulamaları anında tespit et)
        int anlikKuruluSayisi = 0;
        try {
            Intent anaNiyet = new Intent(Intent.ACTION_MAIN, null);
            anaNiyet.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resList = getPackageManager().queryIntentActivities(anaNiyet, 0);
            anlikKuruluSayisi = resList != null ? resList.size() : 0;
        } catch (Exception e) {
            LauncherLog.w("AnaEkranActivity", "Kurulu paket sayimi alinamadi", e);
        }

        long simdikiDegisiklikDamgasi = AyarBankasi.getDegisiklikDamgasi(this);
        boolean paketlerFarkli = (oncekiKuruluUygulamaSayisi != -1 && anlikKuruluSayisi != oncekiKuruluUygulamaSayisi)
                || PaketDegisimAlicisi.paketDegisikligiOldu;
        PaketDegisimAlicisi.paketDegisikligiOldu = false;
        oncekiKuruluUygulamaSayisi = anlikKuruluSayisi;

        boolean yapilandirmaDegisti = (mSutun != oncekiMasaustuSutun)
                || (mSatir != oncekiMasaustuSatir)
                || (cSutun != oncekiCekmeceSutun)
                || (cSatir != oncekiCekmeceSatir)
                || (sayfaSayisi != oncekiSayfaSayisi)
                || (simdikiMod != oncekiMasaustuModu)
                || (siralama != oncekiSiralama)
                || (gizliHash != oncekiGizliHash)
                || (etiketGoster != oncekiEtiketGoster)
                || (simgeBoyutu != oncekiSimgeBoyutu)
                || (dockSayisi != oncekiDockSayisi)
                || (simdikiMasaustuHash != oncekiMasaustuVeriHash)
                || (!oncekiSimgePaketi.equals(yeniPaket))
                || (simdikiDegisiklikDamgasi != oncekiDegisiklikDamgasi)
                || paketlerFarkli;

        if (tumUygulamalarListesi.isEmpty() || yapilandirmaDegisti) {
            if (!oncekiSimgePaketi.equals(yeniPaket)) {
                SimgeOnbellegi.tumunuTemizle();
                KlasorSimgeUretici.onbellegiTemizle();
            }
            oncekiMasaustuSutun = mSutun;
            oncekiMasaustuSatir = mSatir;
            oncekiCekmeceSutun = cSutun;
            oncekiCekmeceSatir = cSatir;
            oncekiSayfaSayisi = sayfaSayisi;
            oncekiMasaustuModu = simdikiMod;
            oncekiSiralama = siralama;
            oncekiGizliHash = gizliHash;
            oncekiEtiketGoster = etiketGoster;
            oncekiSimgeBoyutu = simgeBoyutu;
            oncekiDockSayisi = dockSayisi;
            oncekiMasaustuVeriHash = simdikiMasaustuHash;
            oncekiSimgePaketi = yeniPaket;
            oncekiDegisiklikDamgasi = simdikiDegisiklikDamgasi;

            uygulamalariYukle();
        } else {
            // Başlatıcıya normal dönüş: tüm masaüstü ağacını silip baştan kurma, sadece rozetleri güncelle
            tazeleRozetler();
        }
    }

    @Override
    public void onBildirimlerDegisti() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tazeleRozetler();
            }
        });
    }

    private void tazeleRozetler() {
        if (menuAdaptoru != null) menuAdaptoru.notifyDataSetChanged();
        masaustuRozetleriniGuncelle();
        dockRozetleriniGuncelle();
    }

    private void masaustuRozetleriniGuncelle() {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.masaustuRozetleriniGuncelle();
        }
    }

    private void dockRozetleriniGuncelle() {
        DockArayuzKontrolcusu.dockRozetleriniGuncelle(this, dockYuvalari);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HizliEylemlerDiyalogu.kapatAktifPopup();
        KlasorPenceresi.kapat();
        zamanlayici.removeCallbacks(gecikmeliYuklemeGorevi);
        yuklemeNesli.incrementAndGet();
        yukleyiciHavuzu.shutdownNow();
        BildirimDinleyiciServisi.setGuncellemeDinleyicisi(null);
        PaketDegisimAlicisi.setDinleyici(null);
        if (sistemOlayYoneticisi != null) {
            sistemOlayYoneticisi.durdur();
        }
        if (sallamaAlgilayici != null) {
            sallamaAlgilayici.durdur();
        }
        if (widgetYoneticisi != null) {
            widgetYoneticisi.temizle();
        }
    }

    private void edgeToEdgeSistemCubuklariniAyarla(boolean tamEkran) {
        try {
            android.view.Window w = getWindow();
            if (tamEkran) {
                w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            if (Build.VERSION.SDK_INT >= 21) {
                w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                w.setStatusBarColor(Color.TRANSPARENT);
                w.setNavigationBarColor(Color.TRANSPARENT);
                int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
                w.getDecorView().setSystemUiVisibility(flags);
            }
            if (Build.VERSION.SDK_INT >= 28) {
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                w.setAttributes(lp);
            }
        } catch (Exception e) {
            LauncherLog.d("AnaEkranActivity", "Sistem cubugu ayari atlandi: " + e.getMessage());
        }
    }

    private void bilesenleriBaslat() {
        kokDuzeni = findViewById(R.id.kok_duzeni);
        if (kokDuzeni != null && Build.VERSION.SDK_INT >= 21) {
            kokDuzeni.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public android.view.WindowInsets onApplyWindowInsets(View v, android.view.WindowInsets insets) {
                    GuvenliAlanYoneticisi.sistemInsetleriniGuncelle(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom()
                    );
                    GuvenliAlanYoneticisi.uygula(AnaEkranActivity.this);
                    return insets;
                }
            });
        }
        masaustuSayfaliGorunum = (SayfaliMasaustuGorunumu) findViewById(R.id.masaustu_sayfali_gorunum);
        kapsayiciSayfaNoktalari = (LinearLayout) findViewById(R.id.kapsayici_sayfa_noktalari);
        masaustuGorunumKontrolcusu = new MasaustuGorunumKontrolcusu(
                this, masaustuSayfaliGorunum, kapsayiciSayfaNoktalari, tumUygulamalarListesi);
        guvenliAlanKilavuzu = findViewById(R.id.guvenli_alan_kilavuzu);
        barGeriAl = (LinearLayout) findViewById(R.id.bar_geri_al);
        yaziGeriAlBilgi = (TextView) findViewById(R.id.yazi_geri_al_bilgi);
        dugmeGeriAlEylem = (TextView) findViewById(R.id.dugme_geri_al_eylem);
        geriAlKontrolcusu = new GeriAlKontrolcusu(this, barGeriAl, yaziGeriAlBilgi);
        if (dugmeGeriAlEylem != null) {
            dugmeGeriAlEylem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (geriAlKontrolcusu != null) {
                        geriAlKontrolcusu.geriAlYap(new Runnable() {
                            @Override
                            public void run() {
                                masaustuSayfalariniTazele();
                                dockYukle();
                            }
                        });
                    }
                }
            });
        }

        izgaraTumUygulamalar = (GridView) findViewById(R.id.izgara_tum_uygulamalar);
        panelTumUygulamalar = (LinearLayout) findViewById(R.id.panel_tum_uygulamalar);
        dockYuvalari = (LinearLayout) findViewById(R.id.dock_yuvalari);
        satirSikKullanilanlar = (LinearLayout) findViewById(R.id.satir_sik_kullanilanlar);
        kapsayiciSikKullanilanlar = (LinearLayout) findViewById(R.id.kapsayici_sik_kullanilanlar);
        kapsayiciKategoriSekmeleri = (LinearLayout) findViewById(R.id.kapsayici_kategori_sekmeleri);
        cubukAlfabe = (LinearLayout) findViewById(R.id.cubuk_alfabe);
        barHizliAramaKisayollari = findViewById(R.id.bar_hizli_arama_kisayollari);
        kartHesapMakinesi = findViewById(R.id.kart_hesap_makinesi);
        yaziHesapSonuc = (TextView) findViewById(R.id.yazi_hesap_sonuc);
        dugmeHesapKopyala = (Button) findViewById(R.id.dugme_hesap_kopyala);
        girdiArama = (EditText) findViewById(R.id.girdi_arama);

        Button dugmeAraGoogle = (Button) findViewById(R.id.dugme_ara_google);
        if (dugmeAraGoogle != null) {
            dugmeAraGoogle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String aranan = girdiArama != null ? girdiArama.getText().toString().trim() : "";
                    if (aranan.length() > 0) {
                        AramaMotoruYoneticisi.webAramasiBaslat(AnaEkranActivity.this, aranan);
                    }
                }
            });
        }

        Button dugmeAraPlay = (Button) findViewById(R.id.dugme_ara_play_store);
        if (dugmeAraPlay != null) {
            dugmeAraPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String aranan = girdiArama != null ? girdiArama.getText().toString().trim() : "";
                    if (aranan.length() > 0) {
                        try {
                            Intent niyet = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(aranan)));
                            startActivity(niyet);
                        } catch (Throwable t) {
                            try {
                                Intent niyet = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=" + Uri.encode(aranan)));
                                startActivity(niyet);
                            } catch (Throwable t2) {
                                LauncherLog.d("AnaEkran", "Web store aramasi acilamadi: " + t2.getMessage());
                            }
                        }
                    }
                }
            });
        }

        View btnSesCekmece = findViewById(R.id.dugme_cekmece_sesli_arama);
        if (btnSesCekmece != null) {
            btnSesCekmece.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sesliAramaBaslat();
                }
            });
        }

        // Çekmece Kapatma Düğmesi
        Button dugmeCekmeceKapat = (Button) findViewById(R.id.dugme_cekmece_kapat);
        if (dugmeCekmeceKapat != null) {
            dugmeCekmeceKapat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cekmeceyiKapat();
                }
            });
        }

        // Menü Arama Dinleyicisi
        if (girdiArama != null) {
            girdiArama.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String metin = s != null ? s.toString().trim() : "";
                    if (panelTumUygulamalar != null && panelTumUygulamalar.getVisibility() == View.VISIBLE) {
                        setAktifDurum(metin.length() > 0 ? LauncherDurumu.ARAMA : LauncherDurumu.CEKMECE);
                    }
                    if (barHizliAramaKisayollari != null) {
                        barHizliAramaKisayollari.setVisibility(metin.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                    filtrele(metin);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Menü Adaptörü
        menuAdaptoru = new CekmeceAdaptoru(this, filtrelenmisListe, izgaraTumUygulamalar);
        menuAdaptoru.setEtkilesimDinleyicisi(new CekmeceAdaptoru.UygulamaEtkilesimDinleyicisi() {
            @Override
            public void onUygulamaTiklandi(UygulamaBilgisi app, View view, int position) {
                uygulamaBaslat(app);
                cekmeceyiKapat();
            }

            @Override
            public boolean onUygulamaUzunTiklandi(UygulamaBilgisi app, View view, int position) {
                if (app == null || view == null) return false;
                return suruklemeBaslat(view, new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_CEKMECE));
            }
        });
        izgaraTumUygulamalar.setAdapter(menuAdaptoru);
        izgaraTumUygulamalar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < filtrelenmisListe.size()) {
                    uygulamaBaslat(filtrelenmisListe.get(position));
                    cekmeceyiKapat();
                }
            }
        });

        // A-Z Hızlı Alfabe İndeks Çubuğunu Oluştur
        alfabeCubugunuOlustur();

        // Çok Sayfalı Masaüstü Görünümü ve Sayfa Dinleyicisi
        if (masaustuSayfaliGorunum != null) {
            masaustuSayfaliGorunum.setSayfaDegisimDinleyicisi(new SayfaliMasaustuGorunumu.SayfaDegisimDinleyicisi() {
                @Override
                public void onSayfaDegisti(int yeniSayfa, int toplamSayfa) {
                    if (masaustuGorunumKontrolcusu != null) {
                        masaustuGorunumKontrolcusu.sayfaNoktalariniGuncelle(yeniSayfa, toplamSayfa);
                    }
                }
            });
        }

        surukleBirakAyarla();
    }

    /**
     * A-Z Hızlı Alfabe İndeks Çubuğunu Doldurur ve Dinleyicilerini Bağlar
     */
    private void alfabeCubugunuOlustur() {
        AlfabeIndeksYardimcisi.olustur(this, cubukAlfabe, izgaraTumUygulamalar, filtrelenmisListe);
    }

    /**
     * Sürükle ve Bırak (Drag & Drop) ve Durum Motorunu Başlatır (AOSP / Nova Mimarisi)
     */
    private void surukleBirakAyarla() {
        if (Build.VERSION.SDK_INT < 11) return;

        suruklemeKontrolcusu = new SuruklemeKontrolcusu(this);
        suruklemeKontrolcusu.bilesenleriBagla(
                masaustuSayfaliGorunum,
                findViewById(R.id.sabit_alt_cubuk),
                new SuruklemeKontrolcusu.SuruklemeDinleyicisi() {
                    @Override
                    public void onMasaustuGuncelle() {
                        masaustuSayfalariniTazele();
                    }

                    @Override
                    public void onDockGuncelle() {
                        dockYukle();
                    }

                    @Override
                    public void onCekmeceKapat() {
                        cekmeceyiKapat();
                    }

                    @Override
                    public boolean onMasaustuneBirakildi(SuruklemeVerisi veri, float screenX, float screenY) {
                        return masaustuneBirakilaniIsle(veri, screenX, screenY);
                    }
                }
        );
    }

    public void geriAlCubugunuGoster(String ogeEtiketi) {
        if (geriAlKontrolcusu != null) {
            geriAlKontrolcusu.goster(ogeEtiketi);
        }
    }

    public void geriAlCubugunuGizle() {
        if (geriAlKontrolcusu != null) {
            geriAlKontrolcusu.gizle();
        }
    }

    public boolean isMasaustuKilitli() {
        return AyarBankasi.isMasaustuKilitli(this);
    }

    /**
     * Masaüstüne bırakılan öğeyi hücre matrisine (CellLayout) göre koordinatına yerleştirir veya klasörler
     */
    private boolean masaustuneBirakilaniIsle(SuruklemeVerisi veri, float screenX, float screenY) {
        if (veri == null || veri.app == null) return false;
        if (ArayuzYardimcisi.kilitliMiVeUyar(this)) {
            return false;
        }

        final UygulamaBilgisi suruklenenApp = veri.app;
        final int aktifSayfa = getAktifSayfa();
        final CellLayout cl = masaustuGorunumKontrolcusu != null ? masaustuGorunumKontrolcusu.getSayfaCellLayout(aktifSayfa) : null;
        if (cl == null) return false;

        int[] loc = new int[2];
        cl.getLocationOnScreen(loc);
        float relX = screenX - loc[0];
        float relY = screenY - loc[1];

        int[] targetCell = cl.pixelToCell(relX, relY);
        int targetX = targetCell[0];
        int targetY = targetCell[1];

        View existingChild = cl.findChildAtCell(targetX, targetY);
        UygulamaBilgisi hedefApp = (existingChild != null && existingChild.getTag() instanceof UygulamaBilgisi)
                ? (UygulamaBilgisi) existingChild.getTag() : null;

        // --- DURUM A: Bir simgenin veya klasörün tam üstüne bırakıldı (Klasörleme) ---
        if (hedefApp != null && !suruklenenApp.paketAdi.startsWith("arac:") && !hedefApp.paketAdi.startsWith("arac:")) {
            String suruklenenKimlik = suruklenenApp.getBilesenKimligi();
            String hedefKimlik = hedefApp.getBilesenKimligi();

            // Kendisinin üzerine bırakıldıysa hiçbir şey yapma
            if (DockYoneticisi.bilesenEslesiyorMu(suruklenenKimlik, hedefKimlik)) {
                return true;
            }

            MasaustuModelDeposu.ModelDurumu model = MasaustuModelDeposu.yukle(this);
            MasaustuKaydi hedefKayit = model.masaustundeBul(aktifSayfa, hedefKimlik);
            String hedefItemId = (hedefKayit != null) ? hedefKayit.id : null;

            // A.1: Hedef zaten bir klasör ise klasörün içine atomik olarak taşı
            if (hedefApp.paketAdi.startsWith("klasor:")) {
                String kId = hedefApp.paketAdi.substring("klasor:".length());
                if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_KLASOR && kId.equals(veri.kaynakKlasorId)) {
                    return true;
                }
                boolean eklendi = MasaustuModelDeposu.klasoreOgeTasi(this, kId, veri);
                if (eklendi) {
                    if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
                        dockYukle();
                    }
                    SurukleBirakYardimcisi.titresim(this, 50);
                    KlasorSimgeUretici.onbellegiTemizle();
                    ArayuzYardimcisi.toast(this, suruklenenApp.etiket + " klasöre eklendi.");
                    tazeleEtkilenenSayfalar(veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU ? veri.kaynakSayfa : aktifSayfa, aktifSayfa);
                    return true;
                }
            }

            // A.2: Hedef normal bir uygulama ise ATOMİK OLARAK YENİ KLASÖR OLUŞTUR VE TAŞI
            if (!suruklenenApp.paketAdi.startsWith("klasor:")) {
                String klasorAdi = "Klasör";
                try {
                    String kat1 = KategoriYoneticisi.getKategori(this, hedefApp);
                    String kat2 = KategoriYoneticisi.getKategori(this, suruklenenApp);
                    if (kat1 != null && kat1.equals(kat2) && !kat1.equals(KategoriYoneticisi.KAT_TUMU)) {
                        klasorAdi = kat1;
                    }
                } catch (Throwable t) {
                    LauncherLog.d("AnaEkran", "Klasor kategori adi belirlenemedi: " + t.getMessage());
                }

                String yeniKlasorId = MasaustuModelDeposu.klasorOlusturVeBirlestir(
                        this, hedefItemId, aktifSayfa, hedefKimlik, veri, klasorAdi);
                if (yeniKlasorId != null) {
                    if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
                        dockYukle();
                    }
                    SurukleBirakYardimcisi.titresim(this, 60);
                    KlasorSimgeUretici.onbellegiTemizle();
                    ArayuzYardimcisi.toast(this, suruklenenApp.etiket + " ile yeni klasör oluşturuldu.");
                    tazeleEtkilenenSayfalar(veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU ? veri.kaynakSayfa : aktifSayfa, aktifSayfa);
                    return true;
                }
            }
        }

        // --- DURUM B: Boş bir hücreye bırakıldı (Atomik Koordinat Taşıması) ---
        SurukleBirakYardimcisi.titresim(this, 40);

        int sutunSayisi = AyarlarActivity.getMasaustuSutunSayisi(this);
        if (targetX + veri.spanX > sutunSayisi) {
            targetX = Math.max(0, sutunSayisi - veri.spanX);
        }

        MasaustuModelDeposu.masaustuneOgeTasi(this, veri, aktifSayfa, targetX, targetY);

        if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU && veri.kaynakSayfa == aktifSayfa) {
            ArayuzYardimcisi.toast(this, suruklenenApp.etiket + " konumu güncellendi.");
            tazeleEtkilenenSayfalar(aktifSayfa, aktifSayfa);
        } else if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) {
            ArayuzYardimcisi.toast(this, suruklenenApp.etiket + " Sayfa " + (aktifSayfa + 1) + "'e taşındı.");
            tazeleEtkilenenSayfalar(veri.kaynakSayfa, aktifSayfa);
        } else if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
            ArayuzYardimcisi.toast(this, suruklenenApp.etiket + " Sayfa " + (aktifSayfa + 1) + "'e taşındı.");
            dockYukle();
            tazeleEtkilenenSayfalar(aktifSayfa, aktifSayfa);
        } else if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_KLASOR) {
            ArayuzYardimcisi.toast(this, suruklenenApp.etiket + " masaüstüne çıkarıldı.");
            dockYukle();
            tazeleEtkilenenSayfalar(aktifSayfa, aktifSayfa);
        } else {
            ArayuzYardimcisi.toast(this, suruklenenApp.etiket + " Sayfa " + (aktifSayfa + 1) + "'e eklendi.");
            tazeleEtkilenenSayfalar(aktifSayfa, aktifSayfa);
        }
        return true;
    }

    private void tazeleEtkilenenSayfalar(int kaynakSayfa, int hedefSayfa) {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.ikiSayfayiArtirimliTazele(kaynakSayfa, hedefSayfa);
        }
    }

    public void suruklemeyiAktifEt() {
        suruklemeyiAktifEt((SuruklemeVerisi) null);
    }

    public void suruklemeyiAktifEt(UygulamaBilgisi app) {
        suruklemeyiAktifEt(new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_CEKMECE));
    }

    public void suruklemeyiAktifEt(SuruklemeVerisi veri) {
        // Yeni mimaride suruklemeBaslat içerisinde tek elden yönetilir
    }

    /**
     * KESİNTİSİZ EKRAN HAREKETLERİ MOTORU (Gestures Dispatcher)
     */
    private void hareketleriAyarla() {
        hareketAlgilayici = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float farkY = e2.getY() - e1.getY();
                float farkX = e2.getX() - e1.getX();

                if (Math.abs(farkY) > Math.abs(farkX) * 1.3f) {
                    if (farkY > 120 && Math.abs(velocityY) > 80) {
                        int eylem = HareketYoneticisi.eylemiAl(AnaEkranActivity.this, HareketYoneticisi.PREF_HAREKET_ASAGI, HareketYoneticisi.EYLEM_BILDIRIMLER);
                        HareketYoneticisi.eylemiCalistir(AnaEkranActivity.this, eylem);
                        return true;
                    } else if (farkY < -120 && Math.abs(velocityY) > 80) {
                        int eylem = HareketYoneticisi.eylemiAl(AnaEkranActivity.this, HareketYoneticisi.PREF_HAREKET_YUKARI, HareketYoneticisi.EYLEM_CEKMECE);
                        HareketYoneticisi.eylemiCalistir(AnaEkranActivity.this, eylem);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (suruklemeKontrolcusu != null && !suruklemeKontrolcusu.bosAlanMenusuAcilabilirMi(e.getRawX(), e.getRawY())) {
                    return;
                }
                if (System.currentTimeMillis() - sonOgeUzunBasmaZamani < 800) {
                    return;
                }
                int aktifSayfa = getAktifSayfa();
                CellLayout cl = masaustuGorunumKontrolcusu != null ? masaustuGorunumKontrolcusu.getSayfaCellLayout(aktifSayfa) : null;
                if (cl != null) {
                    int[] loc = new int[2];
                    cl.getLocationOnScreen(loc);
                    float xInCl = e.getRawX() - loc[0];
                    float yInCl = e.getRawY() - loc[1];
                    if (xInCl >= 0 && yInCl >= 0 && xInCl < cl.getWidth() && yInCl < cl.getHeight()) {
                        if (cl.findChildAtPixel(xInCl, yInCl) != null) {
                            return;
                        }
                    }
                }
                bosAlanMenusuGoster();
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int eylem = HareketYoneticisi.eylemiAl(AnaEkranActivity.this, HareketYoneticisi.PREF_HAREKET_CIFT_TIK, HareketYoneticisi.EYLEM_KILITLE);
                HareketYoneticisi.eylemiCalistir(AnaEkranActivity.this, eylem);
                return true;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (suruklemeKontrolcusu != null && suruklemeKontrolcusu.isSurukleniyor()) {
            return super.dispatchTouchEvent(ev);
        }

        if (panelTumUygulamalar == null || panelTumUygulamalar.getVisibility() != View.VISIBLE) {
            if (ev.getPointerCount() == 2) {
                int eylem = ev.getActionMasked();
                if (eylem == MotionEvent.ACTION_POINTER_DOWN) {
                    ikiParmakBaslangicY = (ev.getY(0) + ev.getY(1)) / 2;
                } else if (eylem == MotionEvent.ACTION_POINTER_UP || eylem == MotionEvent.ACTION_UP) {
                    float sonY = (ev.getY(0) + ev.getY(1)) / 2;
                    float fark = sonY - ikiParmakBaslangicY;
                    if (fark > 140) {
                        int e = HareketYoneticisi.eylemiAl(this, HareketYoneticisi.PREF_HAREKET_IKI_ASAGI, HareketYoneticisi.EYLEM_HIZLI_AYARLAR);
                        HareketYoneticisi.eylemiCalistir(this, e);
                        return true;
                    } else if (fark < -140) {
                        int e = HareketYoneticisi.eylemiAl(this, HareketYoneticisi.PREF_HAREKET_IKI_YUKARI, HareketYoneticisi.EYLEM_AYARLAR);
                        HareketYoneticisi.eylemiCalistir(this, e);
                        return true;
                    }
                }
            }

            if (hareketAlgilayici != null) {
                if (hareketAlgilayici.onTouchEvent(ev)) {
                    return true;
                }
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    public void cekmeceyiAc() {
        setAktifDurum(LauncherDurumu.CEKMECE);
        if (panelTumUygulamalar != null) {
            panelTumUygulamalar.setVisibility(View.VISIBLE);
            sikKullanilanlariGoster();
            kategoriSekmeleriniYukle();
            if (girdiArama != null) {
                girdiArama.setText("");
                girdiArama.requestFocus();
            }
        }
    }

    public void cekmeceyiKapat() {
        if (suruklemeKontrolcusu != null && !suruklemeKontrolcusu.isSurukleniyor()) {
            setAktifDurum(LauncherDurumu.NORMAL);
        }
        if (panelTumUygulamalar != null && panelTumUygulamalar.getVisibility() == View.VISIBLE) {
            panelTumUygulamalar.setVisibility(View.GONE);
            if (barHizliAramaKisayollari != null) {
                barHizliAramaKisayollari.setVisibility(View.GONE);
            }
            if (girdiArama != null) {
                girdiArama.clearFocus();
            }
        }
    }

    @Override
    public void onBackPressed() {
        geriTusuIsle();
    }

    private void geriTusuIsle() {
        if (HizliEylemlerDiyalogu.aktifPopupAcikMi()) {
            HizliEylemlerDiyalogu.kapatAktifPopup();
            return;
        }
        if (KlasorPenceresi.acikMi()) {
            KlasorPenceresi.kapat();
            return;
        }
        if (suruklemeKontrolcusu != null && suruklemeKontrolcusu.isSurukleniyor()) {
            suruklemeKontrolcusu.suruklemeBitti();
            setAktifDurum(LauncherDurumu.NORMAL);
            return;
        }
        if (panelTumUygulamalar != null && panelTumUygulamalar.getVisibility() == View.VISIBLE) {
            if (aktifDurum == LauncherDurumu.ARAMA && girdiArama != null && girdiArama.getText().length() > 0) {
                girdiArama.setText("");
                setAktifDurum(LauncherDurumu.CEKMECE);
            } else {
                cekmeceyiKapat();
            }
        } else if (masaustuSayfaliGorunum != null && masaustuSayfaliGorunum.getAktifSayfa() != masaustuSayfaliGorunum.getVarsayilanSayfa()) {
            masaustuSayfaliGorunum.sayfayaGit(masaustuSayfaliGorunum.getVarsayilanSayfa(), true);
        } else {
            int eylem = HareketYoneticisi.eylemiAl(this, HareketYoneticisi.PREF_HAREKET_GERI_TUSU, HareketYoneticisi.EYLEM_YOK);
            if (eylem != HareketYoneticisi.EYLEM_YOK) {
                HareketYoneticisi.eylemiCalistir(this, eylem);
            }
        }
    }

    private boolean aktiviteKapatildiMi() {
        return isFinishing() || (Build.VERSION.SDK_INT >= 17 && isDestroyed());
    }

    public void uygulamalariYukle() {
        zamanlayici.removeCallbacks(gecikmeliYuklemeGorevi);
        if (tumUygulamalarListesi.isEmpty()) {
            uygulamalariYukleSimdi();
        } else {
            zamanlayici.postDelayed(gecikmeliYuklemeGorevi, 80);
        }
    }

    private void uygulamalariYukleSimdi() {
        if (aktiviteKapatildiMi() || yukleyiciHavuzu.isShutdown()) return;
        final int nesil = yuklemeNesli.incrementAndGet();
        try {
            yukleyiciHavuzu.execute(new Runnable() {
                @Override
                public void run() {
                    if (nesil != yuklemeNesli.get() || aktiviteKapatildiMi()) return;
                    final List<UygulamaBilgisi> gecici = UygulamaYukleyici.yukle(AnaEkranActivity.this, true, simgeMotoru);
                    if (nesil != yuklemeNesli.get() || aktiviteKapatildiMi()) return;

                    // Masaüstü veri bütünlüğü ve çakışma denetimi (Arka planda sessizce onarır)
                    MasaustuButunlukDenetleyici.denetleVeOnar(AnaEkranActivity.this);
                    if (nesil != yuklemeNesli.get() || aktiviteKapatildiMi()) return;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (nesil != yuklemeNesli.get() || aktiviteKapatildiMi()) return;
                            tumUygulamalarListesi.clear();
                            tumUygulamalarListesi.addAll(gecici);

                            if (MasaustuYoneticisi.isTekKatmanModu(AnaEkranActivity.this)) {
                                MasaustuYoneticisi.tekKatmanSenkronizeEt(AnaEkranActivity.this, tumUygulamalarListesi);
                            }

                            filtrele(girdiArama != null ? girdiArama.getText().toString() : "");
                            masaustuSayfalariniTazele();
                            dockYukle();
                            kategoriSekmeleriniYukle();
                        }
                    });
                }
            });
        } catch (Exception e) {
            LauncherLog.d("AnaEkranActivity", "Yukleme gorevi planlanamadi: " + e.getMessage());
        }
    }

    void masaustuSayfalariniTazele() {
        if (masaustuGorunumKontrolcusu != null) {
            masaustuGorunumKontrolcusu.masaustuSayfalariniTazele();
        }
    }

    /**
     * Menü Sekmelerini (Categories) oluşturur ve filtreler
     */
    private void kategoriSekmeleriniYukle() {
        AramaVeCekmeceKontrolcusu.kategoriSekmeleriniYukle(this, kapsayiciKategoriSekmeleri, aktifKategori, new AramaVeCekmeceKontrolcusu.KategoriDegisimDinleyici() {
            @Override
            public void onKategoriSecildi(String yeniKategori) {
                aktifKategori = yeniKategori;
                kategoriSekmeleriniYukle();
                filtrele(girdiArama != null ? girdiArama.getText().toString() : "");
            }

            @Override
            public void onSekmeleriYonetTiklandi() {
                SekmeYonetimDiyalogu.goster(AnaEkranActivity.this, tumUygulamalarListesi, new SekmeYonetimDiyalogu.SekmeGuncellemeDinleyicisi() {
                    @Override
                    public void onSekmelerGuncellendi() {
                        kategoriSekmeleriniYukle();
                        filtrele(girdiArama != null ? girdiArama.getText().toString() : "");
                    }
                });
            }
        });
    }

    public boolean suruklemeBaslat(View view, SuruklemeVerisi veri) {
        if (ArayuzYardimcisi.kilitliMiVeUyar(this)) {
            return true;
        }
        sonOgeUzunBasmaZamani = System.currentTimeMillis();
        if (suruklemeKontrolcusu != null) {
            return suruklemeKontrolcusu.suruklemeBaslat(view, veri);
        }
        return false;
    }

    public void dockYukle() {
        HorizontalScrollView dockKaydirici = (HorizontalScrollView) findViewById(R.id.dock_kaydirici);
        DockArayuzKontrolcusu.dockYukle(this, dockYuvalari, dockKaydirici, tumUygulamalarListesi, new DockArayuzKontrolcusu.DockEylemDinleyici() {
            @Override
            public void onUygulamaTiklandi(UygulamaBilgisi app) {
                if (app != null && app.paketAdi != null && app.paketAdi.startsWith("klasor:")) {
                    klasoruAc(app.paketAdi);
                    return;
                }
                uygulamaBaslat(app);
            }

            @Override
            public boolean onUygulamaUzunBasildi(View view, SuruklemeVerisi veri) {
                return suruklemeBaslat(view, veri);
            }
        });
    }

    public void canliDockStudiouGuncelle(int yukseklikDp, int simgeOlcek, int yuvaSayisi, boolean kaydirilabilir) {
        View dockBar = findViewById(R.id.sabit_alt_cubuk);
        if (dockBar != null) {
            ViewGroup.LayoutParams lp = dockBar.getLayoutParams();
            int pxH = ArayuzYardimcisi.dpToPx(this, yukseklikDp);
            if (lp != null && lp.height != pxH) {
                lp.height = pxH;
                dockBar.setLayoutParams(lp);
            }
        }
        HorizontalScrollView dockKaydirici = (HorizontalScrollView) findViewById(R.id.dock_kaydirici);
        DockArayuzKontrolcusu.dockYukleGecici(this, dockYuvalari, dockKaydirici, tumUygulamalarListesi,
                yukseklikDp, simgeOlcek, yuvaSayisi, kaydirilabilir,
                new DockArayuzKontrolcusu.DockEylemDinleyici() {
                    @Override
                    public void onUygulamaTiklandi(UygulamaBilgisi app) {
                        if (app != null && app.paketAdi != null && app.paketAdi.startsWith("klasor:")) {
                            klasoruAc(app.paketAdi);
                            return;
                        }
                        uygulamaBaslat(app);
                    }

                    @Override
                    public boolean onUygulamaUzunBasildi(View view, SuruklemeVerisi veri) {
                        return suruklemeBaslat(view, veri);
                    }
                });
    }

    void sikKullanilanlariGoster() {
        SharedPreferences ayarlar = AyarBankasi.getAyarlar(this);
        boolean acik = ayarlar.getBoolean(AyarlarActivity.PREF_SIK_KULLANILANLAR_MODU, true);

        if (!acik || satirSikKullanilanlar == null || kapsayiciSikKullanilanlar == null) {
            if (satirSikKullanilanlar != null) satirSikKullanilanlar.setVisibility(View.GONE);
            return;
        }

        List<UygulamaBilgisi> siklar = KullanimSayaci.sikKullanilanlariSec(this, tumUygulamalarListesi, 5);
        if (siklar.isEmpty()) {
            satirSikKullanilanlar.setVisibility(View.GONE);
            return;
        }

        satirSikKullanilanlar.setVisibility(View.VISIBLE);
        kapsayiciSikKullanilanlar.removeAllViews();

        int cekmeceOlcek = AyarBankasi.getCekmeceSimgeBoyutu(this);
        int simgePx = ArayuzYardimcisi.dpToPx(this, 56 * (cekmeceOlcek / 100.0f));

        for (final UygulamaBilgisi app : siklar) {
            ImageView simge = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(simgePx, simgePx);
            lp.setMargins(6, 0, 6, 0);
            simge.setLayoutParams(lp);
            simge.setImageDrawable(SimgeNormalizasyonYoneticisi.normalizeEt(this, app.simge));
            simge.setScaleType(ImageView.ScaleType.FIT_CENTER);
            simge.setClickable(true);
            simge.setFocusable(true);
            simge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArayuzYardimcisi.guvenliUygulamaBaslat(AnaEkranActivity.this, app, new Runnable() {
                        @Override
                        public void run() {
                            cekmeceyiKapat();
                        }
                    });
                }
            });
            simge.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return suruklemeBaslat(v, new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_CEKMECE));
                }
            });
            kapsayiciSikKullanilanlar.addView(simge);
        }
    }

    private void filtrele(String metin) {
        AramaVeCekmeceKontrolcusu.aramaVeFiltrelemeYap(
            this, metin, tumUygulamalarListesi, aktifKategori,
            filtrelenmisListe, menuAdaptoru,
            kartHesapMakinesi, yaziHesapSonuc, dugmeHesapKopyala
        );
    }

    void klasoruAc(String klasorPaketAdi) {
        klasoruAc(klasorPaketAdi, false);
    }

    void klasoruAc(String klasorPaketAdi, boolean zorlaPencereAc) {
        if (klasorPaketAdi == null || !klasorPaketAdi.startsWith("klasor:")) return;
        String kId = klasorPaketAdi.substring("klasor:".length());
        List<KlasorYoneticisi.KlasorBilgisi> list = KlasorYoneticisi.klasorleriYukle(this);
        for (final KlasorYoneticisi.KlasorBilgisi kb : list) {
            if (kb.id.equals(kId)) {
                if (!zorlaPencereAc && KlasorKapakYoneticisi.isKlasorKapakModu(this, kId)
                        && kb.paketler != null && !kb.paketler.isEmpty()) {
                    String ilkPaket = kb.paketler.get(0);
                    UygulamaBilgisi ilkApp = UygulamaBilgisi.bilesenBul(tumUygulamalarListesi, ilkPaket);
                    if (ilkApp != null) {
                        uygulamaBaslat(ilkApp);
                        return;
                    }
                }
                setAktifDurum(LauncherDurumu.KLASOR);
                KlasorPenceresi.ac(this, kb, tumUygulamalarListesi, new KlasorPenceresi.KlasorGuncellemeDinleyicisi() {
                    @Override
                    public void onKlasorGuncellendi() {
                        masaustuSayfalariniTazele();
                        dockYukle();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        if (panelTumUygulamalar != null && panelTumUygulamalar.getVisibility() == View.VISIBLE) {
                            String m = girdiArama != null ? girdiArama.getText().toString().trim() : "";
                            setAktifDurum(m.length() > 0 ? LauncherDurumu.ARAMA : LauncherDurumu.CEKMECE);
                        } else {
                            setAktifDurum(LauncherDurumu.NORMAL);
                        }
                    }
                });
                return;
            }
        }
    }

    private void saatiVeyaAlarmiAc() {
        SistemAraclariYardimcisi.saatUygulamasiniAc(this);
    }

    private void takvimiAc() {
        SistemAraclariYardimcisi.takvimUygulamasiniAc(this);
    }

    void uygulamaBaslat(final UygulamaBilgisi app) {
        if (app == null) return;
        if (app.paketAdi != null && app.paketAdi.startsWith("klasor:")) {
            klasoruAc(app.paketAdi);
            return;
        }
        if (app.paketAdi != null && app.paketAdi.startsWith("arac:")) {
            HizliAraclarYoneticisi.araciCalistir(this, app.paketAdi, new Runnable() {
                @Override
                public void run() {
                    masaustuSayfalariniTazele();
                    dockYukle();
                }
            });
            return;
        }
        ArayuzYardimcisi.guvenliUygulamaBaslat(this, app, null);
    }

    public void sesliAramaBaslat() {
        SistemAraclariYardimcisi.sesliAramaBaslat(this);
    }

    private void bosAlanMenusuGoster() {
        MasaustuMenuYardimcisi.menuGoster(this);
    }

    void klasorOlusturDiyalogu() {
        AyarDiyalogYardimcisi.metinGirdiDiyalogu(this, getString(R.string.klasor_olustur), null, "Klasör Adı (Örn: Sosyal)", new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
            @Override
            public void onMetinGirildi(String ad) {
                if (ad.length() > 0) {
                    int aktif = getAktifSayfa();
                    MasaustuModelDeposu.bosKlasorOlustur(AnaEkranActivity.this, aktif, ad);
                    masaustuSayfalariniTazele();
                    ArayuzYardimcisi.toast(AnaEkranActivity.this, ad + " klasörü oluşturuldu.");
                }
            }
        });
    }

    public WidgetYoneticisi getWidgetYoneticisi() {
        return widgetYoneticisi;
    }
}

