package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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
 * Tam Ekran - Çoklu Seçimli Uygulama Gizleme Yöneticisi
 * Nova Launcher tarzında menüden gizlenecek uygulamaları onay kutuları ve anlık arama ile
 * topluca yönetir.
 */
public class UygulamaGizlemeDiyalogu {

    public static void goster(final Activity activity, final List<UygulamaBilgisi> tumUygulamalar, final Runnable guncellendiGorevi) {
        if (activity == null || tumUygulamalar == null) return;

        final List<String> mevcutGizliler = YedeklemeYoneticisi.gizlenenleriAl(activity);
        final Set<String> seciliPaketler = new HashSet<String>(mevcutGizliler);
        final List<UygulamaBilgisi> filtrelenmis = new ArrayList<UygulamaBilgisi>(tumUygulamalar);

        int pad = ArayuzYardimcisi.dpToPx(activity, 16);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundColor(0xFF1E1E1E);

        // Başlık
        TextView baslik = new TextView(activity);
        baslik.setText("👁️ Uygulamaları Gizle / Göster");
        baslik.setTextSize(18);
        baslik.setTypeface(null, android.graphics.Typeface.BOLD);
        baslik.setTextColor(Color.WHITE);
        layout.addView(baslik);

        TextView aciklama = new TextView(activity);
        aciklama.setText("İşaretlenen uygulamalar 'Tüm Uygulamalar' çekmecesinden gizlenir.");
        aciklama.setTextSize(12);
        aciklama.setTextColor(Color.LTGRAY);
        layout.addView(aciklama);

        // Arama Kutusu
        final EditText aramaGirdi = new EditText(activity);
        aramaGirdi.setHint("Gizlenecek uygulamayı ara...");
        aramaGirdi.setHintTextColor(Color.GRAY);
        aramaGirdi.setTextColor(Color.WHITE);
        aramaGirdi.setBackgroundResource(R.drawable.arama_arkaplan);
        int pad12 = ArayuzYardimcisi.dpToPx(activity, 12);
        int pad8 = ArayuzYardimcisi.dpToPx(activity, 8);
        aramaGirdi.setPadding(pad12, pad8, pad12, pad8);
        LinearLayout.LayoutParams lpArama = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ArayuzYardimcisi.dpToPx(activity, 40));
        lpArama.setMargins(0, ArayuzYardimcisi.dpToPx(activity, 10), 0, pad8);
        aramaGirdi.setLayoutParams(lpArama);
        layout.addView(aramaGirdi);

        // Liste
        final ListView listView = new ListView(activity);
        LinearLayout.LayoutParams lpList = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ArayuzYardimcisi.dpToPx(activity, 320));
        listView.setLayoutParams(lpList);

        final BaseAdapter adaptor = new BaseAdapter() {
            @Override
            public int getCount() {
                return filtrelenmis.size();
            }

            @Override
            public UygulamaBilgisi getItem(int position) {
                return filtrelenmis.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout row;
                if (convertView != null) {
                    row = (LinearLayout) convertView;
                } else {
                    row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    int pad8 = ArayuzYardimcisi.dpToPx(activity, 8);
                    row.setPadding(0, pad8, 0, pad8);

                    ImageView icon = new ImageView(activity);
                    icon.setId(101);
                    int dp36 = ArayuzYardimcisi.dpToPx(activity, 36);
                    LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dp36, dp36);
                    lpImg.setMargins(0, 0, ArayuzYardimcisi.dpToPx(activity, 10), 0);
                    icon.setLayoutParams(lpImg);
                    row.addView(icon);

                    TextView name = new TextView(activity);
                    name.setId(102);
                    name.setTextSize(14);
                    name.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams lpText = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    name.setLayoutParams(lpText);
                    row.addView(name);

                    CheckBox cb = new CheckBox(activity);
                    cb.setId(103);
                    cb.setFocusable(false);
                    cb.setClickable(false);
                    row.addView(cb);
                }

                final UygulamaBilgisi app = getItem(position);
                ImageView icon = (ImageView) row.findViewById(101);
                TextView name = (TextView) row.findViewById(102);
                CheckBox cb = (CheckBox) row.findViewById(103);

                icon.setImageDrawable(app.simge);
                name.setText(app.etiket);
                cb.setChecked(seciliPaketler.contains(app.paketAdi));

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (seciliPaketler.contains(app.paketAdi)) {
                            seciliPaketler.remove(app.paketAdi);
                        } else {
                            seciliPaketler.add(app.paketAdi);
                        }
                        notifyDataSetChanged();
                    }
                });

                return row;
            }
        };

        listView.setAdapter(adaptor);
        layout.addView(listView);

        // Anlık Arama Filtresi
        aramaGirdi.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase(new Locale("tr", "TR"));
                filtrelenmis.clear();
                if (q.length() == 0) {
                    filtrelenmis.addAll(tumUygulamalar);
                } else {
                    for (UygulamaBilgisi app : tumUygulamalar) {
                        if ((app.etiket != null && app.etiket.toLowerCase(new Locale("tr", "TR")).contains(q)) ||
                            (app.paketAdi != null && app.paketAdi.toLowerCase(new Locale("tr", "TR")).contains(q))) {
                            filtrelenmis.add(app);
                        }
                    }
                }
                adaptor.notifyDataSetChanged();
            }
        });

        // Alt Butonlar (Kaydet ve Temizle)
        LinearLayout altBar = new LinearLayout(activity);
        altBar.setOrientation(LinearLayout.HORIZONTAL);
        altBar.setPadding(0, ArayuzYardimcisi.dpToPx(activity, 8), 0, 0);

        int dp40 = ArayuzYardimcisi.dpToPx(activity, 40);

        Button btnTemizle = new Button(activity);
        btnTemizle.setText("Tümünü Göster");
        btnTemizle.setBackgroundResource(R.drawable.kart_arkaplan);
        btnTemizle.setTextColor(Color.LTGRAY);
        LinearLayout.LayoutParams lpTemizle = new LinearLayout.LayoutParams(0, dp40, 1.0f);
        lpTemizle.setMargins(0, 0, 8, 0);
        btnTemizle.setLayoutParams(lpTemizle);
        altBar.addView(btnTemizle);

        Button btnKaydet = new Button(activity);
        btnKaydet.setText("Kaydet (" + seciliPaketler.size() + ")");
        btnKaydet.setBackgroundResource(R.drawable.kart_arkaplan);
        btnKaydet.setTextColor(activity.getResources().getColor(R.color.renk_vurgu));
        btnKaydet.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lpKaydet = new LinearLayout.LayoutParams(0, dp40, 1.0f);
        btnKaydet.setLayoutParams(lpKaydet);
        altBar.addView(btnKaydet);

        layout.addView(altBar);

        final AlertDialog diyalog = new AlertDialog.Builder(activity)
            .setView(layout)
            .setNegativeButton("İptal", null)
            .create();

        btnTemizle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seciliPaketler.clear();
                adaptor.notifyDataSetChanged();
                btnKaydet.setText("Kaydet (0)");
            }
        });

        btnKaydet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YedeklemeYoneticisi.gizlenenleriKaydet(activity, new ArrayList<String>(seciliPaketler));
                ArayuzYardimcisi.toast(activity, seciliPaketler.size() + " uygulama gizlendi.");
                diyalog.dismiss();
                if (guncellendiGorevi != null) {
                    guncellendiGorevi.run();
                }
            }
        });

        diyalog.show();
    }
}
