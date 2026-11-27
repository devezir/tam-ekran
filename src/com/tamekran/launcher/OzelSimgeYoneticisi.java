package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tam Ekran - Bireysel Özel Uygulama Simgesi Motoru
 * Herhangi bir uygulamanın simgesini başka bir simge paketiyle veya galeriden seçilen
 * PNG/JPG görseliyle değiştirmeyi sağlar (bileşen kimliği öncelikli, paket geriye dönük uyumlu).
 */
public class OzelSimgeYoneticisi {

    public static final int KOD_GALERI_SIMGE_SEC = 4099;
    private static final String PREF_OZEL_SIMGELER_JSON = "ozel_simgeler_atamalari_json";
    private static final int MAKS_SIMGE_PX = 192;

    private static String bekleyenBilesenKimligi;
    private static String bekleyenPaketAdi;

    public static Map<String, String> ozelSimgeleriYukle(Context context) {
        Map<String, String> harita = new HashMap<String, String>();
        if (context == null) return harita;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        if (sp == null) return harita;
        String ham = sp.getString(PREF_OZEL_SIMGELER_JSON, "{}");

        try {
            JSONObject obj = new JSONObject(ham);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                harita.put(k, obj.getString(k));
            }
        } catch (Throwable t) {
            LauncherLog.w("OzelSimgeYoneticisi", "ozelSimgeleriYukle JSON hatasi", t);
        }

        return harita;
    }

    public static void ozelSimgeleriKaydet(Context context, Map<String, String> harita) {
        if (context == null || harita == null) return;
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, String> entry : harita.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
            SharedPreferences sp = AyarBankasi.getAyarlar(context);
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_OZEL_SIMGELER_JSON, obj.toString()));
        } catch (Throwable t) {
            LauncherLog.e("OzelSimgeYoneticisi", "ozelSimgeleriKaydet JSON hatasi", t);
        }
    }

    public static String getOzelPaket(Context context, String paketAdi) {
        return ozelSimgeKaydiAl(context, paketAdi, paketAdi);
    }

    public static String ozelSimgeKaydiAl(Context context, String bilesenKimligi, String paketAdi) {
        if (context == null) return null;
        Map<String, String> harita = ozelSimgeleriYukle(context);
        if (bilesenKimligi != null && bilesenKimligi.length() > 0 && harita.containsKey(bilesenKimligi)) {
            return harita.get(bilesenKimligi);
        }
        if (paketAdi != null && paketAdi.length() > 0) {
            return harita.get(paketAdi);
        }
        return null;
    }

    public static void setOzelPaket(Context context, String paketAdi, String simgePaketi) {
        setOzelSimge(context, paketAdi, paketAdi, simgePaketi);
    }

    public static void setOzelSimge(Context context, String bilesenKimligi, String paketAdi, String deger) {
        if (context == null) return;
        String hedefAnahtar = (bilesenKimligi != null && bilesenKimligi.length() > 0) ? bilesenKimligi : paketAdi;
        if (hedefAnahtar == null || hedefAnahtar.length() == 0) return;

        Map<String, String> harita = ozelSimgeleriYukle(context);
        if (deger == null || deger.length() == 0) {
            eskiDosyayiSil(harita.remove(hedefAnahtar));
            if (paketAdi != null && !paketAdi.equals(hedefAnahtar)) {
                eskiDosyayiSil(harita.remove(paketAdi));
            }
        } else {
            eskiDosyayiSil(harita.get(hedefAnahtar));
            harita.put(hedefAnahtar, deger);
        }
        ozelSimgeleriKaydet(context, harita);
    }

    public static void galeriSeciminiBaslat(Activity activity, String bilesenKimligi, String paketAdi) {
        if (activity == null) return;
        bekleyenBilesenKimligi = bilesenKimligi;
        bekleyenPaketAdi = paketAdi;
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.startActivityForResult(Intent.createChooser(intent, "Simge Görseli Seç"), KOD_GALERI_SIMGE_SEC);
        } catch (Throwable t) {
            LauncherLog.w("OzelSimgeYoneticisi", "Galeri secici acilamadi", t);
            ArayuzYardimcisi.toast(activity, "Galeri açılırken hata oluştu.");
        }
    }

    public static String bekleyenGaleriSonucunuIsle(Context context, Uri uri) {
        if (context == null || uri == null) return null;
        String bilesen = bekleyenBilesenKimligi;
        String paket = bekleyenPaketAdi;
        bekleyenBilesenKimligi = null;
        bekleyenPaketAdi = null;
        if ((bilesen == null || bilesen.length() == 0) && (paket == null || paket.length() == 0)) {
            return null;
        }
        if (galeriSimgeKaydet(context, bilesen, paket, uri)) {
            return paket != null ? paket : bilesen;
        }
        return null;
    }

    public static boolean galeriSimgeKaydet(Context context, String bilesenKimligi, String paketAdi, Uri uri) {
        if (context == null || uri == null) return false;
        InputStream isBounds = null;
        InputStream isDecode = null;
        FileOutputStream fos = null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            isBounds = context.getContentResolver().openInputStream(uri);
            if (isBounds == null) return false;
            BitmapFactory.decodeStream(isBounds, null, opts);
            isBounds.close();
            isBounds = null;

            int w = opts.outWidth;
            int h = opts.outHeight;
            if (w <= 0 || h <= 0) return false;

            int inSample = 1;
            while ((w / inSample) > MAKS_SIMGE_PX * 2 || (h / inSample) > MAKS_SIMGE_PX * 2) {
                inSample *= 2;
            }

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = inSample;
            isDecode = context.getContentResolver().openInputStream(uri);
            if (isDecode == null) return false;
            Bitmap hamBmp = BitmapFactory.decodeStream(isDecode, null, decodeOpts);
            isDecode.close();
            isDecode = null;
            if (hamBmp == null) return false;

            Bitmap sonBmp = hamBmp;
            if (hamBmp.getWidth() > MAKS_SIMGE_PX || hamBmp.getHeight() > MAKS_SIMGE_PX) {
                float oran = Math.min((float) MAKS_SIMGE_PX / hamBmp.getWidth(), (float) MAKS_SIMGE_PX / hamBmp.getHeight());
                int yeniW = Math.max(1, Math.round(hamBmp.getWidth() * oran));
                int yeniH = Math.max(1, Math.round(hamBmp.getHeight() * oran));
                sonBmp = Bitmap.createScaledBitmap(hamBmp, yeniW, yeniH, true);
                if (sonBmp != hamBmp) hamBmp.recycle();
            }

            File klasor = new File(context.getFilesDir(), "ozel_simgeler");
            if (!klasor.exists()) klasor.mkdirs();
            File hedef = new File(klasor, "simge_" + System.currentTimeMillis() + ".png");
            fos = new FileOutputStream(hedef);
            sonBmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            fos = null;
            sonBmp.recycle();

            setOzelSimge(context, bilesenKimligi, paketAdi, "file:" + hedef.getAbsolutePath());
            return true;
        } catch (Throwable t) {
            LauncherLog.e("OzelSimgeYoneticisi", "galeriSimgeKaydet hatasi", t);
            return false;
        } finally {
            try { if (isBounds != null) isBounds.close(); } catch (Exception e) { LauncherLog.d("OzelSimgeYoneticisi", "isBounds kapatilamadi: " + e.getMessage()); }
            try { if (isDecode != null) isDecode.close(); } catch (Exception e) { LauncherLog.d("OzelSimgeYoneticisi", "isDecode kapatilamadi: " + e.getMessage()); }
            try { if (fos != null) fos.close(); } catch (Exception e) { LauncherLog.d("OzelSimgeYoneticisi", "fos kapatilamadi: " + e.getMessage()); }
        }
    }

    public static Drawable galeriSimgeYukle(Context context, String deger) {
        if (context == null || deger == null || !deger.startsWith("file:")) return null;
        try {
            String yol = deger.substring("file:".length());
            File f = new File(yol);
            if (!f.exists() || !f.isFile()) return null;
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (bmp == null) return null;
            return new BitmapDrawable(context.getResources(), bmp);
        } catch (Throwable t) {
            LauncherLog.d("OzelSimgeYoneticisi", "galeriSimgeYukle hatasi: " + t.getMessage());
            return null;
        }
    }

    private static void eskiDosyayiSil(String deger) {
        if (deger != null && deger.startsWith("file:")) {
            try {
                File f = new File(deger.substring("file:".length()));
                if (f.exists()) f.delete();
            } catch (Throwable t) {
                LauncherLog.d("OzelSimgeYoneticisi", "Eski simge dosyasi silinemedi: " + t.getMessage());
            }
        }
    }
}
