package com.aareno.seen.ui.KDrama;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.aareno.seen.data.WorkScheduler;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.Anime.UndoAction_anime;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class KDramaFragment extends Fragment {

    private static final String TAG = "KDramaFragment";
    private RecyclerView recyclerViewWatching;
    private RecyclerView recyclerViewWatched;
    private WatchingKDramaAdapter watchingAdapter;
    private WatchedKDramaAdapter watchedAdapter;
    private KDramaRepository repository;
    private Stack<UndoAction_kdrama> undoStack = new Stack<>();
    private UndoListener undoListener;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private TextView watchingCountText;
    private TextView watchedCountText;
    private List<KDrama> watchingList = new ArrayList<>();
    private List<KDrama> watchedList = new ArrayList<>();
    private List<KDrama> originalWatchingList = new ArrayList<>();
    private List<KDrama> originalWatchedList = new ArrayList<>();

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
        repository = new KDramaRepository(requireContext());

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kdrama, container, false);

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
        loadKDramaLists();

        return view;
    }

    private void setupAdapters() {
        watchingAdapter = new WatchingKDramaAdapter(
                requireContext(),
                watchingList,
                this::moveKDramaToWatched,
                this::updateKDramaEpisodes,
                this::deleteWatchingKDrama
        );

        watchedAdapter = new WatchedKDramaAdapter(
                requireContext(),
                watchedList,
                this::deleteWatchedKDrama
        );

        recyclerViewWatching.setAdapter(watchingAdapter);
        recyclerViewWatched.setAdapter(watchedAdapter);
    }

    private void loadKDramaLists() {
        // Get adult content filter setting from MainActivity
        boolean showAdultContent = false;
        if (getActivity() instanceof MainActivity) {
            showAdultContent = ((MainActivity) getActivity()).shouldShowAdultContent();
        }

        // Final variable for use in callbacks
        final boolean finalShowAdultContent = showAdultContent;

        Log.d(TAG, "Loading K-drama lists with adult content filter: " + (showAdultContent ? "OFF" : "ON"));

        repository.getWatchingKdrama(new KDramaRepository.OnDataLoadedCallback<List<KDrama>>() {

            @Override
            public void onDataLoaded(List<KDrama> watchingKDrama) {
                if (!isAdded()) {
                    Log.d("KDrama", "Fragment not attached during callback");
                    return;
                }
                originalWatchingList.clear();
                originalWatchingList.addAll(watchingKDrama);
                watchingList.clear();
                watchingList.addAll(watchingKDrama);
                watchingAdapter.notifyDataSetChanged();
                updateCounts();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading watching K-drama", e);
                showErrorMessage("Failed to load watching K-drama");
            }
        });

        repository.getWatchedKdrama(new KDramaRepository.OnDataLoadedCallback<List<KDrama>>() {
            @Override
            public void onDataLoaded(List<KDrama> watchedKDrama) {
                originalWatchedList.clear();
                originalWatchedList.addAll(watchedKDrama);
                watchedList.clear();
                watchedList.addAll(watchedKDrama);
                scheduleUpdatesForAllOngoingAnimes();  // Changed from animes to KDramas
                watchedAdapter.notifyDataSetChanged();
                updateCounts();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading watched K-drama", e);
                showErrorMessage("Failed to load watched K-drama");
            }
        });
    }

    private void scheduleUpdatesForAllOngoingAnimes() {
        if (!isAdded()) {
            Log.d("KDramaFragment", "Fragment not attached during callback");
            return;
        }
        for (KDrama kdrama : watchingList) {
            if (kdrama.getAiringStatus() == Anime.AiringStatus.ONGOING) {
                WorkScheduler.schedulePeriodicUpdate(requireContext(), kdrama.getId(), "kdrama");
            }
        }
    }

    // Search Logic
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

    private void updateKDramaEpisodes(KDrama kdrama) {
        repository.updateKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Void>() {
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


    /* Moving items */
    public void addKDramaToWatchingList(KDrama kdrama) {
        repository.insertKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchingList.add(kdrama);
                originalWatchingList.add(kdrama);
                updateCounts();
                watchingAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_kdrama(
                        UndoAction_kdrama.ActionType.ADD_TO_WATCHING,
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

    public void addKDramaToWatchedList(KDrama kdrama) {
        repository.insertKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Long>() {
            @Override
            public void onDataLoaded(Long id) {
                watchedList.add(kdrama);
                originalWatchedList.add(kdrama);
                updateCounts();
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_kdrama(
                        UndoAction_kdrama.ActionType.ADD_TO_WATCHED,
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

    private void moveKDramaToWatched(KDrama kdrama) {
        int originalPosition = watchingList.indexOf(kdrama);
        watchingList.remove(kdrama);
        originalWatchingList.remove(kdrama);
        kdrama.markAsFinished();
        watchedList.add(kdrama);
        originalWatchedList.add(kdrama);
        updateCounts();
        repository.updateKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_kdrama(
                        UndoAction_kdrama.ActionType.MOVE_TO_WATCHED,
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

    private void deleteWatchingKDrama(KDrama kdrama) {
        int position = watchingList.indexOf(kdrama);
        watchingList.remove(kdrama);

        repository.deleteKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchingAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_kdrama(
                        UndoAction_kdrama.ActionType.REMOVE_FROM_WATCHING,
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

    private void deleteWatchedKDrama(KDrama kdrama) {
        int position = watchedList.indexOf(kdrama);
        watchedList.remove(kdrama);

        repository.deleteKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                watchedAdapter.notifyDataSetChanged();
                addToUndoStack(new UndoAction_kdrama(
                        UndoAction_kdrama.ActionType.REMOVE_FROM_WATCHED,
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
    /* ******************************************** */

    private void showErrorMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /* undo logic */

    public void performUndo() {
        if (!undoStack.isEmpty()) {
            UndoAction_kdrama action = undoStack.pop();

            switch (action.getType()) {
                case ADD_TO_WATCHING:
                    undoAddToWatching(action.getKdrama(), action.getPosition());
                    break;
                case REMOVE_FROM_WATCHED:
                    undoRemoveFromWatched(action.getKdrama(), action.getPosition());
                    break;
                case MOVE_TO_WATCHED:
                    undoMoveToWatched(action.getKdrama(), action.getPosition());
                    break;
                case REMOVE_FROM_WATCHING:
                    undoRemoveFromWatching(action.getKdrama(), action.getPosition());
                    break;
                case ADD_TO_WATCHED:
                    undoAddToWatched(action.getKdrama(), action.getPosition());
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

    private void addToUndoStack(UndoAction_kdrama action) {
        undoStack.push(action);
        if (undoListener != null) {
            undoListener.setUndoEnabled(true);
            undoListener.updateUndoButton(true);
        }
    }

    private void undoMoveToWatched(KDrama kdrama, int originalPosition) {
        watchedList.remove(kdrama);
        originalWatchedList.remove(kdrama);
        kdrama.setWatching(true);
        kdrama.setFinishedDate(null);
        watchingList.add(originalPosition, kdrama);
        originalWatchingList.add(originalPosition, kdrama);


        repository.updateKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Void>() {
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

    private void undoRemoveFromWatching(KDrama kdrama, int position) {
        repository.insertKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Long>() {
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

    private void undoRemoveFromWatched(KDrama kdrama, int position) {
        repository.insertKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Long>() {
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

    private void undoAddToWatched(KDrama kdrama, int originalPosition) {
        repository.deleteKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Void>() {
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

    private void undoAddToWatching(KDrama kdrama, int position) {
        repository.deleteKdrama(kdrama, new KDramaRepository.OnDataLoadedCallback<Void>() {
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

    /* filter logic */

    private void filterAnimeLists(String query) {
        query = query.toLowerCase().trim();

        // Filter watching list
        List<KDrama> filteredWatchingList = new ArrayList<>();
        for (KDrama  KDrama: originalWatchingList) {
            if (KDramaMatchesQuery(KDrama, query)) {
                filteredWatchingList.add(KDrama);
            }
        }

        // Filter watched list
        List<KDrama> filteredWatchedList = new ArrayList<>();
        for (KDrama kdrama : originalWatchedList) {
            if (KDramaMatchesQuery(kdrama, query)) {
                filteredWatchedList.add(kdrama);
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

    private boolean KDramaMatchesQuery(KDrama kdrama, String query) {
        return kdrama.getTitleEnglish().toLowerCase().contains(query) ||
                kdrama.getTitleKorean().contains(query);
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
