package com.mattintech.androidtestapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.mattintech.androidtestapp.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY = 2000; // 2 seconds
    private ActivitySplashBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Check if this is first launch
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean introCompleted = prefs.getBoolean("intro_completed", false);
        
        // Navigate to appropriate activity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (introCompleted) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, IntroActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}