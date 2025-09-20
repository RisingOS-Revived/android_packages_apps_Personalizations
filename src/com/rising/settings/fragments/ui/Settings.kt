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
package com.rising.settings.fragments.ui

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.UserHandle

import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener

import com.android.internal.util.android.SystemRestartUtils
import com.android.settings.preferences.SystemSettingListPreference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Settings : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val SETTINGS_DASHBOARD_STYLE = "settings_dashboard_style"
        private const val TAG = "Settings"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_settingsui) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context)
                return keys
            }
        }
    }

    private lateinit var mSettingsDashBoardStyle: SystemSettingListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_settingsui)
        mSettingsDashBoardStyle = findCachedPreference<SystemSettingListPreference>(SETTINGS_DASHBOARD_STYLE)!!
        mSettingsDashBoardStyle.onPreferenceChangeListener = this
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun onPreferenceChange(preference: Preference, objValue: Any): Boolean {
        val key = preference.key
        val resolver = activity?.contentResolver
        if (preference == mSettingsDashBoardStyle) {
            SystemRestartUtils.showSettingsRestartDialog(requireContext())
            return true
        }
        return false
    }
}
