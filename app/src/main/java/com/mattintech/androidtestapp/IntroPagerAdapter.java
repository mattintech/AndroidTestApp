package com.mattintech.androidtestapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class IntroPagerAdapter extends RecyclerView.Adapter<IntroPagerAdapter.IntroViewHolder> {
    private final Context context;
    
    private final IntroSlide[] slides = {
        new IntroSlide(
            R.drawable.ic_launcher_foreground,
            "Welcome to Android Test App",
            "A comprehensive testing tool designed to push your Android device to its limits"
        ),
        new IntroSlide(
            R.drawable.ic_launcher_foreground,
            "Battery Drain Testing",
            "Test battery consumption with CPU, GPS, screen brightness, sensors, and more running simultaneously"
        ),
        new IntroSlide(
            R.drawable.ic_launcher_foreground,
            "Crash & ANR Simulation",
            "Trigger various types of crashes and Application Not Responding scenarios for testing"
        ),
        new IntroSlide(
            R.drawable.ic_launcher_foreground,
            "Network Stress Testing",
            "Schedule and execute concurrent file downloads to test network performance and reliability"
        ),
        new IntroSlide(
            R.drawable.ic_launcher_foreground,
            "Ready to Test?",
            "All tests can drain battery and may cause temporary device issues. Use responsibly!"
        )
    };
    
    public IntroPagerAdapter(Context context) {
        this.context = context;
    }
    
    @NonNull
    @Override
    public IntroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_intro_slide, parent, false);
        return new IntroViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull IntroViewHolder holder, int position) {
        IntroSlide slide = slides[position];
        holder.bind(slide);
    }
    
    @Override
    public int getItemCount() {
        return slides.length;
    }
    
    static class IntroViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleView;
        private final TextView descriptionView;
        
        public IntroViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivSlideImage);
            titleView = itemView.findViewById(R.id.tvSlideTitle);
            descriptionView = itemView.findViewById(R.id.tvSlideDescription);
        }
        
        public void bind(IntroSlide slide) {
            imageView.setImageResource(slide.imageResId);
            titleView.setText(slide.title);
            descriptionView.setText(slide.description);
        }
    }
    
    static class IntroSlide {
        final int imageResId;
        final String title;
        final String description;
        
        IntroSlide(int imageResId, String title, String description) {
            this.imageResId = imageResId;
            this.title = title;
            this.description = description;
        }
    }
}