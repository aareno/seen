package com.aareno.seen.ui.KDrama;

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
import com.bumptech.glide.Glide;

import java.util.List;

public class WatchingKDramaAdapter extends RecyclerView.Adapter<WatchingKDramaAdapter.ViewHolder> {

    private Context context;
    private List<KDrama> kDramaList;
    private OnWatchedButtonClickListener watchedButtonClickListener;
    private OnEpisodeChangeListener episodeChangeListener;
    private OnDeleteClickListener deleteClickListener;

    public interface OnWatchedButtonClickListener {
        void onWatchedButtonClick(KDrama kdrama);
    }

    public interface OnEpisodeChangeListener {
        void onEpisodeChanged(KDrama kdrama);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(KDrama kdrama);
    }

    public WatchingKDramaAdapter(Context context, List<KDrama> kDramaList,
                                 OnWatchedButtonClickListener listener,
                                 OnEpisodeChangeListener episodeChangeListener, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.kDramaList = kDramaList;
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
        KDrama currentKDrama = kDramaList.get(position);
        holder.bind(currentKDrama);
    }

    @Override
    public int getItemCount() {
        return kDramaList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleEnglishTextView;
        TextView episodesTextView;
        Button incrementButton;
        Button decrementButton;
        Button watchedButton;
        ProgressBar progressBar;
        TextView progressText;

        ImageButton deleteButton;

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
        }

        void bind(KDrama currentKDrama) {
            // Set English title
            titleEnglishTextView.setText(currentKDrama.getTitleEnglish());

            // Set episode count
            episodesTextView.setText("Episode: " + currentKDrama.getWatchedEpisodes());

            // Check episode count here to maintain button state
            if (currentKDrama.getWatchedEpisodes() == currentKDrama.getEpisodeCount()) {
                incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.green)));
                incrementButton.setText("✔ ");
            } else {
                incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.blue)));
                incrementButton.setText("+");
            }

            // Load cover image
            if (!currentKDrama.getCoverImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(currentKDrama.getCoverImageUrl())
                        .into(coverImageView);
            }

            // Click listener remains the same
            incrementButton.setOnClickListener(v -> {
                if (currentKDrama.getWatchedEpisodes() < currentKDrama.getEpisodeCount()) {
                    if (currentKDrama.getWatchedEpisodes() == currentKDrama.getEpisodeCount() - 1) {
                        incrementButton.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.green)));
                        incrementButton.setText("✔ ");
                    }

                    currentKDrama.incrementEpisodes();
                    episodeChangeListener.onEpisodeChanged(currentKDrama);
                    notifyItemChanged(getAdapterPosition());
                } else {
                    watchedButtonClickListener.onWatchedButtonClick(currentKDrama);
                }
            });

            // Decrement episode count
            decrementButton.setOnClickListener(v -> {
                int currentEpisodes = currentKDrama.getWatchedEpisodes();
                if (currentEpisodes > 0) {
                    currentKDrama.setWatchedEpisodes(currentEpisodes - 1);
                    notifyItemChanged(getAdapterPosition());
                    episodeChangeListener.onEpisodeChanged(currentKDrama);
                }
            });

            // delete button
            // Add delete button click listener
            deleteButton.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(currentKDrama);
                }
            });

            progressBar.setMax(currentKDrama.getEpisodeCount());
            progressBar.setProgress(currentKDrama.getWatchedEpisodes());
            progressText.setText(currentKDrama.getWatchedEpisodes() + "/" + currentKDrama.getEpisodeCount());

        }
    }
}
