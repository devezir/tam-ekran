package com.tamekran.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tam Ekran - Bellek Dostu Simge ve Etiket Önbelleği (Icon & Label Cache)
 * AOSP Launcher3 IconCache mimarisinden esinlenilmiştir.
 * Sıfır harici kütüphane, saf Java.
 * Tekrarlayan PackageManager IPC çağrılarını ve kaynak çözümleme gecikmelerini önler.
 */
public class SimgeOnbellegi {

    public static final int MAX_ONBELLEK = 160;

    public static class OnbellekOgesi {
        public final String etiket;
        public final Drawable simge;

        public OnbellekOgesi(String etiket, Drawable simge) {
            this.etiket = etiket;
            this.simge = simge;
        }
    }

    private static final Object ONBELLEK_KILIDI = new Object();

    private static final Map<String, OnbellekOgesi> BELLEK_ONBELLEGI =
            new LinkedHashMap<String, OnbellekOgesi>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, OnbellekOgesi> eldest) {
                    return size() > MAX_ONBELLEK;
                }
            };

    /**
     * Bileşen için önbellekten etiket ve simgeyi getirir; yoksa PackageManager ve SimgePaketi üzerinden yükleyip önbelleğe alır.
     */
    public static OnbellekOgesi getirVeyaYukle(
            Context context,
            PackageManager pm,
            ResolveInfo info,
            SimgePaketiYoneticisi simgeMotoru
    ) {
        if (context == null || pm == null || info == null || info.activityInfo == null) {
            return null;
        }

        String paket = info.activityInfo.packageName;
        String activityName = info.activityInfo.name;
        String bilesenKimlik = (activityName != null && !activityName.isEmpty()) ? (paket + "/" + activityName) : paket;

        OnbellekOgesi mevcut = getir(bilesenKimlik);
        if (mevcut != null) {
            return mevcut;
        }

        // Önbellekte yok, sistemden yükle
        String orijinalEtiket;
        try {
            CharSequence cs = info.loadLabel(pm);
            orijinalEtiket = (cs != null) ? cs.toString() : paket;
        } catch (Throwable t) {
            orijinalEtiket = paket;
        }

        String sonEtiket = OzelEtiketYoneticisi.getEtiket(context, bilesenKimlik, orijinalEtiket);

        Drawable varsayilanSimge;
        try {
            varsayilanSimge = info.loadIcon(pm);
        } catch (Throwable t) {
            varsayilanSimge = context.getResources().getDrawable(R.drawable.simge_varsayilan);
        }

        Drawable sonSimge = varsayilanSimge;
        if (simgeMotoru != null) {
            sonSimge = simgeMotoru.simgeyiGetir(paket, activityName, varsayilanSimge);
        }

        // Simge şekil normalizasyonunu tek seferde uygula ve önbelleğe al
        if (sonSimge != null) {
            sonSimge = SimgeNormalizasyonYoneticisi.normalizeEt(context, sonSimge);
        }

        OnbellekOgesi yeni = new OnbellekOgesi(sonEtiket, sonSimge);
        onbellegeEkle(bilesenKimlik, yeni);
        return yeni;
    }

    /**
     * Yeniden adlandırılan uygulamanın bellekteki önbellek kaydını anında günceller.
     */
    public static void etiketGuncelle(String bilesenKimlik, String yeniEtiket) {
        if (bilesenKimlik == null) return;
        String safPaket = bilesenKimlik.contains("/") ? bilesenKimlik.split("/", 2)[0] : bilesenKimlik;
        synchronized (ONBELLEK_KILIDI) {
            for (Map.Entry<String, OnbellekOgesi> entry : BELLEK_ONBELLEGI.entrySet()) {
                String k = entry.getKey();
                if (k.equals(bilesenKimlik) || DockYoneticisi.bilesenEslesiyorMu(k, safPaket)) {
                    OnbellekOgesi eski = entry.getValue();
                    entry.setValue(new OnbellekOgesi(yeniEtiket, eski != null ? eski.simge : null));
                }
            }
        }
    }

    /**
     * Belirli bir paketin önbelleğini temizler (Uygulama güncellendiğinde veya silindiğinde)
     */
    public static void paketTemizle(String paketAdi) {
        if (paketAdi == null) return;
        synchronized (ONBELLEK_KILIDI) {
            Iterator<Map.Entry<String, OnbellekOgesi>> it = BELLEK_ONBELLEGI.entrySet().iterator();
            while (it.hasNext()) {
                String anahtar = it.next().getKey();
                if (DockYoneticisi.bilesenEslesiyorMu(anahtar, paketAdi)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Tüm simge önbelleğini boşaltır (Simge paketi veya şekli değiştiğinde)
     */
    public static void tumunuTemizle() {
        synchronized (ONBELLEK_KILIDI) {
            BELLEK_ONBELLEGI.clear();
        }
    }

    public static void onbellegeEkle(String bilesenKimlik, OnbellekOgesi oge) {
        if (bilesenKimlik != null && oge != null) {
            synchronized (ONBELLEK_KILIDI) {
                BELLEK_ONBELLEGI.put(bilesenKimlik, oge);
            }
        }
    }

    public static OnbellekOgesi getir(String bilesenKimlik) {
        if (bilesenKimlik == null) return null;
        synchronized (ONBELLEK_KILIDI) {
            return BELLEK_ONBELLEGI.get(bilesenKimlik);
        }
    }

    public static int getOnbellekBoyutu() {
        synchronized (ONBELLEK_KILIDI) {
            return BELLEK_ONBELLEGI.size();
        }
    }
}
