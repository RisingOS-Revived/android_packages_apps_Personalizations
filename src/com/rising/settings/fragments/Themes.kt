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
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.internal.util.android.SystemRestartUtils;
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.preferences.GlobalSettingListPreference
import com.android.settings.preferences.SystemPropertyListPreference
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Themes : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "Themes"
        private const val KEY_LOCK_SOUND = "lock_sound"
        private const val KEY_UNLOCK_SOUND = "unlock_sound"
        private const val KEY_EMOJI_STYLE = "persist.sys.ax_emoji_style"

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

    private var mLockSound: GlobalSettingListPreference? = null
    private var mUnlockSound: GlobalSettingListPreference? = null
    private var mEmojiStyle: SystemPropertyListPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_themes)

        mLockSound = findPreference<GlobalSettingListPreference>(KEY_LOCK_SOUND)
        mLockSound?.onPreferenceChangeListener = this
        mUnlockSound = findPreference<GlobalSettingListPreference>(KEY_UNLOCK_SOUND)
        mUnlockSound?.onPreferenceChangeListener = this
        mEmojiStyle = findPreference<SystemPropertyListPreference>(KEY_EMOJI_STYLE)
        mEmojiStyle?.onPreferenceChangeListener = this

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

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference) {
            mLockSound, mUnlockSound -> {
                context?.let { SystemRestartUtils.showSystemUIRestartDialog(it) }
                true
            }

            mEmojiStyle -> {
                context?.let { SystemRestartUtils.showSystemRestartDialog(it) }
                true
            }

            else -> false
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
