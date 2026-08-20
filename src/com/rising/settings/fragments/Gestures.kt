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

import androidx.preference.ListPreference
import androidx.preference.Preference

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

import lineageos.providers.LineageSettings

import org.lineageos.internal.util.DeviceKeysConstants.Action

@SearchIndexable
class Gestures : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val KEY_THREE_FINGERS_SWIPE = "three_fingers_swipe"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_gestures) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                return keys
            }
        }
    }

    private var mThreeFingersSwipeAction: ListPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_gestures)

        val threeFingersSwipeAction = Action.fromSettings(
            contentResolver,
            LineageSettings.System.KEY_THREE_FINGERS_SWIPE_ACTION,
            Action.NOTHING
        )
        mThreeFingersSwipeAction = initList(KEY_THREE_FINGERS_SWIPE, threeFingersSwipeAction)
    }

    private fun initList(key: String, value: Action): ListPreference? {
        return initList(key, value.ordinal)
    }

    private fun initList(key: String, value: Int): ListPreference? {
        val list = preferenceScreen.findPreference<ListPreference>(key) ?: return null
        list.value = value.toString()
        list.summary = list.entry
        list.onPreferenceChangeListener = this
        return list
    }

    private fun handleListChange(pref: ListPreference, newValue: Any?, setting: String) {
        val value = newValue as String
        val index = pref.findIndexOfValue(value)
        pref.summary = pref.entries[index]
        LineageSettings.System.putIntForUser(
            contentResolver, setting, Integer.valueOf(value), UserHandle.USER_CURRENT
        )
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference === mThreeFingersSwipeAction) {
            handleListChange(
                preference as ListPreference, newValue,
                LineageSettings.System.KEY_THREE_FINGERS_SWIPE_ACTION
            )
            return true
        }
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
