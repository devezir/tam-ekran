package com.tamekran.launcher;

import android.util.Log;

/**
 * Tam Ekran - Merkezi ve Güvenli Loglama Motoru
 * Hem Android çalışma zamanında LogCat'e yazar hem de JVM birim testlerinde stub çökmesini engeller.
 */
public class LauncherLog {
    private static final String TAG_PREFIX = "TamEkran_";
    public static boolean DEBUG = false;

    public static void d(String tag, String msg) {
        if (!DEBUG) return;
        try {
            Log.d(TAG_PREFIX + tag, msg);
        } catch (Throwable t) {
            System.out.println("[DEBUG][" + tag + "] " + msg);
        }
    }

    public static void w(String tag, String msg, Throwable tr) {
        try {
            Log.w(TAG_PREFIX + tag, msg, tr);
        } catch (Throwable t) {
            System.err.println("[WARN][" + tag + "] " + msg + (tr != null ? " : " + tr.getMessage() : ""));
        }
    }

    public static void w(String tag, String msg) {
        w(tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable tr) {
        try {
            Log.e(TAG_PREFIX + tag, msg, tr);
        } catch (Throwable t) {
            System.err.println("[ERROR][" + tag + "] " + msg + (tr != null ? " : " + tr.getMessage() : ""));
        }
    }

    public static void e(String tag, String msg) {
        e(tag, msg, null);
    }
}
