package com.tamekran.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import java.util.List;

/**
 * Tam Ekran - Uygulama Çekmecesi Adaptörü (App Drawer Adapter)
 * AOSP Launcher3 tarzında bağımsız, bellek dostu ve ızgara satır yüksekliği dinamik ölçeklenen adaptör.
 */
public class CekmeceAdaptoru extends BaseAdapter {

    public interface UygulamaEtkilesimDinleyicisi {
        void onUygulamaTiklandi(UygulamaBilgisi app, View view, int position);
        boolean onUygulamaUzunTiklandi(UygulamaBilgisi app, View view, int position);
    }

    private final Context baglam;
    private final List<UygulamaBilgisi> liste;
    private final LayoutInflater sismeServisi;
    private GridView izgaraGorunumu;
    private UygulamaEtkilesimDinleyicisi etkilesimDinleyicisi;
    private int sutunSayisi = 4;
    private int satirSayisi = 6;
    private boolean kilavuzGoster = false;
    private int geciciSimgeBoyutu = -1;
    private Boolean geciciEtiketGoster = null;

    public CekmeceAdaptoru(Context baglam, List<UygulamaBilgisi> liste, GridView izgaraGorunumu) {
        this.baglam = baglam;
        this.liste = liste;
        this.izgaraGorunumu = izgaraGorunumu;
        this.sismeServisi = LayoutInflater.from(baglam);
    }

    public void setGeciciSimgeBoyutu(int olcek) {
        this.geciciSimgeBoyutu = olcek;
        notifyDataSetChanged();
    }

    public void setGeciciEtiketGoster(Boolean goster) {
        this.geciciEtiketGoster = goster;
        notifyDataSetChanged();
    }

    public void setIzgaraGorunumu(GridView izgara) {
        this.izgaraGorunumu = izgara;
    }

    public void setEtkilesimDinleyicisi(UygulamaEtkilesimDinleyicisi dinleyici) {
        this.etkilesimDinleyicisi = dinleyici;
    }

    public void setIzgara(int sutun, int satir, boolean kilavuz) {
        if (sutun >= 3 && sutun <= 12) this.sutunSayisi = sutun;
        if (satir >= 3 && satir <= 12) this.satirSayisi = satir;
        this.kilavuzGoster = kilavuz;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return liste != null ? liste.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return liste != null ? liste.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = sismeServisi.inflate(R.layout.oge_uygulama, parent, false);
        }

        // Kılavuz modu açıkken hücre sınırlarını vurgula
        if (kilavuzGoster) {
            convertView.setBackgroundResource(R.drawable.kilavuz_hucre_cerceve);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        // Çekmece satır sayısına göre öğe yüksekliğini dinamik ayarla
        if (izgaraGorunumu != null && izgaraGorunumu.getHeight() > 0 && satirSayisi > 0) {
            int padY = izgaraGorunumu.getPaddingTop() + izgaraGorunumu.getPaddingBottom();
            int usableH = izgaraGorunumu.getHeight() - padY;
            if (usableH > 0) {
                int satirYukseklik = usableH / satirSayisi;
                if (satirYukseklik > 0) {
                    ViewGroup.LayoutParams clp = convertView.getLayoutParams();
                    if (clp == null) {
                        convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, satirYukseklik));
                    } else if (clp.height != satirYukseklik) {
                        clp.height = satirYukseklik;
                        convertView.setLayoutParams(clp);
                    }
                }
            }
        }

        final UygulamaBilgisi app = (UygulamaBilgisi) getItem(position);
        if (app != null) {
            SharedPreferences pref = AyarBankasi.getAyarlar(baglam);
            boolean etiketlerGoster = (geciciEtiketGoster != null)
                    ? geciciEtiketGoster.booleanValue()
                    : (pref != null && pref.getBoolean(YedeklemeYoneticisi.PREF_ETIKETLERI_GOSTER, true));
            int olcek = (geciciSimgeBoyutu > 0) ? geciciSimgeBoyutu : AyarBankasi.getCekmeceSimgeBoyutu(baglam);

            if (sutunSayisi > 5 || satirSayisi > 6) {
                float carpan = 6.0f / Math.max(sutunSayisi, satirSayisi);
                int maxSimge = (int) (olcek * carpan);
                if (maxSimge > 0 && maxSimge < olcek) {
                    olcek = maxSimge;
                }
            }

            ArayuzYardimcisi.uygulamaGorunumuBagla(baglam, convertView, app, etiketlerGoster, olcek);

            final android.app.Activity aktivite = (baglam instanceof android.app.Activity) ? (android.app.Activity) baglam : null;
            final SuruklemeVerisi cekmeceVerisi = new SuruklemeVerisi(app, SuruklemeVerisi.KAYNAK_CEKMECE);
            NovaUzunBasmaYardimcisi.bagla(aktivite, convertView, new NovaUzunBasmaYardimcisi.UzunBasmaAksiyonu() {
                @Override
                public void onPopupGoster(View kaynakView) {
                    if (aktivite == null) return;
                    HizliEylemlerDiyalogu.gosterNovaPopup(
                            aktivite,
                            kaynakView,
                            app,
                            cekmeceVerisi,
                            new HizliEylemlerDiyalogu.IslemDinleyici() {
                                @Override
                                public void listeYenile() {
                                    if (aktivite instanceof AnaEkranActivity) {
                                        ((AnaEkranActivity) aktivite).uygulamalariYukle();
                                    }
                                }
                            },
                            null
                    );
                }

                @Override
                public boolean onSuruklemeBaslat(View kaynakView) {
                    return etkilesimDinleyicisi != null && etkilesimDinleyicisi.onUygulamaUzunTiklandi(app, kaynakView, position);
                }

                @Override
                public void onKisaTiklama(View kaynakView) {
                    if (etkilesimDinleyicisi != null) {
                        etkilesimDinleyicisi.onUygulamaTiklandi(app, kaynakView, position);
                    }
                }
            });
        }

        return convertView;
    }
}
