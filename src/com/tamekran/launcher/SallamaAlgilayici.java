package com.tamekran.launcher;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Tam Ekran - Cihazı Sallama Hareketi Algılayıcısı (Shake Gesture Engine)
 * İvmeölçer (Accelerometer) sensörü ile telefonun fiziksel olarak sallanmasını
 * pil tüketmeden tespit eder.
 */
public class SallamaAlgilayici implements SensorEventListener {

    public interface SallamaDinleyici {
        void onSallandi();
    }

    public static final String PREF_SALLAMA_EYLEMI = "hareket_sallama_eylemi";
    public static final int EYLEM_KAPALI = 0;
    public static final int EYLEM_KILITLE = 1;
    public static final int EYLEM_ARAMA = 2;
    public static final int EYLEM_CEKMECE = 3;
    public static final int EYLEM_BILDIRIMLER = 4;
    public static final int EYLEM_FENER = 5;
    public static final int EYLEM_SES_MODU = 6;

    public static final String[] SALLAMA_EYLEM_METINLERI = new String[]{
        "Kapalı (Devre Dışı)",
        "🔒 Ekranı Kilitle",
        "🔍 Hızlı Aramayı Aç",
        "📱 Tüm Uygulamalar Menüsünü Aç",
        "🔔 Bildirim Panelini İndir",
        "🔦 Feneri Aç / Kapat",
        "🔊 Ses Modunu Değiştir (Sessiz/Titreşim/Normal)"
    };

    private static final float SALLAMA_ESIK_DEGERI = 2.7f; // g-force eşiği
    private static final int SALLAMA_ZAMAN_ARALIGI_MS = 1500; // 1.5 saniye ara

    private Context context;
    private SensorManager sensorManager;
    private Sensor ivmeolcer;
    private SallamaDinleyici dinleyici;
    private long sonSallamaZamani = 0;
    private boolean calisiyor = false;

    public SallamaAlgilayici(Context context, SallamaDinleyici dinleyici) {
        this.context = context;
        this.dinleyici = dinleyici;
        if (context != null) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                ivmeolcer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }
        }
    }

    public void baslat() {
        if (context == null || sensorManager == null || ivmeolcer == null) return;

        android.content.SharedPreferences ayarlar = AyarBankasi.getAyarlar(context);
        int eylem = ayarlar.getInt(PREF_SALLAMA_EYLEMI, EYLEM_KAPALI);

        // Ayar "Kapalı" ise ivmeölçeri kesinlikle dinlemeye alma (Sıfır pil tüketimi)
        if (eylem == EYLEM_KAPALI) {
            durdur();
            return;
        }

        if (!calisiyor) {
            sensorManager.registerListener(this, ivmeolcer, SensorManager.SENSOR_DELAY_UI);
            calisiyor = true;
        }
    }

    public void durdur() {
        if (calisiyor && sensorManager != null) {
            sensorManager.unregisterListener(this);
            calisiyor = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;

        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce > SALLAMA_ESIK_DEGERI) {
            long simdi = System.currentTimeMillis();
            if (sonSallamaZamani + SALLAMA_ZAMAN_ARALIGI_MS < simdi) {
                sonSallamaZamani = simdi;
                if (dinleyici != null) {
                    dinleyici.onSallandi();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}

