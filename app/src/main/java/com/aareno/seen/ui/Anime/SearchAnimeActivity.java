package com.aareno.seen.ui.Anime;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SearchAnimeActivity extends AppCompatActivity {
    private static final String TAG = "SearchAnimeActivity";
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView recyclerView;
    private AnimeSearchAdapter animeAdapter;
    private List<Anime> animeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);

        // Initialize views
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);

        // Initialize anime list
        animeList = new ArrayList<>();

        // Setup RecyclerView
        animeAdapter = new AnimeSearchAdapter(animeList, new AnimeSearchAdapter.OnItemClickListener() {
            @Override
            public void onAddToWatchingClick(Anime anime) {
                try {
                    // Log the anime details
                    Log.d(TAG, "Adding anime: " + anime.getTitleRomaji());
                    Log.d(TAG, "Anime ID: " + anime.getId());
                    Log.d(TAG, "Anime Cover URL: " + anime.getCoverImageUrl());

                    // Create an intent to return the selected anime
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("selected_anime", anime);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // Close the search activity
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

        // Search button click listener
        searchButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString();
            searchAnime(query);
        });
    }

    private void searchAnime(String query) {
        OkHttpClient client = new OkHttpClient();

        // Prepare GraphQL query
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

                        Anime anime = new Anime(
                                media.getInt("id"),
                                title.getString("romaji"),
                                title.optString("english", ""),
                                coverImage.getString("large")
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
}