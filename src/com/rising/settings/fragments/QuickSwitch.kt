/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import android.os.SystemProperties
import android.provider.SearchIndexableResource
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.internal.util.android.SystemRestartUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.Indexable
import com.android.settingslib.search.SearchIndexable
import com.android.settings.preferences.LauncherIconPreference

@SearchIndexable
class QuickSwitch : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener, Indexable {

    companion object {
        private const val TAG = "QuickSwitch"
        private const val QUICKSWITCH_KEY = "persist.sys.default_launcher"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider() {
            override fun getXmlResourcesToIndex(context: Context, enabled: Boolean): List<SearchIndexableResource> {
                val result = ArrayList<SearchIndexableResource>()
                val sir = SearchIndexableResource(context).apply {
                    xmlResId = R.xml.quick_switch
                }
                result.add(sir)
                return result
            }

            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }

    private lateinit var quickSwitchPref: LauncherIconPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.quick_switch)

        quickSwitchPref = findPreference(QUICKSWITCH_KEY)!!
        quickSwitchPref.onPreferenceChangeListener = this
        
        val defaultLauncher = SystemProperties.getInt(QUICKSWITCH_KEY, 0)
        quickSwitchPref.setValue(defaultLauncher)
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return if (preference == quickSwitchPref) {
            SystemRestartUtils.showSystemRestartDialog(context)
            true
        } else {
            false
        }
    }
}
