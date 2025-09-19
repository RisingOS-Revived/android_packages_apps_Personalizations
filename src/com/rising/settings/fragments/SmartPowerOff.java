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
package com.rising.settings.fragments;

import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.rising.settings.fragments.OptimizedSettingsFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.Calendar;
import java.util.List;

@SearchIndexable
public class SmartPowerOff extends OptimizedSettingsFragment {

    private static final String TAG = "SmartPowerOff";

    private static final String REBOOT_TIME_KEY = "smart_power_off_time";
    private static final String REBOOT_TIME_PREFERENCE_KEY = "power_off_time_preference";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rising_settings_smart_power_off);

        Preference timePreference = findPreference(REBOOT_TIME_PREFERENCE_KEY);
        if (timePreference != null) {
            timePreference.setOnPreferenceClickListener(preference -> {
                showTimePicker();
                return true;
            });
            updateRebootTimeSummary(timePreference);
        }
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int hour = (currentHour + 1) % 24;
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minute) -> {
                    saveRebootTime(hourOfDay, minute);
                },
                hour, currentMinute, DateFormat.is24HourFormat(getContext()));

        timePickerDialog.show();
    }

    private void saveRebootTime(int hourOfDay, int minute) {
        Calendar currentCalendar = Calendar.getInstance();
        Calendar selectedCalendar = Calendar.getInstance();
        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        selectedCalendar.set(Calendar.MINUTE, minute);
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        if (selectedCalendar.getTimeInMillis() <= currentCalendar.getTimeInMillis()) {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        String timeValue = String.format("%02d:%02d", selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE));
        ContentResolver resolver = getContext().getContentResolver();
        Settings.System.putString(resolver, REBOOT_TIME_KEY, timeValue);
        Preference timePreference = findPreference(REBOOT_TIME_PREFERENCE_KEY);
        if (timePreference != null) {
            updateRebootTimeSummary(timePreference);
        }
    }

    private void updateRebootTimeSummary(Preference timePreference) {
        String timeValue = Settings.System.getString(getContext().getContentResolver(), REBOOT_TIME_KEY);
        if (timeValue != null) {
            timePreference.setSummary(getContext().getString(R.string.smart_power_off_desc) + " " + timeValue);
        } else {
            timePreference.setSummary(getContext().getString(R.string.smart_power_off_time_summary));
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_smart_power_off) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
