package com.aareno.seen.ui.Anime;

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

public class AnimeSearchAdapter extends RecyclerView.Adapter<AnimeSearchAdapter.AnimeViewHolder> {
    private List<Anime> animeList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAddToWatchingClick(Anime anime);
    }

    public AnimeSearchAdapter(List<Anime> animeList, OnItemClickListener listener) {
        this.animeList = animeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AnimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anime_search, parent, false);
        return new AnimeViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimeViewHolder holder, int position) {
        Anime anime = animeList.get(position);
        holder.bind(anime);
    }

    @Override
    public int getItemCount() {
        return animeList.size();
    }

    static class AnimeViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView coverImageView;
        Button addToWatchingButton;
        private OnItemClickListener listener;
        private Anime currentAnime;

        public AnimeViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            this.listener = listener;

            titleTextView = itemView.findViewById(R.id.anime_title);
            coverImageView = itemView.findViewById(R.id.anime_cover);
            addToWatchingButton = itemView.findViewById(R.id.btn_add);

            // Set click listener for add to watching button
            addToWatchingButton.setOnClickListener(v -> {
                if (listener != null && currentAnime != null) {
                    listener.onAddToWatchingClick(currentAnime);
                }
            });
        }

        public void bind(Anime anime) {
            currentAnime = anime;
            titleTextView.setText(anime.getTitleRomaji());

            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(anime.getCoverImageUrl())
                    .into(coverImageView);
        }
    }
}