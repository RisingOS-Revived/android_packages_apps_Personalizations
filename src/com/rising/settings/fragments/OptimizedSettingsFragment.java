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

package com.rising.settings.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.Preference;

import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.android.ThemeUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Optimized base class for Settings fragments with performance improvements:
 * - Memory leak prevention
 * - Resource caching
 * - Proper lifecycle management
 * - Background task handling
 */
public abstract class OptimizedSettingsFragment extends SettingsPreferenceFragment {

    // Safe handler to prevent memory leaks
    private static class SafeHandler extends Handler {
        private final WeakReference<OptimizedSettingsFragment> mFragmentRef;
        
        SafeHandler(OptimizedSettingsFragment fragment) {
            super(Looper.getMainLooper());
            mFragmentRef = new WeakReference<>(fragment);
        }
        
        @Override
        public void handleMessage(android.os.Message msg) {
            OptimizedSettingsFragment fragment = mFragmentRef.get();
            if (fragment != null && fragment.isAdded()) {
                super.handleMessage(msg);
            }
        }
    }
    
    protected SafeHandler mHandler;
    protected ThemeUtils mThemeUtils;
    
    // Cache for preference states to prevent redundant operations
    protected final Map<String, Object> mPreferenceCache = new ConcurrentHashMap<>();
    
    // Cache for expensive operations
    protected final Map<String, Object> mOperationCache = new ConcurrentHashMap<>();
    
    // Track if fragment is properly initialized
    private boolean mIsInitialized = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize handler with weak reference
        mHandler = new SafeHandler(this);
        
        // Initialize ThemeUtils with null check
        if (getActivity() != null) {
            mThemeUtils = ThemeUtils.getInstance(getActivity());
        }
        
        mIsInitialized = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Cleanup to prevent memory leaks
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        
        mThemeUtils = null;
        mPreferenceCache.clear();
        mOperationCache.clear();
        mIsInitialized = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        
        // Additional cleanup
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        
        mPreferenceCache.clear();
        mOperationCache.clear();
    }

    /**
     * Safe method to check if fragment is properly initialized and attached
     */
    protected boolean isFragmentReady() {
        return mIsInitialized && isAdded() && getActivity() != null && !getActivity().isFinishing();
    }

    /**
     * Safe method to get context with null checks
     */
    protected Context getSafeContext() {
        if (!isFragmentReady()) {
            return null;
        }
        return getContext();
    }

    /**
     * Cached preference lookup to avoid repeated findViewById calls
     */
    @SuppressWarnings("unchecked")
    protected <T extends Preference> T findCachedPreference(String key) {
        T preference = (T) mPreferenceCache.get(key);
        if (preference == null) {
            preference = findPreference(key);
            if (preference != null) {
                mPreferenceCache.put(key, preference);
            }
        }
        return preference;
    }

    /**
     * Safe method to post delayed tasks with fragment lifecycle checks
     */
    protected void postDelayedSafe(Runnable runnable, long delayMillis) {
        if (mHandler != null && isFragmentReady()) {
            mHandler.postDelayed(() -> {
                if (isFragmentReady()) {
                    runnable.run();
                }
            }, delayMillis);
        }
    }

    /**
     * Cache expensive operations to prevent redundant execution
     */
    protected void cacheOperation(String key, Object result) {
        mOperationCache.put(key, result);
    }

    /**
     * Get cached operation result
     */
    @SuppressWarnings("unchecked")
    protected <T> T getCachedOperation(String key) {
        return (T) mOperationCache.get(key);
    }

    /**
     * Check if operation result is cached
     */
    protected boolean isOperationCached(String key) {
        return mOperationCache.containsKey(key);
    }

    /**
     * Safe ThemeUtils access with lazy initialization
     */
    protected ThemeUtils getSafeThemeUtils() {
        if (mThemeUtils == null && getSafeContext() != null) {
            mThemeUtils = ThemeUtils.getInstance(getSafeContext());
        }
        return mThemeUtils;
    }

    /**
     * Optimized overlay operation with caching
     */
    protected void setOverlayEnabledCached(String category, String packageName, String target) {
        String cacheKey = category + ":" + packageName + ":" + target;
        
        // Check if this operation was already performed
        if (isOperationCached(cacheKey)) {
            return;
        }
        
        ThemeUtils themeUtils = getSafeThemeUtils();
        if (themeUtils != null) {
            themeUtils.setOverlayEnabled(category, packageName, target);
            cacheOperation(cacheKey, true);
        }
    }

    /**
     * Clear operation cache when needed (e.g., on preference changes)
     */
    protected void clearOperationCache() {
        mOperationCache.clear();
    }

    /**
     * Safe method to remove callbacks and messages
     */
    protected void removeCallbacks() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}
