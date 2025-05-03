package com.aareno.seen.ui.KDrama;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchKDramaActivity extends AppCompatActivity {
    private static final String TAG = "SearchKDramaActivity";
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView recyclerView;
    private KDramaSearchAdapter kDramaAdapter;
    private List<KDrama> kDramaList;
    private List<KDrama> selectedKDramaList = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private ImageButton doneButton;

    private RecyclerView latestRecyclerView;
    private KDramaSearchAdapter latestKDramaAdapter;
    private List<KDrama> latestKDramaList = new ArrayList<>();
    private TextView searchResultsTitle;
    private TextView latestTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);

        // Initialize views
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);
        doneButton = findViewById(R.id.back_button);
        latestRecyclerView = findViewById(R.id.popular_recycler_view);
        latestTitle = findViewById(R.id.popular_title);
        searchResultsTitle = findViewById(R.id.search_results_title);

        latestTitle.setText("Latest Releases");

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            sendResultAndFinish();
        });

        // Initialize kdrama lists
        kDramaList = new ArrayList<>();
        latestKDramaList = new ArrayList<>();

        // Setup Latest RecyclerView
        latestKDramaAdapter = new KDramaSearchAdapter(latestKDramaList,
                new KDramaSearchAdapter.OnItemClickListener() {
                    @Override
                    public void onAddToWatchingClick(KDrama kdrama) {
                        executorService.execute(() -> {
                            try {
                                boolean isDuplicate = selectedKDramaList.stream()
                                        .anyMatch(d -> d.getId() == kdrama.getId());

                                if (!isDuplicate) {
                                    kdrama.setWatching(true);
                                    selectedKDramaList.add(kdrama);

                                    mainHandler.post(() -> {
                                        latestKDramaAdapter.notifyDataSetChanged();
                                        Toast.makeText(SearchKDramaActivity.this,
                                                "Added " + kdrama.getTitleEnglish() + " to watching list",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                } else {
                                    mainHandler.post(() -> {
                                        Toast.makeText(SearchKDramaActivity.this,
                                                "Already added to watching list",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } catch (Exception e) {
                                mainHandler.post(() -> {
                                    Log.e(TAG, "Error adding kdrama to watching list", e);
                                    Toast.makeText(SearchKDramaActivity.this,
                                            "Error adding kdrama: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    }
                });

        // Setup Search RecyclerView
        kDramaAdapter = new KDramaSearchAdapter(kDramaList, new KDramaSearchAdapter.OnItemClickListener() {
            @Override
            public void onAddToWatchingClick(KDrama kdrama) {
                executorService.execute(() -> {
                    try {
                        boolean isDuplicate = selectedKDramaList.stream()
                                .anyMatch(d -> d.getId() == kdrama.getId());

                        if (!isDuplicate) {
                            Log.d(TAG, "Adding KDrama: " + kdrama.getTitleEnglish());
                            Log.d(TAG, "KDrama ID: " + kdrama.getId());
                            Log.d(TAG, "KDrama Image URL: " + kdrama.getCoverImageUrl());

                            kdrama.setWatching(true);
                            selectedKDramaList.add(kdrama);

                            mainHandler.post(() -> {
                                kDramaAdapter.notifyDataSetChanged();
                                Toast.makeText(SearchKDramaActivity.this,
                                        "Added " + kdrama.getTitleEnglish() + " to watching list",
                                        Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            mainHandler.post(() -> {
                                Toast.makeText(SearchKDramaActivity.this,
                                        "Already added to watching list",
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            Log.e(TAG, "Error adding kdrama to watching list", e);
                            Toast.makeText(SearchKDramaActivity.this,
                                    "Error adding kdrama: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(kDramaAdapter);

        latestRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        latestRecyclerView.setAdapter(latestKDramaAdapter);

        // Load latest KDramas when activity starts
        loadLatestKDramas();

        // Add text change listener to handle visibility
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateVisibility(!s.toString().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Done button click listener
        doneButton.setOnClickListener(v -> sendResultAndFinish());

        // Search button click listener
        searchButton.setOnClickListener(v -> {
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        });

        // Handle enter/return key press
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

                String query = searchEditText.getText().toString();
                searchKDrama(query);
                return true;
            }
            return false;
        });
    }

    private void loadLatestKDramas() {
        OkHttpClient client = new OkHttpClient();

        // TVMaze API endpoint for shows
        String url = "https://api.tvmaze.com/schedule?country=KR";  // Get Korean schedule

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    Toast.makeText(SearchKDramaActivity.this,
                            "Failed to load latest KDramas: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray schedule = new JSONArray(responseBody);
                    List<KDrama> latestDramas = new ArrayList<>();
                    Set<Integer> addedShowIds = new HashSet<>();

                    for (int i = 0; i < schedule.length(); i++) {
                        JSONObject entry = schedule.getJSONObject(i);
                        JSONObject show = entry.getJSONObject("show");

                        if (show.has("language") && "Korean".equals(show.getString("language"))) {
                            int id = show.getInt("id");

                            if (addedShowIds.contains(id)) {
                                continue;
                            }

                            String title = show.getString("name");
                            String imageUrl = null;

                            if (show.has("image") && !show.isNull("image")) {
                                JSONObject imageObj = show.getJSONObject("image");
                                imageUrl = imageObj.optString("medium", null);
                            }

                            KDrama kdrama = new KDrama(id, title, title, imageUrl);

                            boolean isSelected = selectedKDramaList.stream()
                                    .anyMatch(d -> d.getId() == id);
                            kdrama.setWatching(isSelected);

                            latestDramas.add(kdrama);
                            addedShowIds.add(id);

                            fetchEpisodeCountForLatest(kdrama);

                            if (latestDramas.size() >= 10) {
                                break;
                            }
                        }
                    }

                    mainHandler.post(() -> {
                        latestKDramaList.clear();
                        latestKDramaList.addAll(latestDramas);
                        latestKDramaAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        Toast.makeText(SearchKDramaActivity.this,
                                "Error parsing latest KDramas",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void fetchEpisodeCountForLatest(KDrama kdrama) {
        String episodeUrl = "https://api.tvmaze.com/shows/" + kdrama.getId() + "/episodes";

        Request episodeRequest = new Request.Builder()
                .url(episodeUrl)
                .get()
                .build();

        new OkHttpClient().newCall(episodeRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray episodes = new JSONArray(responseBody);
                    int episodeCount = episodes.length();

                    kdrama.setEpisodeCount(episodeCount);

                    mainHandler.post(() -> {
                        latestKDramaAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void searchKDrama(String query) {
        OkHttpClient client = new OkHttpClient();

        String encodedQuery = Uri.encode(query);
        String url = "https://api.tvmaze.com/search/shows?q=" + encodedQuery;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    Toast.makeText(SearchKDramaActivity.this,
                            "Search failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray searchResults = new JSONArray(responseBody);
                    final List<KDrama> kdramas = new ArrayList<>();

                    for (int i = 0; i < searchResults.length(); i++) {
                        JSONObject result = searchResults.getJSONObject(i);
                        JSONObject show = result.getJSONObject("show");

                        if (show.has("language") && "Korean".equals(show.getString("language"))) {
                            int id = show.getInt("id");
                            fetchEpisodeCount(id, show, kdramas);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        Toast.makeText(SearchKDramaActivity.this,
                                "Error parsing search results",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void fetchEpisodeCount(int showId, JSONObject show, List<KDrama> kdramas) {
        String episodeUrl = "https://api.tvmaze.com/shows/" + showId + "/episodes";

        Request episodeRequest = new Request.Builder()
                .url(episodeUrl)
                .get()
                .build();

        new OkHttpClient().newCall(episodeRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray episodes = new JSONArray(responseBody);
                    int episodeCount = episodes.length();

                    String title = show.getString("name");
                    String imageUrl = null;
                    if (show.has("image") && !show.isNull("image")) {
                        JSONObject imageObj = show.getJSONObject("image");
                        imageUrl = imageObj.optString("medium", null);
                    }

                    KDrama kdrama = new KDrama(showId, title, title, imageUrl);
                    kdrama.setEpisodeCount(episodeCount);

                    boolean isSelected = selectedKDramaList.stream()
                            .anyMatch(d -> d.getId() == showId);
                    kdrama.setWatching(isSelected);

                    kdramas.add(kdrama);

                    mainHandler.post(() -> {
                        kDramaList.clear();
                        kDramaList.addAll(kdramas);
                        kDramaAdapter.notifyDataSetChanged();

                        if (kdramas.isEmpty()) {
                            Toast.makeText(SearchKDramaActivity.this,
                                    "No Korean dramas found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateVisibility(boolean isSearching) {
        latestTitle.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        latestRecyclerView.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        searchResultsTitle.setVisibility(isSearching ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isSearching ? View.VISIBLE : View.GONE);
    }

    private void sendResultAndFinish() {
        if (!selectedKDramaList.isEmpty()) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_kdrama_list", new ArrayList<>(selectedKDramaList));
            setResult(RESULT_OK, resultIntent);
        }
        executorService.shutdown();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}