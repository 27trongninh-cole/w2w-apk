package com.w2wtest.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Luu file ra thu muc cong khai Download/w2w/ de nguoi dung khong can vao sau trong
 * thu muc rieng cua app (Android/data/...) moi lay duoc file .wem/.ogg.
 *
 * - Android 10+ (API 29+): dung MediaStore (Downloads collection), khong can quyen gi ca,
 *   file luu that o Download/w2w/<ten file>.
 * - Android 9 tro xuong (API <=28): ghi truc tiep bang File API vao
 *   Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)/w2w/, can quyen
 *   WRITE_EXTERNAL_STORAGE (da khai bao trong Manifest, maxSdkVersion=28).
 */
public final class DownloadSaver {

    public interface Logger { void log(String msg); }

    /** Tra ve duong dan/URI hien thi de log cho nguoi dung biet file nam o dau. */
    public static String saveToDownloads(Context context, File srcFile, String mimeType, Logger log) {
        String displayName = srcFile.getName();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return saveViaMediaStore(context, srcFile, displayName, mimeType);
            } else {
                return saveViaLegacyFile(srcFile, displayName);
            }
        } catch (Exception e) {
            if (log != null) log.log("LOI khi luu " + displayName + " vao Download/w2w/: " + e.getMessage());
            return null;
        }
    }

    private static String saveViaMediaStore(Context context, File srcFile, String displayName, String mimeType) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/w2w");
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        // Xoa ban ghi cu cung ten (neu co) de tranh MediaStore tu doi ten thanh "ten (1).wem"
        // moi lan convert lai, giu duong dan on dinh Download/w2w/<ten file>.
        try {
            resolver.delete(collection,
                    MediaStore.Downloads.DISPLAY_NAME + "=? AND " + MediaStore.Downloads.RELATIVE_PATH + "=?",
                    new String[]{displayName, Environment.DIRECTORY_DOWNLOADS + "/w2w/"});
        } catch (Exception ignored) {}

        Uri itemUri = resolver.insert(collection, values);
        if (itemUri == null) throw new IOException("MediaStore insert() tra ve null");

        try (OutputStream os = resolver.openOutputStream(itemUri);
             FileInputStream fis = new FileInputStream(srcFile)) {
            if (os == null) throw new IOException("Khong mo duoc OutputStream cho " + itemUri);
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
        }

        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(itemUri, values, null, null);

        return "Download/w2w/" + displayName;
    }

    private static String saveViaLegacyFile(File srcFile, String displayName) throws IOException {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File w2wDir = new File(downloadDir, "w2w");
        if (!w2wDir.exists() && !w2wDir.mkdirs()) {
            throw new IOException("Khong tao duoc thu muc " + w2wDir.getAbsolutePath());
        }
        File dst = new File(w2wDir, displayName);
        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) fos.write(buf, 0, n);
        }
        return dst.getAbsolutePath();
    }

    private DownloadSaver() {}
}
