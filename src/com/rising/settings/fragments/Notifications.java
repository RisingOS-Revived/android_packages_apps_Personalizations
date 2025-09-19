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

import android.content.Context;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.rising.settings.fragments.OptimizedSettingsFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.android.settings.utils.SystemRestartUtils;

import java.util.List;

@SearchIndexable
public class Notifications extends OptimizedSettingsFragment implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "Notifications";
    
    private static final String COMPACT_HUN_KEY = "persist.sys.compact_hun.enabled";
    
    private Preference mCompactHUNPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_notification);
        mCompactHUNPref = findCachedPreference(COMPACT_HUN_KEY);
        if (mCompactHUNPref != null) {
            mCompactHUNPref.setOnPreferenceChangeListener(this);
        }
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCompactHUNPref) {
            Context context = getSafeContext();
            if (context != null) {
                SystemRestartUtils.showSystemUIRestartDialog(context);
            }
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
            new BaseSearchIndexProvider(R.xml.rising_settings_notification) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
