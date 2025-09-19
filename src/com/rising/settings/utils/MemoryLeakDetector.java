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

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to detect potential memory leaks in fragments and activities.
 * Tracks references and reports potential leaks when objects should have been garbage collected.
 */
public class MemoryLeakDetector {
    
    private static final String TAG = "MemoryLeakDetector";
    private static final boolean DEBUG = false; // Set to true for leak detection
    private static final long LEAK_CHECK_DELAY = 5000; // 5 seconds
    
    // Singleton instance
    private static MemoryLeakDetector sInstance;
    
    // Track weak references to fragments and activities
    private final Map<String, WeakReference<Object>> mTrackedObjects = new ConcurrentHashMap<>();
    
    // Track creation times
    private final Map<String, Long> mCreationTimes = new ConcurrentHashMap<>();
    
    // Handler for delayed leak checks
    private final Handler mHandler = new Handler();
    
    private MemoryLeakDetector() {
        // Private constructor for singleton
    }
    
    public static MemoryLeakDetector getInstance() {
        if (sInstance == null) {
            synchronized (MemoryLeakDetector.class) {
                if (sInstance == null) {
                    sInstance = new MemoryLeakDetector();
                }
            }
        }
        return sInstance;
    }
    
    /**
     * Start tracking a fragment for potential memory leaks
     */
    public void trackFragment(Fragment fragment) {
        if (!DEBUG || fragment == null) return;
        
        String key = fragment.getClass().getSimpleName() + "@" + System.identityHashCode(fragment);
        mTrackedObjects.put(key, new WeakReference<>(fragment));
        mCreationTimes.put(key, System.currentTimeMillis());
        
        Log.d(TAG, "Started tracking fragment: " + key);
    }
    
    /**
     * Start tracking an activity for potential memory leaks
     */
    public void trackActivity(Activity activity) {
        if (!DEBUG || activity == null) return;
        
        String key = activity.getClass().getSimpleName() + "@" + System.identityHashCode(activity);
        mTrackedObjects.put(key, new WeakReference<>(activity));
        mCreationTimes.put(key, System.currentTimeMillis());
        
        Log.d(TAG, "Started tracking activity: " + key);
    }
    
    /**
     * Stop tracking an object and schedule a leak check
     */
    public void stopTracking(Object object) {
        if (!DEBUG || object == null) return;
        
        String key = object.getClass().getSimpleName() + "@" + System.identityHashCode(object);
        
        Log.d(TAG, "Stopped tracking: " + key);
        
        // Schedule a delayed check to see if the object was garbage collected
        mHandler.postDelayed(() -> checkForLeak(key), LEAK_CHECK_DELAY);
    }
    
    /**
     * Check if an object has been garbage collected
     */
    private void checkForLeak(String key) {
        WeakReference<Object> ref = mTrackedObjects.get(key);
        if (ref != null) {
            Object obj = ref.get();
            if (obj != null) {
                // Object still exists - potential memory leak
                Long creationTime = mCreationTimes.get(key);
                long age = creationTime != null ? System.currentTimeMillis() - creationTime : 0;
                
                Log.w(TAG, "POTENTIAL MEMORY LEAK: " + key + " still exists after " + 
                        (age / 1000) + " seconds");
                
                // Additional checks for common leak causes
                if (obj instanceof Fragment) {
                    checkFragmentLeaks((Fragment) obj, key);
                } else if (obj instanceof Activity) {
                    checkActivityLeaks((Activity) obj, key);
                }
            } else {
                // Object was garbage collected - good!
                Log.d(TAG, "Object successfully garbage collected: " + key);
            }
            
            // Clean up tracking data
            mTrackedObjects.remove(key);
            mCreationTimes.remove(key);
        }
    }
    
    /**
     * Check for common fragment memory leak patterns
     */
    private void checkFragmentLeaks(Fragment fragment, String key) {
        try {
            // Check if fragment is still attached when it shouldn't be
            if (fragment.isAdded()) {
                Log.w(TAG, "Fragment " + key + " is still attached - possible leak");
            }
            
            // Check if fragment has a context when it shouldn't
            if (fragment.getContext() != null) {
                Log.w(TAG, "Fragment " + key + " still has context - possible leak");
            }
            
            // Check if fragment has an activity reference
            if (fragment.getActivity() != null) {
                Log.w(TAG, "Fragment " + key + " still has activity reference - possible leak");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking fragment leaks for " + key, e);
        }
    }
    
    /**
     * Check for common activity memory leak patterns
     */
    private void checkActivityLeaks(Activity activity, String key) {
        try {
            // Check if activity is still running when it shouldn't be
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                Log.w(TAG, "Activity " + key + " is still running - possible leak");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking activity leaks for " + key, e);
        }
    }
    
    /**
     * Force garbage collection and check all tracked objects
     */
    public void forceLeakCheck() {
        if (!DEBUG) return;
        
        Log.d(TAG, "Forcing leak check for " + mTrackedObjects.size() + " tracked objects");
        
        // Force garbage collection
        System.gc();
        
        // Wait a bit for GC to complete
        mHandler.postDelayed(() -> {
            Set<String> keys = mTrackedObjects.keySet();
            for (String key : keys) {
                checkForLeak(key);
            }
        }, 1000);
    }
    
    /**
     * Get the number of currently tracked objects
     */
    public int getTrackedObjectCount() {
        return mTrackedObjects.size();
    }
    
    /**
     * Clear all tracking data
     */
    public void clearAll() {
        mTrackedObjects.clear();
        mCreationTimes.clear();
        mHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Log statistics about tracked objects
     */
    public void logStatistics() {
        if (!DEBUG) return;
        
        Log.d(TAG, "Memory leak detector statistics:");
        Log.d(TAG, "  Tracked objects: " + mTrackedObjects.size());
        
        long currentTime = System.currentTimeMillis();
        int oldObjects = 0;
        
        for (Map.Entry<String, Long> entry : mCreationTimes.entrySet()) {
            long age = currentTime - entry.getValue();
            if (age > 30000) { // Objects older than 30 seconds
                oldObjects++;
            }
        }
        
        Log.d(TAG, "  Objects older than 30s: " + oldObjects);
        
        if (oldObjects > 0) {
            Log.w(TAG, "Found " + oldObjects + " old objects - potential memory leaks");
        }
    }
    
    /**
     * Enable or disable leak detection
     */
    public static void setDebugEnabled(boolean enabled) {
        // This would typically be controlled by a system property or build flag
        // For now, it's controlled by the DEBUG constant
    }
}
