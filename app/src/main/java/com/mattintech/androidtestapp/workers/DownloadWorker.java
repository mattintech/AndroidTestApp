package com.mattintech.androidtestapp.workers;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.mattintech.androidtestapp.services.DownloadService;

public class DownloadWorker extends Worker {
    
    public DownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        String url = getInputData().getString("url");
        boolean saveFile = getInputData().getBoolean("saveFile", false);
        boolean background = getInputData().getBoolean("background", false);
        
        if (url == null || url.isEmpty()) {
            return Result.failure();
        }
        
        // Start download service
        Intent intent = new Intent(getApplicationContext(), DownloadService.class);
        intent.putExtra("url", url);
        intent.putExtra("saveFile", saveFile);
        intent.putExtra("downloadId", System.currentTimeMillis());
        getApplicationContext().startService(intent);
        
        return Result.success();
    }
}