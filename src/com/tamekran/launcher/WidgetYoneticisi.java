package com.tamekran.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tam Ekran - Ekran Aracı (Widget) Entegrasyon Motoru
 * Nova Launcher benzeri yerel uygulama içi ekran aracı seçici, bağlama ve yönetim motoru.
 * Xiaomi MIUI/HyperOS ve modern Android 11+ cihazlarda çökme yapmaz, saf Java AppWidgetHost desteği sunar.
 */
public class WidgetYoneticisi {

    public static final int APPWIDGET_HOST_ID = 2026;
    public static final int REQUEST_CREATE_WIDGET = 3002;
    public static final int REQUEST_BIND_WIDGET = 3003;

    private static final String PREF_WIDGETLAR_JSON = "masaustu_widgetlar_json";
    private static final String STATE_BEKLEYEN_SAYFA = "widget_bekleyen_sayfa";
    private static final String STATE_BEKLEYEN_ID = "widget_bekleyen_id";
    private static final String STATE_BEKLEYEN_PROVIDER = "widget_bekleyen_provider";

    public interface WidgetIslemDinleyici {
        void onWidgetlerGuncellendi();
    }

    public static class WidgetKaydi {
        public int sayfa;
        public int widgetId;

        public WidgetKaydi(int sayfa, int widgetId) {
            this.sayfa = sayfa;
            this.widgetId = widgetId;
        }
    }

    public static class EkranAraciOgesi {
        public AppWidgetProviderInfo info;
        public String uygulamaAdi;
        public String aracAdi;
        public Drawable simge;
        public String boyutMetni;
    }

    private Context context;
    private AppWidgetManager manager;
    private AppWidgetHost host;
    private int bekleyenSayfa = 0;
    private int bekleyenWidgetId = -1;
    private AppWidgetProviderInfo bekleyenProviderInfo = null;
    private WidgetIslemDinleyici bekleyenDinleyici = null;

    public WidgetYoneticisi(Context context) {
        this.context = context;
        this.manager = AppWidgetManager.getInstance(context);
        this.host = new AppWidgetHost(context, APPWIDGET_HOST_ID);
    }

    public void kaydetDurum(Bundle outState) {
        if (outState == null) return;
        outState.putInt(STATE_BEKLEYEN_SAYFA, bekleyenSayfa);
        outState.putInt(STATE_BEKLEYEN_ID, bekleyenWidgetId);
        if (bekleyenProviderInfo != null && bekleyenProviderInfo.provider != null) {
            outState.putString(STATE_BEKLEYEN_PROVIDER, bekleyenProviderInfo.provider.flattenToString());
        }
    }

    public void yukleDurum(Bundle savedState) {
        if (savedState == null) return;
        bekleyenSayfa = savedState.getInt(STATE_BEKLEYEN_SAYFA, 0);
        bekleyenWidgetId = savedState.getInt(STATE_BEKLEYEN_ID, -1);
        String providerStr = savedState.getString(STATE_BEKLEYEN_PROVIDER);
        if (providerStr != null && providerStr.length() > 0 && manager != null) {
            try {
                ComponentName cn = ComponentName.unflattenFromString(providerStr);
                if (cn != null) {
                    List<AppWidgetProviderInfo> providers = manager.getInstalledProviders();
                    if (providers != null) {
                        for (AppWidgetProviderInfo info : providers) {
                            if (info != null && cn.equals(info.provider)) {
                                bekleyenProviderInfo = info;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LauncherLog.d("WidgetYoneticisi", "Widget saglayici durumu yuklenemedi: " + e.getMessage());
            }
        }
    }

    public void startListening() {
        try {
            if (host != null) host.startListening();
        } catch (Throwable t) {
            LauncherLog.w("WidgetYoneticisi", "startListening hatası", t);
        }
    }

    public void stopListening() {
        try {
            if (host != null) host.stopListening();
        } catch (Throwable t) {
            LauncherLog.w("WidgetYoneticisi", "stopListening hatası", t);
        }
    }

    public void temizle() {
        try {
            if (host != null) {
                host.stopListening();
            }
        } catch (Throwable t) {
            LauncherLog.w("WidgetYoneticisi", "temizle hatası", t);
        }
    }

    public void widgetSeciminiBaslat(Activity activity, int sayfa) {
        widgetSeciminiBaslat(activity, sayfa, null);
    }

    public void widgetSeciminiBaslat(final Activity activity, final int sayfa, final WidgetIslemDinleyici dinleyici) {
        if (activity == null || host == null || manager == null) return;
        this.bekleyenSayfa = sayfa;
        this.bekleyenDinleyici = dinleyici;

        List<AppWidgetProviderInfo> providers = null;
        try {
            providers = manager.getInstalledProviders();
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(activity, "Ekran araçları listelenemedi: " + t.getMessage());
            return;
        }

        if (providers == null || providers.isEmpty()) {
            ArayuzYardimcisi.toast(activity, "Cihazınızda kullanılabilir ekran aracı bulunamadı.");
            return;
        }

        final PackageManager pm = activity.getPackageManager();
        final List<EkranAraciOgesi> liste = new ArrayList<EkranAraciOgesi>();

        for (AppWidgetProviderInfo info : providers) {
            if (info == null || info.provider == null) continue;
            EkranAraciOgesi oge = new EkranAraciOgesi();
            oge.info = info;

            String appName = info.provider.getPackageName();
            Drawable appIcon = null;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(info.provider.getPackageName(), 0);
                appName = pm.getApplicationLabel(ai).toString();
                appIcon = pm.getApplicationIcon(ai);
            } catch (Throwable t) {
                LauncherLog.d("WidgetYoneticisi", "Uygulama simgesi alinamadi: " + t.getMessage());
            }
            if (appIcon == null) {
                try {
                    appIcon = pm.getDefaultActivityIcon();
                } catch (Throwable t) {
                    LauncherLog.d("WidgetYoneticisi", "Varsayilan simge alinamadi: " + t.getMessage());
                }
            }

            oge.uygulamaAdi = appName;
            oge.simge = appIcon;

            String label = null;
            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    label = info.loadLabel(pm);
                } catch (Throwable t) {
                    LauncherLog.d("WidgetYoneticisi", "Widget etiketi yuklenemedi: " + t.getMessage());
                }
            }
            if (label == null || label.trim().isEmpty()) {
                label = info.label;
            }
            if (label == null || label.trim().isEmpty()) {
                label = appName;
            }
            oge.aracAdi = label;

            int widthDp = ArayuzYardimcisi.pxToDp(activity, info.minWidth);
            int heightDp = ArayuzYardimcisi.pxToDp(activity, info.minHeight);
            int cellsW = Math.max(1, (widthDp + 30) / 70);
            int cellsH = Math.max(1, (heightDp + 30) / 70);
            oge.boyutMetni = cellsW + "x" + cellsH;

            liste.add(oge);
        }

        Collections.sort(liste, new Comparator<EkranAraciOgesi>() {
            @Override
            public int compare(EkranAraciOgesi o1, EkranAraciOgesi o2) {
                int c = o1.uygulamaAdi.compareToIgnoreCase(o2.uygulamaAdi);
                if (c != 0) return c;
                return o1.aracAdi.compareToIgnoreCase(o2.aracAdi);
            }
        });

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return liste.size();
            }

            @Override
            public Object getItem(int position) {
                return liste.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout layout;
                ImageView simgeView;
                TextView baslikView;
                TextView altBaslikView;

                if (convertView == null) {
                    layout = new LinearLayout(activity);
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setGravity(Gravity.CENTER_VERTICAL);
                    int p12 = ArayuzYardimcisi.dpToPx(activity, 12);
                    int p8 = ArayuzYardimcisi.dpToPx(activity, 8);
                    layout.setPadding(p12, p8, p12, p8);

                    simgeView = new ImageView(activity);
                    int s40 = ArayuzYardimcisi.dpToPx(activity, 40);
                    LinearLayout.LayoutParams lpSimge = new LinearLayout.LayoutParams(s40, s40);
                    lpSimge.rightMargin = p12;
                    simgeView.setLayoutParams(lpSimge);
                    simgeView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    layout.addView(simgeView);

                    LinearLayout metinKutusu = new LinearLayout(activity);
                    metinKutusu.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams lpMetin = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
                    );
                    metinKutusu.setLayoutParams(lpMetin);

                    baslikView = new TextView(activity);
                    baslikView.setTextSize(15);
                    baslikView.setTypeface(null, Typeface.BOLD);
                    metinKutusu.addView(baslikView);

                    altBaslikView = new TextView(activity);
                    altBaslikView.setTextSize(12);
                    altBaslikView.setTextColor(0xFF888888);
                    metinKutusu.addView(altBaslikView);

                    layout.addView(metinKutusu);

                    layout.setTag(new View[]{simgeView, baslikView, altBaslikView});
                } else {
                    layout = (LinearLayout) convertView;
                    View[] tag = (View[]) layout.getTag();
                    simgeView = (ImageView) tag[0];
                    baslikView = (TextView) tag[1];
                    altBaslikView = (TextView) tag[2];
                }

                EkranAraciOgesi item = liste.get(position);
                simgeView.setImageDrawable(item.simge);
                baslikView.setText(item.aracAdi + " (" + item.boyutMetni + ")");
                altBaslikView.setText(item.uygulamaAdi);

                return layout;
            }
        };

        new AlertDialog.Builder(activity)
            .setTitle("Ekran Aracı Seçin (" + liste.size() + ")")
            .setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EkranAraciOgesi secilen = liste.get(which);
                    ekranAraciSecildi(activity, secilen.info, sayfa, dinleyici);
                }
            })
            .setNegativeButton(R.string.iptal, null)
            .show();
    }

    private void ekranAraciSecildi(final Activity activity, final AppWidgetProviderInfo info, final int sayfa, final WidgetIslemDinleyici dinleyici) {
        if (activity == null || info == null || host == null || manager == null) return;
        this.bekleyenSayfa = sayfa;
        this.bekleyenDinleyici = dinleyici;
        this.bekleyenProviderInfo = info;

        int widgetId = -1;
        try {
            widgetId = host.allocateAppWidgetId();
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(activity, "Ekran aracı kimliği alınamadı: " + t.getMessage());
            return;
        }
        this.bekleyenWidgetId = widgetId;

        boolean baglandi = false;
        if (Build.VERSION.SDK_INT >= 16) {
            try {
                baglandi = manager.bindAppWidgetIdIfAllowed(widgetId, info.provider);
            } catch (Throwable t) {
                LauncherLog.w("WidgetYoneticisi", "bindAppWidgetIdIfAllowed hatası", t);
                baglandi = false;
            }
        }

        if (baglandi) {
            yapilandirmayiTamamla(activity, widgetId, info, dinleyici);
        } else {
            if (Build.VERSION.SDK_INT >= 16) {
                Intent bindIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                bindIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);
                try {
                    activity.startActivityForResult(bindIntent, REQUEST_BIND_WIDGET);
                } catch (Throwable t) {
                    yapilandirmayiTamamla(activity, widgetId, info, dinleyici);
                }
            } else {
                yapilandirmayiTamamla(activity, widgetId, info, dinleyici);
            }
        }
    }

    private void yapilandirmayiTamamla(final Activity activity, final int widgetId, final AppWidgetProviderInfo info, final WidgetIslemDinleyici dinleyici) {
        if (info != null && info.configure != null) {
            Intent niyet = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            niyet.setComponent(info.configure);
            niyet.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            try {
                activity.startActivityForResult(niyet, REQUEST_CREATE_WIDGET);
            } catch (Throwable t) {
                // Yapılandırma başlatılamazsa doğrudan ekle
                widgetKaydet(context, bekleyenSayfa, widgetId);
                if (dinleyici != null) dinleyici.onWidgetlerGuncellendi();
                ArayuzYardimcisi.toast(activity, "Ekran aracı masaüstüne eklendi.");
                sifirla();
            }
        } else {
            widgetKaydet(context, bekleyenSayfa, widgetId);
            if (dinleyici != null) dinleyici.onWidgetlerGuncellendi();
            ArayuzYardimcisi.toast(activity, "Ekran aracı masaüstüne eklendi.");
            sifirla();
        }
    }

    private void sifirla() {
        bekleyenWidgetId = -1;
        bekleyenProviderInfo = null;
        bekleyenDinleyici = null;
    }

    public void onActivityResult(final Activity activity, int requestCode, int resultCode, Intent data, final WidgetIslemDinleyici dinleyici) {
        final WidgetIslemDinleyici etkiliDinleyici = (dinleyici != null) ? dinleyici : bekleyenDinleyici;

        if (requestCode == REQUEST_BIND_WIDGET) {
            if (resultCode == Activity.RESULT_OK) {
                if (bekleyenWidgetId != -1 && bekleyenProviderInfo != null) {
                    yapilandirmayiTamamla(activity, bekleyenWidgetId, bekleyenProviderInfo, etkiliDinleyici);
                }
            } else {
                if (bekleyenWidgetId != -1 && host != null) {
                    try { host.deleteAppWidgetId(bekleyenWidgetId); } catch (Throwable t) { LauncherLog.d("WidgetYoneticisi", "Widget silinemedi"); }
                }
                sifirla();
            }
        } else if (requestCode == REQUEST_CREATE_WIDGET) {
            int widgetId = (data != null) ? data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, bekleyenWidgetId) : bekleyenWidgetId;
            if (resultCode == Activity.RESULT_OK) {
                widgetKaydet(context, bekleyenSayfa, widgetId);
                if (etkiliDinleyici != null) etkiliDinleyici.onWidgetlerGuncellendi();
                ArayuzYardimcisi.toast(activity, "Ekran aracı masaüstüne eklendi.");
            } else {
                if (widgetId != -1 && host != null) {
                    try { host.deleteAppWidgetId(widgetId); } catch (Throwable t) { LauncherLog.d("WidgetYoneticisi", "Widget silinemedi"); }
                }
            }
            sifirla();
        }
    }

    public List<WidgetKaydi> getSayfaWidgetleri(int sayfa) {
        List<WidgetKaydi> tumu = tumWidgetleriYukle(context);
        List<WidgetKaydi> sayfaListesi = new ArrayList<WidgetKaydi>();
        for (WidgetKaydi w : tumu) {
            if (w.sayfa == sayfa) {
                sayfaListesi.add(w);
            }
        }
        return sayfaListesi;
    }

    public View widgetGorunumuOlustur(final Activity activity, final int widgetId, final WidgetIslemDinleyici dinleyici) {
        if (manager == null || host == null || activity == null) return null;
        try {
            AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetId);
            if (info == null) {
                widgetSil(context, widgetId);
                return null;
            }

            AppWidgetHostView hostView = host.createView(activity, widgetId, info);
            hostView.setAppWidget(widgetId, info);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int dp8 = ArayuzYardimcisi.dpToPx(activity, 8);
            int dp4 = ArayuzYardimcisi.dpToPx(activity, 4);
            lp.setMargins(dp8, dp4, dp8, dp4);
            hostView.setLayoutParams(lp);
            hostView.setMinimumHeight(ArayuzYardimcisi.dpToPx(activity, 60));

            // Ekran Aracı üzerine uzun basıldığında silme seçeneği
            hostView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (ArayuzYardimcisi.kilitliMiVeUyar(activity)) {
                        return true;
                    }
                    AyarDiyalogYardimcisi.onayDiyalogu(
                        activity,
                        "Ekran Aracı",
                        "Bu ekran aracını masaüstünden kaldırmak istiyor musunuz?",
                        "Kaldır",
                        new Runnable() {
                            @Override
                            public void run() {
                                widgetSil(context, widgetId);
                                if (host != null) {
                                    try { host.deleteAppWidgetId(widgetId); } catch (Throwable t) { LauncherLog.d("WidgetYoneticisi", "Widget host silinemedi"); }
                                }
                                if (dinleyici != null) dinleyici.onWidgetlerGuncellendi();
                                ArayuzYardimcisi.toast(activity, "Ekran aracı kaldırıldı.");
                            }
                        }
                    );
                    return true;
                }
            });

            return hostView;
        } catch (Throwable t) {
            LauncherLog.w("WidgetYoneticisi", "widgetGorunumuOlustur hatası", t);
            return null;
        }
    }

    public static List<WidgetKaydi> tumWidgetleriYukle(Context context) {
        List<WidgetKaydi> liste = new ArrayList<WidgetKaydi>();
        SharedPreferences sp = AyarBankasi.getAyarlar(context);
        String hamJson = sp.getString(PREF_WIDGETLAR_JSON, "[]");

        try {
            JSONArray arr = new JSONArray(hamJson);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                liste.add(new WidgetKaydi(obj.getInt("sayfa"), obj.getInt("widgetId")));
            }
        } catch (Throwable t) {
            LauncherLog.w("WidgetYoneticisi", "tumWidgetleriYukle JSON okuma hatası", t);
        }
        return liste;
    }

    public static void widgetleriKaydet(Context context, List<WidgetKaydi> liste) {
        try {
            JSONArray arr = new JSONArray();
            for (WidgetKaydi w : liste) {
                JSONObject obj = new JSONObject();
                obj.put("sayfa", w.sayfa);
                obj.put("widgetId", w.widgetId);
                arr.put(obj);
            }
            SharedPreferences sp = AyarBankasi.getAyarlar(context);
            AyarBankasi.editorKaydet(sp.edit().putString(PREF_WIDGETLAR_JSON, arr.toString()));
        } catch (Throwable t) {
            LauncherLog.e("WidgetYoneticisi", "widgetleriKaydet JSON yazma hatası", t);
        }
    }

    public static void widgetKaydet(Context context, int sayfa, int widgetId) {
        List<WidgetKaydi> tumu = tumWidgetleriYukle(context);
        tumu.add(new WidgetKaydi(sayfa, widgetId));
        widgetleriKaydet(context, tumu);
    }

    public static void widgetSil(Context context, int widgetId) {
        List<WidgetKaydi> tumu = tumWidgetleriYukle(context);
        for (int i = 0; i < tumu.size(); i++) {
            if (tumu.get(i).widgetId == widgetId) {
                tumu.remove(i);
                break;
            }
        }
        widgetleriKaydet(context, tumu);
    }
}

