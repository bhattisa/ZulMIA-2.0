package com.zulmia.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zulmia.app.R;
import com.zulmia.app.data.InventoryRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FinishedInventoryAdapter extends RecyclerView.Adapter<FinishedInventoryAdapter.InvVH> {
    public interface OnInventoryClick {
        void onClick(InventoryRepository.InventoryRow row);
    }

    private final List<InventoryRepository.InventoryRow> items = new ArrayList<>();
    private final OnInventoryClick listener;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    public FinishedInventoryAdapter(OnInventoryClick listener) {
        this.listener = listener;
    }

    public void setItems(List<InventoryRepository.InventoryRow> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public InventoryRepository.InventoryRow getItem(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    @NonNull
    @Override
    public InvVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_finished_inventory_card, parent, false);
        return new InvVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull InvVH holder, int position) {
        InventoryRepository.InventoryRow row = items.get(position);
        int total = new InventoryRepository(holder.itemView.getContext()).getInventoryTotalQuantity(row.id);
        holder.txtInvName.setText(row.name + " (" + total + ")");
        holder.txtInvDate.setText(df.format(new Date(row.createdAtMs)));
        String loc = (row.defaultLocation == null || row.defaultLocation.isEmpty()) ? "Location: —" : ("Location: " + row.defaultLocation);
        holder.txtInvLocation.setText(loc);
        holder.btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // View CSV in external viewer
                String name = row.csvName;
                if (name == null || name.isEmpty()) {
                    android.widget.Toast.makeText(v.getContext(), "No CSV file for this inventory", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    android.net.Uri uri = CsvUtils.findCsvInDownloads(v.getContext(), name);
                    if (uri == null) {
                        android.widget.Toast.makeText(v.getContext(), "CSV not found", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.content.Intent viewCsv = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    viewCsv.setDataAndType(uri, "text/csv");
                    viewCsv.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        v.getContext().startActivity(android.content.Intent.createChooser(viewCsv, "View CSV"));
                    } catch (Exception e1) {
                        android.content.Intent viewTxt = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                        viewTxt.setDataAndType(uri, "text/plain");
                        viewTxt.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try {
                            v.getContext().startActivity(android.content.Intent.createChooser(viewTxt, "View File"));
                        } catch (Exception e2) {
                            android.widget.Toast.makeText(v.getContext(), "No app to view file", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    android.widget.Toast.makeText(v.getContext(), "Unable to open", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onClick(row);
            }
        });
        holder.btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareCsv(v, row);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class InvVH extends RecyclerView.ViewHolder {
        TextView txtInvName;
        TextView txtInvDate;
        TextView txtInvLocation;
        android.widget.ImageButton btnEdit;
        android.widget.ImageButton btnOpen;
        android.widget.ImageButton btnShare;

        InvVH(@NonNull View itemView) {
            super(itemView);
            txtInvName = itemView.findViewById(R.id.txtInvName);
            txtInvDate = itemView.findViewById(R.id.txtInvDate);
            txtInvLocation = itemView.findViewById(R.id.txtInvLocation);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnOpen = itemView.findViewById(R.id.btnOpen);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }

    private void shareCsv(View v, InventoryRepository.InventoryRow row) {
        String name = row.csvName;
        if (name == null || name.isEmpty()) {
            android.widget.Toast.makeText(v.getContext(), "No CSV file for this inventory", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            android.net.Uri uri = CsvUtils.findCsvInDownloads(v.getContext(), name);
            if (uri == null) {
                android.widget.Toast.makeText(v.getContext(), "CSV not found", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            v.getContext().startActivity(android.content.Intent.createChooser(share, "Share inventory CSV"));
        } catch (Exception e) {
            android.widget.Toast.makeText(v.getContext(), "Unable to share", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}


