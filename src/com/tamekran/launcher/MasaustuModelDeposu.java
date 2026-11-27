package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tam Ekran - Tek Doğruluk Kaynağı (SSOT) ve Atomik İşlem Motoru (ModelWriter)
 * AOSP Launcher3 / Nova Launcher mimarisine uygun olarak tüm Masaüstü, Dock ve Klasör
 * öğelerini benzersiz kimlik (id) ve kapsayıcı (container) modeliyle tek kilitte yönetir.
 */
public class MasaustuModelDeposu {

    public static final String PREF_MODEL_V2_KLASIK = "masaustu_model_v2_json";
    public static final String PREF_MODEL_V2_TEK_KATMAN = "masaustu_tek_katman_model_v2_json";
    private static final String PREF_KLASORLER = "tamekran_klasorler_json";

    private static final Object MODEL_KILIDI = new Object();
    private static final AtomicLong ID_SAYACI = new AtomicLong(1);

    /**
     * Bellekte tutulan ve birim testlerde doğrudan kullanılabilen tekil model durumu.
     */
    public static class ModelDurumu {
        public final List<MasaustuKaydi> ogeler = new ArrayList<MasaustuKaydi>();
        public int toplamSayfa = 3;
        public int sutun = 4;
        public int satir = 6;
        public int dockLimit = 5;
        public boolean tekKatmanModu = false;

        public MasaustuKaydi idIleBul(String id) {
            if (id == null || id.length() == 0) return null;
            for (MasaustuKaydi k : ogeler) {
                if (id.equals(k.id)) return k;
            }
            return null;
        }

        public MasaustuKaydi masaustundeBul(int sayfa, String bilesenKimligi) {
            if (bilesenKimligi == null) return null;
            for (MasaustuKaydi k : ogeler) {
                if (k.masaustundeMi() && k.sayfa == sayfa
                        && DockYoneticisi.bilesenEslesiyorMu(k.bilesenKimligi, bilesenKimligi)) {
                    return k;
                }
            }
            return null;
        }

        public MasaustuKaydi docktaBul(String bilesenKimligi) {
            if (bilesenKimligi == null) return null;
            for (MasaustuKaydi k : ogeler) {
                if (k.docktaMi() && DockYoneticisi.bilesenEslesiyorMu(k.bilesenKimligi, bilesenKimligi)) {
                    return k;
                }
            }
            return null;
        }

        public MasaustuKaydi klasorKaydiBul(String klasorId) {
            if (klasorId == null) return null;
            String hedef = "klasor:" + klasorId;
            for (MasaustuKaydi k : ogeler) {
                if (k.klasorMu() && (klasorId.equals(k.id) || hedef.equals(k.id) || hedef.equals(k.bilesenKimligi))) {
                    return k;
                }
            }
            return null;
        }

        public MasaustuKaydi klasordeBul(String klasorId, String bilesenKimligi) {
            if (klasorId == null || bilesenKimligi == null) return null;
            for (MasaustuKaydi k : ogeler) {
                if (klasorId.equals(k.container)
                        && DockYoneticisi.bilesenEslesiyorMu(k.bilesenKimligi, bilesenKimligi)) {
                    return k;
                }
            }
            return null;
        }

        public List<MasaustuKaydi> sayfaOgeleri(int sayfa) {
            List<MasaustuKaydi> sonuc = new ArrayList<MasaustuKaydi>();
            for (MasaustuKaydi k : ogeler) {
                if (k.masaustundeMi() && k.sayfa == sayfa) {
                    sonuc.add(k);
                }
            }
            return sonuc;
        }

        public List<MasaustuKaydi> klasorOgeleri(String klasorId) {
            List<MasaustuKaydi> sonuc = new ArrayList<MasaustuKaydi>();
            if (klasorId == null) return sonuc;
            for (MasaustuKaydi k : ogeler) {
                if (klasorId.equals(k.container)) {
                    sonuc.add(k);
                }
            }
            Collections.sort(sonuc, new Comparator<MasaustuKaydi>() {
                @Override
                public int compare(MasaustuKaydi a, MasaustuKaydi b) {
                    return Integer.compare(a.sayfa, b.sayfa);
                }
            });
            return sonuc;
        }

        public List<MasaustuKaydi> dockOgeleri() {
            List<MasaustuKaydi> sonuc = new ArrayList<MasaustuKaydi>();
            for (MasaustuKaydi k : ogeler) {
                if (k.docktaMi()) {
                    sonuc.add(k);
                }
            }
            Collections.sort(sonuc, new Comparator<MasaustuKaydi>() {
                @Override
                public int compare(MasaustuKaydi a, MasaustuKaydi b) {
                    return Integer.compare(a.sayfa, b.sayfa);
                }
            });
            return sonuc;
        }
    }

    public static String yeniIdUret(String onek) {
        return (onek != null ? onek : "item") + "_" + System.currentTimeMillis() + "_" + ID_SAYACI.getAndIncrement();
    }

    private static String getModelPrefKey(boolean tekKatman) {
        return tekKatman ? PREF_MODEL_V2_TEK_KATMAN : PREF_MODEL_V2_KLASIK;
    }

    /**
     * Eski yedekleme geri yüklendiğinde V2 önbelleğini sıfırlar ki eski verilerden temiz göç yapılsın.
     */
    public static void v2ModeliniSifirla(Context context) {
        if (!AyarBankasi.baglamGecerliMi(context)) return;
        synchronized (MODEL_KILIDI) {
            SharedPreferences sp = AyarBankasi.getAyarlar(context);
            AyarBankasi.editorKaydet(sp.edit()
                    .remove(PREF_MODEL_V2_KLASIK)
                    .remove(PREF_MODEL_V2_TEK_KATMAN));
        }
    }

    public static ModelDurumu yukle(Context context) {
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = new ModelDurumu();
            if (!AyarBankasi.baglamGecerliMi(context)) return durum;

            durum.tekKatmanModu = MasaustuYoneticisi.isTekKatmanModu(context);
            durum.toplamSayfa = MasaustuYoneticisi.getSayfaSayisi(context);
            durum.sutun = AyarBankasi.getMasaustuSutun(context);
            durum.satir = AyarBankasi.getMasaustuSatir(context);
            durum.dockLimit = DockYoneticisi.getMaksimumDockSayisi(context);

            SharedPreferences sp = AyarBankasi.getAyarlar(context);
            String modelKey = getModelPrefKey(durum.tekKatmanModu);
            String hamV2 = sp.getString(modelKey, null);

            if (hamV2 != null && hamV2.trim().startsWith("[")) {
                try {
                    JSONArray arr = new JSONArray(hamV2);
                    for (int i = 0; i < arr.length(); i++) {
                        MasaustuKaydi k = MasaustuKaydi.fromJsonObject(arr.optJSONObject(i));
                        if (k != null) {
                            if (k.id == null || k.id.length() == 0) {
                                k.id = k.klasorMu() ? k.bilesenKimligi : yeniIdUret("m");
                            }
                            durum.ogeler.add(k);
                        }
                    }
                    // Eğer mevcut dock veya klasör kayıtları dışarıdan güncellendiyse uyumu koru
                    if (!durum.ogeler.isEmpty()) {
                        butunlukDogrulaVeOnar(durum);
                        return durum;
                    }
                } catch (Throwable t) {
                    LauncherLog.w("MasaustuModelDeposu", "V2 model ayrıştırma hatası, göç deneniyor", t);
                }
            }

            // Eski parçalı depolardan tek seferlik temiz göç
            eskiDepolardanGocEt(context, durum);
            if (!durum.ogeler.isEmpty()) {
                kaydetDahili(context, durum);
            }
            return durum;
        }
    }

    public static void eskiDepolardanGocEt(Context context, ModelDurumu durum) {
        MasaustuModelGocYoneticisi.eskiDepolardanGocEt(context, durum);
    }

    private static void kaydetDahili(Context context, ModelDurumu durum) {
        MasaustuModelGocYoneticisi.kaydetDahili(context, durum);
    }

    // =========================================================================
    // ATOMİK İŞLEMLER (PHASE 2 - MODELWRITER TRANSACTIONS)
    // =========================================================================

    /**
     * İki öğeyi tek bir atomik işlemde yeni klasörde birleştirir.
     * Sürüklenen öğe masaüstünden, dock'tan veya başka bir klasörden gelmişse
     * eski konumunda ASLA kopya bırakmaz; yalnızca container alanı güncellenir.
     */
    public static String klasorOlusturVeBirlestir(
            Context context,
            String hedefItemId,
            int hedefSayfa,
            String hedefKimlik,
            SuruklemeVerisi veri,
            String klasorAdi) {
        if (context == null || veri == null || veri.app == null) return null;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            String yeniKlasorId = klasorOlusturVeBirlestirBellek(
                    durum,
                    hedefItemId,
                    hedefSayfa,
                    hedefKimlik,
                    veri.itemId,
                    veri.app.getBilesenKimligi(),
                    veri.kaynakTuru,
                    veri.kaynakSayfa,
                    veri.kaynakKlasorId,
                    klasorAdi
            );
            if (yeniKlasorId != null) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return yeniKlasorId;
        }
    }

    /**
     * Saf Java (JVM birim testleri için de doğrudan çağrılabilir) atomik klasör oluşturma işlemi.
     */
    public static String klasorOlusturVeBirlestirBellek(
            ModelDurumu durum,
            String hedefItemId,
            int hedefSayfa,
            String hedefKimlik,
            String suruklenenItemId,
            String suruklenenKimlik,
            int kaynakTuru,
            int kaynakSayfa,
            String kaynakKlasorId,
            String klasorAdi) {
        if (durum == null || suruklenenKimlik == null || hedefKimlik == null) return null;
        if (DockYoneticisi.bilesenEslesiyorMu(suruklenenKimlik, hedefKimlik)) return null;
        if (suruklenenKimlik.startsWith("klasor:") || hedefKimlik.startsWith("klasor:")) return null;

        MasaustuKaydi hedef = (hedefItemId != null) ? durum.idIleBul(hedefItemId) : null;
        if (hedef == null) {
            hedef = durum.masaustundeBul(hedefSayfa, hedefKimlik);
        }
        if (hedef == null || !hedef.masaustundeMi()) return null;

        String yeniKlasorId = String.valueOf(System.currentTimeMillis()) + "_" + ID_SAYACI.getAndIncrement();
        String klasorEtiketi = "klasor:" + yeniKlasorId;
        String ad = (klasorAdi != null && klasorAdi.trim().length() > 0) ? klasorAdi.trim() : "Klasör";

        // 1. Yeni klasörü hedefin koordinatında oluştur
        MasaustuKaydi klasorKaydi = new MasaustuKaydi(
                klasorEtiketi,
                MasaustuKaydi.TUR_KLASOR,
                klasorEtiketi,
                ad,
                MasaustuKaydi.CONTAINER_MASAUSTU,
                hedef.sayfa,
                hedef.cellX,
                hedef.cellY,
                1,
                1
        );
        durum.ogeler.add(klasorKaydi);

        // 2. Hedef öğeyi yeni klasörün içine taşı (masaüstünden otomatik çıkmış olur)
        hedef.container = yeniKlasorId;
        hedef.sayfa = 0;
        hedef.cellX = 0;
        hedef.cellY = 0;

        // 3. Sürüklenen öğeyi bul ve yeni klasöre taşı (veya çekmeceden geliyorsa yeni kayıt aç)
        String eskiKaynakKlasorId = null;
        MasaustuKaydi suruklenen = bulSuruklenenKayit(
                durum, suruklenenItemId, suruklenenKimlik, kaynakTuru, kaynakSayfa, kaynakKlasorId);

        if (suruklenen != null) {
            if (suruklenen.klasorIcindeMi()) {
                eskiKaynakKlasorId = suruklenen.container;
            }
            suruklenen.container = yeniKlasorId;
            suruklenen.sayfa = 1;
            suruklenen.cellX = 0;
            suruklenen.cellY = 0;
        } else {
            durum.ogeler.add(new MasaustuKaydi(
                    yeniIdUret("f"),
                    MasaustuKaydi.turBelirle(suruklenenKimlik),
                    suruklenenKimlik,
                    "",
                    yeniKlasorId,
                    1, 0, 0, 1, 1
            ));
        }

        // 4. Eğer başka bir klasörden taşındıysa kaynak klasörde 1/0 öğe kalma durumunu çöz
        if (eskiKaynakKlasorId != null) {
            klasorKucultmeKontrolu(durum, eskiKaynakKlasorId);
        }

        // 5. Tek Katman modunda sürüklenen/hedef uygulamanın masaüstünde başka kopyası kalmamasını garantile
        if (durum.tekKatmanModu) {
            masaustundenTumEslesenleriKaldir(durum, suruklenenKimlik);
            masaustundenTumEslesenleriKaldir(durum, hedefKimlik);
        }

        butunlukDogrulaVeOnar(durum);
        return yeniKlasorId;
    }

    /**
     * Sürüklenen öğeyi mevcut bir klasöre atomik olarak taşır.
     */
    public static boolean klasoreOgeTasi(
            Context context,
            String hedefKlasorId,
            SuruklemeVerisi veri) {
        if (context == null || hedefKlasorId == null || veri == null || veri.app == null) return false;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            boolean basari = klasoreOgeTasiBellek(
                    durum,
                    hedefKlasorId,
                    veri.itemId,
                    veri.app.getBilesenKimligi(),
                    veri.kaynakTuru,
                    veri.kaynakSayfa,
                    veri.kaynakKlasorId
            );
            if (basari) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return basari;
        }
    }

    public static boolean klasoreOgeTasiBellek(
            ModelDurumu durum,
            String hedefKlasorId,
            String suruklenenItemId,
            String suruklenenKimlik,
            int kaynakTuru,
            int kaynakSayfa,
            String kaynakKlasorId) {
        if (durum == null || hedefKlasorId == null || suruklenenKimlik == null) return false;
        if (suruklenenKimlik.startsWith("klasor:")) return false;
        if (durum.idIleBul("klasor:" + hedefKlasorId) == null) return false;
        if (durum.klasordeBul(hedefKlasorId, suruklenenKimlik) != null) return false;

        List<MasaustuKaydi> mevcutlar = durum.klasorOgeleri(hedefKlasorId);
        int sira = mevcutlar.size();

        String eskiKaynakKlasorId = null;
        MasaustuKaydi suruklenen = bulSuruklenenKayit(
                durum, suruklenenItemId, suruklenenKimlik, kaynakTuru, kaynakSayfa, kaynakKlasorId);

        if (suruklenen != null) {
            if (suruklenen.klasorIcindeMi()) {
                eskiKaynakKlasorId = suruklenen.container;
            }
            suruklenen.container = hedefKlasorId;
            suruklenen.sayfa = sira;
            suruklenen.cellX = 0;
            suruklenen.cellY = 0;
        } else {
            durum.ogeler.add(new MasaustuKaydi(
                    yeniIdUret("f"),
                    MasaustuKaydi.turBelirle(suruklenenKimlik),
                    suruklenenKimlik,
                    "",
                    hedefKlasorId,
                    sira, 0, 0, 1, 1
            ));
        }

        if (eskiKaynakKlasorId != null) {
            klasorKucultmeKontrolu(durum, eskiKaynakKlasorId);
        }

        if (durum.tekKatmanModu) {
            masaustundenTumEslesenleriKaldir(durum, suruklenenKimlik);
        }

        butunlukDogrulaVeOnar(durum);
        return true;
    }

    /**
     * Öğeyi masaüstü hücresine atomik olarak taşır veya yerleştirir (aynı sayfa, sayfalar arası, dock'tan, klasörden, çekmeceden).
     */
    public static boolean masaustuneOgeTasi(
            Context context,
            SuruklemeVerisi veri,
            int hedefSayfa,
            int targetX,
            int targetY) {
        if (context == null || veri == null || veri.app == null) return false;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            boolean basari = masaustuneOgeTasiBellek(
                    durum,
                    veri.itemId,
                    veri.app.getBilesenKimligi(),
                    veri.kaynakTuru,
                    veri.kaynakSayfa,
                    veri.kaynakKlasorId,
                    hedefSayfa,
                    targetX,
                    targetY,
                    veri.spanX,
                    veri.spanY
            );
            if (basari) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return basari;
        }
    }

    public static boolean masaustuneOgeTasiBellek(
            ModelDurumu durum,
            String suruklenenItemId,
            String suruklenenKimlik,
            int kaynakTuru,
            int kaynakSayfa,
            String kaynakKlasorId,
            int hedefSayfa,
            int targetX,
            int targetY,
            int spanX,
            int spanY) {
        if (durum == null || suruklenenKimlik == null) return false;
        int sx = Math.max(1, spanX);
        int sy = Math.max(1, spanY);
        int cx = Math.max(0, Math.min(Math.max(0, durum.sutun - sx), targetX));
        int cy = Math.max(0, Math.min(Math.max(0, durum.satir - sy), targetY));

        String eskiKaynakKlasorId = null;
        MasaustuKaydi kayit = bulSuruklenenKayit(
                durum, suruklenenItemId, suruklenenKimlik, kaynakTuru, kaynakSayfa, kaynakKlasorId);

        if (kayit != null) {
            if (kayit.klasorIcindeMi()) {
                eskiKaynakKlasorId = kayit.container;
            }
            kayit.container = MasaustuKaydi.CONTAINER_MASAUSTU;
            kayit.sayfa = Math.max(0, hedefSayfa);
            kayit.cellX = cx;
            kayit.cellY = cy;
            kayit.spanX = sx;
            kayit.spanY = sy;
        } else {
            if (durum.tekKatmanModu) {
                masaustundenTumEslesenleriKaldir(durum, suruklenenKimlik);
            }
            durum.ogeler.add(new MasaustuKaydi(
                    suruklenenKimlik.startsWith("klasor:") ? suruklenenKimlik : yeniIdUret("d"),
                    MasaustuKaydi.turBelirle(suruklenenKimlik),
                    suruklenenKimlik,
                    "",
                    MasaustuKaydi.CONTAINER_MASAUSTU,
                    Math.max(0, hedefSayfa),
                    cx, cy, sx, sy
            ));
        }

        if (eskiKaynakKlasorId != null) {
            klasorKucultmeKontrolu(durum, eskiKaynakKlasorId);
        }

        butunlukDogrulaVeOnar(durum);
        return true;
    }

    /**
     * Yalnızca tek bir öğeyi (öncelikle benzersiz itemId ile) masaüstünden, dock'tan veya klasörden kaldırır.
     * Aynı uygulamanın aynı sayfadaki diğer kısayollarına DOKUNMAZ.
     */
    public static boolean ogeKaldir(
            Context context,
            String itemId,
            int kaynakTuru,
            int kaynakSayfa,
            String bilesenKimligi) {
        if (context == null) return false;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            boolean basari = ogeKaldirBellek(durum, itemId, kaynakTuru, kaynakSayfa, bilesenKimligi);
            if (basari) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return basari;
        }
    }

    public static boolean ogeKaldirBellek(
            ModelDurumu durum,
            String itemId,
            int kaynakTuru,
            int kaynakSayfa,
            String bilesenKimligi) {
        if (durum == null) return false;
        MasaustuKaydi hedef = (itemId != null && itemId.length() > 0) ? durum.idIleBul(itemId) : null;
        if (hedef == null && bilesenKimligi != null) {
            if (kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) {
                hedef = durum.masaustundeBul(kaynakSayfa, bilesenKimligi);
            } else if (kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
                hedef = durum.docktaBul(bilesenKimligi);
            }
        }
        if (hedef == null) return false;

        String eskiKlasorId = hedef.klasorIcindeMi() ? hedef.container : null;
        if (hedef.klasorMu()) {
            String kId = hedef.getKlasorId();
            for (int i = durum.ogeler.size() - 1; i >= 0; i--) {
                if (kId.equals(durum.ogeler.get(i).container)) {
                    durum.ogeler.remove(i);
                }
            }
        }
        durum.ogeler.remove(hedef);

        if (eskiKlasorId != null) {
            klasorKucultmeKontrolu(durum, eskiKlasorId);
        }
        if (hedef.docktaMi()) {
            dockSiralariniNormalizeEt(durum);
        }
        return true;
    }

    /**
     * Öğeyi Alt Bar'a (Dock) atomik olarak taşır veya sıralar.
     * Masaüstünde aynı uygulamanın 2. bir kısayolu varsa onu asla silmez.
     */
    public static boolean dockaOgeTasi(Context context, SuruklemeVerisi veri, int hedefPozisyon) {
        if (context == null || veri == null || veri.app == null) return false;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            boolean basari = dockaOgeTasiBellek(
                    durum,
                    veri.itemId,
                    veri.app.getBilesenKimligi(),
                    veri.kaynakTuru,
                    veri.kaynakSayfa,
                    veri.kaynakKlasorId,
                    hedefPozisyon
            );
            if (basari) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return basari;
        }
    }

    public static boolean dockaOgeTasiBellek(
            ModelDurumu durum,
            String suruklenenItemId,
            String bilesenKimligi,
            int kaynakTuru,
            int kaynakSayfa,
            String kaynakKlasorId,
            int hedefPozisyon) {
        if (durum == null || bilesenKimligi == null) return false;

        String eskiKaynakKlasorId = null;
        MasaustuKaydi kayit = bulSuruklenenKayit(
                durum, suruklenenItemId, bilesenKimligi, kaynakTuru, kaynakSayfa, kaynakKlasorId);

        // Eğer dock'ta aynı bileşen zaten varsa ve masaüstünden/çekmeceden geliyorsa, dock içinde mükerrer yuva açma
        MasaustuKaydi docktakiMevcut = durum.docktaBul(bilesenKimligi);
        if (kayit != null && kayit.klasorIcindeMi()) {
            eskiKaynakKlasorId = kayit.container;
        }

        List<MasaustuKaydi> dockList = durum.dockOgeleri();
        if (kayit != null && !kayit.docktaMi()) {
            if (docktakiMevcut != null && docktakiMevcut != kayit) {
                // Taşınan masaüstü/klasör öğesini sil ve dock'taki mevcut yuvayı hedef pozisyona al
                durum.ogeler.remove(kayit);
                kayit = docktakiMevcut;
            } else {
                kayit.container = MasaustuKaydi.CONTAINER_DOCK;
                kayit.cellX = 0;
                kayit.cellY = 0;
                kayit.spanX = 1;
                kayit.spanY = 1;
            }
        } else if (kayit == null) {
            if (docktakiMevcut != null) {
                kayit = docktakiMevcut;
            } else {
                kayit = new MasaustuKaydi(
                        bilesenKimligi.startsWith("klasor:") ? bilesenKimligi : yeniIdUret("h"),
                        MasaustuKaydi.turBelirle(bilesenKimligi),
                        bilesenKimligi,
                        "",
                        MasaustuKaydi.CONTAINER_DOCK,
                        dockList.size(), 0, 0, 1, 1
                );
                durum.ogeler.add(kayit);
            }
        }

        dockList.remove(kayit);
        int poz = (hedefPozisyon >= 0 && hedefPozisyon <= dockList.size()) ? hedefPozisyon : dockList.size();
        dockList.add(poz, kayit);

        int limit = Math.max(1, durum.dockLimit);
        while (dockList.size() > limit) {
            MasaustuKaydi cikarilan = dockList.remove(dockList.size() - 1);
            durum.ogeler.remove(cikarilan);
        }
        for (int i = 0; i < dockList.size(); i++) {
            dockList.get(i).sayfa = i;
            dockList.get(i).cellX = i;
        }

        if (eskiKaynakKlasorId != null) {
            klasorKucultmeKontrolu(durum, eskiKaynakKlasorId);
        }
        butunlukDogrulaVeOnar(durum);
        return true;
    }

    private static void dockSiralariniNormalizeEt(ModelDurumu durum) {
        List<MasaustuKaydi> dockList = durum.dockOgeleri();
        for (int i = 0; i < dockList.size(); i++) {
            dockList.get(i).sayfa = i;
            dockList.get(i).cellX = i;
        }
    }

    /**
     * Uygulama cihazdan tamamen kaldırıldığında (ACTION_PACKAGE_REMOVED) masaüstü, dock ve klasörlerdeki
     * tüm kopyaları tek atomik işlemde temizler ve etkilenen klasörleri çözer/siler.
     */
    public static boolean paketTumKopyalariSil(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return false;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            boolean degisti = paketTumKopyalariSilBellek(durum, paketAdi);
            if (degisti) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return degisti;
        }
    }

    public static boolean paketTumKopyalariSilBellek(ModelDurumu durum, String paketAdi) {
        if (durum == null || paketAdi == null) return false;
        boolean degisti = false;
        Set<String> etkilenenKlasorler = new HashSet<String>();

        for (int i = durum.ogeler.size() - 1; i >= 0; i--) {
            MasaustuKaydi k = durum.ogeler.get(i);
            if (k != null && DockYoneticisi.bilesenEslesiyorMu(k.bilesenKimligi, paketAdi)) {
                if (k.klasorIcindeMi()) {
                    etkilenenKlasorler.add(k.container);
                }
                durum.ogeler.remove(i);
                degisti = true;
            }
        }

        for (String kId : etkilenenKlasorler) {
            klasorKucultmeKontrolu(durum, kId);
        }
        if (degisti) {
            dockSiralariniNormalizeEt(durum);
            butunlukDogrulaVeOnar(durum);
        }
        return degisti;
    }

    /**
     * Silinen bir öğeyi (veya klasörü) Geri Al işlemiyle eski konumuna geri yükler.
     * Aynı sayfadaki aynı uygulamanın diğer kısayollarını ASLA silmez.
     */
    public static boolean ogeGeriYukle(
            Context context,
            int kaynakTuru,
            int kaynakSayfa,
            String paketAdi,
            int cellX,
            int cellY,
            int spanX,
            int spanY,
            String klasorAdi,
            List<String> klasorPaketleri) {
        if (context == null || paketAdi == null) return false;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            boolean basari = ogeGeriYukleBellek(
                    durum, kaynakTuru, kaynakSayfa, paketAdi,
                    cellX, cellY, spanX, spanY, klasorAdi, klasorPaketleri);
            if (basari) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return basari;
        }
    }

    public static boolean ogeGeriYukleBellek(
            ModelDurumu durum,
            int kaynakTuru,
            int kaynakSayfa,
            String paketAdi,
            int cellX,
            int cellY,
            int spanX,
            int spanY,
            String klasorAdi,
            List<String> klasorPaketleri) {
        if (durum == null || paketAdi == null) return false;
        String hedefKimlik = paketAdi;

        if (klasorPaketleri != null && !klasorPaketleri.isEmpty()) {
            String yeniKlasorId = String.valueOf(System.currentTimeMillis()) + "_" + ID_SAYACI.getAndIncrement();
            hedefKimlik = "klasor:" + yeniKlasorId;
            String ad = (klasorAdi != null && klasorAdi.trim().length() > 0) ? klasorAdi.trim() : "Klasör";
            if (kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) {
                durum.ogeler.add(new MasaustuKaydi(
                        hedefKimlik, MasaustuKaydi.TUR_KLASOR, hedefKimlik, ad,
                        MasaustuKaydi.CONTAINER_MASAUSTU, Math.max(0, kaynakSayfa),
                        Math.max(0, cellX), Math.max(0, cellY), 1, 1));
            }
            for (int i = 0; i < klasorPaketleri.size(); i++) {
                String p = klasorPaketleri.get(i);
                if (p == null || p.trim().length() == 0) continue;
                durum.ogeler.add(new MasaustuKaydi(
                        yeniIdUret("f"), MasaustuKaydi.turBelirle(p), p, "",
                        yeniKlasorId, i, 0, 0, 1, 1));
            }
            if (kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
                dockaOgeTasiBellek(durum, hedefKimlik, hedefKimlik,
                        SuruklemeVerisi.KAYNAK_CEKMECE, 0, null, cellX);
            }
            butunlukDogrulaVeOnar(durum);
            return true;
        }

        if (kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) {
            int sx = Math.max(1, spanX);
            int sy = Math.max(1, spanY);
            int cx = Math.max(0, Math.min(Math.max(0, durum.sutun - sx), cellX));
            int cy = Math.max(0, Math.min(Math.max(0, durum.satir - sy), cellY));
            durum.ogeler.add(new MasaustuKaydi(
                    hedefKimlik.startsWith("klasor:") ? hedefKimlik : yeniIdUret("u"),
                    MasaustuKaydi.turBelirle(hedefKimlik),
                    hedefKimlik,
                    "",
                    MasaustuKaydi.CONTAINER_MASAUSTU,
                    Math.max(0, kaynakSayfa),
                    cx, cy, sx, sy));
            butunlukDogrulaVeOnar(durum);
            return true;
        } else if (kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
            return dockaOgeTasiBellek(durum, null, hedefKimlik,
                    SuruklemeVerisi.KAYNAK_CEKMECE, 0, null, cellX);
        }
        return false;
    }

    private static MasaustuKaydi bulSuruklenenKayit(
            ModelDurumu durum,
            String itemId,
            String bilesenKimligi,
            int kaynakTuru,
            int kaynakSayfa,
            String kaynakKlasorId) {
        if (itemId != null && itemId.length() > 0) {
            MasaustuKaydi byId = durum.idIleBul(itemId);
            if (byId != null) return byId;
        }
        if (kaynakTuru == SuruklemeVerisi.KAYNAK_MASAUSTU) {
            MasaustuKaydi ayniSayfa = durum.masaustundeBul(kaynakSayfa, bilesenKimligi);
            if (ayniSayfa != null) return ayniSayfa;
            // Bayat kaynakSayfa durumunda tüm masaüstü sayfalarında ara (hayalet kalmasını önler)
            for (MasaustuKaydi k : durum.ogeler) {
                if (k.masaustundeMi() && DockYoneticisi.bilesenEslesiyorMu(k.bilesenKimligi, bilesenKimligi)) {
                    return k;
                }
            }
        } else if (kaynakTuru == SuruklemeVerisi.KAYNAK_DOCK) {
            return durum.docktaBul(bilesenKimligi);
        } else if (kaynakTuru == SuruklemeVerisi.KAYNAK_KLASOR && kaynakKlasorId != null) {
            return durum.klasordeBul(kaynakKlasorId, bilesenKimligi);
        }
        return null;
    }

    private static void masaustundenTumEslesenleriKaldir(ModelDurumu durum, String bilesenKimligi) {
        for (int i = durum.ogeler.size() - 1; i >= 0; i--) {
            MasaustuKaydi k = durum.ogeler.get(i);
            if (k.masaustundeMi() && DockYoneticisi.bilesenEslesiyorMu(k.bilesenKimligi, bilesenKimligi)) {
                durum.ogeler.remove(i);
            }
        }
    }

    /**
     * Bir klasörde 1 öğe kaldıysa otomatik çözer (klasörün hücresine geri koyar),
     * 0 öğe kaldıysa klasörü tamamen siler.
     */
    public static void klasorKucultmeKontrolu(ModelDurumu durum, String klasorId) {
        if (durum == null || klasorId == null) return;
        MasaustuKaydi klasorKaydi = durum.idIleBul("klasor:" + klasorId);
        List<MasaustuKaydi> icerik = durum.klasorOgeleri(klasorId);
        if (icerik.size() == 1) {
            MasaustuKaydi sonOge = icerik.get(0);
            if (klasorKaydi != null) {
                sonOge.container = klasorKaydi.container;
                sonOge.sayfa = klasorKaydi.sayfa;
                sonOge.cellX = klasorKaydi.cellX;
                sonOge.cellY = klasorKaydi.cellY;
                sonOge.spanX = 1;
                sonOge.spanY = 1;
                durum.ogeler.remove(klasorKaydi);
            } else {
                sonOge.container = MasaustuKaydi.CONTAINER_MASAUSTU;
                sonOge.sayfa = 0;
                durum.ogeler.remove(klasorKaydi);
            }
        } else if (icerik.isEmpty() && klasorKaydi != null) {
            durum.ogeler.remove(klasorKaydi);
        }
    }

    /**
     * Model Bütünlük Değişmezi (Invariant Gate):
     * - Benzersiz itemId garantisi
     * - Masaüstünde aynı sayfada hücre çakışması varsa boş hücreye onarım
     * - Klasörün kendi içine girmesini engelleme
     */
    public static void butunlukDogrulaVeOnar(ModelDurumu durum) {
        if (durum == null) return;
        Set<String> gorulenIdler = new HashSet<String>();
        for (int i = durum.ogeler.size() - 1; i >= 0; i--) {
            MasaustuKaydi k = durum.ogeler.get(i);
            if (k == null || k.bilesenKimligi == null || k.bilesenKimligi.trim().length() == 0) {
                durum.ogeler.remove(i);
                continue;
            }
            if (k.klasorMu() && k.getKlasorId().equals(k.container)) {
                k.container = MasaustuKaydi.CONTAINER_MASAUSTU;
            }
            if (k.id == null || k.id.length() == 0 || gorulenIdler.contains(k.id)) {
                k.id = k.klasorMu() ? k.bilesenKimligi : yeniIdUret("r");
            }
            gorulenIdler.add(k.id);
        }

        // Sayfa bazlı hücre çakışması onarımı
        int maxSayfa = Math.max(1, durum.toplamSayfa);
        for (MasaustuKaydi k : durum.ogeler) {
            if (k.masaustundeMi() && k.sayfa + 1 > maxSayfa) maxSayfa = k.sayfa + 1;
        }
        for (int s = 0; s < maxSayfa; s++) {
            List<MasaustuKaydi> sayfaList = durum.sayfaOgeleri(s);
            if (sayfaList.size() <= 1) continue;
            List<MasaustuKaydi> yerlesen = new ArrayList<MasaustuKaydi>();
            for (MasaustuKaydi oge : sayfaList) {
                oge.cellX = Math.max(0, Math.min(Math.max(0, durum.sutun - oge.spanX), oge.cellX));
                oge.cellY = Math.max(0, Math.min(Math.max(0, durum.satir - oge.spanY), oge.cellY));
                if (!cakismaVarMi(yerlesen, oge)) {
                    yerlesen.add(oge);
                } else {
                    int[] bos = ilkBosHucreBul(yerlesen, durum.sutun, durum.satir, oge.spanX, oge.spanY);
                    if (bos != null) {
                        oge.cellX = bos[0];
                        oge.cellY = bos[1];
                        yerlesen.add(oge);
                    }
                }
            }
        }
    }

    private static boolean cakismaVarMi(List<MasaustuKaydi> yerlesen, MasaustuKaydi aday) {
        for (MasaustuKaydi y : yerlesen) {
            if (MasaustuYoneticisi.hucrelerCakisiyorMu(
                    y.cellX, y.cellY, y.spanX, y.spanY,
                    aday.cellX, aday.cellY, aday.spanX, aday.spanY)) {
                return true;
            }
        }
        return false;
    }

    private static int[] ilkBosHucreBul(List<MasaustuKaydi> yerlesen, int sutun, int satir, int sx, int sy) {
        for (int r = 0; r <= Math.max(0, satir - sy); r++) {
            for (int c = 0; c <= Math.max(0, sutun - sx); c++) {
                boolean dolu = false;
                for (MasaustuKaydi y : yerlesen) {
                    if (MasaustuYoneticisi.hucrelerCakisiyorMu(y.cellX, y.cellY, y.spanX, y.spanY, c, r, sx, sy)) {
                        dolu = true;
                        break;
                    }
                }
                if (!dolu) return new int[]{c, r};
            }
        }
        return null;
    }

    private interface ModelMutasyonu {
        boolean uygula(ModelDurumu durum);
    }

    private interface ModelIdMutasyonu {
        String uygula(ModelDurumu durum);
    }

    private static boolean modelMutasyonuCalistir(Context context, ModelMutasyonu islem) {
        if (context == null || islem == null) return false;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            if (islem.uygula(durum)) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
                return true;
            }
            return false;
        }
    }

    private static String modelIdMutasyonuCalistir(Context context, ModelIdMutasyonu islem) {
        if (context == null || islem == null) return null;
        synchronized (MODEL_KILIDI) {
            ModelDurumu durum = yukle(context);
            String sonuc = islem.uygula(durum);
            if (sonuc != null) {
                kaydetDahili(context, durum);
                KlasorSimgeUretici.onbellegiTemizle();
            }
            return sonuc;
        }
    }

    public static String bosKlasorOlustur(Context context, final int sayfa, final String klasorAdi) {
        return modelIdMutasyonuCalistir(context, new ModelIdMutasyonu() {
            @Override
            public String uygula(ModelDurumu durum) {
                return bosKlasorOlusturBellek(durum, sayfa, klasorAdi);
            }
        });
    }

    public static String bosKlasorOlusturBellek(ModelDurumu durum, int sayfa, String klasorAdi) {
        if (durum == null) return null;
        String ad = (klasorAdi != null && klasorAdi.trim().length() > 0) ? klasorAdi.trim() : "Klasör";
        String yeniKlasorId = String.valueOf(System.currentTimeMillis()) + "_" + ID_SAYACI.getAndIncrement();
        String klasorEtiketi = "klasor:" + yeniKlasorId;
        int hedefSayfa = Math.max(0, sayfa);
        if (hedefSayfa >= durum.toplamSayfa) {
            durum.toplamSayfa = hedefSayfa + 1;
        }
        int[] bos = ilkBosHucreBul(durum.sayfaOgeleri(hedefSayfa), durum.sutun, durum.satir, 1, 1);
        int cx = bos != null ? bos[0] : 0;
        int cy = bos != null ? bos[1] : 0;
        durum.ogeler.add(new MasaustuKaydi(
                klasorEtiketi, MasaustuKaydi.TUR_KLASOR, klasorEtiketi, ad,
                MasaustuKaydi.CONTAINER_MASAUSTU, hedefSayfa, cx, cy, 1, 1));
        butunlukDogrulaVeOnar(durum);
        return yeniKlasorId;
    }

    public static boolean klasorAdiDegistir(Context context, final String klasorId, final String yeniAd) {
        if (klasorId == null || yeniAd == null || yeniAd.trim().length() == 0) return false;
        return modelMutasyonuCalistir(context, new ModelMutasyonu() {
            @Override
            public boolean uygula(ModelDurumu durum) {
                return klasorAdiDegistirBellek(durum, klasorId, yeniAd);
            }
        });
    }

    public static boolean klasorAdiDegistirBellek(ModelDurumu durum, String klasorId, String yeniAd) {
        if (durum == null || klasorId == null || yeniAd == null || yeniAd.trim().length() == 0) return false;
        MasaustuKaydi klasor = durum.klasorKaydiBul(klasorId);
        if (klasor == null) return false;
        klasor.etiket = yeniAd.trim();
        return true;
    }

    public static boolean klasoreUygulamaEkle(Context context, final String klasorId, final String bilesenKimligi) {
        if (klasorId == null || bilesenKimligi == null) return false;
        return modelMutasyonuCalistir(context, new ModelMutasyonu() {
            @Override
            public boolean uygula(ModelDurumu durum) {
                return klasoreUygulamaEkleBellek(durum, klasorId, bilesenKimligi);
            }
        });
    }

    public static boolean klasoreUygulamaEkleBellek(ModelDurumu durum, String klasorId, String bilesenKimligi) {
        if (durum == null || klasorId == null || bilesenKimligi == null || bilesenKimligi.trim().length() == 0) return false;
        if (bilesenKimligi.startsWith("klasor:")) return false;
        if (durum.klasorKaydiBul(klasorId) == null) return false;
        if (durum.klasordeBul(klasorId, bilesenKimligi) != null) return false;
        int sira = durum.klasorOgeleri(klasorId).size();
        durum.ogeler.add(new MasaustuKaydi(
                yeniIdUret("f"), MasaustuKaydi.turBelirle(bilesenKimligi), bilesenKimligi, "",
                klasorId, sira, 0, 0, 1, 1));
        butunlukDogrulaVeOnar(durum);
        return true;
    }

    public static boolean klasoruSilVeMasaustuneDok(Context context, final String klasorId) {
        if (klasorId == null) return false;
        return modelMutasyonuCalistir(context, new ModelMutasyonu() {
            @Override
            public boolean uygula(ModelDurumu durum) {
                return klasoruSilVeMasaustuneDokBellek(durum, klasorId);
            }
        });
    }

    public static boolean klasoruSilVeMasaustuneDokBellek(ModelDurumu durum, String klasorId) {
        if (durum == null || klasorId == null) return false;
        MasaustuKaydi klasor = durum.klasorKaydiBul(klasorId);
        List<MasaustuKaydi> icerikler = durum.klasorOgeleri(klasorId);
        if (klasor == null && icerikler.isEmpty()) return false;

        int hedefSayfa = (klasor != null && klasor.masaustundeMi()) ? Math.max(0, klasor.sayfa) : 0;
        if (klasor != null) {
            durum.ogeler.remove(klasor);
        }
        for (MasaustuKaydi oge : icerikler) {
            oge.container = MasaustuKaydi.CONTAINER_MASAUSTU;
            oge.sayfa = hedefSayfa;
            int[] bos = ilkBosHucreBul(durum.sayfaOgeleri(hedefSayfa), durum.sutun, durum.satir, 1, 1);
            if (bos == null) {
                hedefSayfa++;
                if (hedefSayfa >= durum.toplamSayfa) durum.toplamSayfa = hedefSayfa + 1;
                oge.sayfa = hedefSayfa;
                bos = ilkBosHucreBul(durum.sayfaOgeleri(hedefSayfa), durum.sutun, durum.satir, 1, 1);
            }
            oge.cellX = bos != null ? bos[0] : 0;
            oge.cellY = bos != null ? bos[1] : 0;
        }
        butunlukDogrulaVeOnar(durum);
        return true;
    }

    public static String klasorOlusturVeCokluEkle(Context context, final int sayfa, final String klasorAdi, final List<String> bilesenKimlikleri) {
        if (bilesenKimlikleri == null || bilesenKimlikleri.isEmpty()) return null;
        return modelIdMutasyonuCalistir(context, new ModelIdMutasyonu() {
            @Override
            public String uygula(ModelDurumu durum) {
                return klasorOlusturVeCokluEkleBellek(durum, sayfa, klasorAdi, bilesenKimlikleri);
            }
        });
    }

    public static String klasorOlusturVeCokluEkleBellek(ModelDurumu durum, int sayfa, String klasorAdi, List<String> bilesenKimlikleri) {
        if (durum == null || bilesenKimlikleri == null || bilesenKimlikleri.isEmpty()) return null;
        String yeniKlasorId = bosKlasorOlusturBellek(durum, sayfa, klasorAdi);
        if (yeniKlasorId == null) return null;
        for (String kimlik : bilesenKimlikleri) {
            if (kimlik != null && kimlik.trim().length() > 0 && !kimlik.startsWith("klasor:")) {
                klasoreUygulamaEkleBellek(durum, yeniKlasorId, kimlik);
            }
        }
        return yeniKlasorId;
    }
}
