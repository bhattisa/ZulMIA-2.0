package com.zulmia.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.zulmia.app.R;
import android.provider.Settings;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
// License gate intentionally disabled for now

import java.io.File;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView title = findViewById(R.id.appTitle);
        title.setText("ZulMIA - Inventory Count Application");

        TextView by = findViewById(R.id.developedByLabel);
        by.setText("Developed by");

        // Brand image is provided via layout: @drawable/zultec

        // Animate logo
        android.view.View logo = findViewById(R.id.logo);
        if (logo != null) {
            android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.splash_logo_anim);
            logo.startAnimation(anim);
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
				// Debug-only bypass: skip registration gate entirely
				if (com.zulmia.app.BuildConfig.BYPASS_REGISTRATION) {
					com.zulmia.app.util.SessionManager session = new com.zulmia.app.util.SessionManager(SplashActivity.this);
					if (session.getUsername() != null) {
						startActivity(new Intent(SplashActivity.this, MainActivity.class));
					} else {
						startActivity(new Intent(SplashActivity.this, LoginActivity.class));
					}
					finish();
					return;
				}

                // Determine current device ANDROID_ID (lowercased for comparisons)
                String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (androidId != null) androidId = androidId.trim().toLowerCase();

                // 1) If we have a saved registration for this device, skip registration
                SharedPreferences sp = getSharedPreferences("zulmia_prefs", MODE_PRIVATE);
                String savedId = sp.getString("registered_android_id", null);
                if (savedId != null && androidId != null && savedId.trim().equalsIgnoreCase(androidId)) {
                    com.zulmia.app.util.SessionManager session = new com.zulmia.app.util.SessionManager(SplashActivity.this);
                    if (session.getUsername() != null) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    }
                    finish();
                    return;
                }

                // 2) Otherwise, check the allowed ids array from resources
                String[] allowed = getResources().getStringArray(R.array.allowed_android_ids);
                boolean allowedMatch = false;
                if (allowed != null && androidId != null) {
                    for (String id : allowed) {
                        if (id != null && !id.trim().isEmpty() && androidId.equals(id.trim().toLowerCase())) { allowedMatch = true; break; }
                    }
                }
                if (allowedMatch) {
                    android.widget.Toast.makeText(SplashActivity.this, "Allowed: " + androidId, android.widget.Toast.LENGTH_SHORT).show();
                    // Persist registration so future launches skip checks
                    SharedPreferences sp2 = getSharedPreferences("zulmia_prefs", MODE_PRIVATE);
                    if (androidId != null) {
                        sp2.edit().putString("registered_android_id", androidId).apply();
                    }
                    com.zulmia.app.util.SessionManager session = new com.zulmia.app.util.SessionManager(SplashActivity.this);
                    if (session.getUsername() != null) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    }
                    finish();
                    return;
                }

                // 3) Fallback: not saved and not allowed -> go to register
                android.widget.Toast.makeText(SplashActivity.this, "Register. ANDROID_ID: " + androidId, android.widget.Toast.LENGTH_LONG).show();
                startActivity(new Intent(SplashActivity.this, RegisterActivity.class));
                finish();
            }
        }, 1500);
    }

    // No external image loading; using @drawable/zultec from resources
}


