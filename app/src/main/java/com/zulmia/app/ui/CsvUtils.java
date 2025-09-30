package com.zulmia.app.ui;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

public class CsvUtils {
    public static Uri findCsvInDownloads(android.content.Context ctx, String displayName) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver resolver = ctx.getContentResolver();
                // Query by display name only to avoid RELATIVE_PATH inconsistencies across OEMs
                String selection = MediaStore.Downloads.DISPLAY_NAME + "=?";
                String[] args = new String[]{displayName};
                Cursor c = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Downloads._ID}, selection, args, MediaStore.Downloads.DATE_ADDED + " DESC");
                try {
                    if (c != null && c.moveToFirst()) {
                        long id = c.getLong(0);
                        return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    }
                } finally {
                    if (c != null) c.close();
                }
                return null;
            } else {
                java.io.File f = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ZulMIA/" + displayName);
                if (f.exists()) {
                    return androidx.core.content.FileProvider.getUriForFile(ctx, "com.zulmia.app.fileprovider", f);
                }
                return null;
            }
        } catch (Exception ignored) { }
        return null;
    }

    public static boolean deleteCsv(android.content.Context ctx, String displayName) {
        try {
            if (displayName == null || displayName.isEmpty()) return false;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver resolver = ctx.getContentResolver();
                String selection = MediaStore.Downloads.DISPLAY_NAME + "=? AND " + MediaStore.Downloads.RELATIVE_PATH + "=?";
                String[] args = new String[]{displayName, Environment.DIRECTORY_DOWNLOADS + "/ZulMIA/"};
                Cursor c = resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Downloads._ID}, selection, args, null);
                try {
                    if (c != null && c.moveToFirst()) {
                        long id = c.getLong(0);
                        Uri uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id));
                        return resolver.delete(uri, null, null) > 0;
                    }
                } finally {
                    if (c != null) c.close();
                }
                return false;
            } else {
                java.io.File f = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ZulMIA/" + displayName);
                return f.exists() && f.delete();
            }
        } catch (Exception ignored) { }
        return false;
    }
}


