package com.aareno.seen;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.aareno.seen.R;
import com.aareno.seen.ui.Anime.AnimeFragment;
import com.aareno.seen.ui.KDrama.KDramaFragment;
import com.aareno.seen.ui.TvMovies.TvMoviesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Undo and Add button setup
        ImageButton btnUndo = findViewById(R.id.btn_undo);
        ImageButton btnAdd = findViewById(R.id.btn_add);

        btnUndo.setOnClickListener(v -> {
            // Undo action
            Toast.makeText(this, "Undo clicked", Toast.LENGTH_SHORT).show();
        });

        btnAdd.setOnClickListener(v -> {
            // Add action
            Toast.makeText(this, "Add clicked", Toast.LENGTH_SHORT).show();
        });

        // Your existing bottom navigation setup
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
            }
        });

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_anime);
        }
    }
}