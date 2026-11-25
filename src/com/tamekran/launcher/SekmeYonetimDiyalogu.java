package com.tamekran.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tam Ekran - Menü Sekmeleri ve Çekmece Kategori Yöneticisi (Nova Prime Drawer Groups)
 * Özel sekme oluşturma, adlandırma, silme ve uygulamaları sekmelere atama işlemlerini yönetir.
 */
public class SekmeYonetimDiyalogu {

    public interface SekmeGuncellemeDinleyicisi {
        void onSekmelerGuncellendi();
    }

    public static void goster(
        final Context context,
        final List<UygulamaBilgisi> tumUygulamalar,
        final SekmeGuncellemeDinleyicisi dinleyici
    ) {
        if (context == null) return;

        final List<String> kategoriler = KategoriYoneticisi.getKategoriler(context);
        List<String> gorunenList = new ArrayList<String>();
        gorunenList.add("➕ Yeni Sekme Oluştur...");

        for (String kat : kategoriler) {
            int sayi = KategoriYoneticisi.getKategoriUygulamaSayisi(context, kat, tumUygulamalar);
            gorunenList.add(kat + " (" + sayi + " uygulama)");
        }

        CharSequence[] secenekler = gorunenList.toArray(new CharSequence[0]);

        new AlertDialog.Builder(context)
            .setTitle("🏷️ Menü Sekmeleri & Kategori Yönetimi")
            .setItems(secenekler, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        // Yeni sekme oluştur
                        yeniSekmeDiyalogu(context, tumUygulamalar, dinleyici);
                    } else {
                        final String secilenKat = kategoriler.get(which - 1);
                        if (secilenKat.equals(KategoriYoneticisi.KAT_TUMU)) {
                            ArayuzYardimcisi.toast(context, context.getString(R.string.sekme_tumu_degismez));
                            return;
                        }
                        sekmeAksiyonDiyalogu(context, secilenKat, tumUygulamalar, dinleyici);
                    }
                }
            })
            .setNegativeButton("Kapat", null)
            .show();
    }

    private static void yeniSekmeDiyalogu(
        final Context context,
        final List<UygulamaBilgisi> tumUygulamalar,
        final SekmeGuncellemeDinleyicisi dinleyici
    ) {
        AyarDiyalogYardimcisi.metinGirdiDiyalogu(context, "➕ Yeni Sekme Oluştur", null, "Örn: Finans, İş, Oyunlar 2", new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
            @Override
            public void onMetinGirildi(String ad) {
                if (ad != null && ad.length() > 0) {
                    KategoriYoneticisi.kategoriEkle(context, ad);
                    ArayuzYardimcisi.toast(context, "'" + ad + "' sekmesi oluşturuldu.");
                    if (dinleyici != null) dinleyici.onSekmelerGuncellendi();
                    // Hemen ardından uygulama atama ekranını aç
                    uygulamaAtamaDiyalogu(context, ad, tumUygulamalar, dinleyici);
                }
            }
        });
    }

    private static void sekmeAksiyonDiyalogu(
        final Context context,
        final String kategori,
        final List<UygulamaBilgisi> tumUygulamalar,
        final SekmeGuncellemeDinleyicisi dinleyici
    ) {
        boolean varsayilan = kategori.equals(KategoriYoneticisi.KAT_ILETISIM) ||
                             kategori.equals(KategoriYoneticisi.KAT_ARACLAR) ||
                             kategori.equals(KategoriYoneticisi.KAT_MEDYA) ||
                             kategori.equals(KategoriYoneticisi.KAT_OYUNLAR);

        List<CharSequence> aksiyonlar = new ArrayList<CharSequence>();
        aksiyonlar.add("📋 Uygulamaları Ata / Seç");
        if (!varsayilan) {
            aksiyonlar.add("✏️ Sekmeyi Yeniden Adlandır");
            aksiyonlar.add("❌ Sekmeyi Sil");
        }

        final CharSequence[] liste = aksiyonlar.toArray(new CharSequence[0]);

        AyarDiyalogYardimcisi.listeSecimDiyalogu(context, "🏷️ " + kategori, liste, new AyarDiyalogYardimcisi.SecimDinleyicisi() {
            @Override
            public void onSecildi(int which) {
                if (which == 0) {
                    uygulamaAtamaDiyalogu(context, kategori, tumUygulamalar, dinleyici);
                } else if (which == 1) {
                    // Yeniden adlandır
                    AyarDiyalogYardimcisi.metinGirdiDiyalogu(context, "Sekmeyi Yeniden Adlandır", kategori, null, new AyarDiyalogYardimcisi.MetinGirdiDinleyicisi() {
                        @Override
                        public void onMetinGirildi(String yeni) {
                            if (yeni != null && yeni.length() > 0 && !yeni.equals(kategori)) {
                                KategoriYoneticisi.kategoriYenidenAdlandir(context, kategori, yeni);
                                ArayuzYardimcisi.toast(context, "Sekme adı güncellendi.");
                                if (dinleyici != null) dinleyici.onSekmelerGuncellendi();
                            }
                        }
                    });
                } else if (which == 2) {
                    // Sil
                    AyarDiyalogYardimcisi.onayDiyalogu(context, "Sekmeyi Sil", "'" + kategori + "' sekmesini silmek istediğinizden emin misiniz?", "Sil", new Runnable() {
                        @Override
                        public void run() {
                            KategoriYoneticisi.kategoriSil(context, kategori);
                            ArayuzYardimcisi.toast(context, "Sekme silindi.");
                            if (dinleyici != null) dinleyici.onSekmelerGuncellendi();
                        }
                    });
                }
            }
        });
    }

    /**
     * Seçilen kategoriye ait uygulamaları çoklu seçim kutularıyla atayan filtreli diyalog
     */
    private static void uygulamaAtamaDiyalogu(
        final Context context,
        final String kategori,
        final List<UygulamaBilgisi> tumUygulamalar,
        final SekmeGuncellemeDinleyicisi dinleyici
    ) {
        if (tumUygulamalar == null || tumUygulamalar.isEmpty()) return;

        final Set<String> seciliPaketler = new HashSet<String>();
        for (UygulamaBilgisi app : tumUygulamalar) {
            if (kategori.equals(KategoriYoneticisi.getKategori(context, app))) {
                seciliPaketler.add(app.paketAdi);
            }
        }

        final List<UygulamaBilgisi> filtrelenmis = new ArrayList<UygulamaBilgisi>(tumUygulamalar);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 8);

        final EditText aramaKutusu = new EditText(context);
        aramaKutusu.setHint("🔍 Uygulama ara...");
        layout.addView(aramaKutusu);

        final ListView lv = new ListView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 650);
        lv.setLayoutParams(lp);

        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return filtrelenmis.size();
            }

            @Override
            public Object getItem(int position) {
                return filtrelenmis.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final UygulamaBilgisi app = filtrelenmis.get(position);
                LinearLayout itemLayout = new LinearLayout(context);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setPadding(12, 12, 12, 12);
                itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                final CheckBox cb = new CheckBox(context);
                cb.setChecked(seciliPaketler.contains(app.paketAdi));
                cb.setClickable(false);

                TextView tv = new TextView(context);
                tv.setText(app.etiket);
                tv.setTextSize(14);
                tv.setTextColor(0xFFEEEEEE);
                tv.setPadding(12, 0, 0, 0);

                itemLayout.addView(cb);
                itemLayout.addView(tv);

                itemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (seciliPaketler.contains(app.paketAdi)) {
                            seciliPaketler.remove(app.paketAdi);
                            cb.setChecked(false);
                        } else {
                            seciliPaketler.add(app.paketAdi);
                            cb.setChecked(true);
                        }
                    }
                });

                return itemLayout;
            }
        };

        lv.setAdapter(adapter);
        layout.addView(lv);

        aramaKutusu.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String aranan = s.toString().toLowerCase(new Locale("tr", "TR")).trim();
                filtrelenmis.clear();
                for (UygulamaBilgisi app : tumUygulamalar) {
                    if (aranan.length() == 0 || (app.etiket != null && app.etiket.toLowerCase(new Locale("tr", "TR")).contains(aranan))) {
                        filtrelenmis.add(app);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        new AlertDialog.Builder(context)
            .setTitle("🏷️ " + kategori + " Uygulamaları")
            .setView(layout)
            .setPositiveButton("Kaydet", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    List<String> list = new ArrayList<String>(seciliPaketler);
                    KategoriYoneticisi.kategoriUygulamalariAta(context, kategori, list);
                    ArayuzYardimcisi.toast(context, seciliPaketler.size() + " uygulama '" + kategori + "' sekmesine atandı.");
                    if (dinleyici != null) dinleyici.onSekmelerGuncellendi();
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }
}
