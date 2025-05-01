package com.aareno.seen.ui.KDrama;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.Anime.WatchedAnimeAdapter;
import com.aareno.seen.ui.Anime.WatchingAnimeAdapter;
import com.aareno.seen.ui.UndoAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class KDramaFragment extends Fragment {

    private static final String TAG = "KDramaFragment";
    private RecyclerView recyclerViewWatching;
    private RecyclerView recyclerViewWatched;
    private WatchingAnimeAdapter watchingAdapter;
    private WatchedAnimeAdapter watchedAdapter;
    private KDramaRepository repository;
    private Stack<UndoAction> undoStack = new Stack<>();
    private UndoListener undoListener;
    private EditText searchEditText;
    private ImageButton clearSearchButton;

    private List<Anime> watchingList = new ArrayList<>();
    private List<Anime> watchedList = new ArrayList<>();
    private List<Anime> originalWatchingList = new ArrayList<>();
    private List<Anime> originalWatchedList = new ArrayList<>();

    public interface UndoListener {
        void setUndoEnabled(boolean enabled);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KDramaRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kdrama, container, false);
    }
}