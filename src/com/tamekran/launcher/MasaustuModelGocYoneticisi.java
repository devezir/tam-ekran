package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tam Ekran - V2 Model Göç ve Kalıcı Depo Projeksiyon Yöneticisi (SRP)
 * Eski parçalı SharedPreferences JSON yapılarından birleşik MasaustuKaydi modeline göçü
 * ve atomik kaydetme / geriye dönük uyumlu projeksiyon yazımını yönetir.
 */
public final class MasaustuModelGocYoneticisi {

    private static final String PREF_KLASORLER = "tamekran_klasorler_json";

    private MasaustuModelGocYoneticisi() {}

    static String getModelPrefKey(boolean tekKatman) {
        return tekKatman
                ? MasaustuModelDeposu.PREF_MODEL_V2_TEK_KATMAN
                : MasaustuModelDeposu.PREF_MODEL_V2_KLASIK;
    }

    /**
     * Eski MasaustuYoneticisi + KlasorYoneticisi + DockYoneticisi JSON verilerinden
     * tekil MasaustuKaydi modeline göç eder.
     */
    public static void eskiDepolardanGocEt(Context context, MasaustuModelDeposu.ModelDurumu durum) {
        if (!AyarBankasi.baglamGecerliMi(context) || durum == null) return;
        durum.ogeler.clear();

        Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> sayfalar = MasaustuYoneticisi.sayfalariYukle(context);
        List<KlasorYoneticisi.KlasorBilgisi> klasorler = KlasorYoneticisi.klasorleriYukle(context);
        List<String> dockListe = DockYoneticisi.dockYukle(context);

        Map<String, KlasorYoneticisi.KlasorBilgisi> klasorHaritasi = new HashMap<String, KlasorYoneticisi.KlasorBilgisi>();
        for (KlasorYoneticisi.KlasorBilgisi kb : klasorler) {
            if (kb != null && kb.id != null) {
                klasorHaritasi.put(kb.id, kb);
            }
        }

        // 1. Masaüstü öğeleri
        for (Map.Entry<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> entry : sayfalar.entrySet()) {
            int sayfaNo = entry.getKey();
            List<MasaustuYoneticisi.MasaustuOgesi> liste = entry.getValue();
            if (liste == null) continue;
            for (MasaustuYoneticisi.MasaustuOgesi o : liste) {
                if (o == null || o.paketAdi == null || o.paketAdi.trim().length() == 0) continue;
                int tur = MasaustuKaydi.turBelirle(o.paketAdi);
                String id = (tur == MasaustuKaydi.TUR_KLASOR) ? o.paketAdi : MasaustuModelDeposu.yeniIdUret("d");
                String etiket = "";
                if (tur == MasaustuKaydi.TUR_KLASOR) {
                    String kId = o.paketAdi.substring("klasor:".length());
                    KlasorYoneticisi.KlasorBilgisi kb = klasorHaritasi.get(kId);
                    if (kb != null && kb.ad != null) etiket = kb.ad;
                }
                durum.ogeler.add(new MasaustuKaydi(
                        id, tur, o.paketAdi, etiket,
                        MasaustuKaydi.CONTAINER_MASAUSTU,
                        sayfaNo, o.cellX, o.cellY, o.spanX, o.spanY
                ));
            }
        }

        // 2. Dock öğeleri
        for (int i = 0; i < dockListe.size(); i++) {
            String pkg = dockListe.get(i);
            if (pkg == null || pkg.trim().length() == 0) continue;
            int tur = MasaustuKaydi.turBelirle(pkg);
            String id = (tur == MasaustuKaydi.TUR_KLASOR) ? pkg : MasaustuModelDeposu.yeniIdUret("h");
            String etiket = "";
            if (tur == MasaustuKaydi.TUR_KLASOR) {
                String kId = pkg.substring("klasor:".length());
                KlasorYoneticisi.KlasorBilgisi kb = klasorHaritasi.get(kId);
                if (kb != null && kb.ad != null) etiket = kb.ad;
            }
            durum.ogeler.add(new MasaustuKaydi(
                    id, tur, pkg, etiket,
                    MasaustuKaydi.CONTAINER_DOCK,
                    i, i, 0, 1, 1
            ));
        }

        // 3. Klasör içi öğeler
        for (KlasorYoneticisi.KlasorBilgisi kb : klasorler) {
            if (kb == null || kb.id == null || kb.paketler == null) continue;
            for (int i = 0; i < kb.paketler.size(); i++) {
                String pkg = kb.paketler.get(i);
                if (pkg == null || pkg.trim().length() == 0) continue;
                durum.ogeler.add(new MasaustuKaydi(
                        MasaustuModelDeposu.yeniIdUret("f"),
                        MasaustuKaydi.turBelirle(pkg),
                        pkg,
                        "",
                        kb.id,
                        i, 0, 0, 1, 1
                ));
            }
        }

        MasaustuModelDeposu.butunlukDogrulaVeOnar(durum);
    }

    /**
     * Modeli hem V2 canonical JSON olarak hem de geriye dönük uyumlu klasik anahtarlara
     * (masaustu_sayfalari_json, tamekran_klasorler_json, dock_uygulamalari_json) TEK Editor işlemiyle yazar.
     */
    public static void kaydetDahili(Context context, MasaustuModelDeposu.ModelDurumu durum) {
        if (!AyarBankasi.baglamGecerliMi(context) || durum == null) return;
        try {
            JSONArray v2Arr = new JSONArray();
            for (MasaustuKaydi k : durum.ogeler) {
                if (k != null) v2Arr.put(k.toJsonObject());
            }

            // Masaüstü projeksiyonu
            JSONObject masaustuObj = new JSONObject();
            int maxSayfa = Math.max(1, durum.toplamSayfa);
            for (MasaustuKaydi k : durum.ogeler) {
                if (k.masaustundeMi() && k.sayfa + 1 > maxSayfa) {
                    maxSayfa = k.sayfa + 1;
                }
            }
            for (int s = 0; s < maxSayfa; s++) {
                JSONArray sArr = new JSONArray();
                for (MasaustuKaydi k : durum.ogeler) {
                    if (k.masaustundeMi() && k.sayfa == s) {
                        MasaustuYoneticisi.MasaustuOgesi legacy = new MasaustuYoneticisi.MasaustuOgesi(
                                k.bilesenKimligi, k.cellX, k.cellY, k.spanX, k.spanY);
                        sArr.put(legacy.toJsonObject());
                    }
                }
                masaustuObj.put("sayfa_" + s, sArr);
            }

            // Klasör projeksiyonu
            JSONArray klasorArr = new JSONArray();
            for (MasaustuKaydi k : durum.ogeler) {
                if (k.klasorMu()) {
                    String kId = k.getKlasorId();
                    JSONObject kObj = new JSONObject();
                    kObj.put("id", kId);
                    kObj.put("ad", (k.etiket != null && k.etiket.length() > 0) ? k.etiket : "Klasör");
                    List<String> paketler = new ArrayList<String>();
                    for (MasaustuKaydi ic : durum.klasorOgeleri(kId)) {
                        paketler.add(ic.bilesenKimligi);
                    }
                    kObj.put("paketler", JsonYardimcisi.stringListesiToJsonArray(paketler));
                    klasorArr.put(kObj);
                }
            }

            // Dock projeksiyonu
            List<String> dockListe = new ArrayList<String>();
            for (MasaustuKaydi d : durum.dockOgeleri()) {
                dockListe.add(d.bilesenKimligi);
            }

            String modelKey = getModelPrefKey(durum.tekKatmanModu);
            String masaustuKey = durum.tekKatmanModu
                    ? MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFALARI_JSON
                    : MasaustuYoneticisi.PREF_MASAUSTU_SAYFALARI_JSON;
            String sayfaSayisiKey = durum.tekKatmanModu
                    ? MasaustuYoneticisi.PREF_TEK_KATMAN_SAYFA_SAYISI
                    : MasaustuYoneticisi.PREF_SAYFA_SAYISI;

            SharedPreferences sp = AyarBankasi.getAyarlar(context);
            AyarBankasi.editorKaydet(sp.edit()
                    .putString(modelKey, v2Arr.toString())
                    .putString(masaustuKey, masaustuObj.toString())
                    .putInt(sayfaSayisiKey, maxSayfa)
                    .putString(PREF_KLASORLER, klasorArr.toString())
                    .putString(DockYoneticisi.PREF_DOCK_UYGULAMALARI_JSON,
                            JsonYardimcisi.stringListesiToJsonArray(dockListe).toString()));
        } catch (Throwable t) {
            LauncherLog.e("MasaustuModelGocYoneticisi", "kaydetDahili hatası", t);
        }
    }
}
