/*
 * Copyright (C) 2016-2024 crDroid Android Project
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
package com.rising.settings.fragments.statusbar

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreferenceCompat

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.colorpicker.ColorPickerPreference
import com.android.settings.preferences.CustomSeekBarPreference

class BatteryBar : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val PREF_BATT_BAR = "statusbar_battery_bar"
    }

    private lateinit var mBatteryBar: SwitchPreferenceCompat
    private var mIsBarSwitchingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.battery_bar)

        val prefSet = preferenceScreen
        val resolver = activity?.contentResolver

        mBatteryBar = findCachedPreference<SwitchPreferenceCompat>(PREF_BATT_BAR)!!

        val showing = Settings.System.getIntForUser(resolver,
                Settings.System.STATUSBAR_BATTERY_BAR, 0, UserHandle.USER_CURRENT) != 0
        mBatteryBar.isChecked = showing
        mBatteryBar.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = activity?.contentResolver
        if (preference == mBatteryBar) {
            if (mIsBarSwitchingMode) {
                return false
            }
            mIsBarSwitchingMode = true
            val value = newValue as Boolean
            Settings.System.putIntForUser(resolver, Settings.System.STATUSBAR_BATTERY_BAR,
                    if (value) 1 else 0, UserHandle.USER_CURRENT)
            postDelayedSafe({
                mIsBarSwitchingMode = false
                if (isFragmentReady()) {
                    val showing = Settings.System.getIntForUser(resolver,
                            Settings.System.STATUSBAR_BATTERY_BAR, 0, UserHandle.USER_CURRENT) != 0
                    mBatteryBar.isChecked = showing
                }
            }, 1500)
            return true
        }
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
