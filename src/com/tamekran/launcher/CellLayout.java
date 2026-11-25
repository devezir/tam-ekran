package com.tamekran.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Tam Ekran - Hücre Matrisi Çalışma Alanı (CellLayout)
 * AOSP Launcher3 / Nova Launcher standartlarında 2 boyutlu satranç tahtası ızgara mimarisi.
 * Android 2.2'den Android 17'ye kadar sıfır harici bağımlılıkla özgür koordinat yerleşimi sunar.
 */
public final class CellLayout extends ViewGroup {

    private int sutunSayisi = 4;
    private int satirSayisi = 6;
    private boolean kilavuzGoster = false;
    private Paint kilavuzCizgiBoyasi;
    private Paint kilavuzYaziBoyasi;

    public CellLayout(Context context) {
        super(context);
        init();
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setClipToPadding(false);
        setClipChildren(false);
        setWillNotDraw(false);
    }

    public void setKilavuzGoster(boolean goster) {
        this.kilavuzGoster = goster;
        if (goster && kilavuzCizgiBoyasi == null) {
            kilavuzCizgiBoyasi = new Paint(Paint.ANTI_ALIAS_FLAG);
            kilavuzCizgiBoyasi.setStyle(Paint.Style.STROKE);
            kilavuzCizgiBoyasi.setStrokeWidth(2f);
            kilavuzCizgiBoyasi.setColor(0x6600E5FF); // Neon cyan
            kilavuzCizgiBoyasi.setPathEffect(new DashPathEffect(new float[]{8, 6}, 0));

            kilavuzYaziBoyasi = new Paint(Paint.ANTI_ALIAS_FLAG);
            kilavuzYaziBoyasi.setColor(0x7700E5FF);
            kilavuzYaziBoyasi.setTextSize(ArayuzYardimcisi.dpToPx(getContext(), 10));
            kilavuzYaziBoyasi.setTextAlign(Paint.Align.CENTER);
        }
        invalidate();
    }

    public boolean isKilavuzGoster() {
        return kilavuzGoster;
    }

    public void setGridBoyutu(int sutun, int satir) {
        if (sutun >= 3 && sutun <= 12) this.sutunSayisi = sutun;
        if (satir >= 3 && satir <= 12) this.satirSayisi = satir;

        // Çocukların koordinatlarını yeni sınırlarla uyuştur
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null && child.getLayoutParams() instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.spanX = Math.min(lp.spanX, this.sutunSayisi);
                lp.spanY = Math.min(lp.spanY, this.satirSayisi);
                if (lp.cellX + lp.spanX > this.sutunSayisi) {
                    lp.cellX = Math.max(0, this.sutunSayisi - lp.spanX);
                }
                if (lp.cellY + lp.spanY > this.satirSayisi) {
                    lp.cellY = Math.max(0, this.satirSayisi - lp.spanY);
                }
            }
        }
        requestLayout();
        invalidate();
    }

    public int getSutunSayisi() {
        return sutunSayisi;
    }

    public int getSatirSayisi() {
        return satirSayisi;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        int paddingW = getPaddingLeft() + getPaddingRight();
        int paddingH = getPaddingTop() + getPaddingBottom();

        int usableW = Math.max(0, width - paddingW);
        int usableH = Math.max(0, height - paddingH);

        int cellW = sutunSayisi > 0 ? usableW / sutunSayisi : usableW;
        int cellH = satirSayisi > 0 ? usableH / satirSayisi : usableH;

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int childW = lp.spanX * cellW;
                int childH = lp.spanY * cellH;

                // Yoğun ızgaralarda (8 - 12 arası) simge kapsayıcısını hücreye göre dinamik ölçekle
                // Düzenleme measure evresinde yapılır; onLayout içinde requestLayout tetiklenmesi önlenir.
                View iconBox = child.findViewById(R.id.kapsayici_simge);
                if (iconBox != null && cellW > 0 && cellH > 0 && iconBox.getLayoutParams() != null) {
                    View etiketView = child.findViewById(R.id.oge_etiket);
                    boolean etiketVar = etiketView != null && etiketView.getVisibility() != GONE;
                    int rezerveDikey = ArayuzYardimcisi.dpToPx(getContext(), etiketVar ? 32 : 14);
                    int maxAllowed = Math.min(cellW - 8, cellH - rezerveDikey);
                    if (maxAllowed > 0 && iconBox.getLayoutParams().width > maxAllowed) {
                        ViewGroup.LayoutParams iblp = iconBox.getLayoutParams();
                        iblp.width = maxAllowed;
                        iblp.height = maxAllowed;
                        iconBox.setLayoutParams(iblp);
                    }
                }

                child.measure(
                    MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY)
                );
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;

        int paddingW = getPaddingLeft() + getPaddingRight();
        int paddingH = getPaddingTop() + getPaddingBottom();

        int usableW = Math.max(0, width - paddingW);
        int usableH = Math.max(0, height - paddingH);

        int cellW = sutunSayisi > 0 ? usableW / sutunSayisi : usableW;
        int cellH = satirSayisi > 0 ? usableH / satirSayisi : usableH;

        int padLeft = getPaddingLeft();
        int padTop = getPaddingTop();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int childL = padLeft + lp.cellX * cellW;
                int childT = padTop + lp.cellY * cellH;
                int childR = childL + lp.spanX * cellW;
                int childB = childT + lp.spanY * cellH;
                child.layout(childL, childT, childR, childB);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (kilavuzGoster && kilavuzCizgiBoyasi != null) {
            int padLeft = getPaddingLeft();
            int padTop = getPaddingTop();
            int usableW = getWidth() - padLeft - getPaddingRight();
            int usableH = getHeight() - padTop - getPaddingBottom();

            if (usableW > 0 && usableH > 0 && sutunSayisi > 0 && satirSayisi > 0) {
                int cellW = usableW / sutunSayisi;
                int cellH = usableH / satirSayisi;

                // Dış sınır
                canvas.drawRect(padLeft, padTop, padLeft + usableW, padTop + usableH, kilavuzCizgiBoyasi);

                // Dikey sütun çizgileri
                for (int c = 1; c < sutunSayisi; c++) {
                    float x = padLeft + c * cellW;
                    canvas.drawLine(x, padTop, x, padTop + usableH, kilavuzCizgiBoyasi);
                }

                // Yatay satır çizgileri
                for (int r = 1; r < satirSayisi; r++) {
                    float y = padTop + r * cellH;
                    canvas.drawLine(padLeft, y, padLeft + usableW, y, kilavuzCizgiBoyasi);
                }

                // Boş hücrelerde hafif koordinat etiketleri
                if (kilavuzYaziBoyasi != null) {
                    for (int r = 0; r < satirSayisi; r++) {
                        for (int c = 0; c < sutunSayisi; c++) {
                            if (!isCellOccupied(c, r)) {
                                float cx = padLeft + c * cellW + cellW / 2.0f;
                                float cy = padTop + r * cellH + cellH / 2.0f + kilavuzYaziBoyasi.getTextSize() / 3.0f;
                                canvas.drawText((r + 1) + "×" + (c + 1), cx, cy, kilavuzYaziBoyasi);
                            }
                        }
                    }
                }
            }
        }
        super.dispatchDraw(canvas);
    }

    /**
     * Verilen piksel koordinatını (CellLayout içine göreceli) hücre indekslerine [cellX, cellY] dönüştürür.
     */
    public int[] pixelToCell(float x, float y) {
        int usableW = getWidth() - getPaddingLeft() - getPaddingRight();
        int usableH = getHeight() - getPaddingTop() - getPaddingBottom();

        if (usableW <= 0 || usableH <= 0 || sutunSayisi <= 0 || satirSayisi <= 0) {
            return new int[]{0, 0};
        }

        int cellW = usableW / sutunSayisi;
        int cellH = usableH / satirSayisi;

        float relX = x - getPaddingLeft();
        float relY = y - getPaddingTop();

        int cellX = cellW > 0 ? (int) (relX / cellW) : 0;
        int cellY = cellH > 0 ? (int) (relY / cellH) : 0;

        cellX = Math.max(0, Math.min(sutunSayisi - 1, cellX));
        cellY = Math.max(0, Math.min(satirSayisi - 1, cellY));

        return new int[]{cellX, cellY};
    }

    /**
     * Belirtilen hücrede bulunan görünümü arar ve döner. Hücre boşsa null döner.
     */
    public View findChildAtCell(int cellX, int cellY) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE && child.getLayoutParams() instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (cellX >= lp.cellX && cellX < lp.cellX + lp.spanX &&
                    cellY >= lp.cellY && cellY < lp.cellY + lp.spanY) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Piksel koordinatındaki görünümü döner. Boş alansa null döner.
     */
    public View findChildAtPixel(float x, float y) {
        int[] cell = pixelToCell(x, y);
        return findChildAtCell(cell[0], cell[1]);
    }

    /**
     * Hücrenin dolu olup olmadığını döner.
     */
    public boolean isCellOccupied(int cellX, int cellY) {
        return findChildAtCell(cellX, cellY) != null;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(0, 0);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /**
     * CellLayout için özel yerleşim parametreleri (cellX, cellY, spanX, spanY)
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int cellX;
        public int cellY;
        public int spanX = 1;
        public int spanY = 1;

        public LayoutParams(int cellX, int cellY) {
            super(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = 1;
            this.spanY = 1;
        }

        public LayoutParams(int cellX, int cellY, int spanX, int spanY) {
            super(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = Math.max(1, spanX);
            this.spanY = Math.max(1, spanY);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.cellX = 0;
            this.cellY = 0;
            this.spanX = 1;
            this.spanY = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            if (source instanceof LayoutParams) {
                LayoutParams p = (LayoutParams) source;
                this.cellX = p.cellX;
                this.cellY = p.cellY;
                this.spanX = p.spanX;
                this.spanY = p.spanY;
            } else {
                this.cellX = 0;
                this.cellY = 0;
                this.spanX = 1;
                this.spanY = 1;
            }
        }
    }
}
