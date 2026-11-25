package com.tamekran.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tam Ekran - Simge Paketi (Icon Pack) Motoru
 * Nova Launcher, ADW, Apex ve Go Launcher uyumlu tüm simge paketlerini
 * hiçbir harici kütüphane olmadan, saf XmlPullParser ile ayrıştırır.
 * Android 2.2'den Android 17'ye kadar tüm sürümlerde çalışır.
 */
public final class SimgePaketiYoneticisi {

    public static final String ANAHTAR_AKTIF_PAKET = "aktif_simge_paketi";
    private static final String[] TEMA_NIYETLERI = new String[]{
        "com.novalauncher.THEME",
        "org.adw.launcher.THEMES",
        "com.fede.launcher.THEME_ICONPACK",
        "com.anddoes.launcher.THEME"
    };

    private Context baglam;
    private String aktifPaketAdi;
    private Resources paketKaynaklari;
    private Map<String, String> simgeHaritasi = new HashMap<String, String>();

    // Özel simge atamaları için bellek içi ayrıştırılmış appfilter önbelleği
    private static final Map<String, Map<String, String>> ozelPaketHaritaOnbellegi = new HashMap<String, Map<String, String>>();

    public static void onbellegiTemizle() {
        ozelPaketHaritaOnbellegi.clear();
    }

    public static class SimgePaketiBilgisi {
        public String paketAdi;
        public String baslik;
        public Drawable simge;

        public SimgePaketiBilgisi(String paketAdi, String baslik, Drawable simge) {
            this.paketAdi = paketAdi;
            this.baslik = baslik;
            this.simge = simge;
        }
    }

    public SimgePaketiYoneticisi(Context baglam) {
        this.baglam = baglam;
        tazele();
    }

    public void tazele() {
        SharedPreferences pref = AyarBankasi.getAyarlar(baglam);
        this.aktifPaketAdi = pref.getString(ANAHTAR_AKTIF_PAKET, "");
        this.simgeHaritasi.clear();
        this.paketKaynaklari = null;
        onbellegiTemizle();

        if (aktifPaketAdi != null && aktifPaketAdi.length() > 0) {
            haritayiYukle(aktifPaketAdi);
        }
    }

    /**
     * Telefonda kurulu Nova/ADW uyumlu tüm simge paketlerini bulur.
     */
    public List<SimgePaketiBilgisi> yukluPaketleriBul() {
        PackageManager pm = baglam.getPackageManager();
        List<SimgePaketiBilgisi> sonuc = new ArrayList<SimgePaketiBilgisi>();
        Map<String, Boolean> bulunanlar = new HashMap<String, Boolean>();

        for (String aksiyon : TEMA_NIYETLERI) {
            Intent niyet = new Intent(aksiyon);
            List<ResolveInfo> paketler = pm.queryIntentActivities(niyet, 0);
            if (paketler != null) {
                for (ResolveInfo info : paketler) {
                    String pName = info.activityInfo.packageName;
                    if (!bulunanlar.containsKey(pName)) {
                        bulunanlar.put(pName, true);
                        String baslik = info.loadLabel(pm).toString();
                        Drawable simge = info.loadIcon(pm);
                        sonuc.add(new SimgePaketiBilgisi(pName, baslik, simge));
                    }
                }
            }
        }
        return sonuc;
    }

    private void haritayiYukle(String paketAdi) {
        try {
            PackageManager pm = baglam.getPackageManager();
            this.paketKaynaklari = pm.getResourcesForApplication(paketAdi);

            int resId = paketKaynaklari.getIdentifier("appfilter", "xml", paketAdi);
            if (resId == 0) return;

            XmlPullParser parser = paketKaynaklari.getXml(resId);
            int olayTuru = parser.getEventType();

            while (olayTuru != XmlPullParser.END_DOCUMENT) {
                if (olayTuru == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                    String component = parser.getAttributeValue(null, "component");
                    String drawable = parser.getAttributeValue(null, "drawable");

                    if (component != null && drawable != null) {
                        String temiz = component.replace("ComponentInfo{", "").replace("}", "").trim();
                        simgeHaritasi.put(temiz, drawable);
                    }
                }
                olayTuru = parser.next();
            }
        } catch (Throwable t) {
            LauncherLog.w("SimgePaketiYoneticisi", "appfilter.xml parse hatası", t);
        }
    }

    /**
     * Uygulama için temalı simgeyi döner.
     * 1. Bireysel özel simge atamasını kontrol eder
     * 2. Aktif genel tema paketini kontrol eder
     * 3. Takvim ise dinamik gün tarihini basar
     */
    public Drawable simgeyiGetir(String paketAdi, String etkinlikAdi, Drawable varsayilan) {
        Drawable sonSimge = varsayilan;

        // 1. Bireysel özel simge paketi var mı?
        String ozelPaket = OzelSimgeYoneticisi.getOzelPaket(baglam, paketAdi);
        if (ozelPaket != null && ozelPaket.length() > 0) {
            Drawable ozelSimge = pakettenSimgeCek(ozelPaket, paketAdi, etkinlikAdi);
            if (ozelSimge != null) {
                sonSimge = ozelSimge;
            }
        } else if (paketKaynaklari != null && !simgeHaritasi.isEmpty()) {
            // 2. Genel aktif simge paketi
            String anahtar = paketAdi + "/" + etkinlikAdi;
            String cizilebilirAd = simgeHaritasi.get(anahtar);
            if (cizilebilirAd == null) {
                cizilebilirAd = simgeHaritasi.get(paketAdi);
            }

            if (cizilebilirAd != null) {
                try {
                    int resId = paketKaynaklari.getIdentifier(cizilebilirAd, "drawable", aktifPaketAdi);
                    if (resId != 0) {
                        sonSimge = paketKaynaklari.getDrawable(resId);
                    }
                } catch (Throwable t) {
                    LauncherLog.d("SimgePaketiYoneticisi", "Tema simgesi yuklenemedi: " + t.getMessage());
                }
            }
        }

        // 3. Dinamik Takvim Tarih Simgesi
        if (DinamikSimgeYoneticisi.takvimUygulamasiMi(paketAdi)) {
            sonSimge = DinamikSimgeYoneticisi.dinamikTakvimSimgesi(baglam, sonSimge);
        }

        return sonSimge;
    }

    private Drawable pakettenSimgeCek(String paket, String hedefPaket, String etkinlik) {
        if (paket == null || hedefPaket == null) return null;
        try {
            PackageManager pm = baglam.getPackageManager();
            Resources res = pm.getResourcesForApplication(paket);

            Map<String, String> harita = ozelPaketHaritaOnbellegi.get(paket);
            if (harita == null) {
                harita = new HashMap<String, String>();
                int resId = res.getIdentifier("appfilter", "xml", paket);
                if (resId != 0) {
                    XmlPullParser parser = res.getXml(resId);
                    int olayTuru = parser.getEventType();
                    while (olayTuru != XmlPullParser.END_DOCUMENT) {
                        if (olayTuru == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                            String comp = parser.getAttributeValue(null, "component");
                            String drw = parser.getAttributeValue(null, "drawable");
                            if (comp != null && drw != null) {
                                String t = comp.replace("ComponentInfo{", "").replace("}", "").trim();
                                harita.put(t, drw);
                                int slashIdx = t.indexOf('/');
                                if (slashIdx > 0) {
                                    String pOnly = t.substring(0, slashIdx);
                                    if (!harita.containsKey(pOnly)) {
                                        harita.put(pOnly, drw);
                                    }
                                }
                            }
                        }
                        olayTuru = parser.next();
                    }
                }
                ozelPaketHaritaOnbellegi.put(paket, harita);
            }

            String cizilebilir = harita.get(hedefPaket + "/" + etkinlik);
            if (cizilebilir == null) {
                cizilebilir = harita.get(hedefPaket);
            }

            if (cizilebilir != null) {
                int simgeResId = res.getIdentifier(cizilebilir, "drawable", paket);
                if (simgeResId != 0) {
                    return res.getDrawable(simgeResId);
                }
            }
        } catch (Throwable t) {
            LauncherLog.d("SimgePaketiYoneticisi", "pakettenSimgeCek basarisiz: " + t.getMessage());
        }
        return null;
    }

    public String getAktifPaketAdi() {
        return aktifPaketAdi;
    }
}

