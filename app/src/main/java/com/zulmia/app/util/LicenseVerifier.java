package com.zulmia.app.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LicenseVerifier {
    // Replace with your own RSA public key (Base64-encoded X.509, without headers)
    private static final String PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw8c9mfmWm0UqVdQp7mEw6F2u5W6qf5kzJ4JX1JzH2H1gDk3Q7Y/1Fszd1H8y9F6Gq9l3oF8q6kKqHh0Z+3g0yN2r1g8b3W7mJ0bF1vW1k4z3mZpR8s5K2h5m0i7o0f3hX2uQ1sVvT6g7U+j0lK6bM0M3z8q9jQ0u1r3k8w2ZrB0Z1vS0vHkZma6r7GYA1eZq+YH5J3z8j3aK3wIDAQAB";

    public static boolean isLicensed(Context context) {
        // Allow when app is debuggable
        boolean isDebuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (isDebuggable) return true;

        try {
            // Preferred location: app-scoped external files
            File licFile = new File(context.getExternalFilesDir(null), "license.bin");
            if (!licFile.exists()) {
                // Fallback: Downloads/ZulMIA/license.bin (may fail on modern Android without permissions)
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                licFile = new File(new File(downloads, "ZulMIA"), "license.bin");
            }
            if (!licFile.exists()) return false;

            String json = readAll(licFile);
            JSONObject obj = new JSONObject(json);
            String licensedDevice = obj.getString("deviceId");
            String expiry = obj.getString("expiry");
            String sigB64 = obj.getString("sig");

            String currentDevice = getDeviceId(context);
            if (!currentDevice.equals(licensedDevice)) return false;

            if (isExpired(expiry)) return false;

            String payload = licensedDevice + "|" + expiry;
            return verifySignature(payload.getBytes(StandardCharsets.UTF_8), Base64.decode(sigB64, Base64.DEFAULT));
        } catch (Exception e) {
            return false;
        }
    }

    public static String getDeviceId(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String model = Build.MODEL == null ? "" : Build.MODEL;
        return androidId + "-" + model;
    }

    private static boolean isExpired(String expiryIso) {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date exp = df.parse(expiryIso);
            return exp == null || new Date().after(exp);
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean verifySignature(byte[] data, byte[] signature) {
        try {
            byte[] keyBytes = Base64.decode(PUBLIC_KEY_BASE64, Base64.DEFAULT);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pub = kf.generatePublic(spec);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(pub);
            sig.update(data);
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private static String readAll(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        FileInputStream fis = new FileInputStream(f);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}


