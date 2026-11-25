package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * Tam Ekran - Uygulama Kilidi ve PIN Koruması
 * Hassas uygulamaları (Galeri, Bankacılık, WhatsApp vb.) 4 haneli PIN ile kilitler.
 * %100 yerel, sıfır telemetri, saf Java ve Android SDK.
 */
public class UygulamaKilidiYoneticisi {

    private static final String ANAHTAR_PIN = "guvenlik_pin_kodu";
    private static final String ANAHTAR_KILITLI_PAKETLER = "kilitli_paketler_seti";
    private static final String ANAHTAR_HATALI_DENEME = "pin_hatali_deneme_sayisi";
    private static final String ANAHTAR_KILIT_BITIS = "pin_gecici_kilit_bitis";
    private static final int MAKS_HATALI_DENEME = 5;
    private static final long GECICI_KILIT_MS = 30000L;

    public static boolean pinAyarliMi(Context context) {
        if (!AyarBankasi.baglamGecerliMi(context)) return false;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return false;
        String pin = sp.getString(ANAHTAR_PIN, "");
        return pin != null && (pin.startsWith("v2:") || pin.trim().length() >= 4);
    }

    public static String pinHashUret(String tuz, String pin) {
        if (tuz == null || pin == null) return "";
        String temizPin = pin.trim();
        String ozet = sha256Hex(tuz + ":" + temizPin);
        return "v2:" + tuz + ":" + ozet;
    }

    public static boolean pinHashDogrula(String saklanan, String girilen) {
        if (saklanan == null || girilen == null) return false;
        String temiz = girilen.trim();
        if (saklanan.startsWith("v2:")) {
            String[] parcalar = saklanan.split(":");
            if (parcalar.length != 3) return false;
            String beklenen = pinHashUret(parcalar[1], temiz);
            return sabitZamanEsitMi(saklanan, beklenen);
        }
        return temiz.length() >= 4 && sabitZamanEsitMi(saklanan.trim(), temiz);
    }

    private static String yeniTuzUret() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String sha256Hex(String girdi) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(girdi.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean sabitZamanEsitMi(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int fark = 0;
        for (int i = 0; i < a.length(); i++) {
            fark |= (a.charAt(i) ^ b.charAt(i));
        }
        return fark == 0;
    }

    public static long kalanKilitlemeSuresiMs(Context context) {
        if (!AyarBankasi.baglamGecerliMi(context)) return 0L;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return 0L;
        long bitis = sp.getLong(ANAHTAR_KILIT_BITIS, 0L);
        long simdi = System.currentTimeMillis();
        return bitis > simdi ? (bitis - simdi) : 0L;
    }

    public static boolean pinDogrula(Context context, String girilenPin) {
        if (!AyarBankasi.baglamGecerliMi(context) || girilenPin == null) return false;
        if (kalanKilitlemeSuresiMs(context) > 0L) return false;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return false;
        String saklanan = sp.getString(ANAHTAR_PIN, "");
        boolean eslesti = pinHashDogrula(saklanan, girilenPin);
        if (eslesti) {
            SharedPreferences.Editor ed = sp.edit().putInt(ANAHTAR_HATALI_DENEME, 0).putLong(ANAHTAR_KILIT_BITIS, 0L);
            if (!saklanan.startsWith("v2:")) {
                ed.putString(ANAHTAR_PIN, pinHashUret(yeniTuzUret(), girilenPin));
            }
            AyarBankasi.editorKaydet(ed);
            return true;
        }
        int hatalar = sp.getInt(ANAHTAR_HATALI_DENEME, 0) + 1;
        SharedPreferences.Editor ed = sp.edit().putInt(ANAHTAR_HATALI_DENEME, hatalar);
        if (hatalar >= MAKS_HATALI_DENEME) {
            ed.putLong(ANAHTAR_KILIT_BITIS, System.currentTimeMillis() + GECICI_KILIT_MS);
            ed.putInt(ANAHTAR_HATALI_DENEME, 0);
        }
        AyarBankasi.editorKaydet(ed);
        return false;
    }

    public static void pinBelirle(Context context, String yeniPin) {
        if (!AyarBankasi.baglamGecerliMi(context) || yeniPin == null) return;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return;
        String hashed = pinHashUret(yeniTuzUret(), yeniPin);
        AyarBankasi.editorKaydet(sp.edit()
                .putString(ANAHTAR_PIN, hashed)
                .putInt(ANAHTAR_HATALI_DENEME, 0)
                .putLong(ANAHTAR_KILIT_BITIS, 0L));
    }

    public static void pinSifirla(Context context) {
        if (context == null) return;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return;
        AyarBankasi.editorKaydet(sp.edit().remove(ANAHTAR_PIN).remove(ANAHTAR_KILITLI_PAKETLER));
    }

    public static Set<String> kilitliPaketleriAl(Context context) {
        if (!AyarBankasi.baglamGecerliMi(context)) return new HashSet<String>();
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return new HashSet<String>();
        try {
            String ham = sp.getString(ANAHTAR_KILITLI_PAKETLER, null);
            if (ham != null) {
                return new HashSet<String>(JsonYardimcisi.jsonToStringListesi(ham));
            }
        } catch (ClassCastException cce) {
            if (android.os.Build.VERSION.SDK_INT >= 11) {
                Set<String> eskiSet = sp.getStringSet(ANAHTAR_KILITLI_PAKETLER, null);
                if (eskiSet != null) {
                    Set<String> kopya = new HashSet<String>(eskiSet);
                    kilitliPaketleriKaydet(sp, kopya);
                    return kopya;
                }
            }
        }
        return new HashSet<String>();
    }

    private static void kilitliPaketleriKaydet(SharedPreferences sp, Set<String> set) {
        if (sp == null) return;
        String json = JsonYardimcisi.stringListesiToJson(new java.util.ArrayList<String>(set));
        AyarBankasi.editorKaydet(sp.edit().putString(ANAHTAR_KILITLI_PAKETLER, json));
    }

    public static boolean kilitliMi(Context context, String paketAdi) {
        if (!AyarBankasi.baglamGecerliMi(context) || paketAdi == null) return false;
        if (!pinAyarliMi(context)) return false;
        Set<String> kilitliler = kilitliPaketleriAl(context);
        return kilitliler.contains(paketAdi);
    }

    public static void kilitle(Context context, String paketAdi) {
        if (!AyarBankasi.baglamGecerliMi(context) || paketAdi == null) return;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return;
        Set<String> set = kilitliPaketleriAl(context);
        set.add(paketAdi);
        kilitliPaketleriKaydet(sp, set);
    }

    public static void kilidiKaldir(Context context, String paketAdi) {
        if (!AyarBankasi.baglamGecerliMi(context) || paketAdi == null) return;
        SharedPreferences sp = AyarBankasi.getOzelAyarlar(context, AyarBankasi.PREF_KILIT_DOSYA);
        if (sp == null) return;
        Set<String> set = kilitliPaketleriAl(context);
        set.remove(paketAdi);
        kilitliPaketleriKaydet(sp, set);
    }

    public static void kilitDurumuDegistir(Context context, String paketAdi) {
        if (kilitliMi(context, paketAdi)) {
            kilidiKaldir(context, paketAdi);
        } else {
            kilitle(context, paketAdi);
        }
    }

    /**
     * Kilitli bir uygulama açılırken PIN sorar ve doğrulanırsa eylemi çalıştırır
     */
    public static void pinSorgulaVeCalistir(final Activity activity, final Runnable basariliGorev) {
        if (activity == null || basariliGorev == null) return;

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = ArayuzYardimcisi.dpToPx(activity, 20);
        layout.setPadding(pad, pad, pad, pad);

        TextView aciklama = new TextView(activity);
        aciklama.setText("Bu uygulama kilitli. Devam etmek için 4 haneli PIN kodunuzu girin:");
        aciklama.setTextSize(14);
        aciklama.setTextColor(Color.WHITE);
        layout.addView(aciklama);

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(22);
        input.setTextColor(Color.WHITE);
        input.setHint("••••");
        input.setHintTextColor(Color.GRAY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, pad / 2, 0, 0);
        input.setLayoutParams(lp);
        layout.addView(input);

        new AlertDialog.Builder(activity)
            .setTitle("🔒 Güvenlik Kilidi")
            .setView(layout)
            .setPositiveButton("Aç", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    long kalanMs = kalanKilitlemeSuresiMs(activity);
                    if (kalanMs > 0L) {
                        long sn = Math.max(1L, (kalanMs + 999L) / 1000L);
                        ArayuzYardimcisi.toast(activity, "Çok fazla hatalı deneme. " + sn + " sn sonra tekrar deneyin.");
                        return;
                    }
                    String girilen = input.getText().toString();
                    if (pinDogrula(activity, girilen)) {
                        basariliGorev.run();
                    } else {
                        ArayuzYardimcisi.toast(activity, "Hatalı PIN kodu!");
                    }
                }
            })
            .setNegativeButton("İptal", null)
            .show();
    }

    /**
     * Kullanıcı ilk defa bir uygulamayı kilitlerken veya Ayarlar'dan yeni PIN belirleme diyalogu
     */
    public static void yeniPinBelirleDiyalogu(final Activity activity, final Runnable tamamlandiGorevi) {
        if (activity == null) return;

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = ArayuzYardimcisi.dpToPx(activity, 20);
        layout.setPadding(pad, pad, pad, pad);

        TextView aciklama = new TextView(activity);
        aciklama.setText("Uygulamalarınızı korumak için 4 haneli yeni bir PIN belirleyin:");
        aciklama.setTextSize(14);
        aciklama.setTextColor(Color.WHITE);
        layout.addView(aciklama);

        final EditText input1 = new EditText(activity);
        input1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input1.setGravity(Gravity.CENTER);
        input1.setTextSize(20);
        input1.setTextColor(Color.WHITE);
        input1.setHint("Yeni PIN (en az 4 hane)");
        input1.setHintTextColor(Color.GRAY);
        layout.addView(input1);

        final EditText input2 = new EditText(activity);
        input2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input2.setGravity(Gravity.CENTER);
        input2.setTextSize(20);
        input2.setTextColor(Color.WHITE);
        input2.setHint("PIN'i Tekrar Girin");
        input2.setHintTextColor(Color.GRAY);
        layout.addView(input2);

        new AlertDialog.Builder(activity)
            .setTitle("🔐 Yeni PIN Belirle")
            .setView(layout)
            .setPositiveButton("Kaydet", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String p1 = input1.getText().toString().trim();
                    String p2 = input2.getText().toString().trim();
                    if (p1.length() < 4) {
                        ArayuzYardimcisi.toast(activity, "PIN en az 4 haneli olmalıdır.");
                        return;
                    }
                    if (!p1.equals(p2)) {
                        ArayuzYardimcisi.toast(activity, "Girdiğiniz PIN kodları uyuşmuyor!");
                        return;
                    }
                    pinBelirle(activity, p1);
                    ArayuzYardimcisi.toast(activity, "Güvenlik PIN'i başarıyla kaydedildi.");
                    if (tamamlandiGorevi != null) {
                        tamamlandiGorevi.run();
                    }
                }
            })
            .setNegativeButton("İptal", null)
            .show();
    }
}
