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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchKDramaActivity extends AppCompatActivity {
    private static final String TAG = "SearchKDramaActivity";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);

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
        kDramaList = new ArrayList<>();
        latestKDramaList = new ArrayList<>();

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        KDramaSearchAdapter.OnItemClickListener listener = kdrama -> {
            try {
                Log.d(TAG, "Adding KDrama: " + kdrama.getTitleEnglish());
                selectedKDramaList.add(kdrama);

                Intent resultIntent = new Intent();
                kdrama.setWatching(true);
                resultIntent.putExtra("selected_kdrama_list", new ArrayList<>(selectedKDramaList));
                setResult(RESULT_OK, resultIntent);

                Toast.makeText(SearchKDramaActivity.this,
                        "Added " + kdrama.getTitleEnglish() + " to watching list",
                        Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error adding KDrama to watching list", e);
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
                searchKDrama(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void searchKDrama(String query) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.tvmaze.com/search/shows?q=" + Uri.encode(query);
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
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
                            KDrama kdrama = parseKDramaFromJson(showObject);
                            results.add(kdrama);
                        }
                    }

                    runOnUiThread(() -> {
                        kDramaList.clear();
                        kDramaList.addAll(results);
                        kDramaAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(SearchKDramaActivity.this,
                            "Error parsing results",
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loadLatestKDramas() {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.tvmaze.com/schedule?country=KR";

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(SearchKDramaActivity.this,
                        "Failed to load latest KDramas: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    JSONArray array = new JSONArray(responseBody);
                    final List<KDrama> results = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject showObject = array.getJSONObject(i).getJSONObject("show");
                        if ("Korean".equalsIgnoreCase(showObject.optString("language"))) {
                            KDrama kdrama = parseKDramaFromJson(showObject);
                            results.add(kdrama);
                        }
                    }

                    runOnUiThread(() -> {
                        latestKDramaList.clear();
                        latestKDramaList.addAll(results);
                        latestKDramaAdapter.notifyDataSetChanged();
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(SearchKDramaActivity.this,
                            "Error parsing results",
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
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

    private Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int convertDayToInteger(String day) {
        switch (day.toLowerCase()) {
            case "monday":    return Calendar.MONDAY;
            case "tuesday":   return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday":  return Calendar.THURSDAY;
            case "friday":    return Calendar.FRIDAY;
            case "saturday":  return Calendar.SATURDAY;
            case "sunday":    return Calendar.SUNDAY;
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