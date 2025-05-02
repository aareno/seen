package com.aareno.seen.ui.KDrama;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class KDramaSearchAdapter extends RecyclerView.Adapter<KDramaSearchAdapter.KDramaViewHolder> {
    private List<KDrama> kDramaList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAddToWatchingClick(KDrama kdrama);
    }

    public KDramaSearchAdapter(List<KDrama> kDramaList, OnItemClickListener listener) {
        this.kDramaList = kDramaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public KDramaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anime_search, parent, false);
        return new KDramaViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull KDramaViewHolder holder, int position) {
        KDrama kdrama = kDramaList.get(position);
        holder.bind(kdrama);
    }

    @Override
    public int getItemCount() {
        return kDramaList.size();
    }

    static class KDramaViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView coverImageView;
        Button addToWatchingButton;
        private OnItemClickListener listener;
        private KDrama currentKDrama;

        public KDramaViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            this.listener = listener;

            titleTextView = itemView.findViewById(R.id.anime_title);
            coverImageView = itemView.findViewById(R.id.anime_cover);
            addToWatchingButton = itemView.findViewById(R.id.btn_add);

            // Set click listener for add to watching button
            addToWatchingButton.setOnClickListener(v -> {
                if (listener != null && currentKDrama != null) {
                    listener.onAddToWatchingClick(currentKDrama);
                }
            });
        }

        public void bind(KDrama kdrama) {
            currentKDrama = kdrama;
            titleTextView.setText(kdrama.getTitleEnglish());
            // Load image with Glide
            if (kdrama.getCoverImageUrl() != null) {
                Glide.with(itemView.getContext())
                        .load(kdrama.getCoverImageUrl())
                        .into(coverImageView);
            }
            }
        }
    }
