package com.aareno.seen;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.aareno.seen.auth.UserAuthManager;
import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.aareno.seen.data.TvMovies.ShowRepository;
import com.aareno.seen.data.WorkScheduler;
import com.aareno.seen.sync.DataSyncManager;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.Anime.AnimeFragment;
import com.aareno.seen.ui.Anime.SearchAnimeActivity;
import com.aareno.seen.ui.KDrama.KDrama;
import com.aareno.seen.ui.KDrama.KDramaFragment;
import com.aareno.seen.ui.KDrama.SearchKDramaActivity;
import com.aareno.seen.ui.TvMovies.SearchShowActivity;
import com.aareno.seen.ui.TvMovies.Show;
import com.aareno.seen.ui.TvMovies.TvMoviesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements AnimeFragment.UndoListener, KDramaFragment.UndoListener, TvMoviesFragment.UndoListener {
    private static final int SEARCH_ANIME_REQUEST_CODE = 1001;
    private static final int SEARCH_KDRAMA_REQUEST_CODE = 1002;
    private static final int SEARCH_TVMOVIES_REQUEST_CODE = 1003;
    private AnimeFragment animeFragment;
    private KDramaFragment kdramaFragment;
    private TvMoviesFragment TvMoviesFragment;
    private SettingsFragment settingsFragment;
    private ImageButton undoButton;
    private ImageButton btnSettings;
    private Button signInButton;

    private BottomNavigationView bottomNavigation;
    private boolean isInSettings = false;

    private UserAuthManager userAuthManager;
    private DataSyncManager dataSyncManager;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Add this at the very beginning of onCreate
        checkGooglePlayServices();
        printKeyHash(); // Print SHA-1 hash for verification
        
        try {
            // Initialize authentication and sync managers
            userAuthManager = UserAuthManager.getInstance(this);
            dataSyncManager = DataSyncManager.getInstance(this);

            // Register for authentication result with improved error handling
            signInLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d("MainActivity", "Sign-in activity returned with resultCode: " + result.getResultCode());
                        
                        if (result.getData() != null) {
                            Log.d("MainActivity", "Intent data is present");
                        } else {
                            Log.d("MainActivity", "Intent data is null");
                        }
                        
                        // Process even if resultCode is not OK - this helps debug the issue
                        if (result.getData() != null) {
                            Log.d("MainActivity", "Processing sign-in result regardless of resultCode");
                            userAuthManager.handleSignInResult(
                                    result.getData(),
                                    new UserAuthManager.AuthListener() {
                                        @Override
                                        public void onAuthSuccess(FirebaseUser user) {
                                            Log.d("MainActivity", "Auth success: " + user.getEmail());
                                            updateUIAfterAuth(user);
                                            syncData();
                                        }

                                        @Override
                                        public void onAuthFailure(Exception e) {
                                            Log.e("MainActivity", "Auth failure: " + e.getMessage(), e);
                                            showAuthError(e.getMessage());
                                        }
                                    }
                            );
                        } else if (result.getResultCode() != Activity.RESULT_OK) {
                            Log.w("MainActivity", "Sign-in cancelled (no data)");
                            showAuthError("Sign-in was cancelled without data");
                        }
                    }
            );
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing auth components", e);
            Toast.makeText(this, "Authentication features unavailable", Toast.LENGTH_LONG).show();
        }

        requestNotificationPermission();

        // Initialize fragments
        animeFragment = new AnimeFragment();
        kdramaFragment = new KDramaFragment();
        TvMoviesFragment = new TvMoviesFragment();
        settingsFragment = new SettingsFragment();

        // Initialize buttons
        initializeButtons();

        // Check if user is already signed in
        if (userAuthManager.isUserSignedIn()) {
            updateUIAfterAuth(userAuthManager.getCurrentUser());
            // Sync data if user is already signed in
            syncData();
        }

        // Bottom Navigation Setup
        bottomNavigation = findViewById(R.id.bottom_navigation);

        // Create ColorStateLists for each tab
        ColorStateList animeColors = createColorStateList(R.color.wood);
        ColorStateList kdramaColors = createColorStateList(R.color.red);
        ColorStateList tvMoviesColors = createColorStateList(R.color.black);

        applyTabVisibilitySettings();


        // Set the custom colors for the icons
        bottomNavigation.setItemIconTintList(null); // Remove default tint
        bottomNavigation.setItemTextColor(createColorStateList(R.color.gray));

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (isInSettings) {
                // Reset settings state when navigating away using bottom navigation
                getSupportFragmentManager().popBackStack();
                resetToMainView();
            }

            if (itemId == R.id.nav_anime) {
                selectedFragment = animeFragment;
                bottomNavigation.setItemIconTintList(animeColors);
            } else if (itemId == R.id.nav_kdrama) {
                selectedFragment = kdramaFragment;
                bottomNavigation.setItemIconTintList(kdramaColors);
            } else if (itemId == R.id.nav_tv_movies) {
                selectedFragment = TvMoviesFragment;
                bottomNavigation.setItemIconTintList(tvMoviesColors);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();

                if (undoButton != null) {
                    undoButton.setVisibility(View.VISIBLE);
                    undoButton.setEnabled(true);
                }

                return true;
            }

            return false;
        });

        applyUserSettings();

        ImageButton btnAdd = findViewById(R.id.btn_add);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                Fragment currentFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);

                boolean showAdultContent = shouldShowAdultContent();


                if (currentFragment instanceof AnimeFragment) {
                    Intent intent = new Intent(MainActivity.this, SearchAnimeActivity.class);
                    intent.putExtra("show_adult_content", showAdultContent);
                    startActivityForResult(intent, SEARCH_ANIME_REQUEST_CODE);
                } else if (currentFragment instanceof KDramaFragment) {
                    Intent intent = new Intent(MainActivity.this, SearchKDramaActivity.class);
                    intent.putExtra("show_adult_content", showAdultContent);
                    startActivityForResult(intent, SEARCH_KDRAMA_REQUEST_CODE);
                } else if (currentFragment instanceof TvMoviesFragment) {
                    Intent intent = new Intent(MainActivity.this, SearchShowActivity.class);
                    intent.putExtra("show_adult_content", showAdultContent);
                    startActivityForResult(intent, SEARCH_TVMOVIES_REQUEST_CODE);
                }
            });
        }

        if (savedInstanceState == null) {
            loadInitialFragment();
        }
    }

    private void initializeButtons() {
        // Initialize Settings Button
        btnSettings = findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                if (!isInSettings) {
                    showSettingsFragment();
                } else {
                    onBackPressed();
                }
            });
        }

        // Initialize undo button
        undoButton = findViewById(R.id.btn_undo);
        if (undoButton != null) {
            undoButton.setOnClickListener(v -> {
                Fragment currentFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof AnimeFragment) {
                    animeFragment.performUndo();
                } else if (currentFragment instanceof KDramaFragment) {
                    Log.d("MainActivity", "Performing undo on KDramaFragment");
                    kdramaFragment.performUndo();
                } else if (currentFragment instanceof TvMoviesFragment) {
                    TvMoviesFragment.performUndo();
                }
            });
            undoButton.setEnabled(false);
        }

        // Add sign-in button
        signInButton = findViewById(R.id.btn_sign_in);
        if (signInButton != null) {
            signInButton.setOnClickListener(v -> {
                // Re-check Google Play Services on button click to catch runtime changes
                com.google.android.gms.common.GoogleApiAvailability googleApi = 
                        com.google.android.gms.common.GoogleApiAvailability.getInstance();
                int resultCode = googleApi.isGooglePlayServicesAvailable(this);
                
                if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                    Log.d("MainActivity", "Google Play Services unavailable, prompting user");
                    if (googleApi.isUserResolvableError(resultCode)) {
                        googleApi.getErrorDialog(this, resultCode, 9000).show();
                        return;
                    } else {
                        Toast.makeText(MainActivity.this, 
                                "Google Play Services is required but not available on this device", 
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                
                // Check userAuthManager first
                if (userAuthManager == null) {
                    Toast.makeText(MainActivity.this, 
                            "Authentication service is not available", 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Now check if the user is already signed in for sign-out flow
                if (userAuthManager.isUserSignedIn()) {
                    // If already signed in, clear data and sign out
                    clearAllUserData(() -> {
                        userAuthManager.signOut(task -> {
                            updateUIAfterSignOut();
                            showMessage("Successfully signed out");
                        });
                    });
                    return;
                } 
                
                // Check signInLauncher separately for sign-in flow
                if (signInLauncher == null) {
                    Log.e("MainActivity", "Cannot sign in: signInLauncher is null");
                    Toast.makeText(MainActivity.this, 
                            "Sign-in functionality is not available", 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Start sign-in flow only if launcher is not null
                try {
                    Intent signInIntent = userAuthManager.getSignInIntent();
                    Log.d("MainActivity", "Launching sign-in intent");
                    
                    // Print SHA-1 fingerprint to help with debugging
                    try {
                        android.content.pm.PackageInfo packageInfo = getPackageManager().getPackageInfo(
                                getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
                        for (android.content.pm.Signature signature : packageInfo.signatures) {
                            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                            md.update(signature.toByteArray());
                            String shaString = android.util.Base64.encodeToString(md.digest(), 
                                    android.util.Base64.DEFAULT);
                            Log.d("MainActivity", "SHA-1 for this build: " + shaString);
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error getting SHA-1", e);
                    }
                    
                    // Check if the intent is properly configured
                    if (signInIntent.resolveActivity(getPackageManager()) != null) {
                        Log.d("MainActivity", "Intent can be resolved by an activity");
                        signInLauncher.launch(signInIntent);
                    } else {
                        Log.e("MainActivity", "No activity can handle the sign-in intent");
                        Toast.makeText(MainActivity.this,
                                "Google Sign-In is not available on this device",
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("MainActivity", "Error launching sign-in flow", e);
                    Toast.makeText(MainActivity.this,
                            "Failed to start sign-in: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showSettingsFragment() {
        if (undoButton != null) {
            undoButton.setVisibility(View.GONE);
        }

        if (btnSettings != null) {
            btnSettings.setImageResource(R.drawable.back);
        }

        isInSettings = true;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, settingsFragment)
                .addToBackStack(null)
                .commit();
    }

    private void resetToMainView() {
        if (undoButton != null) {
            undoButton.setVisibility(View.VISIBLE);
        }

        if (btnSettings != null) {
            btnSettings.setImageResource(R.drawable.settings);
        }

        isInSettings = false;
    }

    // Helper method to create ColorStateList
    private ColorStateList createColorStateList(int colorRes) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };

        int[] colors = new int[]{
                ContextCompat.getColor(this, colorRes),
                ContextCompat.getColor(this, R.color.gray)
        };

        return new ColorStateList(states, colors);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);

            switch (requestCode) {
                case SEARCH_ANIME_REQUEST_CODE:
                    ArrayList<Anime> selectedAnime = (ArrayList<Anime>) data.getSerializableExtra("selected_anime_list");
                    if (selectedAnime != null && currentFragment instanceof AnimeFragment) {
                        for (Anime anime : selectedAnime) {
                            if (anime.isWatching()) {
                                ((AnimeFragment) currentFragment).addAnimeToWatchingList(anime);
                            } else {
                                ((AnimeFragment) currentFragment).addAnimeToWatchedList(anime);
                            }
                        }
                    }
                    break;

                case SEARCH_KDRAMA_REQUEST_CODE:
                    ArrayList<KDrama> selectedKDrama = (ArrayList<KDrama>) data.getSerializableExtra("selected_kdrama_list");
                    if (selectedKDrama != null && currentFragment instanceof KDramaFragment) {
                        for (KDrama kdrama : selectedKDrama) {
                            if (kdrama.isWatching()) {
                                ((KDramaFragment) currentFragment).addKDramaToWatchingList(kdrama);
                            } else {
                                ((KDramaFragment) currentFragment).addKDramaToWatchedList(kdrama);
                            }
                        }
                    }
                    break;

                case SEARCH_TVMOVIES_REQUEST_CODE:
                    ArrayList<Show> selectedTvMovies = (ArrayList<Show>) data.getSerializableExtra("selected_show_list");
                    if (selectedTvMovies != null && currentFragment instanceof TvMoviesFragment) {
                        for (Show show : selectedTvMovies) {
                            if (show.isWatching()) {
                                ((TvMoviesFragment) currentFragment).addShowToWatchingList(show);
                            } else {
                                ((TvMoviesFragment) currentFragment).addShowToWatchedList(show);
                            }
                        }
                    }
            }
        }

        // Sync data after adding new content
        if (userAuthManager.isUserSignedIn()) {
            // Sync in a slight delay to ensure local DB is updated first
            new Handler().postDelayed(this::syncData, 1000);
        }
    }

    @Override
    public void setUndoEnabled(boolean enabled) {
        if (undoButton != null) {
            undoButton.setEnabled(enabled);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof SettingsFragment) {
            // Get enabled tab states before exiting settings
            SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
            boolean animeEnabled = prefs.getBoolean("anime_tab_enabled", true);
            boolean kdramaEnabled = prefs.getBoolean("kdrama_tab_enabled", true);
            boolean tvMoviesEnabled = prefs.getBoolean("tvmovies_tab_enabled", true);

            // Pop back stack first
            getSupportFragmentManager().popBackStack();
            resetToMainView();

            // Check which tab should be selected
            if (bottomNavigation.getSelectedItemId() == NAV_ANIME && !animeEnabled ||
                    bottomNavigation.getSelectedItemId() == NAV_KDRAMA && !kdramaEnabled ||
                    bottomNavigation.getSelectedItemId() == NAV_TVMOVIES && !tvMoviesEnabled) {

                // Select first enabled tab without directly accessing fragments
                if (animeEnabled) {
                    bottomNavigation.setSelectedItemId(NAV_ANIME);
                } else if (kdramaEnabled) {
                    bottomNavigation.setSelectedItemId(NAV_KDRAMA);
                } else if (tvMoviesEnabled) {
                    bottomNavigation.setSelectedItemId(NAV_TVMOVIES);
                }
            }
        } else if (currentFragment instanceof AnimeFragment) {
            super.onBackPressed();
        } else {
            // Let the BottomNavigationView handle the fragment transaction
            bottomNavigation.setSelectedItemId(NAV_ANIME);
        }
    }

    private static final int NAV_ANIME = R.id.nav_anime;
    private static final int NAV_KDRAMA = R.id.nav_kdrama;
    private static final int NAV_TVMOVIES = R.id.nav_tv_movies;

    public void updateTabVisibility() {
        Log.d("MainActivity", "updateTabVisibility called");

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean animeEnabled = prefs.getBoolean("anime_tab_enabled", true);
        boolean kdramaEnabled = prefs.getBoolean("kdrama_tab_enabled", true);
        boolean tvMoviesEnabled = prefs.getBoolean("tvmovies_tab_enabled", true);

        Log.d("MainActivity", "Tab states - Anime: " + animeEnabled +
                ", KDrama: " + kdramaEnabled +
                ", TV/Movies: " + tvMoviesEnabled);

        // Update the menu visibility
        Menu menu = bottomNavigation.getMenu();
        menu.findItem(NAV_ANIME).setVisible(animeEnabled);
        menu.findItem(NAV_KDRAMA).setVisible(kdramaEnabled);
        menu.findItem(NAV_TVMOVIES).setVisible(tvMoviesEnabled);

        // If we are in settings, just update the menu visibility but don't switch tabs
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof SettingsFragment) {
            Log.d("MainActivity", "In SettingsFragment, not switching tabs");
            return;
        }

        // Get the currently selected tab ID
        int selectedItemId = bottomNavigation.getSelectedItemId();
        Log.d("MainActivity", "Current selected tab ID: " + selectedItemId);

        // Check if the current tab is now hidden
        boolean needToSwitch = false;

        if (selectedItemId == NAV_ANIME && !animeEnabled) {
            needToSwitch = true;
            Log.d("MainActivity", "Anime tab is selected but now disabled");
        } else if (selectedItemId == NAV_KDRAMA && !kdramaEnabled) {
            needToSwitch = true;
            Log.d("MainActivity", "KDrama tab is selected but now disabled");
        } else if (selectedItemId == NAV_TVMOVIES && !tvMoviesEnabled) {
            needToSwitch = true;
            Log.d("MainActivity", "TV/Movies tab is selected but now disabled");
        }

        // If we need to switch, find the first available tab
        if (needToSwitch) {
            Log.d("MainActivity", "Need to switch to another tab");
            if (animeEnabled) {
                Log.d("MainActivity", "Switching to Anime tab");
                bottomNavigation.setSelectedItemId(NAV_ANIME);
            } else if (kdramaEnabled) {
                Log.d("MainActivity", "Switching to KDrama tab");
                bottomNavigation.setSelectedItemId(NAV_KDRAMA);
            } else if (tvMoviesEnabled) {
                Log.d("MainActivity", "Switching to TV/Movies tab");
                bottomNavigation.setSelectedItemId(NAV_TVMOVIES);
            }
        } else {
            Log.d("MainActivity", "No need to switch tabs");
        }
    }

    private void applyUserSettings() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Apply tab visibility settings
        boolean animeEnabled = prefs.getBoolean("anime_tab_enabled", true);
        boolean kdramaEnabled = prefs.getBoolean("kdrama_tab_enabled", true);
        boolean tvMoviesEnabled = prefs.getBoolean("tvmovies_tab_enabled", true);

        // Update the menu visibility
        Menu menu = bottomNavigation.getMenu();
        menu.findItem(NAV_ANIME).setVisible(animeEnabled);
        menu.findItem(NAV_KDRAMA).setVisible(kdramaEnabled);
        menu.findItem(NAV_TVMOVIES).setVisible(tvMoviesEnabled);

        // Ensure we start with a valid tab selected
        if (animeEnabled) {
            // If Anime tab is enabled, use it as default
            bottomNavigation.setSelectedItemId(NAV_ANIME);
        } else if (kdramaEnabled) {
            // Otherwise try KDrama
            bottomNavigation.setSelectedItemId(NAV_KDRAMA);
        } else if (tvMoviesEnabled) {
            // Otherwise try TV/Movies
            bottomNavigation.setSelectedItemId(NAV_TVMOVIES);
        }

        // Apply other settings as needed
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        // Apply notification settings if needed
        Log.d("MainActivity", "Notifications enabled: " + notificationsEnabled);
        if (notificationsEnabled) {
            WorkScheduler.scheduleAiringNotifications(this);
        } else {
            WorkScheduler.cancelAiringNotifications(this);
        }
    }

    private void applyTabVisibilitySettings() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean animeEnabled = prefs.getBoolean("anime_tab_enabled", true);
        boolean kdramaEnabled = prefs.getBoolean("kdrama_tab_enabled", true);
        boolean tvMoviesEnabled = prefs.getBoolean("tvmovies_tab_enabled", true);

        // Update the menu visibility
        Menu menu = bottomNavigation.getMenu();
        menu.findItem(NAV_ANIME).setVisible(animeEnabled);
        menu.findItem(NAV_KDRAMA).setVisible(kdramaEnabled);
        menu.findItem(NAV_TVMOVIES).setVisible(tvMoviesEnabled);
    }

    private void loadInitialFragment() {
        // Check which tabs are enabled
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean animeEnabled = prefs.getBoolean("anime_tab_enabled", true);
        boolean kdramaEnabled = prefs.getBoolean("kdrama_tab_enabled", true);
        boolean tvMoviesEnabled = prefs.getBoolean("tvmovies_tab_enabled", true);

        // Try to restore the last selected tab
        int lastSelectedTab = prefs.getInt("last_selected_tab", -1);

        // Check if last tab is valid and enabled
        if (lastSelectedTab == NAV_ANIME && animeEnabled) {
            bottomNavigation.setSelectedItemId(NAV_ANIME);
        } else if (lastSelectedTab == NAV_KDRAMA && kdramaEnabled) {
            bottomNavigation.setSelectedItemId(NAV_KDRAMA);
        } else if (lastSelectedTab == NAV_TVMOVIES && tvMoviesEnabled) {
            bottomNavigation.setSelectedItemId(NAV_TVMOVIES);
        }
        // If last tab is invalid or disabled, select the first enabled tab
        else {
            // Find the first enabled tab and set it
            if (animeEnabled) {
                bottomNavigation.setSelectedItemId(NAV_ANIME);
            } else if (kdramaEnabled) {
                bottomNavigation.setSelectedItemId(NAV_KDRAMA);
            } else if (tvMoviesEnabled) {
                bottomNavigation.setSelectedItemId(NAV_TVMOVIES);
            }
        }
    }

    @Override
    public void updateUndoButton(boolean hasItems) {
        // Update both enabled state and appearance
        undoButton.setEnabled(hasItems);
        if (hasItems) {
            undoButton.setAlpha(1.0f);
            // Optional: You can also change the background tint or image tint
            undoButton.setImageTintList(ColorStateList.valueOf(
                    MaterialColors.getColor(undoButton, com.google.android.material.R.attr.colorOnPrimary)
            ));
        } else {
            undoButton.setAlpha(0.5f);
            // Optional: Change to a duller color when disabled
            undoButton.setImageTintList(ColorStateList.valueOf(
                    MaterialColors.getColor(undoButton, com.google.android.material.R.attr.colorOnPrimary)
            ));
        }
    }

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) { // Android 13 (TIRAMISU)
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted");
            } else {
                Log.d("MainActivity", "Notification permission denied");
                Toast.makeText(this, "Notifications are required to receive show updates", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onNotificationSettingChanged(boolean enabled) {
        if (enabled) {
            WorkScheduler.scheduleAiringNotifications(this);
        } else {
            WorkScheduler.cancelAiringNotifications(this);
        }
    }

    private boolean showAdultContent = false;

    // Method called when adult content filter is changed
    public void onContentFilterChanged(boolean showAdultContent) {
        this.showAdultContent = showAdultContent;

        // Save the setting in SharedPreferences (optional if already done in SettingsFragment)
        SharedPreferences.Editor editor = getSharedPreferences("AppSettings", MODE_PRIVATE).edit();
        editor.putBoolean("show_adult_content", showAdultContent);
        editor.apply();

        // Log the change
        Log.d("MainActivity", "Content filter changed: showing adult content = " + showAdultContent);
    }


    // Getter method for the adult content setting
    public boolean shouldShowAdultContent() {
        // Read from SharedPreferences to ensure we always have the latest value
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        return prefs.getBoolean("show_adult_content", false);
    }

    private void updateUIAfterAuth(FirebaseUser user) {
        if (signInButton != null) {
            signInButton.setText("Sign Out");
        }

        String displayName = user.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            showMessage("Signed in as " + displayName);
        } else {
            showMessage("Signed in successfully");
        }
        
        // Sync data to restore the user's content
        syncData();
        
        // Refresh fragments to display the synced data
        refreshCurrentFragment();
    }

    // Helper method to clear all user data from local database
    private void clearAllUserData(Runnable onComplete) {
        showMessage("Clearing local data...");
        
        // Create repositories if they don't exist
        AnimeRepository animeRepository = new AnimeRepository(this);
        KDramaRepository kdramaRepository = new KDramaRepository(this);
        ShowRepository showRepository = new ShowRepository(this);
        
        // Use atomic counter to track when all clear operations complete
        AtomicInteger pendingOperations = new AtomicInteger(3);
        
        // Clear anime data
        animeRepository.clearAllAnime(new AnimeRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d("MainActivity", "Anime data cleared");
                checkAllClearComplete(pendingOperations, onComplete);
            }

            @Override
            public void onError(Exception e) {
                Log.e("MainActivity", "Error clearing anime data", e);
                checkAllClearComplete(pendingOperations, onComplete);
            }
        });
        
        // Clear KDrama data
        kdramaRepository.clearAllKDrama(new KDramaRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d("MainActivity", "KDrama data cleared");
                checkAllClearComplete(pendingOperations, onComplete);
            }

            @Override
            public void onError(Exception e) {
                Log.e("MainActivity", "Error clearing KDrama data", e);
                checkAllClearComplete(pendingOperations, onComplete);
            }
        });
        
        // Clear TV/Movies data
        showRepository.clearAllShows(new ShowRepository.OnDataLoadedCallback<Void>() {
            @Override
            public void onDataLoaded(Void data) {
                Log.d("MainActivity", "TV/Movies data cleared");
                checkAllClearComplete(pendingOperations, onComplete);
            }

            @Override
            public void onError(Exception e) {
                Log.e("MainActivity", "Error clearing TV/Movies data", e);
                checkAllClearComplete(pendingOperations, onComplete);
            }
        });
    }
    
    // Helper method to check if all clear operations have completed
    private void checkAllClearComplete(AtomicInteger pendingOperations, Runnable onComplete) {
        if (pendingOperations.decrementAndGet() == 0) {
            // All operations complete, refresh UI to show empty lists
            refreshCurrentFragment();
            
            // Run completion callback
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
    
    // Helper method to refresh the current fragment
    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        
        if (currentFragment instanceof AnimeFragment) {
            // Reload the anime fragment
            getSupportFragmentManager().beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commit();
        } else if (currentFragment instanceof KDramaFragment) {
            // Reload the KDrama fragment
            getSupportFragmentManager().beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commit();
        } else if (currentFragment instanceof TvMoviesFragment) {
            // Reload the TV/Movies fragment
            getSupportFragmentManager().beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commit();
        }
    }

    private void updateUIAfterSignOut() {
        if (signInButton != null) {
            signInButton.setText("Sign In");
        }
    }

    private void showAuthError(String message) {
        showMessage("Authentication failed: " + message);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void syncData() {
        if (dataSyncManager == null || userAuthManager == null || !userAuthManager.isUserSignedIn()) {
            Log.d("SyncData", "Cannot sync: Data sync manager not initialized or user not signed in");
            return;
        }

        // Add detection for emulator
        boolean isEmulator = isEmulator();
        if (isEmulator) {
            Log.w("MainActivity", "Running on emulator - some Firebase features may not work properly");
        }

        showMessage("Syncing data...");
        dataSyncManager.syncData(success -> {
            if (success) {
                showMessage("Data sync completed");
            } else {
                // Customize message based on whether we're on emulator
                if (isEmulator) {
                    String errorMsg = "Sync failed. Emulators often have issues with Firebase. " +
                                      "Consider testing on a physical device.";
                    showMessage(errorMsg);
                    
                    // Show a more detailed dialog
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Emulator Detected")
                           .setMessage("You're running on an emulator which may have limited Google Play Services " +
                                      "support. For full Firebase functionality, use a physical device or " +
                                      "an emulator with Google Play Services.")
                           .setPositiveButton("OK", null)
                           .show();
                } else {
                    // Show a more detailed error with options to fix Firestore
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Firestore Configuration Required")
                          .setMessage("There are a few possible issues to fix:\n\n" +
                                     "1. Firestore API might not be enabled\n" +
                                     "2. Firestore security rules may need configuration\n\n" +
                                     "Would you like to open Firebase Console to fix these issues?")
                          .setPositiveButton("Open Firebase Console", (dialog, which) -> {
                              String url = "https://console.firebase.google.com/project/seen-907b9/firestore/rules";
                              Intent intent = new Intent(Intent.ACTION_VIEW);
                              intent.setData(android.net.Uri.parse(url));
                              startActivity(intent);
                          })
                          .setNeutralButton("View Guide", (dialog, which) -> {
                              showFirestoreSetupGuide();
                          })
                          .setNegativeButton("Cancel", null)
                          .show();
                }
            }
        });
    }

    // Add this method to show the setup guide
    private void showFirestoreSetupGuide() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Firestore Setup Guide")
               .setMessage(
                   "1. Enable Firestore API:\n" +
                   "   - Go to Firebase Console > Firestore Database\n" +
                   "   - Click 'Create Database'\n" +
                   "   - Choose 'Start in test mode' for now\n\n" +
                   "2. Set up Security Rules:\n" +
                   "   - Go to Firebase Console > Firestore Database > Rules\n" +
                   "   - Update rules to allow authenticated users access:\n\n" +
                   "rules_version = '2';\n" +
                   "service cloud.firestore {\n" +
                   "  match /databases/{database}/documents {\n" +
                   "    match /users/{userId} {\n" +
                   "      allow read, write: if request.auth != null && request.auth.uid == userId;\n" +
                   "      match /{document=**} {\n" +
                   "        allow read, write: if request.auth != null && request.auth.uid == userId;\n" +
                   "      }\n" +
                   "    }\n" +
                   "  }\n" +
                   "}\n\n" +
                   "3. Wait a few minutes after making changes"
               )
               .setPositiveButton("Open Firebase Console", (dialog, which) -> {
                   String url = "https://console.firebase.google.com/project/seen-907b9/firestore/rules";
                   Intent intent = new Intent(Intent.ACTION_VIEW);
                   intent.setData(android.net.Uri.parse(url));
                   startActivity(intent);
               })
               .setNegativeButton("Close", null)
               .show();
    }

    // Helper method to detect if running on emulator
    private boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

    private void checkGooglePlayServices() {
        try {
            com.google.android.gms.common.GoogleApiAvailability googleApi = 
                    com.google.android.gms.common.GoogleApiAvailability.getInstance();
            int resultCode = googleApi.isGooglePlayServicesAvailable(this);
            
            Log.d("MainActivity", "Google Play Services status: " + resultCode + 
                  " (0 = SUCCESS, others indicate issues)");
                  
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                String errorMessage = "Google Play Services issue detected (code: " + resultCode + ")";
                Log.e("MainActivity", errorMessage);
                
                // Disable sign-in button if Play Services is not available
                if (signInButton != null) {
                    signInButton.setEnabled(false);
                    signInButton.setText("Sign-In Unavailable");
                }
                
                // Create a detailed error dialog with information about the emulator
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Google Play Services Required")
                    .setMessage("This app requires Google Play Services, but they appear to be " +
                               "unavailable or have issues on this device.\n\n" +
                               "If you're using an emulator, please create an emulator with Google Play " +
                               "Services (Google APIs) installed.\n\n" +
                               "Error code: " + resultCode)
                    .setPositiveButton("OK", null);
                
                if (googleApi.isUserResolvableError(resultCode)) {
                    builder.setNeutralButton("Resolve Issue", (dialog, which) -> {
                        googleApi.getErrorDialog(this, resultCode, 9000).show();
                    });
                }
                
                builder.show();
                
                // Fallback to offline mode
                enableOfflineMode();
            } else {
                // Make sure sign-in button is enabled if Play Services is available
                if (signInButton != null) {
                    signInButton.setEnabled(true);
                    signInButton.setText("Sign In");
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error checking Google Play Services", e);
        }
    }

    private void enableOfflineMode() {
        // Set the app to work in offline mode without cloud sync
        Log.d("MainActivity", "Enabling offline mode due to Play Services issues");
        Toast.makeText(this, 
                "Working in offline mode. Cloud sync will be unavailable.", 
                Toast.LENGTH_LONG).show();
                
        // You can hide or disable cloud-dependent features here
    }

    private void printKeyHash() {
        try {
            // Get the package signature
            android.content.pm.PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), android.content.pm.PackageManager.GET_SIGNATURES);
                    
            for (android.content.pm.Signature signature : packageInfo.signatures) {
                // Get the SHA-1 fingerprint
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
                md.update(signature.toByteArray());
                String shaHash = bytesToHex(md.digest());
                
                Log.d("KeyHash", "SHA-1: " + shaHash);
                
                // Also print base64 (used by some Firebase services)
                String base64Hash = android.util.Base64.encodeToString(md.digest(), 
                        android.util.Base64.DEFAULT);
                Log.d("KeyHash", "Base64: " + base64Hash.trim());
            }
        } catch (Exception e) {
            Log.e("KeyHash", "Error getting key hash", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}