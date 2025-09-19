/*
 * Copyright (C) 2016-2024 crDroid Android Project
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

import android.content.Context
import android.content.ContentResolver
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener

import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settingslib.widget.MainSwitchPreference

import com.android.settings.preferences.colorpicker.ColorPickerPreference

class AdaptivePlayback : OptimizedSettingsFragment(), 
        Preference.OnPreferenceChangeListener, OnCheckedChangeListener {

    companion object {
        private val TAG = AdaptivePlayback::class.java.simpleName
        private const val PREF_KEY_ENABLE = "adaptive_playback_enabled"
    }

    private lateinit var mEnable: MainSwitchPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.adaptive_playback_settings)

        mEnable = findCachedPreference<MainSwitchPreference>(PREF_KEY_ENABLE)!!
        val enable = Settings.System.getIntForUser(requireContext().contentResolver,
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT) != 0
        mEnable.isChecked = enable
        mEnable.addOnSwitchChangeListener(this)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return false
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        mEnable.isChecked = isChecked
        if (isChecked) {
            Settings.System.putIntForUser(requireContext().contentResolver,
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 1, UserHandle.USER_CURRENT)
        } else {
            Settings.System.putIntForUser(requireContext().contentResolver,
                Settings.System.ADAPTIVE_PLAYBACK_ENABLED, 0, UserHandle.USER_CURRENT)
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
