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
import com.android.internal.util.android.SystemRestartUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Security : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val HIDE_SCREEN_CAPTURE_STATUS_KEY = "hide_screen_capture_status"
        private const val NO_STORAGE_RESTRICT_KEY = "no_storage_restrict"
        private const val WINDOW_IGNORE_SECURE_KEY = "window_ignore_secure"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_security) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }

    private lateinit var mHideScreenCapturePref: Preference
    private lateinit var mNoStorageRestrictPref: Preference
    private lateinit var mWindowIgnoreSecurePref: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_security)

        mHideScreenCapturePref = findPreference(HIDE_SCREEN_CAPTURE_STATUS_KEY)!!
        mNoStorageRestrictPref = findPreference(NO_STORAGE_RESTRICT_KEY)!!
        mWindowIgnoreSecurePref = findPreference(WINDOW_IGNORE_SECURE_KEY)!!

        mHideScreenCapturePref.onPreferenceChangeListener = this
        mNoStorageRestrictPref.onPreferenceChangeListener = this
        mWindowIgnoreSecurePref.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference) {
            mHideScreenCapturePref, mNoStorageRestrictPref, mWindowIgnoreSecurePref -> {
                SystemRestartUtils.showSystemRestartDialog(context)
                true
            }
            else -> false
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
