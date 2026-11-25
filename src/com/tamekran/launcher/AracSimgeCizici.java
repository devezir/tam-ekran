package com.tamekran.launcher;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Tam Ekran - Dinamik Vektörel Hızlı Araç Simgesi Çizim Motoru (Canvas & Paint)
 * Sorumlulukların Ayrışması (SRP) prensibi doğrultusunda HizliAraclarYoneticisi'nden
 * ayrıştırılmış, statik renk sabitleri ile GC ve CPU dostu çizim motorudur.
 */
public final class AracSimgeCizici {

    private AracSimgeCizici() {}

    // Önceden hesaplanmış derleme zamanı ARGB renk sabitleri (Zero GC Garbage)
    private static final int RENK_PLAKA = 0xFF252A36;
    private static final int RENK_KENAR = 0xFF3D4559;
    private static final int RENK_YARI_BEYAZ = 0x33FFFFFF;
    private static final int RENK_KIRMIZI = 0xFFFF5252;
    private static final int RENK_WIFI_ACIK = 0xFF00E5FF;
    private static final int RENK_BLUETOOTH_ACIK = 0xFF2979FF;
    private static final int RENK_ALTIN = 0xFFFFD700;
    private static final int RENK_GRI = 0xFF888888;
    private static final int RENK_SES_NORMAL = 0xFF00E676;
    private static final int RENK_SES_TITRESIM = 0xFFFFB300;
    private static final int RENK_DONDURME = 0xFFAB47BC;
    private static final int RENK_PIL_DOLU = 0xFF66BB6A;
    private static final int RENK_PIL_GRI = 0xFF78909C;
    private static final int RENK_PARLAKLIK = 0xFFFFA726;
    private static final int RENK_UCAK = 0xFF42A5F5;
    private static final int RENK_ALARM = 0xFFEF5350;
    private static final int RENK_BEYAZ = 0xFFFFFFFF;
    private static final int RENK_TAKVIM_KIRMIZI = 0xFFE53935;
    private static final int RENK_KOYU_YAZI = 0xFF212121;
    private static final int RENK_HESAP = 0xFF26A69A;
    private static final int RENK_ARAMA_KAPSUL = 0xFF333333;

    private static final Map<String, Drawable> SIMGE_ONBELLEGI = new HashMap<String, Drawable>();

    public static void onbellegiTemizle() {
        synchronized (SIMGE_ONBELLEGI) {
            SIMGE_ONBELLEGI.clear();
        }
    }

    /**
     * Arka plan plakası ve kenarlığını çizen ortak DRY yardımcısı (AracSimgeCizici & KlasorSimgeUretici).
     */
    public static void plakaCiz(
            Canvas canvas,
            RectF alan,
            float koseYaricapi,
            int dolguRengi,
            int kenarRengi,
            float kenarKalinligi,
            boolean daire) {
        if (canvas == null || alan == null) return;
        Paint pPlaka = new Paint(Paint.ANTI_ALIAS_FLAG);
        pPlaka.setColor(dolguRengi);
        pPlaka.setStyle(Paint.Style.FILL);

        Paint pKenar = new Paint(Paint.ANTI_ALIAS_FLAG);
        pKenar.setColor(kenarRengi);
        pKenar.setStyle(Paint.Style.STROKE);
        pKenar.setStrokeWidth(kenarKalinligi);

        if (daire) {
            float cx = alan.centerX();
            float cy = alan.centerY();
            float r = Math.min(alan.width(), alan.height()) / 2f;
            canvas.drawCircle(cx, cy, r, pPlaka);
            canvas.drawCircle(cx, cy, r, pKenar);
        } else if (koseYaricapi <= 0f) {
            canvas.drawRect(alan, pPlaka);
            canvas.drawRect(alan, pKenar);
        } else {
            canvas.drawRoundRect(alan, koseYaricapi, koseYaricapi, pPlaka);
            canvas.drawRoundRect(alan, koseYaricapi, koseYaricapi, pKenar);
        }
    }

    public static String onbellekAnahtariOlustur(Context context, String aracId) {
        if (aracId == null) return "";
        String cacheKey = aracId;
        if (HizliAraclarYoneticisi.ARAC_FENER.equals(aracId)) {
            cacheKey += "_" + FenerYoneticisi.isFenerAcik();
        } else if (HizliAraclarYoneticisi.ARAC_SES.equals(aracId)) {
            cacheKey += "_" + getSesModu(context);
        } else if (HizliAraclarYoneticisi.ARAC_PIL.equals(aracId)) {
            cacheKey += "_" + getPilYuzdesi(context);
        } else if (HizliAraclarYoneticisi.ARAC_TAKVIM.equals(aracId)) {
            cacheKey += "_" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        } else if (HizliAraclarYoneticisi.ARAC_DONDURME.equals(aracId)) {
            cacheKey += "_" + HizliAraclarYoneticisi.isOtomatikDondurmeAcik(context);
        } else if (HizliAraclarYoneticisi.ARAC_WIFI.equals(aracId)) {
            boolean wifiAcik = false;
            try {
                if (context != null) {
                    WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (wm != null) wifiAcik = wm.isWifiEnabled();
                }
            } catch (Exception e) {
                LauncherLog.d("AracSimgeCizici", "Wi-Fi durumu okunamadi: " + e.getMessage());
            }
            cacheKey += "_" + wifiAcik;
        } else if (HizliAraclarYoneticisi.ARAC_BLUETOOTH.equals(aracId)) {
            boolean btAcik = false;
            try {
                BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
                if (ba != null) btAcik = ba.isEnabled();
            } catch (Exception e) {
                LauncherLog.d("AracSimgeCizici", "Bluetooth durumu okunamadi: " + e.getMessage());
            }
            cacheKey += "_" + btAcik;
        }
        return cacheKey;
    }

    public static Drawable ciz(Context context, String aracId) {
        if (context == null || aracId == null) return null;

        String cacheKey = onbellekAnahtariOlustur(context, aracId);

        synchronized (SIMGE_ONBELLEGI) {
            Drawable cached = SIMGE_ONBELLEGI.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        int dp = ArayuzYardimcisi.dpToPx(context, 64);
        if (dp <= 0) dp = 192;

        Bitmap bitmap = Bitmap.createBitmap(dp, dp, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 1. Squircle / Arka Plan Plakası
        RectF rPlaka = new RectF(dp * 0.04f, dp * 0.04f, dp * 0.96f, dp * 0.96f);
        plakaCiz(canvas, rPlaka, dp * 0.28f, RENK_PLAKA, RENK_KENAR, dp * 0.02f, false);

        // 2. Glif Boyası
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        if (HizliAraclarYoneticisi.ARAC_CEKMECE.equals(aracId)) {
            p.setColor(RENK_YARI_BEYAZ);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(dp * 0.50f, dp * 0.50f, dp * 0.38f, p);

            p.setColor(RENK_BEYAZ);
            p.setStyle(Paint.Style.FILL);
            float yaricap = dp * 0.055f;
            float[] noktalar = new float[]{0.32f, 0.50f, 0.68f};
            for (float nx : noktalar) {
                for (float ny : noktalar) {
                    canvas.drawCircle(dp * nx, dp * ny, yaricap, p);
                }
            }

        } else if (HizliAraclarYoneticisi.ARAC_KILIT.equals(aracId)) {
            p.setColor(RENK_KIRMIZI);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.075f);
            RectF arc = new RectF(dp * 0.25f, dp * 0.28f, dp * 0.75f, dp * 0.78f);
            canvas.drawArc(arc, 135, 270, false, p);
            canvas.drawLine(dp * 0.5f, dp * 0.18f, dp * 0.5f, dp * 0.48f, p);

        } else if (HizliAraclarYoneticisi.ARAC_WIFI.equals(aracId)) {
            boolean wifiAcik = false;
            try {
                WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) wifiAcik = wm.isWifiEnabled();
            } catch (Throwable t) {
                LauncherLog.d("AracSimgeCizici", "Wifi durumu alinamadi: " + t.getMessage());
            }

            p.setColor(wifiAcik ? RENK_WIFI_ACIK : RENK_GRI);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.065f);

            RectF r1 = new RectF(dp * 0.20f, dp * 0.25f, dp * 0.80f, dp * 0.85f);
            canvas.drawArc(r1, 225, 90, false, p);

            RectF r2 = new RectF(dp * 0.32f, dp * 0.37f, dp * 0.68f, dp * 0.73f);
            canvas.drawArc(r2, 225, 90, false, p);

            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(dp * 0.5f, dp * 0.70f, dp * 0.055f, p);

        } else if (HizliAraclarYoneticisi.ARAC_BLUETOOTH.equals(aracId)) {
            boolean btAcik = false;
            try {
                BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
                if (ba != null) btAcik = ba.isEnabled();
            } catch (Throwable t) {
                LauncherLog.d("AracSimgeCizici", "Bluetooth durumu alinamadi: " + t.getMessage());
            }

            p.setColor(btAcik ? RENK_BLUETOOTH_ACIK : RENK_GRI);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.07f);

            Path path = new Path();
            path.moveTo(dp * 0.34f, dp * 0.33f);
            path.lineTo(dp * 0.66f, dp * 0.65f);
            path.lineTo(dp * 0.50f, dp * 0.80f);
            path.lineTo(dp * 0.50f, dp * 0.20f);
            path.lineTo(dp * 0.66f, dp * 0.35f);
            path.lineTo(dp * 0.34f, dp * 0.67f);
            canvas.drawPath(path, p);

        } else if (HizliAraclarYoneticisi.ARAC_FENER.equals(aracId)) {
            boolean acik = FenerYoneticisi.isFenerAcik();
            p.setColor(acik ? RENK_ALTIN : RENK_GRI);
            p.setStyle(Paint.Style.FILL);

            RectF govde = new RectF(dp * 0.42f, dp * 0.44f, dp * 0.58f, dp * 0.78f);
            canvas.drawRoundRect(govde, dp * 0.03f, dp * 0.03f, p);

            Path baslik = new Path();
            baslik.moveTo(dp * 0.35f, dp * 0.30f);
            baslik.lineTo(dp * 0.65f, dp * 0.30f);
            baslik.lineTo(dp * 0.58f, dp * 0.44f);
            baslik.lineTo(dp * 0.42f, dp * 0.44f);
            baslik.close();
            canvas.drawPath(baslik, p);

            if (acik) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(dp * 0.05f);
                canvas.drawLine(dp * 0.5f, dp * 0.16f, dp * 0.5f, dp * 0.24f, p);
                canvas.drawLine(dp * 0.30f, dp * 0.18f, dp * 0.36f, dp * 0.24f, p);
                canvas.drawLine(dp * 0.70f, dp * 0.18f, dp * 0.64f, dp * 0.24f, p);
            }

        } else if (HizliAraclarYoneticisi.ARAC_SES.equals(aracId)) {
            int mod = getSesModu(context);
            int renk = (mod == AudioManager.RINGER_MODE_NORMAL) ? RENK_SES_NORMAL
                     : (mod == AudioManager.RINGER_MODE_VIBRATE) ? RENK_SES_TITRESIM
                     : RENK_KIRMIZI;

            p.setColor(renk);
            p.setStyle(Paint.Style.FILL);

            canvas.drawRect(dp * 0.26f, dp * 0.40f, dp * 0.38f, dp * 0.60f, p);
            Path hKoni = new Path();
            hKoni.moveTo(dp * 0.38f, dp * 0.40f);
            hKoni.lineTo(dp * 0.54f, dp * 0.28f);
            hKoni.lineTo(dp * 0.54f, dp * 0.72f);
            hKoni.lineTo(dp * 0.38f, dp * 0.60f);
            hKoni.close();
            canvas.drawPath(hKoni, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.06f);

            if (mod == AudioManager.RINGER_MODE_NORMAL) {
                RectF sArc = new RectF(dp * 0.48f, dp * 0.36f, dp * 0.68f, dp * 0.64f);
                canvas.drawArc(sArc, -45, 90, false, p);
                RectF sArc2 = new RectF(dp * 0.56f, dp * 0.26f, dp * 0.82f, dp * 0.74f);
                canvas.drawArc(sArc2, -45, 90, false, p);
            } else if (mod == AudioManager.RINGER_MODE_VIBRATE) {
                canvas.drawLine(dp * 0.66f, dp * 0.36f, dp * 0.66f, dp * 0.64f, p);
                canvas.drawLine(dp * 0.75f, dp * 0.30f, dp * 0.75f, dp * 0.70f, p);
            } else {
                canvas.drawLine(dp * 0.24f, dp * 0.74f, dp * 0.76f, dp * 0.26f, p);
            }

        } else if (HizliAraclarYoneticisi.ARAC_DONDURME.equals(aracId)) {
            boolean dondurmeAktif = HizliAraclarYoneticisi.isOtomatikDondurmeAcik(context);
            int renk = dondurmeAktif ? 0xFF00E5FF : 0xFF888888; // Açıkken parlak turkuaz, kilitliyken nötr gri

            p.setColor(renk);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.07f);

            RectF dArc = new RectF(dp * 0.22f, dp * 0.22f, dp * 0.78f, dp * 0.78f);
            canvas.drawArc(dArc, 30, 120, false, p);
            canvas.drawArc(dArc, 210, 120, false, p);

            p.setStyle(Paint.Style.FILL);
            Path ok1 = new Path();
            ok1.moveTo(dp * 0.78f, dp * 0.50f);
            ok1.lineTo(dp * 0.68f, dp * 0.38f);
            ok1.lineTo(dp * 0.88f, dp * 0.38f);
            ok1.close();
            canvas.drawPath(ok1, p);

            Path ok2 = new Path();
            ok2.moveTo(dp * 0.22f, dp * 0.50f);
            ok2.lineTo(dp * 0.12f, dp * 0.62f);
            ok2.lineTo(dp * 0.32f, dp * 0.62f);
            ok2.close();
            canvas.drawPath(ok2, p);

            if (!dondurmeAktif) {
                // Dikey Kilit: Ortada belirgin altın/kehribar kilit rozeti
                Paint pKilit = new Paint(Paint.ANTI_ALIAS_FLAG);
                pKilit.setColor(0xFFFFB300); // Amber kilit rengi

                // Kilit gövdesi (Filled rounded rect)
                RectF kilitGovde = new RectF(dp * 0.40f, dp * 0.48f, dp * 0.60f, dp * 0.66f);
                canvas.drawRoundRect(kilitGovde, dp * 0.025f, dp * 0.025f, pKilit);

                // Kilit kelepçesi (Stroke arc)
                pKilit.setStyle(Paint.Style.STROKE);
                pKilit.setStrokeWidth(dp * 0.04f);
                RectF kelepce = new RectF(dp * 0.435f, dp * 0.36f, dp * 0.565f, dp * 0.50f);
                canvas.drawArc(kelepce, 180, 180, false, pKilit);
            } else {
                // Otomatik Döndürme Açık: Ortada minik dinamik ekran dönme noktası
                Paint pNokta = new Paint(Paint.ANTI_ALIAS_FLAG);
                pNokta.setColor(0xFF00E5FF);
                pNokta.setStyle(Paint.Style.FILL);
                canvas.drawCircle(dp * 0.50f, dp * 0.50f, dp * 0.045f, pNokta);
            }

        } else if (HizliAraclarYoneticisi.ARAC_PIL.equals(aracId)) {
            int pct = getPilYuzdesi(context);
            int renk = (pct > 20) ? RENK_PIL_DOLU : RENK_KIRMIZI;

            p.setColor(RENK_PIL_GRI);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.05f);

            RectF pKutu = new RectF(dp * 0.30f, dp * 0.28f, dp * 0.70f, dp * 0.78f);
            canvas.drawRoundRect(pKutu, dp * 0.05f, dp * 0.05f, p);

            p.setStyle(Paint.Style.FILL);
            RectF pBas = new RectF(dp * 0.42f, dp * 0.20f, dp * 0.58f, dp * 0.28f);
            canvas.drawRoundRect(pBas, dp * 0.02f, dp * 0.02f, p);

            p.setColor(renk);
            float dolulukOrani = Math.max(0.05f, Math.min(1.0f, pct / 100.0f));
            float dolulukYukseklik = (pKutu.bottom - pKutu.top - dp * 0.06f) * dolulukOrani;
            RectF pDolgu = new RectF(
                pKutu.left + dp * 0.03f,
                pKutu.bottom - dp * 0.03f - dolulukYukseklik,
                pKutu.right - dp * 0.03f,
                pKutu.bottom - dp * 0.03f
            );
            canvas.drawRoundRect(pDolgu, dp * 0.03f, dp * 0.03f, p);

        } else if (HizliAraclarYoneticisi.ARAC_PARLAKLIK.equals(aracId)) {
            p.setColor(RENK_PARLAKLIK);
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(dp * 0.5f, dp * 0.5f, dp * 0.14f, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.06f);
            for (int a = 0; a < 360; a += 45) {
                double rad = Math.toRadians(a);
                float x1 = (float) (dp * 0.5f + Math.cos(rad) * dp * 0.22f);
                float y1 = (float) (dp * 0.5f + Math.sin(rad) * dp * 0.22f);
                float x2 = (float) (dp * 0.5f + Math.cos(rad) * dp * 0.32f);
                float y2 = (float) (dp * 0.5f + Math.sin(rad) * dp * 0.32f);
                canvas.drawLine(x1, y1, x2, y2, p);
            }

        } else if (HizliAraclarYoneticisi.ARAC_UCAK.equals(aracId)) {
            p.setColor(RENK_UCAK);
            p.setStyle(Paint.Style.FILL);
            Path uPath = new Path();
            uPath.moveTo(dp * 0.50f, dp * 0.18f);
            uPath.lineTo(dp * 0.56f, dp * 0.38f);
            uPath.lineTo(dp * 0.82f, dp * 0.52f);
            uPath.lineTo(dp * 0.82f, dp * 0.58f);
            uPath.lineTo(dp * 0.54f, dp * 0.52f);
            uPath.lineTo(dp * 0.54f, dp * 0.72f);
            uPath.lineTo(dp * 0.66f, dp * 0.80f);
            uPath.lineTo(dp * 0.66f, dp * 0.84f);
            uPath.lineTo(dp * 0.50f, dp * 0.78f);
            uPath.lineTo(dp * 0.34f, dp * 0.84f);
            uPath.lineTo(dp * 0.34f, dp * 0.80f);
            uPath.lineTo(dp * 0.46f, dp * 0.72f);
            uPath.lineTo(dp * 0.46f, dp * 0.52f);
            uPath.lineTo(dp * 0.18f, dp * 0.58f);
            uPath.lineTo(dp * 0.18f, dp * 0.52f);
            uPath.lineTo(dp * 0.44f, dp * 0.38f);
            uPath.close();
            canvas.drawPath(uPath, p);

        } else if (HizliAraclarYoneticisi.ARAC_ALARM.equals(aracId)) {
            p.setColor(RENK_ALARM);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.065f);
            canvas.drawCircle(dp * 0.5f, dp * 0.54f, dp * 0.24f, p);

            canvas.drawArc(new RectF(dp * 0.20f, dp * 0.22f, dp * 0.38f, dp * 0.40f), 180, 180, false, p);
            canvas.drawArc(new RectF(dp * 0.62f, dp * 0.22f, dp * 0.80f, dp * 0.40f), 180, 180, false, p);

            canvas.drawLine(dp * 0.5f, dp * 0.54f, dp * 0.5f, dp * 0.38f, p);
            canvas.drawLine(dp * 0.5f, dp * 0.54f, dp * 0.62f, dp * 0.54f, p);

        } else if (HizliAraclarYoneticisi.ARAC_TAKVIM.equals(aracId)) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(RENK_BEYAZ);
            RectF tGövde = new RectF(dp * 0.22f, dp * 0.26f, dp * 0.78f, dp * 0.80f);
            canvas.drawRoundRect(tGövde, dp * 0.05f, dp * 0.05f, p);

            p.setColor(RENK_TAKVIM_KIRMIZI);
            RectF tBant = new RectF(dp * 0.22f, dp * 0.26f, dp * 0.78f, dp * 0.42f);
            canvas.drawRoundRect(tBant, dp * 0.05f, dp * 0.05f, p);
            canvas.drawRect(dp * 0.22f, dp * 0.36f, dp * 0.78f, dp * 0.42f, p);

            int bugun = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            Paint pYazi = new Paint(Paint.ANTI_ALIAS_FLAG);
            pYazi.setColor(RENK_KOYU_YAZI);
            pYazi.setTextSize(dp * 0.28f);
            pYazi.setTextAlign(Paint.Align.CENTER);
            pYazi.setFakeBoldText(true);
            canvas.drawText(String.valueOf(bugun), dp * 0.5f, dp * 0.69f, pYazi);

        } else if (HizliAraclarYoneticisi.ARAC_HESAP_MAKINESI.equals(aracId)) {
            p.setColor(RENK_HESAP);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.06f);

            RectF calcKutu = new RectF(dp * 0.24f, dp * 0.24f, dp * 0.76f, dp * 0.76f);
            canvas.drawRoundRect(calcKutu, dp * 0.06f, dp * 0.06f, p);

            canvas.drawLine(dp * 0.5f, dp * 0.24f, dp * 0.5f, dp * 0.76f, p);
            canvas.drawLine(dp * 0.24f, dp * 0.5f, dp * 0.76f, dp * 0.5f, p);

            canvas.drawLine(dp * 0.37f, dp * 0.31f, dp * 0.37f, dp * 0.43f, p);
            canvas.drawLine(dp * 0.31f, dp * 0.37f, dp * 0.43f, dp * 0.37f, p);
            canvas.drawLine(dp * 0.57f, dp * 0.37f, dp * 0.69f, dp * 0.37f, p);
            canvas.drawLine(dp * 0.32f, dp * 0.58f, dp * 0.42f, dp * 0.68f, p);
            canvas.drawLine(dp * 0.42f, dp * 0.58f, dp * 0.32f, dp * 0.68f, p);
            canvas.drawLine(dp * 0.57f, dp * 0.60f, dp * 0.69f, dp * 0.60f, p);
            canvas.drawLine(dp * 0.57f, dp * 0.66f, dp * 0.69f, dp * 0.66f, p);

        } else if (HizliAraclarYoneticisi.ARAC_SERIT_GUC.equals(aracId)) {
            p.setColor(RENK_ALTIN);
            p.setStyle(Paint.Style.FILL);
            Path bolt = new Path();
            bolt.moveTo(dp * 0.54f, dp * 0.20f);
            bolt.lineTo(dp * 0.34f, dp * 0.52f);
            bolt.lineTo(dp * 0.48f, dp * 0.52f);
            bolt.lineTo(dp * 0.44f, dp * 0.80f);
            bolt.lineTo(dp * 0.66f, dp * 0.46f);
            bolt.lineTo(dp * 0.52f, dp * 0.46f);
            bolt.close();
            canvas.drawPath(bolt, p);

        } else if (HizliAraclarYoneticisi.ARAC_SERIT_ARAMA.equals(aracId)) {
            p.setColor(RENK_ARAMA_KAPSUL);
            p.setStyle(Paint.Style.FILL);
            RectF kapsul = new RectF(dp * 0.10f, dp * 0.32f, dp * 0.90f, dp * 0.68f);
            canvas.drawRoundRect(kapsul, dp * 0.18f, dp * 0.18f, p);

            p.setColor(RENK_BEYAZ);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp * 0.05f);
            canvas.drawCircle(dp * 0.32f, dp * 0.50f, dp * 0.08f, p);
            canvas.drawLine(dp * 0.38f, dp * 0.56f, dp * 0.44f, dp * 0.62f, p);
            p.setColor(RENK_GRI);
            p.setStrokeWidth(dp * 0.04f);
            canvas.drawLine(dp * 0.50f, dp * 0.50f, dp * 0.75f, dp * 0.50f, p);
        }

        BitmapDrawable sonuc = new BitmapDrawable(context.getResources(), bitmap);
        synchronized (SIMGE_ONBELLEGI) {
            SIMGE_ONBELLEGI.put(cacheKey, sonuc);
        }
        return sonuc;
    }

    public static int getPilYuzdesi(Context context) {
        return HizliAraclarYoneticisi.getPilYuzdesi(context);
    }

    public static int getSesModu(Context context) {
        return HizliAraclarYoneticisi.getSesModu(context);
    }
}
