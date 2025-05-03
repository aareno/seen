package com.aareno.seen.ui.KDrama;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.List;
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
    private ImageButton doneButton; // Add this

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);

        // Initialize views
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);
        doneButton = findViewById(R.id.back_button); // Initialize done button

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            sendResultAndFinish();
        });

        // Initialize kdrama list
        kDramaList = new ArrayList<>();

        // Setup RecyclerView
        kDramaAdapter = new KDramaSearchAdapter(kDramaList, new KDramaSearchAdapter.OnItemClickListener() {
            @Override
            public void onAddToWatchingClick(KDrama kdrama) {
                executorService.execute(() -> {
                    try {
                        // Check for duplicates
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

    private void sendResultAndFinish() {
        if (!selectedKDramaList.isEmpty()) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_kdrama_list", new ArrayList<>(selectedKDramaList));
            setResult(RESULT_OK, resultIntent);
        }
        executorService.shutdown();
        finish();
    }

    private void searchKDrama(String query) {
        OkHttpClient client = new OkHttpClient();

        // TVMaze API endpoint for searching shows
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

                        // Filter for Korean shows
                        if (show.has("language") && "Korean".equals(show.getString("language"))) {
                            int id = show.getInt("id");
                            String title = show.getString("name");

                            String imageUrl = null;
                            if (show.has("image") && !show.isNull("image")) {
                                JSONObject imageObj = show.getJSONObject("image");
                                imageUrl = imageObj.optString("medium", null);
                            }

                            KDrama kdrama = new KDrama(id, title, title, imageUrl);
                            // Check if this drama is already selected
                            boolean isSelected = selectedKDramaList.stream()
                                    .anyMatch(d -> d.getId() == id);
                            kdrama.setWatching(isSelected);
                            kdramas.add(kdrama);
                        }
                    }

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
                    mainHandler.post(() -> {
                        Toast.makeText(SearchKDramaActivity.this,
                                "Error parsing search results",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}