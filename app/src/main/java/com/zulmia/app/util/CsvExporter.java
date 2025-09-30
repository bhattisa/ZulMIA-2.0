package com.zulmia.app.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.zulmia.app.data.InventoryRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CsvExporter {

    public static class ExportResult {
        public Uri contentUri;
        public File file;
        public String displayName;
    }

    public static ExportResult exportInventory(Context context, String username, String inventoryName, long inventoryId, long timestampMs, List<InventoryRepository.AggregatedScanRow> rows, String existingDisplayName, String exportFormat) throws IOException {
        String safeName = inventoryName.replaceAll("[^A-Za-z0-9_-]", "_");
        String datePart = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(timestampMs));
        String filename = (existingDisplayName != null && !existingDisplayName.isEmpty()) ? existingDisplayName : (safeName + "-" + datePart + ".csv");

        StringBuilder sb = new StringBuilder();
        
        // Check if we're using the Barcode and Count format
        boolean isBarcodeCountFormat = "barcode_count".equals(exportFormat);
        
        if (isBarcodeCountFormat) {
            // Barcode and Count Format - aggregate by barcode only
            sb.append("Barcode,Count\n");
            Map<String, Integer> barcodeCountMap = new HashMap<>();
            
            // Aggregate quantities by barcode across all locations/shelves
            for (InventoryRepository.AggregatedScanRow r : rows) {
                if (barcodeCountMap.containsKey(r.code)) {
                    barcodeCountMap.put(r.code, barcodeCountMap.get(r.code) + r.qty);
                } else {
                    barcodeCountMap.put(r.code, r.qty);
                }
            }
            
            // Write aggregated data
            for (Map.Entry<String, Integer> entry : barcodeCountMap.entrySet()) {
                sb.append(escapeCsv(entry.getKey())).append(',')
                  .append(entry.getValue()).append('\n');
            }
        } else {
            // Standard Format - all columns
            sb.append("Barcode/QR Code,Quantity,Location,Shelf,User,Timestamp\n");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            for (InventoryRepository.AggregatedScanRow r : rows) {
                String ts = df.format(new Date(r.timestampMs));
                sb.append(escapeCsv(r.code)).append(',')
                  .append(r.qty).append(',')
                  .append(escapeCsv(nullToEmpty(r.location))).append(',')
                  .append(escapeCsv(nullToEmpty(r.shelf))).append(',')
                  .append(escapeCsv(nullToEmpty(r.user))).append(',')
                  .append(escapeCsv(ts)).append('\n');
            }
        }

        ExportResult result = new ExportResult();
        result.displayName = filename;

        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = null;
            // Proactively remove any previous entries with the same display name so we always write a fresh file
            try {
                if (existingDisplayName != null && !existingDisplayName.isEmpty()) {
                    resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            MediaStore.Downloads.DISPLAY_NAME + "=?",
                            new String[]{existingDisplayName});
                } else if (filename != null && !filename.isEmpty()) {
                    resolver.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            MediaStore.Downloads.DISPLAY_NAME + "=?",
                            new String[]{filename});
                }
            } catch (Exception ignored) { }
            if (uri == null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ZulMIA/");
                values.put(MediaStore.MediaColumns.IS_PENDING, 1);
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    throw new IOException("Failed to create download entry");
                }
            }

            OutputStream os = resolver.openOutputStream(uri, "w");
            if (os == null) {
                throw new IOException("Failed to open output stream");
            }
            try {
                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
            } finally {
                os.close();
            }
            // If this was a new insert with pending flag, clear it; ignore if not applicable
            try {
                ContentValues publish = new ContentValues();
                publish.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, publish, null, null);
            } catch (Exception ignored) { }
            // Update displayName to ensure downstream lookup matches
            try {
                ContentValues nameUpdate = new ContentValues();
                nameUpdate.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                resolver.update(uri, nameUpdate, null, null);
            } catch (Exception ignored) { }
            result.contentUri = uri;
            return result;
        } else {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dir = new File(downloads, "ZulMIA");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File out = new File(dir, filename);
            if (out.exists()) {
                // delete before re-writing to ensure contents reflect latest edits
                //noinspection ResultOfMethodCallIgnored
                out.delete();
            }
            FileOutputStream fos = new FileOutputStream(out);
            try {
                fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                fos.flush();
            } finally {
                fos.close();
            }
            result.file = out;
            return result;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escapeCsv(String s) {
        boolean needQuotes = s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"");
        String out = s.replace("\"", "\"\"");
        if (needQuotes) {
            return "\"" + out + "\"";
        }
        return out;
    }
}


