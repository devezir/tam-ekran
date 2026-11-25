package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

/**
 * Tam Ekran - Özel Uygulama İsimlendirme Motoru
 * Kullanıcının istediği herhangi bir uygulamanın adını değiştirmesini sağlar.
 * Çift yönlü bileşen/paket eşleme ile masaüstü, dock, çekmece ve aramada tam tutarlılık sağlar.
 */
public class OzelEtiketYoneticisi {

    public static String getEtiket(Context context, String paketAdi, String varsayilan) {
        if (context == null || paketAdi == null) return varsayilan;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_OZEL_ETIKET_DOSYA);
        if (sp == null) return varsayilan;
        // 1. Önce doğrudan eşleşme (tam kimlik pkg/cls veya saf paket) ile kontrol et
        if (sp.contains(paketAdi)) {
            return sp.getString(paketAdi, varsayilan);
        }
        // 2. Bulunamazsa ve kimlikte aktivite varsa yalın paket adı ile kontrol et
        if (paketAdi.contains("/")) {
            String safPaket = paketAdi.split("/", 2)[0];
            if (sp.contains(safPaket)) {
                return sp.getString(safPaket, varsayilan);
            }
        } else {
            // 3. Aranan saf paket adı ise, pkg/Activity şeklinde kaydedilmiş etiket var mı tara
            Map<String, ?> tumu = sp.getAll();
            if (tumu != null) {
                String onEk = paketAdi + "/";
                for (Map.Entry<String, ?> entry : tumu.entrySet()) {
                    if (entry.getKey().startsWith(onEk) && entry.getValue() instanceof String) {
                        return (String) entry.getValue();
                    }
                }
            }
        }
        return varsayilan;
    }

    public static void setEtiket(Context context, String paketAdi, String yeniEtiket) {
        if (context == null || paketAdi == null) return;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_OZEL_ETIKET_DOSYA);
        if (sp == null) return;
        SharedPreferences.Editor ed = sp.edit();

        if (yeniEtiket == null || yeniEtiket.trim().length() == 0) {
            ed.remove(paketAdi);
            if (paketAdi.contains("/")) {
                ed.remove(paketAdi.split("/", 2)[0]);
            } else {
                Map<String, ?> tumu = sp.getAll();
                if (tumu != null) {
                    String onEk = paketAdi + "/";
                    for (String key : tumu.keySet()) {
                        if (key.startsWith(onEk)) {
                            ed.remove(key);
                        }
                    }
                }
            }
            AyarBankasi.editorKaydet(ed);
        } else {
            String kirpilmis = yeniEtiket.trim();
            ed.putString(paketAdi, kirpilmis);
            if (paketAdi.contains("/")) {
                ed.putString(paketAdi.split("/", 2)[0], kirpilmis);
            }
            AyarBankasi.editorKaydet(ed);
        }
    }
}
