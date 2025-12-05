package com.zulmia.app.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PodExporter {
    public static class ExportResult {
        public Uri contentUri;
        public File file;
        public String displayName;
    }

    public static ExportResult exportPod(Context context,
                                         String inventoryName,
                                         long inventoryId,
                                         String user,
                                         int totalQty,
                                         String recipientName,
                                         String notes,
                                         Bitmap signatureBitmap,
                                         java.util.List<Bitmap> photos) throws IOException {
        String safeName = inventoryName.replaceAll("[^A-Za-z0-9_-]", "_");
        String datePart = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        String filename = "POD_" + safeName + "_" + datePart + ".pdf";

        // Create PDF content
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 at 72dpi
        PdfDocument.Page page = doc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setTextSize(20f);
        title.setFakeBoldText(true);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTextSize(12f);

        int x = 40;
        int y = 60;
        canvas.drawText("Proof of Delivery", x, y, title);
        y += 28;
        canvas.drawText("Inventory: " + inventoryName + " (ID: " + inventoryId + ")", x, y, text);
        y += 18;
        canvas.drawText("User: " + (user == null ? "-" : user), x, y, text);
        y += 18;
        canvas.drawText("Total Quantity: " + totalQty, x, y, text);
        y += 18;
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        canvas.drawText("Date: " + now, x, y, text);
        y += 28;

        canvas.drawText("Recipient Name: " + (recipientName == null ? "-" : recipientName), x, y, text);
        y += 18;
        canvas.drawText("Notes: " + (notes == null ? "-" : notes), x, y, text);
        y += 30;

        if (signatureBitmap != null) {
            int maxW = pageInfo.getPageWidth() - 2 * x;
            int maxH = 200;
            Bitmap scaled = Bitmap.createScaledBitmap(signatureBitmap, Math.min(signatureBitmap.getWidth(), maxW), Math.min(signatureBitmap.getHeight(), maxH), true);
            canvas.drawText("Signature:", x, y, text);
            y += 8;
            canvas.drawRect(x, y, x + scaled.getWidth() + 8, y + scaled.getHeight() + 8, text);
            canvas.drawBitmap(scaled, x + 4, y + 4, null);
            y += scaled.getHeight() + 24;
        } else {
            canvas.drawText("Signature: (not provided)", x, y, text);
            y += 24;
        }

        doc.finishPage(page);

        // Add photos as subsequent pages
        if (photos != null) {
            for (Bitmap photo : photos) {
                if (photo == null) continue;
                PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(595, 842, doc.getPages().size() + 1).create();
                PdfDocument.Page p = doc.startPage(pi);
                Canvas c = p.getCanvas();
                int margin = 20;
                int availW = pi.getPageWidth() - margin * 2;
                int availH = pi.getPageHeight() - margin * 2;
                // Scale photo to fit within page with aspect ratio
                float scale = Math.min(availW / (float) photo.getWidth(), availH / (float) photo.getHeight());
                int dw = Math.round(photo.getWidth() * scale);
                int dh = Math.round(photo.getHeight() * scale);
                Bitmap scaled = Bitmap.createScaledBitmap(photo, dw, dh, true);
                int left = (pi.getPageWidth() - dw) / 2;
                int top = (pi.getPageHeight() - dh) / 2;
                c.drawBitmap(scaled, left, top, null);
                doc.finishPage(p);
            }
        }

        ExportResult result = new ExportResult();
        result.displayName = filename;

        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ZulMIA/");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Failed to create download entry");
            OutputStream os = resolver.openOutputStream(uri, "w");
            if (os == null) throw new IOException("Failed to open output stream");
            try {
                doc.writeTo(os);
                os.flush();
            } finally {
                os.close();
                doc.close();
            }
            try {
                ContentValues publish = new ContentValues();
                publish.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, publish, null, null);
            } catch (Exception ignored) { }
            result.contentUri = uri;
            return result;
        } else {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dir = new File(downloads, "ZulMIA");
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            File out = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(out);
            try {
                doc.writeTo(fos);
                fos.flush();
            } finally {
                fos.close();
                doc.close();
            }
            result.file = out;
            return result;
        }
    }
}


