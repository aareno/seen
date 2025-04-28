package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aareno.seen.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class WatchingAnimeAdapter extends ArrayAdapter<Anime> {
    private Context context;
    private List<Anime> animeList;

    public WatchingAnimeAdapter(@NonNull Context context, List<Anime> animeList) {
        super(context, 0, animeList);
        this.context = context;
        this.animeList = animeList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_anime_watching, parent, false);
        }

        Anime currentAnime = animeList.get(position);

        // Find views
        ImageView coverImageView = listItem.findViewById(R.id.anime_cover);
        TextView titleTextView = listItem.findViewById(R.id.anime_title);
        TextView episodesTextView = listItem.findViewById(R.id.tv_episode_count);
        Button incrementButton = listItem.findViewById(R.id.btn_plus);
        Button decrementButton = listItem.findViewById(R.id.btn_minus);

        // Set anime title
        titleTextView.setText(currentAnime.getTitleRomaji());

        // Set watched episodes
        episodesTextView.setText("Watched: " + currentAnime.getWatchedEpisodes());

        // Load cover image
        Glide.with(context)
                .load(currentAnime.getCoverImageUrl())
                .into(coverImageView);

        // Increment button
        incrementButton.setOnClickListener(v -> {
            currentAnime.incrementEpisodes();
            notifyDataSetChanged();
        });

        // Decrement button
        decrementButton.setOnClickListener(v -> {
            currentAnime.decrementEpisodes();
            notifyDataSetChanged();
        });

        return listItem;
    }
}