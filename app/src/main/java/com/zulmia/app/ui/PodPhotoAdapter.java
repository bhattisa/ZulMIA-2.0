package com.zulmia.app.ui;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zulmia.app.R;

import java.util.ArrayList;
import java.util.List;

public class PodPhotoAdapter extends RecyclerView.Adapter<PodPhotoAdapter.PhotoVH> {
    public interface OnRemove {
        void onRemove(int position);
    }

    private final List<Uri> items = new ArrayList<>();
    private final OnRemove removeListener;

    public PodPhotoAdapter(OnRemove onRemove) {
        this.removeListener = onRemove;
    }

    public void setItems(List<Uri> uris) {
        items.clear();
        if (uris != null) items.addAll(uris);
        notifyDataSetChanged();
    }

    public List<Uri> getItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public PhotoVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pod_photo, parent, false);
        return new PhotoVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoVH holder, int position) {
        Uri uri = items.get(position);
        Context ctx = holder.itemView.getContext();
        try {
            android.graphics.Bitmap bm = decodeForThumb(ctx, uri, 240, 240);
            holder.imgThumb.setImageBitmap(bm);
        } catch (Exception e) {
            holder.imgThumb.setImageResource(android.R.color.darker_gray);
        }
        holder.btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (removeListener != null) removeListener.onRemove(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PhotoVH extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        ImageButton btnRemove;
        PhotoVH(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }

    private static android.graphics.Bitmap decodeForThumb(Context ctx, Uri uri, int reqW, int reqH) throws Exception {
        android.content.ContentResolver resolver = ctx.getContentResolver();
        android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        java.io.InputStream is1 = resolver.openInputStream(uri);
        try {
            android.graphics.BitmapFactory.decodeStream(is1, null, opts);
        } finally {
            if (is1 != null) is1.close();
        }
        int inSample = 1;
        int w = opts.outWidth;
        int h = opts.outHeight;
        while (w / inSample > reqW || h / inSample > reqH) {
            inSample *= 2;
        }
        android.graphics.BitmapFactory.Options opts2 = new android.graphics.BitmapFactory.Options();
        opts2.inSampleSize = inSample;
        java.io.InputStream is2 = resolver.openInputStream(uri);
        try {
            return android.graphics.BitmapFactory.decodeStream(is2, null, opts2);
        } finally {
            if (is2 != null) is2.close();
        }
    }
}


