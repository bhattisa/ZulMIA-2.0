package com.zulmia.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zulmia.app.R;
import com.zulmia.app.data.InventoryRepository;

import java.util.ArrayList;
import java.util.List;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ScanViewHolder> {
    private final List<InventoryRepository.AggregatedScanRow> items = new ArrayList<>();
    public interface OnRowActionListener {
        void onIncrease(InventoryRepository.AggregatedScanRow row);
        void onDecrease(InventoryRepository.AggregatedScanRow row);
        default void onEditRow(InventoryRepository.AggregatedScanRow row) { }
    }
    private final OnRowActionListener listener;
    public ScanAdapter(OnRowActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<InventoryRepository.AggregatedScanRow> rows) {
        items.clear();
        if (rows != null) items.addAll(rows);
        notifyDataSetChanged();
    }

    public InventoryRepository.AggregatedScanRow getItem(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan_card, parent, false);
        return new ScanViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
        InventoryRepository.AggregatedScanRow row = items.get(position);
        holder.txtCode.setText(row.code);
        holder.txtQty.setText(String.valueOf(row.qty));
        String loc = (row.location == null ? "" : row.location);
        String sh = (row.shelf == null ? "" : row.shelf);
        holder.txtLocation.setText(loc);
        holder.txtShelf.setText(sh);
        holder.btnPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onIncrease(row);
            }
        });
        holder.btnMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onDecrease(row);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onEditRow(row);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ScanViewHolder extends RecyclerView.ViewHolder {
        TextView txtCode;
        TextView txtQty;
        TextView txtLocation;
        TextView txtShelf;
        ImageButton btnPlus;
        ImageButton btnMinus;

        ScanViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCode = itemView.findViewById(R.id.txtCode);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtShelf = itemView.findViewById(R.id.txtShelf);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}


