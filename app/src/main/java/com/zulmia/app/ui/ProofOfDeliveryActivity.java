package com.zulmia.app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zulmia.app.R;
import com.zulmia.app.data.InventoryRepository;
import com.zulmia.app.util.PodExporter;
import com.zulmia.app.util.SessionManager;

public class ProofOfDeliveryActivity extends AppCompatActivity {
    private long inventoryId;
    private String inventoryName;
    private String csvName;
    private SessionManager session;
    private InventoryRepository repo;

    private EditText recipientName;
    private EditText notes;
    private SignatureView signatureView;
    private androidx.recyclerview.widget.RecyclerView photosList;
    private PodPhotoAdapter photoAdapter;
    private final java.util.ArrayList<android.net.Uri> photoUris = new java.util.ArrayList<>();
    private android.net.Uri pendingCaptureUri;
    private static final int REQ_TAKE_PHOTO = 5011;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pod);

        session = new SessionManager(this);
        repo = new InventoryRepository(this);

        inventoryId = getIntent().getLongExtra("inventory_id", -1);
        inventoryName = getIntent().getStringExtra("inventory_name");
        csvName = getIntent().getStringExtra("csv_name");

        androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        if (tb != null) {
            setSupportActionBar(tb);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Proof of Delivery");
                getSupportActionBar().setSubtitle(inventoryName);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            tb.setNavigationOnClickListener(v -> finish());
        }

        recipientName = findViewById(R.id.inputRecipient);
        notes = findViewById(R.id.inputNotes);
        signatureView = findViewById(R.id.signatureView);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnShare = findViewById(R.id.btnShare);
        Button btnAddPhoto = findViewById(R.id.btnAddPhoto);
        photosList = findViewById(R.id.photosList);
        if (photosList != null) {
            photosList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
            photoAdapter = new PodPhotoAdapter(new PodPhotoAdapter.OnRemove() {
                @Override
                public void onRemove(int position) {
                    if (position >= 0 && position < photoUris.size()) {
                        photoUris.remove(position);
                        photoAdapter.setItems(photoUris);
                    }
                }
            });
            photosList.setAdapter(photoAdapter);
        }

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signatureView.clear();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPod(false);
            }
        });

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPod(true);
            }
        });

        btnAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });
    }

    private void createPod(boolean shareAfter) {
        String recipient = recipientName.getText() == null ? "" : recipientName.getText().toString().trim();
        if (TextUtils.isEmpty(recipient)) {
            Toast.makeText(this, "Recipient name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap signature = signatureView.isEmpty() ? null : signatureView.getSignatureBitmap();
        int totalQty = repo.getInventoryTotalQuantity(inventoryId);
        try {
            java.util.List<Bitmap> photos = loadPhotoBitmaps();
            PodExporter.ExportResult result = com.zulmia.app.util.PodExporter.exportPod(
                    this,
                    inventoryName,
                    inventoryId,
                    session.getUsername(),
                    totalQty,
                    recipient,
                    notes.getText() == null ? "" : notes.getText().toString().trim(),
                    signature,
                    photos
            );
            String display = result.displayName;
            // Persist for quick access from list
            getSharedPreferences("zulmia_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("pod_name_" + inventoryId, display)
                    .apply();

            Toast.makeText(this, "POD saved to Downloads/ZulMIA", Toast.LENGTH_SHORT).show();
            if (shareAfter) {
                Uri uri;
                if (result.contentUri != null) {
                    uri = result.contentUri;
                } else if (result.file != null) {
                    uri = androidx.core.content.FileProvider.getUriForFile(this, "com.zulmia.app.fileprovider", result.file);
                } else {
                    uri = PodUtils.findPdfInDownloads(this, display);
                }
                if (uri != null) {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("application/pdf");
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "Share POD"));
                } else {
                    Toast.makeText(this, "Unable to share file", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Offer to open
                Uri uri = PodUtils.findPdfInDownloads(this, display);
                if (uri != null) {
                    Intent open = new Intent(Intent.ACTION_VIEW);
                    open.setDataAndType(uri, "application/pdf");
                    open.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(Intent.createChooser(open, "Open POD"));
                    } catch (Exception ignored) { }
                }
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to create POD: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void launchCamera() {
        try {
            java.io.File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            if (dir == null) {
                Toast.makeText(this, "Storage unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            String fname = "POD_IMG_" + System.currentTimeMillis() + ".jpg";
            java.io.File f = new java.io.File(dir, fname);
            pendingCaptureUri = androidx.core.content.FileProvider.getUriForFile(this, "com.zulmia.app.fileprovider", f);
            android.content.Intent intent = new android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, pendingCaptureUri);
            intent.addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION | android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, REQ_TAKE_PHOTO);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_TAKE_PHOTO) {
            if (resultCode == RESULT_OK && pendingCaptureUri != null) {
                photoUris.add(pendingCaptureUri);
                if (photoAdapter != null) photoAdapter.setItems(photoUris);
            } else {
                // cleanup if file was created but canceled
            }
        }
    }

    private java.util.List<Bitmap> loadPhotoBitmaps() {
        java.util.ArrayList<Bitmap> out = new java.util.ArrayList<>();
        for (android.net.Uri uri : photoUris) {
            try {
                out.add(loadScaledBitmap(uri, 1600, 1600));
            } catch (Exception ignored) { }
        }
        return out;
    }

    private Bitmap loadScaledBitmap(android.net.Uri uri, int reqW, int reqH) throws Exception {
        android.content.ContentResolver resolver = getContentResolver();
        android.graphics.BitmapFactory.Options o1 = new android.graphics.BitmapFactory.Options();
        o1.inJustDecodeBounds = true;
        java.io.InputStream is1 = resolver.openInputStream(uri);
        try {
            android.graphics.BitmapFactory.decodeStream(is1, null, o1);
        } finally {
            if (is1 != null) is1.close();
        }
        int inSample = 1;
        int w = o1.outWidth, h = o1.outHeight;
        while (w / inSample > reqW || h / inSample > reqH) {
            inSample *= 2;
        }
        android.graphics.BitmapFactory.Options o2 = new android.graphics.BitmapFactory.Options();
        o2.inSampleSize = inSample;
        java.io.InputStream is2 = resolver.openInputStream(uri);
        try {
            return android.graphics.BitmapFactory.decodeStream(is2, null, o2);
        } finally {
            if (is2 != null) is2.close();
        }
    }
}


