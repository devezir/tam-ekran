package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import java.lang.reflect.Method;

/**
 * Tam Ekran - Kişiselleştirilebilir Ekran Hareketleri (Gestures)
 * Nova Launcher benzeri her harekete özel eylem atama motoru.
 */
public class HareketYoneticisi {

    public static final String PREF_HAREKET_ASAGI = "hareket_asagi";
    public static final String PREF_HAREKET_YUKARI = "hareket_yukari";
    public static final String PREF_HAREKET_CIFT_TIK = "hareket_cift_tik";
    public static final String PREF_HAREKET_IKI_ASAGI = "hareket_iki_asagi";
    public static final String PREF_HAREKET_IKI_YUKARI = "hareket_iki_yukari";
    public static final String PREF_HAREKET_ANA_EKRAN = "hareket_ana_ekran";
    public static final String PREF_HAREKET_GERI_TUSU = "hareket_geri_tusu";

    public static final int EYLEM_YOK = 0;
    public static final int EYLEM_BILDIRIMLER = 1;
    public static final int EYLEM_HIZLI_AYARLAR = 2;
    public static final int EYLEM_CEKMECE = 3;
    public static final int EYLEM_KILITLE = 4;
    public static final int EYLEM_ARAMA = 5;
    public static final int EYLEM_AYARLAR = 6;
    public static final int EYLEM_FENER = 7;
    public static final int EYLEM_SES_MODU = 8;
    public static final int EYLEM_SESLI_ARAMA = 9;

    public static final String[] EYLEM_METINLERI = new String[]{
        "Hiçbir Şey Yapma",
        "Bildirim Panelini Aç",
        "Hızlı Ayarlar Panelini Aç",
        "Tüm Uygulamalar Menüsünü Aç",
        "Ekranı Kilitle",
        "Hızlı Uygulama Arama",
        "Başlatıcı Ayarları",
        "🔦 Feneri Aç / Kapat",
        "🔊 Ses Modunu Değiştir (Sessiz/Titreşim/Normal)",
        "🎙️ Sesli Arama Başlat"
    };

    public static int eylemiAl(Context context, String hareketAnahtari, int varsayilanEylem) {
        SharedPreferences pref = AyarBankasi.getAyarlar(context);
        return pref.getInt(hareketAnahtari, varsayilanEylem);
    }

    public static void eylemiKaydet(Context context, String hareketAnahtari, int eylem) {
        SharedPreferences pref = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(pref.edit().putInt(hareketAnahtari, eylem));
    }

    /**
     * Seçilen eylemi çalıştırır.
     */
    public static void eylemiCalistir(AnaEkranActivity activity, int eylem) {
        if (activity == null || activity.isFinishing()) return;

        switch (eylem) {
            case EYLEM_BILDIRIMLER:
                bildirimPaneliniAc(activity);
                break;
            case EYLEM_HIZLI_AYARLAR:
                hizliAyarlariAc(activity);
                break;
            case EYLEM_CEKMECE:
                activity.cekmeceyiAc();
                break;
            case EYLEM_KILITLE:
                EkranKilitleyici.kilitle(activity);
                break;
            case EYLEM_ARAMA:
                activity.cekmeceyiAc();
                break;
            case EYLEM_AYARLAR:
                activity.startActivity(new Intent(activity, AyarlarActivity.class));
                break;
            case EYLEM_FENER:
                FenerYoneticisi.toggle(activity);
                break;
            case EYLEM_SES_MODU:
                SesModuYoneticisi.sonrakiModaGec(activity);
                break;
            case EYLEM_SESLI_ARAMA:
                activity.sesliAramaBaslat();
                break;
            case EYLEM_YOK:
            default:
                break;
        }
    }

    private static void bildirimPaneliniAc(Context context) {
        try {
            Object service = context.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method expand;
            if (Build.VERSION.SDK_INT >= 17) {
                expand = statusbarManager.getMethod("expandNotificationsPanel");
            } else {
                expand = statusbarManager.getMethod("expand");
            }
            expand.invoke(service);
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(context, "Bildirim paneli açılamadı");
        }
    }

    private static void hizliAyarlariAc(Context context) {
        try {
            Object service = context.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method expand = statusbarManager.getMethod("expandSettingsPanel");
            expand.invoke(service);
        } catch (Throwable t) {
            // Eski cihazlarda bildirim paneline düş
            bildirimPaneliniAc(context);
        }
    }
}

