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
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.SystemRestartUtils
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Notifications : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "Notifications"
        
        private const val COMPACT_HUN_KEY = "persist.sys.compact_hun.enabled"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_notification) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                return keys
            }
        }
    }
    
    private var mCompactHUNPref: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_notification)
        mCompactHUNPref = findCachedPreference(COMPACT_HUN_KEY)
        mCompactHUNPref?.onPreferenceChangeListener = this
    }
    
    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return when (preference) {
            mCompactHUNPref -> {
                val context = getSafeContext()
                context?.let { SystemRestartUtils.showSystemUIRestartDialog(it) }
                true
            }
            else -> false
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
