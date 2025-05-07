package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.aareno.seen.ui.KDrama.KDrama;
import com.aareno.seen.ui.KDrama.WatchingKDramaAdapter;
import com.bumptech.glide.Glide;

import java.util.Date;
import java.util.List;

public class WatchingAnimeAdapter extends RecyclerView.Adapter<WatchingAnimeAdapter.ViewHolder> {

    private Context context;
    private List<Anime> animeList;
    private OnWatchedButtonClickListener watchedButtonClickListener;
    private OnEpisodeChangeListener episodeChangeListener;
    private OnDeleteClickListener deleteClickListener;


    public interface OnWatchedButtonClickListener {
        void onWatchedButtonClick(Anime anime);
    }

    public interface OnEpisodeChangeListener {
        void onEpisodeChanged(Anime anime);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(Anime anime);
    }
    public WatchingAnimeAdapter(Context context, List<Anime> animeList, OnWatchedButtonClickListener listener, OnEpisodeChangeListener episodeChangeListener, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.animeList = animeList;
        this.watchedButtonClickListener = listener;
        this.episodeChangeListener = episodeChangeListener;
        this.deleteClickListener = deleteClickListener;
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
        ProgressBar progressBar;
        TextView progressText;
        ImageButton deleteButton;
        TextView[] dayIndicators;

        ViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.anime_cover);
            titleTextView = itemView.findViewById(R.id.anime_title);
            episodesTextView = itemView.findViewById(R.id.tv_episode_count);
            incrementButton = itemView.findViewById(R.id.btn_plus);
            decrementButton = itemView.findViewById(R.id.btn_minus);
            progressBar = itemView.findViewById(R.id.progress_bar);
            progressText = itemView.findViewById(R.id.tv_progress_text);
            deleteButton = itemView.findViewById(R.id.btn_delete);

            // Initialize day indicators array
            dayIndicators = new TextView[]{
                    itemView.findViewById(R.id.day_mon),
                    itemView.findViewById(R.id.day_tue),
                    itemView.findViewById(R.id.day_wed),
                    itemView.findViewById(R.id.day_thu),
                    itemView.findViewById(R.id.day_fri),
                    itemView.findViewById(R.id.day_sat),
                    itemView.findViewById(R.id.day_sun)
            };
        }

        void bind(Anime currentAnime) {
            Log.d("WatchingAnimeAdapter", "Binding anime: " + currentAnime.getTitleEnglish());
            titleTextView.setText(!"null".equals(currentAnime.getTitleEnglish()) ? currentAnime.getTitleEnglish() : currentAnime.getTitleRomaji());
            episodesTextView.setText("Episode: " + currentAnime.getWatchedEpisodes());
            Glide.with(context).load(currentAnime.getCoverImageUrl()).into(coverImageView);

            // Check episode count here to maintain button state
            if (currentAnime.getWatchedEpisodes() == currentAnime.getEpisodeCount()) {
                incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.green)));
                incrementButton.setText("✔ ");
            } else {
                incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.blue)));
                incrementButton.setText("+");
            }

            // Click listener remains the same
            incrementButton.setOnClickListener(v -> {
            if (currentAnime.getWatchedEpisodes() < currentAnime.getEpisodeCount()) {
                if (currentAnime.getWatchedEpisodes() == currentAnime.getEpisodeCount() - 1) {
                    incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.green)));
                    incrementButton.setText("✔ ");
                }

                currentAnime.incrementEpisodes();
                episodeChangeListener.onEpisodeChanged(currentAnime);
                notifyItemChanged(getAdapterPosition());
            } else {
                watchedButtonClickListener.onWatchedButtonClick(currentAnime);
            }
        });

            decrementButton.setOnClickListener(v -> {
                currentAnime.decrementEpisodes();
                notifyItemChanged(getAdapterPosition());
                episodeChangeListener.onEpisodeChanged(currentAnime);
            });

            // delete button
            deleteButton.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(currentAnime);
                }
            });

            progressBar.setMax(currentAnime.getEpisodeCount());
            progressBar.setProgress(currentAnime.getWatchedEpisodes());
            progressText.setText(currentAnime.getWatchedEpisodes() + "/" + currentAnime.getEpisodeCount());

            updateDayIndicators(currentAnime);
        }

        private void updateDayIndicators(Anime anime) {
            // First, reset all indicators to inactive state
            for (TextView dayIndicator : dayIndicators) {
                dayIndicator.setSelected(false);
                dayIndicator.setAlpha(0.5f);
            }
            // Check if the anime is past its end date
            Date currentDate = new Date();
            if (anime.getEndDate() != null && currentDate.after(anime.getEndDate())) {
                // Switch status to FINISHED if the end date has passed
                anime.setAiringStatus(Anime.AiringStatus.FINISHED);
            }

            // Now update indicators based on the current airing status
            if (anime.getAiringDays() != null && !anime.getAiringDays().isEmpty()) {
                for (Integer day : anime.getAiringDays()) {
                    // Convert day to index (assuming 1 = Monday, 7 = Sunday)
                    int index = day - 1;
                    if (index >= 0 && index < dayIndicators.length) {
                        TextView indicator = dayIndicators[index];

                        switch (anime.getAiringStatus()) {
                            case ONGOING:
                                indicator.setSelected(true);
                                indicator.setAlpha(1.0f);
                                break;
                            case FINISHED:
                                // Ensure all indicators remain inactive for finished anime
                                indicator.setSelected(false);
                                indicator.setAlpha(0.3f);
                                break;
                            case NOT_STARTED:
                                indicator.setSelected(true);
                                indicator.setAlpha(0.7f);
                                break;
                        }
                    }
                }
            }
        }

    }
}