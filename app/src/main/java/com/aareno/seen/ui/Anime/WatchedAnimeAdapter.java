package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.aareno.seen.services.SpotifyService;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WatchedAnimeAdapter extends RecyclerView.Adapter<WatchedAnimeAdapter.ViewHolder> {

    private Context context;
    private List<Anime> watchedAnimeList;
    private SimpleDateFormat dateFormat;
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Anime anime);
    }

    public WatchedAnimeAdapter(Context context, List<Anime> watchedAnimeList, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.watchedAnimeList = watchedAnimeList;
        this.deleteClickListener = deleteClickListener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_watched_anime, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Anime currentAnime = watchedAnimeList.get(position);
        holder.bind(currentAnime);
    }

    @Override
    public int getItemCount() {
        return watchedAnimeList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;
        TextView finishedDateTextView;
        ImageButton deleteButton;
        ImageButton musicButton;

        ViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.anime_cover);
            titleTextView = itemView.findViewById(R.id.anime_title);
            finishedDateTextView = itemView.findViewById(R.id.finished_date);
            deleteButton = itemView.findViewById(R.id.btn_delete);
            musicButton = itemView.findViewById(R.id.btn_music);
        }

        void bind(Anime currentAnime) {
            titleTextView.setText(currentAnime.getTitleEnglish());
            finishedDateTextView.setText(currentAnime.getFinishedDate() != null
                    ? "Finished: " + dateFormat.format(currentAnime.getFinishedDate())
                    : "Finished: Unknown");
            Glide.with(context).load(currentAnime.getCoverImageUrl()).into(coverImageView);

            deleteButton.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(currentAnime);
                }
            });

            // Music button click listener
            musicButton.setOnClickListener(v -> {
                // Inflate the popup layout
                View popupView = LayoutInflater.from(context).inflate(R.layout.popup_music_list, null);

                // Create the popup window
                PopupWindow popupWindow = new PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true
                );

                // Set up the close button
                ImageButton closeButton = popupView.findViewById(R.id.btn_close_popup);
                closeButton.setOnClickListener(view -> popupWindow.dismiss());

                // Initialize ListView and adapter
                ListView musicListView = popupView.findViewById(R.id.music_list_view);
                MusicListAdapter adapter = new MusicListAdapter(context, new ArrayList<>());
                musicListView.setAdapter(adapter);

                // Create SpotifyService instance and search for music
                SpotifyService spotifyService = new SpotifyService(context);
                spotifyService.searchAnimeOST(currentAnime.getTitleEnglish(), new SpotifyService.OnMusicLoadedCallback() {
                    @Override
                    public void onMusicLoaded(List<SpotifyService.Track> tracks) {
                        // Update UI on main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            adapter.updateTracks(tracks);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "Error loading music: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });

                // Show the popup window
                popupWindow.showAsDropDown(v, 0, 0, Gravity.END);
            });
        }
    }
}