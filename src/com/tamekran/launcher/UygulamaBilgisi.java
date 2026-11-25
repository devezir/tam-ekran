package com.tamekran.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import java.text.Collator;
import java.util.List;
import java.util.Locale;

/**
 * Tam Ekran - Uygulama Bilgi Modeli
 * Saf Java, sıfır kütüphane, Türkçe alfabetik sıralama desteği.
 */
public class UygulamaBilgisi implements Comparable<UygulamaBilgisi> {
    public String etiket;
    public String paketAdi;
    public String etkinlikAdi;
    public Drawable simge;
    public Intent baslatmaNiyeti;

    private static final Collator TURKCE_KARSILASTIRICI = Collator.getInstance(new Locale("tr", "TR"));

    public UygulamaBilgisi(String etiket, String paketAdi, String etkinlikAdi, Drawable simge) {
        this.etiket = etiket != null ? etiket : "";
        this.paketAdi = paketAdi;
        this.etkinlikAdi = etkinlikAdi;
        this.simge = simge;

        try {
            this.baslatmaNiyeti = new Intent(Intent.ACTION_MAIN);
            this.baslatmaNiyeti.addCategory(Intent.CATEGORY_LAUNCHER);
            this.baslatmaNiyeti.setComponent(new ComponentName(paketAdi, etkinlikAdi));
            this.baslatmaNiyeti.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        } catch (Throwable t) {
            // JVM birim test veya stub ortamında Intent başlatma istisnasını yut
        }
    }

    public UygulamaBilgisi(String etiket, String paketAdi, Drawable simge, Intent baslatmaNiyeti) {
        this.etiket = etiket != null ? etiket : "";
        this.paketAdi = paketAdi;
        this.etkinlikAdi = "";
        this.simge = simge;
        this.baslatmaNiyeti = baslatmaNiyeti;
    }

    @Override
    public int compareTo(UygulamaBilgisi diger) {
        if (diger == null || diger.etiket == null) return 1;
        if (this.etiket == null) return -1;
        return TURKCE_KARSILASTIRICI.compare(this.etiket, diger.etiket);
    }

    /**
     * Masaüstü ve Dock kayıtları için benzersiz bileşen kimliği döner (Örn: "com.android.contacts/com.android.contacts.DialtactsActivity")
     * Özel ögeler (klasor:, arac:, kisayol:) için doğrudan paket adını döner.
     */
    public String getBilesenKimligi() {
        if (paketAdi == null) return "";
        if (paketAdi.startsWith("klasor:") || paketAdi.startsWith("arac:") || paketAdi.startsWith("kisayol:")) {
            return paketAdi;
        }
        if (etkinlikAdi != null && etkinlikAdi.trim().length() > 0) {
            return paketAdi + "/" + etkinlikAdi;
        }
        return paketAdi;
    }

    /**
     * İki bileşen kimliğinin (veya paket adının) eşleşip eşleşmediğini kontrol eder.
     * Hem tam bileşen eşleşmesini (pkg/cls == pkg/cls) hem de paket kaldırma/eski kayıt durumunda
     * paket bazlı geriye dönük eşleşmeyi destekler.
     */
    public static boolean bilesenEslesiyorMu(String kayitliKimlik, String hedefKimlik) {
        if (kayitliKimlik == null || hedefKimlik == null) return false;
        if (kayitliKimlik.equals(hedefKimlik)) return true;

        String kayitliPaket = kayitliKimlik.contains("/") ? kayitliKimlik.split("/", 2)[0] : kayitliKimlik;
        String hedefPaket = hedefKimlik.contains("/") ? hedefKimlik.split("/", 2)[0] : hedefKimlik;

        // Hedef yalnızca paket adı ise (örn: uygulama kaldırma veya paket bazlı işlem)
        if (!hedefKimlik.contains("/") && kayitliPaket.equals(hedefKimlik)) {
            return true;
        }

        // Kayıtlı öge eski sürümden kalan yalın paket adı ise
        if (!kayitliKimlik.contains("/") && kayitliKimlik.equals(hedefPaket)) {
            return true;
        }

        return false;
    }

    /**
     * Verilen listeden kayıtlı kimliğe (paket adı veya paket/etkinlik) en uygun UygulamaBilgisi nesnesini bulur.
     * Önce tam bileşen eşleşmesine bakar, bulunamazsa geriye dönük uyumluluk için paket adına bakar.
     */
    public static UygulamaBilgisi bilesenBul(List<UygulamaBilgisi> liste, String kayitliKimlik) {
        if (liste == null || kayitliKimlik == null) return null;

        // 1. Aşama: Tam bileşen eşleşmesi (paketAdi + "/" + etkinlikAdi)
        for (UygulamaBilgisi u : liste) {
            if (u != null && kayitliKimlik.equals(u.getBilesenKimligi())) {
                return u;
            }
        }

        // 2. Aşama: Geriye dönük uyumluluk (yalnızca paket adı eşleşmesi)
        String safPaket = kayitliKimlik.contains("/") ? kayitliKimlik.split("/", 2)[0] : kayitliKimlik;
        for (UygulamaBilgisi u : liste) {
            if (u != null && safPaket.equals(u.paketAdi)) {
                return u;
            }
        }

        return null;
    }

    /**
     * Masaüstü, Dock ve Klasörlerdeki herhangi bir bileşen kimliğini (klasor:, kisayol:, arac: veya paket/etkinlik)
     * tek merkezden UygulamaBilgisi nesnesine çözümler (DRY).
     */
    public static UygulamaBilgisi kimliktenCozumle(
            android.content.Context context,
            String kimlik,
            List<UygulamaBilgisi> tumUygulamalar,
            List<KlasorYoneticisi.KlasorBilgisi> tumKlasorler) {
        if (context == null || kimlik == null) return null;
        if (kimlik.startsWith("klasor:")) {
            String kId = kimlik.substring("klasor:".length());
            List<KlasorYoneticisi.KlasorBilgisi> klasorler = (tumKlasorler != null)
                    ? tumKlasorler
                    : KlasorYoneticisi.klasorleriYukle(context);
            for (KlasorYoneticisi.KlasorBilgisi kb : klasorler) {
                if (kb != null && kId.equals(kb.id)) {
                    Drawable klasorSimgesi = KlasorSimgeUretici.klasorSimgesiOlustur(context, kb, tumUygulamalar);
                    return new UygulamaBilgisi("📁 " + kb.ad, kimlik, "", klasorSimgesi);
                }
            }
            return null;
        }
        if (kimlik.startsWith("kisayol:")) {
            return KisayolYoneticisi.kisayolUygulamasiGetir(context, kimlik);
        }
        if (kimlik.startsWith("arac:")) {
            return HizliAraclarYoneticisi.aracUygulamaBilgisiAl(context, kimlik);
        }
        return bilesenBul(tumUygulamalar, kimlik);
    }
}
