package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tam Ekran - Klasör Kapağı Modu Yöneticisi (Nova Folder Cover Mode)
 * Etkinleştirildiğinde klasöre tek dokunuş ilk uygulamayı doğrudan başlatır;
 * yukarı kaydırma veya çift dokunma ise klasör penceresini açar.
 */
public class KlasorKapakYoneticisi {

    public static final String PREF_KLASOR_KAPAK_MODU_GENEL = "klasor_kapak_modu_genel";
    public static final String PREF_OZEL_KAPAK_KLASORLERI = "ozel_kapak_klasorleri_json";
    public static final String PREF_OZEL_KAPAK_KAPALI_KLASORLERI = "ozel_kapak_kapali_klasorleri_json";

    public static boolean isGenelKapakModu(Context context) {
        return AyarBankasi.getBoolean(context, PREF_KLASOR_KAPAK_MODU_GENEL, false);
    }

    public static void setGenelKapakModu(Context context, boolean acik) {
        AyarBankasi.putBoolean(context, PREF_KLASOR_KAPAK_MODU_GENEL, acik);
    }

    public static Set<String> getOzelKapakKlasorleri(Context context) {
        if (!AyarBankasi.baglamGecerliMi(context)) return new HashSet<String>();
        List<String> list = AyarBankasi.getStringListesi(context, PREF_OZEL_KAPAK_KLASORLERI);
        return new HashSet<String>(list);
    }

    public static Set<String> getOzelKapakKapaliKlasorleri(Context context) {
        if (!AyarBankasi.baglamGecerliMi(context)) return new HashSet<String>();
        List<String> list = AyarBankasi.getStringListesi(context, PREF_OZEL_KAPAK_KAPALI_KLASORLERI);
        return new HashSet<String>(list);
    }

    public static boolean hesaplaKapakModu(
            boolean genelKapak,
            Set<String> ozelAcik,
            Set<String> ozelKapali,
            String klasorId) {
        if (klasorId == null) return false;
        if (ozelKapali != null && ozelKapali.contains(klasorId)) return false;
        if (ozelAcik != null && ozelAcik.contains(klasorId)) return true;
        return genelKapak;
    }

    public static boolean isKlasorKapakModu(Context context, String klasorId) {
        if (!AyarBankasi.baglamGecerliMi(context) || klasorId == null) return false;
        return hesaplaKapakModu(
                isGenelKapakModu(context),
                getOzelKapakKlasorleri(context),
                getOzelKapakKapaliKlasorleri(context),
                klasorId
        );
    }

    public static void setKlasorKapakModu(Context context, String klasorId, boolean aktif) {
        if (!AyarBankasi.baglamGecerliMi(context) || klasorId == null) return;
        Set<String> acikSet = getOzelKapakKlasorleri(context);
        Set<String> kapaliSet = getOzelKapakKapaliKlasorleri(context);
        if (aktif) {
            acikSet.add(klasorId);
            kapaliSet.remove(klasorId);
        } else {
            acikSet.remove(klasorId);
            kapaliSet.add(klasorId);
        }
        AyarBankasi.putStringListesi(context, PREF_OZEL_KAPAK_KLASORLERI, new ArrayList<String>(acikSet));
        AyarBankasi.putStringListesi(context, PREF_OZEL_KAPAK_KAPALI_KLASORLERI, new ArrayList<String>(kapaliSet));
    }
}

