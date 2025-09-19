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
import android.os.SystemProperties
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.SystemRestartUtils
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Wallpaper : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "Wallpaper"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_wallpaper) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                return keys
            }
        }
    }
    
    private var mBlurWpPref: Preference? = null
    private var mBlurWpStylePref: Preference? = null
    private var mDimPref: Preference? = null
    private var mDimLvlPref: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_wallpaper)
        mBlurWpPref = findPreference("persist.sys.wallpaper.blur_enabled")
        mBlurWpPref?.onPreferenceChangeListener = this
        mBlurWpStylePref = findPreference("persist.sys.wallpaper.blur_type")
        mBlurWpStylePref?.onPreferenceChangeListener = this
        mDimPref = findPreference("persist.sys.wallpaper.dim_enabled")
        mDimPref?.onPreferenceChangeListener = this
        mDimLvlPref = findPreference("persist.sys.wallpaper.dim_level")
        mDimLvlPref?.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        // Use safe context access
        val context = getSafeContext() ?: return false
        
        return when (preference) {
            mBlurWpPref, mBlurWpStylePref, mDimPref, mDimLvlPref -> {
                if (preference == mDimLvlPref) {
                    SystemProperties.set("persist.sys.wallpaper.dim_level", newValue.toString())
                }
                SystemRestartUtils.showSystemUIRestartDialog(context)
                true
            }
            else -> false
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
