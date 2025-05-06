package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.aareno.seen.data.WorkScheduler;
import com.aareno.seen.ui.KDrama.KDrama;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AnimeFragment extends Fragment {
    private static final String TAG = "AnimeFragment";

    // Interface for undo functionality
    public interface UndoListener {
        void setUndoEnabled(boolean enabled);
        void updateUndoButton(boolean hasItems);
    }
    private List<Anime> watchingList = new ArrayList<>();
    private List<Anime> watchedList = new ArrayList<>();
    private RecyclerView recyclerViewWatching;
    private RecyclerView recyclerViewWatched;
    private WatchingAnimeAdapter watchingAdapter;
    private WatchedAnimeAdapter watchedAdapter;
    private AnimeRepository repository;
    private Stack<UndoAction_anime> undoStack = new Stack<>();
    private UndoListener undoListener;
    private EditText searchEditText;
    private TextView watchingCountText;
    private TextView watchedCountText;
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

        undoListener.updateUndoButton(!undoStack.isEmpty());

        recyclerViewWatching = view.findViewById(R.id.recycler_view_watching);
        recyclerViewWatched = view.findViewById(R.id.recycler_view_watched);

        recyclerViewWatching.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewWatched.setLayoutManager(new LinearLayoutManager(requireContext()));

        searchEditText = view.findViewById(R.id.search_edit_text);
        clearSearchButton = view.findViewById(R.id.clear_search);
        watchingCountText = view.findViewById(R.id.watching_count);
        watchedCountText = view.findViewById(R.id.watched_count);

        setupSearch();
        setupAdapters();
        loadAnimeLists();

        return view;
    }

    private void setupAdapters() {
        watchingAdapter = new WatchingAnimeAdapter(
                requireContext(),
                watchingList,
                this::moveAnimeToWatched,
                this::updateAnimeEpisodes,
                this::deleteWatchingAnime
        );

        watchedAdapter = new WatchedAnimeAdapter(
                requireContext(),
                watchedList,
                this::deleteWatchedAnime
        );

        recyclerViewWatching.setAdapter(watchingAdapter);
        recyclerViewWatched.setAdapter(watchedAdapter);
    }

    private void loadAnimeLists() {
        repository.getWatchingAnime(new AnimeRepository.OnDataLoadedCallback<List<Anime>>() {
            @Override
            public void onDataLoaded(List<Anime> watchingAnime) {
                if (!isAdded()) {
                    Log.d("AnimeFragment", "Fragment not attached during callback");
                    return;
                }
                originalWatchingList.clear();
                originalWatchingList.addAll(watchingAnime);
                watchingList.clear();
                watchingList.addAll(watchingAnime);
                scheduleUpdatesForAllOngoingAnimes();
                watchingAdapter.notifyDataSetChanged();
                updateCounts();
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
                updateCounts();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading watched anime", e);
                showErrorMessage("Failed to load watched anime");
            }
        });
    }

    private void scheduleUpdatesForAllOngoingAnimes() {
        if (!isAdded()) {
            Log.d("AnimeFragment", "Fragment not attached, skipping updates");
            return;
        }
        for (Anime anime : watchingList) {
            if (anime.getAiringStatus() == Anime.AiringStatus.ONGOING) {
                WorkScheduler.schedulePeriodicUpdate(requireContext(), anime.getId(), "anime");
            }
        }
    }

    private void updateAnimeEpisodes(Anime anime) {
        repository.updateAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d(TAG, "Updated episode count for: " + anime.getTitleRomaji());
                // No UI update needed if only updating episode count within already listed items
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error updating episode count", e);
                showErrorMessage("Failed to save episode count");
            }
        });
    }

    private void addToUndoStack(UndoAction_anime action) {
        undoStack.push(action);
        if (undoListener != null) {
            undoListener.setUndoEnabled(true);
            undoListener.updateUndoButton(true);
        }
    }

    public void performUndo() {
        if (!undoStack.isEmpty()) {
            UndoAction_anime action = undoStack.pop();

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
                case REMOVE_FROM_WATCHING:
                    undoRemoveFromWatching(action.getAnime(), action.getPosition());
                    break;
                case ADD_TO_WATCHED:
                    undoAddToWatched(action.getAnime(), action.getPosition());
                    break;
            }

            if (undoListener != null) {
                boolean hasItems = !undoStack.isEmpty();
                undoListener.setUndoEnabled(hasItems);
                undoListener.updateUndoButton(hasItems);
            }
        }
        updateCounts();
    }

    public void addAnimeToWatchingList(Anime anime) {
        repository.insertAnime(anime, new AnimeRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchingList.add(anime);
                originalWatchingList.add(anime);
                updateCounts();
                watchingAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_anime(
                        UndoAction_anime.ActionType.ADD_TO_WATCHING,
                        anime,
                        watchingList.size() - 1
                ));
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to add anime");
            }
        });
        updateCounts();
    }

    public void addAnimeToWatchedList(Anime anime) {
        repository.insertAnime(anime, new AnimeRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchedList.add(anime);
                originalWatchedList.add(anime);
                updateCounts();
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_anime(
                        UndoAction_anime.ActionType.ADD_TO_WATCHED,
                        anime,
                        watchedList.size() - 1


                ));
            }
            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to add anime");
            }
    });
    }

    private void undoAddToWatched(Anime anime, int originalPosition) {
        repository.deleteAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchedList.remove(originalPosition);
                updateCounts();
                watchedAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo add anime");
            }

        });
    }

    private void undoAddToWatching(Anime anime, int position) {
        repository.deleteAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingList.remove(position);
                updateCounts();
                watchingAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo add anime");
            }
        });
        originalWatchingList.remove(anime);
        updateCounts();

    }

    private void moveAnimeToWatched(Anime anime) {


        int originalPosition = watchingList.indexOf(anime);
        watchingList.remove(anime);
        originalWatchingList.remove(anime);
        anime.markAsFinished();
        watchedList.add(anime);
        originalWatchedList.add(anime);
        updateCounts();

        repository.updateAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_anime(
                        UndoAction_anime.ActionType.MOVE_TO_WATCHED,
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
        originalWatchedList.remove(anime);
        anime.setWatching(true);
        anime.setFinishedDate(null);
        watchingList.add(originalPosition, anime);
        originalWatchingList.add(originalPosition, anime);
        updateCounts();

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

    private void deleteWatchingAnime(Anime anime) {
        int position = watchingList.indexOf(anime);
        watchingList.remove(anime);
        updateCounts();
        repository.deleteAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_anime(
                        UndoAction_anime.ActionType.REMOVE_FROM_WATCHING,
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


    private void deleteWatchedAnime(Anime anime) {
        int position = watchedList.indexOf(anime);
        watchedList.remove(anime);
        updateCounts();
        repository.deleteAnime(anime, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_anime(
                        UndoAction_anime.ActionType.REMOVE_FROM_WATCHED,
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

    private void undoRemoveFromWatching(Anime anime, int position) {
        repository.insertAnime(anime, new AnimeRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchingList.add(position, anime);
                updateCounts();
                watchingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo delete");
            }
        });

    }

    private void undoRemoveFromWatched(Anime anime, int position) {
        repository.insertAnime(anime, new AnimeRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchedList.add(position, anime);
                updateCounts();
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
        // Set up EditText click listener to show keyboard
        searchEditText.setOnClickListener(v -> {
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Add text change listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAnimeLists(s.toString());
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle search action
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                // Process search
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    filterAnimeLists(query);
                }

                v.clearFocus();
                return true;
            }
            return false;
        });

        // Set up focus change listener
        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        // Clear search button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            resetLists();
            clearSearchButton.setVisibility(View.GONE);

            // Hide keyboard when clearing
            InputMethodManager imm = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }

            searchEditText.clearFocus();
        });

        // Make sure EditText is focusable
        searchEditText.setFocusable(true);
        searchEditText.setFocusableInTouchMode(true);
    }

    private void updateCounts() {
        watchingCountText.setText(String.format("(%d)", watchingList.size()));
        watchedCountText.setText(String.format("(%d)", watchedList.size()));
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

        watchingList.clear();
        watchingList.addAll(filteredWatchingList);
        watchingAdapter.notifyDataSetChanged();

        watchedList.clear();
        watchedList.addAll(filteredWatchedList);
        watchedAdapter.notifyDataSetChanged();
        updateCounts();
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