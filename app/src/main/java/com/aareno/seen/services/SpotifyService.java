package com.aareno.seen.services;

import android.content.Context;
import android.util.Log;

import com.aareno.seen.auth.SpotifyAuthManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotifyService {
    private static final String TAG = "SpotifyService";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";

    private final OkHttpClient client;
    private final Context context;
    private final SpotifyAuthManager authManager;

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
        this.authManager = SpotifyAuthManager.getInstance(context);
    }

    public void searchAnimeOST(String animeName, OnMusicLoadedCallback callback) {
        String accessToken = authManager.getAccessToken();
        if (accessToken == null) {
            callback.onError("Not authenticated with Spotify. Please sign in first.");
            return;
        }

        String query = animeName;
        Log.d(TAG, "query: " + query);
        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            callback.onError("Encoding error: " + e.getMessage());
            return;
        }

        String market = "US"; // Change to your preferred market code
        String url = SEARCH_URL + "?q=" + encodedQuery + "&type=track&limit=20&market=" + market;

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
                    // Token expired or invalid
                    callback.onError("Authentication expired. Please sign in again.");
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