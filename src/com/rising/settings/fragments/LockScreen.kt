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
import com.android.internal.util.android.Utils
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settings.preferences.ui.PreferenceUtils
import kotlin.math.abs

@SearchIndexable
class LockScreen : OptimizedSettingsFragment() {

    companion object {
        const val TAG = "LockScreen"

        private const val LOCKSCREEN_INTERFACE_CATEGORY = "lockscreen_interface_category"
        private const val LOCKSCREEN_GESTURES_CATEGORY = "lockscreen_gestures_category"
        private const val LOCKSCREEN_FP_CATEGORY = "lockscreen_fp_category"
        private const val LOCKSCREEN_UDFPS_CATEGORY = "lockscreen_udfps_category"
        private const val KEY_RIPPLE_EFFECT = "enable_ripple_effect"
        private const val KEY_UDFPS_ANIMATIONS = "udfps_recognizing_animation_preview"
        private const val KEY_UDFPS_ICONS = "udfps_icon_picker"
        private const val SCREEN_OFF_UDFPS_ENABLED = "screen_off_udfps_enabled"
        private const val KEY_FP_SUCCESS = "fp_success_vibrate"
        private const val KEY_FP_FAIL = "fp_error_vibrate"

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
    private var mScreenOffUdfps: Preference? = null
    private var mFpSuccess: Preference? = null
    private var mFpFail: Preference? = null
    
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

        val screen = preferenceScreen
        screen?.let {
            PreferenceUtils.hideEmptyCategory(udfpsCategory, it)
            PreferenceUtils.hideEmptyCategory(fpCategory, it)
            
            val lockHighlightPref = it.findPreference<com.android.settingslib.widget.LayoutPreference>("lockscreen_highlight_dashboard")
            if (lockHighlightPref != null && context != null) {
                val lockHighlightClickMap = mapOf(
                    R.id.lockscreen_widgets_tile to "PersonalizationsWidgetsActivity",
                    R.id.peek_display_tile to "com.rising.settings.fragments.lockscreen.LockscreenCustomizerActivity",
                    R.id.aod_tile to "PersonalizationsAODActivity",
                    R.id.dw_tile to "PersonalizationsDWActivity"
                )
                com.android.settings.utils.HighlightPrefUtils.setupHighlightPref(context, lockHighlightPref, lockHighlightClickMap)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
