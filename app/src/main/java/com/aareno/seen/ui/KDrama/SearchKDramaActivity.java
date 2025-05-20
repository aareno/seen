package com.aareno.seen.ui.KDrama;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aareno.seen.R;
import com.bumptech.glide.Glide;

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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;
import android.graphics.PorterDuff;
import android.content.res.Configuration;

public class SearchKDramaActivity extends AppCompatActivity {
    private static final String TAG = "SearchKDramaActivity";
    private static final long SEARCH_DEBOUNCE_DELAY = 500; // 500ms delay
    private Handler searchHandler;
    private Runnable searchRunnable;

    // Searching
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView recyclerView;
    private KDramaSearchAdapter kDramaAdapter;
    private List<KDrama> kDramaList;
    private List<KDrama> selectedKDramaList = new ArrayList<>();

    // Latest Releases
    private RecyclerView latestRecyclerView;
    private KDramaSearchAdapter latestKDramaAdapter;
    private List<KDrama> latestKDramaList = new ArrayList<>();
    private TextView searchResultsTitle;
    private TextView latestTitle;

    // Adult content filter
    private boolean showAdultContent = false;

    private View loadingContainer;
    private ImageView loadingIndicator;

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
        loadLatestKDramas();
    }

    private void initializeViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);
        latestRecyclerView = findViewById(R.id.popular_recycler_view);
        searchResultsTitle = findViewById(R.id.search_results_title);
        latestTitle = findViewById(R.id.popular_title);
        loadingContainer = findViewById(R.id.loading_container);
        loadingIndicator = findViewById(R.id.loading_indicator);
        
        // Set up loading indicator color based on theme
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            // Dark theme - set white color
            loadingIndicator.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_IN);
        } else {
            // Light theme - set black color
            loadingIndicator.setColorFilter(ContextCompat.getColor(this, R.color.black), PorterDuff.Mode.SRC_IN);
        }

        kDramaList = new ArrayList<>();
        latestKDramaList = new ArrayList<>();

        // Update titles to reflect K-Drama instead of Anime
        if (searchResultsTitle != null) {
            searchResultsTitle.setText("Search Results");
        }
        if (latestTitle != null) {
            latestTitle.setText("Latest K-Dramas");
        }

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        KDramaSearchAdapter.OnItemClickListener listener = (kdrama, listType) -> {
            try {
                Log.d(TAG, "Adding KDrama: " + kdrama.getTitleEnglish() + " to " + listType);
                kdrama.setWatching("Watching".equals(listType));
                selectedKDramaList.add(kdrama);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selected_kdrama_list", new ArrayList<>(selectedKDramaList));
                setResult(RESULT_OK, resultIntent);

                Toast.makeText(SearchKDramaActivity.this,
                        "Added " + kdrama.getTitleEnglish() + " to " + listType.toLowerCase() + " list",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error adding KDrama to " + listType.toLowerCase() + " list", e);
                Toast.makeText(SearchKDramaActivity.this,
                        "Error adding KDrama: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        kDramaAdapter = new KDramaSearchAdapter(kDramaList, listener);
        latestKDramaAdapter = new KDramaSearchAdapter(latestKDramaList, listener);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(kDramaAdapter);

        latestRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        latestRecyclerView.setAdapter(latestKDramaAdapter);
    }

    private void setupSearchListeners() {
        searchHandler = new Handler(Looper.getMainLooper());
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateVisibility(!s.toString().isEmpty());
                
                // Cancel any pending searches
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                final String query = s.toString().trim();
                if (!query.isEmpty()) {
                    // Create new search runnable
                    searchRunnable = () -> searchKDrama(query);
                    // Post delayed to debounce
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
                } else {
                    // Clear results if search is empty
                    kDramaList.clear();
                    kDramaAdapter.notifyDataSetChanged();
                }
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
                searchKDrama(searchEditText.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove any pending callbacks to prevent memory leaks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    private void showLoading() {
        runOnUiThread(() -> {
            Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .into(loadingIndicator);

            loadingContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            latestRecyclerView.setVisibility(View.GONE);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            Glide.with(this).clear(loadingIndicator); // Stop GIF
            loadingContainer.setVisibility(View.GONE);
            updateVisibility(!searchEditText.getText().toString().trim().isEmpty());
        });
    }

    private void searchKDrama(String query) {
        showLoading();
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.tvmaze.com/search/shows?q=" + Uri.encode(query);
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                hideLoading();
                runOnUiThread(() -> Toast.makeText(SearchKDramaActivity.this,
                        "Search failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray searchArray = new JSONArray(responseBody);
                    final List<KDrama> results = new ArrayList<>();

                    for (int i = 0; i < searchArray.length(); i++) {
                        JSONObject showObject = searchArray.getJSONObject(i).getJSONObject("show");
                        if ("Korean".equalsIgnoreCase(showObject.optString("language"))) {
                            // Check for mature content
                            boolean isMature = isMatureContent(showObject);

                            // Skip mature content if filter is on
                            if (isMature && !showAdultContent) {
                                Log.d(TAG, "Filtered out mature K-Drama: " + showObject.optString("name"));
                                continue;
                            }

                            KDrama kdrama = parseKDramaFromJson(showObject);
                            // Set the mature flag
                            kdrama.setMature(isMature);
                            results.add(kdrama);
                        }
                    }

                    runOnUiThread(() -> {
                        kDramaList.clear();
                        kDramaList.addAll(results);
                        kDramaAdapter.notifyDataSetChanged();

                        // Show message if no results after filtering
                        if (results.isEmpty()) {
                            Toast.makeText(SearchKDramaActivity.this,
                                    "No results found" + (!showAdultContent ? " (adult content filtered)" : ""),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(SearchKDramaActivity.this,
                            "Error parsing results",
                            Toast.LENGTH_SHORT).show());
                } finally {
                    hideLoading();
                }
            }
        });
    }

    private void loadLatestKDramas() {
        showLoading();
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.tvmaze.com/schedule?country=KR";

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                hideLoading();
                runOnUiThread(() -> Toast.makeText(SearchKDramaActivity.this,
                        "Failed to load latest K-Dramas: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray array = new JSONArray(responseBody);
                    final List<KDrama> results = new ArrayList<>();
                    int totalShows = 0;
                    int filteredShows = 0;

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject showObject = array.getJSONObject(i).getJSONObject("show");
                        if ("Korean".equalsIgnoreCase(showObject.optString("language"))) {
                            totalShows++;

                            // Check for mature content
                            boolean isMature = isMatureContent(showObject);

                            // Skip mature content if filter is on
                            if (isMature && !showAdultContent) {
                                Log.d(TAG, "Filtered out mature K-Drama: " + showObject.optString("name"));
                                filteredShows++;
                                continue;
                            }

                            KDrama kdrama = parseKDramaFromJson(showObject);
                            // Set the mature flag
                            kdrama.setMature(isMature);
                            results.add(kdrama);
                        }
                    }

                    final int finalTotalShows = totalShows;
                    final int finalFilteredShows = filteredShows;

                    runOnUiThread(() -> {
                        latestKDramaList.clear();
                        latestKDramaList.addAll(results);
                        latestKDramaAdapter.notifyDataSetChanged();

                        // Log filtering statistics
                        if (finalFilteredShows > 0) {
                            Log.d(TAG, "Filtered " + finalFilteredShows + " out of " + finalTotalShows + " K-Dramas");
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(SearchKDramaActivity.this,
                            "Error parsing results",
                            Toast.LENGTH_SHORT).show());
                } finally {
                    hideLoading();
                }
            }
        });
    }

    // Helper method to determine if content is mature
    private boolean isMatureContent(JSONObject showObject) {
        try {
            // If there's an IMDb ID, check with OMDb
            if (showObject.has("externals")) {
                JSONObject externals = showObject.optJSONObject("externals");
                if (externals != null) {
                    String imdbId = externals.optString("imdb");
                    if (imdbId != null && !imdbId.isEmpty()) {
                        String omdbRating = fetchOMDbRating(imdbId);
                        if (omdbRating != null) {
                            // Common adult ratings
                            List<String> adultRatings = Arrays.asList("R", "NC-17", "TV-MA", "X", "UNRATED");
                            if (adultRatings.contains(omdbRating.toUpperCase())) {
                                return true;
                            }
                        }
                    }
                }
            }

            // Fallback: Check if the show's summary or genre contains mature content indicators
            String summary = showObject.optString("summary", "").toLowerCase();
            JSONArray genres = showObject.optJSONArray("genres");
            
            // Keywords that might indicate mature content
            List<String> matureKeywords = Arrays.asList("mature", "adult", "violence", "gore", "sexual");
            
            // Check summary for mature keywords
            for (String keyword : matureKeywords) {
                if (summary.contains(keyword)) {
                    return true;
                }
            }

            // Check genres for mature indicators
            if (genres != null) {
                for (int i = 0; i < genres.length(); i++) {
                    String genre = genres.getString(i).toLowerCase();
                    if (genre.contains("adult") || genre.contains("mature")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking mature content: " + e.getMessage());
        }
        
        return false;
    }

    private String fetchOMDbRating(String imdbId) {
        if (imdbId == null || imdbId.isEmpty()) {
            return null;
        }

        // Check network connectivity first
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network connection available");
            return null;
        }

        // Create OkHttpClient with timeouts
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        // Construct your OMDb query URL with HTTPS
        String url = "https://www.omdbapi.com/?i=" + imdbId + "&apikey=18f70c59";

        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            // Make the request asynchronously
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                JSONObject omdbJson = new JSONObject(body);
                return omdbJson.optString("Rated", "N/A");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error fetching OMDB rating: " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing OMDB response: " + e.getMessage());
        }
        return null;
    }

    private KDrama parseKDramaFromJson(JSONObject show) throws JSONException {
        int id = show.getInt("id");
        String titleEnglish = show.getString("name");
        String imageUrl = null;
        if (show.has("image") && !show.isNull("image")) {
            imageUrl = show.getJSONObject("image").getString("medium");
        }

        // Extract dates
        Date startDate = parseDate(show.optString("premiered"));
        Date endDate = parseDate(show.optString("ended"));

        // Airing days can be calculated from airing schedule if available
        List<Integer> airingDays = new ArrayList<>();
        if (show.has("schedule") && show.getJSONObject("schedule").has("days")) {
            JSONArray daysArray = show.getJSONObject("schedule").getJSONArray("days");
            for (int i = 0; i < daysArray.length(); i++) {
                String day = daysArray.getString(i);
                airingDays.add(convertDayToInteger(day));
            }
        }

        KDrama kdrama = new KDrama(id, titleEnglish, titleEnglish, imageUrl, airingDays, startDate, endDate);
        kdrama.updateAiringStatus();

        fetchEpisodeCount(kdrama);

        return kdrama;
    }

    private void fetchEpisodeCount(KDrama kdrama) {
        OkHttpClient client = new OkHttpClient();
        String episodeUrl = "https://api.tvmaze.com/shows/" + kdrama.getId() + "/episodes";

        Request request = new Request.Builder().url(episodeUrl).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Error fetching episodes: " + e.getMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray episodesArray = new JSONArray(responseBody);
                    int episodeCount = episodesArray.length();
                    kdrama.setEpisodeCount(episodeCount);

                    runOnUiThread(() -> {
                        // Notify adapters or update UI if necessary
                        latestKDramaAdapter.notifyDataSetChanged();
                        kDramaAdapter.notifyDataSetChanged();
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
            case "monday":    return 1; // Calendar.MONDAY adjusted to your format
            case "tuesday":   return 2; // Calendar.TUESDAY adjusted to your format
            case "wednesday": return 3; // Calendar.WEDNESDAY adjusted to your format
            case "thursday":  return 4; // Calendar.THURSDAY adjusted to your format
            case "friday":    return 5; // Calendar.FRIDAY adjusted to your format
            case "saturday":  return 6; // Calendar.SATURDAY adjusted to your format
            case "sunday":    return 7; // Calendar.SUNDAY adjusted to your format
            default: return -1;
        }
    }

    private void updateVisibility(boolean isSearching) {
        latestTitle.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        latestRecyclerView.setVisibility(isSearching ? View.GONE : View.VISIBLE);
        searchResultsTitle.setVisibility(isSearching ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isSearching ? View.VISIBLE : View.GONE);
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager connectivityManager = (android.net.ConnectivityManager) 
            getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}