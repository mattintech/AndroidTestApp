plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.mattintech.androidtestapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mattintech.androidtestapp"
        minSdk = 32
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation(libs.fragment)
    
    // Coroutines for background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // WorkManager for scheduled downloads
    implementation("androidx.work:work-runtime:2.9.0")
    
    // OkHttp for downloads
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // CameraX for camera operations
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}