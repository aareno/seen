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
import java.util.List;

public class AnimeFragment extends Fragment {
    private static final String TAG = "AnimeFragment";

    // Static list to store watching anime
    public static List<Anime> watchingList = new ArrayList<>();
    private ListView listViewWatching;
    private WatchingAnimeAdapter watchingAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_anime, container, false);

        // Find ListView in your fragment_anime.xml
        listViewWatching = view.findViewById(R.id.anime_list_view);

        // Create and set adapter
        watchingAdapter = new WatchingAnimeAdapter(requireContext(), watchingList);
        listViewWatching.setAdapter(watchingAdapter);

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
}