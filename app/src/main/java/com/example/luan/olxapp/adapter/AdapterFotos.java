package com.example.luan.olxapp.adapter;

import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adapter do slide de fotos (usado no ViewPager2 da tela de detalhes).
 */
public class AdapterFotos extends RecyclerView.Adapter<AdapterFotos.FotoViewHolder> {

    private final List<String> urls;

    public AdapterFotos(List<String> urls) {
        this.urls = urls;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new FotoViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        Picasso.get().load(urls.get(position)).into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        FotoViewHolder(@NonNull ImageView itemView) {
            super(itemView);
            this.imageView = itemView;
        }
    }
}
