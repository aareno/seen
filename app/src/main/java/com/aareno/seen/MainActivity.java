package com.aareno.seen;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

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
import com.aareno.seen.ui.TvMovies.TvMoviesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AnimeFragment.UndoListener {
    private static final int SEARCH_ANIME_REQUEST_CODE = 1001;
    private static final int SEARCH_KDRAMA_REQUEST_CODE = 1002;
    private static final int SEARCH_TVMOVIES_REQUEST_CODE = 1003;
    private AnimeFragment animeFragment;
    private KDramaFragment kdramaFragment;
    private ImageButton undoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize fragments
        animeFragment = new AnimeFragment();
        kdramaFragment = new KDramaFragment();

        // Initialize undo button
        undoButton = findViewById(R.id.btn_undo);
        undoButton.setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof AnimeFragment) {
                animeFragment.performUndo();
            } else if (currentFragment instanceof KDramaFragment) {
                Log.d("MainActivity", "Performing undo on KDramaFragment");
                kdramaFragment.performUndo();
            }
        });
        undoButton.setEnabled(false);

        // Bottom Navigation Setup
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        // Create ColorStateLists for each tab
        ColorStateList animeColors = createColorStateList(R.color.silver);
        ColorStateList kdramaColors = createColorStateList(R.color.red);
        ColorStateList tvMoviesColors = createColorStateList(R.color.black);

        // Set the custom colors for the icons
        bottomNavigation.setItemIconTintList(null); // Remove default tint
        bottomNavigation.setItemTextColor(createColorStateList(R.color.gray));

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_anime) {
                selectedFragment = animeFragment;
                bottomNavigation.setItemIconTintList(animeColors);
            } else if (itemId == R.id.nav_kdrama) {
                selectedFragment = kdramaFragment;
                bottomNavigation.setItemIconTintList(kdramaColors);
            } else if (itemId == R.id.nav_tv_movies) {
                selectedFragment = new TvMoviesFragment();
                bottomNavigation.setItemIconTintList(tvMoviesColors);
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();

                // Show undo button for fragments that support undo
                boolean supportsUndo = (selectedFragment instanceof AnimeFragment) ||
                        (selectedFragment instanceof KDramaFragment);
                undoButton.setVisibility(supportsUndo ? View.VISIBLE : View.GONE);

                if (supportsUndo) {
                    undoButton.setEnabled(true);
                    Log.d("MainActivity", "Undo button enabled: true");
                }

                return true;
            }

            return false;
        });

        ImageButton btnAdd = findViewById(R.id.btn_add);
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
                Toast.makeText(this, "TV/Movies search not implemented", Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, animeFragment)
                    .commit();
            bottomNavigation.setSelectedItemId(R.id.nav_anime);
        }

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
                            ((AnimeFragment) currentFragment).addAnimeToWatchingList(anime);
                        }
                    }
                    break;

                case SEARCH_KDRAMA_REQUEST_CODE:
                    ArrayList<KDrama> selectedKDrama = (ArrayList<KDrama>) data.getSerializableExtra("selected_kdrama_list");
                    if (selectedKDrama != null && currentFragment instanceof KDramaFragment) {
                        for (KDrama kdrama : selectedKDrama) {
                            ((KDramaFragment) currentFragment).addKDramaToWatchingList(kdrama);
                        }
                    }
                    break;
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
        Fragment currentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof AnimeFragment) {
            super.onBackPressed();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, animeFragment)
                    .commit();
            BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
            bottomNavigation.setSelectedItemId(R.id.nav_anime);
        }
    }
}