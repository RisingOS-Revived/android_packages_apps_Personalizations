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

import android.app.Activity
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility class to detect potential memory leaks in fragments and activities.
 * Tracks references and reports potential leaks when objects should have been garbage collected.
 */
class MemoryLeakDetector private constructor() {
    
    companion object {
        private const val TAG = "MemoryLeakDetector"
        private const val DEBUG = false // Set to true for leak detection
        private const val LEAK_CHECK_DELAY = 5000L // 5 seconds
        
        // Singleton instance
        @Volatile
        private var sInstance: MemoryLeakDetector? = null
        
        @JvmStatic
        fun getInstance(): MemoryLeakDetector {
            return sInstance ?: synchronized(this) {
                sInstance ?: MemoryLeakDetector().also { sInstance = it }
            }
        }
        
        /**
         * Enable or disable leak detection
         */
        @JvmStatic
        fun setDebugEnabled(enabled: Boolean) {
            // This would typically be controlled by a system property or build flag
            // For now, it's controlled by the DEBUG constant
        }
    }
    
    // Track weak references to fragments and activities
    private val mTrackedObjects = ConcurrentHashMap<String, WeakReference<Any>>()
    
    // Track creation times
    private val mCreationTimes = ConcurrentHashMap<String, Long>()
    
    // Handler for delayed leak checks
    private val mHandler = Handler()
    
    /**
     * Start tracking a fragment for potential memory leaks
     */
    fun trackFragment(fragment: Fragment?) {
        if (!DEBUG || fragment == null) return
        
        val key = "${fragment.javaClass.simpleName}@${System.identityHashCode(fragment)}"
        mTrackedObjects[key] = WeakReference(fragment)
        mCreationTimes[key] = System.currentTimeMillis()
        
        Log.d(TAG, "Started tracking fragment: $key")
    }
    
    /**
     * Start tracking an activity for potential memory leaks
     */
    fun trackActivity(activity: Activity?) {
        if (!DEBUG || activity == null) return
        
        val key = "${activity.javaClass.simpleName}@${System.identityHashCode(activity)}"
        mTrackedObjects[key] = WeakReference(activity)
        mCreationTimes[key] = System.currentTimeMillis()
        
        Log.d(TAG, "Started tracking activity: $key")
    }
    
    /**
     * Stop tracking an object and schedule a leak check
     */
    fun stopTracking(obj: Any?) {
        if (!DEBUG || obj == null) return
        
        val key = "${obj.javaClass.simpleName}@${System.identityHashCode(obj)}"
        
        Log.d(TAG, "Stopped tracking: $key")
        
        // Schedule a delayed check to see if the object was garbage collected
        mHandler.postDelayed({ checkForLeak(key) }, LEAK_CHECK_DELAY)
    }
    
    /**
     * Check if an object has been garbage collected
     */
    private fun checkForLeak(key: String) {
        val ref = mTrackedObjects[key]
        if (ref != null) {
            val obj = ref.get()
            if (obj != null) {
                // Object still exists - potential memory leak
                val creationTime = mCreationTimes[key]
                val age = if (creationTime != null) System.currentTimeMillis() - creationTime else 0
                
                Log.w(TAG, "POTENTIAL MEMORY LEAK: $key still exists after " + 
                        "${age / 1000} seconds")
                
                // Additional checks for common leak causes
                when (obj) {
                    is Fragment -> checkFragmentLeaks(obj, key)
                    is Activity -> checkActivityLeaks(obj, key)
                }
            } else {
                // Object was garbage collected - good!
                Log.d(TAG, "Object successfully garbage collected: $key")
            }
            
            // Clean up tracking data
            mTrackedObjects.remove(key)
            mCreationTimes.remove(key)
        }
    }
    
    /**
     * Check for common fragment memory leak patterns
     */
    private fun checkFragmentLeaks(fragment: Fragment, key: String) {
        try {
            // Check if fragment is still attached when it shouldn't be
            if (fragment.isAdded) {
                Log.w(TAG, "Fragment $key is still attached - possible leak")
            }
            
            // Check if fragment has a context when it shouldn't
            if (fragment.context != null) {
                Log.w(TAG, "Fragment $key still has context - possible leak")
            }
            
            // Check if fragment has an activity reference
            if (fragment.activity != null) {
                Log.w(TAG, "Fragment $key still has activity reference - possible leak")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking fragment leaks for $key", e)
        }
    }
    
    /**
     * Check for common activity memory leak patterns
     */
    private fun checkActivityLeaks(activity: Activity, key: String) {
        try {
            // Check if activity is still running when it shouldn't be
            if (!activity.isFinishing && !activity.isDestroyed) {
                Log.w(TAG, "Activity $key is still running - possible leak")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking activity leaks for $key", e)
        }
    }
    
    /**
     * Force garbage collection and check all tracked objects
     */
    fun forceLeakCheck() {
        if (!DEBUG) return
        
        Log.d(TAG, "Forcing leak check for ${mTrackedObjects.size} tracked objects")
        
        // Force garbage collection
        System.gc()
        
        // Wait a bit for GC to complete
        mHandler.postDelayed({
            val keys = mTrackedObjects.keys.toSet()
            for (key in keys) {
                checkForLeak(key)
            }
        }, 1000)
    }
    
    /**
     * Get the number of currently tracked objects
     */
    fun getTrackedObjectCount(): Int {
        return mTrackedObjects.size
    }
    
    /**
     * Clear all tracking data
     */
    fun clearAll() {
        mTrackedObjects.clear()
        mCreationTimes.clear()
        mHandler.removeCallbacksAndMessages(null)
    }
    
    /**
     * Log statistics about tracked objects
     */
    fun logStatistics() {
        if (!DEBUG) return
        
        Log.d(TAG, "Memory leak detector statistics:")
        Log.d(TAG, "  Tracked objects: ${mTrackedObjects.size}")
        
        val currentTime = System.currentTimeMillis()
        var oldObjects = 0
        
        for ((_, creationTime) in mCreationTimes) {
            val age = currentTime - creationTime
            if (age > 30000) { // Objects older than 30 seconds
                oldObjects++
            }
        }
        
        Log.d(TAG, "  Objects older than 30s: $oldObjects")
        
        if (oldObjects > 0) {
            Log.w(TAG, "Found $oldObjects old objects - potential memory leaks")
        }
    }
}
