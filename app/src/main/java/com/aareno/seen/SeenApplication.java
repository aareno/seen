package com.aareno.seen;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class SeenApplication extends Application {
    private static final String TAG = "SeenApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully in Application class");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase: " + e.getMessage(), e);
        }
    }
}
