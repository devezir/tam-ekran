package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tam Ekran - Başlatıcı Ayarları (Nova Seviyesi Özelleştirme)
 * Saf Java, %100 Türkçe, sıfır kütüphane bağımlılığı.
 */
public class AyarlarActivity extends Activity {

    public static final String AYARLAR_PREF = AyarBankasi.AYARLAR_PREF;
    public static final String ANAHTAR_IZGARA_SUTUN = AyarBankasi.ANAHTAR_IZGARA_SUTUN;
    public static final String ANAHTAR_IZGARA_SATIR = AyarBankasi.ANAHTAR_IZGARA_SATIR;
    public static final String ANAHTAR_CEKMECE_SUTUN = AyarBankasi.ANAHTAR_CEKMECE_SUTUN;
    public static final String ANAHTAR_CEKMECE_SATIR = AyarBankasi.ANAHTAR_CEKMECE_SATIR;
    public static final String PREF_TAM_EKRAN_MODU = AyarBankasi.PREF_TAM_EKRAN_MODU;
    public static final String PREF_SIK_KULLANILANLAR_MODU = AyarBankasi.PREF_SIK_KULLANILANLAR_MODU;

    private SharedPreferences ayarlar;
    private TextView yaziDurum;
    private TextView yaziAktifPaket;
    private TextView yaziGizliSayisi;

    private Button dugmeMasaustuModu;
    private Button dugmeVarsayilanBaslatici;
    private static final int REQUEST_DOSYADAN_YEDEK = 4001;

    private SimgePaketiYoneticisi simgeMotoru;

    private interface SecimAksiyonu {
        void secildi(int which);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayarlar);

        ayarlar = AyarBankasi.getAyarlar(this);
        simgeMotoru = new SimgePaketiYoneticisi(this);

        bilesenleriAyarla();
    }

    @Override
    protected void onResume() {
        super.onResume();
        durumlariGuncelle();
    }

    private void bilesenleriAyarla() {
        akordeonlariVeAramayiAyarla();

        Button dugmeGeri = (Button) findViewById(R.id.dugme_geri);
        if (dugmeGeri != null) {
            dugmeGeri.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // --- HIZLI İŞLEM: Varsayılan Başlatıcı ---
        dugmeVarsayilanBaslatici = (Button) findViewById(R.id.dugme_varsayilan_baslatici);
        if (dugmeVarsayilanBaslatici != null) {
            dugmeVarsayilanBaslatici.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SistemAraclariYardimcisi.varsayilanBaslaticiAyarla(AyarlarActivity.this);
                }
            });
        }

        // --- KATEGORİ 1: Masaüstü ---
        dugmeMasaustuModu = (Button) findViewById(R.id.dugme_masaustu_modu);
        if (dugmeMasaustuModu != null) {
            dugmeMasaustuModu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{
                        "Klasik Mod (Masaüstü + Uygulama Çekmecesi)",
                        "Tek Katmanlı Mod (Tüm Uygulamalar Masaüstünde - iOS Modu)"
                    };
                    int aktif = MasaustuYoneticisi.getMasaustuModu(AyarlarActivity.this);
                    AyarDiyalogYardimcisi.tekliSecimDiyalogu(AyarlarActivity.this, "Ana Ekran Düzeni", secenekler, aktif, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            if (which != MasaustuYoneticisi.getMasaustuModu(AyarlarActivity.this)) {
                                MasaustuYoneticisi.setMasaustuModu(AyarlarActivity.this, which);
                                AyarBankasi.degisiklikDamgasiGuncelle(AyarlarActivity.this);
                                durumlariGuncelle();
                                String msg = (which == MasaustuYoneticisi.MOD_TEK_KATMAN_SERP)
                                    ? "Tek Katmanlı Mod seçildi. Tüm uygulamalar masaüstüne serpilecek."
                                    : "Klasik Mod seçildi. Özel masaüstünüz ve uygulama çekmeceniz geri yüklendi.";
                                ArayuzYardimcisi.toastUzun(AyarlarActivity.this, msg);
                            }
                        }
                    });
                }
            });
        }

        Button dugmeSayfaSayisi = (Button) findViewById(R.id.dugme_sayfa_sayisi);
        if (dugmeSayfaSayisi != null) {
            dugmeSayfaSayisi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{"1 Sayfa", "2 Sayfa", "3 Sayfa", "4 Sayfa", "5 Sayfa", "7 Sayfa"};
                    final int[] degerler = new int[]{1, 2, 3, 4, 5, 7};
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Masaüstü Sayfa Sayısı", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            MasaustuYoneticisi.setSayfaSayisi(AyarlarActivity.this, degerler[which]);
                            durumlariGuncelle();
                            ArayuzYardimcisi.toast(AyarlarActivity.this, "Masaüstü sayfa sayısı güncellendi: " + degerler[which]);
                        }
                    });
                }
            });
        }

        baglaToggle(R.id.dugme_sonsuz_kaydirma, MasaustuYoneticisi.PREF_SONSUZ_KAYDIRMA, false);
        baglaSecim(R.id.dugme_arama_motoru, "Arama Sağlayıcısı", AramaMotoruYoneticisi.MOTOR_ISIMLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                AramaMotoruYoneticisi.setAktifMotor(AyarlarActivity.this, which);
            }
        });
        baglaToggle(R.id.dugme_masaustu_kilit, YedeklemeYoneticisi.PREF_MASAUSTU_KILITLI, false);
        baglaToggle(R.id.dugme_dock_gorunurluk, YedeklemeYoneticisi.PREF_DOCK_GORUNUR, true);

        Button dugmeDockSayisi = (Button) findViewById(R.id.dugme_dock_sayisi);
        if (dugmeDockSayisi != null) {
            dugmeDockSayisi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{
                        "⚡ Canlı Alt Bar Stüdyosu (Yükseklik, Simge, Yuva Canlı Gör)",
                        "3 Simge", "4 Simge (Varsayılan)", "5 Simge", "6 Simge", "7 Simge"
                    };
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Alt Çubuk Simge Sayısı", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            if (which == 0) {
                                canliKalibratorAc(AnaEkranActivity.EXTRA_AC_DOCK_KALIBRATORU, -1);
                            } else {
                                int[] degerler = new int[]{0, 3, 4, 5, 6, 7};
                                AyarBankasi.editorKaydet(ayarlar.edit().putInt(YedeklemeYoneticisi.PREF_DOCK_SIMGE_SAYISI, degerler[which]));
                                durumlariGuncelle();
                            }
                        }
                    });
                }
            });
        }

        Button dugmeDockYuksekligi = (Button) findViewById(R.id.dugme_dock_yuksekligi);
        if (dugmeDockYuksekligi != null) {
            dugmeDockYuksekligi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{
                        "⚓ Canlı Alt Bar Stüdyosu (Yükseklik, Simge, Yuva ve Kaydırma)",
                        "56 dp (Kompakt)", "64 dp (Sade)", "72 dp (Standart)", "80 dp (Ferah)", "88 dp (Geniş)"
                    };
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Alt Çubuk Ayarları", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            if (which == 0) {
                                canliKalibratorAc(AnaEkranActivity.EXTRA_AC_DOCK_KALIBRATORU, -1);
                            } else {
                                int[] degerler = new int[]{0, 56, 64, 72, 80, 88};
                                AyarBankasi.setDockYuksekligi(AyarlarActivity.this, degerler[which]);
                                durumlariGuncelle();
                            }
                        }
                    });
                }
            });
        }

        baglaSecim(R.id.dugme_gecis_efekti, "Sayfa Geçiş Efekti", SayfaliMasaustuGorunumu.EFEKT_ISIMLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                AyarBankasi.editorKaydet(ayarlar.edit().putInt(YedeklemeYoneticisi.PREF_GECIS_EFEKTI, which));
            }
        });

        baglaSecim(R.id.dugme_klasor_sekli, "Klasör Simge Şekli", KlasorSimgeUretici.SEKIL_ISIMLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                KlasorSimgeUretici.setKlasorSekli(AyarlarActivity.this, which);
            }
        });

        Button dugmeMasaustuDolgu = (Button) findViewById(R.id.dugme_masaustu_dolgu);
        if (dugmeMasaustuDolgu != null) {
            dugmeMasaustuDolgu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{
                        "📐 Canlı Kalibratörü Başlat (Masaüstünde Gör)",
                        "Küçük (6dp)",
                        "Standart (14dp)",
                        "Geniş (22dp)",
                        "Sıfırla (0dp - Kenarsız)"
                    };
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Güvenli Alan & Kenar Kırpma", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            if (which == 0) {
                                canliKalibratorAc(AnaEkranActivity.EXTRA_AC_GUVENLI_ALAN, -1);
                            } else {
                                int[] degerler = new int[]{0, 6, 14, 22, 0};
                                int val = degerler[which];
                                GuvenliAlanYoneticisi.kaydet(AyarlarActivity.this, val, val, val, val);
                                durumlariGuncelle();
                                ArayuzYardimcisi.toast(AyarlarActivity.this, "Kenar boşlukları " + val + "dp yapıldı.");
                            }
                        }
                    });
                }
            });
        }

        baglaSecim(R.id.dugme_dock_stili, "Alt Çubuk (Dock) Stili", DockArkaPlanYoneticisi.STIL_ISIMLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                DockArkaPlanYoneticisi.setStil(AyarlarActivity.this, which);
            }
        });

        baglaToggle(R.id.dugme_otomatik_masaustune_ekle, PaketDegisimAlicisi.PREF_OTOMATIK_MASAUSTUNE_EKLE, true);
        baglaToggle(R.id.dugme_duvar_kagidi_kaydirma, YedeklemeYoneticisi.PREF_DUVAR_KAGIDI_KAYDIRMA, true);
        baglaToggle(R.id.dugme_klasor_kapak_modu, KlasorKapakYoneticisi.PREF_KLASOR_KAPAK_MODU_GENEL, false);

        // --- KATEGORİ 2: Simgeler ve Bildirim Rozetleri ---
        Button dugmeBildirimRozetleri = (Button) findViewById(R.id.dugme_bildirim_rozetleri);
        if (dugmeBildirimRozetleri != null) {
            dugmeBildirimRozetleri.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{"Kapalı", "Sayı Rozeti (Örn: 3)", "Nokta Rozeti (Minimal)"};
                    final int[] degerler = new int[]{
                        BildirimYoneticisi.ROZET_KAPALI,
                        BildirimYoneticisi.ROZET_SAYI,
                        BildirimYoneticisi.ROZET_NOKTA
                    };
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Bildirim Rozeti Türü", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            int secilen = degerler[which];
                            BildirimYoneticisi.setRozetTuru(AyarlarActivity.this, secilen);
                            durumlariGuncelle();

                            if (secilen != BildirimYoneticisi.ROZET_KAPALI && !BildirimYoneticisi.izinVerildiMi(AyarlarActivity.this)) {
                                AyarDiyalogYardimcisi.onayDiyalogu(AyarlarActivity.this, "Bildirim Erişimi Gerekli",
                                    "Simgelerin üzerinde bildirim sayılarını gösterebilmek için bildirim erişim izni gereklidir.\n\nŞimdi etkinleştirmek ister misiniz?",
                                    "İzin Ver", new Runnable() {
                                        @Override
                                        public void run() {
                                            BildirimYoneticisi.izinEkraniAc(AyarlarActivity.this);
                                        }
                                    });
                            }
                        }
                    });
                }
            });
        }

        Button dugmeSimgeBoyutu = (Button) findViewById(R.id.dugme_simge_boyutu);
        if (dugmeSimgeBoyutu != null) {
            dugmeSimgeBoyutu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{
                        "✨ Canlı Kalibratörü Başlat (%50 - %150 Canlı Gör)",
                        "Küçük (%80)", "Standart (%100)", "Büyük (%115)", "Ekstra Büyük (%130)"
                    };
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Simge Boyutu", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            if (which == 0) {
                                canliKalibratorAc(AnaEkranActivity.EXTRA_AC_IZGARA_KALIBRATORU, IzgaraKalibratoru.MOD_MASAUSTU);
                            } else {
                                int[] degerler = new int[]{0, 80, 100, 115, 130};
                                AyarBankasi.setSimgeBoyutu(AyarlarActivity.this, degerler[which]);
                                AyarBankasi.setMasaustuSimgeBoyutu(AyarlarActivity.this, degerler[which]);
                                AyarBankasi.setCekmeceSimgeBoyutu(AyarlarActivity.this, degerler[which]);
                                AyarBankasi.degisiklikDamgasiGuncelle(AyarlarActivity.this);
                                durumlariGuncelle();
                                ArayuzYardimcisi.toast(AyarlarActivity.this, "Simge boyutu: %" + degerler[which]);
                            }
                        }
                    });
                }
            });
        }

        Button dugmeTemaRengi = (Button) findViewById(R.id.dugme_tema_rengi);
        if (dugmeTemaRengi != null) {
            dugmeTemaRengi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Arayüz Vurgu Rengi", TemaYoneticisi.RENK_ISIMLERI, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            TemaYoneticisi.setVurguRengi(AyarlarActivity.this, TemaYoneticisi.RENK_DEGERLERI[which]);
                            durumlariGuncelle();
                            ArayuzYardimcisi.toast(AyarlarActivity.this, "Tema rengi uygulandı: " + TemaYoneticisi.RENK_ISIMLERI[which]);
                        }
                    });
                }
            });
        }

        yaziAktifPaket = (TextView) findViewById(R.id.yazi_aktif_simge_paketi);
        Button dugmeSimgePaketi = (Button) findViewById(R.id.dugme_simge_paketi);
        if (dugmeSimgePaketi != null) {
            dugmeSimgePaketi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    simgePaketiSecimDiyalogu();
                }
            });
        }

        yaziGizliSayisi = (TextView) findViewById(R.id.yazi_gizli_sayisi);
        Button dugmeUygulamaGizle = (Button) findViewById(R.id.dugme_uygulamalari_gizle);
        if (dugmeUygulamaGizle != null) {
            dugmeUygulamaGizle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uygulamaGizlemeDiyalogu();
                }
            });
        }

        baglaToggle(R.id.dugme_simge_etiketleri_degistir, YedeklemeYoneticisi.PREF_ETIKETLERI_GOSTER, true);
        baglaToggle(R.id.dugme_tam_ekran_modu, PREF_TAM_EKRAN_MODU, false);
        baglaToggle(R.id.dugme_sik_kullanilanlar_modu, PREF_SIK_KULLANILANLAR_MODU, true);
        baglaToggle(R.id.dugme_cekmece_sekmeleri_goster, AyarBankasi.PREF_CEKMECE_SEKMELERI_GOSTER, true);

        baglaSecim(R.id.dugme_cekmece_siralamasi, "Menü Sıralaması", CekmeceSiralamasi.SIRALAMA_ISIMLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                CekmeceSiralamasi.setSiralamaTuru(AyarlarActivity.this, which);
            }
        });

        Button dugmeCekmeceSaydamlik = (Button) findViewById(R.id.dugme_cekmece_saydamlik);
        if (dugmeCekmeceSaydamlik != null) {
            dugmeCekmeceSaydamlik.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String[] secenekler = new String[]{
                        "🎨 Canlı Şeffaflık Stüdyosu (Çekmecede Canlı Gör)",
                        "%100 (Tam Opak - Koyu)", "%85 (Hafif Şeffaf)", "%65 (Buzlu Cam)", "%40 (Aero Cam)", "%10 (Saydam Cam)"
                    };
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Menü Arka Plan Şeffaflığı", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            if (which == 0) {
                                canliKalibratorAc(AnaEkranActivity.EXTRA_AC_IZGARA_KALIBRATORU, IzgaraKalibratoru.MOD_CEKMECE);
                            } else {
                                int[] degerler = new int[]{0, 100, 85, 65, 40, 10};
                                AyarBankasi.putInt(AyarlarActivity.this, AyarBankasi.PREF_CEKMECE_SAYDAMLIK, degerler[which]);
                                AyarBankasi.degisiklikDamgasiGuncelle(AyarlarActivity.this);
                                durumlariGuncelle();
                                ArayuzYardimcisi.toast(AyarlarActivity.this, "Menü şeffaflığı güncellendi.");
                            }
                        }
                    });
                }
            });
        }

        baglaSecim(R.id.dugme_yazi_stili, "Simge Yazı Stili", YaziStiliYoneticisi.STIL_ISIMLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                YaziStiliYoneticisi.setYaziStili(AyarlarActivity.this, which);
            }
        });

        Button dugmeSimgeYaziBoyutu = (Button) findViewById(R.id.dugme_simge_yazi_boyutu);
        if (dugmeSimgeYaziBoyutu != null) {
            dugmeSimgeYaziBoyutu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Simge Yazı Boyutu", YaziStiliYoneticisi.BOYUT_ISIMLERI, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            YaziStiliYoneticisi.setYaziBoyutu(AyarlarActivity.this, YaziStiliYoneticisi.BOYUT_DEGERLERI[which]);
                            durumlariGuncelle();
                            ArayuzYardimcisi.toast(AyarlarActivity.this, "Yazı boyutu: " + YaziStiliYoneticisi.BOYUT_ISIMLERI[which]);
                        }
                    });
                }
            });
        }

        baglaToggle(R.id.dugme_metin_golgesi, YaziStiliYoneticisi.PREF_METIN_GOLGESI, true);
        baglaToggle(R.id.dugme_simge_normalizasyonu, SimgeNormalizasyonYoneticisi.PREF_SIMGE_NORMALIZASYONU, false);

        baglaSecim(R.id.dugme_simge_sekli, "Simge Adaptif Şekli", SimgeNormalizasyonYoneticisi.SEKIL_ISIMLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                SimgeNormalizasyonYoneticisi.setSimgeSekli(AyarlarActivity.this, which);
            }
        });

        Button dugmeSekmeleriYonet = (Button) findViewById(R.id.dugme_sekmeleri_yonet);
        if (dugmeSekmeleriYonet != null) {
            dugmeSekmeleriYonet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<UygulamaBilgisi> apps = UygulamaYukleyici.tumunuYukle(AyarlarActivity.this);
                    SekmeYonetimDiyalogu.goster(AyarlarActivity.this, apps, new SekmeYonetimDiyalogu.SekmeGuncellemeDinleyicisi() {
                        @Override
                        public void onSekmelerGuncellendi() {
                            durumlariGuncelle();
                        }
                    });
                }
            });
        }

        // --- KATEGORİ 3: Ekran Hareketleri & Kilit İzni ---
        Button dugmeKilitYetkisi = (Button) findViewById(R.id.dugme_kilit_yetkisi);
        if (dugmeKilitYetkisi != null) {
            dugmeKilitYetkisi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (EkranKilitleyici.kilitYetkisiVarMi(AyarlarActivity.this)) {
                        EkranKilitleyici.kilitle(AyarlarActivity.this);
                    } else {
                        EkranKilitleyici.izinDiyaloguGoster(AyarlarActivity.this);
                    }
                }
            });
        }

        hareketButonuAyarla(R.id.dugme_hareket_asagi, HareketYoneticisi.PREF_HAREKET_ASAGI, HareketYoneticisi.EYLEM_BILDIRIMLER);
        hareketButonuAyarla(R.id.dugme_hareket_yukari, HareketYoneticisi.PREF_HAREKET_YUKARI, HareketYoneticisi.EYLEM_CEKMECE);
        hareketButonuAyarla(R.id.dugme_hareket_cift_tik, HareketYoneticisi.PREF_HAREKET_CIFT_TIK, HareketYoneticisi.EYLEM_KILITLE);
        hareketButonuAyarla(R.id.dugme_hareket_iki_asagi, HareketYoneticisi.PREF_HAREKET_IKI_ASAGI, HareketYoneticisi.EYLEM_HIZLI_AYARLAR);
        hareketButonuAyarla(R.id.dugme_hareket_iki_yukari, HareketYoneticisi.PREF_HAREKET_IKI_YUKARI, HareketYoneticisi.EYLEM_AYARLAR);
        hareketButonuAyarla(R.id.dugme_hareket_ana_ekran, HareketYoneticisi.PREF_HAREKET_ANA_EKRAN, HareketYoneticisi.EYLEM_YOK);
        hareketButonuAyarla(R.id.dugme_hareket_geri_tusu, HareketYoneticisi.PREF_HAREKET_GERI_TUSU, HareketYoneticisi.EYLEM_YOK);

        Button dugmeUzunBasmaOzellestir = (Button) findViewById(R.id.dugme_uzun_basma_ozellestir);
        if (dugmeUzunBasmaOzellestir != null) {
            dugmeUzunBasmaOzellestir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uzunBasmaOzellestirDiyalogu();
                }
            });
        }

        Button dugmeGuvenliAlan = (Button) findViewById(R.id.dugme_guvenli_alan);
        if (dugmeGuvenliAlan != null) {
            dugmeGuvenliAlan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    canliKalibratorAc(AnaEkranActivity.EXTRA_AC_GUVENLI_ALAN, -1);
                }
            });
        }

        Button dugmeVarsayilanSayfa = (Button) findViewById(R.id.dugme_varsayilan_sayfa);
        if (dugmeVarsayilanSayfa != null) {
            dugmeVarsayilanSayfa.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int toplam = Math.max(1, MasaustuYoneticisi.getSayfaSayisi(AyarlarActivity.this));
                    String[] secenekler = new String[toplam];
                    for (int i = 0; i < toplam; i++) {
                        secenekler[i] = (i + 1) + ". Sayfa";
                    }
                    int aktif = Math.max(0, Math.min(toplam - 1, MasaustuYoneticisi.getVarsayilanSayfa(AyarlarActivity.this)));
                    AyarDiyalogYardimcisi.tekliSecimDiyalogu(AyarlarActivity.this, "Varsayılan Ana Sayfa", secenekler, aktif, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            MasaustuYoneticisi.setVarsayilanSayfa(AyarlarActivity.this, which);
                            durumlariGuncelle();
                            ArayuzYardimcisi.toast(AyarlarActivity.this, "Varsayılan ana sayfa " + (which + 1) + ". sayfa olarak ayarlandı.");
                        }
                    });
                }
            });
        }

        baglaSecim(R.id.dugme_hareket_sallama, "Cihazı Sallama Hareketi (Shake)", SallamaAlgilayici.SALLAMA_EYLEM_METINLERI, new SecimAksiyonu() {
            @Override
            public void secildi(int which) {
                AyarBankasi.editorKaydet(ayarlar.edit().putInt(SallamaAlgilayici.PREF_SALLAMA_EYLEMI, which));
            }
        });

        // --- KATEGORİ 4: Izgara ---
        Button dugmeMasaustuIzgaraCanli = (Button) findViewById(R.id.dugme_masaustu_izgara_canli);
        if (dugmeMasaustuIzgaraCanli != null) {
            dugmeMasaustuIzgaraCanli.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    canliKalibratorAc(AnaEkranActivity.EXTRA_AC_IZGARA_KALIBRATORU, IzgaraKalibratoru.MOD_MASAUSTU);
                }
            });
        }

        Button dugmeCekmeceIzgaraCanli = (Button) findViewById(R.id.dugme_cekmece_izgara_canli);
        if (dugmeCekmeceIzgaraCanli != null) {
            dugmeCekmeceIzgaraCanli.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    canliKalibratorAc(AnaEkranActivity.EXTRA_AC_IZGARA_KALIBRATORU, IzgaraKalibratoru.MOD_CEKMECE);
                }
            });
        }

        // --- KATEGORİ: Güvenlik, Gizlilik ve Dijital Denge ---
        Button dugmeUygulamaKilidi = (Button) findViewById(R.id.dugme_uygulama_kilidi);
        if (dugmeUygulamaKilidi != null) {
            dugmeUygulamaKilidi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    uygulamaKilidiYonetimDiyalogu();
                }
            });
        }

        Button dugmeKullanimRaporu = (Button) findViewById(R.id.dugme_kullanim_raporu);
        if (dugmeKullanimRaporu != null) {
            dugmeKullanimRaporu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    kullanimRaporuAc();
                }
            });
        }

        // --- KATEGORİ 5: Yedekleme ---
        Button dugmeYedekAl = (Button) findViewById(R.id.dugme_yedek_al);
        if (dugmeYedekAl != null) {
            dugmeYedekAl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean basarili = YedeklemeYoneticisi.yedekAl(AyarlarActivity.this);
                    if (basarili) {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, getString(R.string.yedek_alindi));
                    } else {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, "Yedek alınırken hata oluştu.");
                    }
                }
            });
        }

        Button dugmeYedekYukle = (Button) findViewById(R.id.dugme_yedek_yukle);
        if (dugmeYedekYukle != null) {
            dugmeYedekYukle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean basarili = YedeklemeYoneticisi.yedeğiYukle(AyarlarActivity.this);
                    if (basarili) {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, getString(R.string.yedek_yuklendi));
                        durumlariGuncelle();
                    } else {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, getString(R.string.yedek_bulunamadi));
                    }
                }
            });
        }

        Button dugmeYedekPaylas = (Button) findViewById(R.id.dugme_yedek_paylas);
        if (dugmeYedekPaylas != null) {
            dugmeYedekPaylas.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    YedeklemeYoneticisi.yedegiPaylas(AyarlarActivity.this);
                }
            });
        }

        Button dugmeYedekDosyadanSec = (Button) findViewById(R.id.dugme_yedek_dosyadan_sec);
        if (dugmeYedekDosyadanSec != null) {
            dugmeYedekDosyadanSec.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent niyet = new Intent(Intent.ACTION_GET_CONTENT);
                        niyet.setType("*/*");
                        startActivityForResult(Intent.createChooser(niyet, "Yedek Dosyası Seçin (.json)"), REQUEST_DOSYADAN_YEDEK);
                    } catch (Throwable t) {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, "Dosya seçici açılamadı.");
                    }
                }
            });
        }

        Button dugmeYedekIndirilenlerKaydet = (Button) findViewById(R.id.dugme_yedek_indirilenler_kaydet);
        if (dugmeYedekIndirilenlerKaydet != null) {
            dugmeYedekIndirilenlerKaydet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean ok = YedeklemeYoneticisi.indirilenlereDisaAktar(AyarlarActivity.this);
                    if (ok) {
                        ArayuzYardimcisi.toastUzun(AyarlarActivity.this, "Yedek İndirilenler klasörüne (TamEkran_Yedek.json) kaydedildi.");
                    } else {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, "İndirilenler klasörüne kaydedilemedi.");
                    }
                }
            });
        }

        Button dugmeYedekIndirilenlerYukle = (Button) findViewById(R.id.dugme_yedek_indirilenler_yukle);
        if (dugmeYedekIndirilenlerYukle != null) {
            dugmeYedekIndirilenlerYukle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean ok = YedeklemeYoneticisi.indirilenlerdenYukle(AyarlarActivity.this);
                    if (ok) {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, "Yedek İndirilenler klasöründen yüklendi!");
                        durumlariGuncelle();
                    } else {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, "İndirilenler'de 'TamEkran_Yedek.json' bulunamadı.");
                    }
                }
            });
        }

        Button dugmeOtoYedekYukle = (Button) findViewById(R.id.dugme_oto_yedek_yukle);
        if (dugmeOtoYedekYukle != null) {
            dugmeOtoYedekYukle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final List<OtomatikYedeklemeYoneticisi.OtoYedekBilgisi> yedekler = OtomatikYedeklemeYoneticisi.otoYedekleriListele(AyarlarActivity.this);
                    if (yedekler == null || yedekler.isEmpty()) {
                        ArayuzYardimcisi.toast(AyarlarActivity.this, "Kayıtlı otomatik yedek bulunamadı.");
                        return;
                    }
                    String[] basliklar = new String[yedekler.size()];
                    for (int i = 0; i < yedekler.size(); i++) {
                        OtomatikYedeklemeYoneticisi.OtoYedekBilgisi b = yedekler.get(i);
                        basliklar[i] = "Yedek " + b.sira + " (" + b.tarihEtiketi + ")";
                    }
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Otomatik Yedekten Geri Yükle", basliklar, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            boolean ok = OtomatikYedeklemeYoneticisi.otoYedektenGeriYukle(AyarlarActivity.this, yedekler.get(which).dosya);
                            if (ok) {
                                ArayuzYardimcisi.toast(AyarlarActivity.this, "Otomatik yedek yüklendi!");
                                durumlariGuncelle();
                            } else {
                                ArayuzYardimcisi.toast(AyarlarActivity.this, "Otomatik yedek geri yüklenemedi.");
                            }
                        }
                    });
                }
            });
        }

        // --- KATEGORİ 6: Güncelleme ---
        Button dugmeSistemUygulamaBilgisi = (Button) findViewById(R.id.dugme_sistem_uygulama_bilgisi);
        if (dugmeSistemUygulamaBilgisi != null) {
            dugmeSistemUygulamaBilgisi.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SistemAraclariYardimcisi.kisitliAyarlarIcinUygulamaBilgisiAc(AyarlarActivity.this);
                }
            });
        }

        Button dugmeKendiniApkPaylas = (Button) findViewById(R.id.dugme_kendini_apk_paylas);
        if (dugmeKendiniApkPaylas != null) {
            dugmeKendiniApkPaylas.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ApkPaylasimYardimcisi.kendiniApkOlarakPaylasSecimli(AyarlarActivity.this);
                }
            });
        }

        yaziDurum = (TextView) findViewById(R.id.yazi_guncelleme_durum);
        Button dugmeGuncelle = (Button) findViewById(R.id.dugme_guncelleme_denetle);
        if (dugmeGuncelle != null) {
            dugmeGuncelle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (yaziDurum != null) {
                        yaziDurum.setVisibility(View.VISIBLE);
                        yaziDurum.setText(R.string.guncelleme_denetleniyor);
                    }
                    GuncellemeYoneticisi.guncellemeleriDenetle(AyarlarActivity.this, new GuncellemeYoneticisi.DurumDinleyici() {
                        @Override
                        public void durumGuncelle(final String mesaj) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (yaziDurum != null) {
                                        yaziDurum.setVisibility(View.VISIBLE);
                                        yaziDurum.setText(mesaj);
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }

        // Sürüm Bilgisi
        TextView yaziSurum = (TextView) findViewById(R.id.yazi_surum_bilgisi);
        if (yaziSurum != null) {
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                yaziSurum.setText("Sürüm " + pInfo.versionName + " (API 8 - API 36+)");
            } catch (Throwable t) {
                LauncherLog.d("AyarlarActivity", "Surum bilgisi alinamadi: " + t.getMessage());
            }
        }
    }

    private void uzunBasmaOzellestirDiyalogu() {
        final String[] anahtarlar = new String[]{
            "adlandir", "ozel_simge", "kaydirma", "dock",
            "klasor", "kategori", "kilit", "uygulama_bilgisi",
            "play_store", "apk_paylas", "gizle", "kaldir"
        };
        final String[] etiketler = new String[]{
            "✏️ Yeniden Adlandır",
            "🖼️ Özel Simge Seç",
            "👆 Yukarı Kaydırma Eylemi",
            "📌 Alt Bara Sabitle / Kaldır",
            "📁 Klasöre Ekle",
            "🏷️ Menü Kategorisi Belirle (Çekmece)",
            "🔒 Uygulamayı Kilitle / Aç",
            "ℹ️ Uygulama Bilgisi",
            "🛍️ Google Play'de Göster (Çekmece)",
            "📦 APK Dosyasını Paylaş (Çekmece)",
            "👁️ Menüde Gizle (Çekmece)",
            "📲 Uygulamayı Kaldır (Uninstall)"
        };
        java.util.Set<String> devreDisi = AyarBankasi.getUzunBasmaDevreDisiOgeler(this);
        final boolean[] secimler = new boolean[anahtarlar.length];
        for (int i = 0; i < anahtarlar.length; i++) {
            secimler[i] = !devreDisi.contains(anahtarlar[i]);
        }
        new AlertDialog.Builder(this)
            .setTitle("Uzun Basma Menüsü Öğeleri")
            .setMultiChoiceItems(etiketler, secimler, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    secimler[which] = isChecked;
                }
            })
            .setPositiveButton(R.string.tamam, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    java.util.Set<String> yeniDevreDisi = new java.util.HashSet<String>();
                    for (int i = 0; i < anahtarlar.length; i++) {
                        if (!secimler[i]) yeniDevreDisi.add(anahtarlar[i]);
                    }
                    AyarBankasi.setUzunBasmaDevreDisiOgeler(AyarlarActivity.this, yeniDevreDisi);
                    ArayuzYardimcisi.toast(AyarlarActivity.this, "Uzun basma menüsü güncellendi.");
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private void hareketButonuAyarla(int butonId, final String anahtar, final int varsayilan) {
        final Button buton = (Button) findViewById(butonId);
        if (buton == null) return;
        int mevcutEylem = HareketYoneticisi.eylemiAl(this, anahtar, varsayilan);
        buton.setText(HareketYoneticisi.EYLEM_METINLERI[mevcutEylem]);

        buton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, "Hareket Eylemi Seçin", HareketYoneticisi.EYLEM_METINLERI, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                    @Override
                    public void onSecildi(int which) {
                        HareketYoneticisi.eylemiKaydet(AyarlarActivity.this, anahtar, which);
                        buton.setText(HareketYoneticisi.EYLEM_METINLERI[which]);
                        ArayuzYardimcisi.toast(AyarlarActivity.this, "Hareket eylemi güncellendi.");
                    }
                });
            }
        });
    }

    private void akordeonlariVeAramayiAyarla() {
        final int[] kartIds = new int[]{
                R.id.kart_akordeon_1, R.id.kart_akordeon_2, R.id.kart_akordeon_3, R.id.kart_akordeon_4,
                R.id.kart_akordeon_5, R.id.kart_akordeon_6, R.id.kart_akordeon_7, R.id.kart_akordeon_8
        };
        final int[] baslikIds = new int[]{
                R.id.baslik_akordeon_1, R.id.baslik_akordeon_2, R.id.baslik_akordeon_3, R.id.baslik_akordeon_4,
                R.id.baslik_akordeon_5, R.id.baslik_akordeon_6, R.id.baslik_akordeon_7, R.id.baslik_akordeon_8
        };
        final int[] icerikIds = new int[]{
                R.id.icerik_akordeon_1, R.id.icerik_akordeon_2, R.id.icerik_akordeon_3, R.id.icerik_akordeon_4,
                R.id.icerik_akordeon_5, R.id.icerik_akordeon_6, R.id.icerik_akordeon_7, R.id.icerik_akordeon_8
        };
        final int[] okIds = new int[]{
                R.id.ok_akordeon_1, R.id.ok_akordeon_2, R.id.ok_akordeon_3, R.id.ok_akordeon_4,
                R.id.ok_akordeon_5, R.id.ok_akordeon_6, R.id.ok_akordeon_7, R.id.ok_akordeon_8
        };

        final boolean[] acikDurumlar = new boolean[kartIds.length];
        for (int i = 0; i < kartIds.length; i++) {
            final int indeks = i;
            View baslik = findViewById(baslikIds[i]);
            final View icerik = findViewById(icerikIds[i]);
            final TextView ok = (TextView) findViewById(okIds[i]);
            if (baslik != null && icerik != null) {
                baslik.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean yeniDurum = icerik.getVisibility() != View.VISIBLE;
                        acikDurumlar[indeks] = yeniDurum;
                        icerik.setVisibility(yeniDurum ? View.VISIBLE : View.GONE);
                        if (ok != null) {
                            ok.setText(yeniDurum ? "▾" : "▸");
                        }
                    }
                });
            }
        }

        EditText girdiAra = (EditText) findViewById(R.id.girdi_ayar_ara);
        if (girdiAra != null) {
            girdiAra.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    ayarlariFiltrele(s != null ? s.toString() : "", kartIds, icerikIds, okIds, acikDurumlar);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void ayarlariFiltrele(
            String hamSorgu,
            int[] kartIds,
            int[] icerikIds,
            int[] okIds,
            boolean[] acikDurumlar) {
        String sorgu = turkceKucult(hamSorgu).trim();
        boolean aramaAktif = sorgu.length() > 0;

        for (int i = 0; i < kartIds.length; i++) {
            View kart = findViewById(kartIds[i]);
            ViewGroup icerik = (ViewGroup) findViewById(icerikIds[i]);
            TextView ok = (TextView) findViewById(okIds[i]);
            if (kart == null || icerik == null) continue;

            if (!aramaAktif) {
                kart.setVisibility(View.VISIBLE);
                for (int c = 0; c < icerik.getChildCount(); c++) {
                    icerik.getChildAt(c).setVisibility(View.VISIBLE);
                }
                icerik.setVisibility(acikDurumlar[i] ? View.VISIBLE : View.GONE);
                if (ok != null) ok.setText(acikDurumlar[i] ? "▾" : "▸");
                continue;
            }

            int eslesenSatir = 0;
            for (int c = 0; c < icerik.getChildCount(); c++) {
                View satir = icerik.getChildAt(c);
                String satirMetni = turkceKucult(gorunumMetniniTopla(satir));
                if (satirMetni.contains(sorgu)) {
                    satir.setVisibility(View.VISIBLE);
                    eslesenSatir++;
                } else {
                    satir.setVisibility(View.GONE);
                }
            }

            if (eslesenSatir > 0) {
                kart.setVisibility(View.VISIBLE);
                icerik.setVisibility(View.VISIBLE);
                if (ok != null) ok.setText("▾");
            } else {
                kart.setVisibility(View.GONE);
            }
        }
    }

    private static String gorunumMetniniTopla(View v) {
        if (v == null) return "";
        if (v instanceof TextView) {
            CharSequence cs = ((TextView) v).getText();
            return cs != null ? cs.toString() : "";
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < vg.getChildCount(); i++) {
                String parca = gorunumMetniniTopla(vg.getChildAt(i));
                if (parca.length() > 0) {
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(parca);
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static String turkceKucult(String s) {
        if (s == null) return "";
        String kucuk = s.toLowerCase(Locale.US);
        StringBuilder sb = new StringBuilder(kucuk.length());
        for (int i = 0; i < kucuk.length(); i++) {
            char c = kucuk.charAt(i);
            switch (c) {
                case 'ç': case 'Ç': sb.append('c'); break;
                case 'ğ': case 'Ğ': sb.append('g'); break;
                case 'ı': case 'İ': sb.append('i'); break;
                case 'ö': case 'Ö': sb.append('o'); break;
                case 'ş': case 'Ş': sb.append('s'); break;
                case 'ü': case 'Ü': sb.append('u'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private void canliKalibratorAc(String extraKey, int izgaraModu) {
        Intent intent = new Intent(AyarlarActivity.this, AnaEkranActivity.class);
        intent.putExtra(extraKey, true);
        if (izgaraModu >= 0) {
            intent.putExtra(AnaEkranActivity.EXTRA_IZGARA_MODU, izgaraModu);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    private void baglaToggle(int id, final String prefKey, final boolean varsayilan) {
        Button b = (Button) findViewById(id);
        if (b != null) {
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean mevcut = ayarlar.getBoolean(prefKey, varsayilan);
                    AyarBankasi.editorKaydet(ayarlar.edit().putBoolean(prefKey, !mevcut));
                    durumlariGuncelle();
                }
            });
        }
    }

    private void guncelleToggle(int id, String prefKey, boolean varsayilan, String baslik, String onDurum, String offDurum) {
        Button b = (Button) findViewById(id);
        if (b != null) {
            boolean aktif = ayarlar.getBoolean(prefKey, varsayilan);
            b.setText(baslik + ": " + (aktif ? onDurum : offDurum));
        }
    }

    private void baglaSecim(int id, final String baslik, final String[] secenekler, final SecimAksiyonu aksiyon) {
        Button b = (Button) findViewById(id);
        if (b != null) {
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AyarDiyalogYardimcisi.listeSecimDiyalogu(AyarlarActivity.this, baslik, secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                        @Override
                        public void onSecildi(int which) {
                            aksiyon.secildi(which);
                            durumlariGuncelle();
                            ArayuzYardimcisi.toast(AyarlarActivity.this, baslik + ": " + secenekler[which]);
                        }
                    });
                }
            });
        }
    }

    private void guncelleSecim(int id, String baslik, String[] secenekler, int indeks) {
        Button b = (Button) findViewById(id);
        if (b != null) {
            if (indeks < 0 || indeks >= secenekler.length) indeks = 0;
            b.setText(baslik + ": " + secenekler[indeks]);
        }
    }

    private void durumlariGuncelle() {
        if (dugmeVarsayilanBaslatici != null) {
            boolean varsayilan = SistemAraclariYardimcisi.varsayilanBaslaticiMi(this);
            dugmeVarsayilanBaslatici.setText(varsayilan
                ? "🏠 Varsayılan Başlatıcı: Etkin (Tam Ekran)"
                : "🏠 Varsayılan Başlatıcı Olarak Ayarla");
        }

        if (dugmeMasaustuModu != null) {
            boolean tekKatman = MasaustuYoneticisi.isTekKatmanModu(this);
            dugmeMasaustuModu.setText("Ana Ekran Düzeni: " + (tekKatman ? "Tek Katman (iOS Modu)" : "Klasik (Çekmeceli)"));
        }

        Button bSayfa = (Button) findViewById(R.id.dugme_sayfa_sayisi);
        if (bSayfa != null) {
            bSayfa.setText("Masaüstü Sayfa Sayısı: " + MasaustuYoneticisi.getSayfaSayisi(this) + " Sayfa");
        }

        Button bVarsayilanSayfa = (Button) findViewById(R.id.dugme_varsayilan_sayfa);
        if (bVarsayilanSayfa != null) {
            int toplam = Math.max(1, MasaustuYoneticisi.getSayfaSayisi(this));
            int vs = Math.max(0, Math.min(toplam - 1, MasaustuYoneticisi.getVarsayilanSayfa(this)));
            bVarsayilanSayfa.setText("Varsayılan Ana Sayfa: " + (vs + 1) + ". Sayfa");
        }

        guncelleToggle(R.id.dugme_sonsuz_kaydirma, MasaustuYoneticisi.PREF_SONSUZ_KAYDIRMA, false, "Masaüstünde Sonsuz Kaydırma", "Açık", "Kapalı");
        guncelleSecim(R.id.dugme_arama_motoru, "Arama Sağlayıcısı", AramaMotoruYoneticisi.MOTOR_ISIMLERI, AramaMotoruYoneticisi.getAktifMotor(this));
        guncelleSecim(R.id.dugme_klasor_sekli, "Klasör Simge Şekli", KlasorSimgeUretici.SEKIL_ISIMLERI, KlasorSimgeUretici.getKlasorSekli(this));
        guncelleSecim(R.id.dugme_cekmece_siralamasi, "Menü Sıralaması", CekmeceSiralamasi.SIRALAMA_ISIMLERI, CekmeceSiralamasi.getSiralamaTuru(this));

        Button bSaydamlik = (Button) findViewById(R.id.dugme_cekmece_saydamlik);
        if (bSaydamlik != null) {
            bSaydamlik.setText("Menü Arka Plan Şeffaflığı: %" + ayarlar.getInt(YedeklemeYoneticisi.PREF_CEKMECE_SAYDAMLIK, 100));
        }

        guncelleSecim(R.id.dugme_hareket_sallama, "Sallama Eylemi (Shake)", SallamaAlgilayici.SALLAMA_EYLEM_METINLERI, ayarlar.getInt(SallamaAlgilayici.PREF_SALLAMA_EYLEMI, SallamaAlgilayici.EYLEM_KAPALI));
        guncelleToggle(R.id.dugme_masaustu_kilit, YedeklemeYoneticisi.PREF_MASAUSTU_KILITLI, false, "Masaüstünü Kilitle", "Kilitli (Düzenlenemez)", "Açık (Serbest)");

        Button bDockSayisi = (Button) findViewById(R.id.dugme_dock_sayisi);
        if (bDockSayisi != null) {
            bDockSayisi.setText("Alt Çubuk Simge Sayısı: " + ayarlar.getInt(YedeklemeYoneticisi.PREF_DOCK_SIMGE_SAYISI, 4));
        }

        Button bDockYuksekligi = (Button) findViewById(R.id.dugme_dock_yuksekligi);
        if (bDockYuksekligi != null) {
            boolean kaydir = AyarBankasi.isDockKaydirilabilir(this);
            bDockYuksekligi.setText("⚓ Canlı Düzen Stüdyosu (Alt Bar): " + AyarBankasi.getDockYuksekligi(this) + " dp (%" + AyarBankasi.getDockSimgeBoyutu(this) + " | " + (kaydir ? "Kaydırılabilir" : "Sabit") + ")");
        }

        guncelleToggle(R.id.dugme_dock_gorunurluk, YedeklemeYoneticisi.PREF_DOCK_GORUNUR, true, "Sabit Alt Çubuk (Dock)", "Görünür", "Gizli");

        Button bKilit = (Button) findViewById(R.id.dugme_uygulama_kilidi);
        if (bKilit != null) {
            boolean pinVar = UygulamaKilidiYoneticisi.pinAyarliMi(this);
            int sayi = UygulamaKilidiYoneticisi.kilitliPaketleriAl(this).size();
            bKilit.setText("🔒 Uygulama Kilidi & PIN: " + (pinVar ? ("Aktif (" + sayi + " kilitli)") : "Kapalı (PIN Ayarla)"));
        }

        guncelleSecim(R.id.dugme_gecis_efekti, "Sayfa Geçiş Efekti", SayfaliMasaustuGorunumu.EFEKT_ISIMLERI, ayarlar.getInt(YedeklemeYoneticisi.PREF_GECIS_EFEKTI, SayfaliMasaustuGorunumu.EFEKT_STANDART));

        Button bRozet = (Button) findViewById(R.id.dugme_bildirim_rozetleri);
        if (bRozet != null) {
            int rozet = BildirimYoneticisi.getRozetTuru(this);
            String ad = (rozet == BildirimYoneticisi.ROZET_SAYI) ? "Sayı Rozeti" : ((rozet == BildirimYoneticisi.ROZET_NOKTA) ? "Nokta Rozeti" : "Kapalı");
            bRozet.setText("Bildirim Rozetleri: " + ad);
        }

        Button bSimgeBoyut = (Button) findViewById(R.id.dugme_simge_boyutu);
        if (bSimgeBoyut != null) {
            bSimgeBoyut.setText("Simge Boyutu: %" + ayarlar.getInt(YedeklemeYoneticisi.PREF_SIMGE_BOYUTU, 100));
        }

        Button bTema = (Button) findViewById(R.id.dugme_tema_rengi);
        if (bTema != null) {
            int mevcut = TemaYoneticisi.getVurguRengi(this);
            String ad = TemaYoneticisi.RENK_ISIMLERI[0];
            for (int i = 0; i < TemaYoneticisi.RENK_DEGERLERI.length; i++) {
                if (TemaYoneticisi.RENK_DEGERLERI[i] == mevcut) {
                    ad = TemaYoneticisi.RENK_ISIMLERI[i];
                    break;
                }
            }
            bTema.setText("Arayüz Vurgu Rengi: " + ad);
        }

        String paket = ayarlar.getString(SimgePaketiYoneticisi.ANAHTAR_AKTIF_PAKET, "");
        if (yaziAktifPaket != null) {
            yaziAktifPaket.setText(paket.length() == 0 ? getString(R.string.varsayilan_simgeler) : paket);
        }

        List<String> gizliListesi = YedeklemeYoneticisi.gizlenenleriAl(this);
        if (yaziGizliSayisi != null) {
            yaziGizliSayisi.setText(gizliListesi.size() + " uygulama gizlendi.");
        }

        guncelleToggle(R.id.dugme_simge_etiketleri_degistir, YedeklemeYoneticisi.PREF_ETIKETLERI_GOSTER, true, "Simge Yazıları", "Açık", "Kapalı");
        guncelleToggle(R.id.dugme_tam_ekran_modu, PREF_TAM_EKRAN_MODU, false, "Durum Çubuğunu Gizle", "Açık", "Kapalı");
        guncelleToggle(R.id.dugme_sik_kullanilanlar_modu, PREF_SIK_KULLANILANLAR_MODU, true, "Sık Kullanılanlar Satırı", "Açık", "Kapalı");
        guncelleToggle(R.id.dugme_cekmece_sekmeleri_goster, AyarBankasi.PREF_CEKMECE_SEKMELERI_GOSTER, true, "Çekmece Sekmeleri (Kategoriler)", "Görünür", "Gizli");

        Button bYetki = (Button) findViewById(R.id.dugme_kilit_yetkisi);
        if (bYetki != null) {
            boolean yetkiVar = EkranKilitleyici.kilitYetkisiVarMi(this);
            bYetki.setText(yetkiVar ? "🔒 Ekran Kilitleme: Yetki Aktif (Test Et)" : "🔒 Ekran Kilitleme: İzin Gerekli (Dokun)");
        }

        Button bDolgu = (Button) findViewById(R.id.dugme_masaustu_dolgu);
        if (bDolgu != null) {
            int sol = GuvenliAlanYoneticisi.getSol(this);
            int ust = GuvenliAlanYoneticisi.getUst(this);
            int sag = GuvenliAlanYoneticisi.getSag(this);
            int alt = GuvenliAlanYoneticisi.getAlt(this);
            bDolgu.setText("📐 Güvenli Alan / Kırpma: " + sol + "," + ust + "," + sag + "," + alt + " dp");
        }

        guncelleSecim(R.id.dugme_dock_stili, "Dock Arka Plan Stili", DockArkaPlanYoneticisi.STIL_ISIMLERI, DockArkaPlanYoneticisi.getStil(this));
        guncelleToggle(R.id.dugme_otomatik_masaustune_ekle, PaketDegisimAlicisi.PREF_OTOMATIK_MASAUSTUNE_EKLE, true, "Yeni Uygulamaları Masaüstüne Ekle", "Açık", "Kapalı");
        guncelleSecim(R.id.dugme_yazi_stili, "Simge Yazı Stili", YaziStiliYoneticisi.STIL_ISIMLERI, YaziStiliYoneticisi.getYaziStili(this));

        Button bYaziBoyut = (Button) findViewById(R.id.dugme_simge_yazi_boyutu);
        if (bYaziBoyut != null) {
            int boyut = YaziStiliYoneticisi.getYaziBoyutu(this);
            String boyutAdi = "Standart (12sp)";
            for (int i = 0; i < YaziStiliYoneticisi.BOYUT_DEGERLERI.length; i++) {
                if (YaziStiliYoneticisi.BOYUT_DEGERLERI[i] == boyut) {
                    boyutAdi = YaziStiliYoneticisi.BOYUT_ISIMLERI[i];
                    break;
                }
            }
            bYaziBoyut.setText("Simge Yazı Boyutu: " + boyutAdi);
        }

        int mS = getMasaustuSutunSayisi(this);
        int mR = getMasaustuSatirSayisi(this);
        int cS = getCekmeceSutunSayisi(this);
        int cR = getCekmeceSatirSayisi(this);

        Button bMIzgara = (Button) findViewById(R.id.dugme_masaustu_izgara_canli);
        if (bMIzgara != null) {
            bMIzgara.setText("🏠 Masaüstü Izgarası: " + mS + " Sütun × " + mR + " Satır (Canlı Ayarla)");
        }
        Button bCIzgara = (Button) findViewById(R.id.dugme_cekmece_izgara_canli);
        if (bCIzgara != null) {
            bCIzgara.setText("📱 Çekmece Izgarası: " + cS + " Sütun × " + cR + " Satır (Canlı Ayarla)");
        }

        guncelleToggle(R.id.dugme_metin_golgesi, YaziStiliYoneticisi.PREF_METIN_GOLGESI, true, "Simge Metin Gölgesi", "Açık", "Kapalı");
        guncelleToggle(R.id.dugme_duvar_kagidi_kaydirma, YedeklemeYoneticisi.PREF_DUVAR_KAGIDI_KAYDIRMA, true, "Duvar Kağıdı Paralaks Kaydırma", "Açık", "Kapalı");

        Button bKapak = (Button) findViewById(R.id.dugme_klasor_kapak_modu);
        if (bKapak != null) {
            boolean kapak = KlasorKapakYoneticisi.isGenelKapakModu(this);
            bKapak.setText("Klasör Kapağı Modu (İlk Uygulamayı Aç): " + (kapak ? "Açık (Tüm Klasörler)" : "Özel / Kapalı"));
        }

        guncelleToggle(R.id.dugme_simge_normalizasyonu, SimgeNormalizasyonYoneticisi.PREF_SIMGE_NORMALIZASYONU, false, "Simge Boyut Normalizasyonu", "Açık", "Kapalı");
        guncelleSecim(R.id.dugme_simge_sekli, "Simge Adaptif Şekli", SimgeNormalizasyonYoneticisi.SEKIL_ISIMLERI, SimgeNormalizasyonYoneticisi.getSimgeSekli(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SistemAraclariYardimcisi.REQUEST_VARSAYILAN_BASLATICI) {
            durumlariGuncelle();
            if (SistemAraclariYardimcisi.varsayilanBaslaticiMi(this)) {
                ArayuzYardimcisi.toast(this, "Tam Ekran varsayılan başlatıcı olarak ayarlandı!");
            }
            return;
        }

        if (requestCode == REQUEST_DOSYADAN_YEDEK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            boolean basarili = YedeklemeYoneticisi.dosyaIcerigindenYukle(this, data.getData());
            if (basarili) {
                ArayuzYardimcisi.toast(this, "Yedek başarıyla yüklendi!");
                durumlariGuncelle();
            } else {
                ArayuzYardimcisi.toast(this, "Yedek dosyası okunamadı veya geçersiz.");
            }
        }
    }

    public static int getMasaustuSutunSayisi(Context context) {
        return AyarBankasi.getMasaustuSutun(context);
    }

    public static int getMasaustuSatirSayisi(Context context) {
        return AyarBankasi.getMasaustuSatir(context);
    }

    public static int getCekmeceSutunSayisi(Context context) {
        return AyarBankasi.getCekmeceSutun(context);
    }

    public static int getCekmeceSatirSayisi(Context context) {
        return AyarBankasi.getCekmeceSatir(context);
    }

    public static int getSatirSayisi(Context context) {
        return getMasaustuSatirSayisi(context);
    }

    private void izgaraKaydet(int sutun, int satir) {
        AyarBankasi.editorKaydet(ayarlar.edit()
            .putInt(ANAHTAR_IZGARA_SUTUN, sutun)
            .putInt(ANAHTAR_IZGARA_SATIR, satir)
            );
        MasaustuYoneticisi.izgaraSinirlariniUyarla(this, sutun, satir);
        ArayuzYardimcisi.toast(this, "Izgara düzeni " + sutun + "×" + satir + " olarak güncellendi.");
    }

    private void uygulamaKilidiYonetimDiyalogu() {
        if (!UygulamaKilidiYoneticisi.pinAyarliMi(this)) {
            UygulamaKilidiYoneticisi.yeniPinBelirleDiyalogu(this, new Runnable() {
                @Override
                public void run() {
                    durumlariGuncelle();
                    ArayuzYardimcisi.toastUzun(AyarlarActivity.this, "PIN belirlendi. Şimdi uygulamalara uzun basarak kilitleyebilirsiniz.");
                }
            });
        } else {
            final Set<String> kilitliler = UygulamaKilidiYoneticisi.kilitliPaketleriAl(this);
            CharSequence[] secenekler = new CharSequence[]{
                "✏️ PIN Kodunu Değiştir",
                "🔓 PIN Korumasını ve Tüm Kilitleri Sıfırla (" + kilitliler.size() + " kilitli)"
            };

            AyarDiyalogYardimcisi.listeSecimDiyalogu(this, "🔒 Uygulama Kilidi Yönetimi", secenekler, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
                @Override
                public void onSecildi(int which) {
                    if (which == 0) {
                        UygulamaKilidiYoneticisi.pinSorgulaVeCalistir(AyarlarActivity.this, new Runnable() {
                            @Override
                            public void run() {
                                UygulamaKilidiYoneticisi.yeniPinBelirleDiyalogu(AyarlarActivity.this, new Runnable() {
                                    @Override
                                    public void run() {
                                        durumlariGuncelle();
                                    }
                                });
                            }
                        });
                    } else if (which == 1) {
                        UygulamaKilidiYoneticisi.pinSorgulaVeCalistir(AyarlarActivity.this, new Runnable() {
                            @Override
                            public void run() {
                                UygulamaKilidiYoneticisi.pinSifirla(AyarlarActivity.this);
                                durumlariGuncelle();
                                ArayuzYardimcisi.toast(AyarlarActivity.this, "Uygulama kilidi ve PIN sıfırlandı.");
                            }
                        });
                    }
                }
            });
        }
    }

    private void kullanimRaporuAc() {
        List<UygulamaBilgisi> apps = UygulamaYukleyici.tumunuYukle(this);
        KullanimRaporuDiyalogu.goster(this, apps, null);
    }

    private void simgePaketiSecimDiyalogu() {
        final List<SimgePaketiYoneticisi.SimgePaketiBilgisi> paketler = simgeMotoru.yukluPaketleriBul();

        List<String> isimler = new ArrayList<String>();
        isimler.add(getString(R.string.varsayilan_simgeler));

        for (SimgePaketiYoneticisi.SimgePaketiBilgisi p : paketler) {
            isimler.add(p.baslik);
        }

        AyarDiyalogYardimcisi.listeSecimDiyalogu(this, getString(R.string.ayar_simge_paketi), isimler.toArray(new CharSequence[0]), new AyarDiyalogYardimcisi.SecimDinleyicisi() {
            @Override
            public void onSecildi(int which) {
                if (which == 0) {
                    AyarBankasi.editorKaydet(ayarlar.edit().putString(SimgePaketiYoneticisi.ANAHTAR_AKTIF_PAKET, ""));
                } else {
                    AyarBankasi.editorKaydet(ayarlar.edit().putString(SimgePaketiYoneticisi.ANAHTAR_AKTIF_PAKET, paketler.get(which - 1).paketAdi));
                }
                durumlariGuncelle();
                ArayuzYardimcisi.toast(AyarlarActivity.this, "Simge paketi uygulandı.");
            }
        });
    }

    private void uygulamaGizlemeDiyalogu() {
        List<UygulamaBilgisi> list = UygulamaYukleyici.tumunuYukle(this);
        UygulamaGizlemeDiyalogu.goster(this, list, new Runnable() {
            @Override
            public void run() {
                durumlariGuncelle();
            }
        });
    }
}
