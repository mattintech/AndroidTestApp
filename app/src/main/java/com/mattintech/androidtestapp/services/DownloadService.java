package com.mattintech.androidtestapp.services;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import com.mattintech.androidtestapp.Constants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadService extends Service {
    private static final String TAG = Constants.LOG_TAG + "DownloadService";
    public static final String ACTION_DOWNLOAD_PROGRESS = "com.mattintech.DOWNLOAD_PROGRESS";
    public static final String ACTION_DOWNLOAD_COMPLETE = "com.mattintech.DOWNLOAD_COMPLETE";
    
    private ExecutorService executorService;
    private Handler mainHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(10);
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String url = intent.getStringExtra("url");
            boolean saveFile = intent.getBooleanExtra("saveFile", false);
            long downloadId = intent.getLongExtra("downloadId", System.currentTimeMillis());
            
            if (url != null) {
                executorService.execute(() -> downloadFile(url, saveFile, downloadId));
            }
        }
        return START_NOT_STICKY;
    }
    
    private void downloadFile(String urlString, boolean saveFile, long downloadId) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        
        Log.d(TAG, "Starting download: " + urlString);
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error code: " + responseCode);
                sendDownloadError(downloadId, "HTTP error: " + responseCode, urlString);
                return;
            }
            
            int fileLength = connection.getContentLength();
            Log.d(TAG, "File length: " + fileLength + " bytes");
            inputStream = connection.getInputStream();
            
            File outputFile = null;
            if (saveFile) {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                outputFile = new File(downloadsDir, "download_" + downloadId + ".tmp");
                outputStream = new FileOutputStream(outputFile);
            }
            
            byte[] buffer = new byte[4096];
            long total = 0;
            int count;
            long lastProgressUpdate = 0;
            
            while ((count = inputStream.read(buffer)) != -1) {
                total += count;
                
                if (saveFile && outputStream != null) {
                    outputStream.write(buffer, 0, count);
                }
                
                // Update progress more frequently - every 10KB for smaller files, 100KB for larger
                long updateInterval = fileLength < 1048576 ? 10240 : 102400; // 10KB if < 1MB, else 100KB
                if (fileLength > 0 && (total - lastProgressUpdate) > updateInterval) {
                    lastProgressUpdate = total;
                    int progress = (int) ((total * 100) / fileLength);
                    Log.d(TAG, "Progress: " + progress + "% (" + total + "/" + fileLength + ")");
                    sendProgressUpdate(downloadId, progress, urlString);
                }
            }
            
            // Send final 100% progress update
            if (fileLength > 0) {
                Log.d(TAG, "Download complete, sending 100% progress");
                sendProgressUpdate(downloadId, 100, urlString);
            }
            
            // Send completion
            Log.d(TAG, "Download finished, total bytes: " + total);
            sendDownloadComplete(downloadId, total, urlString);
            
        } catch (IOException e) {
            Log.e(TAG, "Download failed", e);
            sendDownloadError(downloadId, "Download failed: " + e.getMessage(), urlString);
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                if (connection != null) connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
    
    private void sendProgressUpdate(long downloadId, int progress, String url) {
        Intent intent = new Intent(ACTION_DOWNLOAD_PROGRESS);
        intent.setPackage(getPackageName()); // Restrict to this app
        intent.putExtra("downloadId", downloadId);
        intent.putExtra("progress", progress);
        intent.putExtra("url", url);
        Log.d(TAG, "Sending progress broadcast for download " + downloadId + ": " + progress + "%");
        sendBroadcast(intent);
    }
    
    private void sendDownloadComplete(long downloadId, long bytesDownloaded, String url) {
        Intent intent = new Intent(ACTION_DOWNLOAD_COMPLETE);
        intent.setPackage(getPackageName()); // Restrict to this app
        intent.putExtra("downloadId", downloadId);
        intent.putExtra("bytesDownloaded", bytesDownloaded);
        intent.putExtra("url", url);
        Log.d(TAG, "Sending download complete broadcast for download " + downloadId + ": " + bytesDownloaded + " bytes");
        sendBroadcast(intent);
    }
    
    private void sendDownloadError(long downloadId, String error, String url) {
        Intent intent = new Intent(ACTION_DOWNLOAD_COMPLETE);
        intent.setPackage(getPackageName()); // Restrict to this app
        intent.putExtra("downloadId", downloadId);
        intent.putExtra("error", error);
        intent.putExtra("bytesDownloaded", 0L);
        intent.putExtra("url", url);
        Log.d(TAG, "Sending download error broadcast for download " + downloadId + ": " + error);
        sendBroadcast(intent);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}