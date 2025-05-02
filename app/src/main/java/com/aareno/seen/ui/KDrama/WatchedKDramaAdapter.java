package com.aareno.seen.ui.KDrama;

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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WatchedKDramaAdapter extends RecyclerView.Adapter<WatchedKDramaAdapter.ViewHolder> {

    private Context context;
    private List<KDrama> watchedKDramaList;
    private SimpleDateFormat dateFormat;
    private WatchedKDramaAdapter.OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(KDrama anime);
    }

    public WatchedKDramaAdapter(Context context, List<KDrama> watchedKDramaList, WatchedKDramaAdapter.OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.watchedKDramaList = watchedKDramaList;
        this.deleteClickListener = deleteClickListener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public WatchedKDramaAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_watched_anime, parent, false);
        return new WatchedKDramaAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WatchedKDramaAdapter.ViewHolder holder, int position) {
        KDrama currentKDrama = watchedKDramaList.get(position);
        holder.bind(currentKDrama);
    }

    @Override
    public int getItemCount() {
        return watchedKDramaList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImageView;
        TextView titleTextView;
        TextView finishedDateTextView;
        Button deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            coverImageView = itemView.findViewById(R.id.anime_cover);
            titleTextView = itemView.findViewById(R.id.anime_title);
            finishedDateTextView = itemView.findViewById(R.id.finished_date);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }

        void bind(KDrama currentKDrama) {
            titleTextView.setText(currentKDrama.getTitleEnglish());
            finishedDateTextView.setText(currentKDrama.getFinishedDate() != null
                    ? "Finished: " + dateFormat.format(currentKDrama.getFinishedDate())
                    : "Finished: Unknown");
            Glide.with(context).load(currentKDrama.getCoverImageUrl()).into(coverImageView);

            deleteButton.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(currentKDrama);
                }
            });
        }
    }
}
