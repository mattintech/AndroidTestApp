package com.mattintech.androidtestapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import com.mattintech.androidtestapp.Constants;
import com.mattintech.androidtestapp.MainActivity;
import com.mattintech.androidtestapp.BatteryDrainActivity;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatteryDrainService extends Service implements SensorEventListener {
    private static final String TAG = Constants.LOG_TAG + "BatteryDrainSvc";
    private static final String CHANNEL_ID = Constants.NOTIFICATION_CHANNEL_BATTERY;
    private static final int NOTIFICATION_ID = Constants.NOTIFICATION_ID_BATTERY;
    
    private final IBinder binder = new BatteryDrainBinder();
    private ExecutorService executorService;
    private Handler handler;
    private HandlerThread handlerThread;
    
    // Components
    private PowerManager.WakeLock wakeLock;
    private CameraManager cameraManager;
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Vibrator vibrator;
    private AudioTrack audioTrack;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    
    // State
    private boolean isRunning = false;
    private long endTime = 0;
    private boolean cpuEnabled, screenEnabled, flashlightEnabled, gpsEnabled;
    private boolean bluetoothEnabled, wifiEnabled, networkEnabled, sensorsEnabled;
    private boolean cameraEnabled, audioEnabled, vibrationEnabled;
    
    // Threads
    private Thread cpuThread;
    private Thread networkThread;
    
    public class BatteryDrainBinder extends Binder {
        public BatteryDrainService getService() {
            return BatteryDrainService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(4);
        handlerThread = new HandlerThread("BatteryDrainHandler");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        
        // Initialize managers
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BatteryDrain:WakeLock");
        
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // Check if this is a stop action
            if ("STOP".equals(intent.getAction())) {
                Log.d(TAG, "Received stop action from notification");
                stopSelf();
                return START_NOT_STICKY;
            }
            
            cpuEnabled = intent.getBooleanExtra("cpu", false);
            screenEnabled = intent.getBooleanExtra("screen", false);
            flashlightEnabled = intent.getBooleanExtra("flashlight", false);
            gpsEnabled = intent.getBooleanExtra("gps", false);
            bluetoothEnabled = intent.getBooleanExtra("bluetooth", false);
            wifiEnabled = intent.getBooleanExtra("wifi", false);
            networkEnabled = intent.getBooleanExtra("network", false);
            sensorsEnabled = intent.getBooleanExtra("sensors", false);
            cameraEnabled = intent.getBooleanExtra("camera", false);
            audioEnabled = intent.getBooleanExtra("audio", false);
            vibrationEnabled = intent.getBooleanExtra("vibration", false);
            
            int durationMinutes = intent.getIntExtra("duration", -1);
            if (durationMinutes > 0) {
                endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000);
            } else {
                endTime = 0; // Unlimited
            }
            
            Log.d(TAG, "Battery drain service starting with features:");
            Log.d(TAG, "  CPU: " + cpuEnabled);
            Log.d(TAG, "  Screen: " + screenEnabled);
            Log.d(TAG, "  Flashlight: " + flashlightEnabled);
            Log.d(TAG, "  GPS: " + gpsEnabled);
            Log.d(TAG, "  Bluetooth: " + bluetoothEnabled);
            Log.d(TAG, "  WiFi: " + wifiEnabled);
            Log.d(TAG, "  Network: " + networkEnabled);
            Log.d(TAG, "  Sensors: " + sensorsEnabled);
            Log.d(TAG, "  Camera: " + cameraEnabled);
            Log.d(TAG, "  Audio: " + audioEnabled);
            Log.d(TAG, "  Vibration: " + vibrationEnabled);
            Log.d(TAG, "  Duration: " + (durationMinutes == -1 ? "Unlimited" : durationMinutes + " minutes"));
            
            startForeground(NOTIFICATION_ID, createNotification());
            startDraining();
        }
        
        return START_STICKY;
    }
    
    private Notification createNotification() {
        // Create proper back stack for notification
        Intent mainIntent = new Intent(this, MainActivity.class);
        Intent batteryIntent = new Intent(this, BatteryDrainActivity.class);
        
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(mainIntent);
        stackBuilder.addNextIntent(batteryIntent);
        
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, 
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Build active components list
        StringBuilder activeComponents = new StringBuilder();
        if (cpuEnabled) activeComponents.append("CPU, ");
        if (screenEnabled) activeComponents.append("Screen, ");
        if (flashlightEnabled) activeComponents.append("Flashlight, ");
        if (gpsEnabled) activeComponents.append("GPS, ");
        if (bluetoothEnabled) activeComponents.append("Bluetooth, ");
        if (wifiEnabled) activeComponents.append("WiFi, ");
        if (networkEnabled) activeComponents.append("Network, ");
        if (sensorsEnabled) activeComponents.append("Sensors, ");
        if (cameraEnabled) activeComponents.append("Camera, ");
        if (audioEnabled) activeComponents.append("Audio, ");
        if (vibrationEnabled) activeComponents.append("Vibration, ");
        
        // Remove trailing comma and space
        if (activeComponents.length() > 2) {
            activeComponents.setLength(activeComponents.length() - 2);
        }
        
        String durationText;
        if (endTime > 0) {
            long remainingMillis = endTime - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                durationText = "Duration: Completing...";
            } else {
                long minutes = remainingMillis / 60000;
                long hours = minutes / 60;
                minutes = minutes % 60;
                if (hours > 0) {
                    durationText = String.format("Duration: %dh %dm remaining", hours, minutes);
                } else {
                    durationText = String.format("Duration: %d min remaining", minutes);
                }
            }
        } else {
            durationText = "Duration: Unlimited";
        }
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Drain Test Active")
            .setContentText(activeComponents.toString())
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(activeComponents.toString() + "\n" + durationText))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Test", 
                PendingIntent.getService(this, 0, 
                    new Intent(this, BatteryDrainService.class).setAction("STOP"), 
                    PendingIntent.FLAG_IMMUTABLE))
            .build();
    }
    
    private void startDraining() {
        isRunning = true;
        wakeLock.acquire();
        Log.d(TAG, "Starting battery drain features");
        
        if (cpuEnabled) {
            Log.d(TAG, "Starting CPU drain");
            startCpuDrain();
        }
        if (screenEnabled) {
            Log.d(TAG, "Starting screen drain");
            startScreenDrain();
        }
        if (flashlightEnabled) {
            Log.d(TAG, "Starting flashlight drain");
            startFlashlightDrain();
        }
        if (gpsEnabled) {
            Log.d(TAG, "Starting GPS drain");
            startGpsDrain();
        }
        if (bluetoothEnabled) {
            Log.d(TAG, "Starting Bluetooth drain");
            startBluetoothDrain();
        }
        if (wifiEnabled) {
            Log.d(TAG, "Starting WiFi drain");
            startWifiDrain();
        }
        if (networkEnabled) {
            Log.d(TAG, "Starting network drain");
            startNetworkDrain();
        }
        if (sensorsEnabled) {
            Log.d(TAG, "Starting sensors drain");
            startSensorsDrain();
        }
        if (cameraEnabled) {
            Log.d(TAG, "Starting camera drain");
            startCameraDrain();
        }
        if (audioEnabled) {
            Log.d(TAG, "Starting audio drain");
            startAudioDrain();
        }
        if (vibrationEnabled) {
            Log.d(TAG, "Starting vibration drain");
            startVibrationDrain();
        }
        
        // Schedule check for duration and notification updates
        if (endTime > 0) {
            handler.postDelayed(this::checkDuration, 10000); // Check every 10 seconds
        }
    }
    
    private void checkDuration() {
        if (System.currentTimeMillis() >= endTime) {
            Log.d(TAG, "Battery drain duration reached, stopping service");
            stopSelf();
        } else {
            // Update notification every 10 seconds
            updateNotification();
            handler.postDelayed(this::checkDuration, 10000); // Check every 10 seconds
        }
    }
    
    private void updateNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }
    
    private void startCpuDrain() {
        cpuThread = new Thread(() -> {
            while (isRunning) {
                // Intensive mathematical calculations
                double result = 0;
                for (int i = 0; i < 1000000; i++) {
                    result += Math.sqrt(i) * Math.sin(i) * Math.cos(i);
                }
            }
        });
        cpuThread.start();
    }
    
    private void startScreenDrain() {
        // Note: Screen brightness control requires WRITE_SETTINGS permission
        // which requires user to manually grant in settings
        // For now, we'll just keep the wake lock
        wakeLock.acquire();
    }
    
    private void startFlashlightDrain() {
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    private void startGpsDrain() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0, // minimum time interval
                0, // minimum distance
                location -> {}, // Empty listener
                handler.getLooper()
            );
        }
    }
    
    private void startBluetoothDrain() {
        if (bluetoothLeScanner != null && 
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) 
            == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner.startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    // Do nothing with results
                }
            });
        }
    }
    
    private void startWifiDrain() {
        if (wifiManager != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isRunning) {
                        wifiManager.startScan();
                        handler.postDelayed(this, 1000);
                    }
                }
            });
        }
    }
    
    private void startNetworkDrain() {
        networkThread = new Thread(() -> {
            while (isRunning) {
                try {
                    URL url = new URL("https://www.google.com");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.getResponseCode();
                    connection.disconnect();
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        networkThread.start();
    }
    
    private void startSensorsDrain() {
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
    
    private void startCameraDrain() {
        // Camera preview would require a surface view
        // For battery drain, just opening camera is enough
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length > 0) {
                // Camera opening would be done here with proper surface
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    
    private void startAudioDrain() {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        
        audioTrack = new AudioTrack.Builder()
            .setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build())
            .setAudioFormat(new AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build())
            .setBufferSizeInBytes(bufferSize)
            .build();
        
        audioTrack.play();
        
        // Generate silent audio
        short[] buffer = new short[bufferSize];
        new Thread(() -> {
            while (isRunning) {
                audioTrack.write(buffer, 0, buffer.length);
            }
        }).start();
    }
    
    private void startVibrationDrain() {
        if (vibrator != null && vibrator.hasVibrator()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isRunning) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, 
                                VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(500);
                        }
                        handler.postDelayed(this, 600);
                    }
                }
            });
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Do nothing - just drain battery
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Battery drain service stopping");
        isRunning = false;
        
        // Stop all draining activities
        if (cpuThread != null) cpuThread.interrupt();
        if (networkThread != null) networkThread.interrupt();
        
        if (flashlightEnabled) {
            try {
                String cameraId = cameraManager.getCameraIdList()[0];
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        
        if (gpsEnabled) {
            locationManager.removeUpdates(location -> {});
        }
        
        if (bluetoothLeScanner != null && bluetoothEnabled) {
            bluetoothLeScanner.stopScan(new ScanCallback() {});
        }
        
        if (sensorsEnabled) {
            sensorManager.unregisterListener(this);
        }
        
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
        
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        handler.removeCallbacksAndMessages(null);
        handlerThread.quit();
        executorService.shutdown();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}