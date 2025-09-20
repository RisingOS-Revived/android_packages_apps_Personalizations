/*
 * Copyright (C) 2021 Yet Another AOSP Project
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
package com.rising.settings.fragments.lockscreen.doze

import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.TimePicker

import androidx.preference.Preference
import androidx.preference.PreferenceScreen

import com.android.internal.logging.nano.MetricsProto

import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.SecureSettingListPreference

import java.time.format.DateTimeFormatter
import java.time.LocalTime

class AODSchedule : OptimizedSettingsFragment(), 
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    companion object {
        private const val TAG = "AODSchedule"
        private const val MODE_KEY = "doze_always_on_auto_mode"
        private const val SINCE_PREF_KEY = "doze_always_on_auto_since"
        private const val TILL_PREF_KEY = "doze_always_on_auto_till"

        private const val MODE_DISABLED = 0
        private const val MODE_NIGHT = 1
        private const val MODE_TIME = 2
        private const val MODE_MIXED_SUNSET = 3
        private const val MODE_MIXED_SUNRISE = 4
    }

    private lateinit var mModePref: SecureSettingListPreference
    private lateinit var mSincePref: Preference
    private lateinit var mTillPref: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.always_on_display_schedule)

        val resolver = activity?.contentResolver

        mSincePref = findPreference(SINCE_PREF_KEY)!!
        mSincePref.onPreferenceClickListener = this
        mTillPref = findPreference(TILL_PREF_KEY)!!
        mTillPref.onPreferenceClickListener = this

        val mode = Settings.Secure.getIntForUser(resolver,
                MODE_KEY, MODE_DISABLED, UserHandle.USER_CURRENT)
        mModePref = findPreference<SecureSettingListPreference>(MODE_KEY)!!
        mModePref.value = mode.toString()
        mModePref.summary = mModePref.entry
        mModePref.onPreferenceChangeListener = this

        updateTimeEnablement(mode)
        updateTimeSummary(mode)
    }

    override fun onPreferenceChange(preference: Preference, objValue: Any): Boolean {
        val value = (objValue as String).toInt()
        val index = mModePref.findIndexOfValue(objValue)
        mModePref.summary = mModePref.entries[index]
        Settings.Secure.putIntForUser(activity?.contentResolver,
                MODE_KEY, value, UserHandle.USER_CURRENT)
        updateTimeEnablement(value)
        updateTimeSummary(value)
        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val times = getCustomTimeSetting()
        val isSince = preference == mSincePref
        val hour: Int
        val minute: Int
        val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute1 ->
            updateTimeSetting(isSince, hourOfDay, minute1)
        }
        if (isSince) {
            val sinceValues = times[0].split(":")
            hour = sinceValues[0].toInt()
            minute = sinceValues[1].toInt()
        } else {
            val tillValues = times[1].split(":")
            hour = tillValues[0].toInt()
            minute = tillValues[1].toInt()
        }
        val dialog = TimePickerDialog(context, listener,
                hour, minute, DateFormat.is24HourFormat(context))
        dialog.show()
        return true
    }

    private fun getCustomTimeSetting(): Array<String> {
        var value = Settings.Secure.getStringForUser(activity?.contentResolver,
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_TIME, UserHandle.USER_CURRENT)
        if (value.isNullOrEmpty()) value = "20:00,07:00"
        return value.split(",").toTypedArray()
    }

    private fun updateTimeEnablement(mode: Int) {
        mSincePref.isEnabled = mode == MODE_TIME || mode == MODE_MIXED_SUNRISE
        mTillPref.isEnabled = mode == MODE_TIME || mode == MODE_MIXED_SUNSET
    }

    private fun updateTimeSummary(mode: Int) {
        updateTimeSummary(getCustomTimeSetting(), mode)
    }

    private fun updateTimeSummary(times: Array<String>, mode: Int) {
        if (mode == MODE_DISABLED) {
            mSincePref.summary = "-"
            mTillPref.summary = "-"
            return
        }

        if (mode == MODE_NIGHT) {
            mSincePref.setSummary(R.string.always_on_display_schedule_sunset)
            mTillPref.setSummary(R.string.always_on_display_schedule_sunrise)
            return
        }

        val outputFormat = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        val outputFormatter = DateTimeFormatter.ofPattern(outputFormat)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val sinceDT = LocalTime.parse(times[0], formatter)
        val tillDT = LocalTime.parse(times[1], formatter)

        when (mode) {
            MODE_MIXED_SUNSET -> {
                mSincePref.setSummary(R.string.always_on_display_schedule_sunset)
                mTillPref.summary = tillDT.format(outputFormatter)
            }
            MODE_MIXED_SUNRISE -> {
                mTillPref.setSummary(R.string.always_on_display_schedule_sunrise)
                mSincePref.summary = sinceDT.format(outputFormatter)
            }
            else -> {
                mSincePref.summary = sinceDT.format(outputFormatter)
                mTillPref.summary = tillDT.format(outputFormatter)
            }
        }
    }

    private fun updateTimeSetting(since: Boolean, hour: Int, minute: Int) {
        val times = getCustomTimeSetting()
        var nHour = ""
        var nMinute = ""
        if (hour < 10) nHour += "0"
        if (minute < 10) nMinute += "0"
        nHour += hour.toString()
        nMinute += minute.toString()
        times[if (since) 0 else 1] = "$nHour:$nMinute"
        Settings.Secure.putStringForUser(activity?.contentResolver,
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_TIME,
                "${times[0]},${times[1]}", UserHandle.USER_CURRENT)
        updateTimeSummary(times, mModePref.value.toInt())
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
