package com.tamekran.launcher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Endüstri Standardı Sürükleme ve Durum Kontrolcüsü (DragController & StateManager)
 * AOSP Launcher3 / Nova Launcher drag-pipeline referans alınmıştır.
 * Tüm sürükleme oturumunu, durum geçişlerini ve bırakma hedeflerini tek elden yönetir.
 */
public class SuruklemeKontrolcusu {

    public interface SuruklemeDinleyicisi {
        void onMasaustuGuncelle();
        void onDockGuncelle();
        void onCekmeceKapat();
        boolean onMasaustuneBirakildi(SuruklemeVerisi veri, float screenX, float screenY);
    }

    private final Activity aktivite;
    private int mevcutDurum = LauncherDurumu.NORMAL;
    private SuruklemeVerisi aktifVeri;
    private SuruklemeDinleyicisi dinleyici;

    // Arayüz Görünümleri
    private SayfaliMasaustuGorunumu workspace;
    private View dockAlani;

    // Kenar Kaydırma Zamanlayıcısı
    private long sonSayfaDegisimZamani = 0;
    private long sonBirakmaZamani = 0;

    public SuruklemeKontrolcusu(Activity aktivite) {
        this.aktivite = aktivite;
    }

    public void bilesenleriBagla(
            SayfaliMasaustuGorunumu workspace,
            View dockAlani,
            SuruklemeDinleyicisi dinleyici) {

        this.workspace = workspace;
        this.dockAlani = dockAlani;
        this.dinleyici = dinleyici;

        hedefDinleyicileriniKur();
    }

    public int getDurum() {
        return mevcutDurum;
    }

    public void setDurum(int yeniDurum) {
        this.mevcutDurum = yeniDurum;
    }

    public boolean isSurukleniyor() {
        return mevcutDurum == LauncherDurumu.SURUKLEME;
    }

    public boolean isMasaustuKilitli() {
        return AyarBankasi.isMasaustuKilitli(aktivite);
    }

    public SuruklemeVerisi getAktifVeri() {
        return aktifVeri;
    }

    /**
     * Sürükleme oturumunu resmi olarak başlatır (Spring-Loaded Moduna Geçiş)
     */
    public boolean suruklemeBaslat(View kaynakView, SuruklemeVerisi veri) {
        if (kaynakView == null || veri == null || veri.app == null) return false;

        // Masaüstü kilitli iken hiçbir kaynaktan (masaüstü, dock veya çekmece) sürükleme başlatılamaz
        if (ArayuzYardimcisi.kilitliMiVeUyar(aktivite)) {
            return false;
        }

        this.mevcutDurum = LauncherDurumu.SURUKLEME;
        this.aktifVeri = veri;

        // Çekmeceden geliyorsa arka plandaki masaüstünü göstermek için çekmeceyi gizle
        if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_CEKMECE && dinleyici != null) {
            dinleyici.onCekmeceKapat();
        }

        // Masaüstünü Nova Tarzı Hafifçe Odakla (Spring-Loaded Scale)
        if (workspace != null && Build.VERSION.SDK_INT >= 11) {
            workspace.animate().scaleX(0.94f).scaleY(0.94f).setDuration(220).start();
        }

        // Android Çekirdek Drag Motorunu Başlat
        boolean basari = SurukleBirakYardimcisi.suruklemeyiBaslat(kaynakView, veri);
        if (!basari) {
            suruklemeBitti();
        }
        return basari;
    }

    /**
     * Sürükleme oturumu bittiğinde sistemi NORMAL moda döndürür
     */
    public void suruklemeBitti() {
        this.mevcutDurum = LauncherDurumu.NORMAL;
        this.aktifVeri = null;

        // Masaüstü Ölçeğini Normale Getir
        if (workspace != null && Build.VERSION.SDK_INT >= 11) {
            workspace.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }

    /**
     * Boş alan menüsü açılabilir mi? (Tüm çakışmaları sıfırlayan katı kural motoru)
     */
    public boolean bosAlanMenusuAcilabilirMi(float rawX, float rawY) {
        // Kural 1: Sürükleme yapılıyorsa ASLA menü açılamaz
        if (isSurukleniyor()) return false;

        // Kural 2: Açık bir Nova popup veya klasör penceresi varsa menü açılmaz
        if (HizliEylemlerDiyalogu.aktifPopupAcikMi() || KlasorPenceresi.acikMi()) return false;

        // Kural 3: Dokunulan nokta Alt Bar (Dock) sınırları içindeyse ASLA açılamaz
        if (dockAlani != null && dockAlani.getVisibility() == View.VISIBLE) {
            Rect dockSinir = new Rect();
            dockAlani.getGlobalVisibleRect(dockSinir);
            if (dockSinir.contains((int) rawX, (int) rawY)) {
                return false;
            }
        }

        // Kural 4: Dokunulan nokta ekranın en üstündeki arama çubuğu veya üst bölgedeyse açılamaz
        if (rawY < ArayuzYardimcisi.dpToPx(aktivite, 180)) {
            // Üst bilgi ve arama çubuğu alanı
            return false;
        }

        return true;
    }

    /**
     * Hedef Dinleyicilerini Kurar (Hiçbir hedef diğerini yutamaz)
     */
    private void hedefDinleyicileriniKur() {
        if (Build.VERSION.SDK_INT < 11) return;

        // --- 1. HEDEF: ALT BAR (DOCK) ALANI ---
        if (dockAlani != null) {
            dockAlani.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    switch (event.getAction()) {
                        case DragEvent.ACTION_DRAG_STARTED:
                            return true;

                        case DragEvent.ACTION_DROP:
                            long simdi = System.currentTimeMillis();
                            if (simdi - sonBirakmaZamani < 300) return true;
                            sonBirakmaZamani = simdi;

                            SuruklemeVerisi veri = cozumleVeri(event);
                            try {
                                if (ArayuzYardimcisi.kilitliMiVeUyar(aktivite)) {
                                    return true;
                                }
                                if (veri != null && veri.app != null) {
                                    UygulamaBilgisi app = veri.app;
                                    if (app.paketAdi != null && app.paketAdi.startsWith("arac:serit")) {
                                        ArayuzYardimcisi.toast(aktivite, "Araç şeridi alt bara eklenemez.");
                                        return true;
                                    }
                                    SurukleBirakYardimcisi.titresim(aktivite, 40);

                                    int genislik = v.getWidth();
                                    int maxSlot = DockYoneticisi.getMaksimumDockSayisi(aktivite);
                                    int hedefPozisyon = -1;
                                    if (genislik > 0 && maxSlot > 0) {
                                        hedefPozisyon = (int) ((event.getX() / (float) genislik) * maxSlot);
                                        if (hedefPozisyon < 0) hedefPozisyon = 0;
                                        if (hedefPozisyon >= maxSlot) hedefPozisyon = maxSlot - 1;
                                    }

                                    MasaustuModelDeposu.dockaOgeTasi(aktivite, veri, hedefPozisyon);
                                    if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) {
                                        ArayuzYardimcisi.toast(aktivite, app.etiket + " alt bara taşındı.");
                                        if (dinleyici != null) {
                                            dinleyici.onMasaustuGuncelle();
                                            dinleyici.onDockGuncelle();
                                        }
                                    } else if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_CEKMECE) {
                                        ArayuzYardimcisi.toast(aktivite, app.etiket + " alt bara eklendi.");
                                        if (dinleyici != null) dinleyici.onDockGuncelle();
                                    } else if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
                                        ArayuzYardimcisi.toast(aktivite, app.etiket + " konumu güncellendi.");
                                        if (dinleyici != null) dinleyici.onDockGuncelle();
                                    } else if (veri.kaynakTuru == SuruklemeVerisi.KAYNAK_KLASOR) {
                                        ArayuzYardimcisi.toast(aktivite, app.etiket + " alt bara taşındı.");
                                        if (dinleyici != null) {
                                            dinleyici.onMasaustuGuncelle();
                                            dinleyici.onDockGuncelle();
                                        }
                                    }
                                }
                            } finally {
                                suruklemeBitti();
                            }
                            return true;

                        case DragEvent.ACTION_DRAG_ENDED:
                            suruklemeBitti();
                            return true;
                    }
                    return false;
                }
            });
        }

        // --- 4. HEDEF: MASAÜSTÜ ÇALIŞMA ALANI (WORKSPACE) ---
        if (workspace != null) {
            workspace.setOnDragListener(new View.OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    switch (event.getAction()) {
                        case DragEvent.ACTION_DRAG_STARTED:
                            return true;

                        case DragEvent.ACTION_DRAG_LOCATION:
                            // Kenarlara gelince Nova tarzı otomatik sayfa çevirme
                            long suan = System.currentTimeMillis();
                            if (suan - sonSayfaDegisimZamani > 650) {
                                float x = event.getX();
                                int genislik = workspace.getWidth();
                                int esik = ArayuzYardimcisi.dpToPx(aktivite, 45);
                                if (x < esik) {
                                    workspace.oncekiSayfa();
                                    sonSayfaDegisimZamani = suan;
                                    SurukleBirakYardimcisi.titresim(aktivite, 20);
                                } else if (x > genislik - esik) {
                                    workspace.sonrakiSayfa();
                                    sonSayfaDegisimZamani = suan;
                                    SurukleBirakYardimcisi.titresim(aktivite, 20);
                                }
                            }
                            return true;

                        case DragEvent.ACTION_DROP:
                            long simdi = System.currentTimeMillis();
                            if (simdi - sonBirakmaZamani < 300) return true;
                            sonBirakmaZamani = simdi;

                            SuruklemeVerisi veri = cozumleVeri(event);
                            try {
                                if (ArayuzYardimcisi.kilitliMiVeUyar(aktivite)) {
                                    return true;
                                }
                                if (veri != null && veri.app != null) {
                                    int[] wLoc = new int[2];
                                    v.getLocationOnScreen(wLoc);
                                    float screenX = wLoc[0] + event.getX();
                                    float screenY = wLoc[1] + event.getY();

                                    boolean islendi = false;
                                    if (dinleyici != null) {
                                        islendi = dinleyici.onMasaustuneBirakildi(veri, screenX, screenY);
                                    }
                                    if (!islendi && veri.kaynakTuru == SuruklemeVerisi.KAYNAK_CEKMECE) {
                                        int aktifSayfa = workspace.getAktifSayfa();
                                        MasaustuYoneticisi.sayfayaUygulamaEkle(aktivite, aktifSayfa, veri.app.getBilesenKimligi());
                                        if (dinleyici != null) dinleyici.onMasaustuGuncelle();
                                    }
                                }
                            } finally {
                                suruklemeBitti();
                            }
                            return true;

                        case DragEvent.ACTION_DRAG_ENDED:
                            suruklemeBitti();
                            return true;
                    }
                    return false;
                }
            });
        }
    }

    private SuruklemeVerisi cozumleVeri(DragEvent event) {
        if (event == null) return aktifVeri;
        Object raw = event.getLocalState();
        if (raw instanceof SuruklemeVerisi) {
            return (SuruklemeVerisi) raw;
        } else if (raw instanceof UygulamaBilgisi) {
            return new SuruklemeVerisi((UygulamaBilgisi) raw, SuruklemeVerisi.KAYNAK_CEKMECE);
        }
        return aktifVeri;
    }
}
