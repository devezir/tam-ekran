package com.tamekran.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tam Ekran - Masaüstü Veri Bütünlüğü ve Kendi Kendini Onarma Motoru (Self-Healing Engine)
 * AOSP Launcher3 GridOccupancy ve DB Cleanup mekanizmaları referans alınmıştır.
 * Izgara sınırlarını aşan, birbirinin üzerine binen veya bozulmuş masaüstü ögelerini sessizce onarır.
 */
public class MasaustuButunlukDenetleyici {

    private static final String TAG = "TamEkran_Butunluk";

    public static class OnarimSonucu {
        public int toplamOge = 0;
        public int onarilanOge = 0;
        public int kaldirilanGecersizOge = 0;
        public boolean degisiklikYapildi = false;

        @Override
        public String toString() {
            return "OnarimSonucu{toplam=" + toplamOge + ", onarilan=" + onarilanOge
                    + ", kaldirilan=" + kaldirilanGecersizOge + ", degisti=" + degisiklikYapildi + "}";
        }
    }

    /**
     * Saf Java Seviyesinde Hücre Çakışmalarını ve Sınır Aşımlarını Çözer (Birim Test Edilebilir)
     */
    public static OnarimSonucu hucreCakismalariniOnar(List<MasaustuYoneticisi.MasaustuOgesi> ogeler, int sutunSayisi, int satirSayisi) {
        OnarimSonucu sonuc = new OnarimSonucu();
        if (ogeler == null || sutunSayisi <= 0 || satirSayisi <= 0) {
            return sonuc;
        }

        boolean[][] dolu = new boolean[sutunSayisi][satirSayisi];
        Iterator<MasaustuYoneticisi.MasaustuOgesi> it = ogeler.iterator();

        while (it.hasNext()) {
            MasaustuYoneticisi.MasaustuOgesi oge = it.next();
            sonuc.toplamOge++;

            // 1. Geçersiz / Boş Ögeleri Temizle
            if (oge == null || oge.paketAdi == null || oge.paketAdi.trim().length() == 0) {
                it.remove();
                sonuc.kaldirilanGecersizOge++;
                sonuc.degisiklikYapildi = true;
                continue;
            }

            boolean ogeDegisti = false;

            // 2. Boyutları Izgaraya Uyarla (Span Clamping)
            if (oge.spanX < 1) { oge.spanX = 1; ogeDegisti = true; }
            if (oge.spanY < 1) { oge.spanY = 1; ogeDegisti = true; }
            if (oge.spanX > sutunSayisi) { oge.spanX = sutunSayisi; ogeDegisti = true; }
            if (oge.spanY > satirSayisi) { oge.spanY = satirSayisi; ogeDegisti = true; }

            // 3. Sınır Kontrolü (Boundary Clamping)
            if (oge.cellX < 0) { oge.cellX = 0; ogeDegisti = true; }
            if (oge.cellY < 0) { oge.cellY = 0; ogeDegisti = true; }
            if (oge.cellX + oge.spanX > sutunSayisi) {
                oge.cellX = Math.max(0, sutunSayisi - oge.spanX);
                ogeDegisti = true;
            }
            if (oge.cellY + oge.spanY > satirSayisi) {
                oge.cellY = Math.max(0, satirSayisi - oge.spanY);
                ogeDegisti = true;
            }

            // 4. Çakışma Kontrolü (AABB Overlap Detection)
            boolean cakisiyor = false;
            for (int x = oge.cellX; x < oge.cellX + oge.spanX; x++) {
                for (int y = oge.cellY; y < oge.cellY + oge.spanY; y++) {
                    if (dolu[x][y]) {
                        cakisiyor = true;
                        break;
                    }
                }
                if (cakisiyor) break;
            }

            // 5. Çakışma Varsa İlk Boş Yere Taşı (Self-Healing Relocation)
            if (cakisiyor) {
                int[] bosYer = bosHucreBul(dolu, sutunSayisi, satirSayisi, oge.spanX, oge.spanY);
                if (bosYer != null) {
                    oge.cellX = bosYer[0];
                    oge.cellY = bosYer[1];
                    ogeDegisti = true;
                }
            }

            // 6. Hücreleri Dolu Olarak İşaretle
            for (int x = oge.cellX; x < Math.min(sutunSayisi, oge.cellX + oge.spanX); x++) {
                for (int y = oge.cellY; y < Math.min(satirSayisi, oge.cellY + oge.spanY); y++) {
                    dolu[x][y] = true;
                }
            }

            if (ogeDegisti) {
                sonuc.onarilanOge++;
                sonuc.degisiklikYapildi = true;
            }
        }

        return sonuc;
    }

    /**
     * Izgara üzerinde verilen boyut için ilk boş hücreyi bulur
     */
    private static int[] bosHucreBul(boolean[][] dolu, int sutunSayisi, int satirSayisi, int spanX, int spanY) {
        for (int y = 0; y <= satirSayisi - spanY; y++) {
            for (int x = 0; x <= sutunSayisi - spanX; x++) {
                boolean uygun = true;
                for (int dx = 0; dx < spanX; dx++) {
                    for (int dy = 0; dy < spanY; dy++) {
                        if (dolu[x + dx][y + dy]) {
                            uygun = false;
                            break;
                        }
                    }
                    if (!uygun) break;
                }
                if (uygun) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    /**
     * Bozulmuş veya tek ögeli klasörleri tespit eder ve otomatik çözer (Self-Healing Auto-Unfold)
     * - 0 ögeli klasörler: Masaüstünden ve docktan tamamen kaldırılır.
     * - 1 ögeli klasörler: Klasör çözülür, kalan tek uygulama klasörün bulunduğu hücreye yerleşir.
     */
    public static Set<String> getYukluPaketleriGetir(Context context) {
        Set<String> set = new HashSet<String>();
        if (context == null) return set;
        try {
            PackageManager pm = context.getPackageManager();
            // 1. Başlatıcı (Launcher) aktivitesi olan aktif paketler
            Intent anaNiyet = new Intent(Intent.ACTION_MAIN, null);
            anaNiyet.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> yayinlananlar = pm.queryIntentActivities(anaNiyet, 0);
            if (yayinlananlar != null) {
                for (ResolveInfo info : yayinlananlar) {
                    if (info != null && info.activityInfo != null && info.activityInfo.packageName != null) {
                        set.add(info.activityInfo.packageName);
                    }
                }
            }
            // 2. Devre dışı, askıya alınmış veya launcher aktivitesi olmayan kurulu paketleri koru (yanlış silmeyi önler)
            List<android.content.pm.ApplicationInfo> tumUygulamalar = pm.getInstalledApplications(0);
            if (tumUygulamalar != null) {
                for (android.content.pm.ApplicationInfo appInfo : tumUygulamalar) {
                    if (appInfo != null && appInfo.packageName != null) {
                        set.add(appInfo.packageName);
                    }
                }
            }
        } catch (Exception e) {
            LauncherLog.w("MasaustuButunlukDenetleyici", "getYukluPaketleriGetir hatasi", e);
        }
        return set;
    }

    public static boolean klasorleriDenetleVeOnar(
            List<KlasorYoneticisi.KlasorBilgisi> klasorler,
            Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> masaustuSayfalari,
            List<String> dockListesi
    ) {
        return klasorleriDenetleVeOnar(klasorler, masaustuSayfalari, dockListesi, null);
    }

    public static boolean klasorleriDenetleVeOnar(
            List<KlasorYoneticisi.KlasorBilgisi> klasorler,
            Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> masaustuSayfalari,
            List<String> dockListesi,
            Set<String> yukluPaketler
    ) {
        if (klasorler == null) return false;
        boolean degisti = false;
        Iterator<KlasorYoneticisi.KlasorBilgisi> it = klasorler.iterator();

        while (it.hasNext()) {
            KlasorYoneticisi.KlasorBilgisi k = it.next();
            if (k == null || k.id == null) continue;

            // 1. Yüklü olmayan hayalet paketleri temizle
            if (k.paketler != null && yukluPaketler != null && !yukluPaketler.isEmpty()) {
                for (int i = k.paketler.size() - 1; i >= 0; i--) {
                    String p = k.paketler.get(i);
                    if (p == null || p.trim().isEmpty()) {
                        k.paketler.remove(i);
                        degisti = true;
                        continue;
                    }
                    if (p.startsWith("kisayol:") || p.startsWith("arac:")) {
                        continue;
                    }
                    String safPaket = p.contains("/") ? p.split("/", 2)[0] : p;
                    if (!yukluPaketler.contains(safPaket)) {
                        k.paketler.remove(i);
                        degisti = true;
                    }
                }
            }

            // 2. Klasör içi mükerrer ögeleri ayıkla
            if (k.paketler != null && k.paketler.size() > 1) {
                List<String> tekilPaketler = new ArrayList<String>();
                for (String p : k.paketler) {
                    if (p == null || p.trim().isEmpty()) continue;
                    boolean varMi = false;
                    for (String t : tekilPaketler) {
                        if (DockYoneticisi.bilesenEslesiyorMu(t, p)) {
                            varMi = true;
                            break;
                        }
                    }
                    if (!varMi) {
                        tekilPaketler.add(p);
                    } else {
                        degisti = true;
                    }
                }
                k.paketler = tekilPaketler;
            }

            String klasorEtiketi = "klasor:" + k.id;
            int sayi = (k.paketler != null) ? k.paketler.size() : 0;

            if (sayi == 0) {
                // 0 ögeli boş klasör: Temizle
                it.remove();
                degisti = true;

                if (masaustuSayfalari != null) {
                    for (List<MasaustuYoneticisi.MasaustuOgesi> sayfaListesi : masaustuSayfalari.values()) {
                        if (sayfaListesi == null) continue;
                        for (int i = sayfaListesi.size() - 1; i >= 0; i--) {
                            if (klasorEtiketi.equals(sayfaListesi.get(i).paketAdi)) {
                                sayfaListesi.remove(i);
                            }
                        }
                    }
                }

                if (dockListesi != null) {
                    dockListesi.remove(klasorEtiketi);
                }
            } else if (sayi == 1) {
                // 1 ögeli klasör: Otomatik çöz (Auto-Unfold)
                String kalanPaket = k.paketler.get(0);
                it.remove();
                degisti = true;

                if (masaustuSayfalari != null) {
                    for (List<MasaustuYoneticisi.MasaustuOgesi> sayfaListesi : masaustuSayfalari.values()) {
                        if (sayfaListesi == null) continue;
                        for (MasaustuYoneticisi.MasaustuOgesi o : sayfaListesi) {
                            if (klasorEtiketi.equals(o.paketAdi)) {
                                o.paketAdi = kalanPaket;
                            }
                        }
                    }
                }

                if (dockListesi != null) {
                    int idx = dockListesi.indexOf(klasorEtiketi);
                    if (idx >= 0) {
                        dockListesi.set(idx, kalanPaket);
                    }
                }
            }
        }

        return degisti;
    }

    /**
     * Masaüstü sayfalarındaki mükerrer ögeleri ayıklar (Deduplication).
     * @param harita Masaüstü sayfalarının ögeleri
     * @param tumSayfalarTekillik true ise sayfalar arası da dahil olmak üzere katı tekillik zorlanır (Tek Katman / iOS modu)
     * @return Herhangi bir mükerrer öge silindiyse true döner
     */
    public static boolean mukerrerOgeleriAyikla(Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> harita, boolean tumSayfalarTekillik) {
        return mukerrerOgeleriAyikla(harita, tumSayfalarTekillik, null);
    }

    /**
     * Masaüstü sayfalarındaki mükerrer ögeleri ayıklar.
     * @param harita Masaüstü sayfalarının ögeleri
     * @param tumSayfalarTekillik true ise sayfalar arası da dahil olmak üzere tekillik zorlanır (Tek Katman / iOS modu)
     * @param hariciKimlikler Önceden var sayılan kimlikler (örn: klasör içindeki uygulamalar)
     * @return Herhangi bir mükerrer öge silindiyse true döner
     */
    public static boolean mukerrerOgeleriAyikla(
            Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> harita,
            boolean tumSayfalarTekillik,
            List<String> hariciKimlikler
    ) {
        if (harita == null || harita.isEmpty()) return false;
        boolean degisti = false;

        if (tumSayfalarTekillik) {
            // Tüm sayfalar genelinde katı tekillik (iOS / Tek Katman Standardı)
            List<String> gorulenKimlikler = new ArrayList<String>();
            if (hariciKimlikler != null) {
                for (String hk : hariciKimlikler) {
                    if (hk != null && !hk.trim().isEmpty()) {
                        gorulenKimlikler.add(hk);
                    }
                }
            }

            // Sayfaları sıralı tara (0, 1, 2...)
            List<Integer> sayfaNolari = new ArrayList<Integer>(harita.keySet());
            Collections.sort(sayfaNolari);

            for (Integer sayfaNo : sayfaNolari) {
                List<MasaustuYoneticisi.MasaustuOgesi> sayfaListesi = harita.get(sayfaNo);
                if (sayfaListesi == null) continue;

                Iterator<MasaustuYoneticisi.MasaustuOgesi> it = sayfaListesi.iterator();
                while (it.hasNext()) {
                    MasaustuYoneticisi.MasaustuOgesi oge = it.next();
                    if (oge == null || oge.paketAdi == null || oge.paketAdi.trim().isEmpty()) {
                        continue;
                    }

                    // Araçlar (widget'lar) birden fazla bulunabilir, ancak uygulamalar ve klasörler tekil olmalıdır
                    if (!oge.paketAdi.startsWith("arac:")) {
                        boolean mukerrer = false;
                        for (String gorulen : gorulenKimlikler) {
                            if (DockYoneticisi.bilesenEslesiyorMu(gorulen, oge.paketAdi)) {
                                mukerrer = true;
                                break;
                            }
                        }
                        if (mukerrer) {
                            it.remove();
                            degisti = true;
                        } else {
                            gorulenKimlikler.add(oge.paketAdi);
                        }
                    }
                }
            }
        } else {
            // Klasik mod: Her sayfa kendi içinde tekil olmalıdır (aynı sayfada aynı uygulama iki kez bulunamaz)
            for (List<MasaustuYoneticisi.MasaustuOgesi> sayfaListesi : harita.values()) {
                if (sayfaListesi == null) continue;
                List<String> sayfaGorulenKimlikler = new ArrayList<String>();

                Iterator<MasaustuYoneticisi.MasaustuOgesi> it = sayfaListesi.iterator();
                while (it.hasNext()) {
                    MasaustuYoneticisi.MasaustuOgesi oge = it.next();
                    if (oge == null || oge.paketAdi == null || oge.paketAdi.trim().isEmpty()) {
                        continue;
                    }

                    if (!oge.paketAdi.startsWith("arac:")) {
                        boolean mukerrer = false;
                        for (String gorulen : sayfaGorulenKimlikler) {
                            if (DockYoneticisi.bilesenEslesiyorMu(gorulen, oge.paketAdi)) {
                                mukerrer = true;
                                break;
                            }
                        }
                        if (mukerrer) {
                            it.remove();
                            degisti = true;
                        } else {
                            sayfaGorulenKimlikler.add(oge.paketAdi);
                        }
                    }
                }
            }
        }

        return degisti;
    }

    /**
     * Masaüstü veritabanını tarar, tüm sayfaları, ızgaraları ve klasörleri onarır
     */
    public static OnarimSonucu denetleVeOnar(Context context) {
        OnarimSonucu genelSonuc = new OnarimSonucu();
        if (context == null) return genelSonuc;

        try {
            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);

            Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> harita = MasaustuYoneticisi.sayfalariYukle(context);
            if (MasaustuYoneticisi.sonVeriBozukMu()) {
                LauncherLog.w("MasaustuButunlukDenetleyici", "Bozuk masaustu JSON tespit edildi; karantina modunda yazma durduruldu.");
                return genelSonuc;
            }
            boolean herhangiBirSayfaDegisti = false;

            // 1. Klasör Bütünlüğü ve Otomatik Çözülme (Auto-Unfold) Denetimi
            Set<String> yukluPaketler = getYukluPaketleriGetir(context);
            List<KlasorYoneticisi.KlasorBilgisi> tumKlasorler = KlasorYoneticisi.klasorleriYukle(context);
            List<String> dockListesi = DockYoneticisi.dockYukle(context);
            boolean klasorlerOnarildi = klasorleriDenetleVeOnar(tumKlasorler, harita, dockListesi, yukluPaketler);
            if (klasorlerOnarildi) {
                KlasorYoneticisi.klasorleriKaydet(context, tumKlasorler);
                DockYoneticisi.dockKaydet(context, dockListesi);
                herhangiBirSayfaDegisti = true;
            }

            // 1.b Masaüstü ve Dock'taki Hayalet (Kaldırılmış) Paketleri Temizle
            if (yukluPaketler != null && !yukluPaketler.isEmpty()) {
                for (List<MasaustuYoneticisi.MasaustuOgesi> sayfaListesi : harita.values()) {
                    if (sayfaListesi == null) continue;
                    for (int i = sayfaListesi.size() - 1; i >= 0; i--) {
                        MasaustuYoneticisi.MasaustuOgesi o = sayfaListesi.get(i);
                        if (o == null || o.paketAdi == null) continue;
                        if (o.paketAdi.startsWith("klasor:") || o.paketAdi.startsWith("kisayol:") || o.paketAdi.startsWith("arac:")) {
                            continue;
                        }
                        String safPaket = o.paketAdi.contains("/") ? o.paketAdi.split("/", 2)[0] : o.paketAdi;
                        if (!yukluPaketler.contains(safPaket)) {
                            sayfaListesi.remove(i);
                            herhangiBirSayfaDegisti = true;
                        }
                    }
                }
                if (dockListesi != null) {
                    boolean dockDegisti = false;
                    for (int i = dockListesi.size() - 1; i >= 0; i--) {
                        String p = dockListesi.get(i);
                        if (p == null || p.startsWith("klasor:") || p.startsWith("kisayol:") || p.startsWith("arac:")) {
                            continue;
                        }
                        String safPaket = p.contains("/") ? p.split("/", 2)[0] : p;
                        if (!yukluPaketler.contains(safPaket)) {
                            dockListesi.remove(i);
                            dockDegisti = true;
                            herhangiBirSayfaDegisti = true;
                        }
                    }
                    if (dockDegisti) {
                        DockYoneticisi.dockKaydet(context, dockListesi);
                    }
                }
            }

            // 2. Mükerrer Ögeleri Ayıkla (Deduplication)
            boolean tekKatman = MasaustuYoneticisi.isTekKatmanModu(context);
            List<String> klasordekiPaketler = new ArrayList<String>();
            if (tekKatman && tumKlasorler != null) {
                for (KlasorYoneticisi.KlasorBilgisi kb : tumKlasorler) {
                    if (kb != null && kb.paketler != null) {
                        klasordekiPaketler.addAll(kb.paketler);
                    }
                }
            }
            boolean mukerrerAyiklandi = mukerrerOgeleriAyikla(harita, tekKatman, klasordekiPaketler);
            if (mukerrerAyiklandi) {
                herhangiBirSayfaDegisti = true;
            }

            // 3. Hücre Matrisi ve Çakışma Denetimi
            for (Map.Entry<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> entry : harita.entrySet()) {
                List<MasaustuYoneticisi.MasaustuOgesi> sayfaListesi = entry.getValue();
                if (sayfaListesi == null || sayfaListesi.isEmpty()) continue;

                OnarimSonucu sayfaSonucu = hucreCakismalariniOnar(sayfaListesi, sutun, satir);
                genelSonuc.toplamOge += sayfaSonucu.toplamOge;
                genelSonuc.onarilanOge += sayfaSonucu.onarilanOge;
                genelSonuc.kaldirilanGecersizOge += sayfaSonucu.kaldirilanGecersizOge;

                if (sayfaSonucu.degisiklikYapildi) {
                    herhangiBirSayfaDegisti = true;
                }
            }

            if (herhangiBirSayfaDegisti) {
                genelSonuc.degisiklikYapildi = true;
                MasaustuYoneticisi.sayfalariKaydet(context, harita);
                Log.i(TAG, "Masaüstü veri bütünlüğü onarıldı: " + genelSonuc);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Masaüstü bütünlük denetimi sırasında hata: " + t.getMessage());
        }

        return genelSonuc;
    }
}
