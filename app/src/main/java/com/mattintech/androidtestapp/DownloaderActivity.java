package com.mattintech.androidtestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkInfo;
import com.mattintech.androidtestapp.databinding.ActivityDownloaderBinding;
import com.mattintech.androidtestapp.services.DownloadService;
import com.mattintech.androidtestapp.workers.DownloadWorker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import android.app.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;

public class DownloaderActivity extends AppCompatActivity {
    private static final String TAG = Constants.LOG_TAG + "DownloaderActivity";
    private static final String PREFS_NAME = "DownloaderPrefs";
    private static final String KEY_TOTAL_BYTES = "totalBytesDownloaded";
    private static final String KEY_DOWNLOAD_COUNT = "downloadCount";
    
    private ActivityDownloaderBinding binding;
    private DownloadProgressReceiver progressReceiver;
    private SharedPreferences sharedPrefs;
    private long totalBytesDownloaded = 0;
    private int downloadCount = 0;
    private Map<Long, View> activeDownloads = new HashMap<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize SharedPreferences
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadStats();
        
        // Setup edge-to-edge
        setupWindowInsets();
        
        setupUI();
        registerProgressReceiver();
        
        // Setup toolbar navigation
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Check if this is the root activity (launched from notification)
            if (isTaskRoot()) {
                // Create a new back stack with MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
            finish();
        });
    }
    
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
    
    private void setupUI() {
        // Default URLs
        binding.etUrl.setText("https://test1.bigstam.net/download/1mb.bin");
        
        // File size spinner
        List<String> fileSizes = new ArrayList<>();
        fileSizes.add("1 MB");
        fileSizes.add("10 MB");
        fileSizes.add("100 MB");
        fileSizes.add("Custom URL");
        
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, fileSizes);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFileSize.setAdapter(sizeAdapter);
        
        // Schedule type spinner
        List<String> scheduleTypes = new ArrayList<>();
        scheduleTypes.add("One-time");
        scheduleTypes.add("Every 15 minutes");
        scheduleTypes.add("Every 30 minutes");
        scheduleTypes.add("Every 1 hour");
        scheduleTypes.add("Every 6 hours");
        scheduleTypes.add("Every 24 hours");
        
        ArrayAdapter<String> scheduleAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, scheduleTypes);
        scheduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSchedule.setAdapter(scheduleAdapter);
        
        // Network preference spinner
        List<String> networkTypes = new ArrayList<>();
        networkTypes.add("Any");
        networkTypes.add("WiFi Only");
        networkTypes.add("Cellular Only");
        
        ArrayAdapter<String> networkAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, networkTypes);
        networkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerNetwork.setAdapter(networkAdapter);
        
        // Concurrent downloads
        binding.seekBarConcurrent.setMax(9);
        binding.seekBarConcurrent.setProgress(0);
        binding.tvConcurrentValue.setText("1");
        
        binding.seekBarConcurrent.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvConcurrentValue.setText(String.valueOf(progress + 1));
            }
            
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        
        // Update URL based on file size selection
        binding.spinnerFileSize.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                switch (position) {
                    case 0: // 1 MB
                        binding.etUrl.setText("https://test1.bigstam.net/download/1mb.bin");
                        break;
                    case 1: // 10 MB
                        binding.etUrl.setText("https://test1.bigstam.net/download/10mb.bin");
                        break;
                    case 2: // 100 MB
                        binding.etUrl.setText("https://test1.bigstam.net/download/100mb.bin");
                        break;
                    case 3: // Custom
                        binding.etUrl.setText("");
                        binding.etUrl.setEnabled(true);
                        binding.etUrl.requestFocus();
                        return;
                }
                binding.etUrl.setEnabled(false);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        // Buttons
        binding.btnStartDownload.setOnClickListener(v -> startDownload());
        binding.btnStopAll.setOnClickListener(v -> stopAllDownloads());
        binding.btnClearHistory.setOnClickListener(v -> clearHistory());
        binding.btnViewScheduled.setOnClickListener(v -> viewScheduledDownloads());
        
        updateStats();
    }
    
    private void registerProgressReceiver() {
        progressReceiver = new DownloadProgressReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadService.ACTION_DOWNLOAD_PROGRESS);
        filter.addAction(DownloadService.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(progressReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        Log.d(TAG, "Registered broadcast receiver for download progress");
    }
    
    private void startDownload() {
        String url = binding.etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean saveFile = binding.cbSaveFile.isChecked();
        boolean backgroundDownload = binding.cbBackground.isChecked();
        int concurrentDownloads = binding.seekBarConcurrent.getProgress() + 1;
        String scheduleType = binding.spinnerSchedule.getSelectedItem().toString();
        String networkPref = binding.spinnerNetwork.getSelectedItem().toString();
        
        // Create network constraints
        NetworkType networkType;
        switch (networkPref) {
            case "WiFi Only":
                networkType = NetworkType.UNMETERED;
                break;
            case "Cellular Only":
                networkType = NetworkType.METERED;
                break;
            default:
                networkType = NetworkType.CONNECTED;
                break;
        }
        
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build();
        
        // Create work data
        Data inputData = new Data.Builder()
            .putString("url", url)
            .putBoolean("saveFile", saveFile)
            .putBoolean("background", backgroundDownload)
            .build();
        
        if (scheduleType.equals("One-time")) {
            // Start immediate downloads using service
            for (int i = 0; i < concurrentDownloads; i++) {
                Intent intent = new Intent(this, DownloadService.class);
                intent.putExtra("url", url);
                intent.putExtra("saveFile", saveFile);
                intent.putExtra("downloadId", System.currentTimeMillis() + i);
                Log.d(TAG, "Starting download service for URL: " + url);
                startService(intent);
            }
            Toast.makeText(this, "Started " + concurrentDownloads + " download(s)", Toast.LENGTH_SHORT).show();
        } else {
            // Schedule periodic downloads using WorkManager
            long intervalMinutes = getIntervalMinutes(scheduleType);
            
            PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                    DownloadWorker.class, intervalMinutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag("scheduled_download")
                .addTag("url:" + url)
                .addTag("interval:" + intervalMinutes)
                .build();
            
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "download_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWork
            );
            
            // Show notification for scheduled downloads
            showScheduledDownloadNotification(url, scheduleType);
            
            Toast.makeText(this, "Scheduled downloads " + scheduleType.toLowerCase(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private long getIntervalMinutes(String scheduleType) {
        switch (scheduleType) {
            case "Every 15 minutes": return 15;
            case "Every 30 minutes": return 30;
            case "Every 1 hour": return 60;
            case "Every 6 hours": return 360;
            case "Every 24 hours": return 1440;
            default: return 15;
        }
    }
    
    private void stopAllDownloads() {
        // Stop service
        stopService(new Intent(this, DownloadService.class));
        
        // Cancel scheduled work
        WorkManager.getInstance(this).cancelUniqueWork("download_work");
        
        // Cancel notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.NOTIFICATION_ID_DOWNLOAD);
        
        // Clear all progress views
        clearAllProgressViews();
        
        Toast.makeText(this, "Stopped all downloads", Toast.LENGTH_SHORT).show();
    }
    
    private void addProgressView(long downloadId, String url) {
        // Remove existing view if any
        if (activeDownloads.containsKey(downloadId)) {
            binding.progressContainer.removeView(activeDownloads.get(downloadId));
        }
        
        // Inflate new progress view
        View progressView = LayoutInflater.from(this).inflate(R.layout.item_download_progress, binding.progressContainer, false);
        
        TextView tvDownloadId = progressView.findViewById(R.id.tvDownloadId);
        TextView tvProgress = progressView.findViewById(R.id.tvDownloadProgress);
        ProgressBar progressBar = progressView.findViewById(R.id.progressBarDownload);
        
        // Extract filename from URL
        String filename = url.substring(url.lastIndexOf('/') + 1);
        tvDownloadId.setText("Download: " + filename);
        tvProgress.setText("0%");
        progressBar.setProgress(0);
        
        // Add to container
        binding.progressContainer.addView(progressView);
        activeDownloads.put(downloadId, progressView);
        
        // Hide "no downloads" text
        binding.tvNoDownloads.setVisibility(View.GONE);
        
        Log.d(TAG, "Added progress view for download " + downloadId);
    }
    
    private void updateProgressView(long downloadId, int progress) {
        View progressView = activeDownloads.get(downloadId);
        if (progressView != null) {
            TextView tvProgress = progressView.findViewById(R.id.tvDownloadProgress);
            ProgressBar progressBar = progressView.findViewById(R.id.progressBarDownload);
            
            tvProgress.setText(progress + "%");
            progressBar.setProgress(progress);
        }
    }
    
    private void removeProgressView(long downloadId) {
        View progressView = activeDownloads.remove(downloadId);
        if (progressView != null) {
            binding.progressContainer.removeView(progressView);
            
            // Show "no downloads" text if empty
            if (activeDownloads.isEmpty()) {
                binding.tvNoDownloads.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void clearAllProgressViews() {
        binding.progressContainer.removeAllViews();
        activeDownloads.clear();
        binding.tvNoDownloads.setVisibility(View.VISIBLE);
    }
    
    private void clearHistory() {
        totalBytesDownloaded = 0;
        downloadCount = 0;
        saveStats();
        updateStats();
        Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void updateStats() {
        String stats = String.format("Downloads: %d | Total: %s",
            downloadCount, formatBytes(totalBytesDownloaded));
        binding.tvStats.setText(stats);
        Log.d(TAG, "Updated stats: " + stats);
    }
    
    private void loadStats() {
        totalBytesDownloaded = sharedPrefs.getLong(KEY_TOTAL_BYTES, 0);
        downloadCount = sharedPrefs.getInt(KEY_DOWNLOAD_COUNT, 0);
        Log.d(TAG, "Loaded stats - Downloads: " + downloadCount + ", Bytes: " + totalBytesDownloaded);
    }
    
    private void saveStats() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(KEY_TOTAL_BYTES, totalBytesDownloaded);
        editor.putInt(KEY_DOWNLOAD_COUNT, downloadCount);
        editor.apply();
        Log.d(TAG, "Saved stats - Downloads: " + downloadCount + ", Bytes: " + totalBytesDownloaded);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        else if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        else if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        else return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void viewScheduledDownloads() {
        WorkManager workManager = WorkManager.getInstance(this);
        
        try {
            List<WorkInfo> workInfos = workManager.getWorkInfosForUniqueWork("download_work").get();
            
            if (workInfos == null || workInfos.isEmpty()) {
                showNoScheduledDownloadsDialog();
                return;
            }
            
            StringBuilder scheduledInfo = new StringBuilder();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
            
            for (WorkInfo workInfo : workInfos) {
                scheduledInfo.append("Status: ").append(workInfo.getState().name()).append("\n");
                
                if (workInfo.getState() == WorkInfo.State.ENQUEUED || 
                    workInfo.getState() == WorkInfo.State.RUNNING ||
                    workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    
                    // Extract URL from tags
                    String url = workInfo.getTags().stream()
                        .filter(tag -> tag.startsWith("url:"))
                        .map(tag -> tag.substring(4))
                        .findFirst()
                        .orElse("Unknown URL");
                    
                    // Extract interval from tags
                    String interval = workInfo.getTags().stream()
                        .filter(tag -> tag.startsWith("interval:"))
                        .map(tag -> tag.substring(9))
                        .findFirst()
                        .orElse("Unknown");
                    
                    scheduledInfo.append("URL: ").append(url).append("\n");
                    scheduledInfo.append("Interval: Every ").append(interval).append(" minutes\n");
                    
                    // Show run status
                    if (workInfo.getState() == WorkInfo.State.RUNNING) {
                        scheduledInfo.append("Status: Currently running\n");
                    } else if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
                        scheduledInfo.append("Status: Waiting for next run\n");
                    } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        scheduledInfo.append("Status: Last run succeeded\n");
                    }
                }
                
                scheduledInfo.append("\n");
            }
            
            if (scheduledInfo.length() == 0) {
                showNoScheduledDownloadsDialog();
            } else {
                showScheduledDownloadsDialog(scheduledInfo.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting scheduled downloads", e);
            Toast.makeText(this, "Error retrieving scheduled downloads", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showScheduledDownloadsDialog(String info) {
        new AlertDialog.Builder(this)
            .setTitle("Scheduled Downloads")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancel All", (dialog, which) -> {
                WorkManager.getInstance(this).cancelUniqueWork("download_work");
                Toast.makeText(this, "All scheduled downloads cancelled", Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void showNoScheduledDownloadsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Scheduled Downloads")
            .setMessage("No scheduled downloads found")
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showScheduledDownloadNotification(String url, String scheduleType) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Intent to open this activity when notification is clicked
        Intent intent = new Intent(this, DownloaderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Intent to cancel downloads
        Intent cancelIntent = new Intent(this, DownloaderActivity.class);
        cancelIntent.setAction("CANCEL_DOWNLOADS");
        PendingIntent cancelPendingIntent = PendingIntent.getActivity(this, 1, cancelIntent, 
            PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setContentTitle("Scheduled Downloads Active")
            .setContentText("Downloading " + scheduleType.toLowerCase() + ": " + url)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("URL: " + url + "\nSchedule: " + scheduleType))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent);
        
        notificationManager.notify(Constants.NOTIFICATION_ID_DOWNLOAD, builder.build());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("CANCEL_DOWNLOADS".equals(intent.getAction())) {
            stopAllDownloads();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressReceiver != null) {
            unregisterReceiver(progressReceiver);
        }
    }
    
    private class DownloadProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received broadcast: " + intent.getAction());
            
            long downloadId = intent.getLongExtra("downloadId", -1);
            String url = intent.getStringExtra("url");
            
            if (DownloadService.ACTION_DOWNLOAD_PROGRESS.equals(intent.getAction())) {
                int progress = intent.getIntExtra("progress", 0);
                Log.d(TAG, "Progress update received for download " + downloadId + ": " + progress + "%");
                
                // Ensure UI updates happen on main thread
                runOnUiThread(() -> {
                    // Add progress view if not exists
                    if (!activeDownloads.containsKey(downloadId) && url != null) {
                        addProgressView(downloadId, url);
                    }
                    updateProgressView(downloadId, progress);
                });
            } else if (DownloadService.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                String error = intent.getStringExtra("error");
                if (error != null) {
                    Log.e(TAG, "Download " + downloadId + " error: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(context, "Download failed: " + error, Toast.LENGTH_SHORT).show();
                        removeProgressView(downloadId);
                    });
                } else {
                    long bytesDownloaded = intent.getLongExtra("bytesDownloaded", 0);
                    Log.d(TAG, "Download " + downloadId + " complete, bytes: " + bytesDownloaded);
                    totalBytesDownloaded += bytesDownloaded;
                    downloadCount++;
                    saveStats(); // Save stats after update
                    
                    runOnUiThread(() -> {
                        updateStats();
                        // Remove progress view after a short delay to show 100%
                        binding.progressContainer.postDelayed(() -> removeProgressView(downloadId), 1000);
                    });
                }
            }
        }
    }
}