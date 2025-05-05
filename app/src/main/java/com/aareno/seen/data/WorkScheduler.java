package com.aareno.seen.data;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

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
}