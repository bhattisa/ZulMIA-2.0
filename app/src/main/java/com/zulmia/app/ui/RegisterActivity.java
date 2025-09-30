package com.zulmia.app.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zulmia.app.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private static final String REGISTER_URL = "http://195.250.24.128:2026/api/Devices/RegisterDevice";
    private static final String PREFS = "zulmia_prefs";
    private static final String KEY_REGISTERED_ID = "registered_android_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText inputCustomer = findViewById(R.id.inputCustomer);
        EditText inputApp = findViewById(R.id.inputApp);
        // Keep inputs empty by default (no prefill)

        Button btn = findViewById(R.id.btnRegisterDevice);
        btn.setOnClickListener(v -> attemptRegister(btn, inputCustomer, inputApp));
    }

    private void attemptRegister(Button btn, EditText inputCustomer, EditText inputApp) {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.trim().isEmpty()) {
            Toast.makeText(this, "Android ID unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        btn.setEnabled(false);
        Toast.makeText(this, "Registering...", Toast.LENGTH_SHORT).show();
        String customer = inputCustomer == null ? null : inputCustomer.getText().toString().trim();
        String app = inputApp == null ? null : inputApp.getText().toString().trim();
        if (customer == null || customer.isEmpty()) {
            Toast.makeText(this, "Enter Customer", Toast.LENGTH_SHORT).show();
            btn.setEnabled(true);
            return;
        }
        if (app == null || app.isEmpty()) {
            Toast.makeText(this, "Enter Application Name", Toast.LENGTH_SHORT).show();
            btn.setEnabled(true);
            return;
        }
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(new Date());

        android.util.Log.d("RegisterActivity", "Attempting register: url=" + REGISTER_URL + ", customer=" + customer + ", app=" + app + ", androidId=" + androidId + ", ts=" + timestamp);
        new RegisterTask(customer, app, androidId, timestamp, btn).execute();
    }

    private class RegisterTask extends AsyncTask<Void, Void, Boolean> {
        private final String customer;
        private final String app;
        private final String androidId;
        private final String timestamp;
        private String error;
        private final Button btn;

        RegisterTask(String customer, String app, String androidId, String timestamp, Button btn) {
            this.customer = customer;
            this.app = app;
            this.androidId = androidId;
            this.timestamp = timestamp;
            this.btn = btn;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(REGISTER_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json,text/plain,*/*");

                String json = "{" 
                        + "\"customer\":\"" + customer + "\"," 
                        + "\"app\":\"" + app + "\"," 
                        + "\"androidId\":\"" + androidId + "\"," 
                        + "\"requestDatetime\":\"" + timestamp + "\"" 
                        + "}";
                android.util.Log.d("RegisterActivity", "JSON POST body: " + json);
                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                android.util.Log.d("RegisterActivity", "JSON POST response code: " + code);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                String body = sb.toString().trim();
                android.util.Log.d("RegisterActivity", "JSON POST response body: " + body);
                boolean ok = body.equalsIgnoreCase("true") || body.toLowerCase(Locale.US).contains("\"success\":true") || body.equals("1");
                if (!ok) error = "HTTP " + code + ": " + body;
                return ok;
            } catch (Exception e) {
                error = e.getMessage();
                android.util.Log.e("RegisterActivity", "Exception during register", e);
                return false;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            btn.setEnabled(true);
            if (Boolean.TRUE.equals(success)) {
                SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
                sp.edit().putString(KEY_REGISTERED_ID, androidId).apply();
                Toast.makeText(RegisterActivity.this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                // Return to splash/next screen
                startActivity(new Intent(RegisterActivity.this, SplashActivity.class));
                finish();
            } else {
                String msg = getString(R.string.registration_failed);
                if (error != null && !error.trim().isEmpty()) msg = msg + "\n" + error;
                Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show();
                if (error != null) android.util.Log.e("RegisterActivity", "Registration error: " + error);
            }
        }
    }
}


