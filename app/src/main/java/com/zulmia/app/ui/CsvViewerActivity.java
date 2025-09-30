package com.zulmia.app.ui;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zulmia.app.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CsvViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csv_viewer);

        TextView text = findViewById(R.id.csvText);
        Uri uri = getIntent().getData();
        if (uri == null) {
            Toast.makeText(this, "No file to open", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ContentResolver resolver = getContentResolver();
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            text.setText(sb.toString());
        } catch (IOException e) {
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}


