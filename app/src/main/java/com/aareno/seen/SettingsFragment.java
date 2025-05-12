package com.aareno.seen;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;

    private static final int NAV_ANIME = R.id.animeTabCheckbox;
    private static final int NAV_KDRAMA = R.id.kdramaTabCheckbox;
    private static final int NAV_TVMOVIES = R.id.tvMoviesTabCheckbox;
    private SharedPreferences sharedPreferences;
    private Switch notificationsSwitch;
    private Switch adultContentSwitch;
    private CheckBox animeTabCheckbox;
    private CheckBox kdramaTabCheckbox;
    private CheckBox tvMoviesTabCheckbox;
    private boolean tabSettingsChanged = false;
    private Button signInButtonSettings;

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
        adultContentSwitch = view.findViewById(R.id.adultContentSwitch);
        signInButtonSettings = view.findViewById(R.id.btn_sign_in_settings);

        /*
        Button testButton = view.findViewById(R.id.test_notification_button);
        testButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).testNotification();
            }
        });

           <Button
        android:id="@+id/test_notification_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Test Notification" />

         */

        // Load saved settings
        loadSettings();

        // Set up listeners
        setupListeners();
        setupSignInButton();
        return view;
    }

    private void loadSettings() {
        // Load notification preference
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        if (Build.VERSION.SDK_INT >= 33 && notificationsEnabled) {
            notificationsEnabled = ContextCompat.checkSelfPermission(requireContext(),
                    "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
        }

        notificationsSwitch.setChecked(notificationsEnabled);

        // Load tab visibility preferences
        boolean animeEnabled = sharedPreferences.getBoolean("anime_tab_enabled", true);
        boolean kdramaEnabled = sharedPreferences.getBoolean("kdrama_tab_enabled", true);
        boolean tvMoviesEnabled = sharedPreferences.getBoolean("tvmovies_tab_enabled", true);

        animeTabCheckbox.setChecked(animeEnabled);
        kdramaTabCheckbox.setChecked(kdramaEnabled);
        tvMoviesTabCheckbox.setChecked(tvMoviesEnabled);

        boolean showAdultContent = sharedPreferences.getBoolean("show_adult_content", false);
        adultContentSwitch.setChecked(showAdultContent);
    }

    private void setupListeners() {
        // Notification switch listener
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Temporarily disable to avoid instant toggle effect
                notificationsSwitch.setEnabled(false);
                requestNotificationPermission();  // Actual saving happens only if granted
            } else {
                saveNotificationSetting(false);
            }
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

        adultContentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show a confirmation dialog before enabling adult content
                new AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Age")
                        .setMessage("Are you 18 years or older? This will show mature content.")
                        .setPositiveButton("Yes, I'm 18+", (dialog, which) -> {
                            // Save the setting
                            saveAdultContentSetting(true);
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            // Reset the switch
                            adultContentSwitch.setChecked(false);
                            saveAdultContentSetting(false);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                // Save the setting directly if turning off
                saveAdultContentSetting(false);
            }
        });

        animeTabCheckbox.setOnCheckedChangeListener(tabListener);
        kdramaTabCheckbox.setOnCheckedChangeListener(tabListener);
        tvMoviesTabCheckbox.setOnCheckedChangeListener(tabListener);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    "android.permission.POST_NOTIFICATIONS") ==
                    PackageManager.PERMISSION_GRANTED) {
                // Already granted
                saveNotificationSetting(true);
            } else {
                if (shouldShowRequestPermissionRationale("android.permission.POST_NOTIFICATIONS")) {
                    // First denial or explanation required
                    requestPermissions(
                            new String[]{"android.permission.POST_NOTIFICATIONS"},
                            NOTIFICATION_PERMISSION_REQUEST_CODE
                    );
                } else {
                    // Permission denied twice → treat as "Don't ask again"
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Permission Required")
                            .setMessage("Please allow notifications from the system settings.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }
        } else {
            // Older Android — permission not needed
            saveNotificationSetting(true);
        }
    }

    private void saveNotificationSetting(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("notifications_enabled", enabled);
        editor.apply();

        // Update switch state without triggering listener
        notificationsSwitch.setOnCheckedChangeListener(null);
        notificationsSwitch.setChecked(enabled);
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestNotificationPermission();
            } else {
                saveNotificationSetting(false);
            }
        });

        // Update notification scheduling
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onNotificationSettingChanged(enabled);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            notificationsSwitch.setEnabled(true);  // Re-enable switch

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                saveNotificationSetting(true);
            } else {
                // Permission denied
                saveNotificationSetting(false);
                Toast.makeText(requireContext(),
                        "Notification permission is required to enable notifications.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveAdultContentSetting(boolean showAdultContent) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("show_adult_content", showAdultContent);
        editor.apply();

        // Update the switch state if needed
        adultContentSwitch.setChecked(showAdultContent);

        // Notify any listeners that the content filter has changed
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onContentFilterChanged(showAdultContent);
        }

        // Show feedback to the user
        String message = showAdultContent ?
                "Adult content will be shown" :
                "Adult content will be filtered out";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        // Force a refresh of the current content
        // refreshContent();
    }

    public void setupSignInButton() {
        // Set initial text
        boolean isSignedIn = false;
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            isSignedIn = main.userAuthManager != null && main.userAuthManager.isUserSignedIn();
        }
        signInButtonSettings.setText(isSignedIn ? "Sign Out" : "Sign In");

        signInButtonSettings.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity main = (MainActivity) getActivity();
                if (main.userAuthManager != null && main.userAuthManager.isUserSignedIn()) {
                    // Sign out
                    main.clearAllUserData(() -> {
                        main.userAuthManager.signOut(task -> {
                            main.updateUIAfterSignOut();
                            main.showMessage("Successfully signed out");
                            // Update button text
                            signInButtonSettings.setText("Sign In With Google");
                        });
                    });
                } else {
                    // Sign in
                    // Re-check Google Play Services
                    com.google.android.gms.common.GoogleApiAvailability googleApi =
                            com.google.android.gms.common.GoogleApiAvailability.getInstance();
                    int resultCode = googleApi.isGooglePlayServicesAvailable(requireContext());
                    if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                        if (googleApi.isUserResolvableError(resultCode)) {
                            googleApi.getErrorDialog(requireActivity(), resultCode, 9000).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Google Play Services is required but not available on this device",
                                    Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    if (main.signInLauncher == null) {
                        Toast.makeText(requireContext(),
                                "Sign-in functionality is not available",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Intent signInIntent = main.userAuthManager.getSignInIntent();
                        if (signInIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                            main.signInLauncher.launch(signInIntent);
                        } else {
                            Toast.makeText(requireContext(),
                                    "Google Sign-In is not available on this device",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(),
                                "Failed to start sign-in: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update sign in/out button text when returning to settings
        if (signInButtonSettings != null && getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            boolean isSignedIn = main.userAuthManager != null && main.userAuthManager.isUserSignedIn();
            signInButtonSettings.setText(isSignedIn ? "Sign Out" : "Sign In");
        }
    }
}