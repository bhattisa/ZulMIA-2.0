package com.zulmia.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zulmia.app.R;
import com.zulmia.app.data.InventoryRepository;
import com.zulmia.app.util.SessionManager;

public class InventoryActivity extends BaseActivity {
    private SessionManager session;
    private InventoryRepository repo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        session = new SessionManager(this);
        repo = new InventoryRepository(this);

        attachToolbar(R.id.toolbar);
        androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        setToolbarTitle(tb, "ZulMIA", "Inventory Count Application");

        TextView welcome = findViewById(R.id.welcomeText);
        EditText inventoryName = findViewById(R.id.inventoryName);
        EditText defaultLocation = findViewById(R.id.defaultLocation);
        Button create = findViewById(R.id.createInventoryBtn);

        String username = session.getUsername();
        if (username == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        welcome.setText("Welcome, " + username);

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inventoryName.getText().toString().trim();
                String defLoc = defaultLocation.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(InventoryActivity.this, "Enter inventory name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (repo.inventoryExists(name, username)) {
                    Toast.makeText(InventoryActivity.this, "Inventory already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                long id = repo.createInventory(name, username, System.currentTimeMillis(), defLoc);
                if (id != -1) {
                    session.setInventory(id, name, defLoc);
                    startActivity(new Intent(InventoryActivity.this, ScanActivity.class));
                } else {
                    Toast.makeText(InventoryActivity.this, "Failed to create inventory", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // No pending inventories displayed on this screen
    }
}


