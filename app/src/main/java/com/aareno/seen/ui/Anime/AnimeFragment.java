package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aareno.seen.R;
import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.ui.UndoAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AnimeFragment extends Fragment {
    private static final String TAG = "AnimeFragment";

    // Interface for undo functionality
    public interface UndoListener {
        void setUndoEnabled(boolean enabled);
    }
    private List<Anime> watchingList = new ArrayList<>();
    private List<Anime> watchedList = new ArrayList<>();
    private ListView listViewWatching;
    private ListView listViewWatched;
    private WatchingAnimeAdapter watchingAdapter;
    private WatchedAnimeAdapter watchedAdapter;

    private AnimeRepository repository;
    private Stack<UndoAction> undoStack = new Stack<>();
    private UndoListener undoListener;
    private EditText searchEditText;
    private ImageButton clearSearchButton;

    private List<Anime> originalWatchingList = new ArrayList<>();
    private List<Anime> originalWatchedList = new ArrayList<>();


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof UndoListener) {
            undoListener = (UndoListener) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new AnimeRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_anime, container, false);

        listViewWatching = view.findViewById(R.id.anime_list_view);
        listViewWatched = view.findViewById(R.id.list_watched_anime);

        // search view
        searchEditText = view.findViewById(R.id.search_edit_text);
        clearSearchButton = view.findViewById(R.id.clear_search);
        setupSearch();

        setupWatchingAdapter();
        setupWatchedAdapter();
        loadAnimeLists();

        return view;
    }

    private void setupWatchingAdapter() {
        watchingAdapter = new WatchingAnimeAdapter(
                requireContext(),
                watchingList,
                this::moveAnimeToWatched,  // Watched button click listener
                this::updateAnimeEpisodes  // Episode change listener
        );
        listViewWatching.setAdapter(watchingAdapter);
    }

    private void setupWatchedAdapter() {
        watchedAdapter = new WatchedAnimeAdapter(
                requireContext(),
                watchedList,
                anime -> deleteWatchedAnime(anime)
        );
        listViewWatched.setAdapter(watchedAdapter);
    }

    // Modify your loadAnimeLists() method to store original lists
    private void loadAnimeLists() {
        repository.getWatchingAnime(new AnimeRepository.OnDataLoadedCallback<List<Anime>>() {
            @Override
            public void onDataLoaded(List<Anime> watchingAnime) {
                originalWatchingList.clear();
                originalWatchingList.addAll(watchingAnime);
                watchingList.clear();
                watchingList.addAll(watchingAnime);
                watchingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading watching anime", e);
                showErrorMessage("Failed to load watching anime");
            }
        });

        repository.getWatchedAnime(new AnimeRepository.OnDataLoadedCallback<List<Anime>>() {
            @Override
            public void onDataLoaded(List<Anime> watchedAnime) {
                originalWatchedList.clear();
                originalWatchedList.addAll(watchedAnime);
                watchedList.clear();
                watchedList.addAll(watchedAnime);
                watchedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading watched anime", e);
                showErrorMessage("Failed to load watched anime");
            }
        });
    }

    private void updateAnimeEpisodes(Anime anime) {
        repository.updateAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                // Successfully updated in database
                Log.d(TAG, "Updated episode count for: " + anime.getTitleRomaji());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error updating episode count", e);
                showErrorMessage("Failed to save episode count");
            }
        });
    }

    private void addToUndoStack(UndoAction action) {
        undoStack.push(action);
        if (undoListener != null) {
            undoListener.setUndoEnabled(true);
        }
    }

    public void performUndo() {
        if (!undoStack.isEmpty()) {
            UndoAction action = undoStack.pop();

            switch (action.getType()) {
                case ADD_TO_WATCHING:
                    undoAddToWatching(action.getAnime(), action.getPosition());
                    break;
                case REMOVE_FROM_WATCHED:
                    undoRemoveFromWatched(action.getAnime(), action.getPosition());
                    break;
                case MOVE_TO_WATCHED:
                    undoMoveToWatched(action.getAnime(), action.getPosition());
                    break;
            }

            if (undoListener != null) {
                undoListener.setUndoEnabled(!undoStack.isEmpty());
            }
        }
    }

    public void addAnimeToWatchingList(Anime anime) {
        repository.insertAnime(anime, new AnimeRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchingList.add(anime);
                watchingAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction(
                        UndoAction.ActionType.ADD_TO_WATCHING,
                        anime,
                        watchingList.size() - 1
                ));
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to add anime");
            }
        });
    }

    private void undoAddToWatching(Anime anime, int position) {
        repository.deleteAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingList.remove(position);
                watchingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo add anime");
            }
        });
    }

    private void moveAnimeToWatched(Anime anime) {
        int originalPosition = watchingList.indexOf(anime);
        watchingList.remove(anime);
        anime.markAsFinished();
        watchedList.add(anime);

        repository.updateAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction(
                        UndoAction.ActionType.MOVE_TO_WATCHED,
                        anime,
                        originalPosition
                ));
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to move anime");
            }
        });
    }

    private void undoMoveToWatched(Anime anime, int originalPosition) {
        watchedList.remove(anime);
        anime.setWatching(true);
        anime.setFinishedDate(null);
        watchingList.add(originalPosition, anime);

        repository.updateAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                watchedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo move");
            }
        });
    }

    private void deleteWatchedAnime(Anime anime) {
        int position = watchedList.indexOf(anime);
        watchedList.remove(anime);

        repository.deleteAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction(
                        UndoAction.ActionType.REMOVE_FROM_WATCHED,
                        anime,
                        position
                ));
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to delete anime");
            }
        });
    }

    private void undoRemoveFromWatched(Anime anime, int position) {
        repository.insertAnime(anime, new AnimeRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchedList.add(position, anime);
                watchedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo delete");
            }
        });
    }

    private void showErrorMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
    }

    private void setupSearch() {
        // Add text change listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAnimeLists(s.toString());
                // Show/hide clear button based on text
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add keyboard search action listener
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        // Setup clear button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            resetLists();
            clearSearchButton.setVisibility(View.GONE);
        });
    }

    private void filterAnimeLists(String query) {
        query = query.toLowerCase().trim();

        // Filter watching list
        List<Anime> filteredWatchingList = new ArrayList<>();
        for (Anime anime : originalWatchingList) {
            if (animeMatchesQuery(anime, query)) {
                filteredWatchingList.add(anime);
            }
        }

        // Filter watched list
        List<Anime> filteredWatchedList = new ArrayList<>();
        for (Anime anime : originalWatchedList) {
            if (animeMatchesQuery(anime, query)) {
                filteredWatchedList.add(anime);
            }
        }

        // Update the lists and notify adapters
        watchingList.clear();
        watchingList.addAll(filteredWatchingList);
        watchingAdapter.notifyDataSetChanged();

        watchedList.clear();
        watchedList.addAll(filteredWatchedList);
        watchedAdapter.notifyDataSetChanged();
    }

    private boolean animeMatchesQuery(Anime anime, String query) {
        return anime.getTitleRomaji().toLowerCase().contains(query) ||
                anime.getTitleEnglish().toLowerCase().contains(query);
    }

    private void resetLists() {
        watchingList.clear();
        watchingList.addAll(originalWatchingList);
        watchingAdapter.notifyDataSetChanged();

        watchedList.clear();
        watchedList.addAll(originalWatchedList);
        watchedAdapter.notifyDataSetChanged();
    }
}