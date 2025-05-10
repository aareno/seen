package com.aareno.seen.ui.TvMovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchShowActivity extends AppCompatActivity {
    private static final String TAG = "SearchShowActivity";

    // Searching
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView recyclerView;
    private ShowSearchAdapter showAdapter;
    private List<Show> showList;
    private List<Show> selectedShowList = new ArrayList<>();

    // Latest Releases
    private RecyclerView latestRecyclerView;
    private ShowSearchAdapter latestShowAdapter;
    private List<Show> latestShowList = new ArrayList<>();
    private TextView searchResultsTitle;
    private TextView latestTitle;

    private boolean showAdultContent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);

        // Get adult content filter setting from intent or SharedPreferences
        showAdultContent = getIntent().getBooleanExtra("show_adult_content", false);
        if (!getIntent().hasExtra("show_adult_content")) {
            // Fallback to SharedPreferences if not passed in intent
            SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
            showAdultContent = prefs.getBoolean("show_adult_content", false);
        }

        Log.d(TAG, "Adult content filter: " + (showAdultContent ? "OFF" : "ON"));

        initializeViews();
        setupRecyclerViews();
        setupSearchListeners();
        loadLatestShows();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);
        latestRecyclerView = findViewById(R.id.popular_recycler_view);
        searchResultsTitle = findViewById(R.id.search_results_title);
        latestTitle = findViewById(R.id.popular_title);
        showList = new ArrayList<>();
        latestShowList = new ArrayList<>();

        // Update titles to reflect Shows instead of Anime
        if (searchResultsTitle != null) {
            searchResultsTitle.setText("Search Results");
        }
        if (latestTitle != null) {
            latestTitle.setText("Latest Shows");
        }

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        ShowSearchAdapter.OnItemClickListener listener = (show, listType) -> {
            try {
                Log.d(TAG, "Adding Show: " + show.getTitleEnglish() + " to " + listType);
                show.setWatching("Watching".equals(listType));
                selectedShowList.add(show);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_show_list", new ArrayList<>(selectedShowList));
                setResult(RESULT_OK, resultIntent);

                Toast.makeText(SearchShowActivity.this,
                        "Added " + show.getTitleEnglish() + " to watching list",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error adding Show to watching list", e);
                Toast.makeText(SearchShowActivity.this,
                        "Error adding Show: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        showAdapter = new ShowSearchAdapter(showList, listener);
        latestShowAdapter = new ShowSearchAdapter(latestShowList, listener);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(showAdapter);

        latestRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        latestRecyclerView.setAdapter(latestShowAdapter);
    }

    private void setupSearchListeners() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateVisibility(!s.toString().isEmpty());
            }
            @Override public void afterTextChanged(Editable s) {}
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
                searchShows(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void searchShows(String query) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.tvmaze.com/search/shows?q=" + Uri.encode(query);
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchShowActivity.this,
                        "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray searchArray = new JSONArray(responseBody);
                    final List<Show> results = new ArrayList<>();
                    int totalShows = 0;
                    int filteredShows = 0;

                    for (int i = 0; i < searchArray.length(); i++) {
                        JSONObject showObject = searchArray.getJSONObject(i).getJSONObject("show");
                        totalShows++;

                        // Check for mature content
                        boolean isMature = isMatureContent(showObject);

                        // Skip mature content if filter is on
                        if (isMature && !showAdultContent) {
                            Log.d(TAG, "Filtered out mature Show: " + showObject.optString("name"));
                            filteredShows++;
                            continue;
                        }

                        Show show = parseShowFromJson(showObject);
                        // Set the mature flag
                        show.setMature(isMature);
                        results.add(show);
                    }

                    final int finalTotalShows = totalShows;
                    final int finalFilteredShows = filteredShows;

                    runOnUiThread(() -> {
                        showList.clear();
                        showList.addAll(results);
                        showAdapter.notifyDataSetChanged();

                        // Log filtering statistics
                        if (finalFilteredShows > 0) {
                            Log.d(TAG, "Filtered " + finalFilteredShows + " out of " + finalTotalShows + " Shows");
                        }

                        // Show message if no results after filtering
                        if (results.isEmpty()) {
                            Toast.makeText(SearchShowActivity.this,
                                    "No results found" + (!showAdultContent ? " (adult content filtered)" : ""),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(SearchShowActivity.this,
                            "Error parsing results", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loadLatestShows() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        String url = "https://api.tvmaze.com/schedule?country=US"; // You can generalize this if needed

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchShowActivity.this,
                        "Failed to load latest shows: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray array = new JSONArray(responseBody);
                    final List<Show> results = new ArrayList<>();
                    int totalShows = 0;
                    int filteredShows = 0;

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject showObject = array.getJSONObject(i).getJSONObject("show");
                        // Include only scripted TV shows
                        String type = showObject.optString("type", "");
                        if (!"Scripted".equalsIgnoreCase(type)) {
                            continue; // Skip if not a scripted TV show
                        }

                        totalShows++;

                        // Check for mature content
                        boolean isMature = isMatureContent(showObject);

                        // Skip mature content if filter is on
                        if (isMature && !showAdultContent) {
                            Log.d(TAG, "Filtered out mature Show: " + showObject.optString("name"));
                            filteredShows++;
                            continue;
                        }

                        Show show = parseShowFromJson(showObject);
                        // Set the mature flag
                        show.setMature(isMature);
                        results.add(show);
                    }

                    final int finalTotalShows = totalShows;
                    final int finalFilteredShows = filteredShows;

                    runOnUiThread(() -> {
                        latestShowList.clear();
                        latestShowList.addAll(results);
                        latestShowAdapter.notifyDataSetChanged();

                        // Log filtering statistics
                        if (finalFilteredShows > 0) {
                            Log.d(TAG, "Filtered " + finalFilteredShows + " out of " + finalTotalShows + " Shows");
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(SearchShowActivity.this,
                            "Error parsing results", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // Helper method to determine if content is mature
    private boolean isMatureContent(JSONObject showObject) {
// If there's an IMDb ID, check with OMDb
        if (showObject.has("externals")) {
            JSONObject externals = showObject.optJSONObject("externals");
            if (externals != null) {
                String imdbId = externals.optString("imdb");
                String omdbRating = fetchOMDbRating(imdbId);
                if (omdbRating != null) {
// Common adult ratings:
                    List adultRatings = Arrays.asList("R", "NC-17", "TV-MA", "X", "UNRATED");
                    if (adultRatings.contains(omdbRating.toUpperCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String fetchOMDbRating(String imdbId) {
        if (imdbId == null || imdbId.isEmpty()) {
            return null;
        }
        OkHttpClient client = new OkHttpClient();

// Construct your OMDb query URL
// (Replace YOUR_OMDB_API_KEY with an actual key you obtain from https://www.omdbapi.com/)
        String url = "https://www.omdbapi.com/?i=" + imdbId + "&apikey=18f70c59";

        Request request = new Request.Builder().url(url).get().build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                JSONObject omdbJson = new JSONObject(body);
                // The "Rated" field in OMDb might look like "PG-13", "R", "TV-MA", etc.
                return omdbJson.optString("Rated", "N/A");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Show parseShowFromJson(JSONObject show) throws JSONException {
        int id = show.getInt("id");
        String titleEnglish = show.getString("name");
        String imageUrl = null;
        if (show.has("image") && !show.isNull("image")) {
            imageUrl = show.getJSONObject("image").getString("medium");
        }

        Date startDate = parseDate(show.optString("premiered"));
        Date endDate = parseDate(show.optString("ended"));

        List<Integer> airingDays = new ArrayList<>();
        if (show.has("schedule") && show.getJSONObject("schedule").has("days")) {
            JSONArray daysArray = show.getJSONObject("schedule").getJSONArray("days");
            for (int i = 0; i < daysArray.length(); i++) {
                String day = daysArray.getString(i);
                airingDays.add(convertDayToInteger(day));
            }
        }

        Show parsedShow = new Show(id, titleEnglish, titleEnglish, imageUrl, airingDays, startDate, endDate);
        parsedShow.updateAiringStatus();

        fetchEpisodeCount(parsedShow);

        return parsedShow;
    }

    private void fetchEpisodeCount(Show show) {
        OkHttpClient client = new OkHttpClient();
        String episodeUrl = "https://api.tvmaze.com/shows/" + show.getId() + "/episodes";

        Request request = new Request.Builder().url(episodeUrl).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching episodes: " + e.getMessage(), e);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray episodesArray = new JSONArray(responseBody);
                    int episodeCount = episodesArray.length();
                    show.setEpisodeCount(episodeCount);

                    runOnUiThread(() -> {
                        latestShowAdapter.notifyDataSetChanged();
                        showAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing episodes JSON: " + e.getMessage(), e);
                }
            }
        });
    }

    // Fixed method with null checks
    private Date parseDate(String dateString) {
        if (dateString == null || dateString.equals("null") || dateString.isEmpty()) {
            return null; // Return null for invalid dates
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return format.parse(dateString);
        } catch (ParseException e) {
            Log.w("SearchKDramaActivity", "Could not parse date: " + dateString, e);
            return null; // Return null on parsing failure
        }
    }

    private int convertDayToInteger(String day) {
        switch (day.toLowerCase()) {
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            case "sunday": return Calendar.SUNDAY;
            default: return -1;
        }
    }

    private void updateVisibility(boolean isSearching) {
        latestTitle.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        latestRecyclerView.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        searchResultsTitle.setVisibility(isSearching ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isSearching ? View.VISIBLE : View.GONE);
    }
}