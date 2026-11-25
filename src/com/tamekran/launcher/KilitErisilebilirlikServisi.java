package com.tamekran.launcher;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

/**
 * Tam Ekran - Çift Dokunmayla Ekran Kilitleme Servisi (Android 9.0 - 17)
 * Parmak izi okuyucuyu devre dışı bırakmadan ekranı anında kilitler.
 */
public class KilitErisilebilirlikServisi extends AccessibilityService {

    public static KilitErisilebilirlikServisi ornek;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ornek = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ornek = null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    /**
     * Ekranı donanımsal güç tuşuna basılmış gibi anında kilitler.
     */
    public static boolean ekraniKilitle() {
        if (ornek != null && Build.VERSION.SDK_INT >= 28) {
            return ornek.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
        return false;
    }
}
