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

public class LoginActivity extends AppCompatActivity {
    private InventoryRepository repo;
    private SessionManager session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        repo = new InventoryRepository(this);
        session = new SessionManager(this);

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        Button login = findViewById(R.id.loginBtn);
        Button register = findViewById(R.id.registerBtn);
        TextView status = findViewById(R.id.statusText);

        if (!repo.userExists()) {
            status.setText("No user found. Please register.");
        } else {
            status.setText("Please log in.");
        }

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String u = username.getText().toString().trim();
                String p = password.getText().toString().trim();
                if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
                    Toast.makeText(LoginActivity.this, "Enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (repo.validateUser(u, p)) {
                    session.setUsername(u);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String u = username.getText().toString().trim();
                String p = password.getText().toString().trim();
                if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
                    Toast.makeText(LoginActivity.this, "Enter username and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (repo.createUser(u, p)) {
                    session.setUsername(u);
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "User already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}


