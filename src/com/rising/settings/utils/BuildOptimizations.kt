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

import android.os.SystemProperties
import android.util.Log

/**
 * Build-time and runtime optimizations for the Personalizations app.
 * This class provides utilities for enabling/disabling performance features
 * based on build configuration and system properties.
 */
object BuildOptimizations {
    
    private const val TAG = "PersonalizationsBuildOpt"
    
    // System properties for performance tuning
    private const val PROP_PERFORMANCE_MODE = "persist.personalizations.performance_mode"
    private const val PROP_DEBUG_PERFORMANCE = "persist.personalizations.debug_perf"
    private const val PROP_CACHE_ENABLED = "persist.personalizations.cache_enabled"
    private const val PROP_ANIMATION_OPTIMIZATION = "persist.personalizations.anim_opt"
    
    // Performance modes
    const val PERFORMANCE_MODE_BALANCED = 0
    const val PERFORMANCE_MODE_PERFORMANCE = 1
    const val PERFORMANCE_MODE_BATTERY = 2
    
    // Cache settings
    private const val DEFAULT_CACHE_SIZE = 50
    private const val PERFORMANCE_CACHE_SIZE = 100
    private const val BATTERY_CACHE_SIZE = 25
    
    /**
     * Get the current performance mode
     */
    @JvmStatic
    fun getPerformanceMode(): Int {
        return SystemProperties.getInt(PROP_PERFORMANCE_MODE, PERFORMANCE_MODE_BALANCED)
    }
    
    /**
     * Check if performance debugging is enabled
     */
    @JvmStatic
    fun isPerformanceDebugEnabled(): Boolean {
        return SystemProperties.getBoolean(PROP_DEBUG_PERFORMANCE, false)
    }
    
    /**
     * Check if caching is enabled
     */
    @JvmStatic
    fun isCacheEnabled(): Boolean {
        return SystemProperties.getBoolean(PROP_CACHE_ENABLED, true)
    }
    
    /**
     * Check if animation optimizations are enabled
     */
    @JvmStatic
    fun isAnimationOptimizationEnabled(): Boolean {
        return SystemProperties.getBoolean(PROP_ANIMATION_OPTIMIZATION, true)
    }
    
    /**
     * Get optimal cache size based on performance mode
     */
    @JvmStatic
    fun getOptimalCacheSize(): Int {
        if (!isCacheEnabled()) {
            return 0
        }
        
        return when (getPerformanceMode()) {
            PERFORMANCE_MODE_PERFORMANCE -> PERFORMANCE_CACHE_SIZE
            PERFORMANCE_MODE_BATTERY -> BATTERY_CACHE_SIZE
            PERFORMANCE_MODE_BALANCED -> DEFAULT_CACHE_SIZE
            else -> DEFAULT_CACHE_SIZE
        }
    }
    
    /**
     * Get animation frame rate target based on performance mode
     */
    @JvmStatic
    fun getTargetFrameRate(): Int {
        return when (getPerformanceMode()) {
            PERFORMANCE_MODE_PERFORMANCE -> 60 // 60 FPS for performance mode
            PERFORMANCE_MODE_BATTERY -> 30 // 30 FPS for battery mode
            PERFORMANCE_MODE_BALANCED -> 45 // 45 FPS for balanced mode
            else -> 45
        }
    }
    
    /**
     * Get animation duration multiplier based on performance mode
     */
    @JvmStatic
    fun getAnimationDurationMultiplier(): Float {
        return when (getPerformanceMode()) {
            PERFORMANCE_MODE_PERFORMANCE -> 0.8f // Faster animations
            PERFORMANCE_MODE_BATTERY -> 1.2f // Slower animations to save battery
            PERFORMANCE_MODE_BALANCED -> 1.0f // Normal speed
            else -> 1.0f
        }
    }
    
    /**
     * Check if background processing should be enabled
     */
    @JvmStatic
    fun shouldUseBackgroundProcessing(): Boolean {
        val mode = getPerformanceMode()
        return mode == PERFORMANCE_MODE_PERFORMANCE || mode == PERFORMANCE_MODE_BALANCED
    }
    
    /**
     * Get the optimal thread pool size for background tasks
     */
    @JvmStatic
    fun getOptimalThreadPoolSize(): Int {
        val processors = Runtime.getRuntime().availableProcessors()
        
        return when (getPerformanceMode()) {
            PERFORMANCE_MODE_PERFORMANCE -> maxOf(2, processors) // Use all available processors
            PERFORMANCE_MODE_BATTERY -> 1 // Single thread to save battery
            PERFORMANCE_MODE_BALANCED -> maxOf(1, processors / 2) // Use half the processors
            else -> maxOf(1, processors / 2)
        }
    }
    
    /**
     * Check if memory leak detection should be enabled
     */
    @JvmStatic
    fun shouldEnableMemoryLeakDetection(): Boolean {
        return isPerformanceDebugEnabled()
    }
    
    /**
     * Get the optimal garbage collection hint frequency
     */
    @JvmStatic
    fun getGCHintInterval(): Long {
        return when (getPerformanceMode()) {
            PERFORMANCE_MODE_PERFORMANCE -> 30000L // 30 seconds - less frequent GC hints
            PERFORMANCE_MODE_BATTERY -> 10000L // 10 seconds - more frequent to free memory
            PERFORMANCE_MODE_BALANCED -> 20000L // 20 seconds - balanced approach
            else -> 20000L
        }
    }
    
    /**
     * Check if resource preloading should be enabled
     */
    @JvmStatic
    fun shouldPreloadResources(): Boolean {
        return getPerformanceMode() == PERFORMANCE_MODE_PERFORMANCE
    }
    
    /**
     * Get the optimal bitmap cache size in bytes
     */
    @JvmStatic
    fun getBitmapCacheSize(): Int {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        
        return when (getPerformanceMode()) {
            PERFORMANCE_MODE_PERFORMANCE -> (maxMemory / 6).toInt() // Use 1/6 of available memory
            PERFORMANCE_MODE_BATTERY -> (maxMemory / 12).toInt() // Use 1/12 of available memory
            PERFORMANCE_MODE_BALANCED -> (maxMemory / 8).toInt() // Use 1/8 of available memory
            else -> (maxMemory / 8).toInt()
        }
    }
    
    /**
     * Log current optimization settings
     */
    @JvmStatic
    fun logOptimizationSettings() {
        if (!isPerformanceDebugEnabled()) {
            return
        }
        
        Log.d(TAG, "Performance Optimization Settings:")
        Log.d(TAG, "  Performance Mode: ${getPerformanceMode()}")
        Log.d(TAG, "  Cache Enabled: ${isCacheEnabled()}")
        Log.d(TAG, "  Cache Size: ${getOptimalCacheSize()}")
        Log.d(TAG, "  Target Frame Rate: ${getTargetFrameRate()}")
        Log.d(TAG, "  Animation Duration Multiplier: ${getAnimationDurationMultiplier()}")
        Log.d(TAG, "  Background Processing: ${shouldUseBackgroundProcessing()}")
        Log.d(TAG, "  Thread Pool Size: ${getOptimalThreadPoolSize()}")
        Log.d(TAG, "  Memory Leak Detection: ${shouldEnableMemoryLeakDetection()}")
        Log.d(TAG, "  GC Hint Interval: ${getGCHintInterval()}ms")
        Log.d(TAG, "  Resource Preloading: ${shouldPreloadResources()}")
        Log.d(TAG, "  Bitmap Cache Size: ${getBitmapCacheSize() / 1024 / 1024}MB")
    }
    
    /**
     * Apply runtime optimizations based on current settings
     */
    @JvmStatic
    fun applyRuntimeOptimizations() {
        logOptimizationSettings()
        
        // Enable performance monitoring if debug is enabled
        if (isPerformanceDebugEnabled()) {
            PerformanceMonitor.setDebugEnabled(true)
            MemoryLeakDetector.setDebugEnabled(true)
        }
        
        // Log memory status
        if (isPerformanceDebugEnabled()) {
            PerformanceMonitor.getInstance().logMemoryUsage("app_startup")
        }
    }
    
    /**
     * Get recommended ProGuard/R8 optimization level
     */
    @JvmStatic
    fun getRecommendedOptimizationLevel(): String {
        return when (getPerformanceMode()) {
            PERFORMANCE_MODE_PERFORMANCE -> "aggressive" // Maximum optimization
            PERFORMANCE_MODE_BATTERY -> "conservative" // Minimal optimization to preserve battery
            PERFORMANCE_MODE_BALANCED -> "balanced" // Standard optimization
            else -> "balanced"
        }
    }
}
