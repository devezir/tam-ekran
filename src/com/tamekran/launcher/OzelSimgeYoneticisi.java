package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Tam Ekran - Bireysel Özel Uygulama Simgesi Motoru
 * Herhangi bir uygulamanın simgesini başka bir simge paketiyle değiştirmeyi sağlar.
 */
public class OzelSimgeYoneticisi {

    private static final String PREF_OZEL_SIMGELER_JSON = "ozel_simgeler_atamalari_json";

    public static Map<String, String> ozelSimgeleriYukle(Context context) {
        Map<String, String> harita = new HashMap<String, String>();
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
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
        if (paketAdi == null) return null;
        Map<String, String> harita = ozelSimgeleriYukle(context);
        return harita.get(paketAdi);
    }

    public static void setOzelPaket(Context context, String paketAdi, String simgePaketi) {
        if (paketAdi == null) return;
        Map<String, String> harita = ozelSimgeleriYukle(context);
        if (simgePaketi == null || simgePaketi.length() == 0) {
            harita.remove(paketAdi);
        } else {
            harita.put(paketAdi, simgePaketi);
        }
        ozelSimgeleriKaydet(context, harita);
    }
}

