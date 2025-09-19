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
package com.rising.settings.fragments

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.internal.logging.nano.MetricsProto
import com.android.internal.util.android.OmniJawsClient
import com.android.internal.util.android.Utils
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settings.preferences.ui.PreferenceUtils
import com.android.settings.utils.SystemRestartUtils
import kotlin.math.abs

@SearchIndexable
class LockScreen : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "LockScreen"

        private const val LOCKSCREEN_INTERFACE_CATEGORY = "lockscreen_interface_category"
        private const val LOCKSCREEN_GESTURES_CATEGORY = "lockscreen_gestures_category"
        private const val LOCKSCREEN_FP_CATEGORY = "lockscreen_fp_category"
        private const val LOCKSCREEN_UDFPS_CATEGORY = "lockscreen_udfps_category"
        private const val KEY_RIPPLE_EFFECT = "enable_ripple_effect"
        private const val KEY_WEATHER = "lockscreen_weather_enabled"
        private const val KEY_UDFPS_ANIMATIONS = "udfps_recognizing_animation_preview"
        private const val KEY_UDFPS_ICONS = "udfps_icon_picker"
        private const val SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled"
        private const val KEY_FP_SUCCESS = "fp_success_vibrate"
        private const val KEY_FP_FAIL = "fp_error_vibrate"
        
        // Now Bar settings keys
        private const val KEY_NOW_BAR_ENABLED = "keyguard_now_bar_enabled"
        private const val KEY_NOW_BAR_MARGIN_BOTTOM = "nowbar_margin_bottom"
        private const val DEFAULT_NOW_BAR_MARGIN = 18

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_lockscreen) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()

                val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
                if (fingerprintManager == null || !fingerprintManager.isHardwareDetected) {
                    keys.add(KEY_UDFPS_ANIMATIONS)
                    keys.add(KEY_UDFPS_ICONS)
                    keys.add(KEY_RIPPLE_EFFECT)
                    keys.add(SCREEN_OFF_UDFPS_ENABLED)
                } else {
                    if (!Utils.isPackageInstalled(context, "com.crdroid.udfps.animations")) {
                        keys.add(KEY_UDFPS_ANIMATIONS)
                    }
                    if (!Utils.isPackageInstalled(context, "com.crdroid.udfps.icons")) {
                        keys.add(KEY_UDFPS_ICONS)
                    }
                    val resources = context.resources
                    val screenOffUdfpsAvailable = resources.getBoolean(
                        com.android.internal.R.bool.config_supportScreenOffUdfps) ||
                        !TextUtils.isEmpty(resources.getString(
                            com.android.internal.R.string.config_dozeUdfpsLongPressSensorType))
                    if (!screenOffUdfpsAvailable) {
                        keys.add(SCREEN_OFF_UDFPS_ENABLED)
                    }
                }
                return keys
            }
        }
    }

    private var mUdfpsIcons: Preference? = null
    private var mUdfpsAnimation: Preference? = null
    private var mRippleEffect: Preference? = null
    private var mWeather: Preference? = null
    private var mScreenOffUdfps: Preference? = null
    private var mFpSuccess: Preference? = null
    private var mFpFail: Preference? = null
    
    // Now Bar preferences and position management
    private var mNowBarEnabled: Preference? = null
    private var mNowBarMargin: com.android.settings.preferences.SystemSettingSeekBarPreference? = null
    private var mLastValidNowBarMargin = DEFAULT_NOW_BAR_MARGIN
    
    private var mWeatherClient: OmniJawsClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_lockscreen)

        val udfpsCategory = findPreference<PreferenceCategory>(LOCKSCREEN_UDFPS_CATEGORY)
        val fpCategory = findPreference<PreferenceCategory>(LOCKSCREEN_FP_CATEGORY)

        // Use safe context access and cached preferences
        val context = getSafeContext()
        val fingerprintManager = context?.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
        
        mUdfpsIcons = findCachedPreference(KEY_UDFPS_ICONS)
        mUdfpsAnimation = findCachedPreference(KEY_UDFPS_ANIMATIONS)
        mRippleEffect = findCachedPreference(KEY_RIPPLE_EFFECT)
        mScreenOffUdfps = findCachedPreference(SCREEN_OFF_UDFPS_ENABLED)
        mFpSuccess = findCachedPreference(KEY_FP_SUCCESS)
        mFpFail = findCachedPreference(KEY_FP_FAIL)
        
        // Initialize Now Bar preferences
        mNowBarEnabled = findCachedPreference(KEY_NOW_BAR_ENABLED)
        mNowBarMargin = findCachedPreference(KEY_NOW_BAR_MARGIN_BOTTOM)
        
        // Initialize Now Bar position management
        initializeNowBarPosition()

        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected) {
            udfpsCategory?.let { category ->
                mUdfpsAnimation?.let { category.removePreference(it) }
                mUdfpsIcons?.let { category.removePreference(it) }
                mScreenOffUdfps?.let { category.removePreference(it) }
            }
            fpCategory?.let { category ->
                mRippleEffect?.let { category.removePreference(it) }
                mFpSuccess?.let { category.removePreference(it) }
                mFpFail?.let { category.removePreference(it) }
            }
        } else {
            // Cache package installation checks to avoid repeated calls
            val udfpsAnimationInstalled = if (isOperationCached("udfps_anim_installed")) {
                getCachedOperation<Boolean>("udfps_anim_installed") ?: false
            } else {
                val installed = Utils.isPackageInstalled(context, "com.crdroid.udfps.animations")
                cacheOperation("udfps_anim_installed", installed)
                installed
            }
            
            val udfpsIconsInstalled = if (isOperationCached("udfps_icons_installed")) {
                getCachedOperation<Boolean>("udfps_icons_installed") ?: false
            } else {
                val installed = Utils.isPackageInstalled(context, "com.crdroid.udfps.icons")
                cacheOperation("udfps_icons_installed", installed)
                installed
            }
            
            if (!udfpsAnimationInstalled) {
                udfpsCategory?.let { category ->
                    mUdfpsAnimation?.let { category.removePreference(it) }
                }
            }
            if (!udfpsIconsInstalled) {
                udfpsCategory?.let { category ->
                    mUdfpsIcons?.let { category.removePreference(it) }
                }
            }
            if (!udfpsAnimationInstalled && !udfpsIconsInstalled) {
                udfpsCategory?.let { category ->
                    mScreenOffUdfps?.let { category.removePreference(it) }
                }
            }
        }

        mWeather = findCachedPreference<Preference>(KEY_WEATHER)?.apply {
            onPreferenceChangeListener = this@LockScreen
        }
        
        // Set up Now Bar preference listeners
        mNowBarEnabled?.onPreferenceChangeListener = this
        mNowBarMargin?.onPreferenceChangeListener = this
        
        // Initialize weather client with null check
        context?.let {
            mWeatherClient = OmniJawsClient(it)
            updateWeatherSettings()
        }
        
        val screen = preferenceScreen
        screen?.let {
            PreferenceUtils.hideEmptyCategory(udfpsCategory, it)
            PreferenceUtils.hideEmptyCategory(fpCategory, it)
            
            val lockHighlightPref = it.findPreference<com.android.settingslib.widget.LayoutPreference>("lockscreen_highlight_dashboard")
            if (lockHighlightPref != null && context != null) {
                val lockHighlightClickMap = mapOf(
                    R.id.lockscreen_widgets_tile to "PersonalizationsWidgetsActivity",
                    R.id.peek_display_tile to "PersonalizationsPDActivity",
                    R.id.aod_tile to "PersonalizationsAODActivity",
                    R.id.dw_tile to "PersonalizationsDWActivity"
                )
                com.android.settings.utils.HighlightPrefUtils.setupHighlightPref(context, lockHighlightPref, lockHighlightClickMap)
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val key = preference.key
        
        return when (key) {
            KEY_WEATHER -> {
                // Restart SystemUI when weather preference changes
                getSafeContext()?.let { context ->
                    SystemRestartUtils.restartSystemUI(context)
                }
                true
            }
            KEY_NOW_BAR_ENABLED -> {
                // Handle Now Bar enable/disable
                val enabled = newValue as Boolean
                handleNowBarEnabledChange(enabled)
                true
            }
            KEY_NOW_BAR_MARGIN_BOTTOM -> {
                // Handle Now Bar margin changes with position stability
                val newMargin = newValue as Int
                handleNowBarMarginChange(newMargin)
            }
            else -> false
        }
    }

    private fun updateWeatherSettings() {
        val weatherClient = mWeatherClient
        val weather = mWeather
        if (weatherClient == null || weather == null) return

        val weatherEnabled = weatherClient.isOmniJawsEnabled
        weather.isEnabled = weatherEnabled
        weather.setSummary(if (weatherEnabled) R.string.lockscreen_weather_summary else R.string.lockscreen_weather_enabled_info)
    }

    override fun onResume() {
        super.onResume()
        // Use safe method to update weather settings
        if (isFragmentReady()) {
            updateWeatherSettings()
            // Check and restore Now Bar position to prevent drift
            checkAndRestoreNowBarPosition()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup weather client
        mWeatherClient = null
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
    
    /**
     * Initialize Now Bar position management
     */
    private fun initializeNowBarPosition() {
        val context = getSafeContext() ?: return
        val nowBarMargin = mNowBarMargin ?: return
        
        val resolver = context.contentResolver
        
        // Load cached position
        val currentMargin = Settings.System.getInt(resolver, 
            KEY_NOW_BAR_MARGIN_BOTTOM, DEFAULT_NOW_BAR_MARGIN)
        mLastValidNowBarMargin = currentMargin
    }
    
    /**
     * Handle Now Bar enabled/disabled changes
     */
    private fun handleNowBarEnabledChange(enabled: Boolean) {
        val context = getSafeContext() ?: return
        
        // Restart SystemUI when Now Bar is enabled/disabled to apply changes
        postDelayedSafe(100) {
            SystemRestartUtils.restartSystemUI(context)
        }
    }
    
    /**
     * Handle Now Bar margin changes with position stability
     */
    private fun handleNowBarMarginChange(newMargin: Int): Boolean {
        val context = getSafeContext() ?: return false
        
        // Validate margin range (0-210 as defined in XML)
        val validMargin = newMargin.coerceIn(0, 210)
        
        // Cache the new valid position
        mLastValidNowBarMargin = validMargin
        
        // Apply the setting with stability
        val resolver = context.contentResolver
        Settings.System.putInt(resolver, KEY_NOW_BAR_MARGIN_BOTTOM, validMargin)
        
        // Notify SystemUI of the change with a slight delay for stability
        postDelayedSafe(50) {
            resolver.notifyChange(Settings.System.getUriFor(KEY_NOW_BAR_MARGIN_BOTTOM), null)
        }
        
        return true
    }
    
    /**
     * Check and restore Now Bar position to prevent drift after idle periods
     * This is the key method to fix the position drift issue
     */
    private fun checkAndRestoreNowBarPosition() {
        val nowBarMargin = mNowBarMargin ?: return
        val context = getSafeContext() ?: return
        
        val resolver = context.contentResolver
        
        // Check if Now Bar is enabled
        val nowBarEnabled = Settings.System.getInt(resolver, 
            KEY_NOW_BAR_ENABLED, 0) == 1
        
        if (!nowBarEnabled) {
            return // No need to check position if Now Bar is disabled
        }
        
        val currentMargin = Settings.System.getInt(resolver, 
            KEY_NOW_BAR_MARGIN_BOTTOM, DEFAULT_NOW_BAR_MARGIN)
        
        // Check if position has drifted (tolerance of 2 units)
        if (abs(currentMargin - mLastValidNowBarMargin) > 2) {
            // Position has drifted - restore stable position
            postDelayedSafe(100) {
                Settings.System.putInt(resolver, KEY_NOW_BAR_MARGIN_BOTTOM, mLastValidNowBarMargin)
                nowBarMargin.setValue(mLastValidNowBarMargin)
                
                // Notify SystemUI of the position correction
                resolver.notifyChange(Settings.System.getUriFor(KEY_NOW_BAR_MARGIN_BOTTOM), null)
                
                Log.d(TAG, "Now Bar position drift detected and corrected: $currentMargin -> $mLastValidNowBarMargin")
            }
        } else {
            // Position is stable - update cache
            mLastValidNowBarMargin = currentMargin
        }
    }
}
