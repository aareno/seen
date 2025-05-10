package com.aareno.seen.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.content.Intent;

import com.aareno.seen.auth.UserAuthManager;
import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.aareno.seen.data.TvMovies.ShowRepository;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.KDrama.KDrama;
import com.aareno.seen.ui.TvMovies.Show;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DataSyncManager {
    private static final String TAG = "DataSyncManager";
    private static DataSyncManager instance;

    private final Context context;
    private final UserAuthManager authManager;
    private final FirebaseFirestore firestore;
    private final AnimeRepository animeRepository;
    private final KDramaRepository kdramaRepository;
    private final ShowRepository showRepository;

    private boolean isSyncInProgress = false;

    // Interface for sync callbacks
    public interface SyncCallback {
        void onSyncComplete(boolean success);
    }

    private DataSyncManager(Context context) {
        this.context = context.getApplicationContext();
        
        // Initialize repositories regardless of Firebase status
        this.animeRepository = new AnimeRepository(context);
        this.kdramaRepository = new KDramaRepository(context);
        this.showRepository = new ShowRepository(context);
        
        // Initialize Firebase components with proper error handling
        FirebaseFirestore firestoreInstance = null;
        UserAuthManager authManagerInstance = null;
        
        try {
            // Check if Firebase is already initialized, if not initialize it
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context);
                Log.d(TAG, "Firebase initialized in DataSyncManager");
            }
            
            authManagerInstance = UserAuthManager.getInstance(context);
            firestoreInstance = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase components", e);
            // Still try to get authManager even if Firestore fails
            try {
                authManagerInstance = UserAuthManager.getInstance(context);
            } catch (Exception authEx) {
                Log.e(TAG, "Failed to initialize UserAuthManager", authEx);
            }
        }
        
        // Assign the final fields once
        this.authManager = authManagerInstance;
        this.firestore = firestoreInstance;
    }

    public static synchronized DataSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataSyncManager(context);
        }
        return instance;
    }

    public void syncData(SyncCallback callback) {
        // If Firebase Firestore is not available, return early
        if (firestore == null) {
            Log.w(TAG, "Cannot sync: Firestore is not initialized");
            showFirestoreSetupDialog("Firestore Not Initialized", 
                "Firebase Firestore is not properly initialized. Please check your configuration.");
            if (callback != null) {
                callback.onSyncComplete(false);
            }
            return;
        }
        
        if (authManager == null) {
            Log.w(TAG, "Cannot sync: Auth manager is not initialized");
            if (callback != null) {
                callback.onSyncComplete(false);
            }
            return;
        }
        
        if (!authManager.isUserSignedIn()) {
            Log.w(TAG, "Cannot sync: User not signed in");
            if (callback != null) {
                callback.onSyncComplete(false);
            }
            return;
        }

        if (isSyncInProgress) {
            Log.w(TAG, "Sync already in progress, skipping");
            return;
        }

        if (!isNetworkAvailable()) {
            Log.w(TAG, "Cannot sync: Network unavailable");
            showOfflineDialog();
            if (callback != null) {
                callback.onSyncComplete(false);
            }
            return;
        }

        isSyncInProgress = true;
        String userId = authManager.getUserId();
        Log.d(TAG, "Starting sync for user: " + userId);

        // Use a more reliable collection for checking Firestore API status
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                // If we got here, Firestore is working - proceed with sync
                performFullSync(userId, callback);
            })
            .addOnFailureListener(e -> {
                isSyncInProgress = false;
                Log.e(TAG, "Firestore API check failed", e);
                
                // Handle offline state specifically
                if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException &&
                    e.getMessage() != null && e.getMessage().contains("client is offline")) {
                    
                    showOfflineDialog();
                    if (callback != null) {
                        callback.onSyncComplete(false);
                    }
                    return;
                }
                
                // Check if the error is about API being disabled
                String errorMessage = e.getMessage();
                if (errorMessage != null && 
                    (errorMessage.contains("Firestore API has not been used") || 
                     errorMessage.contains("API has not been used"))) {
                    
                    Log.e(TAG, "Firestore API is not enabled for this project");
                    showFirestoreSetupDialog("Firestore API Not Enabled",
                        "The Firestore API is not enabled in your Firebase project. You need to enable it in the Firebase Console.");
                    
                    if (callback != null) {
                        callback.onSyncComplete(false);
                    }
                } else {
                    // Some other error occurred
                    showGenericErrorDialog("Sync Error", "Could not connect to Firestore: " + errorMessage);
                    if (callback != null) {
                        callback.onSyncComplete(false);
                    }
                }
            });
    }

    // New method to perform the actual sync after Firestore API check
    private void performFullSync(String userId, SyncCallback callback) {
        // Use AtomicInteger to track completion of multiple async operations
        AtomicInteger pendingOperations = new AtomicInteger(3); // 3 content types
        final boolean[] syncSuccess = {true}; // Track overall success

        // Sync Anime
        syncAnime(userId, success -> {
            if (!success) {
                syncSuccess[0] = false;
            }
            if (pendingOperations.decrementAndGet() == 0) {
                isSyncInProgress = false;
                if (callback != null) {
                    callback.onSyncComplete(syncSuccess[0]);
                }
            }
        });

        // Sync KDrama
        syncKDrama(userId, success -> {
            if (!success) {
                syncSuccess[0] = false;
            }
            if (pendingOperations.decrementAndGet() == 0) {
                isSyncInProgress = false;
                if (callback != null) {
                    callback.onSyncComplete(syncSuccess[0]);
                }
            }
        });

        // Sync Shows
        syncShows(userId, success -> {
            if (!success) {
                syncSuccess[0] = false;
            }
            if (pendingOperations.decrementAndGet() == 0) {
                isSyncInProgress = false;
                if (callback != null) {
                    callback.onSyncComplete(syncSuccess[0]);
                }
            }
        });
    }

    private void syncAnime(String userId, SyncCallback callback) {
        Log.d(TAG, "Syncing Anime data");
        
        // Reference to the user's anime collection
        CollectionReference animeCollection = firestore
                .collection("users")
                .document(userId)
                .collection("anime");
        
        // First, get all local anime
        animeRepository.getWatchingAnime(new AnimeRepository.OnDataLoadedCallback<List<Anime>>() {
            @Override
            public void onDataLoaded(List<Anime> watchingAnime) {
                animeRepository.getWatchedAnime(new AnimeRepository.OnDataLoadedCallback<List<Anime>>() {
                    @Override
                    public void onDataLoaded(List<Anime> watchedAnime) {
                        // Combine watching and watched anime
                        List<Anime> allAnime = new ArrayList<>(watchingAnime);
                        allAnime.addAll(watchedAnime);
                        
                        // Get cloud data
                        animeCollection.get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    try {
                                        // Create maps for easier lookups
                                        Map<Integer, Anime> localAnimeMap = new HashMap<>();
                                        for (Anime anime : allAnime) {
                                            localAnimeMap.put(anime.getId(), anime);
                                        }
                                        
                                        Map<Integer, QueryDocumentSnapshot> cloudAnimeMap = new HashMap<>();
                                        List<Anime> cloudAnimeList = new ArrayList<>();
                                        
                                        // Process cloud data
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            try {
                                                Anime anime = document.toObject(Anime.class);
                                                cloudAnimeMap.put(anime.getId(), document);
                                                cloudAnimeList.add(anime);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error converting cloud anime", e);
                                            }
                                        }
                                        
                                        // Sync bidirectionally
                                        syncItemsBidirectionally(
                                                localAnimeMap, 
                                                cloudAnimeMap, 
                                                cloudAnimeList,
                                                animeCollection,
                                                animeRepository,
                                                "anime",
                                                callback);
                                        
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error during anime sync", e);
                                        if (callback != null) {
                                            callback.onSyncComplete(false);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore get failed for anime", e);
                                    if (callback != null) {
                                        callback.onSyncComplete(false);
                                    }
                                });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error fetching watched anime", e);
                        if (callback != null) {
                            callback.onSyncComplete(false);
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching watching anime", e);
                if (callback != null) {
                    callback.onSyncComplete(false);
                }
            }
        });
    }

    private void syncKDrama(String userId, SyncCallback callback) {
        Log.d(TAG, "Syncing KDrama data");
        
        // Reference to the user's kdrama collection
        CollectionReference kdramaCollection = firestore
                .collection("users")
                .document(userId)
                .collection("kdrama");
        
        // First, get all local kdramas
        kdramaRepository.getWatchingKdrama(new KDramaRepository.OnDataLoadedCallback<List<KDrama>>() {
            @Override
            public void onDataLoaded(List<KDrama> watchingKDrama) {
                kdramaRepository.getWatchedKdrama(new KDramaRepository.OnDataLoadedCallback<List<KDrama>>() {
                    @Override
                    public void onDataLoaded(List<KDrama> watchedKDrama) {
                        // Combine watching and watched kdramas
                        List<KDrama> allKDrama = new ArrayList<>(watchingKDrama);
                        allKDrama.addAll(watchedKDrama);
                        
                        // Get cloud data
                        kdramaCollection.get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    try {
                                        // Create maps for easier lookups
                                        Map<Integer, KDrama> localKDramaMap = new HashMap<>();
                                        for (KDrama kdrama : allKDrama) {
                                            localKDramaMap.put(kdrama.getId(), kdrama);
                                        }
                                        
                                        Map<Integer, QueryDocumentSnapshot> cloudKDramaMap = new HashMap<>();
                                        List<KDrama> cloudKDramaList = new ArrayList<>();
                                        
                                        // Process cloud data
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            try {
                                                KDrama kdrama = document.toObject(KDrama.class);
                                                cloudKDramaMap.put(kdrama.getId(), document);
                                                cloudKDramaList.add(kdrama);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error converting cloud kdrama", e);
                                            }
                                        }
                                        
                                        // Sync bidirectionally
                                        syncItemsBidirectionally(
                                                localKDramaMap, 
                                                cloudKDramaMap, 
                                                cloudKDramaList,
                                                kdramaCollection,
                                                kdramaRepository,
                                                "kdrama",
                                                callback);
                                        
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error during kdrama sync", e);
                                        if (callback != null) {
                                            callback.onSyncComplete(false);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore get failed for kdrama", e);
                                    if (callback != null) {
                                        callback.onSyncComplete(false);
                                    }
                                });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error fetching watched kdrama", e);
                        if (callback != null) {
                            callback.onSyncComplete(false);
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching watching kdrama", e);
                if (callback != null) {
                    callback.onSyncComplete(false);
                }
            }
        });
    }

    private void syncShows(String userId, SyncCallback callback) {
        Log.d(TAG, "Syncing Shows data");
        
        // Reference to the user's shows collection
        CollectionReference showsCollection = firestore
                .collection("users")
                .document(userId)
                .collection("shows");
        
        // First, get all local shows
        showRepository.getWatchingShow(new ShowRepository.OnDataLoadedCallback<List<Show>>() {
            @Override
            public void onDataLoaded(List<Show> watchingShows) {
                showRepository.getWatchedShow(new ShowRepository.OnDataLoadedCallback<List<Show>>() {
                    @Override
                    public void onDataLoaded(List<Show> watchedShows) {
                        // Combine watching and watched shows
                        List<Show> allShows = new ArrayList<>(watchingShows);
                        allShows.addAll(watchedShows);
                        
                        // Get cloud data
                        showsCollection.get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    try {
                                        // Create maps for easier lookups
                                        Map<Integer, Show> localShowMap = new HashMap<>();
                                        for (Show show : allShows) {
                                            localShowMap.put(show.getId(), show);
                                        }
                                        
                                        Map<Integer, QueryDocumentSnapshot> cloudShowMap = new HashMap<>();
                                        List<Show> cloudShowList = new ArrayList<>();
                                        
                                        // Process cloud data
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            try {
                                                Show show = document.toObject(Show.class);
                                                cloudShowMap.put(show.getId(), document);
                                                cloudShowList.add(show);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error converting cloud show", e);
                                            }
                                        }
                                        
                                        // Sync bidirectionally
                                        syncItemsBidirectionally(
                                                localShowMap, 
                                                cloudShowMap, 
                                                cloudShowList,
                                                showsCollection,
                                                showRepository,
                                                "show",
                                                callback);
                                        
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error during shows sync", e);
                                        if (callback != null) {
                                            callback.onSyncComplete(false);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore get failed for shows", e);
                                    if (callback != null) {
                                        callback.onSyncComplete(false);
                                    }
                                });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error fetching watched shows", e);
                        if (callback != null) {
                            callback.onSyncComplete(false);
                        }
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching watching shows", e);
                if (callback != null) {
                    callback.onSyncComplete(false);
                }
            }
        });
    }

    // Generic method to sync items bidirectionally
    private <T> void syncItemsBidirectionally(
            Map<Integer, T> localItems,
            Map<Integer, QueryDocumentSnapshot> cloudItems,
            List<T> cloudItemsList,
            CollectionReference collection,
            Object repository,
            String itemType,
            SyncCallback callback) {

        AtomicInteger pendingOperations = new AtomicInteger(0);
        final boolean[] success = {true};

        // Handle local items not in cloud (upload to cloud)
        for (Map.Entry<Integer, T> entry : localItems.entrySet()) {
            Integer itemId = entry.getKey();
            T localItem = entry.getValue();

            if (!cloudItems.containsKey(itemId)) {
                // Item exists locally but not in cloud - upload
                pendingOperations.incrementAndGet();
                collection.document(itemId.toString())
                        .set(localItem)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully uploaded " + itemType + " id: " + itemId);
                            checkCompletion(pendingOperations, success, callback);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error uploading " + itemType + " id: " + itemId, e);
                            success[0] = false;
                            checkCompletion(pendingOperations, success, callback);
                        });
            }
        }

        // Handle cloud items not in local (download to local)
        for (T cloudItem : cloudItemsList) {
            Integer itemId = getItemId(cloudItem);
            if (itemId != null && !localItems.containsKey(itemId)) {
                // Item exists in cloud but not locally - download
                pendingOperations.incrementAndGet();
                
                if (itemType.equals("anime") && repository instanceof AnimeRepository) {
                    AnimeRepository animeRepo = (AnimeRepository) repository;
                    animeRepo.insertAnime((Anime) cloudItem, new AnimeRepository.OnDataLoadedCallback<Long>() {
                        @Override
                        public void onDataLoaded(Long data) {
                            Log.d(TAG, "Successfully downloaded anime id: " + itemId);
                            checkCompletion(pendingOperations, success, callback);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error downloading anime id: " + itemId, e);
                            success[0] = false;
                            checkCompletion(pendingOperations, success, callback);
                        }
                    });
                } 
                else if (itemType.equals("kdrama") && repository instanceof KDramaRepository) {
                    KDramaRepository kdramaRepo = (KDramaRepository) repository;
                    kdramaRepo.insertKdrama((KDrama) cloudItem, new KDramaRepository.OnDataLoadedCallback<Long>() {
                        @Override
                        public void onDataLoaded(Long data) {
                            Log.d(TAG, "Successfully downloaded kdrama id: " + itemId);
                            checkCompletion(pendingOperations, success, callback);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error downloading kdrama id: " + itemId, e);
                            success[0] = false;
                            checkCompletion(pendingOperations, success, callback);
                        }
                    });
                }
                else if (itemType.equals("show") && repository instanceof ShowRepository) {
                    ShowRepository showRepo = (ShowRepository) repository;
                    showRepo.insertShow((Show) cloudItem, new ShowRepository.OnDataLoadedCallback<Long>() {
                        @Override
                        public void onDataLoaded(Long data) {
                            Log.d(TAG, "Successfully downloaded show id: " + itemId);
                            checkCompletion(pendingOperations, success, callback);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error downloading show id: " + itemId, e);
                            success[0] = false;
                            checkCompletion(pendingOperations, success, callback);
                        }
                    });
                }
            }
        }

        // If there are no pending operations, call callback immediately
        if (pendingOperations.get() == 0) {
            if (callback != null) {
                callback.onSyncComplete(success[0]);
            }
        }
    }

    private void checkCompletion(AtomicInteger pendingOperations, boolean[] success, SyncCallback callback) {
        if (pendingOperations.decrementAndGet() == 0 && callback != null) {
            callback.onSyncComplete(success[0]);
        }
    }

    // Helper method to get ID from different item types
    private Integer getItemId(Object item) {
        if (item instanceof Anime) {
            return ((Anime) item).getId();
        } else if (item instanceof KDrama) {
            return ((KDrama) item).getId();
        } else if (item instanceof Show) {
            return ((Show) item).getId();
        }
        return null;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // New method to show a dialog for offline mode
    private void showOfflineDialog() {
        if (!(context instanceof android.app.Activity)) return;
        
        ((android.app.Activity) context).runOnUiThread(() -> {
            try {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setTitle("Offline Mode")
                    .setMessage("You appear to be offline or have connectivity issues. The app will work in offline mode.")
                    .setPositiveButton("OK", null)
                    .show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing offline dialog", e);
            }
        });
    }

    // New method to show Firestore setup dialog with direct link
    private void showFirestoreSetupDialog(String title, String message) {
        if (!(context instanceof android.app.Activity)) return;
        
        ((android.app.Activity) context).runOnUiThread(() -> {
            try {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setTitle(title)
                    .setMessage(message + "\n\nWould you like to open the Firebase console to enable Firestore?")
                    .setPositiveButton("Open Firebase Console", (dialog, which) -> {
                        // Open the Firebase console to enable Firestore
                        String url = "https://console.firebase.google.com/project/seen-907b9/firestore/databases/-default-/data";
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(android.net.Uri.parse(url));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing setup dialog", e);
            }
        });
    }

    private void showGenericErrorDialog(String title, String message) {
        if (!(context instanceof android.app.Activity)) return;
        
        ((android.app.Activity) context).runOnUiThread(() -> {
            try {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing error dialog", e);
            }
        });
    }
}
