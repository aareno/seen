package com.aareno.seen;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private static final int NAV_ANIME = R.id.animeTabCheckbox;
    private static final int NAV_KDRAMA = R.id.kdramaTabCheckbox;
    private static final int NAV_TVMOVIES = R.id.tvMoviesTabCheckbox;
    private SharedPreferences sharedPreferences;
    private Switch notificationsSwitch;
    private CheckBox animeTabCheckbox;
    private CheckBox kdramaTabCheckbox;
    private CheckBox tvMoviesTabCheckbox;
    private boolean tabSettingsChanged = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(
                "AppSettings", Context.MODE_PRIVATE);

        // Initialize views
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        animeTabCheckbox = view.findViewById(R.id.animeTabCheckbox);
        kdramaTabCheckbox = view.findViewById(R.id.kdramaTabCheckbox);
        tvMoviesTabCheckbox = view.findViewById(R.id.tvMoviesTabCheckbox);

        // Load saved settings
        loadSettings();

        // Set up listeners
        setupListeners();

        return view;
    }

    private void loadSettings() {
        // Load notification preference
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        notificationsSwitch.setChecked(notificationsEnabled);

        // Load tab visibility preferences
        boolean animeEnabled = sharedPreferences.getBoolean("anime_tab_enabled", true);
        boolean kdramaEnabled = sharedPreferences.getBoolean("kdrama_tab_enabled", true);
        boolean tvMoviesEnabled = sharedPreferences.getBoolean("tvmovies_tab_enabled", true);

        animeTabCheckbox.setChecked(animeEnabled);
        kdramaTabCheckbox.setChecked(kdramaEnabled);
        tvMoviesTabCheckbox.setChecked(tvMoviesEnabled);
    }

    private void setupListeners() {
        // Notification switch listener
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();
        });

        // Tab checkbox listeners
        CompoundButton.OnCheckedChangeListener tabListener = (buttonView, isChecked) -> {
            // Count how many tabs are enabled
            int enabledTabs = (animeTabCheckbox.isChecked() ? 1 : 0) +
                    (kdramaTabCheckbox.isChecked() ? 1 : 0) +
                    (tvMoviesTabCheckbox.isChecked() ? 1 : 0);

            // If trying to uncheck the last enabled tab, prevent it
            if (enabledTabs == 0) {
                buttonView.setChecked(true);
                Toast.makeText(getContext(), "At least one tab must be enabled", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save the new state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            int id = buttonView.getId();
            if (id == NAV_ANIME) {
                editor.putBoolean("anime_tab_enabled", isChecked);
            } else if (id == NAV_KDRAMA) {
                editor.putBoolean("kdrama_tab_enabled", isChecked);
            } else if (id == NAV_TVMOVIES) {
                editor.putBoolean("tvmovies_tab_enabled", isChecked);
            }
            editor.apply();

            // Notify MainActivity to update tabs
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateTabVisibility();
            }
        };

        animeTabCheckbox.setOnCheckedChangeListener(tabListener);
        kdramaTabCheckbox.setOnCheckedChangeListener(tabListener);
        tvMoviesTabCheckbox.setOnCheckedChangeListener(tabListener);
    }
}