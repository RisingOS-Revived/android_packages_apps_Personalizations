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

import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnCancelListener
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Menu
import android.widget.EditText

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.Preference.OnPreferenceChangeListener

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.CustomSeekBarPreference
import com.android.settings.preferences.SystemSettingListPreference

import java.util.Date

import lineageos.preference.LineageSystemSettingListPreference
import lineageos.providers.LineageSettings

class Clock : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val TAG = "Clock"

        private const val STATUS_BAR_AM_PM = "status_bar_am_pm"
        private const val CLOCK_DATE_DISPLAY = "status_bar_clock_date_display"
        private const val CLOCK_DATE_POSITION = "status_bar_clock_date_position"
        private const val CLOCK_DATE_STYLE = "status_bar_clock_date_style"
        private const val CLOCK_DATE_FORMAT = "status_bar_clock_date_format"
        private const val CLOCK_SECONDS = "status_bar_clock_seconds"

        private const val CLOCK_DATE_STYLE_LOWERCASE = 1
        private const val CLOCK_DATE_STYLE_UPPERCASE = 2
        private const val CUSTOM_CLOCK_DATE_FORMAT_INDEX = 18
    }

    private lateinit var mStatusBarAmPm: LineageSystemSettingListPreference
    private lateinit var mClockDateDisplay: SystemSettingListPreference
    private lateinit var mClockDatePosition: SystemSettingListPreference
    private lateinit var mClockDateStyle: SystemSettingListPreference
    private lateinit var mClockDateFormat: ListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.status_bar_clock)

        val resolver = activity?.contentResolver

        mStatusBarAmPm = findCachedPreference<LineageSystemSettingListPreference>(STATUS_BAR_AM_PM)!!

        if (DateFormat.is24HourFormat(activity)) {
            mStatusBarAmPm.isEnabled = false
            mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info)
        }

        val dateDisplay = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_DISPLAY, 0, UserHandle.USER_CURRENT)

        mClockDateDisplay = findCachedPreference<SystemSettingListPreference>(CLOCK_DATE_DISPLAY)!!
        mClockDateDisplay.onPreferenceChangeListener = this

        mClockDatePosition = findCachedPreference<SystemSettingListPreference>(CLOCK_DATE_POSITION)!!
        mClockDatePosition.isEnabled = dateDisplay > 0
        mClockDatePosition.onPreferenceChangeListener = this

        mClockDateStyle = findCachedPreference<SystemSettingListPreference>(CLOCK_DATE_STYLE)!!
        mClockDateStyle.isEnabled = dateDisplay > 0
        mClockDateStyle.onPreferenceChangeListener = this

        mClockDateFormat = findCachedPreference<ListPreference>(CLOCK_DATE_FORMAT)!!
        if (mClockDateFormat.value == null) {
            mClockDateFormat.value = "EEE"
        }
        parseClockDateFormats()
        mClockDateFormat.isEnabled = dateDisplay > 0
        mClockDateFormat.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = activity?.contentResolver
        when (preference) {
            mClockDateDisplay -> {
                val value = (newValue as String).toInt()
                if (value == 0) {
                    mClockDatePosition.isEnabled = false
                    mClockDateStyle.isEnabled = false
                    mClockDateFormat.isEnabled = false
                } else {
                    mClockDatePosition.isEnabled = true
                    mClockDateStyle.isEnabled = true
                    mClockDateFormat.isEnabled = true
                }
                return true
            }
            mClockDatePosition -> {
                parseClockDateFormats()
                return true
            }
            mClockDateStyle -> {
                parseClockDateFormats()
                return true
            }
            mClockDateFormat -> {
                val index = mClockDateFormat.findIndexOfValue(newValue as String)

                if (index == CUSTOM_CLOCK_DATE_FORMAT_INDEX) {
                    val alert = AlertDialog.Builder(activity)
                    alert.setTitle(R.string.status_bar_date_string_edittext_title)
                    alert.setMessage(R.string.status_bar_date_string_edittext_summary)

                    val input = EditText(activity)
                    val oldText = Settings.System.getString(
                        resolver,
                        Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT)
                    if (oldText != null) {
                        input.setText(oldText)
                    }
                    alert.setView(input)

                    alert.setPositiveButton(R.string.menu_save) { _, _ ->
                        val value = input.text.toString()
                        if (value.isNotEmpty()) {
                            Settings.System.putString(resolver,
                                Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, value)
                        }
                    }

                    alert.setNegativeButton(R.string.menu_cancel) { _, _ ->
                        // Do nothing
                    }
                    val dialog = alert.create()
                    dialog.show()
                } else {
                    if (newValue.isNotEmpty()) {
                        Settings.System.putString(resolver,
                            Settings.System.STATUS_BAR_CLOCK_DATE_FORMAT, newValue)
                    }
                }
                return true
            }
        }
        return false
    }

    private fun parseClockDateFormats() {
        val dateEntries = resources.getStringArray(R.array.status_bar_date_format_entries_values)
        val parsedDateEntries = Array<CharSequence>(dateEntries.size) { "" }
        val now = Date()

        val lastEntry = dateEntries.size - 1
        val dateFormat = Settings.System.getIntForUser(activity?.contentResolver,
                Settings.System.STATUS_BAR_CLOCK_DATE_STYLE, 0, UserHandle.USER_CURRENT)
        
        for (i in dateEntries.indices) {
            if (i == lastEntry) {
                parsedDateEntries[i] = dateEntries[i]
            } else {
                val dateString = DateFormat.format(dateEntries[i], now)
                val newDate = when (dateFormat) {
                    CLOCK_DATE_STYLE_LOWERCASE -> dateString.toString().lowercase()
                    CLOCK_DATE_STYLE_UPPERCASE -> dateString.toString().uppercase()
                    else -> dateString.toString()
                }
                parsedDateEntries[i] = newDate
            }
        }
        mClockDateFormat.entries = parsedDateEntries
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
