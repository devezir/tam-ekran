package com.tamekran.launcher;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Kullanıcı Eylemi Geri Alma Motoru (Addy Osmani Kalite Kapısı)
 * Masaüstünden veya docktan kazara silinen simge, widget ve klasörlerin tek tıkla geri yüklenmesini sağlar.
 */
public class GeriAlYoneticisi {

    public static class SilinenOge {
        public int kaynakTuru; // SuruklemeVerisi.KAYNAK_MASAUSTU veya KAYNAK_DOCK
        public int kaynakSayfa;
        public String paketAdi;
        public String etiket;
        public int cellX;
        public int cellY;
        public int spanX = 1;
        public int spanY = 1;
        public long silinmeZamani;

        public String klasorAdi;
        public List<String> klasorPaketleri;

        public SilinenOge(int kaynakTuru, int kaynakSayfa, String paketAdi, String etiket, int cellX, int cellY, int spanX, int spanY) {
            this.kaynakTuru = kaynakTuru;
            this.kaynakSayfa = kaynakSayfa;
            this.paketAdi = paketAdi;
            this.etiket = etiket != null ? etiket : paketAdi;
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = Math.max(1, spanX);
            this.spanY = Math.max(1, spanY);
            this.silinmeZamani = System.currentTimeMillis();
        }
    }

    private static SilinenOge sonSilinen = null;

    public static void silineniKaydet(int kaynakTuru, int kaynakSayfa, String paketAdi, String etiket, int cellX, int cellY, int spanX, int spanY, String klasorAdi, List<String> klasorPaketleri) {
        if (paketAdi == null) return;
        SilinenOge oge = new SilinenOge(kaynakTuru, kaynakSayfa, paketAdi, etiket, cellX, cellY, spanX, spanY);
        if (klasorPaketleri != null) {
            oge.klasorAdi = klasorAdi;
            oge.klasorPaketleri = new ArrayList<String>(klasorPaketleri);
        }
        sonSilinen = oge;
    }

    public static SilinenOge getSonSilinen() {
        return sonSilinen;
    }

    public static void temizle() {
        sonSilinen = null;
    }

    public static boolean geriAlinabilirMi() {
        if (sonSilinen == null) return false;
        return (System.currentTimeMillis() - sonSilinen.silinmeZamani) <= 10000;
    }

    /**
     * Son silinen öğeyi eski yerine geri koyar
     */
    public static boolean geriAl(Context context) {
        if (context == null || sonSilinen == null || !geriAlinabilirMi()) {
            sonSilinen = null;
            return false;
        }

        SilinenOge oge = sonSilinen;
        sonSilinen = null; // Tüketildi

        try {
            return MasaustuModelDeposu.ogeGeriYukle(
                    context,
                    oge.kaynakTuru,
                    oge.kaynakSayfa,
                    oge.paketAdi,
                    oge.cellX,
                    oge.cellY,
                    oge.spanX,
                    oge.spanY,
                    oge.klasorAdi,
                    oge.klasorPaketleri
            );
        } catch (Throwable t) {
            LauncherLog.e("GeriAlYoneticisi", "Geri alma sırasında hata", t);
        }

        return false;
    }
}
