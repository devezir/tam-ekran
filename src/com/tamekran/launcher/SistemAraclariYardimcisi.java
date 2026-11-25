package com.tamekran.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.speech.RecognizerIntent;

import java.util.ArrayList;
import java.util.List;

/**
 * Tam Ekran - Merkezi Sistem Araçları ve Niyet Yardımcısı (System Tools & Intents Helper)
 * Saat, takvim, hesap makinesi, sesli arama, uygulama sistem ayarları ve kaldırma
 * işlemlerini tek bir merkezde toplar (DRY ve SRP).
 */
public final class SistemAraclariYardimcisi {

    private SistemAraclariYardimcisi() {}

    private static final String[] SAAT_PAKETLERI = new String[]{
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.huawei.deskclock",
        "com.coloros.alarm",
        "com.oppo.alarm",
        "com.vivo.deskclock"
    };

    private static final String[] TAKVIM_PAKETLERI = new String[]{
        "com.google.android.calendar",
        "com.android.calendar",
        "com.samsung.android.calendar",
        "com.huawei.calendar",
        "com.bbk.calendar"
    };

    /**
     * Cihazdaki varsayılan saat / alarm uygulamasını başlatır.
     */
    public static void saatUygulamasiniAc(Context context) {
        if (context == null) return;

        // 1. Standart AlarmClock niyetini dene
        try {
            Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "Standart show alarms intent basarisiz: " + t.getMessage());
        }

        try {
            Intent intent2 = new Intent(AlarmClock.ACTION_SET_ALARM);
            if (!(context instanceof Activity)) intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent2);
            return;
        } catch (Throwable t2) {
            LauncherLog.d("SistemAraclari", "Standart set alarm intent basarisiz: " + t2.getMessage());
        }

        // 2. Yaygın üretici saat paketlerini dene
        for (String pkg : SAAT_PAKETLERI) {
            if (ArayuzYardimcisi.uygulamaBaslat(context, pkg)) {
                return;
            }
        }

        // 3. Bulunamazsa takvimi açmayı dene
        takvimUygulamasiniAc(context);
    }

    /**
     * Cihazdaki takvim uygulamasını başlatır.
     */
    public static void takvimUygulamasiniAc(Context context) {
        if (context == null) return;

        try {
            long baslangic = System.currentTimeMillis();
            Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
            builder.appendPath("time");
            ContentUris.appendId(builder, baslangic);
            Intent intent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "Standart takvim intent basarisiz: " + t.getMessage());
        }

        for (String pkg : TAKVIM_PAKETLERI) {
            if (ArayuzYardimcisi.uygulamaBaslat(context, pkg)) {
                return;
            }
        }

        ArayuzYardimcisi.toast(context, "Takvim veya Saat uygulaması bulunamadı.");
    }

    /**
     * Cihazdaki hesap makinesi uygulamasını başlatır.
     */
    public static void hesapMakinesiAc(Context context) {
        if (context == null) return;

        List<Intent> niyetler = new ArrayList<Intent>();
        niyetler.add(new Intent().setClassName("com.google.android.calculator", "com.android.calculator2.Calculator"));
        niyetler.add(new Intent().setClassName("com.android.calculator2", "com.android.calculator2.Calculator"));
        niyetler.add(new Intent().setClassName("com.miui.calculator", "com.miui.calculator.cal.CalculatorActivity"));
        niyetler.add(new Intent().setClassName("com.sec.android.app.popupcalculator", "com.sec.android.app.popupcalculator.Calculator"));

        for (Intent n : niyetler) {
            try {
                if (!(context instanceof Activity)) n.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(n);
                return;
            } catch (Throwable t) {
                LauncherLog.d("SistemAraclari", "Hesap makinesi intent basarisiz: " + t.getMessage());
            }
        }

        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR);
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(context, "Hesap makinesi bulunamadı.");
        }
    }

    /**
     * Cihazın sesli arama motorunu başlatır.
     */
    public static void sesliAramaBaslat(Context context) {
        if (context == null) return;

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            try {
                Intent ses = new Intent(Intent.ACTION_VOICE_COMMAND);
                if (!(context instanceof Activity)) ses.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(ses);
            } catch (Throwable t2) {
                ArayuzYardimcisi.toast(context, "Sesli arama motoru bulunamadı.");
            }
        }
    }

    /**
     * İlgili paketin Android sistem 'Uygulama Bilgisi' (App Info) ayarlarını açar.
     */
    public static void uygulamaBilgisiAc(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return;

        try {
            Intent niyet = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            niyet.setData(Uri.parse("package:" + paketAdi));
            if (!(context instanceof Activity)) niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(niyet);
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(context, "Uygulama bilgisi açılamadı: " + t.getMessage());
        }
    }

    /**
     * Tam Ekran'ın kendi Android Sistem Uygulama Bilgileri ekranını açar ve Android 13+
     * cihazlarda "Kısıtlı ayarlara izin ver" menüsü için rehber mesajı gösterir.
     */
    public static void kisitliAyarlarIcinUygulamaBilgisiAc(Context context) {
        if (context == null) return;
        uygulamaBilgisiAc(context, context.getPackageName());
        if (Build.VERSION.SDK_INT >= 33) {
            ArayuzYardimcisi.toastUzun(context, context.getString(R.string.ayar_kisitli_izin_rehber));
        }
    }

    /**
     * İlgili paketi cihazdan kaldırma (Uninstall) iletişim penceresini başlatır.
     */
    public static void uygulamayiSil(Context context, String paketAdi) {
        if (context == null || paketAdi == null) return;
        String safPaket = paketAdi.contains("/") ? paketAdi.split("/", 2)[0] : paketAdi;
        if (safPaket.trim().isEmpty()) return;

        try {
            android.content.pm.ApplicationInfo ai = context.getPackageManager().getApplicationInfo(safPaket, 0);
            boolean sistemUygulamasi = (ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean guncellenmisSistem = (ai.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            if (sistemUygulamasi && !guncellenmisSistem) {
                ArayuzYardimcisi.toast(context, "Sistem uygulaması kaldırılamaz; uygulama ayarları açılıyor.");
                uygulamaBilgisiAc(context, safPaket);
                return;
            }
        } catch (Exception e) {
            LauncherLog.d("SistemAraclari", "Paket bilgisi kontrol edilemedi: " + e.getMessage());
        }

        PackageManager pm = context.getPackageManager();
        Intent[] adaylar = new Intent[]{
            new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + safPaket)),
            new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:" + safPaket))
        };
        for (Intent niyet : adaylar) {
            try {
                if (!(context instanceof Activity)) niyet.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (pm == null || niyet.resolveActivity(pm) != null) {
                    context.startActivity(niyet);
                    return;
                }
            } catch (Exception e) {
                LauncherLog.d("SistemAraclari", "Kaldirma niyeti basarisiz (" + niyet.getAction() + "): " + e.getMessage());
            }
        }

        ArayuzYardimcisi.toast(context, "Kaldırma penceresi açılamadı; uygulama bilgileri açılıyor.");
        uygulamaBilgisiAc(context, safPaket);
    }

    /**
     * Sistem duvar kağıdı seçicisini açar.
     */
    public static void duvarKagidiSeciciAc(Activity activity) {
        if (activity == null) return;

        try {
            Intent niyet = new Intent(Intent.ACTION_SET_WALLPAPER);
            activity.startActivity(Intent.createChooser(niyet, activity.getString(R.string.duvar_kagidi_sec)));
        } catch (Throwable t) {
            ArayuzYardimcisi.toast(activity, "Duvar kağıdı seçici bulunamadı.");
        }
    }

    // =========================================================================
    // VARSAYILAN BAŞLATICI (HOME ROLE) VE SİSTEM AYARLARI NİYETLERİ (SSOT)
    // =========================================================================

    public static final int REQUEST_VARSAYILAN_BASLATICI = 8001;

    public static boolean varsayilanBaslaticiMi(Context context) {
        if (context == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                android.app.role.RoleManager roleManager = (android.app.role.RoleManager) context.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                    return roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME);
                }
            }
            Intent niyet = new Intent(Intent.ACTION_MAIN);
            niyet.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo info = context.getPackageManager().resolveActivity(niyet, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null && info.activityInfo != null) {
                return context.getPackageName().equals(info.activityInfo.packageName);
            }
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "varsayilanBaslaticiMi tespiti basarisiz: " + t.getMessage());
        }
        return false;
    }

    public static void varsayilanBaslaticiAyarla(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        // 1. Android 10+ (API 29+) RoleManager standart yöntemi
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                android.app.role.RoleManager roleManager = (android.app.role.RoleManager) activity.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                    if (!roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                        Intent roleIntent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME);
                        activity.startActivityForResult(roleIntent, REQUEST_VARSAYILAN_BASLATICI);
                        return;
                    } else {
                        ArayuzYardimcisi.toast(activity, "Tam Ekran zaten varsayılan ana ekran.");
                        return;
                    }
                }
            } catch (Throwable t) {
                LauncherLog.d("SistemAraclari", "RoleManager baslatilamadi: " + t.getMessage());
            }
        }

        // 2. Xiaomi / Redmi / POCO (MIUI / HyperOS) Özel Yönlendirmeleri
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer != null && (manufacturer.equalsIgnoreCase("xiaomi") ||
                                     manufacturer.equalsIgnoreCase("redmi") ||
                                     manufacturer.equalsIgnoreCase("poco"))) {
            try {
                Intent miuiIntent = new Intent("miui.intent.action.PREFERRED_APP_SETTINGS");
                miuiIntent.addCategory(Intent.CATEGORY_DEFAULT);
                activity.startActivity(miuiIntent);
                ArayuzYardimcisi.toastUzun(activity, "Lütfen 'Başlatıcı' listesinden Tam Ekran'ı seçin.");
                return;
            } catch (Throwable t) {
                LauncherLog.d("SistemAraclari", "MIUI Intent 1 basarisiz: " + t.getMessage());
            }

            try {
                Intent miuiIntent2 = new Intent();
                miuiIntent2.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity"));
                activity.startActivity(miuiIntent2);
                ArayuzYardimcisi.toastUzun(activity, "Lütfen Güvenlik > Varsayılan Uygulamalar bölümünden Tam Ekran'ı seçin.");
                return;
            } catch (Throwable t) {
                LauncherLog.d("SistemAraclari", "MIUI Intent 2 basarisiz: " + t.getMessage());
            }
        }

        // 3. Android Standart Ayarlar > Ana Ekran Ayarları
        try {
            Intent niyet = new Intent(Settings.ACTION_HOME_SETTINGS);
            activity.startActivity(niyet);
            return;
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "ACTION_HOME_SETTINGS basarisiz: " + t.getMessage());
        }

        // 4. Android Standart Ayarlar > Varsayılan Uygulamalar
        try {
            Intent niyet = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            activity.startActivity(niyet);
            return;
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "ACTION_MANAGE_DEFAULT_APPS_SETTINGS basarisiz: " + t.getMessage());
        }

        // 5. Sistem Başlatıcı Seçici Diyaloğu
        try {
            Intent homeChooser = new Intent(Intent.ACTION_MAIN);
            homeChooser.addCategory(Intent.CATEGORY_HOME);
            homeChooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(Intent.createChooser(homeChooser, "Varsayılan Başlatıcıyı Seçin"));
            return;
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "createChooser basarisiz: " + t.getMessage());
        }

        // 6. Uygulama Detayları Sayfası
        try {
            Intent detailsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            detailsIntent.setData(Uri.fromParts("package", activity.getPackageName(), null));
            activity.startActivity(detailsIntent);
            ArayuzYardimcisi.toastUzun(activity, "Lütfen bu sayfadaki 'Varsayılan Olarak Aç' veya 'Ana Ekran' seçeneğinden Tam Ekran'ı belirleyin.");
            return;
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "APPLICATION_DETAILS_SETTINGS basarisiz: " + t.getMessage());
        }

        // 7. Genel Sistem Ayarları Son Çare
        try {
            activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            ArayuzYardimcisi.toastUzun(activity, "Lütfen Uygulamalar > Varsayılan Uygulamalar menüsünden Tam Ekran'ı seçin.");
        } catch (Throwable t) {
            LauncherLog.w("SistemAraclari", "Genel ayarlar dahi acilamadi", t);
        }
    }

    public static void pilAyarlariniAc(Activity activity) {
        if (activity == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable t2) {
                LauncherLog.d("SistemAraclari", "Ayarlar acilamadi: " + t2.getMessage());
            }
        }
    }

    public static void parlaklikAyarlariniAc(Activity activity) {
        if (activity == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable t2) {
                LauncherLog.d("SistemAraclari", "Ekran ayarlari acilamadi: " + t2.getMessage());
            }
        }
    }

    public static void ucakModuAyarlariniAc(Activity activity) {
        if (activity == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable t2) {
                LauncherLog.d("SistemAraclari", "Ucak modu ayarlari acilamadi: " + t2.getMessage());
            }
        }
    }

    public static void bluetoothAyarlariniAc(Activity activity) {
        if (activity == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Throwable t) {
            LauncherLog.d("SistemAraclari", "Bluetooth ayarlari acilamadi: " + t.getMessage());
        }
    }
}
