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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WatchedAnimeAdapter extends ArrayAdapter<Anime> {
    private Context context;
    private List<Anime> watchedAnimeList;
    private SimpleDateFormat dateFormat;
    private OnDeleteClickListener deleteClickListener;

    // Interface for delete button click
    public interface OnDeleteClickListener {
        void onDeleteClick(Anime anime);
    }

    public WatchedAnimeAdapter(@NonNull Context context,
                               List<Anime> watchedAnimeList,
                               OnDeleteClickListener deleteClickListener) {
        super(context, 0, watchedAnimeList);
        this.context = context;
        this.watchedAnimeList = watchedAnimeList;
        this.deleteClickListener = deleteClickListener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if (listItem == null) {
            listItem = LayoutInflater.from(context).inflate(R.layout.item_watched_anime, parent, false);
        }

        Anime currentAnime = watchedAnimeList.get(position);

        // Find views
        ImageView coverImageView = listItem.findViewById(R.id.anime_cover);
        TextView titleTextView = listItem.findViewById(R.id.anime_title);
        TextView finishedDateTextView = listItem.findViewById(R.id.finished_date);
        Button deleteButton = listItem.findViewById(R.id.btn_delete); // Add this button to your layout

        // Set anime title
        titleTextView.setText(currentAnime.getTitleRomaji());

        // Set finished date
        if (currentAnime.getFinishedDate() != null) {
            finishedDateTextView.setText("Finished: " +
                    dateFormat.format(currentAnime.getFinishedDate()));
        } else {
            finishedDateTextView.setText("Finished: Unknown");
        }

        // Load cover image
        Glide.with(context)
                .load(currentAnime.getCoverImageUrl())
                .into(coverImageView);

        // Set up delete button
        deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(currentAnime);
            }
        });

        return listItem;
    }
}