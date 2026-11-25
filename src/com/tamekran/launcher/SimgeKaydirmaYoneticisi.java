package com.tamekran.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Simgelerde Yukarı Kaydırma Eylemi Yöneticisi (Nova Swipe Action)
 * Masaüstündeki simgelerin üzerine yukarı kaydırıldığında ikincil bir uygulama veya sistem eylemi çalıştırır.
 */
public class SimgeKaydirmaYoneticisi {

    public static final String PREF_KAYDIRMA_EYLEMLERI = "simge_kaydirma_eylemleri_json";

    public static final int EYLEM_YOK = 0;
    public static final int EYLEM_UYGULAMA = 1;
    public static final int EYLEM_BILDIRIMLER = 2;
    public static final int EYLEM_HIZLI_AYARLAR = 3;
    public static final int EYLEM_CEKMECE = 4;
    public static final int EYLEM_KILITLE = 5;
    public static final int EYLEM_SESLI_ARAMA = 6;
    public static final int EYLEM_AYARLAR = 7;
    public static final int EYLEM_FENER = 8;
    public static final int EYLEM_SES_MODU = 9;

    public static class KaydirmaBilgisi {
        public int eylemTuru = EYLEM_YOK;
        public String hedefPaket = "";
        public String hedefEtiket = "";
    }

    public static KaydirmaBilgisi getKaydirmaEylemi(Context context, String paketAdi) {
        KaydirmaBilgisi bilgi = new KaydirmaBilgisi();
        if (context == null || paketAdi == null) return bilgi;

        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String hamJson = sp.getString(PREF_KAYDIRMA_EYLEMLERI, "{}");
        try {
            JSONObject obj = new JSONObject(hamJson);
            if (obj.has(paketAdi)) {
                JSONObject item = obj.getJSONObject(paketAdi);
                bilgi.eylemTuru = item.optInt("tur", EYLEM_YOK);
                bilgi.hedefPaket = item.optString("hedefPaket", "");
                bilgi.hedefEtiket = item.optString("hedefEtiket", "");
            }
        } catch (Throwable t) {
            LauncherLog.w("SimgeKaydirmaYoneticisi", "getKaydirmaEylemi JSON okuma hatasi", t);
        }

        return bilgi;
    }

    public static void setKaydirmaEylemi(Context context, String paketAdi, int eylemTuru, String hedefPaket, String hedefEtiket) {
        if (context == null || paketAdi == null) return;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String hamJson = sp.getString(PREF_KAYDIRMA_EYLEMLERI, "{}");
        try {
            JSONObject obj = new JSONObject(hamJson);
            if (eylemTuru == EYLEM_YOK) {
                obj.remove(paketAdi);
            } else {
                JSONObject item = new JSONObject();
                item.put("tur", eylemTuru);
                item.put("hedefPaket", hedefPaket != null ? hedefPaket : "");
                item.put("hedefEtiket", hedefEtiket != null ? hedefEtiket : "");
                obj.put(paketAdi, item);
            }
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_KAYDIRMA_EYLEMLERI, obj.toString()));
        } catch (Throwable t) {
            LauncherLog.e("SimgeKaydirmaYoneticisi", "setKaydirmaEylemi JSON yazma hatasi", t);
        }
    }

    public static String getEylemOzeti(Context context, String paketAdi) {
        KaydirmaBilgisi b = getKaydirmaEylemi(context, paketAdi);
        switch (b.eylemTuru) {
            case EYLEM_UYGULAMA:
                return b.hedefEtiket.length() > 0 ? ("Uygulama: " + b.hedefEtiket) : "İkincil Uygulama";
            case EYLEM_BILDIRIMLER:
                return "Bildirimler";
            case EYLEM_HIZLI_AYARLAR:
                return "Hızlı Ayarlar";
            case EYLEM_CEKMECE:
                return "Uygulama Menüsü";
            case EYLEM_KILITLE:
                return "Ekranı Kilitle";
            case EYLEM_SESLI_ARAMA:
                return "Sesli Arama";
            case EYLEM_AYARLAR:
                return "Başlatıcı Ayarları";
            case EYLEM_FENER:
                return "Fener";
            case EYLEM_SES_MODU:
                return "Ses Profili";
            case EYLEM_YOK:
            default:
                return "Tanımsız";
        }
    }

    /**
     * Masaüstü simgesinde yukarı kaydırma yapıldığında eylemi çalıştırır.
     * @return Bir eylem çalıştırıldıysa true döner.
     */
    public static boolean eylemiCalistir(AnaEkranActivity aktivite, String paketAdi) {
        if (aktivite == null || paketAdi == null) return false;
        KaydirmaBilgisi b = getKaydirmaEylemi(aktivite, paketAdi);
        if (b.eylemTuru == EYLEM_YOK) return false;

        switch (b.eylemTuru) {
            case EYLEM_UYGULAMA:
                if (b.hedefPaket != null && b.hedefPaket.length() > 0) {
                    try {
                        Intent niyet = aktivite.getPackageManager().getLaunchIntentForPackage(b.hedefPaket);
                        if (niyet != null) {
                            UygulamaBilgisi hedefApp = new UygulamaBilgisi("", b.hedefPaket, null, niyet);
                            ArayuzYardimcisi.guvenliUygulamaBaslat(aktivite, hedefApp, null);
                            return true;
                        }
                    } catch (Throwable t) {
                        ArayuzYardimcisi.toast(aktivite, "Hedef uygulama başlatılamadı.");
                    }
                }
                return false;

            case EYLEM_BILDIRIMLER:
                HareketYoneticisi.eylemiCalistir(aktivite, HareketYoneticisi.EYLEM_BILDIRIMLER);
                return true;

            case EYLEM_HIZLI_AYARLAR:
                HareketYoneticisi.eylemiCalistir(aktivite, HareketYoneticisi.EYLEM_HIZLI_AYARLAR);
                return true;

            case EYLEM_CEKMECE:
                HareketYoneticisi.eylemiCalistir(aktivite, HareketYoneticisi.EYLEM_CEKMECE);
                return true;

            case EYLEM_KILITLE:
                HareketYoneticisi.eylemiCalistir(aktivite, HareketYoneticisi.EYLEM_KILITLE);
                return true;

            case EYLEM_SESLI_ARAMA:
                try {
                    Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_WEB_SEARCH);
                    intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    aktivite.startActivity(intent);
                    return true;
                } catch (Throwable t) {
                    return false;
                }

            case EYLEM_AYARLAR:
                HareketYoneticisi.eylemiCalistir(aktivite, HareketYoneticisi.EYLEM_AYARLAR);
                return true;

            case EYLEM_FENER:
                FenerYoneticisi.toggle(aktivite);
                return true;

            case EYLEM_SES_MODU:
                SesModuYoneticisi.sonrakiModaGec(aktivite);
                return true;
        }

        return false;
    }

    /**
     * Kullanıcıya simge için yukarı kaydırma eylemi seçtiren diyalog
     */
    public static void secimDiyalogu(
        final Context context,
        final String paketAdi,
        final String etiket,
        final List<UygulamaBilgisi> tumUygulamalar,
        final Runnable guncellemeSonrasi
    ) {
        if (context == null || paketAdi == null) return;

        CharSequence[] secenekler = new CharSequence[]{
            "❌ Kaydırma Eylemini Kaldır (Kapalı)",
            "📱 Başka Bir Uygulama Başlat...",
            "🔔 Bildirim Panelini Aç",
            "⚙️ Hızlı Ayarlar Panelini Aç",
            "📂 Tüm Uygulamalar Menüsünü Aç",
            "🔒 Ekranı Kilitle",
            "🎙️ Sesli Aramayı Başlat",
            "🛠️ Başlatıcı Ayarlarını Aç",
            "🔦 Feneri Aç / Kapat",
            "🔊 Ses Modunu Değiştir (Sessiz/Titreşim/Normal)"
        };

        new AlertDialog.Builder(context)
            .setTitle("👆 Yukarı Kaydırma: " + etiket)
            .setItems(secenekler, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        setKaydirmaEylemi(context, paketAdi, EYLEM_YOK, "", "");
                        ArayuzYardimcisi.toast(context, "Kaydırma eylemi kaldırıldı.");
                        if (guncellemeSonrasi != null) guncellemeSonrasi.run();
                    } else if (which == 1) {
                        // Uygulama seçimi
                        uygulamaSecimDiyalogu(context, paketAdi, tumUygulamalar, guncellemeSonrasi);
                    } else {
                        int eylem = EYLEM_YOK;
                        if (which == 2) eylem = EYLEM_BILDIRIMLER;
                        else if (which == 3) eylem = EYLEM_HIZLI_AYARLAR;
                        else if (which == 4) eylem = EYLEM_CEKMECE;
                        else if (which == 5) eylem = EYLEM_KILITLE;
                        else if (which == 6) eylem = EYLEM_SESLI_ARAMA;
                        else if (which == 7) eylem = EYLEM_AYARLAR;
                        else if (which == 8) eylem = EYLEM_FENER;
                        else if (which == 9) eylem = EYLEM_SES_MODU;

                        setKaydirmaEylemi(context, paketAdi, eylem, "", "");
                        ArayuzYardimcisi.toast(context, "Yukarı kaydırma eylemi atandı.");
                        if (guncellemeSonrasi != null) guncellemeSonrasi.run();
                    }
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private static void uygulamaSecimDiyalogu(
        final Context context,
        final String paketAdi,
        final List<UygulamaBilgisi> tumUygulamalar,
        final Runnable guncellemeSonrasi
    ) {
        if (tumUygulamalar == null || tumUygulamalar.isEmpty()) return;

        final List<UygulamaBilgisi> adaylar = new ArrayList<UygulamaBilgisi>();
        for (UygulamaBilgisi app : tumUygulamalar) {
            if (!app.paketAdi.equals(paketAdi)) {
                adaylar.add(app);
            }
        }

        String[] basliklar = new String[adaylar.size()];
        for (int i = 0; i < adaylar.size(); i++) {
            basliklar[i] = adaylar.get(i).etiket;
        }

        new AlertDialog.Builder(context)
            .setTitle("👆 Açılacak Uygulamayı Seçin")
            .setItems(basliklar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    UygulamaBilgisi secilen = adaylar.get(which);
                    setKaydirmaEylemi(context, paketAdi, EYLEM_UYGULAMA, secilen.paketAdi, secilen.etiket);
                    ArayuzYardimcisi.toast(context, "Yukarı kaydırınca açılacak: " + secilen.etiket);
                    if (guncellemeSonrasi != null) guncellemeSonrasi.run();
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }
}

