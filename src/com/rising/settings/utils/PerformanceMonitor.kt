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

package com.rising.settings.utils

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance monitoring utility for tracking memory usage, execution times,
 * and identifying performance bottlenecks in the Personalizations app.
 */
class PerformanceMonitor private constructor() {
    
    companion object {
        private const val TAG = "PersonalizationsPerf"
        private const val DEBUG = false // Set to true for performance logging
        
        // Singleton instance
        @Volatile
        private var sInstance: PerformanceMonitor? = null
        
        @JvmStatic
        fun getInstance(): PerformanceMonitor {
            return sInstance ?: synchronized(this) {
                sInstance ?: PerformanceMonitor().also { sInstance = it }
            }
        }
        
        /**
         * Enable or disable debug logging
         */
        @JvmStatic
        fun setDebugEnabled(enabled: Boolean) {
            // This would typically be controlled by a system property or build flag
            // For now, it's controlled by the DEBUG constant
        }
    }
    
    // Track timing operations
    private val mTimingMap = ConcurrentHashMap<String, Long>()
    
    // Track memory usage
    private val mMemoryMap = ConcurrentHashMap<String, Long>()
    
    /**
     * Start timing an operation
     */
    fun startTiming(operationName: String) {
        if (!DEBUG) return
        
        mTimingMap[operationName] = SystemClock.elapsedRealtime()
        logMemoryUsage("${operationName}_start")
    }
    
    /**
     * End timing an operation and log the result
     */
    fun endTiming(operationName: String) {
        if (!DEBUG) return
        
        val startTime = mTimingMap.remove(operationName)
        if (startTime != null) {
            val duration = SystemClock.elapsedRealtime() - startTime
            Log.d(TAG, "$operationName took ${duration}ms")
            
            // Log warning for slow operations
            if (duration > 100) {
                Log.w(TAG, "SLOW OPERATION: $operationName took ${duration}ms")
            }
        }
        
        logMemoryUsage("${operationName}_end")
    }
    
    /**
     * Log current memory usage
     */
    fun logMemoryUsage(checkpoint: String) {
        if (!DEBUG) return
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        // Convert to MB for readability
        val usedMB = usedMemory / (1024 * 1024)
        val maxMB = maxMemory / (1024 * 1024)
        
        Log.d(TAG, "$checkpoint - Memory: ${usedMB}MB / ${maxMB}MB" +
                " (${usedMB * 100 / maxMB}%)")
        
        // Store for comparison
        mMemoryMap[checkpoint] = usedMemory
        
        // Log warning for high memory usage
        if (usedMB * 100 / maxMB > 80) {
            Log.w(TAG, "HIGH MEMORY USAGE at $checkpoint: ${usedMB}MB")
        }
    }
    
    /**
     * Log memory difference between two checkpoints
     */
    fun logMemoryDifference(startCheckpoint: String, endCheckpoint: String) {
        if (!DEBUG) return
        
        val startMemory = mMemoryMap[startCheckpoint]
        val endMemory = mMemoryMap[endCheckpoint]
        
        if (startMemory != null && endMemory != null) {
            val difference = endMemory - startMemory
            val differenceMB = difference / (1024 * 1024)
            
            if (difference > 0) {
                Log.d(TAG, "Memory increased by ${differenceMB}MB from " + 
                        "$startCheckpoint to $endCheckpoint")
                
                // Log warning for significant memory increases
                if (differenceMB > 10) {
                    Log.w(TAG, "MEMORY LEAK POTENTIAL: ${differenceMB}MB increase")
                }
            } else {
                Log.d(TAG, "Memory decreased by ${kotlin.math.abs(differenceMB)}MB from " + 
                        "$startCheckpoint to $endCheckpoint")
            }
        }
    }
    
    /**
     * Force garbage collection and log memory before/after
     */
    fun forceGCAndLog(context: String) {
        if (!DEBUG) return
        
        logMemoryUsage("${context}_before_gc")
        System.gc()
        
        // Wait a bit for GC to complete
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        
        logMemoryUsage("${context}_after_gc")
    }
    
    /**
     * Log native heap information
     */
    fun logNativeHeap(context: String) {
        if (!DEBUG) return
        
        val nativeHeapSize = Debug.getNativeHeapSize() / (1024 * 1024)
        val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        val nativeHeapFree = Debug.getNativeHeapFreeSize() / (1024 * 1024)
        
        Log.d(TAG, "$context - Native Heap: ${nativeHeapAllocated}MB allocated, " +
                "${nativeHeapFree}MB free, ${nativeHeapSize}MB total")
    }
    
    /**
     * Clear all stored timing and memory data
     */
    fun clearData() {
        mTimingMap.clear()
        mMemoryMap.clear()
    }
    
    /**
     * Utility method to wrap operations with timing
     */
    fun timeOperation(operationName: String, operation: Runnable) {
        startTiming(operationName)
        try {
            operation.run()
        } finally {
            endTiming(operationName)
        }
    }
    
    /**
     * Utility method to wrap operations with timing (Kotlin lambda version)
     */
    inline fun timeOperation(operationName: String, operation: () -> Unit) {
        startTiming(operationName)
        try {
            operation()
        } finally {
            endTiming(operationName)
        }
    }
    
    /**
     * Log fragment lifecycle events for performance tracking
     */
    fun logFragmentLifecycle(fragmentName: String, lifecycleEvent: String) {
        if (!DEBUG) return
        
        Log.d(TAG, "$fragmentName - $lifecycleEvent")
        logMemoryUsage("${fragmentName}_$lifecycleEvent")
    }
    
    /**
     * Log animation performance metrics
     */
    fun logAnimationPerformance(animationName: String, frameCount: Long, totalTime: Long) {
        if (!DEBUG) return
        
        if (totalTime > 0) {
            val fps = (frameCount * 1000) / totalTime
            Log.d(TAG, "$animationName - $frameCount frames in ${totalTime}ms" +
                    " (avg $fps FPS)")
            
            if (fps < 30) {
                Log.w(TAG, "LOW FPS ANIMATION: $animationName only $fps FPS")
            }
        }
    }
}
