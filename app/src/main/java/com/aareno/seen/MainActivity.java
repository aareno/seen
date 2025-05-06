package com.aareno.seen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.aareno.seen.data.WorkScheduler;
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

import java.util.ArrayList;

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

    private BottomNavigationView bottomNavigation;
    private boolean isInSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize fragments
        animeFragment = new AnimeFragment();
        kdramaFragment = new KDramaFragment();
        TvMoviesFragment = new TvMoviesFragment();
        settingsFragment = new SettingsFragment();

        // Initialize buttons
        initializeButtons();

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

                if (currentFragment instanceof AnimeFragment) {
                    Intent intent = new Intent(MainActivity.this, SearchAnimeActivity.class);
                    startActivityForResult(intent, SEARCH_ANIME_REQUEST_CODE);
                } else if (currentFragment instanceof KDramaFragment) {
                    Intent intent = new Intent(MainActivity.this, SearchKDramaActivity.class);
                    startActivityForResult(intent, SEARCH_KDRAMA_REQUEST_CODE);
                } else if (currentFragment instanceof TvMoviesFragment) {
                    Intent intent = new Intent(MainActivity.this, SearchShowActivity.class);
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
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };

        int[] colors = new int[] {
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
    }

    @Override
    public void setUndoEnabled(boolean enabled) {
        if (undoButton != null) {
            undoButton.setEnabled(enabled);
        }
    }

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
        if (notificationsEnabled) {
            // Enable notifications logic
        } else {
            // Disable notifications logic
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

}