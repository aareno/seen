package com.aareno.seen.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import com.aareno.seen.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyAuthManager {
    private static final String TAG = "SpotifyAuthManager";
    private static final String CLIENT_ID = "0a25702927cf443694296cdf4b11ae4d";
    private static final String REDIRECT_URI = "seen://spotify-auth";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String[] SCOPES = {
        "user-read-private",
        "user-read-email",
        "user-library-read",
        "playlist-read-private",
        "playlist-read-collaborative"
    };

    private static SpotifyAuthManager instance;
    private final Context context;
    private final OkHttpClient client;
    private final SharedPreferences prefs;
    private String accessToken;
    private String refreshToken;
    private long tokenExpiryTime;
    private AuthCallback callback;

    public interface AuthCallback {
        void onAuthSuccess();
        void onAuthFailure(String error);
    }

    private SpotifyAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.prefs = context.getSharedPreferences("spotify_prefs", Context.MODE_PRIVATE);
        loadTokens();
    }

    public static synchronized SpotifyAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new SpotifyAuthManager(context);
        }
        return instance;
    }

    private void loadTokens() {
        accessToken = prefs.getString("access_token", null);
        refreshToken = prefs.getString("refresh_token", null);
        tokenExpiryTime = prefs.getLong("token_expiry", 0);
    }

    private void saveTokens(String accessToken, String refreshToken, long expiresIn) {
        Log.d(TAG, "Saving tokens");
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.putLong("token_expiry", tokenExpiryTime);
        boolean success = editor.commit(); // Use commit() instead of apply() to ensure immediate write
        Log.d(TAG, "Tokens saved successfully: " + success);
        
        // Verify the tokens were saved
        String savedAccessToken = prefs.getString("access_token", null);
        Log.d(TAG, "Verified saved access token: " + (savedAccessToken != null ? "present" : "null"));
    }

    public void startAuth(Context context, AuthCallback callback) {
        Log.d(TAG, "Starting Spotify authentication");
        this.callback = callback;
        
        // Build the authorization URL
        Uri.Builder builder = Uri.parse(AUTH_URL).buildUpon();
        builder.appendQueryParameter("client_id", CLIENT_ID);
        builder.appendQueryParameter("response_type", "code");
        builder.appendQueryParameter("redirect_uri", REDIRECT_URI);
        
        // Join the scopes array into a single string
        String scopesString = String.join(" ", SCOPES);
        builder.appendQueryParameter("scope", scopesString);
        
        builder.appendQueryParameter("show_dialog", "true");
        
        String authUrl = builder.build().toString();
        Log.d(TAG, "Auth URL: " + authUrl);
        
        try {
            // Launch the auth URL in a Custom Tab
            CustomTabsIntent.Builder customTabsBuilder = new CustomTabsIntent.Builder();
            customTabsBuilder.setShowTitle(true);
            CustomTabsIntent customTabsIntent = customTabsBuilder.build();
            
            // Remove any existing flags
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            
            Log.d(TAG, "Launching Custom Tab for authentication");
            customTabsIntent.launchUrl(context, Uri.parse(authUrl));
        } catch (Exception e) {
            Log.e(TAG, "Error launching authentication", e);
            callback.onAuthFailure("Failed to launch authentication: " + e.getMessage());
        }
    }

    private static final int SPOTIFY_AUTH_REQUEST_CODE = 1234;

    public void handleAuthResponse(Uri uri, AuthCallback callback) {
        Log.d(TAG, "Handling auth response: " + uri.toString());
        String code = uri.getQueryParameter("code");
        String error = uri.getQueryParameter("error");
        
        if (error != null) {
            Log.e(TAG, "Auth error: " + error);
            callback.onAuthFailure("Authentication error: " + error);
            return;
        }
        
        if (code == null) {
            Log.e(TAG, "No authorization code in response");
            callback.onAuthFailure("No authorization code received");
            return;
        }
        Log.d(TAG, "Received authorization code: " + code);

        // Create the request body for token exchange
        RequestBody formBody = new FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("client_id", CLIENT_ID)
            .add("client_secret", "dd5d0acbab6347b49375ed6e5bb169ae")
            .build();

        Log.d(TAG, "Making token request to: " + TOKEN_URL);
        Request request = new Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build();

        Log.d(TAG, "Exchanging code for access token");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get access token", e);
                callback.onAuthFailure("Failed to get access token: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonData = response.body().string();
                Log.d(TAG, "Token response code: " + response.code());
                Log.d(TAG, "Token response body: " + jsonData);
                
                try {
                    JSONObject json = new JSONObject(jsonData);
                    
                    if (!response.isSuccessful()) {
                        String error = json.optString("error", "Unknown error");
                        String errorDescription = json.optString("error_description", "");
                        Log.e(TAG, "Token exchange failed: " + error + " - " + errorDescription);
                        callback.onAuthFailure(error + ": " + errorDescription);
                        return;
                    }
                    
                    String accessToken = json.getString("access_token");
                    String refreshToken = json.getString("refresh_token");
                    long expiresIn = json.getLong("expires_in");
                    
                    Log.d(TAG, "Successfully obtained tokens. Expires in: " + expiresIn + " seconds");
                    saveTokens(accessToken, refreshToken, expiresIn);
                    callback.onAuthSuccess();
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing token response", e);
                    callback.onAuthFailure("Error parsing token response: " + e.getMessage());
                }
            }
        });
    }

    public String getAccessToken() {
        Log.d(TAG, "Getting access token");
        if (accessToken == null) {
            Log.d(TAG, "No access token in memory, checking SharedPreferences");
            accessToken = prefs.getString("access_token", null);
            refreshToken = prefs.getString("refresh_token", null);
            tokenExpiryTime = prefs.getLong("token_expiry", 0);
            
            if (accessToken != null) {
                Log.d(TAG, "Found access token in SharedPreferences");
            } else {
                Log.d(TAG, "No access token found in SharedPreferences");
            }
        }

        if (accessToken == null) {
            Log.d(TAG, "No access token available");
            return null;
        }

        // Check if token needs refresh
        if (System.currentTimeMillis() >= tokenExpiryTime - 60000) { // Refresh 1 minute before expiry
            Log.d(TAG, "Token expired or about to expire, refreshing");
            refreshAccessToken();
        } else {
            Log.d(TAG, "Using existing access token");
        }

        return accessToken;
    }

    private void refreshAccessToken() {
        Log.d(TAG, "Refreshing access token");
        if (refreshToken == null) {
            Log.e(TAG, "No refresh token available");
            return;
        }

        RequestBody formBody = new FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", CLIENT_ID)
            .build();

        Request request = new Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to refresh token", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String jsonData = response.body().string();
                    Log.d(TAG, "Refresh token response: " + jsonData);
                    
                    JSONObject json = new JSONObject(jsonData);
                    
                    String newAccessToken = json.getString("access_token");
                    String newRefreshToken = json.optString("refresh_token", refreshToken); // May not be included
                    long expiresIn = json.getLong("expires_in");
                    
                    Log.d(TAG, "Successfully refreshed tokens. New expiry: " + expiresIn + " seconds");
                    saveTokens(newAccessToken, newRefreshToken, expiresIn);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing refresh token response", e);
                }
            }
        });
    }

    public void signOut() {
        Log.d(TAG, "Signing out");
        accessToken = null;
        refreshToken = null;
        tokenExpiryTime = 0;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "Sign out complete");
    }
} 