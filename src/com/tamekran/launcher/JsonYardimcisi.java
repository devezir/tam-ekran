package com.tamekran.launcher;

import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Merkezi JSON ve Veri Serileştirme Yardımcısı (DRY SSOT)
 * Listeler, diziler ve JSON yapıları arasındaki dönüşüm tekrarlarını tek merkezde toplar.
 */
public final class JsonYardimcisi {

    private JsonYardimcisi() {}

    /**
     * String listesini JSON dizisi dizesine ("[\"a\",\"b\"]") dönüştürür.
     * Pure Java uyumlu (Desktop JVM ve Android API 8-36+ uyumlu, Stub hatası vermez).
     */
    public static String stringListesiToJson(List<String> liste) {
        if (liste == null || liste.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < liste.size(); i++) {
            String s = liste.get(i);
            if (s != null) {
                if (sb.length() > 1) {
                    sb.append(",");
                }
                sb.append("\"").append(s.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * JSON dizisi dizesini List<String> nesnesine dönüştürür.
     * Pure Java uyumlu (Desktop JVM ve Android API 8-36+ uyumlu, Stub hatası vermez).
     */
    public static List<String> jsonToStringListesi(String hamJson) {
        List<String> sonuc = new ArrayList<String>();
        if (hamJson == null || hamJson.trim().length() == 0) {
            return sonuc;
        }
        String kirpilmis = hamJson.trim();
        if (kirpilmis.startsWith("[") && kirpilmis.endsWith("]")) {
            kirpilmis = kirpilmis.substring(1, kirpilmis.length() - 1).trim();
            if (kirpilmis.isEmpty()) return sonuc;

            StringBuilder eleman = new StringBuilder();
            boolean tirnakIci = false;
            boolean kacis = false;
            for (int i = 0; i < kirpilmis.length(); i++) {
                char c = kirpilmis.charAt(i);
                if (kacis) {
                    eleman.append(c);
                    kacis = false;
                } else if (c == '\\') {
                    kacis = true;
                } else if (c == '\"') {
                    tirnakIci = !tirnakIci;
                    if (!tirnakIci) {
                        sonuc.add(eleman.toString());
                        eleman.setLength(0);
                    }
                } else if (tirnakIci) {
                    eleman.append(c);
                }
            }
            return sonuc;
        }

        try {
            JSONArray arr = new JSONArray(hamJson);
            for (int i = 0; i < arr.length(); i++) {
                String eleman = arr.optString(i, null);
                if (eleman != null && !eleman.trim().isEmpty()) {
                    sonuc.add(eleman);
                }
            }
        } catch (Throwable t) {
            LauncherLog.w("JsonYardimcisi", "jsonToStringListesi hatası", t);
        }
        return sonuc;
    }

    /**
     * JSONArray nesnesini List<String> nesnesine dönüştürür.
     */
    public static List<String> jsonArrayToStringListesi(JSONArray arr) {
        List<String> sonuc = new ArrayList<String>();
        if (arr == null) return sonuc;
        for (int i = 0; i < arr.length(); i++) {
            String eleman = arr.optString(i, null);
            if (eleman != null && !eleman.trim().isEmpty()) {
                sonuc.add(eleman);
            }
        }
        return sonuc;
    }

    /**
     * List<String> nesnesini JSONArray nesnesine dönüştürür.
     */
    public static JSONArray stringListesiToJsonArray(List<String> liste) {
        JSONArray arr = new JSONArray();
        if (liste == null) return arr;
        for (String s : liste) {
            if (s != null) {
                arr.put(s);
            }
        }
        return arr;
    }
}
