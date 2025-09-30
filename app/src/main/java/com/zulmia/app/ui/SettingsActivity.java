package com.zulmia.app.ui;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.zulmia.app.R;
import com.zulmia.app.util.SessionManager;
import com.zulmia.app.data.InventoryRepository;

public class SettingsActivity extends BaseActivity {
    private static final String KEY_CLEAR_SHELF_ON_SCAN = "clear_shelf_on_scan";
    private static final String KEY_EXPORT_FORMAT = "export_format";
    public static final String EXPORT_FORMAT_STANDARD = "standard";
    public static final String EXPORT_FORMAT_BARCODE_COUNT = "barcode_count";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        attachToolbar(R.id.toolbar);
        androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        setToolbarTitle(tb, "ZulMIA", "Settings");

        CheckBox cb = findViewById(R.id.checkbox_clear_shelf);
        boolean enabled = getSharedPreferences("session_prefs", MODE_PRIVATE)
                .getBoolean(KEY_CLEAR_SHELF_ON_SCAN, false);
        cb.setChecked(enabled);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                getSharedPreferences("session_prefs", MODE_PRIVATE)
                        .edit().putBoolean(KEY_CLEAR_SHELF_ON_SCAN, isChecked).apply();
            }
        });

        // Setup export format radio group
        RadioGroup radioGroup = findViewById(R.id.radio_export_format);
        RadioButton radioStandard = findViewById(R.id.radio_format_standard);
        RadioButton radioBarcodeCount = findViewById(R.id.radio_format_barcode_count);
        
        String currentFormat = getSharedPreferences("session_prefs", MODE_PRIVATE)
                .getString(KEY_EXPORT_FORMAT, EXPORT_FORMAT_STANDARD);
        
        if (EXPORT_FORMAT_BARCODE_COUNT.equals(currentFormat)) {
            radioBarcodeCount.setChecked(true);
        } else {
            radioStandard.setChecked(true);
        }
        
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String format = EXPORT_FORMAT_STANDARD;
                if (checkedId == R.id.radio_format_barcode_count) {
                    format = EXPORT_FORMAT_BARCODE_COUNT;
                }
                getSharedPreferences("session_prefs", MODE_PRIVATE)
                        .edit().putString(KEY_EXPORT_FORMAT, format).apply();
            }
        });

        findViewById(R.id.btnClearFinished).setOnClickListener(v -> {
            new AlertDialog.Builder(SettingsActivity.this)
                    .setTitle("Clear finished inventories?")
                    .setMessage("This will permanently remove finished inventories from this device. Are you sure?")
                    .setPositiveButton("Clear", (d, w) -> {
                        InventoryRepository repo = new InventoryRepository(SettingsActivity.this);
                        int removed = repo.clearFinishedInventories(getSharedPreferences("session_prefs", MODE_PRIVATE).getString("username", null));
                        Toast.makeText(SettingsActivity.this, "Removed " + removed + " finished inventories", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    public static boolean isClearShelfOnScanEnabled(android.content.Context ctx) {
        return ctx.getSharedPreferences("session_prefs", MODE_PRIVATE)
                .getBoolean(KEY_CLEAR_SHELF_ON_SCAN, false);
    }

    public static String getExportFormat(android.content.Context ctx) {
        return ctx.getSharedPreferences("session_prefs", MODE_PRIVATE)
                .getString(KEY_EXPORT_FORMAT, EXPORT_FORMAT_STANDARD);
    }
}
