/*
 * Copyright (C) 2024 Yet Another AOSP Project
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

package com.rising.settings.fragments.sound

import android.content.ContentResolver
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.Preference.OnPreferenceChangeListener

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.CustomSeekBarPreference

class VolumeSteps : OptimizedSettingsFragment(), OnPreferenceChangeListener {

    companion object {
        private const val TAG = "VolumeSteps"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.volume_steps_settings)

        val resolver = activity?.contentResolver
        val screen = preferenceScreen
        val count = screen.preferenceCount
        for (i in 0 until count) {
            val pref = screen.getPreference(i)
            if (pref !is CustomSeekBarPreference) continue
            
            val key = pref.key
            val def = Settings.System.getIntForUser(resolver, "default_$key", 15, UserHandle.USER_CURRENT)
            val value = Settings.System.getIntForUser(resolver, key, def, UserHandle.USER_CURRENT)
            val sbPref = pref as CustomSeekBarPreference
            // Set default value - property may not exist in this version
            sbPref.setValue(value)
            sbPref.onPreferenceChangeListener = this
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference !is CustomSeekBarPreference) return false
        
        Settings.System.putIntForUser(activity?.contentResolver,
                preference.key, newValue as Int, UserHandle.USER_CURRENT)
        return true
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
