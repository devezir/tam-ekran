package com.tamekran.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Sabit Alt Bar (Dock) Veri Yöneticisi
 * Nova Launcher benzeri kalıcı alt bar yuvalarını, simge sıralamasını ve ekleme/çıkarma işlemlerini yönetir.
 */
public class DockYoneticisi {

    public static final String PREF_DOCK_UYGULAMALARI_JSON = "dock_uygulamalari_json";
    private static final Object DOCK_KILIDI = new Object();

    public static boolean isDockBaslatildi(Context context) {
        if (context == null) return false;
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        return sp.contains(PREF_DOCK_UYGULAMALARI_JSON);
    }

    public static int getMaksimumDockSayisi(Context context) {
        if (AyarBankasi.isDockKaydirilabilir(context)) {
            return 25;
        }
        return AyarBankasi.getDockSimgeSayisi(context);
    }

    public static List<String> dockYukle(Context context) {
        synchronized (DOCK_KILIDI) {
            List<String> tumu = AyarBankasi.getStringListesi(context, PREF_DOCK_UYGULAMALARI_JSON);
            int limit = getMaksimumDockSayisi(context);
            List<String> liste = new ArrayList<String>();
            for (String pkg : tumu) {
                if (liste.size() >= limit) break;
                if (!liste.contains(pkg)) {
                    liste.add(pkg);
                }
            }
            return liste;
        }
    }

    public static void dockKaydet(Context context, List<String> liste) {
        synchronized (DOCK_KILIDI) {
            int limit = getMaksimumDockSayisi(context);
            List<String> kaydedilecek = new ArrayList<String>();
            if (liste != null) {
                for (int i = 0; i < liste.size() && i < limit; i++) {
                    kaydedilecek.add(liste.get(i));
                }
            }
            AyarBankasi.putStringListesi(context, PREF_DOCK_UYGULAMALARI_JSON, kaydedilecek);
            MasaustuModelDeposu.v2ModeliniSifirla(context);
        }
    }

    /**
     * İki bileşen kimliğinin (veya paket adının) eşleşip eşleşmediğini kontrol eder.
     * Hem tam bileşen eşleşmesini (pkg/cls == pkg/cls) hem de paket kaldırma/eski kayıt durumunda
     * paket bazlı geriye dönük eşleşmeyi destekler.
     */
    public static boolean bilesenEslesiyorMu(String kayitliKimlik, String hedefKimlik) {
        return UygulamaBilgisi.bilesenEslesiyorMu(kayitliKimlik, hedefKimlik);
    }

    public static boolean docktaVarMi(Context context, String paketAdi) {
        if (paketAdi == null) return false;
        synchronized (DOCK_KILIDI) {
            List<String> liste = dockYukle(context);
            for (String item : liste) {
                if (bilesenEslesiyorMu(item, paketAdi)) return true;
            }
            return false;
        }
    }

    public static boolean dockaEkle(Context context, String paketAdi) {
        return dockaEkle(context, -1, paketAdi);
    }

    public static boolean dockaEkle(Context context, int pozisyon, String paketAdi) {
        if (paketAdi == null) return false;
        synchronized (DOCK_KILIDI) {
            List<String> liste = dockYukle(context);
            // Varsa önce çıkar, yeni konuma yerleştir
            for (int i = liste.size() - 1; i >= 0; i--) {
                if (bilesenEslesiyorMu(liste.get(i), paketAdi)) {
                    liste.remove(i);
                }
            }

            int limit = getMaksimumDockSayisi(context);
            if (pozisyon >= 0 && pozisyon <= liste.size()) {
                liste.add(pozisyon, paketAdi);
            } else {
                liste.add(paketAdi);
            }

            // Kapasiteyi aşarsa sonuncuyu kırp
            while (liste.size() > limit) {
                liste.remove(liste.size() - 1);
            }

            dockKaydet(context, liste);
            return true;
        }
    }

    public static boolean docktanKaldir(Context context, String paketAdi) {
        if (paketAdi == null) return false;
        synchronized (DOCK_KILIDI) {
            List<String> liste = dockYukle(context);
            boolean silindi = false;
            for (int i = liste.size() - 1; i >= 0; i--) {
                if (bilesenEslesiyorMu(liste.get(i), paketAdi)) {
                    liste.remove(i);
                    silindi = true;
                }
            }
            if (silindi) {
                dockKaydet(context, liste);
            }
            return silindi;
        }
    }

    /**
     * İlk kez çalıştırıldığında veya dock boş olduğunda sistem varsayılanlarını otomatik seçer
     */
    public static List<String> varsayilanlariKur(Context context, List<UygulamaBilgisi> tumUygulamalar) {
        List<String> secilenler = new ArrayList<String>();
        PackageManager pm = context.getPackageManager();
        int hedefAdet = getMaksimumDockSayisi(context);

        // 1. Telefon / Arama
        ekleVarsayilan(pm, new Intent(Intent.ACTION_DIAL), secilenler);

        // 2. Mesajlaşma
        ekleVarsayilan(pm, new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")), secilenler);

        // 3. Web Tarayıcı
        ekleVarsayilan(pm, new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")), secilenler);

        // 4. Kamera
        ekleVarsayilan(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE), secilenler);

        // Eksik yuvaları cihazdaki uygulamalardan tamamla
        if (tumUygulamalar != null) {
            for (UygulamaBilgisi app : tumUygulamalar) {
                if (secilenler.size() >= hedefAdet) break;
                String k = app.getBilesenKimligi();
                boolean var = false;
                for (String sec : secilenler) {
                    if (bilesenEslesiyorMu(sec, k)) {
                        var = true;
                        break;
                    }
                }
                if (!var) {
                    secilenler.add(k);
                }
            }
        }

        dockKaydet(context, secilenler);
        return secilenler;
    }

    private static void ekleVarsayilan(PackageManager pm, Intent intent, List<String> liste) {
        try {
            ResolveInfo ri = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null && ri.activityInfo.packageName != null) {
                String pkg = ri.activityInfo.packageName;
                String cls = ri.activityInfo.name;
                String kimlik = (cls != null && !cls.isEmpty()) ? (pkg + "/" + cls) : pkg;
                boolean var = false;
                for (String item : liste) {
                    if (bilesenEslesiyorMu(item, kimlik)) {
                        var = true;
                        break;
                    }
                }
                if (!var) {
                    liste.add(kimlik);
                }
            }
        } catch (Throwable t) {
            LauncherLog.d("DockYoneticisi", "ekleVarsayilan resolve hatasi: " + t.getMessage());
        }
    }
}

