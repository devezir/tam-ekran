package com.tamekran.launcher;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Tam Ekran - Cihaz Yöneticisi Alıcısı (Android 2.2 - 8.1)
 * Eski Android cihazlarda çift dokunuşla ekran kilitleme desteği sağlar.
 */
public class CihazYoneticiAlicisi extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {}

    @Override
    public void onDisabled(Context context, Intent intent) {}
}
