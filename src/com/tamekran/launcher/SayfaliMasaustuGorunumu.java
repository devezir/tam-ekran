package com.tamekran.launcher;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * Tam Ekran - Çok Sayfalı Masaüstü Çalışma Alanı (Multi-Page Workspace)
 * Nova Launcher tarzı yatay kaydırılabilir, pürüzsüz sayfa geçişleri ve 3D efektleri.
 * Android 2.2'den Android 17'ye kadar saf Java ve sıfır harici kütüphane.
 */
public final class SayfaliMasaustuGorunumu extends ViewGroup {

    public interface SayfaDegisimDinleyicisi {
        void onSayfaDegisti(int yeniSayfa, int toplamSayfa);
    }

    public static final int EFEKT_STANDART = 0;
    public static final int EFEKT_KUP = 1;
    public static final int EFEKT_DERINLIK = 2;
    public static final int EFEKT_AKORDIYON = 3;

    public static final String[] EFEKT_ISIMLERI = new String[]{
        "Standart (Kayan)",
        "Küp (3D Dönüş)",
        "Derinlik (Yakınlaşma)",
        "Akordiyon (Katlanma)"
    };

    private Scroller kaydirici;
    private VelocityTracker hizIzleyici;
    private int aktifSayfa = 0;
    private int varsayilanSayfa = 0;
    private boolean sonsuzKaydirma = false;
    private boolean duvarKagidiKaydirma = true;
    private int gecisEfekti = EFEKT_STANDART;

    private int dokunmaToleransi;
    private int minimumFirlatmaHizi;
    private int maksimumFirlatmaHizi;

    private float sonDokunmaX;
    private float sonDokunmaY;
    private boolean surukleniyor = false;

    private SayfaDegisimDinleyicisi sayfaDinleyicisi;

    public SayfaliMasaustuGorunumu(Context context) {
        super(context);
        baslat(context);
    }

    public SayfaliMasaustuGorunumu(Context context, AttributeSet attrs) {
        super(context, attrs);
        baslat(context);
    }

    public SayfaliMasaustuGorunumu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        baslat(context);
    }

    private void baslat(Context context) {
        kaydirici = new Scroller(context);
        ViewConfiguration yapilandirma = ViewConfiguration.get(context);
        dokunmaToleransi = yapilandirma.getScaledTouchSlop();
        minimumFirlatmaHizi = yapilandirma.getScaledMinimumFlingVelocity();
        maksimumFirlatmaHizi = yapilandirma.getScaledMaximumFlingVelocity();
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setSayfaDegisimDinleyicisi(SayfaDegisimDinleyicisi dinleyici) {
        this.sayfaDinleyicisi = dinleyici;
        if (sayfaDinleyicisi != null) {
            sayfaDinleyicisi.onSayfaDegisti(aktifSayfa, getChildCount());
        }
    }

    public void setSonsuzKaydirma(boolean acik) {
        this.sonsuzKaydirma = acik;
    }

    public void setDuvarKagidiKaydirma(boolean acik) {
        this.duvarKagidiKaydirma = acik;
        duvarKagidiGuncelle();
    }

    public void setGecisEfekti(int efekt) {
        this.gecisEfekti = efekt;
        efektleriUygula();
    }

    public int getGecisEfekti() {
        return gecisEfekti;
    }

    public void setVarsayilanSayfa(int sayfa) {
        this.varsayilanSayfa = sayfa;
    }

    public int getVarsayilanSayfa() {
        return varsayilanSayfa;
    }

    public int getAktifSayfa() {
        return aktifSayfa;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int genislik = MeasureSpec.getSize(widthMeasureSpec);
        final int yukseklik = MeasureSpec.getSize(heightMeasureSpec);
        final int cocukYuksekligi = Math.max(0, yukseklik - getPaddingTop() - getPaddingBottom());

        final int cocukSayisi = getChildCount();
        for (int i = 0; i < cocukSayisi; i++) {
            getChildAt(i).measure(
                MeasureSpec.makeMeasureSpec(genislik, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(cocukYuksekligi, MeasureSpec.EXACTLY)
            );
        }

        if (aktifSayfa >= cocukSayisi && cocukSayisi > 0) {
            aktifSayfa = cocukSayisi - 1;
        }
        scrollTo(aktifSayfa * genislik, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int cocukSolu = 0;
        final int cocukSayisi = getChildCount();
        final int cocukUstu = getPaddingTop();
        for (int i = 0; i < cocukSayisi; i++) {
            final View cocuk = getChildAt(i);
            if (cocuk.getVisibility() != View.GONE) {
                final int cocukGenisligi = cocuk.getMeasuredWidth();
                final int cocukYuksekligi = cocuk.getMeasuredHeight();
                cocuk.layout(cocukSolu, cocukUstu, cocukSolu + cocukGenisligi, cocukUstu + cocukYuksekligi);
                cocukSolu += cocukGenisligi;
            }
        }
        efektleriUygula();
    }

    @Override
    public void computeScroll() {
        if (kaydirici.computeScrollOffset()) {
            scrollTo(kaydirici.getCurrX(), kaydirici.getCurrY());
            efektleriUygula();
            postInvalidate();
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        efektleriUygula();
        duvarKagidiGuncelle();
    }

    private void duvarKagidiGuncelle() {
        if (!duvarKagidiKaydirma || getWindowToken() == null) return;
        int toplamCocuk = getChildCount();
        if (toplamCocuk <= 1) return;
        int genislik = getWidth();
        if (genislik == 0) return;
        float toplamKaydirma = (toplamCocuk - 1) * genislik;
        float xOffset = Math.max(0.0f, Math.min(1.0f, (float) getScrollX() / toplamKaydirma));

        try {
            android.app.WallpaperManager wm = android.app.WallpaperManager.getInstance(getContext());
            if (wm != null) {
                wm.setWallpaperOffsets(getWindowToken(), xOffset, 0.5f);
            }
        } catch (Throwable t) {
            LauncherLog.d("SayfaliMasaustu", "Duvar kagidi kaydirma basarisiz: " + t.getMessage());
        }
    }

    /**
     * Nova Tarzı Donanımsal Hızlandırılmış 3D Sayfa Geçiş Efektleri (Android 3.0+)
     */
    private void efektleriUygula() {
        if (Build.VERSION.SDK_INT < 11) return;

        final int genislik = getWidth();
        if (genislik == 0) return;

        final int cocukSayisi = getChildCount();
        for (int i = 0; i < cocukSayisi; i++) {
            View cocuk = getChildAt(i);
            int cocukSolu = i * genislik;
            float ofset = (float) (getScrollX() - cocukSolu) / genislik;

            switch (gecisEfekti) {
                case EFEKT_KUP:
                    cocuk.setCameraDistance(genislik * 12);
                    cocuk.setPivotX(ofset < 0 ? 0 : cocuk.getWidth());
                    cocuk.setPivotY(cocuk.getHeight() / 2.0f);
                    cocuk.setRotationY(ofset * 35f);
                    cocuk.setScaleX(1.0f);
                    cocuk.setScaleY(1.0f);
                    cocuk.setAlpha(Math.max(0.3f, 1.0f - Math.abs(ofset) * 0.5f));
                    break;

                case EFEKT_DERINLIK:
                    float olcek = Math.max(0.78f, 1.0f - Math.abs(ofset) * 0.22f);
                    cocuk.setScaleX(olcek);
                    cocuk.setScaleY(olcek);
                    cocuk.setAlpha(Math.max(0.4f, 1.0f - Math.abs(ofset) * 0.55f));
                    cocuk.setRotationY(0);
                    break;

                case EFEKT_AKORDIYON:
                    cocuk.setPivotX(ofset < 0 ? 0 : cocuk.getWidth());
                    cocuk.setScaleX(Math.max(0.6f, 1.0f - Math.abs(ofset) * 0.4f));
                    cocuk.setScaleY(1.0f);
                    cocuk.setAlpha(Math.max(0.4f, 1.0f - Math.abs(ofset) * 0.5f));
                    cocuk.setRotationY(0);
                    break;

                case EFEKT_STANDART:
                default:
                    cocuk.setRotationY(0);
                    cocuk.setScaleX(1.0f);
                    cocuk.setScaleY(1.0f);
                    cocuk.setAlpha(1.0f);
                    break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int eylem = ev.getAction();
        if (eylem == MotionEvent.ACTION_MOVE && surukleniyor) {
            return true;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (eylem) {
            case MotionEvent.ACTION_DOWN:
                sonDokunmaX = x;
                sonDokunmaY = y;
                surukleniyor = !kaydirici.isFinished();
                break;

            case MotionEvent.ACTION_MOVE:
                final int farkX = (int) Math.abs(x - sonDokunmaX);
                final int farkY = (int) Math.abs(y - sonDokunmaY);

                if (farkX > dokunmaToleransi && farkX > farkY * 1.2f) {
                    surukleniyor = true;
                    sonDokunmaX = x;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                surukleniyor = false;
                break;
        }

        return surukleniyor;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (hizIzleyici == null) {
            hizIzleyici = VelocityTracker.obtain();
        }
        hizIzleyici.addMovement(ev);

        final int eylem = ev.getAction();
        final float x = ev.getX();

        switch (eylem) {
            case MotionEvent.ACTION_DOWN:
                if (!kaydirici.isFinished()) {
                    kaydirici.abortAnimation();
                }
                sonDokunmaX = x;
                break;

            case MotionEvent.ACTION_MOVE:
                if (surukleniyor) {
                    final int deltaX = (int) (sonDokunmaX - x);
                    sonDokunmaX = x;

                    final int genislik = getWidth();
                    final int maksKaydirma = (getChildCount() - 1) * genislik;

                    if (!sonsuzKaydirma) {
                        if (getScrollX() + deltaX < 0) {
                            scrollTo(0, 0);
                        } else if (getScrollX() + deltaX > maksKaydirma) {
                            scrollTo(maksKaydirma, 0);
                        } else {
                            scrollBy(deltaX, 0);
                        }
                    } else {
                        scrollBy(deltaX, 0);
                    }
                    efektleriUygula();
                } else {
                    final int farkX = (int) Math.abs(x - sonDokunmaX);
                    if (farkX > dokunmaToleransi) {
                        surukleniyor = true;
                        sonDokunmaX = x;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (surukleniyor) {
                    final VelocityTracker izleyici = hizIzleyici;
                    izleyici.computeCurrentVelocity(1000, maksimumFirlatmaHizi);
                    final int hiz = (int) izleyici.getXVelocity();

                    if (hiz > minimumFirlatmaHizi && aktifSayfa > 0) {
                        sayfayaGit(aktifSayfa - 1, true);
                    } else if (hiz < -minimumFirlatmaHizi && aktifSayfa < getChildCount() - 1) {
                        sayfayaGit(aktifSayfa + 1, true);
                    } else if (sonsuzKaydirma && hiz > minimumFirlatmaHizi && aktifSayfa == 0) {
                        sayfayaGit(getChildCount() - 1, true);
                    } else if (sonsuzKaydirma && hiz < -minimumFirlatmaHizi && aktifSayfa == getChildCount() - 1) {
                        sayfayaGit(0, true);
                    } else {
                        hedefSayfayaHizala();
                    }

                    if (hizIzleyici != null) {
                        hizIzleyici.recycle();
                        hizIzleyici = null;
                    }
                    surukleniyor = false;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (surukleniyor) {
                    hedefSayfayaHizala();
                    if (hizIzleyici != null) {
                        hizIzleyici.recycle();
                        hizIzleyici = null;
                    }
                    surukleniyor = false;
                }
                break;
        }

        return true;
    }

    private void hedefSayfayaHizala() {
        final int genislik = getWidth();
        if (genislik == 0) return;
        final int hedefSayfa = (getScrollX() + (genislik / 2)) / genislik;
        sayfayaGit(hedefSayfa, true);
    }

    public void sayfayaGit(int sayfaIndeksi, boolean animasyonlu) {
        final int cocukSayisi = getChildCount();
        if (cocukSayisi == 0) return;

        if (sayfaIndeksi < 0) {
            sayfaIndeksi = sonsuzKaydirma ? cocukSayisi - 1 : 0;
        } else if (sayfaIndeksi >= cocukSayisi) {
            sayfaIndeksi = sonsuzKaydirma ? 0 : cocukSayisi - 1;
        }

        final int hedefX = sayfaIndeksi * getWidth();
        final int farkX = hedefX - getScrollX();

        if (animasyonlu) {
            kaydirici.startScroll(getScrollX(), 0, farkX, 0, Math.min(450, Math.max(250, Math.abs(farkX) / 2)));
            invalidate();
        } else {
            kaydirici.abortAnimation();
            scrollTo(hedefX, 0);
            efektleriUygula();
        }

        aktifSayfa = sayfaIndeksi;
        if (sayfaDinleyicisi != null) {
            sayfaDinleyicisi.onSayfaDegisti(aktifSayfa, cocukSayisi);
        }
    }

    public void sonrakiSayfa() {
        sayfayaGit(aktifSayfa + 1, true);
    }

    public void oncekiSayfa() {
        sayfayaGit(aktifSayfa - 1, true);
    }
}
