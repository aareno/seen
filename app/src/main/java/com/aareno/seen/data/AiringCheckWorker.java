package com.aareno.seen.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aareno.seen.NotificationHelper;
import com.aareno.seen.data.Anime.AnimeRepository;
import com.aareno.seen.data.KDrama.KDramaRepository;
import com.aareno.seen.data.TvMovies.ShowRepository;
import com.aareno.seen.ui.Anime.Anime;
import com.aareno.seen.ui.KDrama.KDrama;
import com.aareno.seen.ui.TvMovies.Show;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AiringCheckWorker extends Worker {
    private static final String TAG = "AiringCheckWorker";
    private final Context context;
    private final AnimeRepository animeRepository;
    private final KDramaRepository kdramaRepository;
    private final ShowRepository showRepository;
    private final NotificationHelper notificationHelper;

    public AiringCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.animeRepository = new AnimeRepository(context);
        this.kdramaRepository = new KDramaRepository(context);
        this.showRepository = new ShowRepository(context);
        this.notificationHelper = new NotificationHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork() started");
        try {
            checkAiringShows();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork()", e);
            return Result.failure();
        }
    }

    private void checkAiringShows() {
        Log.d(TAG, "checkAiringShows() started");
        Calendar today = Calendar.getInstance();
        List<String> airingTodayShows = new ArrayList<>();

        // Create CountDownLatch to wait for all async operations
        CountDownLatch latch = new CountDownLatch(3);

        // Check Anime
        animeRepository.getWatchingAnime(new AnimeRepository.OnDataLoadedCallback<List<Anime>>() {
            @Override
            public void onDataLoaded(List<Anime> animeList) {
                for (Anime anime : animeList) {
                    if (isAiringToday(anime, today)) {
                        airingTodayShows.add(anime.getTitleEnglish());
                    }
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking anime", e);
                latch.countDown();
            }
        });

        // Check KDrama
        kdramaRepository.getWatchingKdrama(new KDramaRepository.OnDataLoadedCallback<List<KDrama>>() {
            @Override
            public void onDataLoaded(List<KDrama> kdramaList) {
                for (KDrama kdrama : kdramaList) {
                    if (isAiringToday(kdrama, today)) {
                        airingTodayShows.add(kdrama.getTitleEnglish());
                    }
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking kdramas", e);
                latch.countDown();
            }
        });

        // Check Shows
        showRepository.getWatchingShow(new ShowRepository.OnDataLoadedCallback<List<Show>>() {
            @Override
            public void onDataLoaded(List<Show> showList) {
                for (Show show : showList) {
                    if (isAiringToday(show, today)) {
                        airingTodayShows.add(show.getTitleEnglish());
                    }
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking shows", e);
                latch.countDown();
            }
        });

        // Wait for all async operations to complete
        try {
            latch.await();

            // Send combined notification if there are any shows airing today
            if (!airingTodayShows.isEmpty()) {
                String notificationTitle = "New Episodes Today!";
                String notificationContent = buildNotificationContent(airingTodayShows);
                notificationHelper.showAiringNotification(notificationTitle, notificationContent);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Error waiting for async operations", e);
        }
    }

    private String buildNotificationContent(List<String> shows) {
        if (shows.size() == 1) {
            return shows.get(0) + " has a new episode today!";
        }

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < shows.size(); i++) {
            if (i == shows.size() - 1) {
                content.append("and ").append(shows.get(i));
            } else if (i == shows.size() - 2) {
                content.append(shows.get(i)).append(" ");
            } else {
                content.append(shows.get(i)).append(", ");
            }
        }
        content.append(" have new episodes today!");
        return content.toString();
    }

    private boolean isAiringToday(Anime anime, Calendar today) {
        // Calendar.DAY_OF_WEEK uses different values: 1 = Sunday, 2 = Monday, etc.
        // So we need to convert it to match our 1 = Monday format
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        // Convert Sunday (1) to 7, and others to 1-6
        int adjustedDay = (dayOfWeek == Calendar.SUNDAY) ? 7 : dayOfWeek - 1;

        return anime.getAiringStatus() == Anime.AiringStatus.ONGOING &&
                anime.getAiringDays().contains(adjustedDay);
    }

    private boolean isAiringToday(KDrama anime, Calendar today) {
        // Calendar.DAY_OF_WEEK uses different values: 1 = Sunday, 2 = Monday, etc.
        // So we need to convert it to match our 1 = Monday format
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        // Convert Sunday (1) to 7, and others to 1-6
        int adjustedDay = (dayOfWeek == Calendar.SUNDAY) ? 7 : dayOfWeek - 1;

        return anime.getAiringStatus() == Anime.AiringStatus.ONGOING &&
                anime.getAiringDays().contains(adjustedDay);
    }

    private boolean isAiringToday(Show anime, Calendar today) {
        // Calendar.DAY_OF_WEEK uses different values: 1 = Sunday, 2 = Monday, etc.
        // So we need to convert it to match our 1 = Monday format
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        // Convert Sunday (1) to 7, and others to 1-6
        int adjustedDay = (dayOfWeek == Calendar.SUNDAY) ? 7 : dayOfWeek - 1;

        return anime.getAiringStatus() == Anime.AiringStatus.ONGOING &&
                anime.getAiringDays().contains(adjustedDay);
    }
}
