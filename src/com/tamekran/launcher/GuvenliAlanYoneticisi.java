package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

/**
 * Tam Ekran - Güvenli Alan ve Kenar Kırpma Yöneticisi (Safe Area & Overscan Insets)
 * TV overscan, ekran çentiği (notch), kavisli ekran kenarları ve sistem çubukları için
 * 4 yönden (Üst, Alt, Sol, Sağ) bağımsız güvenli alan kırpması sağlar.
 */
public class GuvenliAlanYoneticisi {

    public static final String PREF_GUVENLI_UST = "guvenli_alan_ust_dp";
    public static final String PREF_GUVENLI_ALT = "guvenli_alan_alt_dp";
    public static final String PREF_GUVENLI_SOL = "guvenli_alan_sol_dp";
    public static final String PREF_GUVENLI_SAG = "guvenli_alan_sag_dp";

    public static final int VARSAYILAN_UST_DP = 0;
    public static final int VARSAYILAN_ALT_DP = 0;
    public static final int VARSAYILAN_SOL_DP = 0;
    public static final int VARSAYILAN_SAG_DP = 0;

    private static int sistemSolPx = 0;
    private static int sistemUstPx = 0;
    private static int sistemSagPx = 0;
    private static int sistemAltPx = 0;

    public static boolean edgeToEdgeAktifMi() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public static void sistemInsetleriniGuncelle(int solPx, int ustPx, int sagPx, int altPx) {
        sistemSolPx = Math.max(0, solPx);
        sistemUstPx = Math.max(0, ustPx);
        sistemSagPx = Math.max(0, sagPx);
        sistemAltPx = Math.max(0, altPx);
    }

    public static int getUst(Context context) {
        if (context == null) return VARSAYILAN_UST_DP;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_GUVENLI_UST, VARSAYILAN_UST_DP);
    }

    public static int getAlt(Context context) {
        if (context == null) return VARSAYILAN_ALT_DP;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_GUVENLI_ALT, VARSAYILAN_ALT_DP);
    }

    public static int getSol(Context context) {
        if (context == null) return VARSAYILAN_SOL_DP;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_GUVENLI_SOL, VARSAYILAN_SOL_DP);
    }

    public static int getSag(Context context) {
        if (context == null) return VARSAYILAN_SAG_DP;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.getInt(PREF_GUVENLI_SAG, VARSAYILAN_SAG_DP);
    }

    public static void kaydet(Context context, int solDp, int ustDp, int sagDp, int altDp) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(sp.edit()
            .putInt(PREF_GUVENLI_SOL, Math.max(0, solDp))
            .putInt(PREF_GUVENLI_UST, Math.max(0, ustDp))
            .putInt(PREF_GUVENLI_SAG, Math.max(0, sagDp))
            .putInt(PREF_GUVENLI_ALT, Math.max(0, altDp))
            );
    }

    public static void sifirla(Context context) {
        kaydet(context, VARSAYILAN_SOL_DP, VARSAYILAN_UST_DP, VARSAYILAN_SAG_DP, VARSAYILAN_ALT_DP);
    }

    /**
     * Masaüstü ve çekmece katmanlarına kayıtlı güvenli alan dolgularını uygular
     */
    public static void uygula(Activity activity) {
        if (activity == null) return;
        uygulaPiksel(activity, getSol(activity), getUst(activity), getSag(activity), getAlt(activity));
    }

    /**
     * İnteraktif canlı kalibrasyon sırasında anlık dp değerlerini masaüstüne yansıtır
     */
    public static void uygulaPiksel(Activity activity, int solDp, int ustDp, int sagDp, int altDp) {
        if (activity == null) return;
        View masaustu = activity.findViewById(R.id.masaustu_katmani);
        if (masaustu != null) {
            int solPx = ArayuzYardimcisi.dpToPx(activity, solDp);
            int sagPx = ArayuzYardimcisi.dpToPx(activity, sagDp);
            int altPx = ArayuzYardimcisi.dpToPx(activity, altDp);
            int ustPx = ArayuzYardimcisi.dpToPx(activity, ustDp);

            if (edgeToEdgeAktifMi()) {
                // Edge-to-Edge modunda pencere 0,0'dan başlar; sistem çubukları kadar pay ekle
                if (!AyarBankasi.isTamEkranModu(activity)) {
                    int tabanUst = (sistemUstPx > 0) ? sistemUstPx : ArayuzYardimcisi.getDurumCubuguYuksekligi(activity);
                    ustPx += tabanUst;
                }
                solPx += sistemSolPx;
                sagPx += sistemSagPx;
                altPx += sistemAltPx;
            }
            // API < 21'de sistem durum/gezinme çubuğunu zaten ayırdığı için ikinci kez eklenmez.

            masaustu.setPadding(solPx, ustPx, sagPx, altPx);
        }

        View cekmece = activity.findViewById(R.id.panel_tum_uygulamalar);
        if (cekmece != null && edgeToEdgeAktifMi()) {
            int cUst = AyarBankasi.isTamEkranModu(activity)
                    ? ArayuzYardimcisi.dpToPx(activity, 8)
                    : ((sistemUstPx > 0 ? sistemUstPx : ArayuzYardimcisi.getDurumCubuguYuksekligi(activity)) + ArayuzYardimcisi.dpToPx(activity, 6));
            cekmece.setPadding(sistemSolPx, cUst, sistemSagPx, sistemAltPx);
        }
    }
}

