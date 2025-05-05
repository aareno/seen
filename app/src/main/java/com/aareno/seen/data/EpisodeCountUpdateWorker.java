package com.aareno.seen.data;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;

import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EpisodeCountUpdateWorker extends Worker {

    private static final String ANI_LIST_API_URL = "https://graphql.anilist.co";
    private static final String TVMAZE_API_URL = "https://api.tvmaze.com/shows/";
    private final OkHttpClient client;
    private final Gson gson;

    private KDramaRepository kDramaRepository;
    private AnimeRepository animeRepository;

    public EpisodeCountUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        client = new OkHttpClient();
        gson = new Gson();
        kDramaRepository = new KDramaRepository(context);
        animeRepository = new AnimeRepository(context);
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

        Response response = client.newCall(request).execute();
        if (response.isSuccessful() && response.body() != null) {
            String jsonResponse = response.body().string();
            handleAnimeResponse(jsonResponse);
        } else {
            throw new IOException("Failed to fetch anime data: " + response);
        }
    }

    private void fetchKdramaEpisodeCount(int showId) throws IOException {
        String episodeUrl = "https://api.tvmaze.com/shows/" + showId + "/episodes";

        Request request = new Request.Builder().url(episodeUrl).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();

                handleKdramaEpisodesResponse(jsonResponse, showId);
            } else {
                throw new IOException("Failed to fetch Kdrama data: " + response);
            }
        }
    }

    private void handleAnimeResponse(String jsonResponse) {
        JsonObject responseObject = gson.fromJson(jsonResponse, JsonObject.class);
        JsonObject data = responseObject.getAsJsonObject("data");
        JsonObject media = data.getAsJsonObject("Media");

        if (media != null) {
            int episodeCount = media.get("episodes").getAsInt();
            String title = media.getAsJsonObject("title").get("romaji").getAsString();
            System.out.println("Anime: " + title + ", Episode Count: " + episodeCount);
            // Update your local data storage or UI as needed
            updateAnimeEpisodeCountInRepository(title, episodeCount);
        }
    }

    private void handleKdramaEpisodesResponse(String jsonResponse, int showId) {
        try {
            // The TVmaze /episodes endpoint should return an array
            JSONArray episodesArray = new JSONArray(jsonResponse);
            int episodeCount = episodesArray.length();

            Log.d("EpisodeUpdateWorker", "Kdrama ID: " + showId + ", Episode Count: " + episodeCount);

            // Update or store episode count in local data storage
            updateKdramaEpisodeCountInRepository(showId, episodeCount);

        } catch (JSONException e) {
            Log.e("EpisodeUpdateWorker", "Error parsing Kdrama episodes JSON: " + e.getMessage(), e);
        }
    }

    private void updateKdramaEpisodeCountInRepository(int showId, int episodeCount) {
        kDramaRepository.updateKdramaEpisodeCount(showId, episodeCount, new KDramaRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d("EpisodeUpdateWorker", "Successfully updated KDrama ID: " + showId + " with new episode count: " + episodeCount);
            }

            @Override
            public void onError(Exception e) {
                Log.e("EpisodeUpdateWorker", "Failed to update KDrama episode count: " + e.getMessage(), e);
            }
        });
    }

    private void updateAnimeEpisodeCountInRepository(String showId, int episodeCount) {
        animeRepository.updateAnimeEpisodeCount(showId, episodeCount, new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d("EpisodeUpdateWorker", "Successfully updated Anime: " + showId + " with new episode count: " + episodeCount);
            }

            @Override
            public void onError(Exception e) {
                Log.e("EpisodeUpdateWorker", "Failed to update Anime episode count: " + e.getMessage(), e);
            }
        });
    }

}