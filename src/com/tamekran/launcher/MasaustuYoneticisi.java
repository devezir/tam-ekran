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
 * Tam Ekran - Çok Sayfalı Masaüstü Veri Yöneticisi (CellLayout Matrix Grid)
 * Masaüstündeki uygulamaların ve klasörlerin hücre koordinatlarını (cellX, cellY) yönetir ve saklar.
 */
public class MasaustuYoneticisi {

    public static final String PREF_MASAUSTU_MODU = AyarBankasi.PREF_MASAUSTU_MODU;
    public static final int MOD_KLASIK_CIFTE_KATMAN = AyarBankasi.MOD_KLASIK_CIFTE_KATMAN;
    public static final int MOD_TEK_KATMAN_SERP = AyarBankasi.MOD_TEK_KATMAN_SERP;

    public static final int VERI_DURUMU_ILK_KURULUM = 0;
    public static final int VERI_DURUMU_GECERLI = 1;
    public static final int VERI_DURUMU_BOZUK = 2;

    public static final String PREF_SAYFA_SAYISI = "masaustu_sayfa_sayisi";
    public static final String PREF_TEK_KATMAN_SAYFA_SAYISI = "masaustu_tek_katman_sayfa_sayisi";
    public static final String PREF_VARSAYILAN_SAYFA = "masaustu_varsayilan_sayfa";
    public static final String PREF_SONSUZ_KAYDIRMA = "masaustu_sonsuz_kaydirma";
    public static final String PREF_MASAUSTU_SAYFALARI_JSON = "masaustu_sayfalari_json";
    public static final String PREF_TEK_KATMAN_SAYFALARI_JSON = "masaustu_tek_katman_sayfalari_json";

    private static final Object MASAUSTU_KILIDI = new Object();
    private static int sonYuklemeDurumu = VERI_DURUMU_ILK_KURULUM;

    public static int getSonYuklemeDurumu() {
        return sonYuklemeDurumu;
    }

    public static boolean sonVeriBozukMu() {
        return sonYuklemeDurumu == VERI_DURUMU_BOZUK;
    }

    public static int parseDurumunuSifirla() {
        int onceki = sonYuklemeDurumu;
        sonYuklemeDurumu = VERI_DURUMU_ILK_KURULUM;
        return onceki;
    }

    public static class MasaustuOgesi {
        public String paketAdi;
        public int cellX;
        public int cellY;
        public int spanX = 1;
        public int spanY = 1;

        public MasaustuOgesi(String paketAdi, int cellX, int cellY) {
            this.paketAdi = paketAdi;
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = 1;
            this.spanY = 1;
        }

        public MasaustuOgesi(String paketAdi, int cellX, int cellY, int spanX, int spanY) {
            this.paketAdi = paketAdi;
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = Math.max(1, spanX);
            this.spanY = Math.max(1, spanY);
        }

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("pkg", paketAdi);
                obj.put("x", cellX);
                obj.put("y", cellY);
                obj.put("sx", spanX);
                obj.put("sy", spanY);
            } catch (Throwable t) {
                LauncherLog.w("MasaustuOgesi", "toJsonObject hatası", t);
            }
            return obj;
        }

        public static MasaustuOgesi fromJson(Object jsonItem, int varsayilanSira, int sutunSayisi) {
            if (jsonItem instanceof JSONObject) {
                JSONObject obj = (JSONObject) jsonItem;
                String pkg = obj.optString("pkg", "");
                int x = obj.optInt("x", varsayilanSira % sutunSayisi);
                int y = obj.optInt("y", varsayilanSira / sutunSayisi);
                int sx = obj.optInt("sx", 1);
                int sy = obj.optInt("sy", 1);
                return new MasaustuOgesi(pkg, x, y, sx, sy);
            } else if (jsonItem instanceof String) {
                String pkg = (String) jsonItem;
                int x = varsayilanSira % sutunSayisi;
                int y = varsayilanSira / sutunSayisi;
                return new MasaustuOgesi(pkg, x, y, 1, 1);
            }
            return null;
        }
    }

    public static int getMasaustuModu(Context context) {
        return AyarBankasi.getInt(context, PREF_MASAUSTU_MODU, MOD_KLASIK_CIFTE_KATMAN);
    }

    public static boolean isTekKatmanModu(Context context) {
        return getMasaustuModu(context) == MOD_TEK_KATMAN_SERP;
    }

    public static void setMasaustuModu(Context context, int mod) {
        AyarBankasi.putInt(context, PREF_MASAUSTU_MODU, mod);
    }

    public static int getSayfaSayisi(Context context) {
        if (isTekKatmanModu(context)) {
            return Math.max(1, AyarBankasi.getInt(context, PREF_TEK_KATMAN_SAYFA_SAYISI, 1));
        }
        return AyarBankasi.getInt(context, PREF_SAYFA_SAYISI, 3);
    }

    public static void setSayfaSayisi(Context context, int sayisi) {
        String key = isTekKatmanModu(context) ? PREF_TEK_KATMAN_SAYFA_SAYISI : PREF_SAYFA_SAYISI;
        AyarBankasi.putInt(context, key, Math.max(1, sayisi));
    }

    public static int getVarsayilanSayfa(Context context) {
        int def = AyarBankasi.getInt(context, PREF_VARSAYILAN_SAYFA, 0);
        int max = getSayfaSayisi(context);
        return (def >= max) ? 0 : def;
    }

    public static void setVarsayilanSayfa(Context context, int sayfa) {
        AyarBankasi.putInt(context, PREF_VARSAYILAN_SAYFA, sayfa);
    }

    public static boolean getSonsuzKaydirma(Context context) {
        return AyarBankasi.getBoolean(context, PREF_SONSUZ_KAYDIRMA, false);
    }

    public static void setSonsuzKaydirma(Context context, boolean acik) {
        AyarBankasi.putBoolean(context, PREF_SONSUZ_KAYDIRMA, acik);
    }

    /**
     * Ham masaüstü JSON metnini ayrıştırır; geçersizse sonYuklemeDurumu = VERI_DURUMU_BOZUK işaretler.
     */
    public static Map<Integer, List<MasaustuOgesi>> sayfalariJsonAnalizEt(String hamJson, int toplamSayfa, int sutun) {
        Map<Integer, List<MasaustuOgesi>> harita = new HashMap<Integer, List<MasaustuOgesi>>();
        for (int i = 0; i < toplamSayfa; i++) {
            harita.put(i, new ArrayList<MasaustuOgesi>());
        }

        if (hamJson == null || hamJson.trim().length() == 0) {
            sonYuklemeDurumu = VERI_DURUMU_ILK_KURULUM;
            return harita;
        }

        String kirpilmis = hamJson.trim();
        if (!kirpilmis.startsWith("{") || !kirpilmis.endsWith("}")) {
            sonYuklemeDurumu = VERI_DURUMU_BOZUK;
            LauncherLog.e("MasaustuYoneticisi", "sayfalariYukle JSON yapisi gecersiz; veri ezilmesi engellendi.");
            return harita;
        }

        try {
            JSONObject anaObj = new JSONObject(kirpilmis);
            for (int i = 0; i < toplamSayfa; i++) {
                String anahtar = "sayfa_" + i;
                if (anaObj.has(anahtar)) {
                    JSONArray arr = anaObj.getJSONArray(anahtar);
                    List<MasaustuOgesi> sayfaListesi = harita.get(i);
                    for (int j = 0; j < arr.length(); j++) {
                        Object item = arr.get(j);
                        MasaustuOgesi oge = MasaustuOgesi.fromJson(item, j, sutun);
                        if (oge != null && oge.paketAdi != null && oge.paketAdi.trim().length() > 0) {
                            sayfaListesi.add(oge);
                        }
                    }
                }
            }
            sonYuklemeDurumu = VERI_DURUMU_GECERLI;
        } catch (Exception e) {
            sonYuklemeDurumu = VERI_DURUMU_BOZUK;
            LauncherLog.e("MasaustuYoneticisi", "sayfalariYukle JSON bozuk; veri ezilmesi engellendi.", e);
        }

        return harita;
    }

    /**
     * Sayfa bazlı masaüstü öğelerini koordinatlarıyla (cellX, cellY) yükler.
     * Bozuk JSON tespit edildiğinde eski veriyi karantinaya kopyalar ve VERI_DURUMU_BOZUK durumunu işaretler.
     */
    public static Map<Integer, List<MasaustuOgesi>> sayfalariYukle(Context context) {
        synchronized (MASAUSTU_KILIDI) {
            int toplamSayfa = getSayfaSayisi(context);
            int sutun = AyarBankasi.getMasaustuSutun(context);
            String jsonKey = isTekKatmanModu(context) ? PREF_TEK_KATMAN_SAYFALARI_JSON : PREF_MASAUSTU_SAYFALARI_JSON;
            String hamJson = AyarBankasi.getString(context, jsonKey, null);

            Map<Integer, List<MasaustuOgesi>> harita = sayfalariJsonAnalizEt(hamJson, toplamSayfa, sutun);
            if (sonYuklemeDurumu == VERI_DURUMU_BOZUK && context != null && hamJson != null) {
                AyarBankasi.putString(context, "karantina_" + jsonKey, hamJson);
            }
            return harita;
        }
    }

    /**
     * Verilen sayfalar haritasından hedef sayfa dışındaki mükerrer paket kopyalarını temizler (DRY).
     */
    private static void digerSayfalardanKopyalariTemizle(Map<Integer, List<MasaustuOgesi>> harita, int haricSayfa, String paketAdi) {
        if (harita == null || paketAdi == null) return;
        for (Map.Entry<Integer, List<MasaustuOgesi>> entry : harita.entrySet()) {
            if (entry.getKey() != haricSayfa && entry.getValue() != null) {
                for (int i = entry.getValue().size() - 1; i >= 0; i--) {
                    if (DockYoneticisi.bilesenEslesiyorMu(entry.getValue().get(i).paketAdi, paketAdi)) {
                        entry.getValue().remove(i);
                    }
                }
            }
        }
    }

    /**
     * Sayfaları koordinatlı JSON olarak kaydeder
     */
    public static void sayfalariKaydet(Context context, Map<Integer, List<MasaustuOgesi>> harita) {
        synchronized (MASAUSTU_KILIDI) {
            try {
                JSONObject anaObj = new JSONObject();
                for (Map.Entry<Integer, List<MasaustuOgesi>> entry : harita.entrySet()) {
                    JSONArray arr = new JSONArray();
                    for (MasaustuOgesi oge : entry.getValue()) {
                        if (oge != null) {
                            arr.put(oge.toJsonObject());
                        }
                    }
                    anaObj.put("sayfa_" + entry.getKey(), arr);
                }
                String jsonKey = isTekKatmanModu(context) ? PREF_TEK_KATMAN_SAYFALARI_JSON : PREF_MASAUSTU_SAYFALARI_JSON;
                AyarBankasi.putString(context, jsonKey, anaObj.toString());
                MasaustuModelDeposu.v2ModeliniSifirla(context);
            } catch (Throwable t) {
                LauncherLog.e("MasaustuYoneticisi", "sayfalariKaydet JSON yazma hatası", t);
            }
        }
    }

    /**
     * İki hücre bölgesinin (AABB) çakışıp çakışmadığını tespit eder
     */
    public static boolean hucrelerCakisiyorMu(int x1, int y1, int sx1, int sy1, int x2, int y2, int sx2, int sy2) {
        return x1 < x2 + sx2 && x1 + sx1 > x2 && y1 < y2 + sy2 && y1 + sy1 > y2;
    }

    /**
     * Belirtilen öğenin (c, r) hücresini kapsayıp kapsamadığını tespit eder
     */
    public static boolean hucreyiKapsiyorMu(MasaustuOgesi oge, int c, int r) {
        if (oge == null) return false;
        return c >= oge.cellX && c < oge.cellX + oge.spanX && r >= oge.cellY && r < oge.cellY + oge.spanY;
    }

    /**
     * Öğeyi belirtilen sayfada doğrudan verilen (cellX, cellY) koordinatına yerleştirir.
     */
    public static void sayfayaOgeYerlestir(Context context, int sayfa, String paketAdi, int cellX, int cellY) {
        sayfayaOgeYerlestir(context, sayfa, paketAdi, cellX, cellY, 1, 1);
    }

    private static List<MasaustuOgesi> sayfaListesiAlVeyaOlustur(Map<Integer, List<MasaustuOgesi>> harita, int sayfa) {
        List<MasaustuOgesi> sayfaListesi = harita.get(sayfa);
        if (sayfaListesi == null) {
            sayfaListesi = new ArrayList<MasaustuOgesi>();
            harita.put(sayfa, sayfaListesi);
        }
        return sayfaListesi;
    }

    private static void tekilPaketSayfadanVeDigerlerindenTemizle(
            Context context,
            Map<Integer, List<MasaustuOgesi>> harita,
            List<MasaustuOgesi> sayfaListesi,
            int sayfa,
            String paketAdi) {
        for (int i = sayfaListesi.size() - 1; i >= 0; i--) {
            if (DockYoneticisi.bilesenEslesiyorMu(sayfaListesi.get(i).paketAdi, paketAdi)) {
                sayfaListesi.remove(i);
            }
        }
        if (isTekKatmanModu(context)) {
            digerSayfalardanKopyalariTemizle(harita, sayfa, paketAdi);
        }
    }

    public static void sayfayaOgeYerlestir(Context context, int sayfa, String paketAdi, int cellX, int cellY, int spanX, int spanY) {
        if (paketAdi == null) return;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            List<MasaustuOgesi> sayfaListesi = sayfaListesiAlVeyaOlustur(harita, sayfa);
            tekilPaketSayfadanVeDigerlerindenTemizle(context, harita, sayfaListesi, sayfa, paketAdi);
            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);
            if (spanX > sutun) spanX = sutun;
            if (spanY > satir) spanY = satir;
            if (cellX + spanX > sutun) cellX = Math.max(0, sutun - spanX);
            if (cellY + spanY > satir) cellY = Math.max(0, satir - spanY);

            MasaustuOgesi yeniOge = new MasaustuOgesi(paketAdi, cellX, cellY, spanX, spanY);
            sayfaListesi.add(0, yeniOge);
            MasaustuButunlukDenetleyici.hucreCakismalariniOnar(sayfaListesi, sutun, satir);
            sayfalariKaydet(context, harita);
        }
    }

    /**
     * Sayfadaki bir öğenin koordinatını değiştirir (Özgür taşıma)
     */
    public static boolean sayfadaOgeTasi(Context context, int sayfa, String paketAdi, int yeniCellX, int yeniCellY) {
        if (paketAdi == null) return false;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            List<MasaustuOgesi> sayfaListesi = harita.get(sayfa);
            if (sayfaListesi == null) return false;

            MasaustuOgesi bulunan = null;
            // 1. Önce bu sayfadaki eşleşen öğeyi bul ve mükerrer kopyaları temizle
            for (int i = sayfaListesi.size() - 1; i >= 0; i--) {
                MasaustuOgesi o = sayfaListesi.get(i);
                if (DockYoneticisi.bilesenEslesiyorMu(o.paketAdi, paketAdi)) {
                    if (bulunan == null) {
                        bulunan = o;
                    } else {
                        sayfaListesi.remove(i);
                    }
                }
            }

            // 2. Diğer sayfalardaki olası kopyaları temizle (özellikle Tek Katman modunda katı tekillik)
            for (Map.Entry<Integer, List<MasaustuOgesi>> entry : harita.entrySet()) {
                if (entry.getKey() != sayfa && entry.getValue() != null) {
                    for (int i = entry.getValue().size() - 1; i >= 0; i--) {
                        MasaustuOgesi o = entry.getValue().get(i);
                        if (DockYoneticisi.bilesenEslesiyorMu(o.paketAdi, paketAdi)) {
                            if (bulunan == null) {
                                bulunan = o;
                            }
                            entry.getValue().remove(i);
                        }
                    }
                }
            }

            int sx = bulunan != null ? bulunan.spanX : 1;
            int sy = bulunan != null ? bulunan.spanY : 1;

            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);
            if (sx > sutun) sx = sutun;
            if (sy > satir) sy = satir;
            if (yeniCellX + sx > sutun) yeniCellX = Math.max(0, sutun - sx);
            if (yeniCellY + sy > satir) yeniCellY = Math.max(0, satir - sy);

            if (bulunan != null) {
                bulunan.cellX = yeniCellX;
                bulunan.cellY = yeniCellY;
                bulunan.spanX = sx;
                bulunan.spanY = sy;
                sayfaListesi.remove(bulunan);
                sayfaListesi.add(0, bulunan);
            } else {
                MasaustuOgesi yeniOge = new MasaustuOgesi(paketAdi, yeniCellX, yeniCellY, sx, sy);
                sayfaListesi.add(0, yeniOge);
            }

            MasaustuButunlukDenetleyici.hucreCakismalariniOnar(sayfaListesi, sutun, satir);
            sayfalariKaydet(context, harita);
            return true;
        }
    }

    /**
     * Uygulamayı belirtilen sayfada ilk boş hücreye yerleştirir
     */
    public static void sayfayaUygulamaEkle(Context context, int sayfa, String paketAdi) {
        if (paketAdi == null) return;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            List<MasaustuOgesi> sayfaListesi = sayfaListesiAlVeyaOlustur(harita, sayfa);
            tekilPaketSayfadanVeDigerlerindenTemizle(context, harita, sayfaListesi, sayfa, paketAdi);

            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);

            int hedefX = 0, hedefY = 0;
            boolean bulundu = false;
            for (int r = 0; r < satir; r++) {
                for (int c = 0; c < sutun; c++) {
                    boolean dolu = false;
                    for (MasaustuOgesi o : sayfaListesi) {
                        if (hucreyiKapsiyorMu(o, c, r)) {
                            dolu = true;
                            break;
                        }
                    }
                    if (!dolu) {
                        hedefX = c;
                        hedefY = r;
                        bulundu = true;
                        break;
                    }
                }
                if (bulundu) break;
            }
            if (!bulundu) {
                hedefX = sayfaListesi.size() % sutun;
                hedefY = sayfaListesi.size() / sutun;
            }
            sayfaListesi.add(new MasaustuOgesi(paketAdi, hedefX, hedefY));
            sayfalariKaydet(context, harita);
        }
    }

    public static boolean isMasaustuKilitli(Context context) {
        return AyarBankasi.isMasaustuKilitli(context);
    }

    public static boolean sayfayaOgeEkle(Context context, int sayfa, String paketAdi) {
        return sayfayaOgeEkle(context, sayfa, paketAdi, 1, 1);
    }

    /**
     * Belirtilen sayfada spanX ve spanY boyutuna uygun ilk boş hücreye öğe ekler
     */
    public static boolean sayfayaOgeEkle(Context context, int sayfa, String paketAdi, int spanX, int spanY) {
        if (context == null || paketAdi == null) return false;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            List<MasaustuOgesi> sayfaListesi = sayfaListesiAlVeyaOlustur(harita, sayfa);

            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);
            spanX = Math.max(1, Math.min(spanX, sutun));
            spanY = Math.max(1, Math.min(spanY, satir));

            int hedefX = -1, hedefY = -1;
            for (int r = 0; r <= satir - spanY; r++) {
                for (int c = 0; c <= sutun - spanX; c++) {
                    boolean cakisma = false;
                    for (MasaustuOgesi o : sayfaListesi) {
                        if (hucrelerCakisiyorMu(c, r, spanX, spanY, o.cellX, o.cellY, o.spanX, o.spanY)) {
                            cakisma = true;
                            break;
                        }
                    }
                    if (!cakisma) {
                        hedefX = c;
                        hedefY = r;
                        break;
                    }
                }
                if (hedefX != -1) break;
            }

            if (hedefX == -1) {
                return false;
            }

            sayfaListesi.add(new MasaustuOgesi(paketAdi, hedefX, hedefY, spanX, spanY));
            sayfalariKaydet(context, harita);
            return true;
        }
    }

    /**
     * Uygulamayı belirtilen sayfada istenen pozisyona ekler (Eski API uyumluluğu)
     */
    public static void sayfayaUygulamaEkle(Context context, int sayfa, int pozisyon, String paketAdi) {
        if (paketAdi == null) return;
        int sutun = AyarBankasi.getMasaustuSutun(context);
        int cx = pozisyon >= 0 ? (pozisyon % sutun) : 0;
        int cy = pozisyon >= 0 ? (pozisyon / sutun) : 0;
        sayfayaOgeYerlestir(context, sayfa, paketAdi, cx, cy);
    }

    /**
     * Sayfadaki bir öğeyi başka bir öğeyle (örneğin klasörle) değiştirir.
     * Öğenin mevcut hücre koordinatı (cellX, cellY) aynen korunur!
     */
    public static boolean sayfadaOgeDegistir(Context context, int sayfa, String eskiOge, String yeniOge) {
        if (eskiOge == null || yeniOge == null) return false;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            List<MasaustuOgesi> sayfaListesi = harita.get(sayfa);
            if (sayfaListesi == null) return false;
            for (MasaustuOgesi oge : sayfaListesi) {
                if (DockYoneticisi.bilesenEslesiyorMu(oge.paketAdi, eskiOge)) {
                    oge.paketAdi = yeniOge;
                    sayfalariKaydet(context, harita);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Uygulamanın herhangi bir masaüstü sayfasında olup olmadığını döner
     */
    public static boolean masaustundeVarMi(Context context, String paketAdi) {
        if (paketAdi == null) return false;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            for (List<MasaustuOgesi> liste : harita.values()) {
                if (liste != null) {
                    for (MasaustuOgesi o : liste) {
                        if (DockYoneticisi.bilesenEslesiyorMu(o.paketAdi, paketAdi)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * Uygulamayı belirli bir sayfadan kaldırır
     */
    public static boolean sayfadanUygulamaKaldir(Context context, int sayfa, String paketAdi) {
        if (paketAdi == null) return false;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            List<MasaustuOgesi> sayfaListesi = harita.get(sayfa);
            if (sayfaListesi != null) {
                boolean degisti = false;
                for (int i = sayfaListesi.size() - 1; i >= 0; i--) {
                    if (DockYoneticisi.bilesenEslesiyorMu(sayfaListesi.get(i).paketAdi, paketAdi)) {
                        sayfaListesi.remove(i);
                        degisti = true;
                    }
                }
                if (degisti) {
                    sayfalariKaydet(context, harita);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Uygulamayı tüm masaüstü sayfalarından kaldırır
     */
    public static boolean masaustundenKaldir(Context context, String paketAdi) {
        if (paketAdi == null) return false;
        synchronized (MASAUSTU_KILIDI) {
            if (paketAdi.startsWith("kisayol:")) {
                String kId = paketAdi.substring("kisayol:".length());
                KisayolYoneticisi.kisayolSil(context, kId);
            }
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            boolean degisti = false;
            for (List<MasaustuOgesi> liste : harita.values()) {
                if (liste != null) {
                    for (int i = liste.size() - 1; i >= 0; i--) {
                        if (DockYoneticisi.bilesenEslesiyorMu(liste.get(i).paketAdi, paketAdi)) {
                            liste.remove(i);
                            degisti = true;
                        }
                    }
                }
            }
            if (degisti) {
                sayfalariKaydet(context, harita);
            }
            return degisti;
        }
    }

    /**
     * Izgara boyutu küçüldüğünde koordinatları sınırların dışına çıkan öğeleri güvenli sınırlara çeker
     */
    public static void izgaraSinirlariniUyarla(Context context, int yeniSutun, int yeniSatir) {
        if (context == null || yeniSutun < 3 || yeniSatir < 3) return;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            boolean degisti = false;
            for (List<MasaustuOgesi> sayfaListesi : harita.values()) {
                if (sayfaListesi == null) continue;
                for (MasaustuOgesi oge : sayfaListesi) {
                    if (oge == null) continue;
                    if (oge.spanX > yeniSutun) {
                        oge.spanX = yeniSutun;
                        degisti = true;
                    }
                    if (oge.spanY > yeniSatir) {
                        oge.spanY = yeniSatir;
                        degisti = true;
                    }
                    if (oge.cellX + oge.spanX > yeniSutun) {
                        oge.cellX = Math.max(0, yeniSutun - oge.spanX);
                        degisti = true;
                    }
                    if (oge.cellY + oge.spanY > yeniSatir) {
                        oge.cellY = Math.max(0, yeniSatir - oge.spanY);
                        degisti = true;
                    }
                }
            }
            if (degisti) {
                sayfalariKaydet(context, harita);
            }
        }
    }

    /**
     * Tek Katmanlı Mod (iOS / MIUI Tarzı):
     * Cihazda yüklü olan tüm uygulamaları (gizlenenler hariç) ızgara kapasitesine göre
     * hesaplanan masaüstü sayfalarına (cellX, cellY) muntazam şekilde serper.
     */
    public static void tumUygulamalariMasaustuneSerp(Context context, List<UygulamaBilgisi> uygulamalar) {
        if (context == null || uygulamalar == null || uygulamalar.isEmpty()) return;
        synchronized (MASAUSTU_KILIDI) {
            List<String> gizliListesi = YedeklemeYoneticisi.gizlenenleriAl(context);
            List<UygulamaBilgisi> serpilecekler = new ArrayList<UygulamaBilgisi>();
            for (UygulamaBilgisi u : uygulamalar) {
                if (u != null && u.paketAdi != null && !gizliListesi.contains(u.paketAdi)) {
                    serpilecekler.add(u);
                }
            }

            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);
            int sayfaKapasite = Math.max(1, sutun * satir);

            int gerekenSayfa = Math.max(1, (int) Math.ceil((double) serpilecekler.size() / sayfaKapasite));
            setSayfaSayisi(context, gerekenSayfa);

            Map<Integer, List<MasaustuOgesi>> harita = new HashMap<Integer, List<MasaustuOgesi>>();
            for (int i = 0; i < gerekenSayfa; i++) {
                harita.put(i, new ArrayList<MasaustuOgesi>());
            }

            for (int i = 0; i < serpilecekler.size(); i++) {
                UygulamaBilgisi app = serpilecekler.get(i);
                int sayfaNo = i / sayfaKapasite;
                int slot = i % sayfaKapasite;
                int cx = slot % sutun;
                int cy = slot / sutun;

                List<MasaustuOgesi> sayfaList = harita.get(sayfaNo);
                if (sayfaList != null) {
                    sayfaList.add(new MasaustuOgesi(app.getBilesenKimligi(), cx, cy));
                }
            }

            sayfalariKaydet(context, harita);
        }
    }

    /**
     * Tek Katmanlı Mod senkronizasyonu:
     * Masaüstü boşsa tüm uygulamaları serper, doluysa yeni yüklenip masaüstünde olmayan
     * uygulamaları tespit edip boş kalan hücrelere veya gerekirse yeni bir sayfaya yerleştirir.
     */
    public static boolean tekKatmanSenkronizeEt(Context context, List<UygulamaBilgisi> tumUygulamalar) {
        if (context == null || !isTekKatmanModu(context) || tumUygulamalar == null || tumUygulamalar.isEmpty()) {
            return false;
        }

        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);
            if (sonVeriBozukMu()) {
                LauncherLog.w("MasaustuYoneticisi", "tekKatmanSenkronizeEt: Bozuk veri algilandi, otomatik yeniden serpme atlandi.");
                return false;
            }
            boolean hicUygulamaYok = true;
            for (List<MasaustuOgesi> l : harita.values()) {
                if (l != null && !l.isEmpty()) {
                    hicUygulamaYok = false;
                    break;
                }
            }

            if (hicUygulamaYok) {
                tumUygulamalariMasaustuneSerp(context, tumUygulamalar);
                return true;
            }

            // Mevcut masaüstündeki ve klasörlerdeki tüm paketleri topla
            List<String> mevcutKimlikler = new ArrayList<String>();
            for (List<MasaustuOgesi> list : harita.values()) {
                if (list != null) {
                    for (MasaustuOgesi o : list) {
                        if (o != null && o.paketAdi != null) {
                            mevcutKimlikler.add(o.paketAdi);
                        }
                    }
                }
            }
            List<KlasorYoneticisi.KlasorBilgisi> klasorler = KlasorYoneticisi.klasorleriYukle(context);
            for (KlasorYoneticisi.KlasorBilgisi kb : klasorler) {
                if (kb != null && kb.paketler != null) {
                    mevcutKimlikler.addAll(kb.paketler);
                }
            }

            List<String> gizliListesi = YedeklemeYoneticisi.gizlenenleriAl(context);
            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);
            int sayfaKapasite = Math.max(1, sutun * satir);

            boolean degisiklik = false;
            int toplamSayfa = getSayfaSayisi(context);

            for (UygulamaBilgisi app : tumUygulamalar) {
                if (app == null || app.paketAdi == null || gizliListesi.contains(app.paketAdi)) continue;
                String kimlik = app.getBilesenKimligi();
                boolean varMi = false;
                for (String m : mevcutKimlikler) {
                    if (DockYoneticisi.bilesenEslesiyorMu(m, kimlik) || DockYoneticisi.bilesenEslesiyorMu(m, app.paketAdi)) {
                        varMi = true;
                        break;
                    }
                }

                if (!varMi) {
                    // Boş hücre bul ve ekle
                    boolean eklendi = false;
                    for (int s = 0; s < toplamSayfa; s++) {
                        List<MasaustuOgesi> sList = harita.get(s);
                        if (sList == null) {
                            sList = new ArrayList<MasaustuOgesi>();
                            harita.put(s, sList);
                        }
                        if (sList.size() < sayfaKapasite) {
                            int slot = sList.size();
                            int cx = slot % sutun;
                            int cy = slot / sutun;
                            sList.add(new MasaustuOgesi(kimlik, cx, cy));
                            MasaustuButunlukDenetleyici.hucreCakismalariniOnar(sList, sutun, satir);
                            mevcutKimlikler.add(kimlik);
                            eklendi = true;
                            degisiklik = true;
                            break;
                        }
                    }

                    if (!eklendi) {
                        // Yeni sayfa aç
                        int yeniSayfa = toplamSayfa;
                        toplamSayfa++;
                        setSayfaSayisi(context, toplamSayfa);
                        List<MasaustuOgesi> yeniList = new ArrayList<MasaustuOgesi>();
                        yeniList.add(new MasaustuOgesi(kimlik, 0, 0));
                        harita.put(yeniSayfa, yeniList);
                        mevcutKimlikler.add(kimlik);
                        degisiklik = true;
                    }
                }
            }

            if (degisiklik) {
                sayfalariKaydet(context, harita);
            }
            return degisiklik;
        }
    }

    /**
     * Tek Katman modunda veya genel eklemelerde boş bir hücre bulup yerleştirir,
     * tüm sayfalar doluysa otomatik yeni sayfa oluşturur.
     */
    public static boolean bostaHucreyeVeyaYeniSayfayaEkle(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return false;
        synchronized (MASAUSTU_KILIDI) {
            Map<Integer, List<MasaustuOgesi>> harita = sayfalariYukle(context);

            // Tek katmanlı (iOS) modda diğer sayfalardaki olası kopyayı da kaldır (katı tekillik)
            if (isTekKatmanModu(context)) {
                for (List<MasaustuOgesi> liste : harita.values()) {
                    if (liste != null) {
                        for (int i = liste.size() - 1; i >= 0; i--) {
                            if (DockYoneticisi.bilesenEslesiyorMu(liste.get(i).paketAdi, paketAdi)) {
                                liste.remove(i);
                            }
                        }
                    }
                }
            }

            int toplamSayfa = getSayfaSayisi(context);
            int sutun = AyarBankasi.getMasaustuSutun(context);
            int satir = AyarBankasi.getMasaustuSatir(context);
            int sayfaKapasite = Math.max(1, sutun * satir);

            for (int s = 0; s < toplamSayfa; s++) {
                List<MasaustuOgesi> sayfaList = harita.get(s);
                if (sayfaList == null) {
                    sayfaList = new ArrayList<MasaustuOgesi>();
                    harita.put(s, sayfaList);
                }
                if (sayfaList.size() < sayfaKapasite) {
                    for (int r = 0; r < satir; r++) {
                        for (int c = 0; c < sutun; c++) {
                            boolean dolu = false;
                            for (MasaustuOgesi o : sayfaList) {
                                if (hucreyiKapsiyorMu(o, c, r)) {
                                    dolu = true;
                                    break;
                                }
                            }
                            if (!dolu) {
                                sayfaList.add(new MasaustuOgesi(paketAdi, c, r));
                                sayfalariKaydet(context, harita);
                                return true;
                            }
                        }
                    }
                }
            }

            // Yeni sayfa aç
            int yeniSayfa = toplamSayfa;
            setSayfaSayisi(context, toplamSayfa + 1);
            List<MasaustuOgesi> yeniSayfaList = new ArrayList<MasaustuOgesi>();
            yeniSayfaList.add(new MasaustuOgesi(paketAdi, 0, 0));
            harita.put(yeniSayfa, yeniSayfaList);
            sayfalariKaydet(context, harita);
            return true;
        }
    }
}

