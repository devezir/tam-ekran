package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tam Ekran - Klasör Yönetim Motoru (Container Transition SSOT)
 * Masaüstünde ve menüde Nova / iOS SpringBoard tarzı klasörleme ve atomik konteyner desteği sağlar.
 */
public class KlasorYoneticisi {

    public static final int CIKARIM_BASARISIZ = 0;
    public static final int CIKARIM_NORMAL = 1;
    public static final int CIKARIM_VE_COZULDU = 2;
    public static final int CIKARIM_VE_SILINDI = 3;

    private static final String PREF_KLASORLER = "tamekran_klasorler_json";
    private static final Object KLASOR_KILIDI = new Object();

    public static class KlasorBilgisi {
        public String id;
        public String ad;
        public List<String> paketler = new ArrayList<String>();

        public KlasorBilgisi(String id, String ad) {
            this.id = id;
            this.ad = ad;
        }
    }

    public static List<KlasorBilgisi> klasorleriYukle(Context context) {
        if (!AyarBankasi.baglamGecerliMi(context)) return new ArrayList<KlasorBilgisi>();
        synchronized (KLASOR_KILIDI) {
            List<KlasorBilgisi> liste = new ArrayList<KlasorBilgisi>();
            SharedPreferences sp = AyarBankasi.getAyarlar(context);
            String hamJson = sp.getString(PREF_KLASORLER, "[]");

            try {
                JSONArray arr = new JSONArray(hamJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    KlasorBilgisi k = new KlasorBilgisi(obj.getString("id"), obj.getString("ad"));
                    k.paketler = JsonYardimcisi.jsonArrayToStringListesi(obj.optJSONArray("paketler"));
                    liste.add(k);
                }
            } catch (Throwable t) {
                LauncherLog.w("KlasorYoneticisi", "klasorleriYukle JSON okuma hatası", t);
            }
            return liste;
        }
    }

    public static void klasorleriKaydet(Context context, List<KlasorBilgisi> liste) {
        if (!AyarBankasi.baglamGecerliMi(context) || liste == null) return;
        synchronized (KLASOR_KILIDI) {
            try {
                JSONArray arr = new JSONArray();
                for (KlasorBilgisi k : liste) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", k.id);
                    obj.put("ad", k.ad);
                    obj.put("paketler", JsonYardimcisi.stringListesiToJsonArray(k.paketler));
                    arr.put(obj);
                }
                SharedPreferences sp = AyarBankasi.getAyarlar(context);
                AyarBankasi.editorKaydet(sp.edit().putString(PREF_KLASORLER, arr.toString()));
                MasaustuModelDeposu.v2ModeliniSifirla(context);
            } catch (Throwable t) {
                LauncherLog.e("KlasorYoneticisi", "klasorleriKaydet JSON yazma hatası", t);
            }
        }
    }

    public static KlasorBilgisi klasorGetir(Context context, String klasorId) {
        if (context == null || klasorId == null) return null;
        List<KlasorBilgisi> tumu = klasorleriYukle(context);
        for (KlasorBilgisi kb : tumu) {
            if (kb.id.equals(klasorId)) return kb;
        }
        return null;
    }

    public static KlasorBilgisi klasorOlustur(Context context, String ad, String ilkPaket) {
        return klasorOlustur(context, ad, ilkPaket, null);
    }

    public static KlasorBilgisi klasorOlustur(Context context, String ad, String paket1, String paket2) {
        if (!AyarBankasi.baglamGecerliMi(context)) return null;
        synchronized (KLASOR_KILIDI) {
            List<KlasorBilgisi> tumu = klasorleriYukle(context);
            KlasorBilgisi yeni = new KlasorBilgisi(String.valueOf(System.currentTimeMillis()), ad != null && !ad.trim().isEmpty() ? ad : "Klasör");
            if (paket1 != null && paket1.length() > 0) {
                yeni.paketler.add(paket1);
            }
            if (paket2 != null && paket2.length() > 0) {
                boolean ayniMi = false;
                for (String p : yeni.paketler) {
                    if (DockYoneticisi.bilesenEslesiyorMu(p, paket2)) {
                        ayniMi = true;
                        break;
                    }
                }
                if (!ayniMi) {
                    yeni.paketler.add(paket2);
                }
            }
            tumu.add(yeni);
            klasorleriKaydet(context, tumu);
            KlasorSimgeUretici.onbellegiTemizle();
            return yeni;
        }
    }

    public static boolean klasoreUygulamaEkle(Context context, String klasorId, String paketAdi) {
        if (context == null || klasorId == null || paketAdi == null) return false;
        synchronized (KLASOR_KILIDI) {
            List<KlasorBilgisi> tumu = klasorleriYukle(context);
            for (KlasorBilgisi kb : tumu) {
                if (kb.id.equals(klasorId)) {
                    // Mükerrer kontrolü FQCN uyumlu
                    for (String varOlan : kb.paketler) {
                        if (DockYoneticisi.bilesenEslesiyorMu(varOlan, paketAdi)) {
                            return false;
                        }
                    }
                    kb.paketler.add(paketAdi);
                    klasorleriKaydet(context, tumu);

                    // Tek Katmanlı (iOS) modda katı tekillik: Masaüstündeki açık kopyayı kaldır
                    if (MasaustuYoneticisi.isTekKatmanModu(context)) {
                        MasaustuYoneticisi.masaustundenKaldir(context, paketAdi);
                    }

                    KlasorSimgeUretici.onbellegiTemizle();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Klasörden bir uygulamayı çıkarır (Endüstri Standardı Atomik Konteyner Transferi).
     * - Uygulamayı klasörden siler.
     * - Masaüstüne boş hücreye yerleştirir (sayfa doluysa sonraki sayfaya veya yeni sayfaya taşar).
     * - Klasörde 1 öğe kalırsa otomatik çözülmeyi (auto-unfold) tetikler.
     * - Klasör boşalırsa klasörü tamamen siler.
     */
    public static int klasordenUygulamaCikar(Context context, String klasorId, String cikarilacakKimlik) {
        if (context == null || klasorId == null || cikarilacakKimlik == null) return CIKARIM_BASARISIZ;
        synchronized (KLASOR_KILIDI) {
            List<KlasorBilgisi> tumu = klasorleriYukle(context);
            KlasorBilgisi bulunanKlasor = null;
            for (KlasorBilgisi kb : tumu) {
                if (kb.id.equals(klasorId)) {
                    bulunanKlasor = kb;
                    break;
                }
            }
            if (bulunanKlasor == null || bulunanKlasor.paketler == null) return CIKARIM_BASARISIZ;

            // 1. Eşleşen öğeyi FQCN desteğiyle sil
            boolean silindi = false;
            String tamSilinenKimlik = null;
            for (int i = bulunanKlasor.paketler.size() - 1; i >= 0; i--) {
                String p = bulunanKlasor.paketler.get(i);
                if (DockYoneticisi.bilesenEslesiyorMu(p, cikarilacakKimlik)) {
                    tamSilinenKimlik = p;
                    bulunanKlasor.paketler.remove(i);
                    silindi = true;
                }
            }
            if (!silindi) return CIKARIM_BASARISIZ;

            // Çıkarılan öğe kimliği: eğer tam kimlik bulunduysa onu, yoksa cikarilacakKimlik'i kullan
            String yerlesecekKimlik = (tamSilinenKimlik != null) ? tamSilinenKimlik : cikarilacakKimlik;

            // 2. Klasörün bulunduğu sayfayı veya dock durumunu tespit et
            int hedefSayfa = 0;
            boolean masaustundeBulundu = false;
            String klasorEtiketi = "klasor:" + klasorId;

            Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> harita = MasaustuYoneticisi.sayfalariYukle(context);
            for (Map.Entry<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> entry : harita.entrySet()) {
                if (entry.getValue() != null) {
                    for (MasaustuYoneticisi.MasaustuOgesi o : entry.getValue()) {
                        if (klasorEtiketi.equals(o.paketAdi)) {
                            hedefSayfa = entry.getKey();
                            masaustundeBulundu = true;
                            break;
                        }
                    }
                }
                if (masaustundeBulundu) break;
            }

            // 3. Çıkarılan uygulamayı masaüstüne yerleştir (Taşma/Spillover Korumalı)
            boolean eklendi = false;
            if (masaustundeBulundu) {
                eklendi = MasaustuYoneticisi.sayfayaOgeEkle(context, hedefSayfa, yerlesecekKimlik);
            }
            if (!eklendi) {
                // Sayfa doluysa veya dock'taysa sonraki sayfalara taşır veya yeni sayfa aç
                MasaustuYoneticisi.bostaHucreyeVeyaYeniSayfayaEkle(context, yerlesecekKimlik);
            }

            // 4. Kalan öğe sayısına göre Otomatik Çözülme (Auto-Unfold) veya Temizleme
            int kalanSayi = bulunanKlasor.paketler.size();
            int sonuc = CIKARIM_NORMAL;

            if (kalanSayi == 1) {
                String kalanPaket = bulunanKlasor.paketler.get(0);
                if (masaustundeBulundu) {
                    MasaustuYoneticisi.sayfadaOgeDegistir(context, hedefSayfa, klasorEtiketi, kalanPaket);
                } else if (DockYoneticisi.docktaVarMi(context, klasorEtiketi)) {
                    List<String> dockListe = DockYoneticisi.dockYukle(context);
                    int idx = dockListe.indexOf(klasorEtiketi);
                    if (idx >= 0) {
                        dockListe.set(idx, kalanPaket);
                        DockYoneticisi.dockKaydet(context, dockListe);
                    }
                } else {
                    MasaustuYoneticisi.bostaHucreyeVeyaYeniSayfayaEkle(context, kalanPaket);
                }

                // Klasörü veritabanından tamamen sil
                tumu.remove(bulunanKlasor);
                klasorleriKaydet(context, tumu);
                sonuc = CIKARIM_VE_COZULDU;
            } else if (kalanSayi == 0) {
                // Boş kalan klasörü her yerden sil
                MasaustuYoneticisi.masaustundenKaldir(context, klasorEtiketi);
                DockYoneticisi.docktanKaldir(context, klasorEtiketi);
                tumu.remove(bulunanKlasor);
                klasorleriKaydet(context, tumu);
                sonuc = CIKARIM_VE_SILINDI;
            } else {
                // 2 veya daha fazla öğe kaldı, klasörü kaydet
                klasorleriKaydet(context, tumu);
                sonuc = CIKARIM_NORMAL;
            }

            KlasorSimgeUretici.onbellegiTemizle();
            return sonuc;
        }
    }

    /**
     * Klasörü siler ve içindeki tüm uygulamaları masaüstüne güvenle boşaltır (Spillover Korumalı).
     * iOS / Tek Katman modunda hiçbir uygulamanın kaybolmamasını garanti eder.
     */
    public static boolean klasorUygulamalariniMasaustuneDokVeSil(Context context, String klasorId) {
        if (context == null || klasorId == null) return false;
        synchronized (KLASOR_KILIDI) {
            List<KlasorBilgisi> tumu = klasorleriYukle(context);
            KlasorBilgisi hedefKlasor = null;
            for (KlasorBilgisi kb : tumu) {
                if (kb.id.equals(klasorId)) {
                    hedefKlasor = kb;
                    break;
                }
            }
            if (hedefKlasor == null) return false;

            String klasorEtiketi = "klasor:" + klasorId;

            // Klasörün bulunduğu sayfayı tespit et
            int hedefSayfa = 0;
            boolean masaustundeBulundu = false;
            Map<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> harita = MasaustuYoneticisi.sayfalariYukle(context);
            for (Map.Entry<Integer, List<MasaustuYoneticisi.MasaustuOgesi>> entry : harita.entrySet()) {
                if (entry.getValue() != null) {
                    for (MasaustuYoneticisi.MasaustuOgesi o : entry.getValue()) {
                        if (klasorEtiketi.equals(o.paketAdi)) {
                            hedefSayfa = entry.getKey();
                            masaustundeBulundu = true;
                            break;
                        }
                    }
                }
                if (masaustundeBulundu) break;
            }

            // 1. Klasörü masaüstü ve docktan sil (böylece hücresi boşa çıkar ve dökülen ilk öğe buraya oturur)
            MasaustuYoneticisi.masaustundenKaldir(context, klasorEtiketi);
            DockYoneticisi.docktanKaldir(context, klasorEtiketi);

            // 2. Klasördeki tüm uygulamaları sırayla masaüstüne dök (Taşma / Spillover korumalı)
            if (hedefKlasor.paketler != null) {
                for (String pkg : hedefKlasor.paketler) {
                    if (pkg == null || pkg.trim().isEmpty()) continue;
                    boolean eklendi = false;
                    if (masaustundeBulundu) {
                        eklendi = MasaustuYoneticisi.sayfayaOgeEkle(context, hedefSayfa, pkg);
                    }
                    if (!eklendi) {
                        MasaustuYoneticisi.bostaHucreyeVeyaYeniSayfayaEkle(context, pkg);
                    }
                }
            }

            // 3. Klasörü veritabanından sil
            tumu.remove(hedefKlasor);
            klasorleriKaydet(context, tumu);
            KlasorSimgeUretici.onbellegiTemizle();
            return true;
        }
    }

    public static void klasorSil(Context context, String klasorId) {
        if (context == null || klasorId == null) return;
        synchronized (KLASOR_KILIDI) {
            List<KlasorBilgisi> tumu = klasorleriYukle(context);
            for (int i = 0; i < tumu.size(); i++) {
                if (tumu.get(i).id.equals(klasorId)) {
                    tumu.remove(i);
                    break;
                }
            }
            klasorleriKaydet(context, tumu);
            KlasorSimgeUretici.onbellegiTemizle();
        }
    }
}

