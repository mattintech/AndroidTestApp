package com.mattintech.androidtestapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.mattintech.androidtestapp.databinding.ActivityBatteryDrainBinding;
import com.mattintech.androidtestapp.services.BatteryDrainService;
import java.util.ArrayList;
import java.util.List;
import android.app.AlertDialog;
import android.app.ActivityManager;
import android.content.SharedPreferences;

public class BatteryDrainActivity extends AppCompatActivity {
    private static final String TAG = Constants.LOG_TAG + "BatteryDrain";
    private ActivityBatteryDrainBinding binding;
    private BatteryDrainService.BatteryDrainBinder binder;
    private boolean isServiceBound = false;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (BatteryDrainService.BatteryDrainBinder) service;
            isServiceBound = true;
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBatteryDrainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup edge-to-edge
        setupWindowInsets();
        
        setupUI();
        setupPresets();
        
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
        // Setup duration spinner
        List<String> durations = new ArrayList<>();
        durations.add("1 minute");
        durations.add("5 minutes");
        durations.add("10 minutes");
        durations.add("30 minutes");
        durations.add("1 hour");
        durations.add("Unlimited");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_1, durations);
        ((AutoCompleteTextView)binding.spinnerDuration).setAdapter(adapter);
        ((AutoCompleteTextView)binding.spinnerDuration).setText(durations.get(0), false);
        
        // Start button
        binding.btnStart.setOnClickListener(v -> startBatteryDrain());
        
        // Stop button
        binding.btnStop.setOnClickListener(v -> stopBatteryDrain());
        binding.btnStop.setEnabled(false);
        
        // View active test button
        binding.btnViewActiveTest.setOnClickListener(v -> viewActiveTest());
    }
    
    private void setupPresets() {
        binding.btnPresetMax.setOnCheckedChangeListener((chip, isChecked) -> {
            if (isChecked) {
            // Select all options
            binding.cbCpu.setChecked(true);
            binding.cbScreen.setChecked(true);
            binding.cbFlashlight.setChecked(true);
            binding.cbGps.setChecked(true);
            binding.cbBluetooth.setChecked(true);
            binding.cbWifi.setChecked(true);
            binding.cbNetwork.setChecked(true);
            binding.cbSensors.setChecked(true);
            binding.cbCamera.setChecked(true);
            binding.cbAudio.setChecked(true);
            binding.cbVibration.setChecked(true);
            }
        });
        
        binding.btnPresetCpuGpu.setOnCheckedChangeListener((chip, isChecked) -> {
            if (isChecked) {
            clearAllOptions();
            binding.cbCpu.setChecked(true);
            binding.cbScreen.setChecked(true);
            }
        });
        
        binding.btnPresetRadio.setOnCheckedChangeListener((chip, isChecked) -> {
            if (isChecked) {
            clearAllOptions();
            binding.cbGps.setChecked(true);
            binding.cbBluetooth.setChecked(true);
            binding.cbWifi.setChecked(true);
            }
        });
        
        binding.btnPresetSensors.setOnCheckedChangeListener((chip, isChecked) -> {
            if (isChecked) {
            clearAllOptions();
            binding.cbSensors.setChecked(true);
            binding.cbVibration.setChecked(true);
            }
        });
    }
    
    private void clearAllOptions() {
        binding.cbCpu.setChecked(false);
        binding.cbScreen.setChecked(false);
        binding.cbFlashlight.setChecked(false);
        binding.cbGps.setChecked(false);
        binding.cbBluetooth.setChecked(false);
        binding.cbWifi.setChecked(false);
        binding.cbNetwork.setChecked(false);
        binding.cbSensors.setChecked(false);
        binding.cbCamera.setChecked(false);
        binding.cbAudio.setChecked(false);
        binding.cbVibration.setChecked(false);
    }
    
    private void startBatteryDrain() {
        // Validate at least one option is selected
        if (!binding.cbCpu.isChecked() && !binding.cbScreen.isChecked() && 
            !binding.cbFlashlight.isChecked() && !binding.cbGps.isChecked() && 
            !binding.cbBluetooth.isChecked() && !binding.cbWifi.isChecked() && 
            !binding.cbNetwork.isChecked() && !binding.cbSensors.isChecked() && 
            !binding.cbCamera.isChecked() && !binding.cbAudio.isChecked() && 
            !binding.cbVibration.isChecked()) {
            Toast.makeText(this, "Please select at least one option", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get duration
        int durationMinutes = getDurationMinutes();
        
        // Log test configuration
        Log.d(TAG, "Starting battery drain test with configuration:");
        Log.d(TAG, "  Duration: " + (durationMinutes == -1 ? "Unlimited" : durationMinutes + " minutes"));
        Log.d(TAG, "  CPU: " + binding.cbCpu.isChecked());
        Log.d(TAG, "  Screen: " + binding.cbScreen.isChecked());
        Log.d(TAG, "  Flashlight: " + binding.cbFlashlight.isChecked());
        Log.d(TAG, "  GPS: " + binding.cbGps.isChecked());
        Log.d(TAG, "  Bluetooth: " + binding.cbBluetooth.isChecked());
        Log.d(TAG, "  WiFi: " + binding.cbWifi.isChecked());
        Log.d(TAG, "  Network: " + binding.cbNetwork.isChecked());
        Log.d(TAG, "  Sensors: " + binding.cbSensors.isChecked());
        Log.d(TAG, "  Camera: " + binding.cbCamera.isChecked());
        Log.d(TAG, "  Audio: " + binding.cbAudio.isChecked());
        Log.d(TAG, "  Vibration: " + binding.cbVibration.isChecked());
        
        // Create intent for service
        Intent serviceIntent = new Intent(this, BatteryDrainService.class);
        serviceIntent.putExtra("cpu", binding.cbCpu.isChecked());
        serviceIntent.putExtra("screen", binding.cbScreen.isChecked());
        serviceIntent.putExtra("flashlight", binding.cbFlashlight.isChecked());
        serviceIntent.putExtra("gps", binding.cbGps.isChecked());
        serviceIntent.putExtra("bluetooth", binding.cbBluetooth.isChecked());
        serviceIntent.putExtra("wifi", binding.cbWifi.isChecked());
        serviceIntent.putExtra("network", binding.cbNetwork.isChecked());
        serviceIntent.putExtra("sensors", binding.cbSensors.isChecked());
        serviceIntent.putExtra("camera", binding.cbCamera.isChecked());
        serviceIntent.putExtra("audio", binding.cbAudio.isChecked());
        serviceIntent.putExtra("vibration", binding.cbVibration.isChecked());
        serviceIntent.putExtra("duration", durationMinutes);
        
        // Save configuration to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("BatteryDrainPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("cpu", binding.cbCpu.isChecked());
        editor.putBoolean("screen", binding.cbScreen.isChecked());
        editor.putBoolean("flashlight", binding.cbFlashlight.isChecked());
        editor.putBoolean("gps", binding.cbGps.isChecked());
        editor.putBoolean("bluetooth", binding.cbBluetooth.isChecked());
        editor.putBoolean("wifi", binding.cbWifi.isChecked());
        editor.putBoolean("network", binding.cbNetwork.isChecked());
        editor.putBoolean("sensors", binding.cbSensors.isChecked());
        editor.putBoolean("camera", binding.cbCamera.isChecked());
        editor.putBoolean("audio", binding.cbAudio.isChecked());
        editor.putBoolean("vibration", binding.cbVibration.isChecked());
        editor.putInt("duration", durationMinutes);
        editor.putLong("startTime", System.currentTimeMillis());
        editor.apply();
        
        // Start service
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        // Update UI
        binding.btnStart.setEnabled(false);
        binding.btnStop.setEnabled(true);
        disableOptions();
        
        Toast.makeText(this, "Battery drain test started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopBatteryDrain() {
        Log.d(TAG, "Stopping battery drain test");
        
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        
        stopService(new Intent(this, BatteryDrainService.class));
        
        // Update UI
        binding.btnStart.setEnabled(true);
        binding.btnStop.setEnabled(false);
        enableOptions();
        
        Toast.makeText(this, "Battery drain test stopped", Toast.LENGTH_SHORT).show();
    }
    
    private int getDurationMinutes() {
        String selected = ((AutoCompleteTextView)binding.spinnerDuration).getText().toString();
        switch (selected) {
            case "1 minute": return 1;
            case "5 minutes": return 5;
            case "10 minutes": return 10;
            case "30 minutes": return 30;
            case "1 hour": return 60;
            default: return -1; // Unlimited
        }
    }
    
    private void disableOptions() {
        binding.cbCpu.setEnabled(false);
        binding.cbScreen.setEnabled(false);
        binding.cbFlashlight.setEnabled(false);
        binding.cbGps.setEnabled(false);
        binding.cbBluetooth.setEnabled(false);
        binding.cbWifi.setEnabled(false);
        binding.cbNetwork.setEnabled(false);
        binding.cbSensors.setEnabled(false);
        binding.cbCamera.setEnabled(false);
        binding.cbAudio.setEnabled(false);
        binding.cbVibration.setEnabled(false);
        binding.spinnerDuration.setEnabled(false);
    }
    
    private void enableOptions() {
        binding.cbCpu.setEnabled(true);
        binding.cbScreen.setEnabled(true);
        binding.cbFlashlight.setEnabled(true);
        binding.cbGps.setEnabled(true);
        binding.cbBluetooth.setEnabled(true);
        binding.cbWifi.setEnabled(true);
        binding.cbNetwork.setEnabled(true);
        binding.cbSensors.setEnabled(true);
        binding.cbCamera.setEnabled(true);
        binding.cbAudio.setEnabled(true);
        binding.cbVibration.setEnabled(true);
        binding.spinnerDuration.setEnabled(true);
    }
    
    private void viewActiveTest() {
        // Check if service is running
        if (isServiceRunning(BatteryDrainService.class)) {
            // Get test configuration from SharedPreferences (we'll store it when starting)
            SharedPreferences prefs = getSharedPreferences("BatteryDrainPrefs", Context.MODE_PRIVATE);
            
            StringBuilder activeTestInfo = new StringBuilder();
            activeTestInfo.append("Battery Drain Test Active\n\n");
            
            // Duration
            int duration = prefs.getInt("duration", -1);
            activeTestInfo.append("Duration: ").append(duration == -1 ? "Unlimited" : duration + " minutes").append("\n\n");
            
            // Active components
            activeTestInfo.append("Active Components:\n");
            if (prefs.getBoolean("cpu", false)) activeTestInfo.append("• CPU Intensive Operations\n");
            if (prefs.getBoolean("screen", false)) activeTestInfo.append("• Screen (Max Brightness)\n");
            if (prefs.getBoolean("flashlight", false)) activeTestInfo.append("• Flashlight\n");
            if (prefs.getBoolean("gps", false)) activeTestInfo.append("• GPS Location\n");
            if (prefs.getBoolean("bluetooth", false)) activeTestInfo.append("• Bluetooth Scanning\n");
            if (prefs.getBoolean("wifi", false)) activeTestInfo.append("• WiFi Scanning\n");
            if (prefs.getBoolean("network", false)) activeTestInfo.append("• Network Activity\n");
            if (prefs.getBoolean("sensors", false)) activeTestInfo.append("• Sensors\n");
            if (prefs.getBoolean("camera", false)) activeTestInfo.append("• Camera Preview\n");
            if (prefs.getBoolean("audio", false)) activeTestInfo.append("• Audio Playback\n");
            if (prefs.getBoolean("vibration", false)) activeTestInfo.append("• Vibration\n");
            
            // Start time
            long startTime = prefs.getLong("startTime", 0);
            if (startTime > 0) {
                long elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000;
                activeTestInfo.append("\nElapsed Time: ").append(elapsedMinutes).append(" minutes");
            }
            
            new AlertDialog.Builder(this)
                .setTitle("Active Battery Drain Test")
                .setMessage(activeTestInfo.toString())
                .setPositiveButton("OK", null)
                .setNegativeButton("Stop Test", (dialog, which) -> stopBatteryDrain())
                .show();
        } else {
            new AlertDialog.Builder(this)
                .setTitle("No Active Test")
                .setMessage("No battery drain test is currently running")
                .setPositiveButton("OK", null)
                .show();
        }
    }
    
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
        }
    }
}