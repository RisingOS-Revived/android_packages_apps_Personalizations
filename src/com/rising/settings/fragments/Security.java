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

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.Log;
import android.provider.Settings;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.rising.settings.fragments.OptimizedSettingsFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.android.SystemRestartUtils;

import java.util.List;

@SearchIndexable
public class Security extends OptimizedSettingsFragment implements Preference.OnPreferenceChangeListener {

    private final static String HIDE_SCREEN_CAPTURE_STATUS_KEY = "hide_screen_capture_status";
    private final static String NO_STORAGE_RESTRICT_KEY = "no_storage_restrict";
    private final static String WINDOW_IGNORE_SECURE_KEY = "window_ignore_secure";
    
    Preference mHideScreenCapturePref;
    Preference mNoStorageRestrictPref;
    Preference mWindowIgnoreSecurePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rising_settings_security);
        
        mHideScreenCapturePref = findPreference(HIDE_SCREEN_CAPTURE_STATUS_KEY);
        mNoStorageRestrictPref = findPreference(NO_STORAGE_RESTRICT_KEY);
        mWindowIgnoreSecurePref = findPreference(WINDOW_IGNORE_SECURE_KEY);
        
        mHideScreenCapturePref.setOnPreferenceChangeListener(this);
        mNoStorageRestrictPref.setOnPreferenceChangeListener(this);
        mWindowIgnoreSecurePref.setOnPreferenceChangeListener(this);
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHideScreenCapturePref 
            || preference == mNoStorageRestrictPref
            || preference == mWindowIgnoreSecurePref) {
            SystemRestartUtils.showSystemRestartDialog(getContext());
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_security) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
