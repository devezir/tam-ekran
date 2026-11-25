package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Android Sistem Kısayolları Yöneticisi (System Shortcuts Engine)
 * ACTION_CREATE_SHORTCUT ile oluşturulan kişi, arama, ayar, sohbet ve web kısayollarını
 * masaüstüne ekler ve yönetir. Android 2.2 - Android 17 tam uyumlu.
 */
public class KisayolYoneticisi {

    public static final int KOD_KISAYOL_SEC = 2001;
    public static final String PREF_KISAYOLLAR = "tamekran_kisayollar_json";

    public static class KisayolBilgisi {
        public String id;
        public String ad;
        public int sayfa;
        public String intentUri;
        public String simgeDosyaYolu;
        public String simgeBase64;
        public String simgePaket;
        public String simgeKaynak;
        public String shortcutPaket;
        public String shortcutId;
    }

    private static String simgeyiDosyayaKaydet(Context context, String id, Bitmap bmp) {
        if (context == null || id == null || bmp == null) return null;
        try {
            java.io.File klasor = new java.io.File(context.getFilesDir(), "kisayol_simgeleri");
            if (!klasor.exists()) klasor.mkdirs();
            java.io.File dosya = new java.io.File(klasor, id + ".png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(dosya);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return dosya.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    public static void kisayolEkleIstegiBaslat(Activity activity) {
        if (activity == null) return;
        Intent niyet = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        try {
            activity.startActivityForResult(niyet, KOD_KISAYOL_SEC);
        } catch (Throwable t) {
            // Alternatif picker bulunamadıysa
        }
    }

    public static boolean kisayolSonucunuKaydet(Context context, Intent data, int hedefSayfa) {
        if (context == null || data == null) return false;

        Intent shortcutIntent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        if (shortcutIntent == null || name == null || name.trim().length() == 0) {
            return false;
        }

        KisayolBilgisi kb = new KisayolBilgisi();
        kb.id = "kisayol_" + System.currentTimeMillis();
        kb.ad = name.trim();
        kb.sayfa = hedefSayfa;
        kb.intentUri = shortcutIntent.toUri(Intent.URI_INTENT_SCHEME);

        Bitmap iconBmp = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (iconBmp != null) {
            String dosyaYolu = simgeyiDosyayaKaydet(context, kb.id, iconBmp);
            kb.simgeDosyaYolu = dosyaYolu;
        } else {
            Intent.ShortcutIconResource iconRes = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (iconRes != null) {
                kb.simgePaket = iconRes.packageName;
                kb.simgeKaynak = iconRes.resourceName;
            }
        }

        List<KisayolBilgisi> tumu = tumKisayollariYukle(context);
        tumu.add(kb);
        kisayollariKaydet(context, tumu);
        AyarBankasi.degisiklikDamgasiGuncelle(context);
        return true;
    }

    public static boolean pinnedKisayolKaydet(
            Context context,
            String shortcutPaket,
            String shortcutId,
            String ad,
            Bitmap iconBmp,
            int hedefSayfa) {
        return pinnedKisayolKaydet(context, shortcutPaket, shortcutId, ad, null, iconBmp, hedefSayfa);
    }

    public static boolean pinnedKisayolKaydet(
            Context context,
            String shortcutPaket,
            String shortcutId,
            String ad,
            String yedekIntentUri,
            Bitmap iconBmp,
            int hedefSayfa) {
        if (context == null || shortcutPaket == null || shortcutId == null) return false;
        String sonAd = (ad != null && ad.trim().length() > 0) ? ad.trim() : shortcutPaket;

        List<KisayolBilgisi> tumu = tumKisayollariYukle(context);
        for (KisayolBilgisi mevcut : tumu) {
            if (shortcutPaket.equals(mevcut.shortcutPaket) && shortcutId.equals(mevcut.shortcutId)) {
                mevcut.ad = sonAd;
                if (yedekIntentUri != null && yedekIntentUri.length() > 0) {
                    mevcut.intentUri = yedekIntentUri;
                }
                if (iconBmp != null) {
                    mevcut.simgeDosyaYolu = simgeyiDosyayaKaydet(context, mevcut.id, iconBmp);
                }
                kisayollariKaydet(context, tumu);
                AyarBankasi.degisiklikDamgasiGuncelle(context);
                return true;
            }
        }

        KisayolBilgisi kb = new KisayolBilgisi();
        kb.id = "kisayol_" + System.currentTimeMillis();
        kb.ad = sonAd;
        kb.sayfa = hedefSayfa;
        kb.shortcutPaket = shortcutPaket;
        kb.shortcutId = shortcutId;
        kb.intentUri = (yedekIntentUri != null) ? yedekIntentUri : "";
        if (iconBmp != null) {
            kb.simgeDosyaYolu = simgeyiDosyayaKaydet(context, kb.id, iconBmp);
        }

        tumu.add(kb);
        kisayollariKaydet(context, tumu);
        AyarBankasi.degisiklikDamgasiGuncelle(context);
        return true;
    }

    public static String kisayolHedefPaketGetir(Context context, String kisayolPaketKimligi) {
        if (context == null || kisayolPaketKimligi == null) return null;
        String kId = kisayolPaketKimligi.startsWith("kisayol:")
                ? kisayolPaketKimligi.substring("kisayol:".length())
                : kisayolPaketKimligi;
        List<KisayolBilgisi> list = tumKisayollariYukle(context);
        for (KisayolBilgisi kb : list) {
            if (kb.id.equals(kId)) {
                if (kb.shortcutPaket != null && kb.shortcutPaket.length() > 0) {
                    return kb.shortcutPaket;
                }
                if (kb.intentUri != null && kb.intentUri.length() > 0) {
                    try {
                        Intent niyet = Intent.parseUri(kb.intentUri, Intent.URI_INTENT_SCHEME);
                        if (niyet.getPackage() != null) return niyet.getPackage();
                        if (niyet.getComponent() != null) return niyet.getComponent().getPackageName();
                    } catch (Exception e) {
                        LauncherLog.d("KisayolYoneticisi", "Hedef paket cozumlenemedi: " + e.getMessage());
                    }
                }
                break;
            }
        }
        return null;
    }

    public static boolean kisayolBaslat(Context context, String kisayolPaketKimligi) {
        if (context == null || kisayolPaketKimligi == null) return false;
        String kId = kisayolPaketKimligi.startsWith("kisayol:")
                ? kisayolPaketKimligi.substring("kisayol:".length())
                : kisayolPaketKimligi;

        List<KisayolBilgisi> list = tumKisayollariYukle(context);
        for (KisayolBilgisi kb : list) {
            if (kb.id.equals(kId)) {
                if (kb.shortcutPaket != null && kb.shortcutId != null && android.os.Build.VERSION.SDK_INT >= 25) {
                    if (PinKisayolYardimcisi.baslatPinnedShortcut(context, kb.shortcutPaket, kb.shortcutId)) {
                        return true;
                    }
                }
                if (kb.intentUri != null && kb.intentUri.length() > 0) {
                    try {
                        Intent niyet = Intent.parseUri(kb.intentUri, Intent.URI_INTENT_SCHEME);
                        if (!(context instanceof Activity)) {
                            niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        context.startActivity(niyet);
                        return true;
                    } catch (Exception e) {
                        LauncherLog.w("KisayolYoneticisi", "kisayolBaslat intent hatasi", e);
                    }
                }
                break;
            }
        }
        return false;
    }

    public static List<KisayolBilgisi> tumKisayollariYukle(Context context) {
        List<KisayolBilgisi> liste = new ArrayList<KisayolBilgisi>();
        if (context == null) return liste;

        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String jsonStr = sp.getString(PREF_KISAYOLLAR, "[]");

        boolean migrasyonGerekli = false;
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                KisayolBilgisi kb = new KisayolBilgisi();
                kb.id = obj.getString("id");
                kb.ad = obj.getString("ad");
                kb.sayfa = obj.optInt("sayfa", 0);
                kb.intentUri = obj.optString("intentUri", "");
                kb.simgeDosyaYolu = obj.optString("simgeDosyaYolu", null);
                kb.simgeBase64 = obj.optString("simgeBase64", null);
                kb.simgePaket = obj.optString("simgePaket", null);
                kb.simgeKaynak = obj.optString("simgeKaynak", null);
                kb.shortcutPaket = obj.optString("shortcutPaket", null);
                kb.shortcutId = obj.optString("shortcutId", null);

                // Eski Base64 simgeleri varsa arka planda dosyaya taşı (SharedPreferences optimizasyonu)
                if (kb.simgeDosyaYolu == null && kb.simgeBase64 != null && kb.simgeBase64.length() > 0) {
                    try {
                        byte[] bytes = Base64.decode(kb.simgeBase64, Base64.NO_WRAP);
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bmp != null) {
                            kb.simgeDosyaYolu = simgeyiDosyayaKaydet(context, kb.id, bmp);
                            kb.simgeBase64 = null;
                            migrasyonGerekli = true;
                        }
                    } catch (Throwable t) {
                        LauncherLog.w("KisayolYoneticisi", "Base64 simge migrasyon hatası", t);
                    }
                }

                liste.add(kb);
            }
        } catch (Throwable t) {
            LauncherLog.w("KisayolYoneticisi", "tumKisayollariYukle JSON hatası", t);
        }

        if (migrasyonGerekli) {
            kisayollariKaydet(context, liste);
        }

        return liste;
    }

    public static void kisayollariKaydet(Context context, List<KisayolBilgisi> liste) {
        if (context == null) return;
        try {
            JSONArray arr = new JSONArray();
            if (liste != null) {
                for (KisayolBilgisi kb : liste) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", kb.id);
                    obj.put("ad", kb.ad);
                    obj.put("sayfa", kb.sayfa);
                    obj.put("intentUri", kb.intentUri != null ? kb.intentUri : "");
                    if (kb.simgeDosyaYolu != null) obj.put("simgeDosyaYolu", kb.simgeDosyaYolu);
                    else if (kb.simgeBase64 != null) obj.put("simgeBase64", kb.simgeBase64);
                    if (kb.simgePaket != null) obj.put("simgePaket", kb.simgePaket);
                    if (kb.simgeKaynak != null) obj.put("simgeKaynak", kb.simgeKaynak);
                    if (kb.shortcutPaket != null) obj.put("shortcutPaket", kb.shortcutPaket);
                    if (kb.shortcutId != null) obj.put("shortcutId", kb.shortcutId);
                    arr.put(obj);
                }
            }
            SharedPreferences sp = AyarBankasi.getAyarlar(context);
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_KISAYOLLAR, arr.toString()));
        } catch (Throwable t) {
            LauncherLog.e("KisayolYoneticisi", "kisayollariKaydet JSON yazma hatası", t);
        }
    }

    public static void kisayolSil(Context context, String kisayolId) {
        if (context == null || kisayolId == null) return;
        List<KisayolBilgisi> list = tumKisayollariYukle(context);
        List<KisayolBilgisi> yeni = new ArrayList<KisayolBilgisi>();
        for (KisayolBilgisi kb : list) {
            if (!kb.id.equals(kisayolId)) {
                yeni.add(kb);
            } else {
                if (kb.simgeDosyaYolu != null) {
                    try {
                        new java.io.File(kb.simgeDosyaYolu).delete();
                    } catch (Throwable t) {
                        LauncherLog.d("KisayolYoneticisi", "Simge dosyasi silinemedi: " + t.getMessage());
                    }
                }
            }
        }
        kisayollariKaydet(context, yeni);
    }

    public static List<UygulamaBilgisi> sayfadakiKisayollariAl(Context context, int sayfa) {
        List<UygulamaBilgisi> sonuc = new ArrayList<UygulamaBilgisi>();
        if (context == null) return sonuc;

        PackageManager pm = context.getPackageManager();
        List<KisayolBilgisi> list = tumKisayollariYukle(context);

        for (KisayolBilgisi kb : list) {
            if (kb.sayfa == sayfa) {
                try {
                    Intent niyet = (kb.intentUri != null && kb.intentUri.length() > 0)
                            ? Intent.parseUri(kb.intentUri, Intent.URI_INTENT_SCHEME)
                            : null;
                    Drawable simge = simgeYukle(context, pm, kb);
                    sonuc.add(new UygulamaBilgisi(kb.ad, "kisayol:" + kb.id, simge, niyet));
                } catch (Throwable t) {
                    LauncherLog.w("KisayolYoneticisi", "sayfadakiKisayollariAl niyet cozumleme hatasi", t);
                }
            }
        }
        return sonuc;
    }

    private static Drawable simgeYukle(Context context, PackageManager pm, KisayolBilgisi kb) {
        Drawable simge = null;
        if (kb.simgeDosyaYolu != null) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(kb.simgeDosyaYolu);
                if (bmp != null) {
                    simge = new BitmapDrawable(context.getResources(), bmp);
                }
            } catch (Throwable t) {
                LauncherLog.d("KisayolYoneticisi", "Simge dosyasindan yuklenemedi: " + t.getMessage());
            }
        }

        if (simge == null && kb.simgeBase64 != null && kb.simgeBase64.length() > 0) {
            try {
                byte[] bytes = Base64.decode(kb.simgeBase64, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    simge = new BitmapDrawable(context.getResources(), bmp);
                }
            } catch (Throwable t) {
                LauncherLog.d("KisayolYoneticisi", "Simge Base64 yuklenemedi: " + t.getMessage());
            }
        } else if (simge == null && kb.simgePaket != null && kb.simgeKaynak != null && pm != null) {
            try {
                Resources res = pm.getResourcesForApplication(kb.simgePaket);
                int id = res.getIdentifier(kb.simgeKaynak, null, null);
                if (id != 0) {
                    simge = res.getDrawable(id);
                }
            } catch (Throwable t) {
                LauncherLog.d("KisayolYoneticisi", "Simge paketten yuklenemedi: " + t.getMessage());
            }
        }

        if (simge == null) {
            simge = context.getResources().getDrawable(R.drawable.simge_varsayilan);
        }
        return simge;
    }

    /**
     * "kisayol:id" şeklindeki kimlikten ilgili kısayolu bularak UygulamaBilgisi nesnesine dönüştürür (DRY SSOT).
     */
    public static UygulamaBilgisi kisayolUygulamasiGetir(Context context, String kisayolPaketKimligi) {
        if (context == null || kisayolPaketKimligi == null) return null;
        String kId = kisayolPaketKimligi.startsWith("kisayol:")
                ? kisayolPaketKimligi.substring("kisayol:".length())
                : kisayolPaketKimligi;

        PackageManager pm = context.getPackageManager();
        List<KisayolBilgisi> list = tumKisayollariYukle(context);
        for (KisayolBilgisi kb : list) {
            if (kb.id.equals(kId)) {
                try {
                    Intent niyet = (kb.intentUri != null && kb.intentUri.length() > 0)
                            ? Intent.parseUri(kb.intentUri, Intent.URI_INTENT_SCHEME)
                            : null;
                    Drawable simge = simgeYukle(context, pm, kb);
                    return new UygulamaBilgisi(kb.ad, "kisayol:" + kb.id, simge, niyet);
                } catch (Throwable t) {
                    LauncherLog.w("KisayolYoneticisi", "kisayolUygulamasiGetir cozumleme hatasi", t);
                }
                break;
            }
        }
        return null;
    }
}

