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

import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Performance monitoring utility for tracking memory usage, execution times,
 * and identifying performance bottlenecks in the Personalizations app.
 */
public class PerformanceMonitor {
    
    private static final String TAG = "PersonalizationsPerf";
    private static final boolean DEBUG = false; // Set to true for performance logging
    
    // Singleton instance
    private static PerformanceMonitor sInstance;
    
    // Track timing operations
    private final Map<String, Long> mTimingMap = new ConcurrentHashMap<>();
    
    // Track memory usage
    private final Map<String, Long> mMemoryMap = new ConcurrentHashMap<>();
    
    private PerformanceMonitor() {
        // Private constructor for singleton
    }
    
    public static PerformanceMonitor getInstance() {
        if (sInstance == null) {
            synchronized (PerformanceMonitor.class) {
                if (sInstance == null) {
                    sInstance = new PerformanceMonitor();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Start timing an operation
     */
    public void startTiming(String operationName) {
        if (!DEBUG) return;
        
        mTimingMap.put(operationName, SystemClock.elapsedRealtime());
        logMemoryUsage(operationName + "_start");
    }
    
    /**
     * End timing an operation and log the result
     */
    public void endTiming(String operationName) {
        if (!DEBUG) return;
        
        Long startTime = mTimingMap.remove(operationName);
        if (startTime != null) {
            long duration = SystemClock.elapsedRealtime() - startTime;
            Log.d(TAG, operationName + " took " + duration + "ms");
            
            // Log warning for slow operations
            if (duration > 100) {
                Log.w(TAG, "SLOW OPERATION: " + operationName + " took " + duration + "ms");
            }
        }
        
        logMemoryUsage(operationName + "_end");
    }
    
    /**
     * Log current memory usage
     */
    public void logMemoryUsage(String checkpoint) {
        if (!DEBUG) return;
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        // Convert to MB for readability
        long usedMB = usedMemory / (1024 * 1024);
        long maxMB = maxMemory / (1024 * 1024);
        
        Log.d(TAG, checkpoint + " - Memory: " + usedMB + "MB / " + maxMB + "MB" +
                " (" + (usedMB * 100 / maxMB) + "%)");
        
        // Store for comparison
        mMemoryMap.put(checkpoint, usedMemory);
        
        // Log warning for high memory usage
        if (usedMB * 100 / maxMB > 80) {
            Log.w(TAG, "HIGH MEMORY USAGE at " + checkpoint + ": " + usedMB + "MB");
        }
    }
    
    /**
     * Log memory difference between two checkpoints
     */
    public void logMemoryDifference(String startCheckpoint, String endCheckpoint) {
        if (!DEBUG) return;
        
        Long startMemory = mMemoryMap.get(startCheckpoint);
        Long endMemory = mMemoryMap.get(endCheckpoint);
        
        if (startMemory != null && endMemory != null) {
            long difference = endMemory - startMemory;
            long differenceMB = difference / (1024 * 1024);
            
            if (difference > 0) {
                Log.d(TAG, "Memory increased by " + differenceMB + "MB from " + 
                        startCheckpoint + " to " + endCheckpoint);
                
                // Log warning for significant memory increases
                if (differenceMB > 10) {
                    Log.w(TAG, "MEMORY LEAK POTENTIAL: " + differenceMB + "MB increase");
                }
            } else {
                Log.d(TAG, "Memory decreased by " + Math.abs(differenceMB) + "MB from " + 
                        startCheckpoint + " to " + endCheckpoint);
            }
        }
    }
    
    /**
     * Force garbage collection and log memory before/after
     */
    public void forceGCAndLog(String context) {
        if (!DEBUG) return;
        
        logMemoryUsage(context + "_before_gc");
        System.gc();
        
        // Wait a bit for GC to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logMemoryUsage(context + "_after_gc");
    }
    
    /**
     * Log native heap information
     */
    public void logNativeHeap(String context) {
        if (!DEBUG) return;
        
        long nativeHeapSize = Debug.getNativeHeapSize() / (1024 * 1024);
        long nativeHeapAllocated = Debug.getNativeHeapAllocatedSize() / (1024 * 1024);
        long nativeHeapFree = Debug.getNativeHeapFreeSize() / (1024 * 1024);
        
        Log.d(TAG, context + " - Native Heap: " + nativeHeapAllocated + "MB allocated, " +
                nativeHeapFree + "MB free, " + nativeHeapSize + "MB total");
    }
    
    /**
     * Clear all stored timing and memory data
     */
    public void clearData() {
        mTimingMap.clear();
        mMemoryMap.clear();
    }
    
    /**
     * Enable or disable debug logging
     */
    public static void setDebugEnabled(boolean enabled) {
        // This would typically be controlled by a system property or build flag
        // For now, it's controlled by the DEBUG constant
    }
    
    /**
     * Utility method to wrap operations with timing
     */
    public void timeOperation(String operationName, Runnable operation) {
        startTiming(operationName);
        try {
            operation.run();
        } finally {
            endTiming(operationName);
        }
    }
    
    /**
     * Log fragment lifecycle events for performance tracking
     */
    public void logFragmentLifecycle(String fragmentName, String lifecycleEvent) {
        if (!DEBUG) return;
        
        Log.d(TAG, fragmentName + " - " + lifecycleEvent);
        logMemoryUsage(fragmentName + "_" + lifecycleEvent);
    }
    
    /**
     * Log animation performance metrics
     */
    public void logAnimationPerformance(String animationName, long frameCount, long totalTime) {
        if (!DEBUG) return;
        
        if (totalTime > 0) {
            long fps = (frameCount * 1000) / totalTime;
            Log.d(TAG, animationName + " - " + frameCount + " frames in " + totalTime + "ms" +
                    " (avg " + fps + " FPS)");
            
            if (fps < 30) {
                Log.w(TAG, "LOW FPS ANIMATION: " + animationName + " only " + fps + " FPS");
            }
        }
    }
}
