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

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Runtime performance optimizer that dynamically adjusts performance parameters
 * based on device capabilities and current system state.
 */
class RuntimeOptimizer private constructor() {
    
    companion object {
        private const val TAG = "RuntimeOptimizer"
        private const val DEBUG = false
        
        // Singleton instance
        @Volatile
        private var sInstance: RuntimeOptimizer? = null
        
        @JvmStatic
        fun getInstance(): RuntimeOptimizer {
            return sInstance ?: synchronized(this) {
                sInstance ?: RuntimeOptimizer().also { sInstance = it }
            }
        }
    }
    
    // Performance monitoring
    private val mOptimizationActive = AtomicBoolean(false)
    private val mLastOptimizationTime = AtomicLong(0)
    
    // Device capabilities
    private var mDeviceMemoryClass: Int = 0
    private var mProcessorCount: Int = 0
    private var mIsLowRamDevice: Boolean = false
    
    // Optimization parameters
    private var mOptimalCacheSize: Int = 0
    private var mOptimalThreadPoolSize: Int = 0
    private var mOptimalGCInterval: Long = 0
    
    /**
     * Initialize the optimizer with device capabilities
     */
    fun initialize(context: Context?) {
        if (context == null) return
        
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null) {
            mDeviceMemoryClass = am.memoryClass
            mIsLowRamDevice = am.isLowRamDevice
        }
        
        mProcessorCount = Runtime.getRuntime().availableProcessors()
        
        calculateOptimalParameters()
        
        if (DEBUG) {
            Log.d(TAG, "RuntimeOptimizer initialized:")
            Log.d(TAG, "  Memory Class: ${mDeviceMemoryClass}MB")
            Log.d(TAG, "  Processor Count: $mProcessorCount")
            Log.d(TAG, "  Low RAM Device: $mIsLowRamDevice")
            Log.d(TAG, "  Optimal Cache Size: $mOptimalCacheSize")
            Log.d(TAG, "  Optimal Thread Pool Size: $mOptimalThreadPoolSize")
        }
    }
    
    /**
     * Calculate optimal parameters based on device capabilities
     */
    private fun calculateOptimalParameters() {
        // Calculate optimal cache size based on available memory
        mOptimalCacheSize = if (mIsLowRamDevice) {
            maxOf(10, mDeviceMemoryClass / 8) // Conservative for low RAM
        } else {
            maxOf(25, mDeviceMemoryClass / 4) // More aggressive for high RAM
        }
        
        // Calculate optimal thread pool size
        mOptimalThreadPoolSize = if (mIsLowRamDevice) {
            maxOf(1, mProcessorCount / 2)
        } else {
            maxOf(2, mProcessorCount)
        }
        
        // Calculate optimal GC interval
        mOptimalGCInterval = if (mIsLowRamDevice) {
            10000L // 10 seconds for low RAM devices
        } else {
            30000L // 30 seconds for normal devices
        }
    }
    
    /**
     * Get optimal cache size for the current device
     */
    fun getOptimalCacheSize(): Int {
        return mOptimalCacheSize
    }
    
    /**
     * Get optimal thread pool size for the current device
     */
    fun getOptimalThreadPoolSize(): Int {
        return mOptimalThreadPoolSize
    }
    
    /**
     * Get optimal garbage collection interval
     */
    fun getOptimalGCInterval(): Long {
        return mOptimalGCInterval
    }
    
    /**
     * Check if device is low on memory
     */
    fun isLowMemory(context: Context?): Boolean {
        if (context == null) return false
        
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (am != null) {
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            return memInfo.lowMemory
        }
        return false
    }
    
    /**
     * Optimize performance based on current system state
     */
    fun optimizePerformance(context: Context?) {
        if (mOptimizationActive.get()) {
            return // Already optimizing
        }
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - mLastOptimizationTime.get() < 5000) {
            return // Don't optimize too frequently
        }
        
        mOptimizationActive.set(true)
        mLastOptimizationTime.set(currentTime)
        
        try {
            // Check memory pressure and adjust accordingly
            if (isLowMemory(context)) {
                performLowMemoryOptimization()
            } else {
                performNormalOptimization()
            }
        } finally {
            mOptimizationActive.set(false)
        }
    }
    
    /**
     * Perform optimizations for low memory conditions
     */
    private fun performLowMemoryOptimization() {
        if (DEBUG) {
            Log.d(TAG, "Performing low memory optimization")
        }
        
        // Suggest garbage collection
        Runtime.getRuntime().gc()
        
        // Clear performance monitor caches if available
        PerformanceMonitor.getInstance().clearData()
        
        // Force memory leak detector to check
        MemoryLeakDetector.getInstance().forceLeakCheck()
    }
    
    /**
     * Perform normal optimizations
     */
    private fun performNormalOptimization() {
        if (DEBUG) {
            Log.d(TAG, "Performing normal optimization")
        }
        
        // Log current memory usage
        PerformanceMonitor.getInstance().logMemoryUsage("runtime_optimization")
    }
    
    /**
     * Get recommended animation duration multiplier based on device performance
     */
    fun getAnimationDurationMultiplier(): Float {
        return when {
            mIsLowRamDevice -> 1.2f // Slower animations for low-end devices
            mProcessorCount >= 8 -> 0.8f // Faster animations for high-end devices
            else -> 1.0f // Normal speed for mid-range devices
        }
    }
    
    /**
     * Check if background processing should be limited
     */
    fun shouldLimitBackgroundProcessing(): Boolean {
        return mIsLowRamDevice || mProcessorCount < 4
    }
    
    /**
     * Get recommended frame rate target based on device capabilities
     */
    fun getRecommendedFrameRate(): Int {
        return when {
            mIsLowRamDevice -> 30 // 30 FPS for low-end devices
            mProcessorCount >= 8 && mDeviceMemoryClass >= 512 -> 60 // 60 FPS for high-end devices
            else -> 45 // 45 FPS for mid-range devices
        }
    }
    
    /**
     * Schedule periodic optimization
     */
    fun schedulePeriodicOptimization(context: Context?) {
        val handler = Handler(Looper.getMainLooper())
        
        val optimizationRunnable = object : Runnable {
            override fun run() {
                optimizePerformance(context)
                // Schedule next optimization
                handler.postDelayed(this, getOptimalGCInterval())
            }
        }
        
        // Start periodic optimization
        handler.postDelayed(optimizationRunnable, getOptimalGCInterval())
    }
    
    /**
     * Get device performance tier
     */
    fun getDevicePerformanceTier(): Int {
        return when {
            mIsLowRamDevice || mProcessorCount < 4 || mDeviceMemoryClass < 256 -> 1 // Low-end
            mProcessorCount >= 8 && mDeviceMemoryClass >= 512 -> 3 // High-end
            else -> 2 // Mid-range
        }
    }
    
    /**
     * Apply device-specific optimizations
     */
    fun applyDeviceOptimizations() {
        val tier = getDevicePerformanceTier()
        
        if (DEBUG) {
            Log.d(TAG, "Applying optimizations for performance tier: $tier")
        }
        
        // Apply optimizations based on device tier
        when (tier) {
            1 -> { // Low-end
                // Apply low-end optimizations - disable debug features
                if (BuildOptimizations.shouldEnableMemoryLeakDetection()) {
                    MemoryLeakDetector.setDebugEnabled(false)
                }
            }
            2 -> { // Mid-range
                // Default settings
            }
            3 -> { // High-end
                // Enable all optimizations
            }
        }
    }
    
    /**
     * Get memory usage statistics
     */
    fun getMemoryStats(): String {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        return String.format("Memory: %dMB used / %dMB max (%.1f%%)",
                usedMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                (usedMemory * 100.0f) / maxMemory)
    }
}
