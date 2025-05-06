package com.aareno.seen.data.KDrama;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.KDrama.KDrama;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KDramaRepository {
    private KDramaDatabase database;
    private ExecutorService executorService;

    public interface OnDataLoadedCallback<T> {
        void onDataLoaded(T data);
        void onError(Exception e);
    }

    public KDramaRepository(Context context) {
        database = KDramaDatabase.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
    }

    public void getWatchingKdrama(KDramaRepository.OnDataLoadedCallback<List<KDrama>> callback) {
        executorService.execute(() -> {
            try {
                List<KDrama> watchingKdrama = database.kDramaDao().getWatchingKdrama();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(watchingKdrama)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
            });
    }

    public void getWatchedKdrama(KDramaRepository.OnDataLoadedCallback<List<KDrama>> callback) {
        executorService.execute(() -> {
            try {
                List<KDrama> watchedKdrama = database.kDramaDao().getWatchedKdrama();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(watchedKdrama)
                );
        } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
            });
    }

    public void insertKdrama(KDrama kdrama, KDramaRepository.OnDataLoadedCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = database.kDramaDao().insert(kdrama);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(id)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void updateKdrama(KDrama kdrama, KDramaRepository.OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                database.kDramaDao().update(kdrama);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(null)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void deleteKdrama(KDrama kdrama, KDramaRepository.OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                database.kDramaDao().delete(kdrama);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(null)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void updateKdramaEpisodeCount(int kdramaId, int newEpisodeCount, KDramaRepository.OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                KDrama kdrama = database.kDramaDao().getKdramaById(kdramaId); // Fetch the existing KDrama
                if (kdrama != null) {
                    int currentEpisodeCount = kdrama.getEpisodeCount();

                    // Only update if the episode count has changed
                    if (currentEpisodeCount != newEpisodeCount) {
                        kdrama.setEpisodeCount(newEpisodeCount);
                        database.kDramaDao().update(kdrama);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.d("KDramaRepository", "Updated episode count for KDrama ID: " + kdramaId +
                                    " from " + currentEpisodeCount + " to " + newEpisodeCount);
                            callback.onDataLoaded(null);
                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.d("KDramaRepository", "No update needed for KDrama ID: " + kdramaId +
                                    " (episode count unchanged: " + newEpisodeCount + ")");
                            callback.onDataLoaded(null);
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError(new Exception("KDrama not found"))
                    );
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
