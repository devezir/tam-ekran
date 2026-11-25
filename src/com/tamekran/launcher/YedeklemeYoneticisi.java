package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tam Ekran - Ayarları Yedekle, Geri Yükle ve Paylaş (Backup & Restore Engine)
 * Saf JSON formatında başlatıcı düzenini, widget'ları, özel simgeleri ve ayarları yedekler.
 * WhatsApp, Telegram veya Drive ile dışa aktarabilme desteği.
 * Android 2.2 - Android 17 tam uyumlu.
 */
public class YedeklemeYoneticisi {

    public static final String PREF_GIZLENENLER_CSV = AyarBankasi.PREF_GIZLENENLER_CSV;
    public static final String PREF_ETIKETLERI_GOSTER = AyarBankasi.PREF_ETIKETLERI_GOSTER;
    public static final String PREF_MASAUSTU_KILITLI = AyarBankasi.PREF_MASAUSTU_KILITLI;
    public static final String PREF_DOCK_SIMGE_SAYISI = AyarBankasi.PREF_DOCK_SIMGE_SAYISI;
    public static final String PREF_SIMGE_BOYUTU = AyarBankasi.PREF_SIMGE_BOYUTU;
    public static final String PREF_MASAUSTU_SIMGE_BOYUTU = AyarBankasi.PREF_MASAUSTU_SIMGE_BOYUTU;
    public static final String PREF_CEKMECE_SIMGE_BOYUTU = AyarBankasi.PREF_CEKMECE_SIMGE_BOYUTU;
    public static final String PREF_DOCK_SIMGE_BOYUTU = AyarBankasi.PREF_DOCK_SIMGE_BOYUTU;
    public static final String PREF_GECIS_EFEKTI = AyarBankasi.PREF_GECIS_EFEKTI;
    public static final String PREF_DOCK_GORUNUR = AyarBankasi.PREF_DOCK_GORUNUR;
    public static final String PREF_CEKMECE_SAYDAMLIK = AyarBankasi.PREF_CEKMECE_SAYDAMLIK;
    public static final String PREF_MASAUSTU_DOLGU = AyarBankasi.PREF_MASAUSTU_DOLGU;
    public static final String PREF_DUVAR_KAGIDI_KAYDIRMA = AyarBankasi.PREF_DUVAR_KAGIDI_KAYDIRMA;
    public static final String PREF_DOCK_YUKSEKLIK = AyarBankasi.PREF_DOCK_YUKSEKLIK;
    public static final String PREF_DOCK_KAYDIRILABILIR = AyarBankasi.PREF_DOCK_KAYDIRILABILIR;

    private static void atomikYaz(File hedefDosya, byte[] veri) throws Exception {
        File ebeveyn = hedefDosya.getParentFile();
        File gecici = new File(ebeveyn, hedefDosya.getName() + ".tmp");
        File yedek = new File(ebeveyn, hedefDosya.getName() + ".bak");
        if (gecici.exists()) gecici.delete();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(gecici);
            fos.write(veri);
            fos.flush();
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception e) { LauncherLog.d("YedeklemeYoneticisi", "Stream kapatma uyarisi: " + e.getMessage()); }
        }
        if (yedek.exists()) yedek.delete();
        if (hedefDosya.exists() && !hedefDosya.renameTo(yedek)) {
            gecici.delete();
            throw new java.io.IOException("Mevcut yedek korunamadi: " + hedefDosya.getName());
        }
        if (!gecici.renameTo(hedefDosya)) {
            if (yedek.exists()) yedek.renameTo(hedefDosya);
            gecici.delete();
            throw new java.io.IOException("Yedek dosyasi tasinamadi: " + hedefDosya.getName());
        }
        if (yedek.exists()) yedek.delete();
    }

    public static File yedekDosyasi(Context context) {
        File dizin = AyarBankasi.getDosyalarDizini(context);
        return new File(dizin, "TamEkran_Yedek.json");
    }

    public static List<String> gizlenenleriAl(Context context) {
        SharedPreferences pref = AyarBankasi.getAyarlar(context);
        String csv = pref.getString(PREF_GIZLENENLER_CSV, "");
        if (csv == null || csv.trim().length() == 0) {
            return new ArrayList<String>();
        }
        String[] parcalar = csv.split(",");
        return new ArrayList<String>(Arrays.asList(parcalar));
    }

    public static void gizlenenleriKaydet(Context context, List<String> liste) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < liste.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(liste.get(i));
        }
        SharedPreferences pref = AyarBankasi.getAyarlar(context);
        AyarBankasi.editorKaydet(pref.edit().putString(PREF_GIZLENENLER_CSV, sb.toString()));
    }

    private static final int TIP_INT = 0;
    private static final int TIP_BOOL = 1;
    private static final int TIP_STRING = 2;

    private static final class YedekAnahtari {
        final String jsonAnahtari;
        final String prefAnahtari;
        final int tip;
        final Object varsayilan;

        YedekAnahtari(String jsonAnahtari, String prefAnahtari, int tip, Object varsayilan) {
            this.jsonAnahtari = jsonAnahtari;
            this.prefAnahtari = prefAnahtari;
            this.tip = tip;
            this.varsayilan = varsayilan;
        }
    }

    private static YedekAnahtari[] yedekSemasiOlustur(SharedPreferences pref) {
        int genelSimge = pref != null ? pref.getInt(PREF_SIMGE_BOYUTU, 100) : 100;
        return new YedekAnahtari[]{
            new YedekAnahtari("izgara_sutun", AyarBankasi.ANAHTAR_IZGARA_SUTUN, TIP_INT, 4),
            new YedekAnahtari("izgara_satir", AyarBankasi.ANAHTAR_IZGARA_SATIR, TIP_INT, 6),
            new YedekAnahtari("cekmece_izgara_sutun", AyarBankasi.ANAHTAR_CEKMECE_SUTUN, TIP_INT, 4),
            new YedekAnahtari("cekmece_izgara_satir", AyarBankasi.ANAHTAR_CEKMECE_SATIR, TIP_INT, 6),
            new YedekAnahtari("aktif_simge_paketi", SimgePaketiYoneticisi.ANAHTAR_AKTIF_PAKET, TIP_STRING, ""),
            new YedekAnahtari(PREF_ETIKETLERI_GOSTER, PREF_ETIKETLERI_GOSTER, TIP_BOOL, true),
            new YedekAnahtari(PREF_MASAUSTU_KILITLI, PREF_MASAUSTU_KILITLI, TIP_BOOL, false),
            new YedekAnahtari(PREF_DOCK_SIMGE_SAYISI, PREF_DOCK_SIMGE_SAYISI, TIP_INT, 4),
            new YedekAnahtari(PREF_SIMGE_BOYUTU, PREF_SIMGE_BOYUTU, TIP_INT, 100),
            new YedekAnahtari(PREF_MASAUSTU_SIMGE_BOYUTU, PREF_MASAUSTU_SIMGE_BOYUTU, TIP_INT, genelSimge),
            new YedekAnahtari(PREF_CEKMECE_SIMGE_BOYUTU, PREF_CEKMECE_SIMGE_BOYUTU, TIP_INT, genelSimge),
            new YedekAnahtari(PREF_DOCK_SIMGE_BOYUTU, PREF_DOCK_SIMGE_BOYUTU, TIP_INT, genelSimge),
            new YedekAnahtari(PREF_GECIS_EFEKTI, PREF_GECIS_EFEKTI, TIP_INT, SayfaliMasaustuGorunumu.EFEKT_STANDART),
            new YedekAnahtari(TemaYoneticisi.PREF_VURGU_RENGI, TemaYoneticisi.PREF_VURGU_RENGI, TIP_INT, TemaYoneticisi.RENK_KIRMIZI),
            new YedekAnahtari(BildirimYoneticisi.PREF_BILDIRIM_ROZETI_TURU, BildirimYoneticisi.PREF_BILDIRIM_ROZETI_TURU, TIP_INT, BildirimYoneticisi.ROZET_SAYI),
            new YedekAnahtari(MasaustuYoneticisi.PREF_SAYFA_SAYISI, MasaustuYoneticisi.PREF_SAYFA_SAYISI, TIP_INT, 3),
            new YedekAnahtari(MasaustuYoneticisi.PREF_SONSUZ_KAYDIRMA, MasaustuYoneticisi.PREF_SONSUZ_KAYDIRMA, TIP_BOOL, false),
            new YedekAnahtari(PREF_GIZLENENLER_CSV, PREF_GIZLENENLER_CSV, TIP_STRING, ""),
            new YedekAnahtari(MasaustuYoneticisi.PREF_MASAUSTU_SAYFALARI_JSON, MasaustuYoneticisi.PREF_MASAUSTU_SAYFALARI_JSON, TIP_STRING, "{}"),
            new YedekAnahtari(MasaustuYoneticisi.PREF_MASAUSTU_MODU, MasaustuYoneticisi.PREF_MASAUSTU_MODU, TIP_INT, MasaustuYoneticisi.MOD_KLASIK_CIFTE_KATMAN),
            new YedekAnahtari(MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFALARI_JSON, MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFALARI_JSON, TIP_STRING, "{}"),
            new YedekAnahtari(MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFA_SAYISI, MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFA_SAYISI, TIP_INT, 1),
            new YedekAnahtari("masaustu_widgetlar_json", "masaustu_widgetlar_json", TIP_STRING, "[]"),
            new YedekAnahtari("tamekran_klasorler_json", "tamekran_klasorler_json", TIP_STRING, "[]"),
            new YedekAnahtari("ozel_simgeler_atamalari_json", "ozel_simgeler_atamalari_json", TIP_STRING, "{}"),
            new YedekAnahtari(PREF_DOCK_GORUNUR, PREF_DOCK_GORUNUR, TIP_BOOL, true),
            new YedekAnahtari(KisayolYoneticisi.PREF_KISAYOLLAR, KisayolYoneticisi.PREF_KISAYOLLAR, TIP_STRING, "[]"),
            new YedekAnahtari(PREF_CEKMECE_SAYDAMLIK, PREF_CEKMECE_SAYDAMLIK, TIP_INT, 100),
            new YedekAnahtari(AramaMotoruYoneticisi.PREF_ARAMA_MOTORU, AramaMotoruYoneticisi.PREF_ARAMA_MOTORU, TIP_INT, AramaMotoruYoneticisi.MOTOR_YEREL),
            new YedekAnahtari(CekmeceSiralamasi.PREF_SIRALAMA, CekmeceSiralamasi.PREF_SIRALAMA, TIP_INT, CekmeceSiralamasi.SIRALAMA_A_TO_Z),
            new YedekAnahtari(KlasorSimgeUretici.PREF_KLASOR_SEKLI, KlasorSimgeUretici.PREF_KLASOR_SEKLI, TIP_INT, KlasorSimgeUretici.SEKIL_YUVARLATILMIS),
            new YedekAnahtari(SallamaAlgilayici.PREF_SALLAMA_EYLEMI, SallamaAlgilayici.PREF_SALLAMA_EYLEMI, TIP_INT, SallamaAlgilayici.EYLEM_KAPALI),
            new YedekAnahtari(YaziStiliYoneticisi.PREF_YAZI_STILI, YaziStiliYoneticisi.PREF_YAZI_STILI, TIP_INT, YaziStiliYoneticisi.STIL_NORMAL),
            new YedekAnahtari(YaziStiliYoneticisi.PREF_METIN_GOLGESI, YaziStiliYoneticisi.PREF_METIN_GOLGESI, TIP_BOOL, true),
            new YedekAnahtari(DockArkaPlanYoneticisi.PREF_DOCK_STILI, DockArkaPlanYoneticisi.PREF_DOCK_STILI, TIP_INT, DockArkaPlanYoneticisi.STIL_KAYAN_HAP),
            new YedekAnahtari(PaketDegisimAlicisi.PREF_OTOMATIK_MASAUSTUNE_EKLE, PaketDegisimAlicisi.PREF_OTOMATIK_MASAUSTUNE_EKLE, TIP_BOOL, true),
            new YedekAnahtari(PREF_MASAUSTU_DOLGU, PREF_MASAUSTU_DOLGU, TIP_INT, 1),
            new YedekAnahtari(GuvenliAlanYoneticisi.PREF_GUVENLI_UST, GuvenliAlanYoneticisi.PREF_GUVENLI_UST, TIP_INT, 0),
            new YedekAnahtari(GuvenliAlanYoneticisi.PREF_GUVENLI_ALT, GuvenliAlanYoneticisi.PREF_GUVENLI_ALT, TIP_INT, 0),
            new YedekAnahtari(GuvenliAlanYoneticisi.PREF_GUVENLI_SOL, GuvenliAlanYoneticisi.PREF_GUVENLI_SOL, TIP_INT, 0),
            new YedekAnahtari(GuvenliAlanYoneticisi.PREF_GUVENLI_SAG, GuvenliAlanYoneticisi.PREF_GUVENLI_SAG, TIP_INT, 0),
            new YedekAnahtari(PREF_DUVAR_KAGIDI_KAYDIRMA, PREF_DUVAR_KAGIDI_KAYDIRMA, TIP_BOOL, true),
            new YedekAnahtari(PREF_DOCK_YUKSEKLIK, PREF_DOCK_YUKSEKLIK, TIP_INT, 72),
            new YedekAnahtari(PREF_DOCK_KAYDIRILABILIR, PREF_DOCK_KAYDIRILABILIR, TIP_BOOL, false),
            new YedekAnahtari(KlasorKapakYoneticisi.PREF_KLASOR_KAPAK_MODU_GENEL, KlasorKapakYoneticisi.PREF_KLASOR_KAPAK_MODU_GENEL, TIP_BOOL, false),
            new YedekAnahtari(KlasorKapakYoneticisi.PREF_OZEL_KAPAK_KLASORLERI, KlasorKapakYoneticisi.PREF_OZEL_KAPAK_KLASORLERI, TIP_STRING, "[]"),
            new YedekAnahtari(KlasorKapakYoneticisi.PREF_OZEL_KAPAK_KAPALI_KLASORLERI, KlasorKapakYoneticisi.PREF_OZEL_KAPAK_KAPALI_KLASORLERI, TIP_STRING, "[]"),
            new YedekAnahtari(SimgeNormalizasyonYoneticisi.PREF_SIMGE_NORMALIZASYONU, SimgeNormalizasyonYoneticisi.PREF_SIMGE_NORMALIZASYONU, TIP_BOOL, false),
            new YedekAnahtari(SimgeNormalizasyonYoneticisi.PREF_SIMGE_SEKLI, SimgeNormalizasyonYoneticisi.PREF_SIMGE_SEKLI, TIP_INT, SimgeNormalizasyonYoneticisi.SEKIL_ORIJINAL),
            new YedekAnahtari(SimgeKaydirmaYoneticisi.PREF_KAYDIRMA_EYLEMLERI, SimgeKaydirmaYoneticisi.PREF_KAYDIRMA_EYLEMLERI, TIP_STRING, "{}"),
            new YedekAnahtari("ozel_kategoriler_json", "ozel_kategoriler_json", TIP_STRING, "[]"),
            new YedekAnahtari("uygulama_kategori_atamalari_json", "uygulama_kategori_atamalari_json", TIP_STRING, "{}"),
            new YedekAnahtari(DockYoneticisi.PREF_DOCK_UYGULAMALARI_JSON, DockYoneticisi.PREF_DOCK_UYGULAMALARI_JSON, TIP_STRING, "[]"),
            new YedekAnahtari(MasaustuModelDeposu.PREF_MODEL_V2_KLASIK, MasaustuModelDeposu.PREF_MODEL_V2_KLASIK, TIP_STRING, ""),
            new YedekAnahtari(MasaustuModelDeposu.PREF_MODEL_V2_TEK_KATMAN, MasaustuModelDeposu.PREF_MODEL_V2_TEK_KATMAN, TIP_STRING, ""),
            new YedekAnahtari(HareketYoneticisi.PREF_HAREKET_ANA_EKRAN, HareketYoneticisi.PREF_HAREKET_ANA_EKRAN, TIP_INT, HareketYoneticisi.EYLEM_YOK),
            new YedekAnahtari(HareketYoneticisi.PREF_HAREKET_GERI_TUSU, HareketYoneticisi.PREF_HAREKET_GERI_TUSU, TIP_INT, HareketYoneticisi.EYLEM_YOK)
        };
    }

    public static boolean yedegiDogrula(JSONObject json) {
        if (json == null || json.length() == 0) return false;
        if (json.has("schemaVersion")) {
            int v = json.optInt("schemaVersion", -1);
            if (v < 1) return false;
        }
        int eslesenAnahtar = 0;
        for (YedekAnahtari a : yedekSemasiOlustur(null)) {
            if (!json.has(a.jsonAnahtari)) continue;
            eslesenAnahtar++;
            try {
                if (a.tip == TIP_INT) {
                    json.getInt(a.jsonAnahtari);
                } else if (a.tip == TIP_BOOL) {
                    json.getBoolean(a.jsonAnahtari);
                } else {
                    json.getString(a.jsonAnahtari);
                }
            } catch (Exception e) {
                return false;
            }
        }
        return eslesenAnahtar > 0;
    }

    public static boolean yedekAl(Context context) {
        try {
            SharedPreferences pref = AyarBankasi.getAyarlar(context);
            JSONObject json = new JSONObject();
            json.put("schemaVersion", 2);

            for (YedekAnahtari a : yedekSemasiOlustur(pref)) {
                if (a.tip == TIP_INT) {
                    json.put(a.jsonAnahtari, pref.getInt(a.prefAnahtari, ((Integer) a.varsayilan).intValue()));
                } else if (a.tip == TIP_BOOL) {
                    json.put(a.jsonAnahtari, pref.getBoolean(a.prefAnahtari, ((Boolean) a.varsayilan).booleanValue()));
                } else {
                    json.put(a.jsonAnahtari, pref.getString(a.prefAnahtari, (String) a.varsayilan));
                }
            }

            File dosya = yedekDosyasi(context);
            atomikYaz(dosya, json.toString(2).getBytes("UTF-8"));
            return true;
        } catch (Exception e) {
            LauncherLog.e("YedeklemeYoneticisi", "yedekAl hatasi", e);
            return false;
        }
    }

    public static boolean yedeğiYukle(Context context) {
        try {
            File dosya = yedekDosyasi(context);
            if (!dosya.exists()) return false;

            FileInputStream fis = new FileInputStream(dosya);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String satir;
            while ((satir = reader.readLine()) != null) {
                sb.append(satir);
            }
            reader.close();

            JSONObject json = new JSONObject(sb.toString());
            if (!yedegiDogrula(json)) return false;
            SharedPreferences pref = AyarBankasi.getAyarlar(context);
            SharedPreferences.Editor editor = pref.edit();

            for (YedekAnahtari a : yedekSemasiOlustur(pref)) {
                if (!json.has(a.jsonAnahtari)) continue;
                if (a.tip == TIP_INT) {
                    editor.putInt(a.prefAnahtari, json.getInt(a.jsonAnahtari));
                } else if (a.tip == TIP_BOOL) {
                    editor.putBoolean(a.prefAnahtari, json.getBoolean(a.jsonAnahtari));
                } else {
                    editor.putString(a.prefAnahtari, json.getString(a.jsonAnahtari));
                }
            }

            if (json.has("oy") || json.length() >= 0) {
                if (!editor.commit()) {
                    return false;
                }
                boolean v2Var = json.has(MasaustuModelDeposu.PREF_MODEL_V2_KLASIK)
                        || json.has(MasaustuModelDeposu.PREF_MODEL_V2_TEK_KATMAN);
                if (!v2Var) {
                    MasaustuModelDeposu.v2ModeliniSifirla(context);
                }
                AyarBankasi.degisiklikDamgasiGuncelle(context);
            }
            return true;
        } catch (Exception e) {
            LauncherLog.e("YedeklemeYoneticisi", "Yedek geri yukleme basarisiz", e);
            return false;
        }
    }

    /**
     * Yedek dosyasını WhatsApp, Telegram, Drive veya Bluetooth ile dışa aktarır
     */
    public static void yedegiPaylas(Activity activity) {
        if (!yedekAl(activity)) {
            ArayuzYardimcisi.toast(activity, "Yedek dosyası oluşturulamadı.");
            return;
        }

        File dosya = yedekDosyasi(activity);
        if (!dosya.exists()) {
            ArayuzYardimcisi.toast(activity, "Yedek dosyası bulunamadı.");
            return;
        }

        try {
            Uri dosyaUri = DosyaSaglayici.getUriForFile(activity, dosya);
            Intent niyet = new Intent(Intent.ACTION_SEND);
            niyet.setType("application/json");
            niyet.putExtra(Intent.EXTRA_STREAM, dosyaUri);
            niyet.putExtra(Intent.EXTRA_SUBJECT, "Tam Ekran Başlatıcı Yedeği");
            niyet.putExtra(Intent.EXTRA_TEXT, "Tam Ekran başlatıcı yedek dosyam (TamEkran_Yedek.json).");
            niyet.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            activity.startActivity(Intent.createChooser(niyet, "Yedek Dosyasını Paylaş"));
        } catch (Exception e) {
            ArayuzYardimcisi.toast(activity, "Paylaşma başlatılamadı: " + e.getMessage());
        }
    }

    /**
     * Dışarıdan seçilen bir JSON dosyasından yedeği doğrulayarak içe aktarır (önce doğrula, sonra üzerine yaz).
     */
    public static boolean dosyaIcerigindenYukle(Context context, Uri dosyaUri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(dosyaUri);
            if (is == null) return false;

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String satir;
            while ((satir = reader.readLine()) != null) {
                sb.append(satir);
            }
            reader.close();
            is.close();

            String icerik = sb.toString();
            JSONObject dogrulanan = new JSONObject(icerik); // Geçerli JSON ve şema değilse yerel yedeği ezmeden durdur
            if (!yedegiDogrula(dogrulanan)) {
                return false;
            }

            File yerel = yedekDosyasi(context);
            atomikYaz(yerel, icerik.getBytes("UTF-8"));

            return yedeğiYukle(context);
        } catch (Exception e) {
            LauncherLog.e("YedeklemeYoneticisi", "dosyaIcerigindenYukle hatasi", e);
            return false;
        }
    }

    public static boolean indirilenlereDisaAktar(Context context) {
        if (!yedekAl(context)) return false;
        try {
            File kaynak = yedekDosyasi(context);
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloads.exists()) downloads.mkdirs();
            File hedef = new File(downloads, "TamEkran_Yedek.json");

            byte[] veri = okuDosyaByte(kaynak);
            atomikYaz(hedef, veri);
            return true;
        } catch (Exception e) {
            LauncherLog.e("YedeklemeYoneticisi", "indirilenlereDisaAktar hatasi", e);
            return false;
        }
    }

    public static boolean indirilenlerdenYukle(Context context) {
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dosya = new File(downloads, "TamEkran_Yedek.json");
            if (!dosya.exists()) return false;

            byte[] veri = okuDosyaByte(dosya);
            new JSONObject(new String(veri, "UTF-8")); // Geçersiz JSON ise yerel yedeği ezme

            File yerel = yedekDosyasi(context);
            atomikYaz(yerel, veri);

            return yedeğiYukle(context);
        } catch (Exception e) {
            LauncherLog.e("YedeklemeYoneticisi", "indirilenlerdenYukle hatasi", e);
            return false;
        }
    }

    private static byte[] okuDosyaByte(File dosya) throws Exception {
        FileInputStream fis = new FileInputStream(dosya);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        } finally {
            try { fis.close(); } catch (Exception ignored) {}
        }
    }
}

