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
package com.rising.settings.fragments.lockscreen

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.os.UserHandle
import android.widget.Button
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.SystemRestartUtils
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.LayoutPreference

@SearchIndexable
class LockScreenWidgets : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "LockScreenWidgets"

        private const val KEY_LOCKSCREEN_INFO_WIDGETS = "lockscreen_info_widgets_enabled"
        private const val KEY_LOCKSCREEN_WIDGETS_STYLE = "lockscreen_widgets_style"
        private const val KEY_APPLY_CHANGE_BUTTON = "apply_change_button"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_lockscreen_widgets) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }

    private lateinit var mApplyChange: Button
    private lateinit var mInfoWidgetsPref: SwitchPreferenceCompat
    private lateinit var mWidgetsStylePref: Preference

    private var hasUnsavedChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_lockscreen_widgets)

        mInfoWidgetsPref = findPreference(KEY_LOCKSCREEN_INFO_WIDGETS)!!
        mWidgetsStylePref = findPreference(KEY_LOCKSCREEN_WIDGETS_STYLE)!!

        val layoutPreference = findPreference<LayoutPreference>(KEY_APPLY_CHANGE_BUTTON)!!
        mApplyChange = layoutPreference.findViewById(R.id.apply_change)

        mInfoWidgetsPref.onPreferenceChangeListener = this
        mWidgetsStylePref.onPreferenceChangeListener = this

        mApplyChange.isEnabled = false
        mApplyChange.setOnClickListener {
            applyChanges()
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        hasUnsavedChanges = true
        mApplyChange.isEnabled = true
        return true
    }

    private fun applyChanges() {
        hasUnsavedChanges = false
        mApplyChange.isEnabled = false

        context?.let { ctx ->
            SystemRestartUtils.restartSystemUI(ctx)
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
