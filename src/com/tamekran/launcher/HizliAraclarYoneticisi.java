package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Tam Ekran - Dahili Hızlı Kontrol Araçları (Built-in Power Widgets & Quick Toggles)
 * Sıfır harici kütüphane, saf Java & Canvas.
 * Android 2.2'den Android 17'ye kadar tam uyumlu.
 */
public class HizliAraclarYoneticisi {

    // Araç Kimlikleri
    public static final String ARAC_CEKMECE = "arac:cekmece";
    public static final String ARAC_KILIT = "arac:kilit";
    public static final String ARAC_WIFI = "arac:wifi";
    public static final String ARAC_BLUETOOTH = "arac:bluetooth";
    public static final String ARAC_FENER = "arac:fener";
    public static final String ARAC_SES = "arac:ses";
    public static final String ARAC_DONDURME = "arac:dondurme";
    public static final String ARAC_PIL = "arac:pil";
    public static final String ARAC_PARLAKLIK = "arac:parlaklik";
    public static final String ARAC_UCAK = "arac:ucak";
    public static final String ARAC_ALARM = "arac:alarm";
    public static final String ARAC_TAKVIM = "arac:takvim";
    public static final String ARAC_HESAP_MAKINESI = "arac:hesap_makinesi";
    public static final String ARAC_SERIT_GUC = "arac:serit_guc";
    public static final String ARAC_SERIT_ARAMA = "arac:serit_arama";

    public static synchronized void onbellegiTemizle() {
        AracSimgeCizici.onbellegiTemizle();
    }

    /**
     * Araç Tanımı Veri Modeli
     */
    public static class AracTanimi {
        public String id;
        public String baslik;
        public String aciklama;
        public int varsayilanSpanX;
        public int varsayilanSpanY;

        public AracTanimi(String id, String baslik, String aciklama, int spanX, int spanY) {
            this.id = id;
            this.baslik = baslik;
            this.aciklama = aciklama;
            this.varsayilanSpanX = spanX;
            this.varsayilanSpanY = spanY;
        }
    }

    /**
     * Kullanılabilir tüm dahili araçların listesini döner
     */
    public static List<AracTanimi> getTumAraclar() {
        List<AracTanimi> liste = new ArrayList<AracTanimi>();
        // 1x1 Bireysel Hızlı Kontrol Araçları
        liste.add(new AracTanimi(ARAC_CEKMECE, "Tüm Uygulamalar", "Uygulama çekmecesini açan klasik menü butonu (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_KILIT, "Ekranı Kapat", "Ekranı anında uyutur ve kilitler (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_WIFI, "Wi-Fi", "Wi-Fi hızlı kontrolü ve açma/kapatma (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_BLUETOOTH, "Bluetooth", "Bluetooth açma/kapatma kontrolü (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_FENER, "El Feneri", "Kamera LED flaşını açar / kapatır (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_SES, "Ses Profili", "Sesli, Titreşim ve Sessiz mod döngüsü (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_DONDURME, "Ekran Döndürme", "Dikey kilit ile otomatik döndürme (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_PIL, "Pil Durumu", "Canlı pil seviyesi ve güç tasarrufu (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_PARLAKLIK, "Ekran Parlaklığı", "Hızlı ekran parlaklığı ayarları (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_UCAK, "Uçak Modu", "Uçak modu hızlı geçiş ayarı (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_ALARM, "Alarm & Saat", "Yerleşik sistem alarmını açar (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_TAKVIM, "Takvim", "Sistem takvim uygulamasını açar (1×1)", 1, 1));
        liste.add(new AracTanimi(ARAC_HESAP_MAKINESI, "Hesap Makinesi", "Sistem hesap makinesini açar (1×1)", 1, 1));
        // Çoklu Güç Denetimi Şeridi
        liste.add(new AracTanimi(ARAC_SERIT_GUC, "⚡ Güç Denetimi Şeridi", "Wi-Fi, BT, Fener, Ses, Kilit tek satırda (Yatay Şerit)", 4, 1));
        // Hızlı Arama & Ayarlar Şeridi
        liste.add(new AracTanimi(ARAC_SERIT_ARAMA, "🔍 Hızlı Arama & Ayarlar", "Arama, sesli arama ve ayarlar butonu (Yatay Şerit)", 4, 1));
        return liste;
    }

    /**
     * Araç için dinamik UygulamaBilgisi üretir (Masaüstü ve Dock entegrasyonu için)
     */
    public static UygulamaBilgisi aracUygulamaBilgisiAl(Context context, String aracId) {
        if (aracId == null) return null;
        String etiket = "Araç";
        for (AracTanimi t : getTumAraclar()) {
            if (t.id.equals(aracId)) {
                etiket = t.baslik;
                break;
            }
        }

        // Canlı etiket zenginleştirmeleri
        if (ARAC_PIL.equals(aracId)) {
            int yuzde = getPilYuzdesi(context);
            etiket = "Pil: %" + yuzde;
        } else if (ARAC_SES.equals(aracId)) {
            int mod = getSesModu(context);
            if (mod == AudioManager.RINGER_MODE_VIBRATE) {
                etiket = "Titreşim";
            } else if (mod == AudioManager.RINGER_MODE_SILENT) {
                etiket = "Sessiz";
            } else {
                etiket = "Sesli";
            }
        } else if (ARAC_FENER.equals(aracId)) {
            etiket = FenerYoneticisi.isFenerAcik() ? "Fener: Açık" : "El Feneri";
        } else if (ARAC_DONDURME.equals(aracId)) {
            boolean acik = isOtomatikDondurmeAcik(context);
            etiket = acik ? "Döndürme: Açık" : "Döndürme: Kilitli";
        }

        Drawable simge = cizAracSimgesi(context, aracId);
        return new UygulamaBilgisi(etiket, aracId, "", simge);
    }

    /**
     * Aracın tıklanma eylemini yürütür
     */
    public static void araciCalistir(final Activity activity, final String aracId, final Runnable onBitti) {
        if (activity == null || aracId == null) return;

        if (ARAC_CEKMECE.equals(aracId)) {
            if (activity instanceof AnaEkranActivity) {
                ((AnaEkranActivity) activity).cekmeceyiAc();
            }
        } else if (ARAC_KILIT.equals(aracId)) {
            EkranKilitleyici.kilitle(activity);
        } else if (ARAC_WIFI.equals(aracId)) {
            wifiGecisiYap(activity);
        } else if (ARAC_BLUETOOTH.equals(aracId)) {
            bluetoothGecisiYap(activity);
        } else if (ARAC_FENER.equals(aracId)) {
            fenerGecisiYap(activity);
        } else if (ARAC_SES.equals(aracId)) {
            sesModuGecisiYap(activity);
        } else if (ARAC_DONDURME.equals(aracId)) {
            dondurmeGecisiYap(activity);
        } else if (ARAC_PIL.equals(aracId)) {
            pilAyarlariniAc(activity);
        } else if (ARAC_PARLAKLIK.equals(aracId)) {
            parlaklikAyarlariniAc(activity);
        } else if (ARAC_UCAK.equals(aracId)) {
            ucakModuAyarlariniAc(activity);
        } else if (ARAC_ALARM.equals(aracId)) {
            alarmUygulamasiniAc(activity);
        } else if (ARAC_TAKVIM.equals(aracId)) {
            takvimUygulamasiniAc(activity);
        } else if (ARAC_HESAP_MAKINESI.equals(aracId)) {
            hesapMakinesiAc(activity);
        }

        if (onBitti != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onBitti.run();
                }
            });
        }
    }

    /**
     * Masaüstü uzun basışında açılan "Tam Ekran Araçları" seçim penceresi
     */
    public static void aracSecimDiyaloguGoster(final Activity activity, final int sayfa, final Runnable onEklendi) {
        if (activity == null || activity.isFinishing()) return;

        final List<AracTanimi> araclar = getTumAraclar();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("⚡ Tam Ekran Araçları (Widget & Kontroller)");

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return araclar.size();
            }

            @Override
            public Object getItem(int position) {
                return araclar.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout layout;
                if (convertView instanceof LinearLayout) {
                    layout = (LinearLayout) convertView;
                } else {
                    layout = new LinearLayout(activity);
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setGravity(Gravity.CENTER_VERTICAL);
                    int dp10 = ArayuzYardimcisi.dpToPx(activity, 10);
                    layout.setPadding(dp10, dp10, dp10, dp10);

                    ImageView simge = new ImageView(activity);
                    simge.setId(1001);
                    int dp40 = ArayuzYardimcisi.dpToPx(activity, 40);
                    LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dp40, dp40);
                    lpImg.setMargins(0, 0, dp10, 0);
                    layout.addView(simge, lpImg);

                    LinearLayout metinKutusu = new LinearLayout(activity);
                    metinKutusu.setOrientation(LinearLayout.VERTICAL);

                    TextView txtBaslik = new TextView(activity);
                    txtBaslik.setId(1002);
                    txtBaslik.setTextSize(15);
                    txtBaslik.setTextColor(Color.WHITE);
                    txtBaslik.getPaint().setFakeBoldText(true);
                    metinKutusu.addView(txtBaslik);

                    TextView txtAciklama = new TextView(activity);
                    txtAciklama.setId(1003);
                    txtAciklama.setTextSize(12);
                    txtAciklama.setTextColor(Color.parseColor("#AAAAAA"));
                    metinKutusu.addView(txtAciklama);

                    layout.addView(metinKutusu);
                }

                AracTanimi tanim = araclar.get(position);
                ImageView img = (ImageView) layout.findViewById(1001);
                TextView baslik = (TextView) layout.findViewById(1002);
                TextView aciklama = (TextView) layout.findViewById(1003);

                if (img != null) img.setImageDrawable(cizAracSimgesi(activity, tanim.id));
                if (baslik != null) baslik.setText(tanim.baslik);
                if (aciklama != null) aciklama.setText(tanim.aciklama);

                return layout;
            }
        };

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AracTanimi secilen = araclar.get(which);
                int spanX = secilen.varsayilanSpanX;
                int spanY = secilen.varsayilanSpanY;

                if (ARAC_SERIT_GUC.equals(secilen.id)) {
                    int sutun = AyarlarActivity.getMasaustuSutunSayisi(activity);
                    spanX = Math.min(sutun, 5);
                } else if (ARAC_SERIT_ARAMA.equals(secilen.id)) {
                    int sutun = AyarlarActivity.getMasaustuSutunSayisi(activity);
                    spanX = sutun;
                }

                boolean eklendi = MasaustuYoneticisi.sayfayaOgeEkle(activity, sayfa, secilen.id, spanX, spanY);
                if (eklendi) {
                    ArayuzYardimcisi.toast(activity, secilen.baslik + " masaüstüne eklendi.");
                    if (onEklendi != null) onEklendi.run();
                } else {
                    ArayuzYardimcisi.toastUzun(activity, "Bu sayfada yeterli boş hücre bulunamadı.");
                }
            }
        });

        builder.setNegativeButton(R.string.iptal, null);
        builder.show();
    }

    /**
     * Çoklu Güç Denetimi Şeridi Görünümü Üretir (Sürükle-Bırak ve Çöp Kutusu Destekli)
     */
    public static View seritGorunumuOlustur(final Activity activity, final int sayfaNo, final MasaustuYoneticisi.MasaustuOgesi oge, final View.OnLongClickListener longClickListener, final Runnable onGuncelle) {
        if (activity == null) return new View(activity);

        final LinearLayout serit = new LinearLayout(activity);
        serit.setOrientation(LinearLayout.HORIZONTAL);
        serit.setBackgroundResource(R.drawable.kart_arkaplan);
        serit.setGravity(Gravity.CENTER);

        int pDikey = ArayuzYardimcisi.dpToPx(activity, 6);
        int pYatay = ArayuzYardimcisi.dpToPx(activity, 8);
        serit.setPadding(pYatay, pDikey, pYatay, pDikey);

        final String[] butonIdleri = new String[]{ARAC_WIFI, ARAC_BLUETOOTH, ARAC_FENER, ARAC_SES, ARAC_KILIT};
        final String[] butonEtiketleri = new String[]{"Wi-Fi", "BT", "Fener", "Ses", "Kilit"};

        for (int i = 0; i < butonIdleri.length; i++) {
            final String bId = butonIdleri[i];
            final String bEtiket = butonEtiketleri[i];

            final LinearLayout btnLayout = new LinearLayout(activity);
            btnLayout.setOrientation(LinearLayout.VERTICAL);
            btnLayout.setGravity(Gravity.CENTER);
            btnLayout.setClickable(true);
            btnLayout.setFocusable(true);

            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            btnLayout.setLayoutParams(bLp);

            final ImageView icon = new ImageView(activity);
            int iconSize = ArayuzYardimcisi.dpToPx(activity, 26);
            LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(iconSize, iconSize);
            icon.setLayoutParams(iLp);
            icon.setImageDrawable(cizAracSimgesi(activity, bId));
            btnLayout.addView(icon);

            TextView txt = new TextView(activity);
            txt.setText(bEtiket);
            txt.setTextSize(10);
            txt.setTextColor(Color.parseColor("#CCCCCC"));
            txt.setGravity(Gravity.CENTER);
            btnLayout.addView(txt);

            btnLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    araciCalistir(activity, bId, new Runnable() {
                        @Override
                        public void run() {
                            icon.setImageDrawable(cizAracSimgesi(activity, bId));
                            if (onGuncelle != null) onGuncelle.run();
                        }
                    });
                }
            });

            // Buton üzerine uzun basıldığında tüm şerit için sürüklemeyi başlat
            btnLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (longClickListener != null) {
                        return longClickListener.onLongClick(serit);
                    }
                    return false;
                }
            });

            serit.addView(btnLayout);
        }

        // Şeridin kendisine uzun basıldığında da sürüklemeyi başlat
        serit.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longClickListener != null) {
                    return longClickListener.onLongClick(serit);
                }
                return false;
            }
        });

        return serit;
    }

    public static View seritGorunumuOlustur(final Activity activity, final MasaustuYoneticisi.MasaustuOgesi oge, final Runnable onGuncelle) {
        return seritGorunumuOlustur(activity, 0, oge, null, onGuncelle);
    }

    /**
     * Hızlı Arama & Ayarlar Şeridi Görünümü Üretir (Sürükle-Bırak, Arama, Ses ve Ayarlar Destekli)
     */
    public static View aramaSeritGorunumuOlustur(final Activity activity, final int sayfaNo, final MasaustuYoneticisi.MasaustuOgesi oge, final View.OnLongClickListener longClickListener, final Runnable onAramaTiklandi, final Runnable onSesliAramaTiklandi) {
        if (activity == null) return new View(activity);

        final FrameLayout wrapper = new FrameLayout(activity);
        wrapper.setClickable(true);
        wrapper.setFocusable(true);

        final LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundResource(R.drawable.arama_arkaplan);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setClickable(true);
        bar.setFocusable(true);

        int pSol = ArayuzYardimcisi.dpToPx(activity, 14);
        int pSag = ArayuzYardimcisi.dpToPx(activity, 6);
        int pDikey = ArayuzYardimcisi.dpToPx(activity, 2);
        bar.setPadding(pSol, pDikey, pSag, pDikey);

        // Arama Metni Alanı (Kutunun kalanı)
        TextView txtArama = new TextView(activity);
        LinearLayout.LayoutParams lpTxt = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        txtArama.setLayoutParams(lpTxt);
        txtArama.setText("🔍 Uygulama veya web'de ara...");
        txtArama.setTextColor(Color.parseColor("#BBBBBB"));
        txtArama.setTextSize(13);
        txtArama.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(txtArama);

        // Sesli Arama Butonu (🎙️)
        TextView btnSes = new TextView(activity);
        int btnBoyut = ArayuzYardimcisi.dpToPx(activity, 34);
        LinearLayout.LayoutParams lpSes = new LinearLayout.LayoutParams(btnBoyut, btnBoyut);
        btnSes.setLayoutParams(lpSes);
        btnSes.setText("🎙️");
        btnSes.setTextSize(15);
        btnSes.setGravity(Gravity.CENTER);
        btnSes.setClickable(true);
        btnSes.setFocusable(true);
        bar.addView(btnSes);

        // Ayarlar Butonu (⚙️)
        TextView btnAyar = new TextView(activity);
        LinearLayout.LayoutParams lpAyar = new LinearLayout.LayoutParams(btnBoyut, btnBoyut);
        btnAyar.setLayoutParams(lpAyar);
        btnAyar.setText("⚙️");
        btnAyar.setTextSize(15);
        btnAyar.setGravity(Gravity.CENTER);
        btnAyar.setClickable(true);
        btnAyar.setFocusable(true);
        bar.addView(btnAyar);

        // Bar'ı wrapper içinde dikey ortala ve 40dp yükseklik ver
        int barYukseklik = ArayuzYardimcisi.dpToPx(activity, 40);
        FrameLayout.LayoutParams lpBar = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            barYukseklik,
            Gravity.CENTER
        );
        int mX = ArayuzYardimcisi.dpToPx(activity, 4);
        lpBar.setMargins(mX, 0, mX, 0);
        wrapper.addView(bar, lpBar);

        // Tıklama Olayları
        View.OnClickListener aramaTiklaDinleyici = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onAramaTiklandi != null) {
                    onAramaTiklandi.run();
                }
            }
        };
        bar.setOnClickListener(aramaTiklaDinleyici);
        txtArama.setOnClickListener(aramaTiklaDinleyici);

        btnSes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onSesliAramaTiklandi != null) {
                    onSesliAramaTiklandi.run();
                }
            }
        });

        btnAyar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startActivity(new Intent(activity, AyarlarActivity.class));
            }
        });

        // Uzun basış olayları (Sürükleme ve Çöp Kutusu Desteği)
        View.OnLongClickListener lc = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (longClickListener != null) {
                    return longClickListener.onLongClick(wrapper);
                }
                return false;
            }
        };

        wrapper.setOnLongClickListener(lc);
        bar.setOnLongClickListener(lc);
        txtArama.setOnLongClickListener(lc);
        btnSes.setOnLongClickListener(lc);
        btnAyar.setOnLongClickListener(lc);

        return wrapper;
    }

    // =========================================================================
    // ARAÇ EYLEMLERİ (Wifi, Bluetooth, Fener, Ses, vb.)
    // =========================================================================

    private static void wifiGecisiYap(Activity activity) {
        Context appCtx = activity.getApplicationContext();
        WifiManager wm = (WifiManager) appCtx.getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= 29) { // Android 10+
            try {
                Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(panelIntent);
                return;
            } catch (Throwable t) {
                try {
                    Intent settingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(settingsIntent);
                    return;
                } catch (Throwable t2) {
                    LauncherLog.d("HizliAraclar", "Wi-Fi ayarlari acilamadi: " + t2.getMessage());
                }
            }
        } else {
            if (wm != null) {
                try {
                    boolean enabled = wm.isWifiEnabled();
                    wm.setWifiEnabled(!enabled);
                    ArayuzYardimcisi.toast(activity, enabled ? "Wi-Fi kapatılıyor..." : "Wi-Fi açılıyor...");
                    return;
                } catch (Throwable t) {
                    LauncherLog.d("HizliAraclar", "Wi-Fi durumu degistirilemedi: " + t.getMessage());
                }
            }
        }
    }

    private static void bluetoothGecisiYap(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= 31) { // Android 12+ (API 31+)
                if (activity.checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
                    try {
                        activity.requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"}, 7002);
                        return;
                    } catch (Throwable t) {
                        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                        return;
                    }
                }
            }

            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba == null) {
                ArayuzYardimcisi.toast(activity, "Cihazda Bluetooth donanımı bulunamadı.");
                return;
            }
            if (ba.isEnabled()) {
                ba.disable();
                ArayuzYardimcisi.toast(activity, "Bluetooth kapatılıyor...");
            } else {
                ba.enable();
                ArayuzYardimcisi.toast(activity, "Bluetooth açılıyor...");
            }
        } catch (Throwable t) {
            try {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } catch (Throwable t2) {
                LauncherLog.d("HizliAraclar", "Bluetooth ayarlari acilamadi: " + t2.getMessage());
            }
        }
    }

    public static boolean isTorchOn() {
        return FenerYoneticisi.isFenerAcik();
    }

    private static void fenerGecisiYap(Activity activity) {
        FenerYoneticisi.toggle(activity);
    }

    private static void sesModuGecisiYap(Activity activity) {
        SesModuYoneticisi.sonrakiModaGec(activity);
    }

    public static boolean isOtomatikDondurmeAcik(Context context) {
        if (context == null) return true;
        try {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1) == 1;
        } catch (Throwable t) {
            return true;
        }
    }

    private static void dondurmeGecisiYap(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(activity)) {
                ArayuzYardimcisi.toast(activity, "Sistem ayarlarını değiştirme izni gereklidir.");
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                return;
            }
            int cur = Settings.System.getInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
            int yeni = (cur == 1) ? 0 : 1;
            Settings.System.putInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, yeni);
            ArayuzYardimcisi.toast(activity, yeni == 1 ? "🔄 Otomatik Döndürme: Açık" : "🔒 Dikey Kilit: Açık");
        } catch (Throwable t) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
            } catch (Throwable t2) {
                LauncherLog.d("HizliAraclar", "Ekran ayarlari acilamadi: " + t2.getMessage());
            }
        }
    }

    private static void pilAyarlariniAc(Activity activity) {
        SistemAraclariYardimcisi.pilAyarlariniAc(activity);
    }

    private static void parlaklikAyarlariniAc(Activity activity) {
        SistemAraclariYardimcisi.parlaklikAyarlariniAc(activity);
    }

    private static void ucakModuAyarlariniAc(Activity activity) {
        SistemAraclariYardimcisi.ucakModuAyarlariniAc(activity);
    }

    private static void alarmUygulamasiniAc(Activity activity) {
        SistemAraclariYardimcisi.saatUygulamasiniAc(activity);
    }

    private static void takvimUygulamasiniAc(Activity activity) {
        SistemAraclariYardimcisi.takvimUygulamasiniAc(activity);
    }

    private static void hesapMakinesiAc(Activity activity) {
        SistemAraclariYardimcisi.hesapMakinesiAc(activity);
    }

    public static int getPilYuzdesi(Context context) {
        try {
            Intent bat = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bat != null) {
                int level = bat.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = bat.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    return (int) ((level / (float) scale) * 100);
                }
            }
        } catch (Throwable t) {
            LauncherLog.d("HizliAraclar", "Pil yuzdesi alinamadi: " + t.getMessage());
        }
        return 100;
    }

    public static int getSesModu(Context context) {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) return am.getRingerMode();
        } catch (Throwable t) {
            LauncherLog.d("HizliAraclar", "Ses modu alinamadi: " + t.getMessage());
        }
        return AudioManager.RINGER_MODE_NORMAL;
    }

    // =========================================================================
    // DİNAMİK VEKTÖREL SİMGE ÇİZİM MOTORU (Canvas & Paint)
    // =========================================================================

    public static Drawable cizAracSimgesi(Context context, String aracId) {
        return AracSimgeCizici.ciz(context, aracId);
    }
}
