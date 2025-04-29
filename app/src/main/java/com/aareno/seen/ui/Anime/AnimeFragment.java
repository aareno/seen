package com.aareno.seen.ui.Anime;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aareno.seen.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AnimeFragment extends Fragment {
    private static final String TAG = "AnimeFragment";

    // Static list to store watching anime
    public static List<Anime> watchingList = new ArrayList<>();
    public static List<Anime> watchedList = new ArrayList<>();
    private ListView listViewWatching;
    private ListView listViewWatched;
    private WatchingAnimeAdapter watchingAdapter;
    private WatchedAnimeAdapter watchedAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_anime, container, false);

        // Find ListView in your fragment_anime.xml
        listViewWatching = view.findViewById(R.id.anime_list_view);
        listViewWatched = view.findViewById(R.id.list_watched_anime);

        // Create and set adapter
        watchingAdapter = new WatchingAnimeAdapter(requireContext(), watchingList, new WatchingAnimeAdapter.OnWatchedButtonClickListener() {
            @Override
            public void onWatchedButtonClick(Anime anime) {
                watchingList.remove(anime);
                watchedList.add(anime);
                addToWatchedList(anime);
                watchingAdapter.notifyDataSetChanged();
                watchedAdapter.notifyDataSetChanged();
                // Implement later: save to database
            }
        });
        listViewWatching.setAdapter(watchingAdapter);

        // Create and Set watched adapter
        watchedAdapter = new WatchedAnimeAdapter(requireContext(), watchedList);
        listViewWatched.setAdapter(watchedAdapter);

        return view;
    }

    // Method to add anime to watching list
    public static void addToWatchingList(Anime anime) {
        try {
            // Check if anime is already in the list to avoid duplicates
            boolean exists = false;
            for (Anime existingAnime : watchingList) {
                if (existingAnime.getId() == anime.getId()) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                watchingList.add(anime);
                Log.d(TAG, "Added anime to watching list: " + anime.getTitleRomaji());
            } else {
                Log.d(TAG, "Anime already exists in watching list: " + anime.getTitleRomaji());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding anime to watching list", e);
        }
    }

        // Method to add anime to watched list
        private void addToWatchedList(Anime anime) {
            try {
                // Check if anime is already in the watched list to avoid duplicates
                boolean exists = watchedList.stream()
                        .anyMatch(existingAnime -> existingAnime.getId() == anime.getId());

                if (!exists) {
                    anime.markAsFinished();
                    Log.d(TAG, "Finished Date: " + anime.getFinishedDate());
                    watchedList.add(anime);
                    Log.d(TAG, "Added anime to watched list: " + anime.getTitleRomaji());
                } else {
                    Log.d(TAG, "Anime already exists in watched list: " + anime.getTitleRomaji());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding anime to watched list", e);
            }
        }
    }