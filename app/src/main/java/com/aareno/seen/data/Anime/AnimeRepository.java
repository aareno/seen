package com.aareno.seen.data.Anime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aareno.seen.ui.Anime.Anime;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnimeRepository {
    private AnimeDatabase database;
    private ExecutorService executorService;

    private AnimeDao animeDao;

    public AnimeRepository(Context context) {
        database = AnimeDatabase.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
    }

    public void getWatchingAnime(OnDataLoadedCallback<List<Anime>> callback) {
        executorService.execute(() -> {
            try {
                List<Anime> watchingAnime = database.animeDao().getWatchingAnime();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(watchingAnime)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void getWatchedAnime(OnDataLoadedCallback<List<Anime>> callback) {
        executorService.execute(() -> {
            try {
                List<Anime> watchedAnime = database.animeDao().getWatchedAnime();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(watchedAnime)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void insertAnime(Anime anime, OnDataLoadedCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = database.animeDao().insert(anime);
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

    public void updateAnime(Anime anime, OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                database.animeDao().update(anime);
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

    public void deleteAnime(Anime anime, OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                database.animeDao().delete(anime);
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

    public void updateAnimeEpisodeCount(String title, int newEpisodeCount, OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                Anime anime = database.animeDao().getAnimeByTitle(title);
                if (anime != null) {
                    int currentEpisodeCount = anime.getEpisodeCount();

                    // Only update if the episode count has changed
                    if (currentEpisodeCount != newEpisodeCount) {
                        anime.setEpisodeCount(newEpisodeCount);
                        database.animeDao().update(anime);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.d("AnimeRepository", "Updated episode count for Anime: " + title +
                                    " from " + currentEpisodeCount + " to " + newEpisodeCount);
                            callback.onDataLoaded(null);
                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Log.d("AnimeRepository", "No update needed for Anime: " + title +
                                    " (episode count unchanged: " + newEpisodeCount + ")");
                            callback.onDataLoaded(null);
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError(new Exception("Anime not found"))
                    );
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    /**
     * Clears all anime from the local database.
     * @param callback Callback to notify when the operation is complete
     */
    public void clearAllAnime(OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                database.animeDao().deleteAllAnime();
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

    public void shutdown() {
        executorService.shutdown();
    }

    public interface OnDataLoadedCallback<T> {
        void onDataLoaded(T data);
        void onError(Exception e);
    }
}