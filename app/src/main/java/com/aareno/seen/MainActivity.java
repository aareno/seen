package com.aareno.seen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.aareno.seen.R;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.Anime.AnimeFragment;
import com.aareno.seen.ui.Anime.SearchAnimeActivity;
import com.aareno.seen.ui.KDrama.KDramaFragment;
import com.aareno.seen.ui.TvMovies.TvMoviesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final int SEARCH_ANIME_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bottom Navigation Setup
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_anime) {
                selectedFragment = new AnimeFragment();
            } else if (itemId == R.id.nav_kdrama) {
                selectedFragment = new KDramaFragment();
            } else if (itemId == R.id.nav_tv_movies) {
                selectedFragment = new TvMoviesFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }

            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_anime);
        }

        // Add button click listener
        ImageButton btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchAnimeActivity.class);
            startActivityForResult(intent, SEARCH_ANIME_REQUEST_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SEARCH_ANIME_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Anime selectedAnime = (Anime) data.getSerializableExtra("selected_anime");
                if (selectedAnime != null) {
                    // Add to watching list
                    AnimeFragment.addToWatchingList(selectedAnime);

                    // Show toast notification
                    Toast.makeText(this,
                            "Added " + selectedAnime.getTitleRomaji() + " to watching list",
                            Toast.LENGTH_SHORT).show();

                    // Refresh the Anime Fragment to show updated list
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new AnimeFragment())
                            .commit();
                }
            }
        }
    }
}