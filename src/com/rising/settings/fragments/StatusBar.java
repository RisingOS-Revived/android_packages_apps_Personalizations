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

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.rising.settings.fragments.OptimizedSettingsFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.android.settings.preferences.CustomSeekBarPreference;

import java.util.List;

@SearchIndexable
public class StatusBar extends OptimizedSettingsFragment {

    public static final String TAG = "StatusBar";
    
    private static final String KEY_STATUSBAR_TOP_PADDING = "statusbar_top_padding";
    private static final String KEY_STATUSBAR_LEFT_PADDING = "statusbar_left_padding";
    private static final String KEY_STATUSBAR_RIGHT_PADDING = "statusbar_right_padding";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_status_bar);

        // Use cached preference lookup for better performance
        CustomSeekBarPreference leftSeekBar = findCachedPreference(KEY_STATUSBAR_LEFT_PADDING);
        if (leftSeekBar != null && getResources() != null) {
            int defaultLeftPadding = getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_padding_start);
            leftSeekBar.setDefaultValue(defaultLeftPadding, true);
        }
        
        CustomSeekBarPreference rightSeekBar = findCachedPreference(KEY_STATUSBAR_RIGHT_PADDING);
        if (rightSeekBar != null && getResources() != null) {
            int defaultRightPadding = getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_padding_end);
            rightSeekBar.setDefaultValue(defaultRightPadding, true);
        }

        CustomSeekBarPreference topSeekbar = findCachedPreference(KEY_STATUSBAR_TOP_PADDING);
        if (topSeekbar != null && getResources() != null) {
            int defaultTopPadding = getResources().getDimensionPixelSize(com.android.internal.R.dimen.status_bar_padding_top);
            topSeekbar.setDefaultValue(defaultTopPadding, true);
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
            new BaseSearchIndexProvider(R.xml.rising_settings_status_bar) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
