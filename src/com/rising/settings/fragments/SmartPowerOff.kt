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
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class SmartPowerOff : OptimizedSettingsFragment() {

    companion object {
        const val TAG = "SmartPowerOff"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_smart_power_off) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                return keys
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_smart_power_off)
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
