package com.tamekran.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Merkezi Uygulama Yükleme Motoru (App Loader SSOT)
 * Cihazdaki yüklü uygulamaları tek noktadan sorgular, SimgeOnbellegi ile entegre eder,
 * gizli uygulamaları filtreler ve Türkçe alfabetik/özel sıralama ile sunar (AOSP Launcher3 / Nova Modeli).
 */
public final class UygulamaYukleyici {

    private UygulamaYukleyici() {}

    /**
     * Cihazdaki tüm başlatılabilir uygulamaları SimgeOnbellegi kullanarak yükler.
     *
     * @param context Bağlam
     * @param gizlileriFiltrele True ise YedeklemeYoneticisi'ndeki gizli paketler elenir.
     * @param simgeMotoru İsteğe bağlı SimgePaketiYoneticisi (null verilebilir).
     * @return Hazır UygulamaBilgisi listesi
     */
    public static List<UygulamaBilgisi> yukle(Context context, boolean gizlileriFiltrele, SimgePaketiYoneticisi simgeMotoru) {
        if (context == null) return new ArrayList<UygulamaBilgisi>();

        PackageManager pm = context.getPackageManager();
        Intent anaNiyet = new Intent(Intent.ACTION_MAIN, null);
        anaNiyet.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> yayinlananlar = null;
        try {
            yayinlananlar = pm.queryIntentActivities(anaNiyet, 0);
        } catch (Throwable t) {
            LauncherLog.w("UygulamaYukleyici", "queryIntentActivities hatasi", t);
        }

        List<UygulamaBilgisi> sonuc = new ArrayList<UygulamaBilgisi>();
        if (yayinlananlar == null) return sonuc;

        String kendiPaketimiz = context.getPackageName();
        List<String> gizliListesi = gizlileriFiltrele ? YedeklemeYoneticisi.gizlenenleriAl(context) : null;

        for (ResolveInfo info : yayinlananlar) {
            if (info == null || info.activityInfo == null) continue;
            String paket = info.activityInfo.packageName;
            if (kendiPaketimiz.equals(paket)) continue;
            if (gizliListesi != null && gizliListesi.contains(paket)) continue;

            SimgeOnbellegi.OnbellekOgesi oge = SimgeOnbellegi.getirVeyaYukle(context, pm, info, simgeMotoru);
            if (oge != null) {
                sonuc.add(new UygulamaBilgisi(oge.etiket, paket, info.activityInfo.name, oge.simge));
            }
        }

        CekmeceSiralamasi.sirala(context, sonuc);
        return sonuc;
    }

    /**
     * Tüm uygulamaları yükler (gizli olanlar dahil, simge paketi varsayılan).
     */
    public static List<UygulamaBilgisi> tumunuYukle(Context context) {
        return yukle(context, false, null);
    }
}
