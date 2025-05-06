package com.aareno.seen.ui.TvMovies;

import android.app.AlertDialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.aareno.seen.ui.KDrama.KDrama;
import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShowSearchAdapter extends RecyclerView.Adapter<ShowSearchAdapter.ShowViewHolder> {
    private List<Show> showList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onAddToListClick(Show show, String listType); // listType = "Watching" or "Watched"
    }

    public ShowSearchAdapter(List<Show> showList, ShowSearchAdapter.OnItemClickListener listener) {
        this.showList = showList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anime_search, parent, false);
        return new ShowViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ShowViewHolder holder, int position) {
        Show kdrama = showList.get(position);
        holder.bind(kdrama);
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    static class ShowViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        ImageView coverImageView;
        Button addToWatchingButton;
        private OnItemClickListener listener;
        private Show currentKDrama;

        public ShowViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            this.listener = listener;

            titleTextView = itemView.findViewById(R.id.anime_title);
            coverImageView = itemView.findViewById(R.id.anime_cover);
            addToWatchingButton = itemView.findViewById(R.id.btn_add);

            // Set click listener for add to watching button
            addToWatchingButton.setOnClickListener(v -> {
                if (listener != null && currentKDrama != null) {
                    showAddDialog(v.getContext(), currentKDrama);
                }
            });
        }

        public void bind(Show kdrama) {
            currentKDrama = kdrama;
            titleTextView.setText(kdrama.getTitleEnglish());
            // Load image with Glide
            if (kdrama.getCoverImageUrl() != null) {
                Glide.with(itemView.getContext())
                        .load(kdrama.getCoverImageUrl())
                        .into(coverImageView);
            }
        }

        private void showAddDialog(Context context, Show kdrama) {
            final String[] options = {"Watching", "Watched"};
            final int[] selectedIndex = {0}; // default to "Watching"

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Add to List")
                    .setSingleChoiceItems(options, 0, (dialog, which) -> selectedIndex[0] = which)
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        String chosen = options[selectedIndex[0]];
                        listener.onAddToListClick(kdrama, chosen);
                    })
                    .setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(d -> {
                int colorOnPrimary = resolveThemeAttr(context, com.google.android.material.R.attr.colorOnPrimary);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(colorOnPrimary);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(colorOnPrimary);
            });

            dialog.show();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date today = sdf.parse(sdf.format(new Date()));
                kdrama.setFinishedDate(today);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        private int resolveThemeAttr(Context context, int attr) {
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(attr, typedValue, true);
            return typedValue.data;
        }


    }
}

