package com.tamekran.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran Tasarım Felsefesi — İçerik ve Ekran Duyarlı Akış Düzeni.
 * Alt görünümleri (ör. butonları) önce kendi metin genişliklerine göre ölçer;
 * satıra sığanları yan yana dizer ve ekstra boşluğu eşit paylaştırır,
 * sığmayanları ise otomatik olarak alt satıra geçirir (API 8+, sıfır kütüphane).
 */
public class AkisSatiriLayout extends ViewGroup {

    private int yatayAralik;
    private int dikeyAralik;
    private boolean satiriDoldur = true;

    private final List<List<View>> satirlar = new ArrayList<List<View>>();
    private final List<Integer> satirYukseklikleri = new ArrayList<Integer>();

    public AkisSatiriLayout(Context context) {
        super(context);
        baslat(context);
    }

    public AkisSatiriLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        baslat(context);
    }

    public AkisSatiriLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        baslat(context);
    }

    private void baslat(Context context) {
        int dp8 = ArayuzYardimcisi.dpToPx(context, 8);
        yatayAralik = dp8;
        dikeyAralik = dp8;
    }

    public void setAralik(int yatayPx, int dikeyPx) {
        this.yatayAralik = Math.max(0, yatayPx);
        this.dikeyAralik = Math.max(0, dikeyPx);
        requestLayout();
    }

    public void setSatiriDoldur(boolean satiriDoldur) {
        this.satiriDoldur = satiriDoldur;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        satirlar.clear();
        satirYukseklikleri.clear();

        int genislikModu = MeasureSpec.getMode(widthMeasureSpec);
        int toplamGenislik = MeasureSpec.getSize(widthMeasureSpec);
        int kullanilabilirGenislik = (genislikModu == MeasureSpec.UNSPECIFIED)
                ? Integer.MAX_VALUE
                : Math.max(0, toplamGenislik - getPaddingLeft() - getPaddingRight());

        int cocukSayisi = getChildCount();
        List<View> mevcutSatir = new ArrayList<View>();
        int mevcutSatirGenislik = 0;
        int mevcutSatirYukseklik = 0;

        // 1. Aşama: Her görünümü doğal içerik boyutuna göre ölç ve satırlara böl
        for (int i = 0; i < cocukSayisi; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            int childWidthSpec = (kullanilabilirGenislik == Integer.MAX_VALUE)
                    ? MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    : MeasureSpec.makeMeasureSpec(kullanilabilirGenislik, MeasureSpec.AT_MOST);
            int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            child.measure(childWidthSpec, childHeightSpec);

            int cw = child.getMeasuredWidth();
            int ch = child.getMeasuredHeight();

            int eklenecekGenislik = mevcutSatir.isEmpty() ? cw : (yatayAralik + cw);
            if (!mevcutSatir.isEmpty() && mevcutSatirGenislik + eklenecekGenislik > kullanilabilirGenislik) {
                satirlar.add(mevcutSatir);
                satirYukseklikleri.add(mevcutSatirYukseklik);
                mevcutSatir = new ArrayList<View>();
                mevcutSatirGenislik = cw;
                mevcutSatirYukseklik = ch;
            } else {
                mevcutSatirGenislik += eklenecekGenislik;
                if (ch > mevcutSatirYukseklik) {
                    mevcutSatirYukseklik = ch;
                }
            }
            mevcutSatir.add(child);
        }

        if (!mevcutSatir.isEmpty()) {
            satirlar.add(mevcutSatir);
            satirYukseklikleri.add(mevcutSatirYukseklik);
        }

        // 2. Aşama: Satır içi boşluğu paylaştır ve aynı satırdaki butonları eşit yüksekliğe getir
        int enGenisSatir = 0;
        int toplamYukseklik = getPaddingTop() + getPaddingBottom();

        for (int s = 0; s < satirlar.size(); s++) {
            List<View> satir = satirlar.get(s);
            int n = satir.size();
            int dogalToplam = 0;
            for (int j = 0; j < n; j++) {
                dogalToplam += satir.get(j).getMeasuredWidth();
            }
            int aralikToplami = (n > 1) ? (n - 1) * yatayAralik : 0;
            int satirDogalGenislik = dogalToplam + aralikToplami;

            int maksYukseklik = satirYukseklikleri.get(s);
            if (satiriDoldur && kullanilabilirGenislik != Integer.MAX_VALUE && n > 0) {
                int ekstra = Math.max(0, kullanilabilirGenislik - satirDogalGenislik);
                int pay = ekstra / n;
                int artik = ekstra % n;
                int yeniMaksYukseklik = 0;
                for (int j = 0; j < n; j++) {
                    View child = satir.get(j);
                    int hedefGenislik = child.getMeasuredWidth() + pay + (j == n - 1 ? artik : 0);
                    child.measure(
                            MeasureSpec.makeMeasureSpec(hedefGenislik, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    );
                    if (child.getMeasuredHeight() > yeniMaksYukseklik) {
                        yeniMaksYukseklik = child.getMeasuredHeight();
                    }
                }
                maksYukseklik = Math.max(maksYukseklik, yeniMaksYukseklik);
                for (int j = 0; j < n; j++) {
                    View child = satir.get(j);
                    child.measure(
                            MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(maksYukseklik, MeasureSpec.EXACTLY)
                    );
                }
                satirYukseklikleri.set(s, maksYukseklik);
                enGenisSatir = Math.max(enGenisSatir, kullanilabilirGenislik);
            } else {
                enGenisSatir = Math.max(enGenisSatir, satirDogalGenislik);
            }

            toplamYukseklik += maksYukseklik;
            if (s < satirlar.size() - 1) {
                toplamYukseklik += dikeyAralik;
            }
        }

        int olculenGenislik = (genislikModu == MeasureSpec.EXACTLY)
                ? toplamGenislik
                : Math.min(toplamGenislik, enGenisSatir + getPaddingLeft() + getPaddingRight());
        if (genislikModu == MeasureSpec.UNSPECIFIED) {
            olculenGenislik = enGenisSatir + getPaddingLeft() + getPaddingRight();
        }

        setMeasuredDimension(olculenGenislik, resolveSize(toplamYukseklik, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int y = getPaddingTop();
        for (int s = 0; s < satirlar.size(); s++) {
            List<View> satir = satirlar.get(s);
            int satirH = satirYukseklikleri.get(s);
            int x = getPaddingLeft();
            for (int j = 0; j < satir.size(); j++) {
                View child = satir.get(j);
                int cw = child.getMeasuredWidth();
                child.layout(x, y, x + cw, y + satirH);
                x += cw + yatayAralik;
            }
            y += satirH + dikeyAralik;
        }
    }
}
