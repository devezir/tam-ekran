package com.tamekran.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * Tam Ekran - Dinamik Sistem ve Paket Olay Yoneticisi
 * Paket yukleme/silme BroadcastReceiver, LauncherApps callback ve ekran dondurme ContentObserver
 * yasam dongulerini AnaEkranActivity disinda tek merkezden yonetir (SRP).
 */
public class SistemOlayYoneticisi {

    public interface OlayDinleyicisi {
        void onUygulamalarYenile();
        void onSistemRotasyonuDegisti();
    }

    private final Context appContext;
    private final OlayDinleyicisi dinleyici;
    private final Handler anaHandler = new Handler(Looper.getMainLooper());

    private BroadcastReceiver dinamikPaketAlicisi;
    private Object launcherAppsCallbackRef;
    private ContentObserver dondurmeGozlemcisi;

    public SistemOlayYoneticisi(Context context, OlayDinleyicisi dinleyici) {
        this.appContext = context.getApplicationContext();
        this.dinleyici = dinleyici;
    }

    public void baslat() {
        dinamikPaketAlicisiniBaslat();
        launcherAppsCallbackRef = launcherAppsKaydet();
        kaydetDondurmeGozlemcisi();
    }

    public void durdur() {
        if (dinamikPaketAlicisi != null) {
            try {
                appContext.unregisterReceiver(dinamikPaketAlicisi);
            } catch (Exception e) {
                LauncherLog.d("SistemOlayYoneticisi", "dinamikPaketAlicisi kaldirma atlandi: " + e.getMessage());
            }
            dinamikPaketAlicisi = null;
        }
        launcherAppsKaldir(launcherAppsCallbackRef);
        launcherAppsCallbackRef = null;
        if (dondurmeGozlemcisi != null) {
            try {
                appContext.getContentResolver().unregisterContentObserver(dondurmeGozlemcisi);
            } catch (Exception e) {
                LauncherLog.d("SistemOlayYoneticisi", "dondurmeGozlemcisi kaldirma atlandi: " + e.getMessage());
            }
            dondurmeGozlemcisi = null;
        }
    }

    private void uiUygulamalariYenile() {
        anaHandler.post(new Runnable() {
            @Override
            public void run() {
                if (dinleyici != null) {
                    dinleyici.onUygulamalarYenile();
                }
            }
        });
    }

    private void dinamikPaketAlicisiniBaslat() {
        try {
            dinamikPaketAlicisi = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    Uri data = intent.getData();
                    String pkg = data != null ? data.getSchemeSpecificPart() : null;
                    if (pkg == null || pkg.equals(appContext.getPackageName())) return;

                    boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
                    if (Intent.ACTION_PACKAGE_REMOVED.equals(action)
                            || "android.intent.action.PACKAGE_FULLY_REMOVED".equals(action)) {
                        if (!replacing) {
                            PaketDegisimAlicisi.paketiSistemdenTemizle(appContext, pkg);
                        }
                    } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                        if (!replacing) {
                            PaketDegisimAlicisi.paketiSistemeEkle(appContext, pkg);
                        }
                    }
                    SimgeOnbellegi.paketTemizle(pkg);
                    KlasorSimgeUretici.onbellegiTemizle();
                    uiUygulamalariYenile();
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            if (Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(dinamikPaketAlicisi, filter, Context.RECEIVER_EXPORTED);
            } else {
                appContext.registerReceiver(dinamikPaketAlicisi, filter);
            }
        } catch (Exception t) {
            LauncherLog.w("SistemOlayYoneticisi", "dinamikPaketAlicisi kayit hatasi", t);
        }
    }

    private void kaydetDondurmeGozlemcisi() {
        try {
            dondurmeGozlemcisi = new ContentObserver(anaHandler) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (dinleyici != null) {
                        dinleyici.onSistemRotasyonuDegisti();
                    }
                }
            };
            appContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false,
                dondurmeGozlemcisi
            );
        } catch (Exception t) {
            LauncherLog.w("SistemOlayYoneticisi", "dondurmeGozlemcisi kaydedilemedi", t);
        }
    }

    private Object launcherAppsKaydet() {
        if (Build.VERSION.SDK_INT < 21) return null;
        try {
            LauncherApps la = (LauncherApps) appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (la == null) return null;
            LauncherApps.Callback cb = new LauncherApps.Callback() {
                @Override
                public void onPackageRemoved(String packageName, UserHandle user) {
                    PaketDegisimAlicisi.paketiSistemdenTemizle(appContext, packageName);
                    SimgeOnbellegi.paketTemizle(packageName);
                    uiUygulamalariYenile();
                }
                @Override
                public void onPackageAdded(String packageName, UserHandle user) {
                    PaketDegisimAlicisi.paketiSistemeEkle(appContext, packageName);
                    uiUygulamalariYenile();
                }
                @Override
                public void onPackageChanged(String packageName, UserHandle user) {
                    SimgeOnbellegi.paketTemizle(packageName);
                    uiUygulamalariYenile();
                }
                @Override
                public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
                    uiUygulamalariYenile();
                }
                @Override
                public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
                    uiUygulamalariYenile();
                }
            };
            la.registerCallback(cb);
            return cb;
        } catch (Exception t) {
            LauncherLog.w("SistemOlayYoneticisi", "LauncherApps callback kaydedilemedi", t);
            return null;
        }
    }

    private void launcherAppsKaldir(Object callback) {
        if (Build.VERSION.SDK_INT < 21 || callback == null) return;
        try {
            LauncherApps la = (LauncherApps) appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (la != null && callback instanceof LauncherApps.Callback) {
                la.unregisterCallback((LauncherApps.Callback) callback);
            }
        } catch (Exception e) {
            LauncherLog.d("SistemOlayYoneticisi", "LauncherApps callback kaldirma atlandi: " + e.getMessage());
        }
    }
}
