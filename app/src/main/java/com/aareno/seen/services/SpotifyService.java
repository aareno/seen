package com.aareno.seen.services;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

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

public class SpotifyService {
    private static final String TAG = "SpotifyService";
    private static final String CLIENT_ID = "0a25702927cf443694296cdf4b11ae4d";
    private static final String CLIENT_SECRET = "dd5d0acbab6347b49375ed6e5bb169ae";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";

    private String accessToken;
    private final OkHttpClient client;
    private final Context context;
    private boolean isGettingToken = false;
    private List<PendingSearch> pendingSearches = new ArrayList<>();

    private static class PendingSearch {
        String query;
        OnMusicLoadedCallback callback;

        PendingSearch(String query, OnMusicLoadedCallback callback) {
            this.query = query;
            this.callback = callback;
        }
    }

    public interface OnMusicLoadedCallback {
        void onMusicLoaded(List<Track> tracks);
        void onError(String error);
    }

    public static class Track {
        private String title;
        private String artist;
        private String albumArt;
        private String spotifyUrl;

        public Track(String title, String artist, String albumArt, String spotifyUrl) {
            this.title = title;
            this.artist = artist;
            this.albumArt = albumArt;
            this.spotifyUrl = spotifyUrl;
        }

        public String getTitle() { return title; }
        public String getArtist() { return artist; }
        public String getAlbumArt() { return albumArt; }
        public String getSpotifyUrl() { return spotifyUrl; }
    }

    public SpotifyService(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        getAccessToken();
    }

    private void getAccessToken() {
        if (isGettingToken) {
            return;
        }

        isGettingToken = true;
        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String base64Credentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .header("Authorization", "Basic " + base64Credentials)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get access token", e);
                isGettingToken = false;
                processPendingSearchesWithError("Failed to authenticate with Spotify");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    JSONObject json = new JSONObject(jsonData);
                    accessToken = json.getString("access_token");
                    isGettingToken = false;
                    processPendingSearches();
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing token response", e);
                    isGettingToken = false;
                    processPendingSearchesWithError("Error authenticating with Spotify");
                }
            }
        });
    }

    private void processPendingSearches() {
        for (PendingSearch search : pendingSearches) {
            performSearch(search.query, search.callback);
        }
        pendingSearches.clear();
    }

    private void processPendingSearchesWithError(String error) {
        for (PendingSearch search : pendingSearches) {
            search.callback.onError(error);
        }
        pendingSearches.clear();
    }

    public void searchAnimeOST(String animeName, OnMusicLoadedCallback callback) {
        if (accessToken == null) {
            if (!isGettingToken) {
                getAccessToken();
            }
            pendingSearches.add(new PendingSearch(animeName, callback));
            return;
        }

        performSearch(animeName, callback);
    }

    private void performSearch(String animeName, OnMusicLoadedCallback callback) {
        String query = animeName + " OST soundtrack";
        String encodedQuery = query.replace(" ", "%20");
        String url = SEARCH_URL + "?q=" + encodedQuery + "&type=track&limit=20";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed to search tracks: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 401) {
                    // Token expired or invalid, get a new one and retry
                    accessToken = null;
                    searchAnimeOST(animeName, callback);
                    return;
                }

                try {
                    String jsonData = response.body().string();
                    JSONObject json = new JSONObject(jsonData);
                    JSONObject tracks = json.getJSONObject("tracks");
                    JSONArray items = tracks.getJSONArray("items");

                    List<Track> trackList = new ArrayList<>();
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject track = items.getJSONObject(i);
                        String title = track.getString("name");
                        
                        JSONArray artists = track.getJSONArray("artists");
                        String artist = artists.getJSONObject(0).getString("name");
                        
                        JSONObject album = track.getJSONObject("album");
                        JSONArray images = album.getJSONArray("images");
                        String albumArt = images.length() > 0 ? 
                                images.getJSONObject(0).getString("url") : "";
                        
                        JSONObject externalUrls = track.getJSONObject("external_urls");
                        String spotifyUrl = externalUrls.getString("spotify");

                        trackList.add(new Track(title, artist, albumArt, spotifyUrl));
                    }

                    callback.onMusicLoaded(trackList);
                } catch (JSONException e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }
        });
    }
} 