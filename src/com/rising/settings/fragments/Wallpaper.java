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

import com.android.settings.preferences.CustomSeekBarPreference;

import java.util.List;

import com.android.settings.utils.SystemRestartUtils;

@SearchIndexable
public class Wallpaper extends OptimizedSettingsFragment 
            implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "Wallpaper";
    
    private Preference mBlurWpPref;
    private Preference mBlurWpStylePref;
    private Preference mDimPref;
    private Preference mDimLvlPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_wallpaper);
        mBlurWpPref = findPreference("persist.sys.wallpaper.blur_enabled");
        mBlurWpPref.setOnPreferenceChangeListener(this);
        mBlurWpStylePref = findPreference("persist.sys.wallpaper.blur_type");
        mBlurWpStylePref.setOnPreferenceChangeListener(this);
        mDimPref = findPreference("persist.sys.wallpaper.dim_enabled");
        mDimPref.setOnPreferenceChangeListener(this);
        mDimLvlPref = findPreference("persist.sys.wallpaper.dim_level");
        mDimLvlPref.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Use safe context access
        Context context = getSafeContext();
        if (context == null) {
            return false;
        }
        
        if (preference == mBlurWpPref 
                || preference == mBlurWpStylePref 
                || preference == mDimPref 
                || preference == mDimLvlPref) {
          if (preference == mDimLvlPref) {
              android.os.SystemProperties.set("persist.sys.wallpaper.dim_level", newValue.toString());
          }
          SystemRestartUtils.showSystemUIRestartDialog(context);
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
            new BaseSearchIndexProvider(R.xml.rising_settings_wallpaper) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
