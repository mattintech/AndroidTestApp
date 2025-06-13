package com.mattintech.androidtestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.mattintech.androidtestapp.databinding.ActivityBadBehaviorBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BadBehaviorActivity extends AppCompatActivity {
    private static final String TAG = Constants.LOG_TAG + "BadBehavior";
    private ActivityBadBehaviorBinding binding;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Random random = new Random();
    private boolean isRandomModeActive = false;
    private Runnable randomCrashRunnable;
    private boolean isServiceRunning = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up crash handler to restart app
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // Check if we're in random mode
                SharedPreferences prefs = getSharedPreferences("BadBehaviorPrefs", Context.MODE_PRIVATE);
                boolean isRandomActive = prefs.getBoolean("isRandomActive", false);
                
                // Restart the app
                Intent intent;
                if (isRandomActive) {
                    // Go directly to BadBehaviorActivity to resume random mode
                    intent = new Intent(getApplicationContext(), BadBehaviorActivity.class);
                    intent.putExtra("resumeRandomMode", true);
                } else {
                    // Normal restart to MainActivity
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        
        binding = ActivityBadBehaviorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup edge-to-edge
        setupWindowInsets();
        
        setupUI();
        
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
        
        // Check if we need to resume random mode after a crash
        checkAndResumeRandomMode();
    }
    
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
    
    private void setupUI() {
        // Setup crash type spinner
        List<String> crashTypes = new ArrayList<>();
        crashTypes.add("Division by Zero");
        crashTypes.add("Null Pointer Exception");
        crashTypes.add("Out of Memory");
        crashTypes.add("Stack Overflow");
        crashTypes.add("Array Index Out of Bounds");
        
        ArrayAdapter<String> crashAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, crashTypes);
        crashAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCrashType.setAdapter(crashAdapter);
        
        // Setup ANR type spinner
        List<String> anrTypes = new ArrayList<>();
        anrTypes.add("UI Thread Blocking");
        anrTypes.add("Broadcast Receiver Timeout");
        anrTypes.add("Service Timeout");
        
        ArrayAdapter<String> anrAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, anrTypes);
        anrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerAnrType.setAdapter(anrAdapter);
        
        // Setup frequency spinner
        List<String> frequencies = new ArrayList<>();
        frequencies.add("Manual");
        frequencies.add("Every 10 seconds");
        frequencies.add("Every 30 seconds");
        frequencies.add("Every 1 minute");
        frequencies.add("Every 5 minutes");
        frequencies.add("Random (10-60 seconds)");
        
        ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, frequencies);
        frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFrequency.setAdapter(frequencyAdapter);
        
        // Button listeners
        binding.btnTriggerCrash.setOnClickListener(v -> triggerCrash());
        binding.btnTriggerAnr.setOnClickListener(v -> triggerANR());
        binding.btnStartRandom.setOnClickListener(v -> startRandomMode());
        binding.btnStopRandom.setOnClickListener(v -> stopRandomMode());
        binding.btnViewScheduled.setOnClickListener(v -> viewScheduledCrashes());
        
        // Initially disable stop button
        binding.btnStopRandom.setEnabled(false);
    }
    
    private void triggerCrash() {
        String crashType = binding.spinnerCrashType.getSelectedItem().toString();
        Log.d(TAG, "Triggering crash: " + crashType);
        
        switch (crashType) {
            case "Division by Zero":
                int a = 10;
                int b = 0;
                int result = a / b; // This will crash
                break;
                
            case "Null Pointer Exception":
                String nullString = null;
                nullString.length(); // This will crash
                break;
                
            case "Out of Memory":
                List<byte[]> list = new ArrayList<>();
                while (true) {
                    list.add(new byte[1024 * 1024]); // Allocate 1MB until OOM
                }
                
            case "Stack Overflow":
                recursiveMethod(); // This will crash
                break;
                
            case "Array Index Out of Bounds":
                int[] array = new int[5];
                int value = array[10]; // This will crash
                break;
        }
    }
    
    private void recursiveMethod() {
        recursiveMethod(); // Infinite recursion causes stack overflow
    }
    
    private void triggerANR() {
        String anrType = binding.spinnerAnrType.getSelectedItem().toString();
        Log.d(TAG, "Triggering ANR: " + anrType);
        
        switch (anrType) {
            case "UI Thread Blocking":
                // Show warning dialog
                new AlertDialog.Builder(this)
                    .setTitle("UI Thread Blocking ANR")
                    .setMessage("This will block the UI thread for 10 seconds, causing an ANR.\n\n" +
                               "To clear the ANR:\n" +
                               "1. Wait for the ANR dialog (appears after ~5 seconds)\n" +
                               "2. Choose 'Wait' to continue or 'Close app'\n" +
                               "3. The app will respond again after 10 seconds total\n\n" +
                               "During this time, the app will be completely frozen.")
                    .setPositiveButton("Block UI Thread", (dialog, which) -> {
                        Log.d(TAG, "User confirmed UI Thread Blocking ANR");
                        Toast.makeText(this, "Blocking UI thread for 10 seconds...", Toast.LENGTH_SHORT).show();
                        handler.postDelayed(() -> {
                            try {
                                Log.d(TAG, "Starting UI thread block");
                                Thread.sleep(10000); // This will cause ANR
                                Log.d(TAG, "UI thread block completed");
                            } catch (InterruptedException e) {
                                Log.e(TAG, "UI thread block interrupted", e);
                                e.printStackTrace();
                            }
                        }, 100); // Small delay to show toast
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                break;
                
            case "Broadcast Receiver Timeout":
                // Show warning dialog
                new AlertDialog.Builder(this)
                    .setTitle("Broadcast Receiver Timeout ANR")
                    .setMessage("This will trigger a broadcast receiver that blocks for 15 seconds, causing an ANR.\n\n" +
                               "To clear the ANR:\n" +
                               "1. Wait for the ANR dialog (appears after ~10 seconds)\n" +
                               "2. Choose 'Wait' or 'Close app'\n" +
                               "3. The receiver will complete after 15 seconds\n\n" +
                               "Note: The app may remain partially responsive during this ANR.")
                    .setPositiveButton("Trigger Broadcast ANR", (dialog, which) -> {
                        Log.d(TAG, "User confirmed Broadcast Receiver ANR");
                        BroadcastReceiver slowReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                try {
                                    Log.d(TAG, "Starting broadcast receiver block");
                                    Thread.sleep(15000); // This will cause ANR
                                    Log.d(TAG, "Broadcast receiver block completed");
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "Broadcast receiver block interrupted", e);
                                    e.printStackTrace();
                                }
                            }
                        };
                        registerReceiver(slowReceiver, new IntentFilter("com.mattintech.SLOW_ACTION"));
                        sendBroadcast(new Intent("com.mattintech.SLOW_ACTION"));
                        Toast.makeText(this, "Broadcast sent - ANR will occur soon", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                break;
                
            case "Service Timeout":
                if (!isServiceRunning) {
                    // Show warning dialog
                    new AlertDialog.Builder(this)
                        .setTitle("Service Timeout ANR")
                        .setMessage("This will start a service that blocks for 20 seconds, causing an ANR.\n\n" +
                                   "To clear the ANR:\n" +
                                   "1. Wait for the ANR dialog to appear\n" +
                                   "2. Choose 'Wait' or 'Close app'\n" +
                                   "3. Force stop the app from Settings if needed\n\n" +
                                   "The service will automatically stop after 20 seconds.")
                        .setPositiveButton("Start Service", (dialog, which) -> {
                            Log.d(TAG, "User confirmed Service Timeout ANR");
                            isServiceRunning = true;
                            Intent serviceIntent = new Intent(this, SlowService.class);
                            startService(serviceIntent);
                            Log.d(TAG, "Starting slow service");
                            Toast.makeText(this, "Service started - ANR will occur in ~5 seconds", Toast.LENGTH_LONG).show();
                            
                            // Schedule service status reset
                            handler.postDelayed(() -> {
                                isServiceRunning = false;
                                Log.d(TAG, "Service timeout cleared");
                                Toast.makeText(this, "Service timeout cleared", Toast.LENGTH_SHORT).show();
                            }, 20000);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                } else {
                    Toast.makeText(this, "Service is already running. Wait for it to complete.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
    
    private void startRandomMode() {
        String frequency = binding.spinnerFrequency.getSelectedItem().toString();
        if (frequency.equals("Manual")) {
            Toast.makeText(this, "Please select a time interval", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show warning dialog before starting
        new AlertDialog.Builder(this)
            .setTitle("⚠️ Warning: Random Crash Mode")
            .setMessage("IMPORTANT: The app must stay in the foreground!\n" +
                       "Crashes only occur when this screen is visible.\n\n" +
                       "Starting random crash mode will:\n" +
                       "• INTERRUPT all other running tests\n" +
                       "• STOP battery drain tests\n" +
                       "• FAIL scheduled downloads\n" +
                       "• Crash the app " + frequency.toLowerCase() + "\n\n" +
                       "The app will auto-restart and return to this screen.\n" +
                       "Keep the app open for continuous testing.\n" +
                       "This mode will automatically stop after 24 hours.\n\n" +
                       "Are you sure you want to start random crash mode?")
            .setPositiveButton("Start", (dialog, which) -> {
                Log.d(TAG, "Starting random mode with frequency: " + frequency);
                
                // Save random mode configuration
                SharedPreferences prefs = getSharedPreferences("BadBehaviorPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isRandomActive", true);
                editor.putString("frequency", frequency);
                editor.putLong("startTime", System.currentTimeMillis());
                editor.apply();
                
                isRandomModeActive = true;
                binding.btnStartRandom.setEnabled(false);
                binding.btnStopRandom.setEnabled(true);
                binding.btnTriggerCrash.setEnabled(false);
                binding.btnTriggerAnr.setEnabled(false);
                
                long delayMillis = getDelayMillis(frequency);
                scheduleRandomBehavior(delayMillis);
                
                // Show notification for random mode
                showRandomModeNotification(frequency);
                
                Toast.makeText(this, "Random mode started - keep app in foreground!", Toast.LENGTH_LONG).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void scheduleRandomBehavior(long delayMillis) {
        if (!isRandomModeActive) return;
        
        randomCrashRunnable = () -> {
            if (!isRandomModeActive) return;
            
            // Only trigger crashes in random mode (no ANRs to avoid dialogs)
            int crashIndex = random.nextInt(5);
            binding.spinnerCrashType.setSelection(crashIndex);
            Log.d(TAG, "Random mode triggering crash type: " + binding.spinnerCrashType.getSelectedItem());
            triggerCrash();
            
            // Note: The next crash will be scheduled after the app restarts
            // via checkAndResumeRandomMode()
        };
        
        handler.postDelayed(randomCrashRunnable, delayMillis);
    }
    
    private long getDelayMillis(String frequency) {
        switch (frequency) {
            case "Every 10 seconds":
                return 10000;
            case "Every 30 seconds":
                return 30000;
            case "Every 1 minute":
                return 60000;
            case "Every 5 minutes":
                return 300000;
            case "Random (10-60 seconds)":
                return 10000 + random.nextInt(50000);
            default:
                return 10000;
        }
    }
    
    private void stopRandomMode() {
        Log.d(TAG, "Stopping random mode");
        
        isRandomModeActive = false;
        handler.removeCallbacks(randomCrashRunnable);
        
        // Clear random mode from preferences
        SharedPreferences prefs = getSharedPreferences("BadBehaviorPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRandomActive", false);
        editor.apply();
        
        binding.btnStartRandom.setEnabled(true);
        binding.btnStopRandom.setEnabled(false);
        binding.btnTriggerCrash.setEnabled(true);
        binding.btnTriggerAnr.setEnabled(true);
        
        // Cancel notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.NOTIFICATION_ID_BAD_BEHAVIOR);
        
        Toast.makeText(this, "Random mode stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void viewScheduledCrashes() {
        SharedPreferences prefs = getSharedPreferences("BadBehaviorPrefs", Context.MODE_PRIVATE);
        boolean isRandomActive = prefs.getBoolean("isRandomActive", false);
        
        if (isRandomActive && isRandomModeActive) {
            String frequency = prefs.getString("frequency", "Unknown");
            long startTime = prefs.getLong("startTime", 0);
            
            StringBuilder info = new StringBuilder();
            info.append("Random Crash Mode Active\n\n");
            info.append("Frequency: ").append(frequency).append("\n");
            
            if (startTime > 0) {
                long elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000;
                info.append("Running for: ").append(elapsedMinutes).append(" minutes\n");
            }
            
            info.append("\nThis mode randomly triggers:\n");
            info.append("• App crashes only (no ANRs)\n");
            info.append("• 5 different crash types\n");
            info.append("\nThe app will auto-restart after crashes.\n");
            info.append("Mode stops automatically after 24 hours.");
            
            new AlertDialog.Builder(this)
                .setTitle("Scheduled Crashes")
                .setMessage(info.toString())
                .setPositiveButton("OK", null)
                .setNegativeButton("Stop Random Mode", (dialog, which) -> stopRandomMode())
                .show();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("No Scheduled Crashes")
                .setMessage("Random crash mode is not active.\n\nUse 'Start Random' to schedule automatic crashes/ANRs.")
                .setPositiveButton("OK", null)
                .show();
        }
    }
    
    private void showRandomModeNotification(String frequency) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Intent to open this activity when notification is clicked
        Intent intent = new Intent(this, BadBehaviorActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Intent to stop random mode
        Intent stopIntent = new Intent(this, BadBehaviorActivity.class);
        stopIntent.setAction("STOP_RANDOM_MODE");
        PendingIntent stopPendingIntent = PendingIntent.getActivity(this, 1, stopIntent, 
            PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_BAD_BEHAVIOR)
            .setContentTitle("⚠️ Random Crash Mode Active")
            .setContentText("Keep app in foreground! Crashes " + frequency.toLowerCase())
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("IMPORTANT: App must stay in foreground for crashes!\n" +
                    "All other tests are being interrupted!\n" +
                    "Frequency: " + frequency + "\n" +
                    "Auto-restarts after crashes\n" +
                    "Stops after 24 hours"))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);
        
        notificationManager.notify(Constants.NOTIFICATION_ID_BAD_BEHAVIOR, builder.build());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("STOP_RANDOM_MODE".equals(intent.getAction())) {
            stopRandomMode();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRandomMode();
        // Stop service if running
        if (isServiceRunning) {
            stopService(new Intent(this, SlowService.class));
            isServiceRunning = false;
        }
    }
    
    private void stopSlowService() {
        if (isServiceRunning) {
            stopService(new Intent(this, SlowService.class));
            isServiceRunning = false;
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkAndResumeRandomMode() {
        SharedPreferences prefs = getSharedPreferences("BadBehaviorPrefs", Context.MODE_PRIVATE);
        boolean wasRandomActive = prefs.getBoolean("isRandomActive", false);
        
        if (wasRandomActive) {
            String frequency = prefs.getString("frequency", "");
            long startTime = prefs.getLong("startTime", 0);
            
            // Check if we should still be in random mode (e.g., within 24 hours)
            long elapsedHours = (System.currentTimeMillis() - startTime) / (1000 * 60 * 60);
            if (elapsedHours < 24 && !frequency.isEmpty()) {
                Log.d(TAG, "Resuming random mode after restart: " + frequency);
                
                // Update UI to reflect active state
                isRandomModeActive = true;
                binding.btnStartRandom.setEnabled(false);
                binding.btnStopRandom.setEnabled(true);
                binding.btnTriggerCrash.setEnabled(false);
                binding.btnTriggerAnr.setEnabled(false);
                
                // Set the spinner to the saved frequency
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) binding.spinnerFrequency.getAdapter();
                int position = adapter.getPosition(frequency);
                if (position >= 0) {
                    binding.spinnerFrequency.setSelection(position);
                }
                
                // Resume scheduling with a shorter initial delay after crash
                long delayMillis = Math.min(10000, getDelayMillis(frequency)); // 10 seconds or normal delay, whichever is shorter
                scheduleRandomBehavior(delayMillis);
                
                // Show notification again
                showRandomModeNotification(frequency);
                
                Toast.makeText(this, "Random mode resumed", Toast.LENGTH_SHORT).show();
            } else {
                // Clear stale random mode
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isRandomActive", false);
                editor.apply();
            }
        }
    }
    
    // Inner class for slow service
    public static class SlowService extends android.app.Service {
        private static final String TAG = Constants.LOG_TAG + "SlowService";
        
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "SlowService started - blocking for 20 seconds");
            // Block the service
            try {
                Thread.sleep(20000); // This will cause ANR
                Log.d(TAG, "SlowService block completed");
            } catch (InterruptedException e) {
                Log.e(TAG, "SlowService interrupted", e);
                e.printStackTrace();
            }
            return START_NOT_STICKY;
        }
        
        @Override
        public android.os.IBinder onBind(Intent intent) {
            return null;
        }
    }
}