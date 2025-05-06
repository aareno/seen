package com.aareno.seen.ui.Anime;

import android.app.AlertDialog;
import android.content.Context;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnimeSearchAdapter extends RecyclerView.Adapter<AnimeSearchAdapter.AnimeViewHolder> {
    private List<Anime> animeList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAddToListClick(Anime anime, String listType); // listType = "Watching" or "Watched"
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
                    showAddDialog(v.getContext(), currentAnime);
                }
            });

        }

        public void bind(Anime anime) {
            currentAnime = anime;
            titleTextView.setText(
                    (anime.getTitleEnglish() != null && !anime.getTitleEnglish().trim().isEmpty() && !anime.getTitleEnglish().equals("null")) ?
                            anime.getTitleEnglish() :
                            (anime.getTitleRomaji() != null && !anime.getTitleRomaji().equals("null") ? anime.getTitleRomaji() : "No Title Available")
            );

            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(anime.getCoverImageUrl())
                    .into(coverImageView);
        }

        private void showAddDialog(Context context, Anime anime) {
            final String[] options = {"Watching", "Watched"};
            final int[] selectedIndex = {0}; // default to "Watching"

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Add to List")
                    .setSingleChoiceItems(options, 0, (dialog, which) -> selectedIndex[0] = which)
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        String chosen = options[selectedIndex[0]];
                        listener.onAddToListClick(anime, chosen);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date today = sdf.parse(sdf.format(new Date()));
                anime.setFinishedDate(today);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}