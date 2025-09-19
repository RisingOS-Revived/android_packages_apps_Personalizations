/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments.lockscreen;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.preferences.SecureSettingSeekBarPreference;
import com.android.settings.preferences.SecureSettingListPreference;
import com.android.settings.preferences.SecureSettingSwitchPreference;
import com.rising.settings.fragments.OptimizedSettingsFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

@SearchIndexable
public class PeekDisplay extends OptimizedSettingsFragment implements 
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "PeekDisplay";
    
    // Preference keys
    private static final String KEY_PEEK_DISPLAY_NOTIFICATIONS = "peek_display_notifications";
    private static final String KEY_PEEK_DISPLAY_STYLE = "peek_display_style";
    private static final String KEY_PEEK_DISPLAY_LOCATION = "peek_display_location";
    private static final String KEY_PEEK_DISPLAY_VERTICAL_OFFSET = "peek_display_vertical_offset";
    private static final String KEY_PEEK_DISPLAY_ALWAYS_VISIBLE = "peek_display_always_visible";
    private static final String KEY_PEEK_DISPLAY_TIMEOUT = "peek_display_timeout";
    
    // Settings keys for SystemUI communication
    private static final String SYSTEMUI_PEEK_DISPLAY_ENABLED = "peek_display_enabled";
    private static final String SYSTEMUI_PEEK_DISPLAY_DISMISSIBLE = "peek_display_dismissible";
    private static final String SYSTEMUI_PEEK_DISPLAY_CLOSE_ENABLED = "peek_display_close_enabled";
    private static final String SYSTEMUI_PEEK_DISPLAY_TIMEOUT = "peek_display_timeout_ms";
    
    // Position stability constants
    private static final int POSITION_UPDATE_DELAY = 100; // ms
    private static final int DEFAULT_VERTICAL_OFFSET = 50; // percent
    private static final int DEFAULT_TIMEOUT_SECONDS = 3; // seconds
    private static final int DEFAULT_TIMEOUT_MS = DEFAULT_TIMEOUT_SECONDS * 1000; // ms for SystemUI
    
    // Preferences
    private SecureSettingSwitchPreference mPeekDisplayNotifications;
    private SecureSettingListPreference mPeekDisplayStyle;
    private SecureSettingListPreference mPeekDisplayLocation;
    private SecureSettingSeekBarPreference mVerticalOffset;
    private SecureSettingSwitchPreference mAlwaysVisible;
    private SecureSettingSeekBarPreference mTimeout;
    
    // Position management
    private Handler mPositionHandler;
    private int mLastValidOffset = DEFAULT_VERTICAL_OFFSET;
    private boolean mIsUpdatingPosition = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_peek_display);
        
        // Initialize position handler
        mPositionHandler = new Handler(Looper.getMainLooper());
        
        initializePreferences();
        updatePreferenceDependencies();
        
        // Ensure close button functionality is enabled
        enableCloseButtonFunctionality();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * Initialize all preferences and set up listeners
     */
    private void initializePreferences() {
        Context context = getSafeContext();
        if (context == null) return;
        
        ContentResolver resolver = context.getContentResolver();
        
        // Get preferences
        mPeekDisplayNotifications = findCachedPreference(KEY_PEEK_DISPLAY_NOTIFICATIONS);
        mPeekDisplayStyle = findCachedPreference(KEY_PEEK_DISPLAY_STYLE);
        mPeekDisplayLocation = findCachedPreference(KEY_PEEK_DISPLAY_LOCATION);
        mVerticalOffset = findCachedPreference(KEY_PEEK_DISPLAY_VERTICAL_OFFSET);
        mAlwaysVisible = findCachedPreference(KEY_PEEK_DISPLAY_ALWAYS_VISIBLE);
        mTimeout = findCachedPreference(KEY_PEEK_DISPLAY_TIMEOUT);
        
        // Set up listeners
        if (mPeekDisplayNotifications != null) {
            mPeekDisplayNotifications.setOnPreferenceChangeListener(this);
        }
        if (mPeekDisplayLocation != null) {
            mPeekDisplayLocation.setOnPreferenceChangeListener(this);
        }
        if (mVerticalOffset != null) {
            mVerticalOffset.setOnPreferenceChangeListener(this);
            // Load cached position
            int currentOffset = Settings.Secure.getInt(resolver, 
                KEY_PEEK_DISPLAY_VERTICAL_OFFSET, DEFAULT_VERTICAL_OFFSET);
            mLastValidOffset = currentOffset;
        }
        if (mAlwaysVisible != null) {
            mAlwaysVisible.setOnPreferenceChangeListener(this);
        }
        if (mTimeout != null) {
            mTimeout.setOnPreferenceChangeListener(this);
            // Initialize timeout setting for SystemUI
            int timeoutSeconds = Settings.Secure.getInt(resolver, 
                KEY_PEEK_DISPLAY_TIMEOUT, DEFAULT_TIMEOUT_SECONDS);
            int timeoutMs = timeoutSeconds * 1000;
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, timeoutMs);
        }
    }
    
    /**
     * Update preference dependencies based on current state
     */
    private void updatePreferenceDependencies() {
        Context context = getSafeContext();
        if (context == null) return;
        
        ContentResolver resolver = context.getContentResolver();
        boolean peekEnabled = Settings.Secure.getInt(resolver, 
            KEY_PEEK_DISPLAY_NOTIFICATIONS, 0) == 1;
        boolean alwaysVisible = Settings.Secure.getInt(resolver, 
            KEY_PEEK_DISPLAY_ALWAYS_VISIBLE, 0) == 1;
        
        // Update dependent preferences
        if (mPeekDisplayStyle != null) {
            mPeekDisplayStyle.setEnabled(peekEnabled);
        }
        if (mPeekDisplayLocation != null) {
            mPeekDisplayLocation.setEnabled(peekEnabled);
        }
        if (mVerticalOffset != null) {
            mVerticalOffset.setEnabled(peekEnabled);
        }
        if (mAlwaysVisible != null) {
            mAlwaysVisible.setEnabled(peekEnabled);
        }
        if (mTimeout != null) {
            // Timeout is disabled when always visible is enabled
            mTimeout.setEnabled(peekEnabled && !alwaysVisible);
            if (alwaysVisible) {
                mTimeout.setSummary(getString(R.string.peek_display_timeout_summary) + " (disabled - always visible enabled)");
            } else {
                mTimeout.setSummary(getString(R.string.peek_display_timeout_summary));
            }
        }
    }
    
    /**
     * Enable close button functionality for peek display
     * This is the key method to fix the close button issue and configure timeout behavior
     */
    private void enableCloseButtonFunctionality() {
        Context context = getSafeContext();
        if (context == null) return;
        
        ContentResolver resolver = context.getContentResolver();
        
        // Check if always visible is enabled
        boolean alwaysVisible = Settings.Secure.getInt(resolver, 
            KEY_PEEK_DISPLAY_ALWAYS_VISIBLE, 0) == 1;
        
        // Enable dismissible peek display notifications (unless always visible)
        Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_DISMISSIBLE, alwaysVisible ? 0 : 1);
        
        // Enable close button functionality
        Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_CLOSE_ENABLED, 1);
        
        // Set timeout behavior
        if (!alwaysVisible) {
            // Get timeout in seconds and convert to milliseconds for SystemUI
            int timeoutSeconds = Settings.Secure.getInt(resolver, 
                KEY_PEEK_DISPLAY_TIMEOUT, DEFAULT_TIMEOUT_SECONDS);
            int timeoutMs = timeoutSeconds * 1000;
            
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, timeoutMs);
            
            // Notify SystemUI of timeout changes
            resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_TIMEOUT), null);
        } else {
            // When always visible, disable timeout (set to 0 or very high value)
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, 0);
            resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_TIMEOUT), null);
        }
        
        // Notify SystemUI of the changes
        resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_DISMISSIBLE), null);
        resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_CLOSE_ENABLED), null);
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = getSafeContext();
        if (context == null) return false;
        
        String key = preference.getKey();
        ContentResolver resolver = context.getContentResolver();
        
        if (KEY_PEEK_DISPLAY_NOTIFICATIONS.equals(key)) {
            boolean enabled = (Boolean) newValue;
            
            // Update SystemUI settings for peek display
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_ENABLED, enabled ? 1 : 0);
            
            // Ensure close button functionality is maintained when peek display is enabled
            if (enabled) {
                postDelayedSafe(() -> enableCloseButtonFunctionality(), 100);
            }
            
            // Update dependencies immediately
            postDelayedSafe(() -> updatePreferenceDependencies(), 50);
            return true;
            
        } else if (KEY_PEEK_DISPLAY_VERTICAL_OFFSET.equals(key)) {
            return handleVerticalOffsetChange((Integer) newValue);
            
        } else if (KEY_PEEK_DISPLAY_LOCATION.equals(key)) {
            // Reset offset when location changes to prevent position conflicts
            postDelayedSafe(() -> {
                if (mVerticalOffset != null) {
                    mVerticalOffset.setValue(DEFAULT_VERTICAL_OFFSET);
                    mLastValidOffset = DEFAULT_VERTICAL_OFFSET;
                }
            }, POSITION_UPDATE_DELAY);
            return true;
            
        } else if (KEY_PEEK_DISPLAY_ALWAYS_VISIBLE.equals(key)) {
            boolean alwaysVisible = (Boolean) newValue;
            
            // Update dependencies when always visible changes
            postDelayedSafe(() -> updatePreferenceDependencies(), 50);
            
            // Apply the always visible setting
            Settings.Secure.putInt(resolver, KEY_PEEK_DISPLAY_ALWAYS_VISIBLE, alwaysVisible ? 1 : 0);
            
            // When always visible is disabled, ensure close button works
            if (!alwaysVisible) {
                postDelayedSafe(() -> enableCloseButtonFunctionality(), 100);
            }
            
            // Force refresh of the system UI to apply changes
            postDelayedSafe(() -> {
                resolver.notifyChange(Settings.Secure.getUriFor(KEY_PEEK_DISPLAY_ALWAYS_VISIBLE), null);
            }, 50);
            
            return true;
            
        } else if (KEY_PEEK_DISPLAY_TIMEOUT.equals(key)) {
            int timeoutSeconds = (Integer) newValue;
            // Validate timeout range (1-10 seconds)
            if (timeoutSeconds < 1) timeoutSeconds = 1;
            if (timeoutSeconds > 10) timeoutSeconds = 10;
            
            // Convert to milliseconds for SystemUI
            int timeoutMs = timeoutSeconds * 1000;
            
            // Update SystemUI timeout setting
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, timeoutMs);
            
            // Notify SystemUI of timeout changes
            postDelayedSafe(() -> {
                resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_TIMEOUT), null);
                // Ensure close button and timeout behavior work together
                enableCloseButtonFunctionality();
            }, 100);
            
            return true;
        }
        
        return true;
    }
    
    /**
     * Handle vertical offset changes with stability measures
     */
    private boolean handleVerticalOffsetChange(int newOffset) {
        if (mIsUpdatingPosition) {
            return false; // Prevent recursive updates
        }
        
        Context context = getSafeContext();
        if (context == null) return false;
        
        // Validate offset range
        if (newOffset < 0) newOffset = 0;
        if (newOffset > 100) newOffset = 100;
        
        // Make final copy for lambda
        final int finalOffset = newOffset;
        
        // Check for rapid changes (potential instability)
        if (Math.abs(finalOffset - mLastValidOffset) > 20) {
            // Large jump detected - use delayed update for stability
            mIsUpdatingPosition = true;
            
            mPositionHandler.removeCallbacksAndMessages(null);
            mPositionHandler.postDelayed(() -> {
                applyStableVerticalOffset(finalOffset);
                mIsUpdatingPosition = false;
            }, POSITION_UPDATE_DELAY);
            
            return false; // Don't apply immediately
        } else {
            // Small change - apply immediately
            return applyStableVerticalOffset(finalOffset);
        }
    }
    
    /**
     * Apply vertical offset with stability checks
     */
    private boolean applyStableVerticalOffset(int offset) {
        Context context = getSafeContext();
        if (context == null) return false;
        
        ContentResolver resolver = context.getContentResolver();
        
        // Cache the position for stability
        mLastValidOffset = offset;
        
        // Apply the setting
        Settings.Secure.putInt(resolver, KEY_PEEK_DISPLAY_VERTICAL_OFFSET, offset);
        
        // Force refresh of the system UI to apply changes
        postDelayedSafe(() -> {
            // Notify system of the change
            resolver.notifyChange(Settings.Secure.getUriFor(KEY_PEEK_DISPLAY_VERTICAL_OFFSET), null);
        }, 50);
        
        return true;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Restore cached position on resume to prevent drift
        if (mVerticalOffset != null) {
            Context context = getSafeContext();
            if (context != null) {
                ContentResolver resolver = context.getContentResolver();
                int currentOffset = Settings.Secure.getInt(resolver, 
                    KEY_PEEK_DISPLAY_VERTICAL_OFFSET, DEFAULT_VERTICAL_OFFSET);
                
                // Check if position has drifted
                if (Math.abs(currentOffset - mLastValidOffset) > 5) {
                    // Position has drifted - restore stable position
                    postDelayedSafe(() -> {
                        Settings.Secure.putInt(resolver, KEY_PEEK_DISPLAY_VERTICAL_OFFSET, mLastValidOffset);
                        if (mVerticalOffset != null) {
                            mVerticalOffset.setValue(mLastValidOffset);
                        }
                    }, 100);
                } else {
                    mLastValidOffset = currentOffset;
                }
            }
        }
        
        updatePreferenceDependencies();
        
        // Ensure close button functionality is always enabled on resume
        enableCloseButtonFunctionality();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up position handler
        if (mPositionHandler != null) {
            mPositionHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_peek_display) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}