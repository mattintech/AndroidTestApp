package com.mattintech.androidtestapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.viewpager2.widget.ViewPager2;
import com.mattintech.androidtestapp.databinding.ActivityIntroBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class IntroActivity extends AppCompatActivity {
    private ActivityIntroBinding binding;
    private IntroPagerAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup edge-to-edge
        setupWindowInsets();
        
        setupViewPager();
        setupButtons();
    }
    
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
    
    private void setupViewPager() {
        adapter = new IntroPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
            (tab, position) -> {
                // Empty - we just want the dots
            }).attach();
        
        // Update button visibility based on page
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateButtonVisibility(position);
            }
        });
    }
    
    private void setupButtons() {
        binding.btnNext.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem < adapter.getItemCount() - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1);
            }
        });
        
        binding.btnSkip.setOnClickListener(v -> finishIntro());
        
        binding.btnGetStarted.setOnClickListener(v -> finishIntro());
    }
    
    private void updateButtonVisibility(int position) {
        boolean isLastPage = position == adapter.getItemCount() - 1;
        
        binding.btnNext.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
        binding.btnSkip.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
        binding.btnGetStarted.setVisibility(isLastPage ? View.VISIBLE : View.GONE);
        
        // Hide tab indicators on the last page
        binding.tabLayout.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
    }
    
    private void finishIntro() {
        // Mark intro as seen
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("intro_completed", true).apply();
        
        // Navigate to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}