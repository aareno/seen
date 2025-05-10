package com.aareno.seen.data;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.aareno.seen.data.TvMovies.ShowRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;

import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class EpisodeCountUpdateWorker extends Worker {

    private static final String ANI_LIST_API_URL = "https://graphql.anilist.co";
    private static final String TVMAZE_API_URL = "https://api.tvmaze.com/shows/";
    private final OkHttpClient client;
    private final Gson gson;

    private KDramaRepository kDramaRepository;
    private AnimeRepository animeRepository;
    private ShowRepository showRepository;

    public EpisodeCountUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        client = new OkHttpClient();
        gson = new Gson();
        kDramaRepository = new KDramaRepository(context);
        animeRepository = new AnimeRepository(context);
        showRepository = new ShowRepository(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("EpisodeUpdateWorker", "Starting to fetch episode count.");

        int contentId = getInputData().getInt("CONTENT_ID", -1);
        String type = getInputData().getString("CONTENT_TYPE");

        if (contentId == -1 || type == null) {
            Log.e("EpisodeUpdateWorker", "Invalid content data provided.");
            return Result.failure();
        }

        try {
            if ("anime".equals(type)) {
                fetchAnimeEpisodeCount(contentId);
            } else if ("kdrama".equals(type)) {
                fetchKdramaEpisodeCount(contentId);
            } else if ("show".equals(type)) {
                fetchShowEpisodeCount(contentId);
            } else {
                Log.e("EpisodeUpdateWorker", "Unsupported content type: " + type);
                return Result.failure();
            }
            Log.d("EpisodeUpdateWorker", "Episode count update successful.");
            return Result.success();
        } catch (Exception e) {
            Log.e("EpisodeUpdateWorker", "Error updating episode count", e);
            e.printStackTrace();
            return Result.retry(); // Retry on failure
        }
    }

    private void fetchAnimeEpisodeCount(int animeId) throws IOException {
        String query = "query ($id: Int) { Media(id: $id, type: ANIME) { id title { romaji } episodes } }";
        Map<String, Object> variables = new HashMap<>();
        variables.put("id", animeId);
        Log.d("EpisodeUpdateWorker", "Fetching anime data for ID: " + animeId);

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("query", query);
        jsonBody.add("variables", gson.toJsonTree(variables));

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(ANI_LIST_API_URL)
                .post(body)
                .build();

        // FIX: Use try-with-resources to ensure response is closed
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch anime data: " + response);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response body from AniList API");
            }

            String jsonResponse = responseBody.string();
            handleAnimeResponse(jsonResponse);
        }
    }

    private void fetchKdramaEpisodeCount(int showId) throws IOException {
        String episodeUrl = TVMAZE_API_URL + showId + "/episodes";

        Request request = new Request.Builder().url(episodeUrl).get().build();

        // FIX: Use try-with-resources to ensure response is closed
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Kdrama data: " + response);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response body from TVMaze API");
            }

            String jsonResponse = responseBody.string();
            try {
                JSONArray episodesArray = new JSONArray(jsonResponse);
                int newEpisodeCount = episodesArray.length();

                Log.d("EpisodeUpdateWorker", "Fetched episode count for KDrama ID: " + showId + ", New count: " + newEpisodeCount);

                // Use a latch to make the worker wait for the repository operation
                final CountDownLatch latch = new CountDownLatch(1);

                // Get current KDrama to check episode count
                kDramaRepository.updateKdramaEpisodeCount(showId, newEpisodeCount, new KDramaRepository.OnDataLoadedCallback<Void>() {
                    @Override
                    public void onDataLoaded(Void data) {
                        Log.d("EpisodeUpdateWorker", "Successfully checked/updated KDrama ID: " + showId);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (e.getMessage().contains("KDrama not found")) {
                            Log.e("EpisodeUpdateWorker", "KDrama ID: " + showId + " not found in repository");
                        } else {
                            Log.e("EpisodeUpdateWorker", "Failed to update KDrama episode count: " + e.getMessage(), e);
                        }
                        latch.countDown();
                    }
                });

                try {
                    latch.await(); // Wait for the repository operation to complete
                } catch (InterruptedException e) {
                    Log.e("EpisodeUpdateWorker", "Repository operation interrupted", e);
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }

            } catch (JSONException e) {
                Log.e("EpisodeUpdateWorker", "Error parsing Kdrama episodes JSON: " + e.getMessage(), e);
            }
        }
    }

    private void fetchShowEpisodeCount(int showId) throws IOException {
        String episodeUrl = TVMAZE_API_URL + showId + "/episodes";

        Request request = new Request.Builder().url(episodeUrl).get().build();

        // FIX: Use try-with-resources to ensure response is closed
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch Show data: " + response);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response body from TVMaze API");
            }

            String jsonResponse = responseBody.string();
            try {
                JSONArray episodesArray = new JSONArray(jsonResponse);
                int newEpisodeCount = episodesArray.length();

                Log.d("EpisodeUpdateWorker", "Fetched episode count for Show ID: " + showId + ", New count: " + newEpisodeCount);

                // Use a latch to make the worker wait for the repository operation
                final CountDownLatch latch = new CountDownLatch(1);

                // Get current KDrama to check episode count
                showRepository.updateShowEpisodeCount(showId, newEpisodeCount, new ShowRepository.OnDataLoadedCallback<Void>() {
                    @Override
                    public void onDataLoaded(Void data) {
                        Log.d("EpisodeUpdateWorker", "Successfully checked/updated Show ID: " + showId);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (e.getMessage().contains("Show not found")) {
                            Log.e("EpisodeUpdateWorker", "Show ID: " + showId + " not found in repository");
                        } else {
                            Log.e("EpisodeUpdateWorker", "Failed to update KDrama episode count: " + e.getMessage(), e);
                        }
                        latch.countDown();
                    }
                });

                try {
                    latch.await(); // Wait for the repository operation to complete
                } catch (InterruptedException e) {
                    Log.e("EpisodeUpdateWorker", "Repository operation interrupted", e);
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }

            } catch (JSONException e) {
                Log.e("EpisodeUpdateWorker", "Error parsing Show episodes JSON: " + e.getMessage(), e);
            }
        }
    }

    private void handleAnimeResponse(String jsonResponse) {
        JsonObject responseObject = gson.fromJson(jsonResponse, JsonObject.class);
        JsonObject data = responseObject.getAsJsonObject("data");

        // Add null check for data
        if (data == null) {
            Log.e("EpisodeUpdateWorker", "API response doesn't contain data field: " + jsonResponse);
            return;
        }

        JsonObject media = data.getAsJsonObject("Media");

        if (media != null) {
            // Check if episodes field exists and is not null
            JsonElement episodesElement = media.get("episodes");
            int newEpisodeCount = 0; // Default value

            if (episodesElement != null && !episodesElement.isJsonNull()) {
                newEpisodeCount = episodesElement.getAsInt();
            } else {
                // Episodes count is null or not present
                Log.d("EpisodeUpdateWorker", "Episodes count is null for this anime");
            }

            JsonObject titleObj = media.getAsJsonObject("title");
            if (titleObj == null) {
                Log.e("EpisodeUpdateWorker", "Title information missing in response");
                return;
            }

            JsonElement romajiElement = titleObj.get("romaji");
            if (romajiElement == null || romajiElement.isJsonNull()) {
                Log.e("EpisodeUpdateWorker", "Romaji title missing in response");
                return;
            }

            String title = romajiElement.getAsString();
            Log.d("EpisodeUpdateWorker", "Anime: " + title + ", New Episode Count: " + newEpisodeCount);

            // Only update if we have a valid episode count
            if (newEpisodeCount > 0) {
                updateAnimeEpisodeCountInRepository(title, newEpisodeCount);
            } else {
                Log.d("EpisodeUpdateWorker", "Skipping update for " + title + " due to invalid episode count: " + newEpisodeCount);
            }
        } else {
            Log.e("EpisodeUpdateWorker", "Media field missing in response: " + jsonResponse);
        }
    }

    private void updateAnimeEpisodeCountInRepository(String showId, int newEpisodeCount) {
        animeRepository.updateAnimeEpisodeCount(showId, newEpisodeCount, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d("EpisodeUpdateWorker", "Successfully updated Anime: " + showId + " with new episode count: " + newEpisodeCount);
            }

            @Override
            public void onError(Exception e) {
                Log.e("EpisodeUpdateWorker", "Failed to update Anime episode count: " + e.getMessage(), e);
            }
        });
    }
}