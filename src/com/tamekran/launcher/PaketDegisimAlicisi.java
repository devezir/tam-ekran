package com.tamekran.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.List;

/**
 * Tam Ekran - Paket Değişim Alıcısı (Package Add/Remove Engine)
 * Yeni bir uygulama yüklendiğinde veya silindiğinde başlatıcıyı otomatik günceller.
 * İsteğe bağlı olarak yeni yüklenen uygulamayı masaüstüne otomatik ekler.
 * Silinen uygulamaları masaüstünden, klasörlerden ve kilit listesinden temizler.
 */
public class PaketDegisimAlicisi extends BroadcastReceiver {

    public static final String PREF_OTOMATIK_MASAUSTUNE_EKLE = "yeni_uygulamayi_masaustune_ekle";
    public static volatile boolean paketDegisikligiOldu = false;

    public interface PaketDegisimDinleyici {
        void onPaketlerDegisti();
    }

    private static PaketDegisimDinleyici dinleyici;

    public static void setDinleyici(PaketDegisimDinleyici d) {
        dinleyici = d;
    }

    public static void paketiSistemdenTemizle(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return;

        // 1-3. Masaüstü, Dock ve Klasörlerdeki tüm kopyaları tek atomik işlemde temizle
        MasaustuModelDeposu.paketTumKopyalariSil(context, paketAdi);

        // 4. Kilit listesinden kaldır
        UygulamaKilidiYoneticisi.kilidiKaldir(context, paketAdi);

        // 5. Gizlenenlerden kaldır
        List<String> gizliler = YedeklemeYoneticisi.gizlenenleriAl(context);
        if (gizliler.contains(paketAdi)) {
            gizliler.remove(paketAdi);
            YedeklemeYoneticisi.gizlenenleriKaydet(context, gizliler);
        }

        // 6. Önbellekleri tazele
        SimgeOnbellegi.paketTemizle(paketAdi);
        KlasorSimgeUretici.onbellegiTemizle();

        paketDegisikligiOldu = true;
        if (dinleyici != null) {
            try {
                dinleyici.onPaketlerDegisti();
            } catch (Throwable t) {
                LauncherLog.w("PaketDegisimAlicisi", "dinleyici bildirim hatasi", t);
            }
        }
    }

    public static void paketiSistemeEkle(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return;

        SharedPreferences ayarlar = AyarBankasi.getAyarlar(context);
        if (MasaustuYoneticisi.isTekKatmanModu(context)) {
            // Tek katman (iOS) modunda tüm yeni uygulamalar otomatik olarak masaüstüne eklenir
            MasaustuYoneticisi.bostaHucreyeVeyaYeniSayfayaEkle(context, paketAdi);
        } else {
            boolean otoEkle = ayarlar.getBoolean(PREF_OTOMATIK_MASAUSTUNE_EKLE, true);
            if (otoEkle) {
                // Varsayılan sayfaya veya ilk sayfaya ekle
                int sayfa = MasaustuYoneticisi.getVarsayilanSayfa(context);
                MasaustuYoneticisi.sayfayaUygulamaEkle(context, sayfa, paketAdi);
            }
        }

        SimgeOnbellegi.paketTemizle(paketAdi);
        KlasorSimgeUretici.onbellegiTemizle();

        paketDegisikligiOldu = true;
        if (dinleyici != null) {
            try {
                dinleyici.onPaketlerDegisti();
            } catch (Throwable t) {
                LauncherLog.w("PaketDegisimAlicisi", "dinleyici bildirim hatasi", t);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        android.net.Uri data = intent.getData();
        String paketAdi = data != null ? data.getSchemeSpecificPart() : null;

        if (paketAdi == null || paketAdi.equals(context.getPackageName())) return;

        boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (!replacing) {
                paketiSistemeEkle(context, paketAdi);
            } else {
                SimgeOnbellegi.paketTemizle(paketAdi);
                KlasorSimgeUretici.onbellegiTemizle();
                paketDegisikligiOldu = true;
                if (dinleyici != null) dinleyici.onPaketlerDegisti();
            }
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action) || "android.intent.action.PACKAGE_FULLY_REMOVED".equals(action)) {
            if (!replacing) {
                paketiSistemdenTemizle(context, paketAdi);
            } else {
                SimgeOnbellegi.paketTemizle(paketAdi);
                KlasorSimgeUretici.onbellegiTemizle();
            }
        } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            SimgeOnbellegi.paketTemizle(paketAdi);
            KlasorSimgeUretici.onbellegiTemizle();
            paketDegisikligiOldu = true;
            if (dinleyici != null) dinleyici.onPaketlerDegisti();
        }
    }
}

