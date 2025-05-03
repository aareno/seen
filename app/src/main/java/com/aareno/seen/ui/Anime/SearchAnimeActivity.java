package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SearchAnimeActivity extends AppCompatActivity {
    private static final String TAG = "SearchAnimeActivity";

    // Searching
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView recyclerView;
    private AnimeSearchAdapter animeAdapter;
    private List<Anime> animeList;
    private List<Anime> selectedAnimeList = new ArrayList<>();

    // Popular
    private RecyclerView popularRecyclerView;
    private AnimeSearchAdapter popularAnimeAdapter;
    private List<Anime> popularAnimeList;
    private TextView searchResultsTitle;
    private TextView popularTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);


        // Initialize views
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);

        // Initialize popular anime views and list
        popularRecyclerView = findViewById(R.id.popular_recycler_view);
        popularAnimeList = new ArrayList<>();
        searchResultsTitle = findViewById(R.id.search_results_title);
        popularTitle = findViewById(R.id.popular_title);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Initialize anime list
        animeList = new ArrayList<>();

        // Setup popular anime RecyclerView
        popularAnimeAdapter = new AnimeSearchAdapter(popularAnimeList, new AnimeSearchAdapter.OnItemClickListener() {
            @Override
            public void onAddToWatchingClick(Anime anime) {
                // Same implementation as your existing onAddToWatchingClick
            }
        });

        // Setup RecyclerView
        animeAdapter = new AnimeSearchAdapter(animeList, new AnimeSearchAdapter.OnItemClickListener() {
            @Override
            public void onAddToWatchingClick(Anime anime) {
                try {
                    // Log the anime details
                    Log.d(TAG, "Adding anime: " + anime.getTitleRomaji());
                    Log.d(TAG, "Anime ID: " + anime.getId());
                    Log.d(TAG, "Anime Cover URL: " + anime.getCoverImageUrl());

                    selectedAnimeList.add(anime);

                    // Create an intent to return the selected anime
                    Intent resultIntent = new Intent();
                    anime.setWatching(true);
                    resultIntent.putExtra("selected_anime_list", new ArrayList<>(selectedAnimeList));
                    setResult(RESULT_OK, resultIntent);

                    Toast.makeText(SearchAnimeActivity.this,
                            "Added " + anime.getTitleEnglish() + " to watching list",
                            Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    // Log any exceptions
                    Log.e(TAG, "Error adding anime to watching list", e);
                    Toast.makeText(SearchAnimeActivity.this,
                            "Error adding anime: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(animeAdapter);

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        popularRecyclerView.setAdapter(popularAnimeAdapter);

        loadPopularAnime();
        // Modify search related listeners to handle visibility
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

        // Search button click listener
        searchButton.setOnClickListener(v -> {
            // Show keyboard and focus on search EditText
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        });

        // Handle enter/return key press
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

                // Perform search
                String query = searchEditText.getText().toString();
                searchAnime(query);
                return true;
            }
            return false;
        });
    }

    private void searchAnime(String query) {
        OkHttpClient client = new OkHttpClient();

        // Updated GraphQL query to include episodes
        RequestBody requestBody = new FormBody.Builder()
                .add("query", "query ($search: String) { " +
                        "Page(page: 1, perPage: 10) { " +
                        "  media(search: $search, type: ANIME) { " +
                        "    id " +
                        "    title { " +
                        "      romaji " +
                        "      english " +
                        "    } " +
                        "    coverImage { " +
                        "      large " +
                        "    } " +
                        "    episodes " +  // Added episodes field
                        "  } " +
                        "} " +
                        "}")
                .add("variables", "{\"search\":\"" + query + "\"}")
                .build();

        // Create request
        Request request = new Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        // Execute request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(SearchAnimeActivity.this,
                            "Search failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray mediaList = jsonObject
                            .getJSONObject("data")
                            .getJSONObject("Page")
                            .getJSONArray("media");

                    final List<Anime> searchResults = new ArrayList<>();
                    for (int i = 0; i < mediaList.length(); i++) {
                        JSONObject media = mediaList.getJSONObject(i);
                        JSONObject title = media.getJSONObject("title");
                        JSONObject coverImage = media.getJSONObject("coverImage");

                        // Get episodes count (handle null case)
                        int episodes = media.isNull("episodes") ? 0 : media.getInt("episodes");

                        Anime anime = new Anime(
                                media.getInt("id"),
                                title.getString("romaji"),
                                title.optString("english", ""),
                                coverImage.getString("large"),
                                episodes  // Add episodes to constructor
                        );
                        searchResults.add(anime);
                    }

                    runOnUiThread(() -> {
                        animeList.clear();
                        animeList.addAll(searchResults);
                        animeAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(SearchAnimeActivity.this,
                                "Error parsing search results",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
    private void updateVisibility(boolean isSearching) {
        popularTitle.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        popularRecyclerView.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        searchResultsTitle.setVisibility(isSearching ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isSearching ? View.VISIBLE : View.GONE);
    }

    private void loadPopularAnime() {
        OkHttpClient client = new OkHttpClient();

        // GraphQL query for popular anime
        RequestBody requestBody = new FormBody.Builder()
                .add("query", "query { " +
                        "Page(page: 1, perPage: 10) { " +
                        "  media(sort: POPULARITY_DESC, type: ANIME) { " +  // Sort by popularity
                        "    id " +
                        "    title { " +
                        "      romaji " +
                        "      english " +
                        "    } " +
                        "    coverImage { " +
                        "      large " +
                        "    } " +
                        "    episodes " +
                        "  } " +
                        "} " +
                        "}")
                .build();

        Request request = new Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(SearchAnimeActivity.this,
                            "Failed to load popular anime: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray mediaList = jsonObject
                            .getJSONObject("data")
                            .getJSONObject("Page")
                            .getJSONArray("media");

                    final List<Anime> popularResults = new ArrayList<>();
                    for (int i = 0; i < mediaList.length(); i++) {
                        JSONObject media = mediaList.getJSONObject(i);
                        JSONObject title = media.getJSONObject("title");
                        JSONObject coverImage = media.getJSONObject("coverImage");

                        int episodes = media.isNull("episodes") ? 0 : media.getInt("episodes");

                        Anime anime = new Anime(
                                media.getInt("id"),
                                title.getString("romaji"),
                                title.optString("english", ""),
                                coverImage.getString("large"),
                                episodes
                        );
                        popularResults.add(anime);
                    }

                    runOnUiThread(() -> {
                        popularAnimeList.clear();
                        popularAnimeList.addAll(popularResults);
                        popularAnimeAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(SearchAnimeActivity.this,
                                "Error parsing popular anime results",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

}