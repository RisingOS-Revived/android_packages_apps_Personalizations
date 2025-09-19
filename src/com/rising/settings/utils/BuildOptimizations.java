/*
 * Copyright (C) 2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rising.settings.utils;

import android.os.SystemProperties;
import android.util.Log;

/**
 * Build-time and runtime optimizations for the Personalizations app.
 * This class provides utilities for enabling/disabling performance features
 * based on build configuration and system properties.
 */
public class BuildOptimizations {
    
    private static final String TAG = "PersonalizationsBuildOpt";
    
    // System properties for performance tuning
    private static final String PROP_PERFORMANCE_MODE = "persist.personalizations.performance_mode";
    private static final String PROP_DEBUG_PERFORMANCE = "persist.personalizations.debug_perf";
    private static final String PROP_CACHE_ENABLED = "persist.personalizations.cache_enabled";
    private static final String PROP_ANIMATION_OPTIMIZATION = "persist.personalizations.anim_opt";
    
    // Performance modes
    public static final int PERFORMANCE_MODE_BALANCED = 0;
    public static final int PERFORMANCE_MODE_PERFORMANCE = 1;
    public static final int PERFORMANCE_MODE_BATTERY = 2;
    
    // Cache settings
    private static final int DEFAULT_CACHE_SIZE = 50;
    private static final int PERFORMANCE_CACHE_SIZE = 100;
    private static final int BATTERY_CACHE_SIZE = 25;
    
    /**
     * Get the current performance mode
     */
    public static int getPerformanceMode() {
        return SystemProperties.getInt(PROP_PERFORMANCE_MODE, PERFORMANCE_MODE_BALANCED);
    }
    
    /**
     * Check if performance debugging is enabled
     */
    public static boolean isPerformanceDebugEnabled() {
        return SystemProperties.getBoolean(PROP_DEBUG_PERFORMANCE, false);
    }
    
    /**
     * Check if caching is enabled
     */
    public static boolean isCacheEnabled() {
        return SystemProperties.getBoolean(PROP_CACHE_ENABLED, true);
    }
    
    /**
     * Check if animation optimizations are enabled
     */
    public static boolean isAnimationOptimizationEnabled() {
        return SystemProperties.getBoolean(PROP_ANIMATION_OPTIMIZATION, true);
    }
    
    /**
     * Get optimal cache size based on performance mode
     */
    public static int getOptimalCacheSize() {
        if (!isCacheEnabled()) {
            return 0;
        }
        
        switch (getPerformanceMode()) {
            case PERFORMANCE_MODE_PERFORMANCE:
                return PERFORMANCE_CACHE_SIZE;
            case PERFORMANCE_MODE_BATTERY:
                return BATTERY_CACHE_SIZE;
            case PERFORMANCE_MODE_BALANCED:
            default:
                return DEFAULT_CACHE_SIZE;
        }
    }
    
    /**
     * Get animation frame rate target based on performance mode
     */
    public static int getTargetFrameRate() {
        switch (getPerformanceMode()) {
            case PERFORMANCE_MODE_PERFORMANCE:
                return 60; // 60 FPS for performance mode
            case PERFORMANCE_MODE_BATTERY:
                return 30; // 30 FPS for battery mode
            case PERFORMANCE_MODE_BALANCED:
            default:
                return 45; // 45 FPS for balanced mode
        }
    }
    
    /**
     * Get animation duration multiplier based on performance mode
     */
    public static float getAnimationDurationMultiplier() {
        switch (getPerformanceMode()) {
            case PERFORMANCE_MODE_PERFORMANCE:
                return 0.8f; // Faster animations
            case PERFORMANCE_MODE_BATTERY:
                return 1.2f; // Slower animations to save battery
            case PERFORMANCE_MODE_BALANCED:
            default:
                return 1.0f; // Normal speed
        }
    }
    
    /**
     * Check if background processing should be enabled
     */
    public static boolean shouldUseBackgroundProcessing() {
        int mode = getPerformanceMode();
        return mode == PERFORMANCE_MODE_PERFORMANCE || mode == PERFORMANCE_MODE_BALANCED;
    }
    
    /**
     * Get the optimal thread pool size for background tasks
     */
    public static int getOptimalThreadPoolSize() {
        int processors = Runtime.getRuntime().availableProcessors();
        
        switch (getPerformanceMode()) {
            case PERFORMANCE_MODE_PERFORMANCE:
                return Math.max(2, processors); // Use all available processors
            case PERFORMANCE_MODE_BATTERY:
                return 1; // Single thread to save battery
            case PERFORMANCE_MODE_BALANCED:
            default:
                return Math.max(1, processors / 2); // Use half the processors
        }
    }
    
    /**
     * Check if memory leak detection should be enabled
     */
    public static boolean shouldEnableMemoryLeakDetection() {
        return isPerformanceDebugEnabled();
    }
    
    /**
     * Get the optimal garbage collection hint frequency
     */
    public static long getGCHintInterval() {
        switch (getPerformanceMode()) {
            case PERFORMANCE_MODE_PERFORMANCE:
                return 30000; // 30 seconds - less frequent GC hints
            case PERFORMANCE_MODE_BATTERY:
                return 10000; // 10 seconds - more frequent to free memory
            case PERFORMANCE_MODE_BALANCED:
            default:
                return 20000; // 20 seconds - balanced approach
        }
    }
    
    /**
     * Check if resource preloading should be enabled
     */
    public static boolean shouldPreloadResources() {
        return getPerformanceMode() == PERFORMANCE_MODE_PERFORMANCE;
    }
    
    /**
     * Get the optimal bitmap cache size in bytes
     */
    public static int getBitmapCacheSize() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        
        switch (getPerformanceMode()) {
            case PERFORMANCE_MODE_PERFORMANCE:
                return (int) (maxMemory / 6); // Use 1/6 of available memory
            case PERFORMANCE_MODE_BATTERY:
                return (int) (maxMemory / 12); // Use 1/12 of available memory
            case PERFORMANCE_MODE_BALANCED:
            default:
                return (int) (maxMemory / 8); // Use 1/8 of available memory
        }
    }
    
    /**
     * Log current optimization settings
     */
    public static void logOptimizationSettings() {
        if (!isPerformanceDebugEnabled()) {
            return;
        }
        
        Log.d(TAG, "Performance Optimization Settings:");
        Log.d(TAG, "  Performance Mode: " + getPerformanceMode());
        Log.d(TAG, "  Cache Enabled: " + isCacheEnabled());
        Log.d(TAG, "  Cache Size: " + getOptimalCacheSize());
        Log.d(TAG, "  Target Frame Rate: " + getTargetFrameRate());
        Log.d(TAG, "  Animation Duration Multiplier: " + getAnimationDurationMultiplier());
        Log.d(TAG, "  Background Processing: " + shouldUseBackgroundProcessing());
        Log.d(TAG, "  Thread Pool Size: " + getOptimalThreadPoolSize());
        Log.d(TAG, "  Memory Leak Detection: " + shouldEnableMemoryLeakDetection());
        Log.d(TAG, "  GC Hint Interval: " + getGCHintInterval() + "ms");
        Log.d(TAG, "  Resource Preloading: " + shouldPreloadResources());
        Log.d(TAG, "  Bitmap Cache Size: " + (getBitmapCacheSize() / 1024 / 1024) + "MB");
    }
    
    /**
     * Apply runtime optimizations based on current settings
     */
    public static void applyRuntimeOptimizations() {
        logOptimizationSettings();
        
        // Enable performance monitoring if debug is enabled
        if (isPerformanceDebugEnabled()) {
            PerformanceMonitor.setDebugEnabled(true);
            MemoryLeakDetector.setDebugEnabled(true);
        }
        
        // Log memory status
        if (isPerformanceDebugEnabled()) {
            PerformanceMonitor.getInstance().logMemoryUsage("app_startup");
        }
    }
    
    /**
     * Get recommended ProGuard/R8 optimization level
     */
    public static String getRecommendedOptimizationLevel() {
        switch (getPerformanceMode()) {
            case PERFORMANCE_MODE_PERFORMANCE:
                return "aggressive"; // Maximum optimization
            case PERFORMANCE_MODE_BATTERY:
                return "conservative"; // Minimal optimization to preserve battery
            case PERFORMANCE_MODE_BALANCED:
            default:
                return "balanced"; // Standard optimization
        }
    }
}
