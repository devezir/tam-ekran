package com.tamekran.launcher;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Tam Ekran - Canlı Düzen Stüdyosu (Masaüstü, Çekmece ve Alt Bar / Dock)
 * Tüm görsel yerleşim, ızgara, simge boyutu, saydamlık, dock stili ve sıralama ayarlarını
 * tek bir yüzen pencerede canlı önizlemeyle yönetir.
 */
public class IzgaraKalibratoru {

    public static final int MOD_MASAUSTU = 0;
    public static final int MOD_CEKMECE = 1;
    public static final int MOD_DOCK = 2;

    private static final String[] DOLGU_ISIMLERI = new String[]{
        "Dar (Tam Kenar)",
        "Standart",
        "Geniş (Ferah)"
    };

    private interface PolimorfikSliderHedef {
        void onProgress(int mod, int progress);
        void onAdim(int mod, int yon);
    }

    private static void baglaPolimorfikSlider(
            SeekBar seekBar,
            Button eksi,
            Button arti,
            final int[] aktifMod,
            final Runnable guncelleArayuz,
            final PolimorfikSliderHedef hedef) {
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser) {
                        hedef.onProgress(aktifMod[0], progress);
                        guncelleArayuz.run();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            });
        }
        if (eksi != null) {
            eksi.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { hedef.onAdim(aktifMod[0], -1); }
            });
        }
        if (arti != null) {
            arti.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { hedef.onAdim(aktifMod[0], 1); }
            });
        }
    }

    private static void adimla(int[] arr, int adim, int min, int max, Runnable onChange) {
        int yeni = arr[0] + adim;
        if (yeni >= min && yeni <= max) {
            arr[0] = yeni;
            if (onChange != null) onChange.run();
        }
    }

    public static void goster(final AnaEkranActivity aktivite, final int baslangicModu) {
        if (aktivite == null || aktivite.isFinishing()) return;

        final SharedPreferences sp = AyarBankasi.getAyarlar(aktivite);

        // Başlangıç değerleri (İptal durumunda geri döndürmek için)
        final int basMasaustuSutun = AyarBankasi.getMasaustuSutun(aktivite);
        final int basMasaustuSatir = AyarBankasi.getMasaustuSatir(aktivite);
        final int basCekmeceSutun = AyarBankasi.getCekmeceSutun(aktivite);
        final int basCekmeceSatir = AyarBankasi.getCekmeceSatir(aktivite);
        final int basMasaustuSimge = AyarBankasi.getMasaustuSimgeBoyutu(aktivite);
        final int basCekmeceSimge = AyarBankasi.getCekmeceSimgeBoyutu(aktivite);
        final int basDockSimge = AyarBankasi.getDockSimgeBoyutu(aktivite);
        final int basDockYukseklik = AyarBankasi.getDockYuksekligi(aktivite);
        final int basDockYuva = AyarBankasi.getDockSimgeSayisi(aktivite);
        final boolean basDockKaydirilabilir = AyarBankasi.isDockKaydirilabilir(aktivite);
        final int basDockStil = DockArkaPlanYoneticisi.getDockStili(aktivite);
        final boolean basDockGorunur = AyarBankasi.isDockGorunur(aktivite);

        final boolean basEtiketGoster = AyarBankasi.isEtiketlerGoster(aktivite);
        final int basMasaustuDolgu = AyarBankasi.getInt(aktivite, AyarBankasi.PREF_MASAUSTU_DOLGU, 1);
        final int basKlasorSekli = KlasorSimgeUretici.getKlasorSekli(aktivite);
        final int basGecisEfekti = AyarBankasi.getInt(aktivite, AyarBankasi.PREF_GECIS_EFEKTI, SayfaliMasaustuGorunumu.EFEKT_STANDART);
        final int basSaydamlik = AyarBankasi.getCekmeceSaydamlik(aktivite);
        final boolean basSikKullanilanlar = AyarBankasi.getBoolean(aktivite, AyarBankasi.PREF_SIK_KULLANILANLAR_MODU, true);
        final boolean basCekmeceSekmeleri = AyarBankasi.isCekmeceSekmeleriGoster(aktivite);
        final int basSiralama = CekmeceSiralamasi.getSiralamaTuru(aktivite);

        // Düzenlenebilir anlık durumlar
        final int[] mSutun = new int[]{basMasaustuSutun};
        final int[] mSatir = new int[]{basMasaustuSatir};
        final int[] cSutun = new int[]{basCekmeceSutun};
        final int[] cSatir = new int[]{basCekmeceSatir};
        final int[] mSimge = new int[]{basMasaustuSimge};
        final int[] cSimge = new int[]{basCekmeceSimge};
        final int[] dSimge = new int[]{basDockSimge};
        final int[] dYukseklik = new int[]{basDockYukseklik};
        final int[] dYuva = new int[]{basDockYuva};
        final boolean[] dKaydirilabilir = new boolean[]{basDockKaydirilabilir};
        final int[] dStil = new int[]{basDockStil};
        final boolean[] dGorunur = new boolean[]{basDockGorunur};

        final boolean[] etiketGoster = new boolean[]{basEtiketGoster};
        final int[] masaustuDolgu = new int[]{basMasaustuDolgu};
        final int[] klasorSekli = new int[]{basKlasorSekli};
        final int[] gecisEfekti = new int[]{basGecisEfekti};
        final int[] saydamlik = new int[]{basSaydamlik};
        final boolean[] sikKullanilanlar = new boolean[]{basSikKullanilanlar};
        final boolean[] cekmeceSekmeleri = new boolean[]{basCekmeceSekmeleri};
        final int[] siralama = new int[]{basSiralama};

        final int[] aktifMod = new int[]{baslangicModu};
        final boolean[] kilavuzAcik = new boolean[]{true};

        final Dialog dialog = new Dialog(aktivite);
        dialog.setContentView(R.layout.diyalog_izgara_kalibrasyonu);
        ArayuzYardimcisi.kalibratorPenceresiAyarla(dialog, ArayuzYardimcisi.isKalibratorKonumAltta(), 0.08f);

        // Konum Butonu (Üste / Alta Taşı)
        final Button dugmeKonum = (Button) dialog.findViewById(R.id.dugme_konum);
        ArayuzYardimcisi.baglaKalibratorKonumDugmesi(dialog, dugmeKonum);

        // 3 Sekme
        final Button dugmeSekmeMasaustu = (Button) dialog.findViewById(R.id.dugme_sekme_masaustu);
        final Button dugmeSekmeCekmece = (Button) dialog.findViewById(R.id.dugme_sekme_cekmece);
        final Button dugmeSekmeDock = (Button) dialog.findViewById(R.id.dugme_sekme_dock);

        // Ortak / Polimorfik 3 Slider
        final TextView yaziSatir = (TextView) dialog.findViewById(R.id.yazi_etiket_satir);
        final SeekBar seekSatir = (SeekBar) dialog.findViewById(R.id.seekbar_satir);
        final Button dugmeSatirEksi = (Button) dialog.findViewById(R.id.dugme_satir_eksi);
        final Button dugmeSatirArti = (Button) dialog.findViewById(R.id.dugme_satir_arti);

        final TextView yaziSutun = (TextView) dialog.findViewById(R.id.yazi_etiket_sutun);
        final SeekBar seekSutun = (SeekBar) dialog.findViewById(R.id.seekbar_sutun);
        final Button dugmeSutunEksi = (Button) dialog.findViewById(R.id.dugme_sutun_eksi);
        final Button dugmeSutunArti = (Button) dialog.findViewById(R.id.dugme_sutun_arti);

        final TextView yaziSimge = (TextView) dialog.findViewById(R.id.yazi_etiket_simge_boyutu);
        final SeekBar seekSimge = (SeekBar) dialog.findViewById(R.id.seekbar_simge_boyutu);
        final Button dugmeSimgeEksi = (Button) dialog.findViewById(R.id.dugme_simge_eksi);
        final Button dugmeSimgeArti = (Button) dialog.findViewById(R.id.dugme_simge_arti);

        // Sekmeye Özel Gruplar
        final LinearLayout grupMasaustu = (LinearLayout) dialog.findViewById(R.id.grup_ek_masaustu);
        final LinearLayout grupCekmece = (LinearLayout) dialog.findViewById(R.id.grup_ek_cekmece);
        final LinearLayout grupDock = (LinearLayout) dialog.findViewById(R.id.grup_ek_dock);

        // Masaüstü Ek Kontrolleri
        final Button dugmeKilavuz = (Button) dialog.findViewById(R.id.dugme_kilavuz);
        final Button dugmeEtiket = (Button) dialog.findViewById(R.id.dugme_etiket_goster_canli);
        final Button dugmeDolgu = (Button) dialog.findViewById(R.id.dugme_masaustu_dolgu_canli);
        final Button dugmeKlasorSekli = (Button) dialog.findViewById(R.id.dugme_klasor_sekli_canli);
        final Button dugmeGecisEfekti = (Button) dialog.findViewById(R.id.dugme_gecis_efekti_canli);

        // Çekmece Ek Kontrolleri
        final TextView yaziSaydamlik = (TextView) dialog.findViewById(R.id.yazi_etiket_saydamlik);
        final SeekBar seekSaydamlik = (SeekBar) dialog.findViewById(R.id.seekbar_saydamlik);
        final Button dugmeSaydamlikEksi = (Button) dialog.findViewById(R.id.dugme_saydamlik_eksi);
        final Button dugmeSaydamlikArti = (Button) dialog.findViewById(R.id.dugme_saydamlik_arti);
        final Button dugmeKilavuzCekmece = (Button) dialog.findViewById(R.id.dugme_kilavuz_cekmece);
        final Button dugmeSikKullanilanlar = (Button) dialog.findViewById(R.id.dugme_sik_kullanilanlar_canli);
        final Button dugmeCekmeceSekmeleri = (Button) dialog.findViewById(R.id.dugme_cekmece_sekmeleri_canli);
        final Button dugmeSiralama = (Button) dialog.findViewById(R.id.dugme_siralama_canli);

        // Alt Bar Ek Kontrolleri
        final Button dugmeDockKaydir = (Button) dialog.findViewById(R.id.dugme_dock_kaydirilabilir_canli);
        final Button dugmeDockStili = (Button) dialog.findViewById(R.id.dugme_dock_stili_canli);
        final Button dugmeDockGorunur = (Button) dialog.findViewById(R.id.dugme_dock_gorunur_canli);

        // Arayüzü ve Canlı Önizlemeyi Güncelleyen Yardımcı
        final Runnable guncelleArayuz = new Runnable() {
            @Override
            public void run() {
                int mod = aktifMod[0];
                boolean masaustuMu = (mod == MOD_MASAUSTU);
                boolean cekmeceMi = (mod == MOD_CEKMECE);
                boolean dockMu = (mod == MOD_DOCK);

                // Sekme renkleri
                if (dugmeSekmeMasaustu != null) dugmeSekmeMasaustu.setTextColor(masaustuMu ? 0xFF00E5FF : 0xFF888888);
                if (dugmeSekmeCekmece != null) dugmeSekmeCekmece.setTextColor(cekmeceMi ? 0xFF00E5FF : 0xFF888888);
                if (dugmeSekmeDock != null) dugmeSekmeDock.setTextColor(dockMu ? 0xFF00E5FF : 0xFF888888);

                // Grup görünürlükleri
                if (grupMasaustu != null) grupMasaustu.setVisibility(masaustuMu ? View.VISIBLE : View.GONE);
                if (grupCekmece != null) grupCekmece.setVisibility(cekmeceMi ? View.VISIBLE : View.GONE);
                if (grupDock != null) grupDock.setVisibility(dockMu ? View.VISIBLE : View.GONE);

                if (dockMu) {
                    // Alt Bar (Dock) slider değerleri
                    if (yaziSatir != null) yaziSatir.setText("📏 Alt Bar Yüksekliği: " + dYukseklik[0] + " dp");
                    if (yaziSutun != null) yaziSutun.setText("⚡ Yuva Sayısı: " + dYuva[0] + " Simge");
                    if (yaziSimge != null) yaziSimge.setText("⚓ Dock Simge Büyüklüğü: %" + dSimge[0]);

                    if (seekSatir != null) {
                        if (seekSatir.getMax() != 48) seekSatir.setMax(48);
                        if (seekSatir.getProgress() != (dYukseklik[0] - 48)) seekSatir.setProgress(dYukseklik[0] - 48);
                    }
                    if (seekSutun != null) {
                        if (seekSutun.getMax() != 8) seekSutun.setMax(8);
                        if (seekSutun.getProgress() != (dYuva[0] - 1)) seekSutun.setProgress(dYuva[0] - 1);
                    }
                    if (seekSimge != null && seekSimge.getProgress() != (dSimge[0] - 50)) {
                        seekSimge.setProgress(dSimge[0] - 50);
                    }

                    if (dugmeDockKaydir != null) {
                        dugmeDockKaydir.setText(dKaydirilabilir[0] ? "🔄 Kaydırılabilir Alt Bar: Açık" : "🔒 Sabit Alt Bar: Kapalı");
                        dugmeDockKaydir.setTextColor(dKaydirilabilir[0] ? 0xFF00E5FF : 0xFFFFA726);
                    }
                    if (dugmeDockStili != null) {
                        String stilAdi = DockArkaPlanYoneticisi.STIL_ISIMLERI[Math.max(0, Math.min(DockArkaPlanYoneticisi.STIL_ISIMLERI.length - 1, dStil[0]))];
                        dugmeDockStili.setText("🎨 Dock Stili: " + stilAdi);
                    }
                    if (dugmeDockGorunur != null) {
                        dugmeDockGorunur.setText(dGorunur[0] ? "👁️ Alt Bar Görünürlüğü: Görünür" : "👁️ Alt Bar Görünürlüğü: Gizli");
                        dugmeDockGorunur.setTextColor(dGorunur[0] ? 0xFF69F0AE : 0xFFFF5252);
                    }

                    aktivite.canliDockGorunurlukGuncelle(dGorunur[0]);
                    aktivite.canliDockStiliGuncelle(dStil[0]);
                    aktivite.canliDockStudiouGuncelle(dYukseklik[0], dSimge[0], dYuva[0], dKaydirilabilir[0]);
                } else {
                    // Masaüstü veya Çekmece slider değerleri
                    int anlikSatir = masaustuMu ? mSatir[0] : cSatir[0];
                    int anlikSutun = masaustuMu ? mSutun[0] : cSutun[0];
                    int anlikSimge = masaustuMu ? mSimge[0] : cSimge[0];

                    if (yaziSatir != null) yaziSatir.setText("↕️ Satır Sayısı (Rows): " + anlikSatir + " Satır");
                    if (yaziSutun != null) yaziSutun.setText("↔️ Sütun Sayısı (Columns): " + anlikSutun + " Sütun");
                    if (yaziSimge != null) yaziSimge.setText((masaustuMu ? "🏠 Masaüstü" : "📱 Çekmece") + " Simge Büyüklüğü: %" + anlikSimge);

                    if (seekSatir != null) {
                        if (seekSatir.getMax() != 9) seekSatir.setMax(9);
                        if (seekSatir.getProgress() != (anlikSatir - 3)) seekSatir.setProgress(anlikSatir - 3);
                    }
                    if (seekSutun != null) {
                        if (seekSutun.getMax() != 9) seekSutun.setMax(9);
                        if (seekSutun.getProgress() != (anlikSutun - 3)) seekSutun.setProgress(anlikSutun - 3);
                    }
                    if (seekSimge != null && seekSimge.getProgress() != (anlikSimge - 50)) {
                        seekSimge.setProgress(anlikSimge - 50);
                    }

                    String kilavuzMetin = kilavuzAcik[0] ? "📐 Hücre Kılavuz Çizgileri: Açık" : "📐 Hücre Kılavuz Çizgileri: Kapalı";
                    int kilavuzRenk = kilavuzAcik[0] ? 0xFF00E5FF : 0xFF888888;
                    if (dugmeKilavuz != null) {
                        dugmeKilavuz.setText(kilavuzMetin);
                        dugmeKilavuz.setTextColor(kilavuzRenk);
                    }
                    if (dugmeKilavuzCekmece != null) {
                        dugmeKilavuzCekmece.setText(kilavuzMetin);
                        dugmeKilavuzCekmece.setTextColor(kilavuzRenk);
                    }

                    if (masaustuMu) {
                        if (dugmeEtiket != null) {
                            dugmeEtiket.setText(etiketGoster[0] ? "🏷️ Simge Yazıları: Açık" : "🏷️ Simge Yazıları: Kapalı");
                        }
                        if (dugmeDolgu != null) {
                            dugmeDolgu.setText("📏 Kenar Boşluğu: " + DOLGU_ISIMLERI[Math.max(0, Math.min(2, masaustuDolgu[0]))]);
                        }
                        if (dugmeKlasorSekli != null) {
                            String ksAdi = KlasorSimgeUretici.SEKIL_ISIMLERI[Math.max(0, Math.min(KlasorSimgeUretici.SEKIL_ISIMLERI.length - 1, klasorSekli[0]))];
                            dugmeKlasorSekli.setText("📁 Klasör Şekli: " + ksAdi);
                        }
                        if (dugmeGecisEfekti != null) {
                            String geAdi = SayfaliMasaustuGorunumu.EFEKT_ISIMLERI[Math.max(0, Math.min(SayfaliMasaustuGorunumu.EFEKT_ISIMLERI.length - 1, gecisEfekti[0]))];
                            dugmeGecisEfekti.setText("✨ Geçiş Efekti: " + geAdi);
                        }

                        aktivite.masaustuIzgaraGuncelle(mSutun[0], mSatir[0], kilavuzAcik[0]);
                        aktivite.canliMasaustuSimgeBoyutuGuncelle(mSimge[0]);
                        aktivite.canliMasaustuDolguGuncelle(masaustuDolgu[0]);
                        aktivite.canliEtiketGosterGuncelle(etiketGoster[0]);
                    } else {
                        if (yaziSaydamlik != null) yaziSaydamlik.setText("🎨 Çekmece Şeffaflığı: %" + saydamlik[0]);
                        if (seekSaydamlik != null && seekSaydamlik.getProgress() != saydamlik[0]) {
                            seekSaydamlik.setProgress(saydamlik[0]);
                        }
                        if (dugmeSikKullanilanlar != null) {
                            dugmeSikKullanilanlar.setText(sikKullanilanlar[0] ? "⭐ Sık Kullanılanlar Satırı: Açık" : "⭐ Sık Kullanılanlar Satırı: Kapalı");
                        }
                        if (dugmeCekmeceSekmeleri != null) {
                            dugmeCekmeceSekmeleri.setText(cekmeceSekmeleri[0] ? "🏷️ Çekmece Sekmeleri: Görünür" : "🏷️ Çekmece Sekmeleri: Gizli");
                        }
                        if (dugmeSiralama != null) {
                            String sirAdi = CekmeceSiralamasi.SIRALAMA_ISIMLERI[Math.max(0, Math.min(CekmeceSiralamasi.SIRALAMA_ISIMLERI.length - 1, siralama[0]))];
                            dugmeSiralama.setText("🔤 Sıralama: " + sirAdi);
                        }

                        aktivite.cekmeceIzgaraGuncelle(cSutun[0], cSatir[0], kilavuzAcik[0]);
                        aktivite.canliCekmeceSimgeBoyutuGuncelle(cSimge[0]);
                        aktivite.canliCekmeceSaydamlikGuncelle(saydamlik[0]);
                        aktivite.canliSikKullanilanlarGuncelle(sikKullanilanlar[0]);
                        aktivite.canliCekmeceSekmeleriGuncelle(cekmeceSekmeleri[0]);
                    }
                }
            }
        };

        // Sekme Geçişleri
        if (dugmeSekmeMasaustu != null) {
            dugmeSekmeMasaustu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    aktifMod[0] = MOD_MASAUSTU;
                    aktivite.cekmeceyiKapat();
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeSekmeCekmece != null) {
            dugmeSekmeCekmece.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    aktifMod[0] = MOD_CEKMECE;
                    aktivite.cekmeceyiAc();
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeSekmeDock != null) {
            dugmeSekmeDock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    aktifMod[0] = MOD_DOCK;
                    aktivite.cekmeceyiKapat();
                    guncelleArayuz.run();
                }
            });
        }

        // Slider 1 Dinleyicileri (Satır 3-12 veya Dock Yükseklik 48-96 dp)
        baglaPolimorfikSlider(seekSatir, dugmeSatirEksi, dugmeSatirArti, aktifMod, guncelleArayuz, new PolimorfikSliderHedef() {
            @Override
            public void onProgress(int mod, int progress) {
                if (mod == MOD_DOCK) dYukseklik[0] = progress + 48;
                else if (mod == MOD_MASAUSTU) mSatir[0] = progress + 3;
                else cSatir[0] = progress + 3;
            }
            @Override
            public void onAdim(int mod, int yon) {
                if (mod == MOD_DOCK) adimla(dYukseklik, yon * 2, 48, 96, guncelleArayuz);
                else adimla(mod == MOD_MASAUSTU ? mSatir : cSatir, yon, 3, 12, guncelleArayuz);
            }
        });

        // Slider 2 Dinleyicileri (Sütun 3-12 veya Dock Yuva Sayısı 1-9)
        baglaPolimorfikSlider(seekSutun, dugmeSutunEksi, dugmeSutunArti, aktifMod, guncelleArayuz, new PolimorfikSliderHedef() {
            @Override
            public void onProgress(int mod, int progress) {
                if (mod == MOD_DOCK) dYuva[0] = progress + 1;
                else if (mod == MOD_MASAUSTU) mSutun[0] = progress + 3;
                else cSutun[0] = progress + 3;
            }
            @Override
            public void onAdim(int mod, int yon) {
                if (mod == MOD_DOCK) adimla(dYuva, yon, 1, 9, guncelleArayuz);
                else adimla(mod == MOD_MASAUSTU ? mSutun : cSutun, yon, 3, 12, guncelleArayuz);
            }
        });

        // Slider 3 Dinleyicileri (Simge Büyüklüğü %50 - %150)
        baglaPolimorfikSlider(seekSimge, dugmeSimgeEksi, dugmeSimgeArti, aktifMod, guncelleArayuz, new PolimorfikSliderHedef() {
            private int[] hedefDizi(int mod) {
                return (mod == MOD_MASAUSTU) ? mSimge : (mod == MOD_CEKMECE ? cSimge : dSimge);
            }
            @Override
            public void onProgress(int mod, int progress) {
                hedefDizi(mod)[0] = progress + 50;
            }
            @Override
            public void onAdim(int mod, int yon) {
                adimla(hedefDizi(mod), yon * 5, 50, 150, guncelleArayuz);
            }
        });

        // Kılavuz Çizgileri (Masaüstü ve Çekmece)
        View.OnClickListener kilavuzTikla = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kilavuzAcik[0] = !kilavuzAcik[0];
                guncelleArayuz.run();
            }
        };
        if (dugmeKilavuz != null) dugmeKilavuz.setOnClickListener(kilavuzTikla);
        if (dugmeKilavuzCekmece != null) dugmeKilavuzCekmece.setOnClickListener(kilavuzTikla);

        // Masaüstü Ek Butonlar
        if (dugmeEtiket != null) {
            dugmeEtiket.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etiketGoster[0] = !etiketGoster[0];
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeDolgu != null) {
            dugmeDolgu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    masaustuDolgu[0] = (masaustuDolgu[0] + 1) % 3;
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeKlasorSekli != null) {
            dugmeKlasorSekli.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    klasorSekli[0] = (klasorSekli[0] + 1) % KlasorSimgeUretici.SEKIL_ISIMLERI.length;
                    aktivite.canliKlasorSekliGuncelle(klasorSekli[0]);
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeGecisEfekti != null) {
            dugmeGecisEfekti.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    gecisEfekti[0] = (gecisEfekti[0] + 1) % SayfaliMasaustuGorunumu.EFEKT_ISIMLERI.length;
                    aktivite.canliGecisEfektiGuncelle(gecisEfekti[0]);
                    guncelleArayuz.run();
                }
            });
        }

        // Çekmece Ek Kontrolleri
        ArayuzYardimcisi.baglaAyarAdimlayici(
                seekSaydamlik, dugmeSaydamlikEksi, dugmeSaydamlikArti,
                saydamlik, 0, 100, 5, guncelleArayuz);
        if (dugmeSikKullanilanlar != null) {
            dugmeSikKullanilanlar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sikKullanilanlar[0] = !sikKullanilanlar[0];
                    aktivite.canliSikKullanilanlarGuncelle(sikKullanilanlar[0]);
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeCekmeceSekmeleri != null) {
            dugmeCekmeceSekmeleri.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cekmeceSekmeleri[0] = !cekmeceSekmeleri[0];
                    aktivite.canliCekmeceSekmeleriGuncelle(cekmeceSekmeleri[0]);
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeSiralama != null) {
            dugmeSiralama.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    siralama[0] = (siralama[0] + 1) % CekmeceSiralamasi.SIRALAMA_ISIMLERI.length;
                    aktivite.canliSiralamaGuncelle(siralama[0]);
                    guncelleArayuz.run();
                }
            });
        }

        // Alt Bar Ek Kontrolleri
        if (dugmeDockKaydir != null) {
            dugmeDockKaydir.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dKaydirilabilir[0] = !dKaydirilabilir[0];
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeDockStili != null) {
            dugmeDockStili.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dStil[0] = (dStil[0] + 1) % DockArkaPlanYoneticisi.STIL_ISIMLERI.length;
                    guncelleArayuz.run();
                }
            });
        }
        if (dugmeDockGorunur != null) {
            dugmeDockGorunur.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dGorunur[0] = !dGorunur[0];
                    guncelleArayuz.run();
                }
            });
        }

        // Sıfırla (Aktif sekmenin varsayılanlarına döndürür)
        Button dugmeSifirla = (Button) dialog.findViewById(R.id.dugme_sifirla);
        if (dugmeSifirla != null) {
            dugmeSifirla.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (aktifMod[0] == MOD_MASAUSTU) {
                        mSutun[0] = 4; mSatir[0] = 6; mSimge[0] = 100;
                        etiketGoster[0] = true; masaustuDolgu[0] = 1;
                        klasorSekli[0] = KlasorSimgeUretici.SEKIL_YUVARLATILMIS;
                        gecisEfekti[0] = SayfaliMasaustuGorunumu.EFEKT_STANDART;
                        aktivite.canliKlasorSekliGuncelle(klasorSekli[0]);
                        aktivite.canliGecisEfektiGuncelle(gecisEfekti[0]);
                    } else if (aktifMod[0] == MOD_CEKMECE) {
                        cSutun[0] = 4; cSatir[0] = 6; cSimge[0] = 100;
                        saydamlik[0] = 95; sikKullanilanlar[0] = true;
                        cekmeceSekmeleri[0] = true;
                        siralama[0] = CekmeceSiralamasi.SIRALAMA_A_TO_Z;
                        aktivite.canliSikKullanilanlarGuncelle(sikKullanilanlar[0]);
                        aktivite.canliCekmeceSekmeleriGuncelle(cekmeceSekmeleri[0]);
                        aktivite.canliSiralamaGuncelle(siralama[0]);
                    } else {
                        dYukseklik[0] = 72; dYuva[0] = 4; dSimge[0] = 100;
                        dKaydirilabilir[0] = false;
                        dStil[0] = DockArkaPlanYoneticisi.STIL_KAYAN_HAP;
                        dGorunur[0] = true;
                    }
                    guncelleArayuz.run();
                    ArayuzYardimcisi.toast(aktivite, aktivite.getString(R.string.duzen_studyosu_sifirlandi));
                }
            });
        }

        // İptal
        Button dugmeIptal = (Button) dialog.findViewById(R.id.dugme_iptal);
        if (dugmeIptal != null) {
            dugmeIptal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.cancel();
                }
            });
        }

        // Kaydet / Tamam
        Button dugmeKaydet = (Button) dialog.findViewById(R.id.dugme_kaydet);
        if (dugmeKaydet != null) {
            dugmeKaydet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (sp != null) {
                        AyarBankasi.editorKaydet(sp.edit()
                            .putInt(AyarBankasi.ANAHTAR_IZGARA_SUTUN, mSutun[0])
                            .putInt(AyarBankasi.ANAHTAR_IZGARA_SATIR, mSatir[0])
                            .putInt(AyarBankasi.ANAHTAR_CEKMECE_SUTUN, cSutun[0])
                            .putInt(AyarBankasi.ANAHTAR_CEKMECE_SATIR, cSatir[0])
                            .putInt(AyarBankasi.PREF_MASAUSTU_SIMGE_BOYUTU, mSimge[0])
                            .putInt(AyarBankasi.PREF_CEKMECE_SIMGE_BOYUTU, cSimge[0])
                            .putInt(AyarBankasi.PREF_DOCK_SIMGE_BOYUTU, dSimge[0])
                            .putInt(AyarBankasi.PREF_SIMGE_BOYUTU, mSimge[0])
                            .putBoolean(AyarBankasi.PREF_ETIKETLERI_GOSTER, etiketGoster[0])
                            .putInt(AyarBankasi.PREF_MASAUSTU_DOLGU, masaustuDolgu[0])
                            .putInt(AyarBankasi.PREF_GECIS_EFEKTI, gecisEfekti[0])
                            .putInt(AyarBankasi.PREF_CEKMECE_SAYDAMLIK, saydamlik[0])
                            .putBoolean(AyarBankasi.PREF_SIK_KULLANILANLAR_MODU, sikKullanilanlar[0])
                            .putBoolean(AyarBankasi.PREF_CEKMECE_SEKMELERI_GOSTER, cekmeceSekmeleri[0])
                            .putInt(AyarBankasi.PREF_DOCK_YUKSEKLIK, dYukseklik[0])
                            .putInt(AyarBankasi.PREF_DOCK_SIMGE_SAYISI, dYuva[0])
                            .putBoolean(AyarBankasi.PREF_DOCK_KAYDIRILABILIR, dKaydirilabilir[0])
                            .putBoolean(AyarBankasi.PREF_DOCK_GORUNUR, dGorunur[0])
                            );
                    }

                    KlasorSimgeUretici.setKlasorSekli(aktivite, klasorSekli[0]);
                    CekmeceSiralamasi.setSiralamaTuru(aktivite, siralama[0]);
                    DockArkaPlanYoneticisi.setDockStili(aktivite, dStil[0]);

                    MasaustuYoneticisi.izgaraSinirlariniUyarla(aktivite, mSutun[0], mSatir[0]);

                    aktivite.masaustuIzgaraGuncelle(mSutun[0], mSatir[0], false);
                    aktivite.cekmeceIzgaraGuncelle(cSutun[0], cSatir[0], false);
                    aktivite.canliMasaustuSimgeBoyutuGuncelle(mSimge[0]);
                    aktivite.canliCekmeceSimgeBoyutuGuncelle(cSimge[0]);
                    aktivite.canliSikKullanilanlarGuncelle(sikKullanilanlar[0]);
                    aktivite.canliCekmeceSekmeleriGuncelle(cekmeceSekmeleri[0]);
                    aktivite.canliEtiketGosterGuncelle(etiketGoster[0]);
                    aktivite.canliMasaustuDolguGuncelle(masaustuDolgu[0]);
                    aktivite.canliDockGorunurlukGuncelle(dGorunur[0]);
                    aktivite.canliDockStiliGuncelle(dStil[0]);
                    aktivite.canliDockStudiouGuncelle(dYukseklik[0], dSimge[0], dYuva[0], dKaydirilabilir[0]);

                    dialog.dismiss();
                    ArayuzYardimcisi.toastUzun(aktivite, aktivite.getString(R.string.duzen_studyosu_kaydedildi));
                }
            });
        }

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                if (basKlasorSekli != klasorSekli[0]) {
                    aktivite.canliKlasorSekliGuncelle(basKlasorSekli);
                }
                if (basSiralama != siralama[0]) {
                    aktivite.canliSiralamaGuncelle(basSiralama);
                }
                aktivite.canliGecisEfektiGuncelle(basGecisEfekti);
                aktivite.masaustuIzgaraGuncelle(basMasaustuSutun, basMasaustuSatir, false);
                aktivite.cekmeceIzgaraGuncelle(basCekmeceSutun, basCekmeceSatir, false);
                aktivite.canliMasaustuSimgeBoyutuGuncelle(basMasaustuSimge);
                aktivite.canliCekmeceSimgeBoyutuGuncelle(basCekmeceSimge);
                aktivite.canliEtiketGosterGuncelle(basEtiketGoster);
                aktivite.canliMasaustuDolguGuncelle(basMasaustuDolgu);
                aktivite.canliCekmeceSaydamlikGuncelle(basSaydamlik);
                aktivite.canliSikKullanilanlarGuncelle(basSikKullanilanlar);
                aktivite.canliCekmeceSekmeleriGuncelle(basCekmeceSekmeleri);
                aktivite.canliDockGorunurlukGuncelle(basDockGorunur);
                aktivite.canliDockStiliGuncelle(basDockStil);
                aktivite.canliDockStudiouGuncelle(basDockYukseklik, basDockSimge, basDockYuva, basDockKaydirilabilir);
                if (baslangicModu == MOD_CEKMECE || aktifMod[0] == MOD_CEKMECE) {
                    aktivite.cekmeceyiKapat();
                }
            }
        });

        // Başlangıç moduna göre çekmeceyi aç veya kapat
        if (baslangicModu == MOD_CEKMECE) {
            aktivite.cekmeceyiAc();
        } else {
            aktivite.cekmeceyiKapat();
        }

        guncelleArayuz.run();
        dialog.show();
    }
}
