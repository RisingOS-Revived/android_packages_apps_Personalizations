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
import com.android.internal.util.android.SystemRestartUtils
import com.android.settings.utils.SystemRestartUtils as SystemUiRestartUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.preferences.GlobalSettingListPreference
import com.android.settings.preferences.SystemPropertyListPreference
import com.android.settings.preferences.SystemSettingListPreference
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Themes : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "Themes"
        private const val KEY_LOCK_SOUND = "lock_sound"
        private const val KEY_UNLOCK_SOUND = "unlock_sound"
        private const val KEY_EMOJI_STYLE = "persist.sys.ax_emoji_style"
        private const val KEY_VOLUME_DIALOG_TYPE = "volume_dialog_type"
        private const val KEY_SHOW_VOLUME_PERCENTAGE = "show_volume_percentage"
        private const val KEY_AXION_VOLUME_STYLE = "axion_volume_style"
        private const val SYS_ANI_OVERRIDE_ENABLED = "persist.sys.activity_anim_perf_override"

        private const val VOLUME_TYPE_AXION = 0
        private const val VOLUME_TYPE_REDESIGNED = 1
        private const val VOLUME_TYPE_STOCK = 2

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
    private var mVolumeDialogType: SystemSettingListPreference? = null
    private var mAxionVolumeStyle: SystemSettingListPreference? = null
    private var mShowVolumePercentage: SystemSettingListPreference? = null
    private var mAniOverrideEnabled: SystemPropertyListPreference?? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_themes)

        mLockSound = findPreference<GlobalSettingListPreference>(KEY_LOCK_SOUND)
        mLockSound?.onPreferenceChangeListener = this
        mUnlockSound = findPreference<GlobalSettingListPreference>(KEY_UNLOCK_SOUND)
        mUnlockSound?.onPreferenceChangeListener = this
        mEmojiStyle = findPreference<SystemPropertyListPreference>(KEY_EMOJI_STYLE)
        mEmojiStyle?.onPreferenceChangeListener = this

        mAniOverrideEnabled = findPreference<SystemPropertyListPreference?>(SYS_ANI_OVERRIDE_ENABLED)
        mAniOverrideEnabled?.onPreferenceChangeListener = this

        mVolumeDialogType = findPreference<SystemSettingListPreference>(KEY_VOLUME_DIALOG_TYPE)
        mVolumeDialogType?.onPreferenceChangeListener = this

        mAxionVolumeStyle = findPreference<SystemSettingListPreference>(KEY_AXION_VOLUME_STYLE)
        mShowVolumePercentage = findPreference<SystemSettingListPreference>(KEY_SHOW_VOLUME_PERCENTAGE)

        updateVolumeRelatedVisibility(getCurrentVolumeDialogType())

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

    private fun getCurrentVolumeDialogType(): Int {
        val ctx = context ?: return VOLUME_TYPE_REDESIGNED
        return Settings.System.getIntForUser(
            ctx.contentResolver,
            KEY_VOLUME_DIALOG_TYPE,
            VOLUME_TYPE_REDESIGNED,
            UserHandle.USER_CURRENT
        )
    }

    private fun updateVolumeRelatedVisibility(type: Int) {
        mAxionVolumeStyle?.isVisible = type == VOLUME_TYPE_AXION
        mShowVolumePercentage?.isVisible = type == VOLUME_TYPE_REDESIGNED
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference) {
            mLockSound, mUnlockSound -> {
                context?.let { SystemUiRestartUtils.showSystemUIRestartDialog(it) }
                true
            }

            mEmojiStyle -> {
                context?.let { SystemRestartUtils.showSystemRestartDialog(it) }
                true
            }

            mVolumeDialogType -> {
                context?.let { SystemUiRestartUtils.showSystemUIRestartDialog(it) }
                val type = (newValue as? String)?.toIntOrNull() ?: VOLUME_TYPE_REDESIGNED
                updateVolumeRelatedVisibility(type)
                true
            }

            mAniOverrideEnabled -> {
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
