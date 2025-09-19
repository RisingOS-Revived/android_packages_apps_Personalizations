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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.internal.util.android.ThemeUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.preferences.CustomSeekBarPreference
import com.android.settings.preferences.SecureSettingSwitchPreference
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.SystemRestartUtils
import com.android.settingslib.search.SearchIndexable
import java.lang.ref.WeakReference

@SearchIndexable
class QuickSettings : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "QuickSettings"
        
        private const val KEY_QS_UI_STYLE = "qs_tile_ui_style"
        private const val KEY_QS_PANEL_STYLE = "qs_panel_style"
        private const val KEY_PREF_TILE_ANIM_STYLE = "qs_tile_animation_style"
        private const val KEY_PREF_TILE_ANIM_DURATION = "qs_tile_animation_duration"
        private const val KEY_PREF_TILE_ANIM_INTERPOLATOR = "qs_tile_animation_interpolator"
        private const val KEY_QS_REFACTOR_ENABLED = "qs_refactor_enabled"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_qs) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                return keys
            }
        }
    }

    private var mQsUI: ListPreference? = null
    private var mQsPanelStyle: ListPreference? = null
    private var mTileAnimationStyle: ListPreference? = null
    private var mTileAnimationInterpolator: ListPreference? = null
    private var mTileAnimationDuration: CustomSeekBarPreference? = null
    private var mSplitShadePref: Preference? = null
    private var mQsRefactorEnabled: SecureSettingSwitchPreference? = null
    
    private var mThemeUtils: ThemeUtils? = null
    
    // Use WeakReference to prevent memory leaks
    private class SafeHandler(fragment: QuickSettings) : Handler(Looper.getMainLooper()) {
        private val mFragmentRef = WeakReference(fragment)
        
        override fun handleMessage(msg: android.os.Message) {
            val fragment = mFragmentRef.get()
            if (fragment != null && fragment.isAdded) {
                super.handleMessage(msg)
            }
        }
    }
    
    private var mHandler: SafeHandler? = null
    
    // Cache for overlay operations to prevent redundant calls
    private var mLastQsUIStyle: String? = null
    private var mLastQsPanelStyle: String? = null
    private var mLastSplitShadeEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_qs)
        
        val mContext = activity?.applicationContext
        
        // Initialize handler with weak reference
        mHandler = SafeHandler(this)
        
        activity?.let {
            mThemeUtils = ThemeUtils.getInstance(it)
        }
        
        mQsUI = findPreference<ListPreference>(KEY_QS_UI_STYLE)?.apply {
            onPreferenceChangeListener = this@QuickSettings
        }

        mQsPanelStyle = findPreference<ListPreference>(KEY_QS_PANEL_STYLE)?.apply {
            onPreferenceChangeListener = this@QuickSettings
        }

        mSplitShadePref = findPreference<Preference>("qs_split_shade_enabled")?.apply {
            onPreferenceChangeListener = this@QuickSettings
        }

        mQsRefactorEnabled = findPreference<SecureSettingSwitchPreference>(KEY_QS_REFACTOR_ENABLED)?.apply {
            onPreferenceChangeListener = this@QuickSettings
        }

        mTileAnimationStyle = findPreference<ListPreference>(KEY_PREF_TILE_ANIM_STYLE)
        mTileAnimationDuration = findPreference(KEY_PREF_TILE_ANIM_DURATION)
        mTileAnimationInterpolator = findPreference(KEY_PREF_TILE_ANIM_INTERPOLATOR)

        mTileAnimationStyle?.onPreferenceChangeListener = this

        val tileAnimationStyle = Settings.System.getIntForUser(activity?.contentResolver,
                KEY_PREF_TILE_ANIM_STYLE, 0, UserHandle.USER_CURRENT)
        updateAnimTileStyle(tileAnimationStyle)

        mContext?.let { checkQSOverlays(it) }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val resolver = activity?.contentResolver ?: return false

        return when (preference) {
            mQsUI -> {
                val value = (newValue as String).toInt()
                Settings.System.putIntForUser(resolver,
                        Settings.System.QS_TILE_UI_STYLE, value, UserHandle.USER_CURRENT)
                activity?.let { updateQsStyle(it) }
                activity?.let { checkQSOverlays(it) }
                true
            }
            
            mQsPanelStyle -> {
                val value = (newValue as String).toInt()
                Settings.System.putIntForUser(resolver,
                        Settings.System.QS_PANEL_STYLE, value, UserHandle.USER_CURRENT)
                activity?.let { updateQsPanelStyle(it) }
                activity?.let { checkQSOverlays(it) }
                true
            }
            
            mTileAnimationStyle -> {
                val value = (newValue as String).toInt()
                updateAnimTileStyle(value)
                true
            }
            
            mSplitShadePref -> {
                val value = if (newValue as Boolean) 1 else 0
                Settings.System.putIntForUser(resolver,
                       "qs_split_shade_enabled", value, UserHandle.USER_CURRENT)
                activity?.let { updateSplitShadeEnabled(it) }
                true
            }
            
            mQsRefactorEnabled -> {
                // QS Refactor setting changed - restart SystemUI
                context?.let { SystemRestartUtils.restartSystemUI(it) }
                true
            }
            
            else -> false
        }
    }
    
    private fun updateAnimTileStyle(tileAnimationStyle: Int) {
        mTileAnimationDuration?.isEnabled = tileAnimationStyle != 0
        mTileAnimationInterpolator?.isEnabled = tileAnimationStyle != 0
    }
    
    private fun updateSplitShadeEnabled(context: Context) {
        val resolver = context.contentResolver
        val splitShadeEnabled = Settings.System.getIntForUser(
                resolver,
                "qs_split_shade_enabled", 0, UserHandle.USER_CURRENT) != 0
        val splitShadeStyleCategory = "android.theme.customization.better_qs"
        val overlayThemeTarget = "com.android.systemui"
        val overlayThemePackage = "com.android.system.qs.ui.better_qs"
        
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context)
        }
        
        // Optimize: Only apply if state actually changed
        if (splitShadeEnabled != mLastSplitShadeEnabled) {
            mLastSplitShadeEnabled = splitShadeEnabled
            mHandler?.postDelayed({
                if (isAdded && mThemeUtils != null) {
                    mThemeUtils?.setOverlayEnabled(splitShadeStyleCategory, overlayThemeTarget, overlayThemeTarget)
                    if (splitShadeEnabled) {
                        mThemeUtils?.setOverlayEnabled(splitShadeStyleCategory, overlayThemePackage, overlayThemeTarget)
                    }
                }
            }, 1250)
        }
    }

    private fun updateQsStyle(context: Context) {
        val resolver = context.contentResolver

        val isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE, 0, UserHandle.USER_CURRENT) != 0
        
        val currentStyle = if (isA11Style) "A11" else "default"
        
        // Optimize: Only apply if style actually changed
        if (currentStyle == mLastQsUIStyle) {
            return
        }
        mLastQsUIStyle = currentStyle

        val qsUIStyleCategory = "android.theme.customization.qs_ui"
        val overlayThemeTarget = "com.android.systemui"
        val overlayThemePackage = "com.android.system.qs.ui.A11"

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context)
        }

        // reset all overlays before applying
        mThemeUtils?.setOverlayEnabled(qsUIStyleCategory, overlayThemeTarget, overlayThemeTarget)

        if (isA11Style) {
            mThemeUtils?.setOverlayEnabled(qsUIStyleCategory, overlayThemePackage, overlayThemeTarget)
        }
    }
    
    private fun updateQsPanelStyle(context: Context) {
        val resolver = context.contentResolver

        val qsPanelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT)
        
        val currentPanelStyle = qsPanelStyle.toString()
        
        // Optimize: Only apply if style actually changed
        if (currentPanelStyle == mLastQsPanelStyle) {
            return
        }
        mLastQsPanelStyle = currentPanelStyle

        val qsPanelStyleCategory = "android.theme.customization.qs_panel"
        val overlayThemeTarget = "com.android.systemui"
        var overlayThemePackage = "com.android.systemui"

        when (qsPanelStyle) {
            1 -> overlayThemePackage = "com.android.system.qs.outline"
            2, 3 -> overlayThemePackage = "com.android.system.qs.twotoneaccent"
            4 -> overlayThemePackage = "com.android.system.qs.shaded"
            5 -> overlayThemePackage = "com.android.system.qs.cyberpunk"
            6 -> overlayThemePackage = "com.android.system.qs.neumorph"
            7 -> overlayThemePackage = "com.android.system.qs.reflected"
            8 -> overlayThemePackage = "com.android.system.qs.surround"
            9 -> overlayThemePackage = "com.android.system.qs.thin"
        }

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context)
        }

        // reset all overlays before applying
        mThemeUtils?.setOverlayEnabled(qsPanelStyleCategory, overlayThemeTarget, overlayThemeTarget)

        if (qsPanelStyle > 0) {
            mThemeUtils?.setOverlayEnabled(qsPanelStyleCategory, overlayThemePackage, overlayThemeTarget)
        }
    }

    private fun checkQSOverlays(context: Context) {
        val resolver = context.contentResolver
        val isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE, 0, UserHandle.USER_CURRENT)
        val qsPanelStyleValue = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT)

        // Update summaries
        mQsUI?.let { qsUI ->
            val index = qsUI.findIndexOfValue(isA11Style.toString())
            if (index >= 0 && index < qsUI.entries.size) {
                qsUI.value = isA11Style.toString()
                qsUI.summary = qsUI.entries[index]
            }
        }

        mQsPanelStyle?.let { qsPanelStylePref ->
            val index = qsPanelStylePref.findIndexOfValue(qsPanelStyleValue.toString())
            if (index >= 0 && index < qsPanelStylePref.entries.size) {
                qsPanelStylePref.value = qsPanelStyleValue.toString()
                qsPanelStylePref.summary = qsPanelStylePref.entries[index]
            }
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup to prevent memory leaks
        mHandler?.let {
            it.removeCallbacksAndMessages(null)
            mHandler = null
        }
        mThemeUtils = null
        mLastQsUIStyle = null
        mLastQsPanelStyle = null
    }
    
    override fun onDetach() {
        super.onDetach()
        // Additional cleanup
        mHandler?.removeCallbacksAndMessages(null)
    }
}
