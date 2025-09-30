package com.zulmia.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zulmia.app.R;
import com.zulmia.app.data.InventoryRepository;
import com.zulmia.app.util.SessionManager;

import java.util.List;

public class MainActivity extends BaseActivity {
    private SessionManager session;
    private InventoryRepository repo;
    private InventoryAdapter pendingAdapter;
    private FinishedInventoryAdapter finishedAdapter;
    private android.widget.TextView txtPendingTitle;
    private android.widget.TextView txtFinishedTitle;
    private androidx.recyclerview.widget.RecyclerView pendingList;
    private androidx.recyclerview.widget.RecyclerView finishedList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = new SessionManager(this);
        repo = new InventoryRepository(this);

        attachToolbar(R.id.toolbar);
        androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        setToolbarTitle(tb, "ZulMIA", "Home");

        Button newInventory = findViewById(R.id.btnNewInventory);
        txtPendingTitle = findViewById(R.id.txtPendingTitle);
        txtFinishedTitle = findViewById(R.id.txtFinishedTitle);
        pendingList = findViewById(R.id.recyclerPending);
        finishedList = findViewById(R.id.recyclerFinished);

        pendingList.setLayoutManager(new LinearLayoutManager(this));
        finishedList.setLayoutManager(new LinearLayoutManager(this));

        pendingAdapter = new InventoryAdapter(new InventoryAdapter.OnInventoryClick() {
            @Override
            public void onClick(InventoryRepository.InventoryRow row) {
                session.setInventory(row.id, row.name, row.defaultLocation == null ? "" : row.defaultLocation);
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
            }
        });
        pendingList.setAdapter(pendingAdapter);

        finishedAdapter = new FinishedInventoryAdapter(new FinishedInventoryAdapter.OnInventoryClick() {
            @Override
            public void onClick(InventoryRepository.InventoryRow row) {
                session.setInventory(row.id, row.name, row.defaultLocation == null ? "" : row.defaultLocation);
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
            }
        });
        finishedList.setAdapter(finishedAdapter);

        // Swipe to delete with confirmation for Pending
        new androidx.recyclerview.widget.ItemTouchHelper(new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(androidx.recyclerview.widget.RecyclerView recyclerView, androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, androidx.recyclerview.widget.RecyclerView.ViewHolder target) { return false; }
            @Override
            public void onSwiped(androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                InventoryRepository.InventoryRow row = pendingAdapter.getItem(pos);
                if (row == null) { refreshData(); return; }
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Remove inventory?")
                        .setMessage("This will remove the pending inventory from this device.")
                        .setPositiveButton("Remove", (d, w) -> {
                            new InventoryRepository(MainActivity.this).deleteInventory(row.id, false);
                            refreshData();
                        })
                        .setNegativeButton("Cancel", (d, w) -> { refreshData(); })
                        .show();
            }
        }).attachToRecyclerView(pendingList);

        // Swipe to delete with confirmation for Finished (also delete CSV)
        new androidx.recyclerview.widget.ItemTouchHelper(new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(androidx.recyclerview.widget.RecyclerView recyclerView, androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, androidx.recyclerview.widget.RecyclerView.ViewHolder target) { return false; }
            @Override
            public void onSwiped(androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                InventoryRepository.InventoryRow row = finishedAdapter.getItem(pos);
                if (row == null) { refreshData(); return; }
                String msg = "This will remove the finished inventory and delete its CSV file from device. Are you sure?";
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Remove finished inventory?")
                        .setMessage(msg)
                        .setPositiveButton("Remove", (d, w) -> {
                            if (row.csvName != null && !row.csvName.isEmpty()) {
                                CsvUtils.deleteCsv(MainActivity.this, row.csvName);
                            }
                            new InventoryRepository(MainActivity.this).deleteInventory(row.id, true);
                            refreshData();
                        })
                        .setNegativeButton("Cancel", (d, w) -> { refreshData(); })
                        .show();
            }
        }).attachToRecyclerView(finishedList);

        newInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, InventoryActivity.class));
            }
        });

        refreshData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();
    }

    private void refreshData() {
        String user = session.getUsername();
        java.util.List<InventoryRepository.InventoryRow> open = repo.getOpenInventories(user);
        pendingAdapter.setItems(open);
        int pendingCount = (open == null ? 0 : open.size());
        if (txtPendingTitle != null) {
            txtPendingTitle.setText("Pending Inventories (" + pendingCount + ")");
        }
        if (txtPendingTitle != null) txtPendingTitle.setVisibility(android.view.View.VISIBLE);
        if (pendingList != null) {
            if (pendingCount == 0) {
                pendingList.setVisibility(android.view.View.GONE);
                android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) pendingList.getLayoutParams();
                if (lp != null) { lp.height = 0; lp.weight = 0f; pendingList.setLayoutParams(lp); }
            } else {
                pendingList.setVisibility(android.view.View.VISIBLE);
                android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) pendingList.getLayoutParams();
                if (lp != null) { lp.height = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT; lp.weight = 0f; pendingList.setLayoutParams(lp); }
            }
        }

        java.util.List<InventoryRepository.InventoryRow> finished = repo.getFinishedInventories(user);
        finishedAdapter.setItems(finished);
        if (txtFinishedTitle != null) {
            txtFinishedTitle.setText("Finished Inventories (" + (finished == null ? 0 : finished.size()) + ")");
        }
    }
}


