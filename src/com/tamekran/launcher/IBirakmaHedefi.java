package com.tamekran.launcher;

/**
 * Endüstri Standardı Bırakma Hedefi Arayüzü (DropTarget)
 * Masaüstü sayfaları, Alt Bar (Dock), Çöp Kutusu ve Düzenleme Çubuğu bu arayüzü uygular.
 */
public interface IBirakmaHedefi {

    /**
     * Bu hedef sürüklenen veriyi kabul ediyor mu?
     */
    boolean isBirakmaKabulEdiyor(SuruklemeVerisi veri);

    /**
     * Sürüklenen nesne hedefin sınırlarına girdiğinde çağrılır.
     */
    void onSuruklemeGirdi(SuruklemeVerisi veri);

    /**
     * Sürüklenen nesne hedef üzerinde gezinirken çağrılır.
     */
    void onSuruklemeKonumu(SuruklemeVerisi veri, float x, float y);

    /**
     * Sürüklenen nesne hedefin sınırlarından çıktığında çağrılır.
     */
    void onSuruklemeCikti(SuruklemeVerisi veri);

    /**
     * Sürüklenen nesne bu hedefin üzerine bırakıldığında çağrılır.
     * @return true ise bırakma başarıyla işlenmiştir, false ise iptal.
     */
    boolean onBirakmaTamamlandi(SuruklemeVerisi veri, float x, float y);
}
