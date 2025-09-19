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
package com.rising.settings.fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.android.OmniJawsClient;
import com.android.internal.util.android.Utils;

import com.android.settings.R;
import com.rising.settings.fragments.OptimizedSettingsFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

import lineageos.providers.LineageSettings;

import com.android.settings.preferences.ui.PreferenceUtils;

import com.android.settings.utils.SystemRestartUtils;

@SearchIndexable
public class LockScreen extends OptimizedSettingsFragment
            implements Preference.OnPreferenceChangeListener  {

    public static final String TAG = "LockScreen";

    private static final String LOCKSCREEN_INTERFACE_CATEGORY = "lockscreen_interface_category";
    private static final String LOCKSCREEN_GESTURES_CATEGORY = "lockscreen_gestures_category";
    private static final String LOCKSCREEN_FP_CATEGORY = "lockscreen_fp_category";
    private static final String LOCKSCREEN_UDFPS_CATEGORY = "lockscreen_udfps_category";
    private static final String KEY_RIPPLE_EFFECT = "enable_ripple_effect";
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";
    private static final String KEY_UDFPS_ANIMATIONS = "udfps_recognizing_animation_preview";
    private static final String KEY_UDFPS_ICONS = "udfps_icon_picker";
    private static final String SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled";
    private static final String KEY_FP_SUCCESS = "fp_success_vibrate";
    private static final String KEY_FP_FAIL = "fp_error_vibrate";
    
    // Now Bar settings keys
    private static final String KEY_NOW_BAR_ENABLED = "keyguard_now_bar_enabled";
    private static final String KEY_NOW_BAR_MARGIN_BOTTOM = "nowbar_margin_bottom";
    private static final int DEFAULT_NOW_BAR_MARGIN = 18;

    private Preference mUdfpsIcons;
    private Preference mUdfpsAnimation;
    private Preference mRippleEffect;
    private Preference mWeather;
    private Preference mScreenOffUdfps;
    private Preference mFpSuccess;
    private Preference mFpFail;
    
    // Now Bar preferences and position management
    private Preference mNowBarEnabled;
    private com.android.settings.preferences.SystemSettingSeekBarPreference mNowBarMargin;
    private int mLastValidNowBarMargin = DEFAULT_NOW_BAR_MARGIN;
    
    private OmniJawsClient mWeatherClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_lockscreen);

        PreferenceCategory udfpsCategory = (PreferenceCategory) findPreference(LOCKSCREEN_UDFPS_CATEGORY);
        PreferenceCategory fpCategory = (PreferenceCategory) findPreference(LOCKSCREEN_FP_CATEGORY);

        // Use safe context access and cached preferences
        Context context = getSafeContext();
        FingerprintManager mFingerprintManager = context != null ? 
                (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE) : null;
        
        mUdfpsIcons = findCachedPreference(KEY_UDFPS_ICONS);
        mUdfpsAnimation = findCachedPreference(KEY_UDFPS_ANIMATIONS);
        mRippleEffect = findCachedPreference(KEY_RIPPLE_EFFECT);
        mScreenOffUdfps = findCachedPreference(SCREEN_OFF_UDFPS_ENABLED);
        mFpSuccess = findCachedPreference(KEY_FP_SUCCESS);
        mFpFail = findCachedPreference(KEY_FP_FAIL);
        
        // Initialize Now Bar preferences
        mNowBarEnabled = findCachedPreference(KEY_NOW_BAR_ENABLED);
        mNowBarMargin = findCachedPreference(KEY_NOW_BAR_MARGIN_BOTTOM);
        
        // Initialize Now Bar position management
        initializeNowBarPosition();

        if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
            if (udfpsCategory != null) {
                if (mUdfpsAnimation != null) udfpsCategory.removePreference(mUdfpsAnimation);
                if (mUdfpsIcons != null) udfpsCategory.removePreference(mUdfpsIcons);
                if (mScreenOffUdfps != null) udfpsCategory.removePreference(mScreenOffUdfps);
            }
            if (fpCategory != null) {
                if (mRippleEffect != null) fpCategory.removePreference(mRippleEffect);
                if (mFpSuccess != null) fpCategory.removePreference(mFpSuccess);
                if (mFpFail != null) fpCategory.removePreference(mFpFail);
            }
        } else {
            // Cache package installation checks to avoid repeated calls
            final boolean udfpsAnimationInstalled = isOperationCached("udfps_anim_installed") ? 
                    (Boolean) getCachedOperation("udfps_anim_installed") : 
                    Utils.isPackageInstalled(context, "com.crdroid.udfps.animations");
            if (!isOperationCached("udfps_anim_installed")) {
                cacheOperation("udfps_anim_installed", udfpsAnimationInstalled);
            }
            
            final boolean udfpsIconsInstalled = isOperationCached("udfps_icons_installed") ? 
                    (Boolean) getCachedOperation("udfps_icons_installed") : 
                    Utils.isPackageInstalled(context, "com.crdroid.udfps.icons");
            if (!isOperationCached("udfps_icons_installed")) {
                cacheOperation("udfps_icons_installed", udfpsIconsInstalled);
            }
            if (!udfpsAnimationInstalled && udfpsCategory != null && mUdfpsAnimation != null) {
                udfpsCategory.removePreference(mUdfpsAnimation);
            }
            if (!udfpsIconsInstalled && udfpsCategory != null && mUdfpsIcons != null) {
                udfpsCategory.removePreference(mUdfpsIcons);
            }
            if (!udfpsAnimationInstalled && !udfpsIconsInstalled && udfpsCategory != null && mScreenOffUdfps != null) {
                udfpsCategory.removePreference(mScreenOffUdfps);
            }
        }

        mWeather = findCachedPreference(KEY_WEATHER);
        if (mWeather != null) {
            mWeather.setOnPreferenceChangeListener(this);
        }
        
        // Set up Now Bar preference listeners
        if (mNowBarEnabled != null) {
            mNowBarEnabled.setOnPreferenceChangeListener(this);
        }
        if (mNowBarMargin != null) {
            mNowBarMargin.setOnPreferenceChangeListener(this);
        }
        
        // Initialize weather client with null check
        if (context != null) {
            mWeatherClient = new OmniJawsClient(context);
            updateWeatherSettings();
        }
        
        PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            PreferenceUtils.hideEmptyCategory(udfpsCategory, screen);
            PreferenceUtils.hideEmptyCategory(fpCategory, screen);
            
            com.android.settingslib.widget.LayoutPreference lockHighlightPref = screen.findPreference("lockscreen_highlight_dashboard");
            if (lockHighlightPref != null && context != null) {
                java.util.Map<Integer, String> lockHighlightClickMap = new java.util.HashMap<>();
                lockHighlightClickMap.put(R.id.lockscreen_widgets_tile, "PersonalizationsWidgetsActivity");
                lockHighlightClickMap.put(R.id.peek_display_tile, "PersonalizationsPDActivity");
                lockHighlightClickMap.put(R.id.aod_tile, "PersonalizationsAODActivity");
                lockHighlightClickMap.put(R.id.dw_tile, "PersonalizationsDWActivity");
                com.android.settings.utils.HighlightPrefUtils.Companion.setupHighlightPref(context, lockHighlightPref, lockHighlightClickMap);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        
        if (KEY_WEATHER.equals(key)) {
            // Restart SystemUI when weather preference changes
            Context context = getSafeContext();
            if (context != null) {
                SystemRestartUtils.restartSystemUI(context);
            }
            return true;
        } else if (KEY_NOW_BAR_ENABLED.equals(key)) {
            // Handle Now Bar enable/disable
            boolean enabled = (Boolean) newValue;
            handleNowBarEnabledChange(enabled);
            return true;
        } else if (KEY_NOW_BAR_MARGIN_BOTTOM.equals(key)) {
            // Handle Now Bar margin changes with position stability
            int newMargin = (Integer) newValue;
            return handleNowBarMarginChange(newMargin);
        }
        
        return false;
    }

    private void updateWeatherSettings() {
        if (mWeatherClient == null || mWeather == null) return;

        boolean weatherEnabled = mWeatherClient.isOmniJawsEnabled();
        mWeather.setEnabled(weatherEnabled);
        mWeather.setSummary(weatherEnabled ? R.string.lockscreen_weather_summary :
            R.string.lockscreen_weather_enabled_info);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Use safe method to update weather settings
        if (isFragmentReady()) {
            updateWeatherSettings();
            // Check and restore Now Bar position to prevent drift
            checkAndRestoreNowBarPosition();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup weather client
        mWeatherClient = null;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }
    
    /**
     * Initialize Now Bar position management
     */
    private void initializeNowBarPosition() {
        Context context = getSafeContext();
        if (context == null || mNowBarMargin == null) return;
        
        ContentResolver resolver = context.getContentResolver();
        
        // Load cached position
        int currentMargin = Settings.System.getInt(resolver, 
            KEY_NOW_BAR_MARGIN_BOTTOM, DEFAULT_NOW_BAR_MARGIN);
        mLastValidNowBarMargin = currentMargin;
    }
    
    /**
     * Handle Now Bar enabled/disabled changes
     */
    private void handleNowBarEnabledChange(boolean enabled) {
        Context context = getSafeContext();
        if (context == null) return;
        
        // Restart SystemUI when Now Bar is enabled/disabled to apply changes
        postDelayedSafe(() -> {
            SystemRestartUtils.restartSystemUI(context);
        }, 100);
    }
    
    /**
     * Handle Now Bar margin changes with position stability
     */
    private boolean handleNowBarMarginChange(int newMargin) {
        Context context = getSafeContext();
        if (context == null) return false;
        
        // Validate margin range (0-210 as defined in XML)
        if (newMargin < 0) newMargin = 0;
        if (newMargin > 210) newMargin = 210;
        
        // Cache the new valid position
        mLastValidNowBarMargin = newMargin;
        
        // Apply the setting with stability
        ContentResolver resolver = context.getContentResolver();
        Settings.System.putInt(resolver, KEY_NOW_BAR_MARGIN_BOTTOM, newMargin);
        
        // Notify SystemUI of the change with a slight delay for stability
        postDelayedSafe(() -> {
            resolver.notifyChange(Settings.System.getUriFor(KEY_NOW_BAR_MARGIN_BOTTOM), null);
        }, 50);
        
        return true;
    }
    
    /**
     * Check and restore Now Bar position to prevent drift after idle periods
     * This is the key method to fix the position drift issue
     */
    private void checkAndRestoreNowBarPosition() {
        if (mNowBarMargin == null) return;
        
        Context context = getSafeContext();
        if (context == null) return;
        
        ContentResolver resolver = context.getContentResolver();
        
        // Check if Now Bar is enabled
        boolean nowBarEnabled = Settings.System.getInt(resolver, 
            KEY_NOW_BAR_ENABLED, 0) == 1;
        
        if (!nowBarEnabled) {
            return; // No need to check position if Now Bar is disabled
        }
        
        int currentMargin = Settings.System.getInt(resolver, 
            KEY_NOW_BAR_MARGIN_BOTTOM, DEFAULT_NOW_BAR_MARGIN);
        
        // Check if position has drifted (tolerance of 2 units)
        if (Math.abs(currentMargin - mLastValidNowBarMargin) > 2) {
            // Position has drifted - restore stable position
            postDelayedSafe(() -> {
                Settings.System.putInt(resolver, KEY_NOW_BAR_MARGIN_BOTTOM, mLastValidNowBarMargin);
                if (mNowBarMargin != null) {
                    mNowBarMargin.setValue(mLastValidNowBarMargin);
                }
                
                // Notify SystemUI of the position correction
                resolver.notifyChange(Settings.System.getUriFor(KEY_NOW_BAR_MARGIN_BOTTOM), null);
                
                android.util.Log.d(TAG, "Now Bar position drift detected and corrected: " + 
                    currentMargin + " -> " + mLastValidNowBarMargin);
            }, 100);
        } else {
            // Position is stable - update cache
            mLastValidNowBarMargin = currentMargin;
        }
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_lockscreen) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    FingerprintManager mFingerprintManager = (FingerprintManager)
                            context.getSystemService(Context.FINGERPRINT_SERVICE);
                    if (mFingerprintManager == null || !mFingerprintManager.isHardwareDetected()) {
                        keys.add(KEY_UDFPS_ANIMATIONS);
                        keys.add(KEY_UDFPS_ICONS);
                        keys.add(KEY_RIPPLE_EFFECT);
                        keys.add(SCREEN_OFF_UDFPS_ENABLED);
                    } else {
                        if (!Utils.isPackageInstalled(context, "com.crdroid.udfps.animations")) {
                            keys.add(KEY_UDFPS_ANIMATIONS);
                        }
                        if (!Utils.isPackageInstalled(context, "com.crdroid.udfps.icons")) {
                            keys.add(KEY_UDFPS_ICONS);
                        }
                        Resources resources = context.getResources();
                        boolean screenOffUdfpsAvailable = resources.getBoolean(
                            com.android.internal.R.bool.config_supportScreenOffUdfps) ||
                            !TextUtils.isEmpty(resources.getString(
                                com.android.internal.R.string.config_dozeUdfpsLongPressSensorType));
                        if (!screenOffUdfpsAvailable)
                            keys.add(SCREEN_OFF_UDFPS_ENABLED);
                        }
                    return keys;
                }
            };
}
