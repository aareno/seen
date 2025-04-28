package com.aareno.seen.ui.Anime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.aareno.seen.ui.Anime.Anime;
import com.bumptech.glide.Glide;

import java.util.List;

public class AnimeSearchAdapter extends RecyclerView.Adapter<AnimeSearchAdapter.AnimeViewHolder> {
    private List<Anime> animeList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Anime anime);
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
        return new AnimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimeViewHolder holder, int position) {
        Anime anime = animeList.get(position);
        holder.bind(anime, listener);
    }

    @Override
    public int getItemCount() {
        return animeList.size();
    }

    static class AnimeViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView coverImageView;

        public AnimeViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.anime_title);
            coverImageView = itemView.findViewById(R.id.anime_cover);
        }

        public void bind(final Anime anime, final OnItemClickListener listener) {
            titleTextView.setText(anime.getTitleRomaji());

            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(anime.getCoverImageUrl())
                    .into(coverImageView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(anime);
                }
            });
        }
    }
}