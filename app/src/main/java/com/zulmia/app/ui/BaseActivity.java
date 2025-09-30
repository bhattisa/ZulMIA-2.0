package com.zulmia.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;

import com.zulmia.app.R;
import com.zulmia.app.util.SessionManager;

public abstract class BaseActivity extends AppCompatActivity {
    protected SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
    }

    protected void attachToolbar(int toolbarId) {
        Toolbar toolbar = findViewById(toolbarId);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    protected void setToolbarTitle(Toolbar toolbar, String main, String sub) {
        if (toolbar == null) return;
        String separator = " - ";
        String full = main + separator + sub;
        SpannableString ss = new SpannableString(full);
        // Reduce overall title size slightly to fit
        ss.setSpan(new RelativeSizeSpan(0.9f), 0, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Reduce subtitle even more
        ss.setSpan(new RelativeSizeSpan(0.75f), main.length() + separator.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbar.setTitle(ss);
        toolbar.setTitleTextColor(Color.BLACK);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle(ss);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            session.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_show_device_id) {
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null) androidId = "(unknown)";
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("ANDROID_ID", androidId));
            }
            Toast.makeText(this, "ANDROID_ID: " + androidId + "\n(Copied)", Toast.LENGTH_LONG).show();
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_unregister) {
            new AlertDialog.Builder(this)
                .setTitle("Unregister device?")
                .setMessage("This will clear registration and restart the app.")
                .setPositiveButton("Unregister", (dialog, which) -> {
                    try {
                        getSharedPreferences("zulmia_prefs", MODE_PRIVATE)
                            .edit().remove("registered_android_id").apply();
                    } catch (Exception ignored) {}
                    Intent i = new Intent(this, SplashActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


