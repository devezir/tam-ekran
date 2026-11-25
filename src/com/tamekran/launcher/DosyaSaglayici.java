package com.tamekran.launcher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Tam Ekran - Sıfır Bağımlılıklı Dosya Sağlayıcısı
 * Android 7.0+ (API 24+) sistemlerde FileUriExposedException hatasını
 * AndroidX FileProvider'a ihtiyaç duymadan, saf Java ile çözer.
 * APK güncelleme kurulumlarını ve JSON yedek dosyası paylaşımını destekler.
 */
public class DosyaSaglayici extends ContentProvider {

    public static final String OTORITE = "com.tamekran.launcher.dosyasaglayici";

    public static Uri getUriForFile(Context context, File file) {
        return Uri.parse("content://" + OTORITE + "/" + file.getName());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * Dizin geçişi (path traversal) saldırılarına karşı dosya adını doğrular.
     */
    public static boolean dosyaAdiGecerliMi(String dosyaAdi) {
        if (dosyaAdi == null || dosyaAdi.trim().isEmpty()) return false;
        if (dosyaAdi.contains("..") || dosyaAdi.contains("/") || dosyaAdi.contains("\\") || dosyaAdi.contains("\0")) {
            return false;
        }
        return true;
    }

    private File dizindenGuvenliDosyaBul(File dizin, String dosyaAdi) {
        if (dizin == null || !dosyaAdiGecerliMi(dosyaAdi)) return null;
        try {
            File aday = new File(dizin, dosyaAdi);
            String dizinYolu = dizin.getCanonicalPath();
            String adayYolu = aday.getCanonicalPath();
            if (!adayYolu.startsWith(dizinYolu + File.separator)) {
                return null;
            }
            if (aday.exists() && aday.isFile()) {
                return aday;
            }
        } catch (Exception e) {
            LauncherLog.w("DosyaSaglayici", "Guvenli dosya yolu kontrolu basarisiz: " + dosyaAdi, e);
        }
        return null;
    }

    private File bulDosya(String dosyaAdi) {
        if (getContext() == null || !dosyaAdiGecerliMi(dosyaAdi)) return null;

        File sonuc;
        if ((sonuc = dizindenGuvenliDosyaBul(getContext().getExternalCacheDir(), dosyaAdi)) != null) return sonuc;
        if ((sonuc = dizindenGuvenliDosyaBul(getContext().getCacheDir(), dosyaAdi)) != null) return sonuc;
        if ((sonuc = dizindenGuvenliDosyaBul(getContext().getExternalFilesDir(null), dosyaAdi)) != null) return sonuc;
        if ((sonuc = dizindenGuvenliDosyaBul(getContext().getFilesDir(), dosyaAdi)) != null) return sonuc;

        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String dosyaAdi = uri.getLastPathSegment();
        File dosya = bulDosya(dosyaAdi);

        if (dosya == null || !dosya.exists()) {
            throw new FileNotFoundException("Dosya bulunamadı: " + dosyaAdi);
        }

        return ParcelFileDescriptor.open(dosya, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        String dosyaAdi = uri.getLastPathSegment();
        if (dosyaAdi != null) {
            if (dosyaAdi.endsWith(".apk")) {
                return "application/vnd.android.package-archive";
            } else if (dosyaAdi.endsWith(".json")) {
                return "application/json";
            }
        }
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String dosyaAdi = uri.getLastPathSegment();
        File dosya = bulDosya(dosyaAdi);
        if (dosya == null || !dosya.exists()) return null;

        if (projection == null) {
            projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        }

        MatrixCursor cursor = new MatrixCursor(projection, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String col : projection) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                row.add(dosya.getName());
            } else if (OpenableColumns.SIZE.equals(col)) {
                row.add(dosya.length());
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
