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

import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.internal.util.android.ThemeUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.preferences.GlobalSettingListPreference
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.SystemRestartUtils
import com.android.settingslib.search.SearchIndexable
import java.util.concurrent.ConcurrentHashMap

@SearchIndexable
class Themes : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "Themes"
        private const val KEY_PGB_STYLE = "progress_bar_style"
        private const val KEY_NOTIF_STYLE = "notification_style"
        private const val KEY_POWERMENU_STYLE = "powermenu_style"
        private const val KEY_HIDE_IME_STYLE = "hide_ime_space_style"
        private const val KEY_LOCK_SOUND = "lock_sound"
        private const val KEY_UNLOCK_SOUND = "unlock_sound"

        private val POWER_MENU_OVERLAYS = arrayOf(
            "com.android.theme.powermenu.cyberpunk",
            "com.android.theme.powermenu.duoline",
            "com.android.theme.powermenu.ios",
            "com.android.theme.powermenu.layers"
        )

        private val NOTIF_OVERLAYS = arrayOf(
            "com.android.theme.notification.cyberpunk",
            "com.android.theme.notification.duoline",
            "com.android.theme.notification.ios",
            "com.android.theme.notification.layers"
        )

        private val PROGRESS_BAR_OVERLAYS = arrayOf(
            "com.android.theme.progressbar.blocky_thumb",
            "com.android.theme.progressbar.minimal_thumb",
            "com.android.theme.progressbar.outline_thumb",
            "com.android.theme.progressbar.shishu"
        )

        private val HIDE_IME_OVERLAYS = arrayOf(
            "com.android.system.theme.hide_ime_space_narrow",
            "com.android.system.theme.hide_ime_space_no_space"
        )

        // Cache keys
        private const val CACHE_KEY_PROGRESS_BAR = "progress_bar_style"
        private const val CACHE_KEY_NOTIFICATION = "notification_style"
        private const val CACHE_KEY_POWER_MENU = "powermenu_style"
        private const val CACHE_KEY_HIDE_IME = "hide_ime_style"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_themes) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                return keys
            }
        }
    }

    private var mThemeUtils: ThemeUtils? = null
    private var mProgressBarPref: Preference? = null
    private var mNotificationStylePref: Preference? = null
    private var mPowerMenuStylePref: Preference? = null
    private var mHideImePref: Preference? = null
    private var mLockSound: GlobalSettingListPreference? = null
    private var mUnlockSound: GlobalSettingListPreference? = null
    
    // Cache for style states to prevent redundant operations
    private val mStyleCache = ConcurrentHashMap<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_themes)
        
        // Initialize ThemeUtils with null check
        activity?.let { activity ->
            mThemeUtils = ThemeUtils.getInstance(activity)
        }

        mProgressBarPref = findPreference(KEY_PGB_STYLE)
        mProgressBarPref?.onPreferenceChangeListener = this

        mNotificationStylePref = findPreference(KEY_NOTIF_STYLE)
        mNotificationStylePref?.onPreferenceChangeListener = this

        mPowerMenuStylePref = findPreference(KEY_POWERMENU_STYLE)
        mPowerMenuStylePref?.onPreferenceChangeListener = this

        mHideImePref = findPreference(KEY_HIDE_IME_STYLE)
        mHideImePref?.onPreferenceChangeListener = this

        mLockSound = findPreference<GlobalSettingListPreference>(KEY_LOCK_SOUND)
        mLockSound?.onPreferenceChangeListener = this
        mUnlockSound = findPreference<GlobalSettingListPreference>(KEY_UNLOCK_SOUND)
        mUnlockSound?.onPreferenceChangeListener = this

        // Initialize highlight preferences with null checks
        preferenceScreen?.let { screen ->
            val highlightPref = screen.findPreference<com.android.settingslib.widget.LayoutPreference>("themes_highlight_dashboard")
            highlightPref?.let { pref ->
                context?.let { ctx ->
                    val highlightClickMap = hashMapOf<Int, String>().apply {
                        put(R.id.boot_styles_tile, "PersonalizationsBSActivity")
                        put(R.id.icon_pack_tile, "PersonalizationsIconPackActivity")
                        put(R.id.settings_tile, "PersonalizationsSettingsUIActivity")
                        put(R.id.wallpaper_styles_tile, "PersonalizationsWSActivity")
                    }
                    com.android.settings.utils.HighlightPrefUtils.setupHighlightPref(ctx, pref, highlightClickMap)
                }
            }
        }
    }

    private fun updateStyle(key: String, category: String, target: String,
            defaultValue: Int, overlayPackages: Array<String>, restartSystemUI: Boolean) {
        val style = Settings.System.getIntForUser(
                context?.contentResolver,
                key,
                defaultValue,
                UserHandle.USER_CURRENT
        )
        
        // Check cache to avoid redundant operations
        val cachedStyle = mStyleCache[key]
        if (cachedStyle != null && cachedStyle == style) {
            return // Style hasn't changed, skip update
        }
        
        // Update cache
        mStyleCache[key] = style
        
        if (mThemeUtils == null && context != null) {
            mThemeUtils = ThemeUtils.getInstance(context)
        }
        
        mThemeUtils?.let { themeUtils ->
            themeUtils.setOverlayEnabled(category, target, target)
            if (style > 0 && style <= overlayPackages.size) {
                themeUtils.setOverlayEnabled(category, overlayPackages[style - 1], target)
            }
        }
        
        if (restartSystemUI) {
            context?.let { SystemRestartUtils.restartSystemUI(it) }
        }
    }

    private fun updatePowerMenuStyle() {
        updateStyle(KEY_POWERMENU_STYLE, "android.theme.customization.powermenu", "com.android.systemui", 0, POWER_MENU_OVERLAYS, false)
    }

    private fun updateNotifStyle() {
        updateStyle(KEY_NOTIF_STYLE, "android.theme.customization.notification", "com.android.systemui", 0, NOTIF_OVERLAYS, true)
    }

    private fun updateProgressBarStyle() {
        updateStyle(KEY_PGB_STYLE, "android.theme.customization.progress_bar", "android", 0, PROGRESS_BAR_OVERLAYS, false)
    }

    private fun updateHideImeSpaceStyle() {
        updateStyle(KEY_HIDE_IME_STYLE, "android.theme.customization.hide_ime_space", "android", 0, HIDE_IME_OVERLAYS, false)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference) {
            mLockSound, mUnlockSound -> {
                context?.let { SystemRestartUtils.showSystemUIRestartDialog(it) }
                true
            }
            
            mProgressBarPref -> {
                val value = (newValue as String).toInt()
                Settings.System.putIntForUser(activity?.contentResolver,
                        KEY_PGB_STYLE, value, UserHandle.USER_CURRENT)
                updateProgressBarStyle()
                true
            }
            
            mNotificationStylePref -> {
                val value = (newValue as String).toInt()
                Settings.System.putIntForUser(activity?.contentResolver,
                        KEY_NOTIF_STYLE, value, UserHandle.USER_CURRENT)
                updateNotifStyle()
                true
            }
            
            mPowerMenuStylePref -> {
                val value = (newValue as String).toInt()
                Settings.System.putIntForUser(activity?.contentResolver,
                        KEY_POWERMENU_STYLE, value, UserHandle.USER_CURRENT)
                updatePowerMenuStyle()
                true
            }
            
            mHideImePref -> {
                val value = (newValue as String).toInt()
                Settings.System.putIntForUser(activity?.contentResolver,
                        KEY_HIDE_IME_STYLE, value, UserHandle.USER_CURRENT)
                updateHideImeSpaceStyle()
                true
            }
            
            else -> false
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear cache and cleanup resources
        mStyleCache.clear()
        mThemeUtils = null
    }

    override fun onDetach() {
        super.onDetach()
        // Additional cleanup
        mStyleCache.clear()
    }
}
