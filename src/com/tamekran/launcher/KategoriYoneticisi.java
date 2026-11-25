package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tam Ekran - Menü Sekmeleri ve Akıllı Kategori Motoru (Nova Prime Drawer Groups)
 * Uygulamaları otomatik veya manuel olarak kategorilere ayırır.
 */
public class KategoriYoneticisi {

    public static final String KAT_TUMU = "Tümü";
    public static final String KAT_ILETISIM = "İletişim & Sosyal";
    public static final String KAT_ARACLAR = "Araçlar & Sistem";
    public static final String KAT_MEDYA = "Medya & Müzik";
    public static final String KAT_OYUNLAR = "Oyunlar";

    private static final String PREF_OZEL_KATEGORILER = "ozel_kategoriler_json";
    private static final String PREF_UYGULAMA_KATEGORILERI = "uygulama_kategori_atamalari_json";

    public static List<String> getKategoriler(Context context) {
        List<String> liste = new ArrayList<String>();
        liste.add(KAT_TUMU);
        liste.add(KAT_ILETISIM);
        liste.add(KAT_ARACLAR);
        liste.add(KAT_MEDYA);
        liste.add(KAT_OYUNLAR);

        List<String> ozel = AyarBankasi.getStringListesi(context, PREF_OZEL_KATEGORILER);
        for (String kat : ozel) {
            if (!liste.contains(kat)) {
                liste.add(kat);
            }
        }
        return liste;
    }

    public static void kategoriEkle(Context context, String yeniKategori) {
        if (yeniKategori == null || yeniKategori.trim().length() == 0) return;
        List<String> ozel = AyarBankasi.getStringListesi(context, PREF_OZEL_KATEGORILER);
        String temiz = yeniKategori.trim();
        if (!ozel.contains(temiz)) {
            ozel.add(temiz);
            AyarBankasi.putStringListesi(context, PREF_OZEL_KATEGORILER, ozel);
        }
    }

    public static void kategoriSil(Context context, String silinecekKategori) {
        if (context == null || silinecekKategori == null) return;
        if (silinecekKategori.equals(KAT_TUMU)) return;

        List<String> ozel = AyarBankasi.getStringListesi(context, PREF_OZEL_KATEGORILER);
        ozel.remove(silinecekKategori);
        AyarBankasi.putStringListesi(context, PREF_OZEL_KATEGORILER, ozel);

        // Bu kategoriye atanmış uygulamaları sıfırla
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        if (sp == null) return;
        try {
            String hamAppJson = sp.getString(PREF_UYGULAMA_KATEGORILERI, "{}");
            JSONObject obj = new JSONObject(hamAppJson);
            java.util.Iterator<String> keys = obj.keys();
            List<String> silinecekAnahtarlar = new ArrayList<String>();
            while (keys.hasNext()) {
                String key = keys.next();
                if (silinecekKategori.equals(obj.optString(key))) {
                    silinecekAnahtarlar.add(key);
                }
            }
            for (String k : silinecekAnahtarlar) {
                obj.remove(k);
            }
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_UYGULAMA_KATEGORILERI, obj.toString()));
        } catch (Throwable t) {
            LauncherLog.w("KategoriYoneticisi", "kategoriSil uygulama JSON hatası", t);
        }
    }

    public static void kategoriYenidenAdlandir(Context context, String eskiAd, String yeniAd) {
        if (context == null || eskiAd == null || yeniAd == null || yeniAd.trim().length() == 0) return;
        if (eskiAd.equals(KAT_TUMU)) return;

        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String hamJson = sp.getString(PREF_OZEL_KATEGORILER, "[]");
        try {
            JSONArray arr = new JSONArray(hamJson);
            JSONArray yeniArr = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                String kat = arr.getString(i);
                if (kat.equals(eskiAd)) {
                    yeniArr.put(yeniAd.trim());
                } else {
                    yeniArr.put(kat);
                }
            }
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_OZEL_KATEGORILER, yeniArr.toString()));
        } catch (Throwable t) {
            LauncherLog.w("KategoriYoneticisi", "kategoriYenidenAdlandir JSON hatası", t);
        }

        // Uygulama atamalarını güncelle
        String hamAppJson = sp.getString(PREF_UYGULAMA_KATEGORILERI, "{}");
        try {
            JSONObject obj = new JSONObject(hamAppJson);
            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (eskiAd.equals(obj.optString(key))) {
                    obj.put(key, yeniAd.trim());
                }
            }
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_UYGULAMA_KATEGORILERI, obj.toString()));
        } catch (Throwable t) {
            LauncherLog.w("KategoriYoneticisi", "kategoriYenidenAdlandir uygulama JSON hatası", t);
        }
    }

    public static void kategoriUygulamalariAta(Context context, String kategori, List<String> paketAdlari) {
        if (context == null || kategori == null || paketAdlari == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String hamAppJson = sp.getString(PREF_UYGULAMA_KATEGORILERI, "{}");
        try {
            JSONObject obj = new JSONObject(hamAppJson);
            // Önce bu kategorideki mevcut atamaları temizle
            java.util.Iterator<String> keys = obj.keys();
            List<String> temizlenecek = new ArrayList<String>();
            while (keys.hasNext()) {
                String key = keys.next();
                if (kategori.equals(obj.optString(key))) {
                    temizlenecek.add(key);
                }
            }
            for (String k : temizlenecek) {
                obj.remove(k);
            }

            // Yeni seçilenleri ata
            for (String pkg : paketAdlari) {
                obj.put(pkg, kategori);
            }
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_UYGULAMA_KATEGORILERI, obj.toString()));
        } catch (Throwable t) {
            LauncherLog.w("KategoriYoneticisi", "kategoriUygulamalariAta hatası", t);
        }
    }

    public static int getKategoriUygulamaSayisi(Context context, String kategori, List<UygulamaBilgisi> tumUygulamalar) {
        if (context == null || kategori == null || tumUygulamalar == null) return 0;
        if (kategori.equals(KAT_TUMU)) return tumUygulamalar.size();

        int sayi = 0;
        for (UygulamaBilgisi app : tumUygulamalar) {
            if (kategori.equals(getKategori(context, app))) {
                sayi++;
            }
        }
        return sayi;
    }

    public static void kategoriAta(Context context, String paketAdi, String kategori) {
        if (paketAdi == null || kategori == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String hamJson = sp.getString(PREF_UYGULAMA_KATEGORILERI, "{}");
        try {
            JSONObject obj = new JSONObject(hamJson);
            obj.put(paketAdi, kategori);
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_UYGULAMA_KATEGORILERI, obj.toString()));
        } catch (Throwable t) {
            LauncherLog.w("KategoriYoneticisi", "kategoriAta hatası", t);
        }
    }

    public static String getKategori(Context context, UygulamaBilgisi app) {
        if (app == null) return KAT_ARACLAR;

        // 1. Kullanıcının el ile atadığı kategori kontrolü
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String hamJson = sp.getString(PREF_UYGULAMA_KATEGORILERI, "{}");
        try {
            JSONObject obj = new JSONObject(hamJson);
            if (obj.has(app.paketAdi)) {
                return obj.getString(app.paketAdi);
            }
        } catch (Throwable t) {
            LauncherLog.d("KategoriYoneticisi", "getKategori JSON hatası: " + t.getMessage());
        }

        // 2. Android 8.0+ Resmi Kategori Tespiti
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                ApplicationInfo info = context.getPackageManager().getApplicationInfo(app.paketAdi, 0);
                if (info.category == ApplicationInfo.CATEGORY_GAME) {
                    return KAT_OYUNLAR;
                } else if (info.category == ApplicationInfo.CATEGORY_AUDIO ||
                           info.category == ApplicationInfo.CATEGORY_VIDEO ||
                           info.category == ApplicationInfo.CATEGORY_IMAGE) {
                    return KAT_MEDYA;
                } else if (info.category == ApplicationInfo.CATEGORY_SOCIAL ||
                           info.category == ApplicationInfo.CATEGORY_NEWS) {
                    return KAT_ILETISIM;
                } else if (info.category == ApplicationInfo.CATEGORY_MAPS ||
                           info.category == ApplicationInfo.CATEGORY_PRODUCTIVITY) {
                    return KAT_ARACLAR;
                }
            } catch (Throwable t) {
                LauncherLog.d("KategoriYoneticisi", "Resmi kategori alinamadi: " + t.getMessage());
            }
        }

        // 3. Akıllı Heuristic Kural Tespiti (Android 2.2 - Android 17 evrensel)
        String p = app.paketAdi.toLowerCase(Locale.US);
        String e = app.etiket != null ? app.etiket.toLowerCase(new Locale("tr", "TR")) : "";

        if (p.contains("whatsapp") || p.contains("telegram") || p.contains("instagram") ||
            p.contains("twitter") || p.contains("facebook") || p.contains("discord") ||
            p.contains("signal") || p.contains("viber") || p.contains("contact") ||
            p.contains("dialer") || p.contains("telecom") || p.contains("mms") ||
            p.contains("message") || p.contains("mail") || p.contains("outlook") ||
            e.contains("whatsapp") || e.contains("mesaj") || e.contains("telefon") ||
            e.contains("kişiler") || e.contains("rehber") || e.contains("posta")) {
            return KAT_ILETISIM;
        }

        if (p.contains("youtube") || p.contains("spotify") || p.contains("music") ||
            p.contains("video") || p.contains("audio") || p.contains("player") ||
            p.contains("gallery") || p.contains("galeri") || p.contains("camera") ||
            p.contains("kamera") || p.contains("photo") || p.contains("netflix") ||
            e.contains("müzik") || e.contains("kamera") || e.contains("galeri") ||
            e.contains("fotoğraf") || e.contains("video") || e.contains("film")) {
            return KAT_MEDYA;
        }

        if (p.contains("game") || p.contains("oyun") || p.contains("supercell") ||
            p.contains("rovio") || p.contains("gameloft") || p.contains("unity") ||
            e.contains("oyun") || e.contains("game")) {
            return KAT_OYUNLAR;
        }

        return KAT_ARACLAR;
    }
}

