package com.aareno.seen.ui.TvMovies;

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

import com.aareno.seen.MainActivity;
import com.aareno.seen.R;
import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.aareno.seen.data.TvMovies.ShowRepository;
import com.aareno.seen.data.WorkScheduler;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.Anime.AnimeFragment;
import com.aareno.seen.ui.KDrama.KDrama;
import com.aareno.seen.ui.KDrama.UndoAction_kdrama;
import com.aareno.seen.ui.KDrama.WatchedKDramaAdapter;
import com.aareno.seen.ui.KDrama.WatchingKDramaAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TvMoviesFragment extends Fragment {

    private static final String TAG = "TvMoviesFragment";
    private List<Show> watchingList = new ArrayList<>();
    private List<Show> watchedList = new ArrayList<>();

    private RecyclerView recyclerViewWatching;
    private RecyclerView recyclerViewWatched;
    private WatchingShowAdapter watchingAdapter;
    private WatchedShowAdapter watchedAdapter;
    private ShowRepository repository;
    private Stack<UndoAction_show> undoStack = new Stack<>();
    private UndoListener undoListener;
    private EditText searchEditText;
    private TextView watchingCountText;
    private TextView watchedCountText;
    private ImageButton clearSearchButton;
    private List<Show> originalWatchingList = new ArrayList<>();
    private List<Show> originalWatchedList = new ArrayList<>();

    public interface UndoListener {
        void setUndoEnabled(boolean enabled);
        void updateUndoButton(boolean hasItems);
    }


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
        repository = new ShowRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tv_movies, container, false);

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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAnimeLists(s.toString());
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
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

    private void setupAdapters() {
        watchingAdapter = new WatchingShowAdapter(
                requireContext(),
                watchingList,
                this::moveShowToWatched,
                this::updateShowEpisodes,
                this::deleteWatchingShow
        );

        watchedAdapter = new WatchedShowAdapter(
                requireContext(),
                watchedList,
                this::deleteWatchedShow
        );

        recyclerViewWatching.setAdapter(watchingAdapter);
        recyclerViewWatched.setAdapter(watchedAdapter);
    }

    private void loadAnimeLists() {
        boolean showAdultContent = false;
        if (getActivity() instanceof MainActivity) {
            showAdultContent = ((MainActivity) getActivity()).shouldShowAdultContent();
        }

        // Final variable for use in callbacks
        final boolean finalShowAdultContent = showAdultContent;

        repository.getWatchingShow(new ShowRepository.OnDataLoadedCallback<List<Show>>() {

            @Override
            public void onDataLoaded(List<Show> watchingKDrama) {

                List<Show> filteredList = watchingKDrama;
                if (!finalShowAdultContent) {
                    filteredList = new ArrayList<>();
                    for (Show anime : watchingKDrama) {
                        if (!anime.isMature()) {
                            filteredList.add(anime);
                        }
                    }
                    Log.d(TAG, "Filtered out " + (watchingKDrama.size() - filteredList.size()) +
                            " mature anime from watching list");
                }

                originalWatchingList.clear();
                originalWatchingList.addAll(filteredList);
                watchingList.clear();
                watchingList.addAll(filteredList);
                watchingAdapter.notifyDataSetChanged();
                updateCounts();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading watching kdrama", e);
                showErrorMessage("Failed to load watching kdrama");
            }
        });

        repository.getWatchedShow(new ShowRepository.OnDataLoadedCallback<List<Show>>() {
            @Override
            public void onDataLoaded(List<Show> watchedKDrama) {

                List<Show> filteredList = watchedKDrama;
                if (!finalShowAdultContent) {
                    filteredList = new ArrayList<>();
                    for (Show anime : watchedKDrama) {
                        if (!anime.isMature()) {
                            filteredList.add(anime);
                        }
                    }
                    Log.d(TAG, "Filtered out " + (watchedKDrama.size() - filteredList.size()) +
                            " mature anime from watching list");
                }

                originalWatchedList.clear();
                originalWatchedList.addAll(filteredList);
                watchedList.clear();
                watchedList.addAll(filteredList);
                scheduleUpdatesForAllOngoingAnimes();
                watchedAdapter.notifyDataSetChanged();
                updateCounts();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading watched kdrama", e);
                showErrorMessage("Failed to load watched kdrama");
            }
        });
    }

    private void showErrorMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /* List Manipulation Logic */

    public void addShowToWatchingList(Show kdrama) {
        repository.insertShow(kdrama, new ShowRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchingList.add(kdrama);
                originalWatchingList.add(kdrama);
                updateCounts();
                watchingAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_show(
                        UndoAction_show.ActionType.ADD_TO_WATCHING,
                        kdrama,
                        watchingList.size() - 1
                ));
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to add anime");
            }
        });
    }

    public void addShowToWatchedList(Show kdrama) {
        repository.insertShow(kdrama, new ShowRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchedList.add(kdrama);
                originalWatchedList.add(kdrama);
                updateCounts();
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_show(
                        UndoAction_show.ActionType.ADD_TO_WATCHED,
                        kdrama,
                        watchedList.size() - 1
                ));
            }
            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to add Kdrama");
            }
        });
    }

    private void moveShowToWatched(Show kdrama) {
        int originalPosition = watchingList.indexOf(kdrama);
        watchingList.remove(kdrama);
        originalWatchingList.remove(kdrama);
        kdrama.markAsFinished();
        watchedList.add(kdrama);
        originalWatchedList.add(kdrama);
        updateCounts();
        repository.updateShow(kdrama, new ShowRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_show(
                        UndoAction_show.ActionType.MOVE_TO_WATCHED,
                        kdrama,
                        originalPosition
                ));
            }
            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to move anime");
            }
        });
    }

    private void deleteWatchingShow(Show kdrama) {
        int position = watchingList.indexOf(kdrama);
        watchingList.remove(kdrama);

        repository.deleteShow(kdrama, new ShowRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_show(
                        UndoAction_show.ActionType.REMOVE_FROM_WATCHING,
                        kdrama,
                        position
                ));
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to delete anime");
            }
        });
        updateCounts();
    }

    private void deleteWatchedShow(Show kdrama) {
        int position = watchedList.indexOf(kdrama);
        watchedList.remove(kdrama);

        repository.deleteShow(kdrama, new ShowRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_show(
                        UndoAction_show.ActionType.REMOVE_FROM_WATCHED,
                        kdrama,
                        position
                ));
            }
            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to delete anime");
            }
        });
        updateCounts();
    }

    private void updateShowEpisodes(Show kdrama) {
        repository.updateShow(kdrama, new ShowRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d(TAG, "Updated episode count for: " + kdrama.getTitleEnglish());
                // No UI update needed if only updating episode count within already listed items
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error updating episode count", e);
                showErrorMessage("Failed to save episode count");
            }
        });
    }
    private void scheduleUpdatesForAllOngoingAnimes() {
        for (Show kdrama : watchingList) {
            if (kdrama.getAiringStatus() == Anime.AiringStatus.ONGOING) {
                WorkScheduler.schedulePeriodicUpdate(requireContext(), kdrama.getId(), "kdrama");
            }
        }
    }

    /* Undo Logic */

    public void performUndo() {
        if (!undoStack.isEmpty()) {
            UndoAction_show action = undoStack.pop();

            switch (action.getType()) {
                case ADD_TO_WATCHING:
                    undoAddToWatching(action.getShow(), action.getPosition());
                    break;
                case REMOVE_FROM_WATCHED:
                    undoRemoveFromWatched(action.getShow(), action.getPosition());
                    break;
                case MOVE_TO_WATCHED:
                    undoMoveToWatched(action.getShow(), action.getPosition());
                    break;
                case REMOVE_FROM_WATCHING:
                    undoRemoveFromWatching(action.getShow(), action.getPosition());
                    break;
                case ADD_TO_WATCHED:
                    undoAddToWatched(action.getShow(), action.getPosition());
                    break;
            }

            if (undoListener != null) {
                boolean hasItems = !undoStack.isEmpty();
                undoListener.setUndoEnabled(hasItems);
                undoListener.updateUndoButton(hasItems);
            }
        }
    }


    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    private void addToUndoStack(UndoAction_show action) {
        undoStack.push(action);
        if (undoListener != null) {
            undoListener.setUndoEnabled(true);
            undoListener.updateUndoButton(true);
        }
    }

    private void undoMoveToWatched(Show kdrama, int originalPosition) {
        watchedList.remove(kdrama);
        originalWatchedList.remove(kdrama);
        kdrama.setWatching(true);
        kdrama.setFinishedDate(null);
        watchingList.add(originalPosition, kdrama);
        originalWatchingList.add(originalPosition, kdrama);


        repository.updateShow(kdrama, new ShowRepository.OnDataLoadedCallback<Void>() {
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
        updateCounts();
    }

    private void undoRemoveFromWatching(Show kdrama, int position) {
        repository.insertShow(kdrama, new ShowRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchingList.add(position, kdrama);
                updateCounts();
                watchingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo delete");
            }
        });
        updateCounts();
    }

    private void undoRemoveFromWatched(Show kdrama, int position) {
        repository.insertShow(kdrama, new ShowRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchedList.add(position, kdrama);
                updateCounts();
                watchedAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to undo delete");
            }
        });
        updateCounts();
    }

    private void undoAddToWatched(Show kdrama, int originalPosition) {
        repository.deleteShow(kdrama, new ShowRepository.OnDataLoadedCallback<Void>() {
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

    private void undoAddToWatching(Show kdrama, int position) {
        repository.deleteShow(kdrama, new ShowRepository.OnDataLoadedCallback<Void>() {
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
        originalWatchingList.remove(kdrama);
        updateCounts();
    }

    private boolean ShowMatchesQuery(Show kdrama, String query) {
        return kdrama.getTitleEnglish().toLowerCase().contains(query) ||
                kdrama.getTitleAlt().contains(query);
    }

    /* Filter Logic */

    private void filterAnimeLists(String query) {
        query = query.toLowerCase().trim();

        // Filter watching list
        List<Show> filteredWatchingList = new ArrayList<>();
        for (Show show: originalWatchingList) {
            if (ShowMatchesQuery(show, query)) {
                filteredWatchingList.add(show);
            }
        }

        // Filter watched list
        List<Show> filteredWatchedList = new ArrayList<>();
        for (Show show : originalWatchedList) {
            if (ShowMatchesQuery(show, query)) {
                filteredWatchedList.add(show);
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

    private void updateCounts() {
        watchingCountText.setText(String.format("(%d)", watchingList.size()));
        watchedCountText.setText(String.format("(%d)", watchedList.size()));
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