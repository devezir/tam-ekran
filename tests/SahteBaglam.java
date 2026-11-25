package tests;

import android.content.SharedPreferences;
import com.tamekran.launcher.AyarBankasi;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Emülatör gerektirmeden kalıcılık, göç ve yedekleme testlerini JVM'de koşmak için
 * AyarBankasi.TestDepoSaglayici üzerinden bellek içi SharedPreferences ve geçici dosya dizini sağlar.
 */
public final class SahteBaglam implements AyarBankasi.TestDepoSaglayici {

    private final Map<String, SahteSharedPreferences> depolar = new HashMap<String, SahteSharedPreferences>();
    private final File dosyalarDizini;

    public SahteBaglam(File geciciDizin) {
        this.dosyalarDizini = geciciDizin;
        if (this.dosyalarDizini != null && !this.dosyalarDizini.exists()) {
            this.dosyalarDizini.mkdirs();
        }
    }

    @Override
    public File getDosyalarDizini() {
        return dosyalarDizini;
    }

    @Override
    public SharedPreferences getAyarlar(String dosyaAdi) {
        String anahtar = (dosyaAdi != null) ? dosyaAdi : "varsayilan";
        SahteSharedPreferences sp = depolar.get(anahtar);
        if (sp == null) {
            sp = new SahteSharedPreferences();
            depolar.put(anahtar, sp);
        }
        return sp;
    }

    public static final class SahteSharedPreferences implements SharedPreferences {
        private final Map<String, Object> veriler = new HashMap<String, Object>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<String, Object>(veriler);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = veriler.get(key);
            if (v == null) return defValue;
            if (v instanceof String) return (String) v;
            throw new ClassCastException("Not a String: " + key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object v = veriler.get(key);
            if (v == null) return defValues;
            if (v instanceof Set<?>) return new HashSet<String>((Set<String>) v);
            throw new ClassCastException("Not a Set: " + key);
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = veriler.get(key);
            return (v instanceof Integer) ? ((Integer) v).intValue() : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = veriler.get(key);
            return (v instanceof Long) ? ((Long) v).longValue() : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = veriler.get(key);
            return (v instanceof Float) ? ((Float) v).floatValue() : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = veriler.get(key);
            return (v instanceof Boolean) ? ((Boolean) v).booleanValue() : defValue;
        }

        @Override
        public boolean contains(String key) {
            return veriler.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new SahteEditor(veriler);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }
    }

    private static final class SahteEditor implements SharedPreferences.Editor {
        private final Map<String, Object> hedef;
        private final Map<String, Object> eklenecekler = new HashMap<String, Object>();
        private final Set<String> silinecekler = new HashSet<String>();
        private boolean temizle = false;

        SahteEditor(Map<String, Object> hedef) {
            this.hedef = hedef;
        }

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            eklenecekler.put(key, value);
            silinecekler.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            eklenecekler.put(key, values != null ? new HashSet<String>(values) : null);
            silinecekler.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            eklenecekler.put(key, Integer.valueOf(value));
            silinecekler.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            eklenecekler.put(key, Long.valueOf(value));
            silinecekler.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            eklenecekler.put(key, Float.valueOf(value));
            silinecekler.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            eklenecekler.put(key, Boolean.valueOf(value));
            silinecekler.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            silinecekler.add(key);
            eklenecekler.remove(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            temizle = true;
            eklenecekler.clear();
            silinecekler.clear();
            return this;
        }

        @Override
        public boolean commit() {
            uygula();
            return true;
        }

        @Override
        public void apply() {
            uygula();
        }

        private void uygula() {
            if (temizle) {
                hedef.clear();
            }
            for (String s : silinecekler) {
                hedef.remove(s);
            }
            for (Map.Entry<String, Object> e : eklenecekler.entrySet()) {
                if (e.getValue() == null) {
                    hedef.remove(e.getKey());
                } else {
                    hedef.put(e.getKey(), e.getValue());
                }
            }
        }
    }
}
