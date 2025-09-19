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

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime performance optimizer that dynamically adjusts performance parameters
 * based on device capabilities and current system state.
 */
public class RuntimeOptimizer {
    
    private static final String TAG = "RuntimeOptimizer";
    private static final boolean DEBUG = false;
    
    // Singleton instance
    private static RuntimeOptimizer sInstance;
    
    // Performance monitoring
    private final AtomicBoolean mOptimizationActive = new AtomicBoolean(false);
    private final AtomicLong mLastOptimizationTime = new AtomicLong(0);
    
    // Device capabilities
    private int mDeviceMemoryClass;
    private int mProcessorCount;
    private boolean mIsLowRamDevice;
    
    // Optimization parameters
    private int mOptimalCacheSize;
    private int mOptimalThreadPoolSize;
    private long mOptimalGCInterval;
    
    private RuntimeOptimizer() {
        // Private constructor for singleton
    }
    
    public static RuntimeOptimizer getInstance() {
        if (sInstance == null) {
            synchronized (RuntimeOptimizer.class) {
                if (sInstance == null) {
                    sInstance = new RuntimeOptimizer();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Initialize the optimizer with device capabilities
     */
    public void initialize(Context context) {
        if (context == null) return;
        
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            mDeviceMemoryClass = am.getMemoryClass();
            mIsLowRamDevice = am.isLowRamDevice();
        }
        
        mProcessorCount = Runtime.getRuntime().availableProcessors();
        
        calculateOptimalParameters();
        
        if (DEBUG) {
            Log.d(TAG, "RuntimeOptimizer initialized:");
            Log.d(TAG, "  Memory Class: " + mDeviceMemoryClass + "MB");
            Log.d(TAG, "  Processor Count: " + mProcessorCount);
            Log.d(TAG, "  Low RAM Device: " + mIsLowRamDevice);
            Log.d(TAG, "  Optimal Cache Size: " + mOptimalCacheSize);
            Log.d(TAG, "  Optimal Thread Pool Size: " + mOptimalThreadPoolSize);
        }
    }
    
    /**
     * Calculate optimal parameters based on device capabilities
     */
    private void calculateOptimalParameters() {
        // Calculate optimal cache size based on available memory
        if (mIsLowRamDevice) {
            mOptimalCacheSize = Math.max(10, mDeviceMemoryClass / 8); // Conservative for low RAM
        } else {
            mOptimalCacheSize = Math.max(25, mDeviceMemoryClass / 4); // More aggressive for high RAM
        }
        
        // Calculate optimal thread pool size
        if (mIsLowRamDevice) {
            mOptimalThreadPoolSize = Math.max(1, mProcessorCount / 2);
        } else {
            mOptimalThreadPoolSize = Math.max(2, mProcessorCount);
        }
        
        // Calculate optimal GC interval
        if (mIsLowRamDevice) {
            mOptimalGCInterval = 10000; // 10 seconds for low RAM devices
        } else {
            mOptimalGCInterval = 30000; // 30 seconds for normal devices
        }
    }
    
    /**
     * Get optimal cache size for the current device
     */
    public int getOptimalCacheSize() {
        return mOptimalCacheSize;
    }
    
    /**
     * Get optimal thread pool size for the current device
     */
    public int getOptimalThreadPoolSize() {
        return mOptimalThreadPoolSize;
    }
    
    /**
     * Get optimal garbage collection interval
     */
    public long getOptimalGCInterval() {
        return mOptimalGCInterval;
    }
    
    /**
     * Check if device is low on memory
     */
    public boolean isLowMemory(Context context) {
        if (context == null) return false;
        
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(memInfo);
            return memInfo.lowMemory;
        }
        return false;
    }
    
    /**
     * Optimize performance based on current system state
     */
    public void optimizePerformance(Context context) {
        if (mOptimizationActive.get()) {
            return; // Already optimizing
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastOptimizationTime.get() < 5000) {
            return; // Don't optimize too frequently
        }
        
        mOptimizationActive.set(true);
        mLastOptimizationTime.set(currentTime);
        
        try {
            // Check memory pressure and adjust accordingly
            if (isLowMemory(context)) {
                performLowMemoryOptimization();
            } else {
                performNormalOptimization();
            }
        } finally {
            mOptimizationActive.set(false);
        }
    }
    
    /**
     * Perform optimizations for low memory conditions
     */
    private void performLowMemoryOptimization() {
        if (DEBUG) {
            Log.d(TAG, "Performing low memory optimization");
        }
        
        // Suggest garbage collection
        Runtime.getRuntime().gc();
        
        // Clear performance monitor caches if available
        PerformanceMonitor.getInstance().clearData();
        
        // Force memory leak detector to check
        MemoryLeakDetector.getInstance().forceLeakCheck();
    }
    
    /**
     * Perform normal optimizations
     */
    private void performNormalOptimization() {
        if (DEBUG) {
            Log.d(TAG, "Performing normal optimization");
        }
        
        // Log current memory usage
        PerformanceMonitor.getInstance().logMemoryUsage("runtime_optimization");
    }
    
    /**
     * Get recommended animation duration multiplier based on device performance
     */
    public float getAnimationDurationMultiplier() {
        if (mIsLowRamDevice) {
            return 1.2f; // Slower animations for low-end devices
        } else if (mProcessorCount >= 8) {
            return 0.8f; // Faster animations for high-end devices
        } else {
            return 1.0f; // Normal speed for mid-range devices
        }
    }
    
    /**
     * Check if background processing should be limited
     */
    public boolean shouldLimitBackgroundProcessing() {
        return mIsLowRamDevice || mProcessorCount < 4;
    }
    
    /**
     * Get recommended frame rate target based on device capabilities
     */
    public int getRecommendedFrameRate() {
        if (mIsLowRamDevice) {
            return 30; // 30 FPS for low-end devices
        } else if (mProcessorCount >= 8 && mDeviceMemoryClass >= 512) {
            return 60; // 60 FPS for high-end devices
        } else {
            return 45; // 45 FPS for mid-range devices
        }
    }
    
    /**
     * Schedule periodic optimization
     */
    public void schedulePeriodicOptimization(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        
        Runnable optimizationRunnable = new Runnable() {
            @Override
            public void run() {
                optimizePerformance(context);
                // Schedule next optimization
                handler.postDelayed(this, getOptimalGCInterval());
            }
        };
        
        // Start periodic optimization
        handler.postDelayed(optimizationRunnable, getOptimalGCInterval());
    }
    
    /**
     * Get device performance tier
     */
    public int getDevicePerformanceTier() {
        if (mIsLowRamDevice || mProcessorCount < 4 || mDeviceMemoryClass < 256) {
            return 1; // Low-end
        } else if (mProcessorCount >= 8 && mDeviceMemoryClass >= 512) {
            return 3; // High-end
        } else {
            return 2; // Mid-range
        }
    }
    
    /**
     * Apply device-specific optimizations
     */
    public void applyDeviceOptimizations() {
        int tier = getDevicePerformanceTier();
        
        if (DEBUG) {
            Log.d(TAG, "Applying optimizations for performance tier: " + tier);
        }
        
        // Apply optimizations based on device tier
        switch (tier) {
            case 1: // Low-end
                // Apply low-end optimizations - disable debug features
                if (BuildOptimizations.shouldEnableMemoryLeakDetection()) {
                    MemoryLeakDetector.setDebugEnabled(false);
                }
                break;
            case 2: // Mid-range
                // Default settings
                break;
            case 3: // High-end
                // Enable all optimizations
                break;
        }
    }
    
    /**
     * Get memory usage statistics
     */
    public String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        return String.format("Memory: %dMB used / %dMB max (%.1f%%)",
                usedMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                (usedMemory * 100.0f) / maxMemory);
    }
}
