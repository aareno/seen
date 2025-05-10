package com.aareno.seen.data.TvMovies;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.aareno.seen.data.KDrama.KDramaDatabase;
import com.aareno.seen.ui.KDrama.KDrama;
import com.aareno.seen.ui.TvMovies.Show;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShowRepository {
    private ShowDatabase database;
    private ExecutorService executorService;

    public interface OnDataLoadedCallback<T> {
        void onDataLoaded(T data);
        void onError(Exception e);
    }

    public ShowRepository(Context context) {
        database = ShowDatabase.getInstance(context);
        executorService = Executors.newSingleThreadExecutor();
    }

    public void getWatchingShow(ShowRepository.OnDataLoadedCallback<List<Show>> callback) {
        executorService.execute(() -> {
            try {
                List<Show> watchingShow = database.ShowDao().getWatchingShow();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(watchingShow)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void getWatchedShow(ShowRepository.OnDataLoadedCallback<List<Show>> callback) {
        executorService.execute(() -> {
            try {
                List<Show> watchedShow = database.ShowDao().getWatchedShow();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onDataLoaded(watchedShow)
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError(e)
                );
            }
        });
    }

    public void insertShow(Show show, ShowRepository.OnDataLoadedCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long id = database.ShowDao().insert(show);
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

    public void updateShow(Show show, ShowRepository.OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                database.ShowDao().update(show);
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

    public void deleteShow(Show show, ShowRepository.OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                database.ShowDao().delete(show);
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

    public void updateShowEpisodeCount(int showId, int newEpisodeCount, ShowRepository.OnDataLoadedCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                Show show = database.ShowDao().getShowById(showId); // Fetch the existing KDrama
                if (show != null) {
                    show.setEpisodeCount(newEpisodeCount);
                    database.ShowDao().update(show);
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onDataLoaded(null)
                    );
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError(new Exception("Show not found"))
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
