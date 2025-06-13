package com.mattintech.androidtestapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.mattintech.androidtestapp.databinding.ActivityMainBinding;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private final String[] requiredPermissions = {
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.VIBRATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.RECORD_AUDIO
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup edge-to-edge
        setupWindowInsets();
        
        setupButtons();
        setVersionText();
        checkAndRequestPermissions();
        createNotificationChannels();
        checkBatteryOptimization();
    }
    
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
    
    private void setupButtons() {
        binding.btnBatteryDrain.setOnClickListener(v -> {
            Intent intent = new Intent(this, BatteryDrainActivity.class);
            startActivity(intent);
        });
        
        binding.btnBadBehavior.setOnClickListener(v -> {
            Intent intent = new Intent(this, BadBehaviorActivity.class);
            startActivity(intent);
        });
        
        binding.btnDownloader.setOnClickListener(v -> {
            Intent intent = new Intent(this, DownloaderActivity.class);
            startActivity(intent);
        });
        
        binding.btnInfo.setOnClickListener(v -> showInfoDialog());
    }
    
    private void setVersionText() {
        try {
            String version = "v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            binding.tvVersion.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            binding.tvVersion.setText("v1.0");
        }
    }
    
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // Add runtime permissions based on Android version
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        // Add Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, "Some permissions were denied. App may not function properly.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void showInfoDialog() {
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "1.0";
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Android Test App")
            .setMessage("A comprehensive testing tool for Android devices that simulates battery drain, crashes, ANRs, and network downloads to help developers test device behavior under stress conditions.")
            .setPositiveButton("Report Issue / Request Feature", (dialog, which) -> {
                String url = "https://github.com/mattintech/AndroidTestApp/issues";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            })
            .setNegativeButton("Close", null)
            .show();
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            // Battery Drain channel
            NotificationChannel batteryChannel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_BATTERY,
                "Battery Drain Test",
                NotificationManager.IMPORTANCE_HIGH
            );
            batteryChannel.setDescription("Notifications for active battery drain tests");
            batteryChannel.setShowBadge(false);
            batteryChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(batteryChannel);
            
            // Downloads channel
            NotificationChannel downloadChannel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_DOWNLOAD,
                "Downloads",
                NotificationManager.IMPORTANCE_HIGH
            );
            downloadChannel.setDescription("Notifications for scheduled downloads");
            downloadChannel.setShowBadge(false);
            downloadChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(downloadChannel);
            
            // Bad Behavior channel
            NotificationChannel badBehaviorChannel = new NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_BAD_BEHAVIOR,
                "Bad Behavior",
                NotificationManager.IMPORTANCE_HIGH
            );
            badBehaviorChannel.setDescription("Notifications for scheduled crashes/ANRs");
            badBehaviorChannel.setShowBadge(false);
            badBehaviorChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(badBehaviorChannel);
        }
    }
    
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // Show dialog explaining why battery optimization should be disabled
                new AlertDialog.Builder(this)
                    .setTitle("Disable Battery Optimization")
                    .setMessage("This app needs to run tests in the background without interruption.\n\n" +
                               "Battery optimization may stop or limit:\n" +
                               "• Long-running battery drain tests\n" +
                               "• Scheduled downloads\n" +
                               "• Background test services\n\n" +
                               "Please disable battery optimization for this app to ensure tests run properly.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        try {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivity(intent);
                        } catch (Exception e) {
                            // Fallback to battery optimization settings
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Not Now", (dialog, which) -> {
                        Toast.makeText(this, "Tests may be interrupted by battery optimization", 
                            Toast.LENGTH_LONG).show();
                    })
                    .setCancelable(false)
                    .show();
            }
        }
    }
}