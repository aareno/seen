package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.bumptech.glide.Glide;

import java.util.List;

public class WatchingAnimeAdapter extends RecyclerView.Adapter<WatchingAnimeAdapter.ViewHolder> {

    private Context context;
    private List<Anime> animeList;
    private OnWatchedButtonClickListener watchedButtonClickListener;
    private OnEpisodeChangeListener episodeChangeListener;

    public interface OnWatchedButtonClickListener {
        void onWatchedButtonClick(Anime anime);
    }

    public interface OnEpisodeChangeListener {
        void onEpisodeChanged(Anime anime);
    }

    public WatchingAnimeAdapter(Context context, List<Anime> animeList, OnWatchedButtonClickListener listener, OnEpisodeChangeListener episodeChangeListener) {
        this.context = context;
        this.animeList = animeList;
        this.watchedButtonClickListener = listener;
        this.episodeChangeListener = episodeChangeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_anime_watching, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Anime currentAnime = animeList.get(position);
        holder.bind(currentAnime);
    }

    @Override
    public int getItemCount() {
        return animeList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;
        TextView episodesTextView;
        Button incrementButton;
        Button decrementButton;
        Button watchedButton;

        ViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.anime_cover);
            titleTextView = itemView.findViewById(R.id.anime_title);
            episodesTextView = itemView.findViewById(R.id.tv_episode_count);
            incrementButton = itemView.findViewById(R.id.btn_plus);
            decrementButton = itemView.findViewById(R.id.btn_minus);
            watchedButton = itemView.findViewById(R.id.btn_watched);
        }

        void bind(Anime currentAnime) {
            titleTextView.setText(currentAnime.getTitleEnglish());
            episodesTextView.setText("Episode: " + currentAnime.getWatchedEpisodes());
            Glide.with(context).load(currentAnime.getCoverImageUrl()).into(coverImageView);

            incrementButton.setOnClickListener(v -> {
                currentAnime.incrementEpisodes();
                notifyItemChanged(getAdapterPosition());
                episodeChangeListener.onEpisodeChanged(currentAnime);
            });

            decrementButton.setOnClickListener(v -> {
                currentAnime.decrementEpisodes();
                notifyItemChanged(getAdapterPosition());
                episodeChangeListener.onEpisodeChanged(currentAnime);
            });

            watchedButton.setOnClickListener(v -> {
                if (watchedButtonClickListener != null) {
                    watchedButtonClickListener.onWatchedButtonClick(currentAnime);
                }
            });
        }
    }
}