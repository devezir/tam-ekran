package com.tamekran.launcher;

/**
 * Tam Ekran - Başlatıcı Durum Makinesi Sabitleri (LauncherState)
 * Uygulamanın aktif arayüz durumlarını tek merkezden tanımlar.
 */
public final class LauncherDurumu {
    public static final int NORMAL = 0;      // Normal masaüstü gezinme modu
    public static final int CEKMECE = 1;     // Tüm uygulamalar çekmecesi açık
    public static final int ARAMA = 2;       // Çekmece içi anlık arama / T9 filtresi aktif
    public static final int KLASOR = 3;      // Masaüstü veya dock klasör penceresi açık
    public static final int SURUKLEME = 4;   // Bir simge/arac havalandı ve sürükleniyor

    private LauncherDurumu() {}
}
