package com.aareno.seen.ui.TvMovies;


import android.content.Context;
import android.content.res.ColorStateList;
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
import com.aareno.seen.ui.Anime.Anime;
import com.bumptech.glide.Glide;

import java.util.Date;
import java.util.List;
public class WatchingShowAdapter extends RecyclerView.Adapter<WatchingShowAdapter.ViewHolder> {

    private Context context;
    private List<Show> showList;
    private OnWatchedButtonClickListener watchedButtonClickListener;
    private OnEpisodeChangeListener episodeChangeListener;
    private OnDeleteClickListener deleteClickListener;

    public interface OnWatchedButtonClickListener {
        void onWatchedButtonClick(Show show);
    }

    public interface OnEpisodeChangeListener {
        void onEpisodeChanged(Show show);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Show show);
    }

    public WatchingShowAdapter(Context context, List<Show> showList,
                                 OnWatchedButtonClickListener listener,
                               OnEpisodeChangeListener episodeChangeListener, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.showList = showList;
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
    public void onBindViewHolder(@NonNull WatchingShowAdapter.ViewHolder holder, int position) {
        Show currentShow = showList.get(position);
        holder.bind(currentShow);
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleEnglishTextView;
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
            titleEnglishTextView = itemView.findViewById(R.id.anime_title);
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

        void bind(Show currentShow) {
            // Set English title
            titleEnglishTextView.setText(currentShow.getTitleEnglish());

            // Set episode count
            episodesTextView.setText("Episode: " + currentShow.getWatchedEpisodes());

            // Check episode count here to maintain button state
            if (currentShow.getWatchedEpisodes() == currentShow.getEpisodeCount()) {
                incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.green)));
                incrementButton.setText("✔ ");
            } else {
                incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.blue)));
                incrementButton.setText("+");
            }

            // Load cover image
            if (!currentShow.getCoverImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(currentShow.getCoverImageUrl())
                        .into(coverImageView);
            }

            // Click listener remains the same
            incrementButton.setOnClickListener(v -> {
                if (currentShow.getWatchedEpisodes() < currentShow.getEpisodeCount()) {
                    if (currentShow.getWatchedEpisodes() == currentShow.getEpisodeCount() - 1) {
                        incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.green)));
                        incrementButton.setText("✔ ");
                    }

                    currentShow.incrementEpisodes();
                    episodeChangeListener.onEpisodeChanged(currentShow);
                    notifyItemChanged(getAdapterPosition());
                } else {
                    watchedButtonClickListener.onWatchedButtonClick(currentShow);
                }
            });

            // Decrement episode count
            decrementButton.setOnClickListener(v -> {
                int currentEpisodes = currentShow.getWatchedEpisodes();
                if (currentEpisodes > 0) {
                    currentShow.setWatchedEpisodes(currentEpisodes - 1);
                    notifyItemChanged(getAdapterPosition());
                    episodeChangeListener.onEpisodeChanged(currentShow);
                }
            });

            // delete button
            // Add delete button click listener
            deleteButton.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(currentShow);
                }
            });

            progressBar.setMax(currentShow.getEpisodeCount());
            progressBar.setProgress(currentShow.getWatchedEpisodes());
            progressText.setText(currentShow.getWatchedEpisodes() + "/" + currentShow.getEpisodeCount());


            updateDayIndicators(currentShow);
        }

        private void updateDayIndicators(Show Show) {
            // First, reset all indicators to inactive state
            for (TextView dayIndicator : dayIndicators) {
                dayIndicator.setSelected(false);
                dayIndicator.setAlpha(0.5f);
            }
            // Check if the anime is past its end date
            Date currentDate = new Date();
            if (Show.getEndDate() != null && currentDate.after(Show.getEndDate())) {
                // Switch status to FINISHED if the end date has passed
                Show.setAiringStatus(Anime.AiringStatus.FINISHED);
            }

            // Now update indicators based on the current airing status
            if (Show.getAiringDays() != null && !Show.getAiringDays().isEmpty()) {
                for (Integer day : Show.getAiringDays()) {
                    // Convert day to index (assuming 1 = Monday, 7 = Sunday)
                    int index = day - 1;
                    if (index >= 0 && index < dayIndicators.length) {
                        TextView indicator = dayIndicators[index];

                        switch (Show.getAiringStatus()) {
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