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
package com.rising.settings.fragments.lockscreen

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.preferences.SecureSettingSeekBarPreference
import com.android.settings.preferences.SecureSettingListPreference
import com.android.settings.preferences.SecureSettingSwitchPreference
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import kotlin.math.abs

@SearchIndexable
class PeekDisplay : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "PeekDisplay"
        
        // Preference keys
        private const val KEY_PEEK_DISPLAY_NOTIFICATIONS = "peek_display_notifications"
        private const val KEY_PEEK_DISPLAY_STYLE = "peek_display_style"
        private const val KEY_PEEK_DISPLAY_LOCATION = "peek_display_location"
        private const val KEY_PEEK_DISPLAY_VERTICAL_OFFSET = "peek_display_vertical_offset"
        private const val KEY_PEEK_DISPLAY_ALWAYS_VISIBLE = "peek_display_always_visible"
        private const val KEY_PEEK_DISPLAY_TIMEOUT = "peek_display_timeout"
        
        // Settings keys for SystemUI communication
        private const val SYSTEMUI_PEEK_DISPLAY_ENABLED = "peek_display_enabled"
        private const val SYSTEMUI_PEEK_DISPLAY_DISMISSIBLE = "peek_display_dismissible"
        private const val SYSTEMUI_PEEK_DISPLAY_CLOSE_ENABLED = "peek_display_close_enabled"
        private const val SYSTEMUI_PEEK_DISPLAY_TIMEOUT = "peek_display_timeout_ms"
        
        // Position stability constants
        private const val POSITION_UPDATE_DELAY = 100 // ms
        private const val DEFAULT_VERTICAL_OFFSET = 50 // percent
        private const val DEFAULT_TIMEOUT_SECONDS = 3 // seconds
        private const val DEFAULT_TIMEOUT_MS = DEFAULT_TIMEOUT_SECONDS * 1000 // ms for SystemUI

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_peek_display) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                return keys
            }
        }
    }
    
    // Preferences
    private var mPeekDisplayNotifications: SecureSettingSwitchPreference? = null
    private var mPeekDisplayStyle: SecureSettingListPreference? = null
    private var mPeekDisplayLocation: SecureSettingListPreference? = null
    private var mVerticalOffset: SecureSettingSeekBarPreference? = null
    private var mAlwaysVisible: SecureSettingSwitchPreference? = null
    private var mTimeout: SecureSettingSeekBarPreference? = null
    
    // Position management
    private var mPositionHandler: Handler? = null
    private var mLastValidOffset = DEFAULT_VERTICAL_OFFSET
    private var mIsUpdatingPosition = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_peek_display)
        
        // Initialize position handler
        mPositionHandler = Handler(Looper.getMainLooper())
        
        initializePreferences()
        updatePreferenceDependencies()
        
        // Ensure close button functionality is enabled
        enableCloseButtonFunctionality()
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    /**
     * Initialize all preferences and set up listeners
     */
    private fun initializePreferences() {
        val context = getSafeContext() ?: return
        val resolver = context.contentResolver
        
        // Get preferences
        mPeekDisplayNotifications = findCachedPreference(KEY_PEEK_DISPLAY_NOTIFICATIONS)
        mPeekDisplayStyle = findCachedPreference(KEY_PEEK_DISPLAY_STYLE)
        mPeekDisplayLocation = findCachedPreference(KEY_PEEK_DISPLAY_LOCATION)
        mVerticalOffset = findCachedPreference(KEY_PEEK_DISPLAY_VERTICAL_OFFSET)
        mAlwaysVisible = findCachedPreference(KEY_PEEK_DISPLAY_ALWAYS_VISIBLE)
        mTimeout = findCachedPreference(KEY_PEEK_DISPLAY_TIMEOUT)
        
        // Set up listeners
        mPeekDisplayNotifications?.onPreferenceChangeListener = this
        mPeekDisplayLocation?.onPreferenceChangeListener = this
        mVerticalOffset?.let { preference ->
            preference.onPreferenceChangeListener = this
            // Load cached position
            val currentOffset = Settings.Secure.getInt(resolver, 
                KEY_PEEK_DISPLAY_VERTICAL_OFFSET, DEFAULT_VERTICAL_OFFSET)
            mLastValidOffset = currentOffset
        }
        mAlwaysVisible?.onPreferenceChangeListener = this
        mTimeout?.let { preference ->
            preference.onPreferenceChangeListener = this
            // Initialize timeout setting for SystemUI
            val timeoutSeconds = Settings.Secure.getInt(resolver, 
                KEY_PEEK_DISPLAY_TIMEOUT, DEFAULT_TIMEOUT_SECONDS)
            val timeoutMs = timeoutSeconds * 1000
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, timeoutMs)
        }
    }
    
    /**
     * Update preference dependencies based on current state
     */
    private fun updatePreferenceDependencies() {
        val context = getSafeContext() ?: return
        val resolver = context.contentResolver
        
        val peekEnabled = Settings.Secure.getInt(resolver, 
            KEY_PEEK_DISPLAY_NOTIFICATIONS, 0) == 1
        val alwaysVisible = Settings.Secure.getInt(resolver, 
            KEY_PEEK_DISPLAY_ALWAYS_VISIBLE, 0) == 1
        
        // Update dependent preferences
        mPeekDisplayStyle?.isEnabled = peekEnabled
        mPeekDisplayLocation?.isEnabled = peekEnabled
        mVerticalOffset?.isEnabled = peekEnabled
        mAlwaysVisible?.isEnabled = peekEnabled
        
        mTimeout?.let { timeout ->
            // Timeout is disabled when always visible is enabled
            timeout.isEnabled = peekEnabled && !alwaysVisible
            timeout.summary = if (alwaysVisible) {
                "${getString(R.string.peek_display_timeout_summary)} (disabled - always visible enabled)"
            } else {
                getString(R.string.peek_display_timeout_summary)
            }
        }
    }
    
    /**
     * Enable close button functionality for peek display
     * This is the key method to fix the close button issue and configure timeout behavior
     */
    private fun enableCloseButtonFunctionality() {
        val context = getSafeContext() ?: return
        val resolver = context.contentResolver
        
        // Check if always visible is enabled
        val alwaysVisible = Settings.Secure.getInt(resolver, 
            KEY_PEEK_DISPLAY_ALWAYS_VISIBLE, 0) == 1
        
        // Enable dismissible peek display notifications (unless always visible)
        Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_DISMISSIBLE, if (alwaysVisible) 0 else 1)
        
        // Enable close button functionality
        Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_CLOSE_ENABLED, 1)
        
        // Set timeout behavior
        if (!alwaysVisible) {
            // Get timeout in seconds and convert to milliseconds for SystemUI
            val timeoutSeconds = Settings.Secure.getInt(resolver, 
                KEY_PEEK_DISPLAY_TIMEOUT, DEFAULT_TIMEOUT_SECONDS)
            val timeoutMs = timeoutSeconds * 1000
            
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, timeoutMs)
            
            // Notify SystemUI of timeout changes
            resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_TIMEOUT), null)
        } else {
            // When always visible, disable timeout (set to 0 or very high value)
            Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, 0)
            resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_TIMEOUT), null)
        }
        
        // Notify SystemUI of the changes
        resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_DISMISSIBLE), null)
        resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_CLOSE_ENABLED), null)
    }
    
    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val context = getSafeContext() ?: return false
        val key = preference.key
        val resolver = context.contentResolver
        
        return when (key) {
            KEY_PEEK_DISPLAY_NOTIFICATIONS -> {
                val enabled = newValue as Boolean
                
                // Update SystemUI settings for peek display
                Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_ENABLED, if (enabled) 1 else 0)
                
                // Ensure close button functionality is maintained when peek display is enabled
                if (enabled) {
                    postDelayedSafe(100) { enableCloseButtonFunctionality() }
                }
                
                // Update dependencies immediately
                postDelayedSafe(50) { updatePreferenceDependencies() }
                true
            }
            
            KEY_PEEK_DISPLAY_VERTICAL_OFFSET -> {
                handleVerticalOffsetChange(newValue as Int)
            }
            
            KEY_PEEK_DISPLAY_LOCATION -> {
                // Reset offset when location changes to prevent position conflicts
                postDelayedSafe(POSITION_UPDATE_DELAY.toLong()) {
                    mVerticalOffset?.let { offset ->
                        offset.value = DEFAULT_VERTICAL_OFFSET
                        mLastValidOffset = DEFAULT_VERTICAL_OFFSET
                    }
                }
                true
            }
            
            KEY_PEEK_DISPLAY_ALWAYS_VISIBLE -> {
                val alwaysVisible = newValue as Boolean
                
                // Update dependencies when always visible changes
                postDelayedSafe(50) { updatePreferenceDependencies() }
                
                // Apply the always visible setting
                Settings.Secure.putInt(resolver, KEY_PEEK_DISPLAY_ALWAYS_VISIBLE, if (alwaysVisible) 1 else 0)
                
                // When always visible is disabled, ensure close button works
                if (!alwaysVisible) {
                    postDelayedSafe(100) { enableCloseButtonFunctionality() }
                }
                
                // Force refresh of the system UI to apply changes
                postDelayedSafe(50) {
                    resolver.notifyChange(Settings.Secure.getUriFor(KEY_PEEK_DISPLAY_ALWAYS_VISIBLE), null)
                }
                
                true
            }
            
            KEY_PEEK_DISPLAY_TIMEOUT -> {
                var timeoutSeconds = newValue as Int
                // Validate timeout range (1-10 seconds)
                timeoutSeconds = timeoutSeconds.coerceIn(1, 10)
                
                // Convert to milliseconds for SystemUI
                val timeoutMs = timeoutSeconds * 1000
                
                // Update SystemUI timeout setting
                Settings.Secure.putInt(resolver, SYSTEMUI_PEEK_DISPLAY_TIMEOUT, timeoutMs)
                
                // Notify SystemUI of timeout changes
                postDelayedSafe(100) {
                    resolver.notifyChange(Settings.Secure.getUriFor(SYSTEMUI_PEEK_DISPLAY_TIMEOUT), null)
                    // Ensure close button and timeout behavior work together
                    enableCloseButtonFunctionality()
                }
                
                true
            }
            
            else -> true
        }
    }
    
    /**
     * Handle vertical offset changes with stability measures
     */
    private fun handleVerticalOffsetChange(newOffset: Int): Boolean {
        if (mIsUpdatingPosition) {
            return false // Prevent recursive updates
        }
        
        val context = getSafeContext() ?: return false
        
        // Validate offset range
        val validOffset = newOffset.coerceIn(0, 100)
        
        // Check for rapid changes (potential instability)
        return if (abs(validOffset - mLastValidOffset) > 20) {
            // Large jump detected - use delayed update for stability
            mIsUpdatingPosition = true
            
            mPositionHandler?.removeCallbacksAndMessages(null)
            mPositionHandler?.postDelayed({
                applyStableVerticalOffset(validOffset)
                mIsUpdatingPosition = false
            }, POSITION_UPDATE_DELAY.toLong())
            
            false // Don't apply immediately
        } else {
            // Small change - apply immediately
            applyStableVerticalOffset(validOffset)
        }
    }
    
    /**
     * Apply vertical offset with stability checks
     */
    private fun applyStableVerticalOffset(offset: Int): Boolean {
        val context = getSafeContext() ?: return false
        val resolver = context.contentResolver
        
        // Cache the position for stability
        mLastValidOffset = offset
        
        // Apply the setting
        Settings.Secure.putInt(resolver, KEY_PEEK_DISPLAY_VERTICAL_OFFSET, offset)
        
        // Force refresh of the system UI to apply changes
        postDelayedSafe(50) {
            // Notify system of the change
            resolver.notifyChange(Settings.Secure.getUriFor(KEY_PEEK_DISPLAY_VERTICAL_OFFSET), null)
        }
        
        return true
    }
    
    override fun onResume() {
        super.onResume()
        
        // Restore cached position on resume to prevent drift
        mVerticalOffset?.let { verticalOffset ->
            val context = getSafeContext()
            if (context != null) {
                val resolver = context.contentResolver
                val currentOffset = Settings.Secure.getInt(resolver, 
                    KEY_PEEK_DISPLAY_VERTICAL_OFFSET, DEFAULT_VERTICAL_OFFSET)
                
                // Check if position has drifted
                if (abs(currentOffset - mLastValidOffset) > 5) {
                    // Position has drifted - restore stable position
                    postDelayedSafe(100) {
                        Settings.Secure.putInt(resolver, KEY_PEEK_DISPLAY_VERTICAL_OFFSET, mLastValidOffset)
                        verticalOffset.value = mLastValidOffset
                    }
                } else {
                    mLastValidOffset = currentOffset
                }
            }
        }
        
        updatePreferenceDependencies()
        
        // Ensure close button functionality is always enabled on resume
        enableCloseButtonFunctionality()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up position handler
        mPositionHandler?.removeCallbacksAndMessages(null)
    }
}
