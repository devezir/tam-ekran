package com.tamekran.launcher;

/**
 * Tam Ekran - Sürükleme Oturumu Veri Modeli
 * Sürüklenen uygulamanın nereden geldiğini (Çekmece, Masaüstü Sayfa X, Alt Bar) takip eder.
 */
public class SuruklemeVerisi {

    public static final int KAYNAK_CEKMECE = 1;
    public static final int KAYNAK_MASAUSTU = 2;
    public static final int KAYNAK_DOCK = 3;
    public static final int KAYNAK_KLASOR = 4;

    public UygulamaBilgisi app;
    public String itemId;
    public int kaynakTuru;
    public int kaynakSayfa;
    public int kaynakPozisyon;
    public int kaynakCellX = -1;
    public int kaynakCellY = -1;
    public int spanX = 1;
    public int spanY = 1;
    public String kaynakKlasorId;

    public SuruklemeVerisi(UygulamaBilgisi app, int kaynakTuru) {
        this(app, kaynakTuru, 0, 0);
    }

    public SuruklemeVerisi(UygulamaBilgisi app, int kaynakTuru, int kaynakSayfa, int kaynakPozisyon) {
        this.app = app;
        this.kaynakTuru = kaynakTuru;
        this.kaynakSayfa = kaynakSayfa;
        this.kaynakPozisyon = kaynakPozisyon;
        this.spanX = 1;
        this.spanY = 1;
    }

    public SuruklemeVerisi(UygulamaBilgisi app, int kaynakTuru, int kaynakSayfa, int cellX, int cellY) {
        this(app, kaynakTuru, kaynakSayfa, cellX, cellY, 1, 1);
    }

    public SuruklemeVerisi(UygulamaBilgisi app, int kaynakTuru, int kaynakSayfa, int cellX, int cellY, int spanX, int spanY) {
        this.app = app;
        this.kaynakTuru = kaynakTuru;
        this.kaynakSayfa = kaynakSayfa;
        this.kaynakPozisyon = 0;
        this.kaynakCellX = cellX;
        this.kaynakCellY = cellY;
        this.spanX = Math.max(1, spanX);
        this.spanY = Math.max(1, spanY);
    }

    public SuruklemeVerisi(UygulamaBilgisi app, String kaynakKlasorId, int pozisyon) {
        this.app = app;
        this.kaynakTuru = KAYNAK_KLASOR;
        this.kaynakKlasorId = kaynakKlasorId;
        this.kaynakPozisyon = pozisyon;
        this.spanX = 1;
        this.spanY = 1;
    }

    public SuruklemeVerisi withItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }
}
