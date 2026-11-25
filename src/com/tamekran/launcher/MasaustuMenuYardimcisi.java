package com.tamekran.launcher;

import android.content.Intent;

/**
 * Tam Ekran - Masaüstü Boş Alan Menüsü Yardımcısı (Desktop Context Menu Helper)
 * Duvar kağıdı, widget ekleme, hızlı araçlar, kısayollar, klasör oluşturma,
 * ızgara stüdyosu ve ayarlar menüsünü AnaEkranActivity dışına modüler olarak taşır (SRP).
 */
public final class MasaustuMenuYardimcisi {

    private MasaustuMenuYardimcisi() {}

    public static void menuGoster(final AnaEkranActivity activity) {
        if (activity == null) return;

        CharSequence[] secenekler = new CharSequence[]{
            activity.getString(R.string.duvar_kagidi_sec),
            "🧩 Ekran Aracı Ekle (Sistem/Uygulama)",
            "⚡ Tam Ekran Araçları (Widget & Kontroller)",
            "🔗 Sistem Kısayolu Ekle",
            activity.getString(R.string.klasor_olustur),
            "📐 Canlı Düzen Stüdyosu",
            "📐 Güvenli Alan / Kenar Kırpma",
            "📊 Kullanım Raporu",
            "🔒 Ekranı Kilitle",
            activity.getString(R.string.ayar_baslik)
        };

        AyarDiyalogYardimcisi.listeSecimDiyalogu(activity, activity.getString(R.string.uygulama_adi), secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
            @Override
            public void onSecildi(int which) {
                if (which >= 1 && which <= 6 && ArayuzYardimcisi.kilitliMiVeUyar(activity)) {
                    return;
                }
                switch (which) {
                    case 0:
                        SistemAraclariYardimcisi.duvarKagidiSeciciAc(activity);
                        break;
                    case 1:
                        if (activity.getWidgetYoneticisi() != null) {
                            activity.getWidgetYoneticisi().widgetSeciminiBaslat(activity, activity.getAktifSayfa(), new WidgetYoneticisi.WidgetIslemDinleyici() {
                                @Override
                                public void onWidgetlerGuncellendi() {
                                    activity.masaustuSayfalariniTazele();
                                }
                            });
                        }
                        break;
                    case 2:
                        HizliAraclarYoneticisi.aracSecimDiyaloguGoster(activity, activity.getAktifSayfa(), new Runnable() {
                            @Override
                            public void run() {
                                activity.masaustuSayfalariniTazele();
                            }
                        });
                        break;
                    case 3:
                        KisayolYoneticisi.kisayolEkleIstegiBaslat(activity);
                        break;
                    case 4:
                        activity.klasorOlusturDiyalogu();
                        break;
                    case 5:
                        IzgaraKalibratoru.goster(activity, IzgaraKalibratoru.MOD_MASAUSTU);
                        break;
                    case 6:
                        GuvenliAlanKalibratoru.goster(activity);
                        break;
                    case 7:
                        KullanimRaporuDiyalogu.goster(activity, activity.getTumUygulamalarListesi(), new Runnable() {
                            @Override
                            public void run() {
                                activity.sikKullanilanlariGoster();
                            }
                        });
                        break;
                    case 8:
                        EkranKilitleyici.kilitle(activity);
                        break;
                    case 9:
                        activity.startActivity(new Intent(activity, AyarlarActivity.class));
                        break;
                }
            }
        });
    }
}
