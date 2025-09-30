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

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InvVH> {
    public interface OnInventoryClick {
        void onClick(InventoryRepository.InventoryRow row);
    }

    private final List<InventoryRepository.InventoryRow> items = new ArrayList<>();
    private final OnInventoryClick listener;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    public InventoryAdapter(OnInventoryClick listener) {
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory_card, parent, false);
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
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onClick(row);
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

        InvVH(@NonNull View itemView) {
            super(itemView);
            txtInvName = itemView.findViewById(R.id.txtInvName);
            txtInvDate = itemView.findViewById(R.id.txtInvDate);
            txtInvLocation = itemView.findViewById(R.id.txtInvLocation);
        }
    }
}


