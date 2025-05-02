package com.aareno.seen.ui.KDrama;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_anime);

        // Initialize views
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        recyclerView = findViewById(R.id.search_recycler_view);

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> onBackPressed());

        // Initialize kdrama list
        kDramaList = new ArrayList<>();

        // Setup RecyclerView
        kDramaAdapter = new KDramaSearchAdapter(kDramaList, new KDramaSearchAdapter.OnItemClickListener() {
            @Override
            public void onAddToWatchingClick(KDrama kdrama) {
                try {
                    Log.d(TAG, "Adding KDrama: " + kdrama.getTitleEnglish());
                    Log.d(TAG, "KDrama ID: " + kdrama.getId());
                    Log.d(TAG, "KDrama Image URL: " + kdrama.getCoverImageUrl());

                    Intent resultIntent = new Intent();
                    kdrama.setWatching(true);
                    resultIntent.putExtra("selected_kdrama", kdrama);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error adding kdrama to watching list", e);
                    Toast.makeText(SearchKDramaActivity.this,
                            "Error adding kdrama: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(kDramaAdapter);

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
                runOnUiThread(() -> {
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
                            Log.d(TAG, "Title: " + show);

                            String imageUrl = null;
                            JSONObject imageObj = show.getJSONObject("image");
                            imageUrl = imageObj.optString("medium", null);
                            Log.d(TAG, "Image URL: " + imageUrl);

                            KDrama kdrama = new KDrama(
                                    id,
                                    title,
                                    title,
                                    imageUrl
                            );
                            kdramas.add(kdrama);
                        }
                    }

                    runOnUiThread(() -> {
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
                    runOnUiThread(() -> {
                        Toast.makeText(SearchKDramaActivity.this,
                                "Error parsing search results",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}