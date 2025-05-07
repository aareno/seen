package com.aareno.seen.ui.Anime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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

    // Adult content filter setting
    private boolean showAdultContent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);

        // Load adult content filter setting
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        showAdultContent = prefs.getBoolean("show_adult_content", false);
        Log.d(TAG, "Adult content filter: " + (showAdultContent ? "OFF" : "ON"));

        initializeViews();
        setupRecyclerViews();
        setupSearchListeners();
        loadPopularAnime();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);
        popularRecyclerView = findViewById(R.id.popular_recycler_view);
        searchResultsTitle = findViewById(R.id.search_results_title);
        popularTitle = findViewById(R.id.popular_title);
        animeList = new ArrayList<>();
        popularAnimeList = new ArrayList<>();

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        AnimeSearchAdapter.OnItemClickListener listener = (anime, listType) -> {
            try {
                Log.d(TAG, "Adding anime: " + anime.getTitleRomaji() + " to " + listType);

                // Set watching status based on selection
                anime.setWatching("Watching".equals(listType));

                selectedAnimeList.add(anime);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_anime_list", new ArrayList<>(selectedAnimeList));
                setResult(RESULT_OK, resultIntent);

                Toast.makeText(SearchAnimeActivity.this,
                        "Added " + anime.getTitleEnglish() + " to " + listType.toLowerCase() + " list",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error adding anime to " + listType.toLowerCase() + " list", e);
                Toast.makeText(SearchAnimeActivity.this,
                        "Error adding anime: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        animeAdapter = new AnimeSearchAdapter(animeList, listener);
        popularAnimeAdapter = new AnimeSearchAdapter(popularAnimeList, listener);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(animeAdapter);

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        popularRecyclerView.setAdapter(popularAnimeAdapter);
    }

    private void setupSearchListeners() {
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

        searchButton.setOnClickListener(v -> {
            searchEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                searchAnime(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void searchAnime(String query) {
        OkHttpClient client = new OkHttpClient();

        // Modified GraphQL query to include isAdult filter
        String graphqlQuery = "query ($search: String, $isAdult: Boolean) { " +
                "Page(page: 1, perPage: 10) { " +
                "  media(search: $search, type: ANIME, isAdult: $isAdult) { " +
                "    id " +
                "    title { " +
                "      romaji " +
                "      english " +
                "    } " +
                "    coverImage { " +
                "      large " +
                "    } " +
                "    episodes " +
                "    status " +
                "    isAdult " +
                "    startDate { " +
                "      year " +
                "      month " +
                "      day " +
                "    } " +
                "    endDate { " +
                "      year " +
                "      month " +
                "      day " +
                "    } " +
                "    airingSchedule { " +
                "      nodes { " +
                "        airingAt " +
                "        timeUntilAiring " +
                "        episode " +
                "        airingAt " +
                "      } " +
                "    } " +
                "  } " +
                "} " +
                "}";

        // Create JSON object for variables
        JSONObject variables = new JSONObject();
        try {
            variables.put("search", query);
            // If showAdultContent is false, explicitly filter out adult content
            // If showAdultContent is true, we don't specify isAdult to get all content
            if (!showAdultContent) {
                variables.put("isAdult", false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("query", graphqlQuery)
                .add("variables", variables.toString())
                .build();

        Request request = new Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        Log.d(TAG, "Sending search request with adult filter: " + (!showAdultContent));

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchAnimeActivity.this,
                        "Search failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleAnimeResponse(response, animeList, animeAdapter);
            }
        });
    }

    private void loadPopularAnime() {
        OkHttpClient client = new OkHttpClient();

        // Modified GraphQL query to include isAdult filter
        String graphqlQuery = "query ($isAdult: Boolean) { " +
                "Page(page: 1, perPage: 10) { " +
                "  media(sort: POPULARITY_DESC, type: ANIME, isAdult: $isAdult) { " +
                "    id " +
                "    title { " +
                "      romaji " +
                "      english " +
                "    } " +
                "    coverImage { " +
                "      large " +
                "    } " +
                "    episodes " +
                "    status " +
                "    isAdult " +
                "    startDate { " +
                "      year " +
                "      month " +
                "      day " +
                "    } " +
                "    endDate { " +
                "      year " +
                "      month " +
                "      day " +
                "    } " +
                "    airingSchedule { " +
                "      nodes { " +
                "        airingAt " +
                "        timeUntilAiring " +
                "        episode " +
                "        airingAt " +
                "      } " +
                "    } " +
                "  } " +
                "} " +
                "}";

        // Create JSON object for variables
        JSONObject variables = new JSONObject();
        try {
            // If showAdultContent is false, explicitly filter out adult content
            if (!showAdultContent) {
                variables.put("isAdult", false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("query", graphqlQuery)
                .add("variables", variables.toString())
                .build();

        Request request = new Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        Log.d(TAG, "Loading popular anime with adult filter: " + (!showAdultContent));

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchAnimeActivity.this,
                        "Failed to load popular anime: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleAnimeResponse(response, popularAnimeList, popularAnimeAdapter);
            }
        });
    }

    private void handleAnimeResponse(Response response, List<Anime> animeList,
                                     AnimeSearchAdapter adapter) throws IOException {
        try {
            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONArray mediaList = jsonObject
                    .getJSONObject("data")
                    .getJSONObject("Page")
                    .getJSONArray("media");

            List<Anime> results = new ArrayList<>();
            for (int i = 0; i < mediaList.length(); i++) {
                JSONObject media = mediaList.getJSONObject(i);
                Anime anime = parseAnimeFromJson(media);
                results.add(anime);
            }

            runOnUiThread(() -> {
                animeList.clear();
                animeList.addAll(results);
                adapter.notifyDataSetChanged();
            });

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(SearchAnimeActivity.this,
                    "Error parsing results",
                    Toast.LENGTH_SHORT).show());
        }
    }

    private Anime parseAnimeFromJson(JSONObject media) throws JSONException {
        JSONObject title = media.getJSONObject("title");
        JSONObject coverImage = media.getJSONObject("coverImage");
        int episodes = media.isNull("episodes") ? 12 : media.getInt("episodes");

        // Get isAdult value if present
        boolean isAdult = media.optBoolean("isAdult", false);

        // Parse dates
        JSONObject startDateObj = media.getJSONObject("startDate");
        JSONObject endDateObj = media.getJSONObject("endDate");

        // Create Calendar instances for dates
        Calendar startCal = Calendar.getInstance();
        Log.d("SearchAnimeActivity", "Anime: " + title);
        Log.d("SearchAnimeActivity", "StartDate : " + startDateObj.optInt("year", 0) + " " + startDateObj.optInt("month", 0));
        if (!startDateObj.isNull("year") && !startDateObj.isNull("month")) {
            startCal.set(
                    startDateObj.getInt("year"),
                    startDateObj.getInt("month") - 1,
                    startDateObj.optInt("day", 1)
            );
        }

        Calendar endCal = Calendar.getInstance();
        if (!endDateObj.isNull("year") && !endDateObj.isNull("month")) {
            endCal.set(
                    endDateObj.getInt("year"),
                    endDateObj.getInt("month") - 1,
                    endDateObj.optInt("day", 1)
            );
        } else {
            endCal.setTime(startCal.getTime()); // Copy startCal date
            endCal.add(Calendar.WEEK_OF_YEAR, 12); // Add 12 weeks
        }

        Log.d(TAG, "End date: " + endCal.getTime());

        // Parse airing schedule
        List<Integer> airingDays = new ArrayList<>();
        if (!media.isNull("airingSchedule") && !media.getJSONObject("airingSchedule").isNull("nodes")) {
            JSONArray nodes = media.getJSONObject("airingSchedule")
                    .getJSONArray("nodes");

            for (int i = 0; i < nodes.length(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                long airingAt = node.getLong("airingAt") * 1000; // Convert to milliseconds
                Calendar airDate = Calendar.getInstance();
                airDate.setTimeInMillis(airingAt);

                // Convert day of week to your format (1 = Monday, 7 = Sunday)
                int dayOfWeek = airDate.get(Calendar.DAY_OF_WEEK);
                dayOfWeek = dayOfWeek == 1 ? 7 : dayOfWeek - 1;

                if (!airingDays.contains(dayOfWeek)) {
                    airingDays.add(dayOfWeek);
                }
            }
        }

        Log.d(TAG, "Airing days: " + airingDays);

        // Create and return the Anime object
        Anime anime = new Anime(
                media.getInt("id"),
                title.getString("romaji"),
                title.optString("english", title.getString("romaji")), // Use romaji if English title is missing
                coverImage.getString("large"),
                episodes,
                airingDays,
                startCal.getTime(),
                endCal.getTime()
        );

        // Set the mature flag
        anime.setMature(isAdult);

        // If the anime is mature, log it (for debugging)
        if (isAdult) {
            Log.d(TAG, "Found adult anime: " + anime.getTitleRomaji());
        }

        // Update the airing status
        anime.updateAiringStatus();

        return anime;
    }

    private void updateVisibility(boolean isSearching) {
        popularTitle.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        popularRecyclerView.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        searchResultsTitle.setVisibility(isSearching ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isSearching ? View.VISIBLE : View.GONE);
    }
}