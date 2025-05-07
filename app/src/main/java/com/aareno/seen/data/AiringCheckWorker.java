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
import java.util.Calendar;
import java.util.List;

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
        int currentDay = today.get(Calendar.DAY_OF_WEEK);
        int adjustedDay = (currentDay == Calendar.SUNDAY) ? 7 : currentDay - 1;
        Log.d(TAG, "Current adjusted day: " + adjustedDay);

        Context context = getApplicationContext();
        AnimeRepository repository = new AnimeRepository(context);
        NotificationHelper notificationHelper = new NotificationHelper(context);

        // Test notification to verify notification system works
        Log.d(TAG, "Sending test notification");
        notificationHelper.showAiringNotification(
                "Test Notification",
                "This is a test notification from AiringCheckWorker"
        );

        repository.getWatchingAnime(new AnimeRepository.OnDataLoadedCallback<List<Anime>>() {
            @Override
            public void onDataLoaded(List<Anime> animeList) {
                Log.d(TAG, "getWatchingAnime callback received, size: " + animeList.size());

                for (Anime anime : animeList) {
                    Log.d(TAG, "Checking anime: " + anime.getTitleEnglish());
                    Log.d(TAG, "Airing days: " + anime.getAiringDays());
                    Log.d(TAG, "Is watching: " + anime.isWatching());

                    if (anime.getAiringDays() != null && anime.getAiringDays().contains(adjustedDay)) {
                        Log.d(TAG, "Found matching anime: " + anime.getTitleEnglish());
                        notificationHelper.showAiringNotification(
                                "New Episode Today!",
                                anime.getTitleEnglish() + " has a new episode today!"
                        );
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error in getWatchingAnime", e);
            }
        });


        // Check kdramas
        kdramaRepository.getWatchingKdrama(new KDramaRepository.OnDataLoadedCallback<List<KDrama>>() {
            @Override
            public void onDataLoaded(List<KDrama> kdramaList) {
                for (KDrama kdrama : kdramaList) {
                    if (isAiringToday(kdrama, today)) {
                        notificationHelper.showAiringNotification(
                                "New Episode Today!",
                                kdrama.getTitleEnglish() + " has a new episode today!"
                        );
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("AiringCheckWorker", "Error checking kdramas", e);
            }
        });

        // Check Show
        showRepository.getWatchingShow(new ShowRepository.OnDataLoadedCallback<List<Show>>() {
            @Override
            public void onDataLoaded(List<Show> kdramaList) {
                for (Show kdrama : kdramaList) {
                    if (isAiringToday(kdrama, today)) {
                        notificationHelper.showAiringNotification(
                                "New Episode Today!",
                                kdrama.getTitleEnglish() + " has a new episode today!"
                        );
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("AiringCheckWorker", "Error checking kdramas", e);
            }
        });

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
