package com.aareno.seen.data;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class WorkScheduler {
    public static void schedulePeriodicUpdate(Context context, int contentId, String contentType) {
        Data inputData = new Data.Builder()
                .putInt("CONTENT_ID", contentId)
                .putString("CONTENT_TYPE", contentType)
                .build();

        PeriodicWorkRequest updateWorkRequest =
                new PeriodicWorkRequest.Builder(EpisodeCountUpdateWorker.class, 12, TimeUnit.HOURS)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(context).enqueue(updateWorkRequest);
    }

    private static final String AIRING_WORK_NAME = "airing_notification_work";

    public static void scheduleAiringNotifications(Context context) {
        Log.d("WorkScheduler", "Scheduling airing notifications");
        // Run daily at a specific time (e.g., 9 AM)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest airingWorkRequest = new PeriodicWorkRequest.Builder(
                AiringCheckWorker.class,
                1, TimeUnit.MINUTES)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        AIRING_WORK_NAME,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        airingWorkRequest
                );
    }

    // Optional: Method to cancel airing notifications
    public static void cancelAiringNotifications(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(AIRING_WORK_NAME);
    }

}