package com.tamekran.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * Tam Ekran - Uygulama Çekmecesi, Arama ve Akıllı İfade Kontrolcüsü
 * Arama metni filtreleme, T9 tuş takımı eşleşmeleri, kategori sekmeleri ve hesap makinesi kartını yönetir.
 */
public class AramaVeCekmeceKontrolcusu {

    public interface KategoriDegisimDinleyici {
        void onKategoriSecildi(String yeniKategori);
        void onSekmeleriYonetTiklandi();
    }

    /**
     * Menü Sekmelerini (Categories) oluşturur ve bağlar
     */
    public static void kategoriSekmeleriniYukle(
            final Context context,
            final LinearLayout kapsayiciKategoriSekmeleri,
            final String aktifKategori,
            final KategoriDegisimDinleyici dinleyici) {

        if (context == null || kapsayiciKategoriSekmeleri == null) return;
        kapsayiciKategoriSekmeleri.removeAllViews();

        List<String> kategoriler = KategoriYoneticisi.getKategoriler(context);
        int dp8 = ArayuzYardimcisi.dpToPx(context, 8);
        int dp14 = ArayuzYardimcisi.dpToPx(context, 14);
        int vurgu = TemaYoneticisi.getVurguRengi(context);

        for (final String kat : kategoriler) {
            final boolean secili = kat.equals(aktifKategori);
            TextView sekme = new TextView(context);
            sekme.setText(kat);
            sekme.setTextSize(12);
            sekme.setTextColor(secili ? Color.WHITE : Color.parseColor("#CCCCCC"));
            sekme.setBackgroundResource(secili ? R.drawable.sekme_secili_arkaplan : R.drawable.sekme_bos_arkaplan);
            sekme.setPadding(dp14, dp8, dp14, dp8);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, dp8, 0);
            sekme.setLayoutParams(lp);

            sekme.setClickable(true);
            sekme.setFocusable(true);
            sekme.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dinleyici != null) dinleyici.onKategoriSecildi(kat);
                }
            });

            kapsayiciKategoriSekmeleri.addView(sekme);
        }

        // Sekmeleri Yönet Butonu
        TextView sekmeYonetim = new TextView(context);
        sekmeYonetim.setText("⚙️ Sekmeler");
        sekmeYonetim.setTextSize(12);
        sekmeYonetim.setTextColor(vurgu);
        sekmeYonetim.setBackgroundResource(R.drawable.sekme_bos_arkaplan);
        sekmeYonetim.setPadding(dp14, dp8, dp14, dp8);

        LinearLayout.LayoutParams lpYonet = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sekmeYonetim.setLayoutParams(lpYonet);
        sekmeYonetim.setClickable(true);
        sekmeYonetim.setFocusable(true);
        sekmeYonetim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dinleyici != null) dinleyici.onSekmeleriYonetTiklandi();
            }
        });

        kapsayiciKategoriSekmeleri.addView(sekmeYonetim);
    }

    /**
     * Arama çubuğu metnine, T9 koduna ve seçili kategoriye göre uygulama listesini filtreler.
     * Matematiksel işlem tespit edilirse hesap makinesi kartını otomatik devreye sokar.
     */
    public static void aramaVeFiltrelemeYap(
            final Activity activity,
            final String metin,
            final List<UygulamaBilgisi> tumUygulamalarListesi,
            final String aktifKategori,
            final List<UygulamaBilgisi> filtrelenmisListe,
            final BaseAdapter menuAdaptoru,
            final View kartHesapMakinesi,
            final TextView yaziHesapSonuc,
            final View dugmeHesapKopyala) {

        if (activity == null || filtrelenmisListe == null) return;
        filtrelenmisListe.clear();

        final String aranan = metin != null ? metin.trim().toLowerCase(new Locale("tr", "TR")) : "";

        if (tumUygulamalarListesi != null) {
            for (UygulamaBilgisi app : tumUygulamalarListesi) {
                if (app == null) continue;

                // 1. Kategori Filtresi
                if (aktifKategori != null && !KategoriYoneticisi.KAT_TUMU.equals(aktifKategori)) {
                    String kat = KategoriYoneticisi.getKategori(activity, app);
                    if (!aktifKategori.equals(kat)) {
                        continue;
                    }
                }

                // 2. Metin & T9 Filtresi
                if (aranan.isEmpty()) {
                    filtrelenmisListe.add(app);
                } else {
                    boolean etiketUygun = app.etiket != null && app.etiket.toLowerCase(new Locale("tr", "TR")).contains(aranan);
                    boolean paketUygun = app.paketAdi != null && app.paketAdi.toLowerCase(new Locale("tr", "TR")).contains(aranan);
                    boolean t9Uygun = T9AramaYardimcisi.eslesiyorMu(aranan, app.etiket);
                    if (etiketUygun || paketUygun || t9Uygun) {
                        filtrelenmisListe.add(app);
                    }
                }
            }
        }

        if (menuAdaptoru != null) {
            menuAdaptoru.notifyDataSetChanged();
        }

        // 3. Akıllı Hesap Makinesi & İfade Çözücü
        HesapAramaYardimcisi.HesapSonucu hs = HesapAramaYardimcisi.hesapla(metin);
        if (hs.basarili && kartHesapMakinesi != null) {
            kartHesapMakinesi.setVisibility(View.VISIBLE);
            if (yaziHesapSonuc != null) {
                yaziHesapSonuc.setText("🧮 " + hs.ifade + " = " + hs.formatliSonuc);
            }
            final String kopyalanacak = hs.formatliSonuc;
            if (dugmeHesapKopyala != null) {
                dugmeHesapKopyala.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= 11) {
                                android.content.ClipboardManager cb = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("Hesaplama Sonucu", kopyalanacak);
                                cb.setPrimaryClip(clip);
                            } else {
                                android.text.ClipboardManager cb = (android.text.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                cb.setText(kopyalanacak);
                            }
                            ArayuzYardimcisi.toast(activity, "Sonuç panoya kopyalandı: " + kopyalanacak);
                        } catch (Throwable t) {
                            ArayuzYardimcisi.toast(activity, "Kopyalanamadı.");
                        }
                    }
                });
            }
        } else if (kartHesapMakinesi != null) {
            kartHesapMakinesi.setVisibility(View.GONE);
        }
    }
}
