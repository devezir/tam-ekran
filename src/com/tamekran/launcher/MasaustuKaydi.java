package com.tamekran.launcher;

import org.json.JSONObject;

/**
 * Tam Ekran - Birleşik Öğe Bilgi Modeli (Industry-Standard ItemInfo)
 * Masaüstü, Alt Bar (Dock) ve Klasör içi tüm öğeler benzersiz bir kimlik (id) ve
 * tekil bir kapsayıcı (container) ile temsil edilir.
 * Bir öğe aynı anda yalnızca TEK bir container içinde bulunabilir.
 */
public class MasaustuKaydi {

    public static final String CONTAINER_MASAUSTU = "desktop";
    public static final String CONTAINER_DOCK = "dock";

    public static final int TUR_UYGULAMA = 0;
    public static final int TUR_KLASOR = 1;
    public static final int TUR_KISAYOL = 2;
    public static final int TUR_ARAC = 3;
    public static final int TUR_WIDGET = 4;

    public String id;
    public int tur;
    public String bilesenKimligi;
    public String etiket;
    public String container;
    public int sayfa;
    public int cellX;
    public int cellY;
    public int spanX = 1;
    public int spanY = 1;

    public MasaustuKaydi() {
    }

    public MasaustuKaydi(
            String id,
            int tur,
            String bilesenKimligi,
            String etiket,
            String container,
            int sayfa,
            int cellX,
            int cellY,
            int spanX,
            int spanY) {
        this.id = id;
        this.tur = tur;
        this.bilesenKimligi = bilesenKimligi;
        this.etiket = etiket != null ? etiket : "";
        this.container = container != null ? container : CONTAINER_MASAUSTU;
        this.sayfa = Math.max(0, sayfa);
        this.cellX = Math.max(0, cellX);
        this.cellY = Math.max(0, cellY);
        this.spanX = Math.max(1, spanX);
        this.spanY = Math.max(1, spanY);
    }

    public boolean masaustundeMi() {
        return CONTAINER_MASAUSTU.equals(container);
    }

    public boolean docktaMi() {
        return CONTAINER_DOCK.equals(container);
    }

    public boolean klasorIcindeMi() {
        return container != null && !CONTAINER_MASAUSTU.equals(container) && !CONTAINER_DOCK.equals(container);
    }

    public boolean klasorMu() {
        return tur == TUR_KLASOR || (bilesenKimligi != null && bilesenKimligi.startsWith("klasor:"));
    }

    public String getKlasorId() {
        if (bilesenKimligi != null && bilesenKimligi.startsWith("klasor:")) {
            return bilesenKimligi.substring("klasor:".length());
        }
        return id;
    }

    public static int turBelirle(String bilesenKimligi) {
        if (bilesenKimligi == null) return TUR_UYGULAMA;
        if (bilesenKimligi.startsWith("klasor:")) return TUR_KLASOR;
        if (bilesenKimligi.startsWith("kisayol:")) return TUR_KISAYOL;
        if (bilesenKimligi.startsWith("arac:")) return TUR_ARAC;
        return TUR_UYGULAMA;
    }

    public MasaustuKaydi kopyala() {
        return new MasaustuKaydi(id, tur, bilesenKimligi, etiket, container, sayfa, cellX, cellY, spanX, spanY);
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id != null ? id : "");
            obj.put("tur", tur);
            obj.put("pkg", bilesenKimligi != null ? bilesenKimligi : "");
            obj.put("etiket", etiket != null ? etiket : "");
            obj.put("container", container != null ? container : CONTAINER_MASAUSTU);
            obj.put("sayfa", sayfa);
            obj.put("x", cellX);
            obj.put("y", cellY);
            obj.put("sx", spanX);
            obj.put("sy", spanY);
        } catch (Throwable t) {
            LauncherLog.w("MasaustuKaydi", "toJsonObject hatası", t);
        }
        return obj;
    }

    public static MasaustuKaydi fromJsonObject(JSONObject obj) {
        if (obj == null) return null;
        String pkg = obj.optString("pkg", "");
        if (pkg.trim().length() == 0) return null;
        String id = obj.optString("id", "");
        int tur = obj.optInt("tur", turBelirle(pkg));
        String etiket = obj.optString("etiket", "");
        String container = obj.optString("container", CONTAINER_MASAUSTU);
        int sayfa = obj.optInt("sayfa", 0);
        int x = obj.optInt("x", 0);
        int y = obj.optInt("y", 0);
        int sx = obj.optInt("sx", 1);
        int sy = obj.optInt("sy", 1);
        return new MasaustuKaydi(id, tur, pkg, etiket, container, sayfa, x, y, sx, sy);
    }
}
