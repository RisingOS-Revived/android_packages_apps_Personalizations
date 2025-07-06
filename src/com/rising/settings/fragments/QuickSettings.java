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

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.android.ThemeUtils;

import com.android.settings.preferences.CustomSeekBarPreference;
import com.android.settings.preferences.SecureSettingSwitchPreference;

import com.android.settings.utils.SystemRestartUtils;

import java.util.List;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "QuickSettings";
    
    private static final String KEY_QS_UI_STYLE  = "qs_tile_ui_style";
    private static final String KEY_QS_PANEL_STYLE  = "qs_panel_style";
    private static final String KEY_PREF_TILE_ANIM_STYLE = "qs_tile_animation_style";
    private static final String KEY_PREF_TILE_ANIM_DURATION = "qs_tile_animation_duration";
    private static final String KEY_PREF_TILE_ANIM_INTERPOLATOR = "qs_tile_animation_interpolator";
    private static final String KEY_QS_REFACTOR_ENABLED = "qs_refactor_enabled";

    private ListPreference mQsUI;
    private ListPreference mQsPanelStyle;
    private ListPreference mTileAnimationStyle;
    private ListPreference mTileAnimationInterpolator;
    private CustomSeekBarPreference mTileAnimationDuration;
    private Preference mSplitShadePref;
    private SecureSettingSwitchPreference mQsRefactorEnabled;
    
    private ThemeUtils mThemeUtils;
    
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_qs);
        
        final Context mContext = getActivity().getApplicationContext();
        
        mThemeUtils = ThemeUtils.getInstance(getActivity());
        
        mQsUI = (ListPreference) findPreference(KEY_QS_UI_STYLE);
        mQsUI.setOnPreferenceChangeListener(this);

        mQsPanelStyle = (ListPreference) findPreference(KEY_QS_PANEL_STYLE);
        mQsPanelStyle.setOnPreferenceChangeListener(this);

        mSplitShadePref = (Preference) findPreference("qs_split_shade_enabled");
        mSplitShadePref.setOnPreferenceChangeListener(this);

        mQsRefactorEnabled = (SecureSettingSwitchPreference) findPreference(KEY_QS_REFACTOR_ENABLED);
        mQsRefactorEnabled.setOnPreferenceChangeListener(this);

        mTileAnimationStyle = (ListPreference) findPreference(KEY_PREF_TILE_ANIM_STYLE);
        mTileAnimationDuration = (CustomSeekBarPreference) findPreference(KEY_PREF_TILE_ANIM_DURATION);
        mTileAnimationInterpolator = (ListPreference) findPreference(KEY_PREF_TILE_ANIM_INTERPOLATOR);

        mTileAnimationStyle.setOnPreferenceChangeListener(this);

        int tileAnimationStyle = Settings.System.getIntForUser(getActivity().getContentResolver(),
                KEY_PREF_TILE_ANIM_STYLE, 0, UserHandle.USER_CURRENT);
        updateAnimTileStyle(tileAnimationStyle);

        checkQSOverlays(mContext);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();

        if (preference == mQsUI) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.QS_TILE_UI_STYLE, value, UserHandle.USER_CURRENT);
            updateQsStyle(getActivity());
            checkQSOverlays(getActivity());
            return true;
        } else if (preference == mQsPanelStyle) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.QS_PANEL_STYLE, value, UserHandle.USER_CURRENT);
            updateQsPanelStyle(getActivity());
            checkQSOverlays(getActivity());
            return true;
        } else if (preference == mTileAnimationStyle) {
            int value = Integer.parseInt((String) newValue);
            updateAnimTileStyle(value);
            return true;
        } else if (preference == mSplitShadePref) {
            int value = (boolean) newValue ? 1 : 0;
            Settings.System.putIntForUser(resolver,
                   "qs_split_shade_enabled", value, UserHandle.USER_CURRENT);
            updateSplitShadeEnabled(getActivity());
            return true;
        } else if (preference == mQsRefactorEnabled) {
            // QS Refactor setting changed - restart SystemUI
            SystemRestartUtils.restartSystemUI(getContext());
            return true;
        }
        return false;
    }
    
    private void updateAnimTileStyle(int tileAnimationStyle) {
        mTileAnimationDuration.setEnabled(tileAnimationStyle != 0);
        mTileAnimationInterpolator.setEnabled(tileAnimationStyle != 0);
    }
    
    private void updateSplitShadeEnabled(Context context) {
        ContentResolver resolver = context.getContentResolver();
        boolean splitShadeEnabled = Settings.System.getIntForUser(
                resolver,
                "qs_split_shade_enabled" , 0, UserHandle.USER_CURRENT) != 0;
	    String splitShadeStyleCategory = "android.theme.customization.better_qs";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.system.qs.ui.better_qs";
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }
        mHandler.postDelayed(() -> {
            mThemeUtils.setOverlayEnabled(splitShadeStyleCategory, overlayThemeTarget, overlayThemeTarget);
            if (splitShadeEnabled) {
                mThemeUtils.setOverlayEnabled(splitShadeStyleCategory, overlayThemePackage, overlayThemeTarget);
            }
        }, 1250);
    }

    private void updateQsStyle(Context context) {
        ContentResolver resolver = context.getContentResolver();

        boolean isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE , 0, UserHandle.USER_CURRENT) != 0;

	    String qsUIStyleCategory = "android.theme.customization.qs_ui";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.system.qs.ui.A11";

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }

	    // reset all overlays before applying
        mThemeUtils.setOverlayEnabled(qsUIStyleCategory, overlayThemeTarget, overlayThemeTarget);

	    if (isA11Style) {
            mThemeUtils.setOverlayEnabled(qsUIStyleCategory, overlayThemePackage, overlayThemeTarget);
	    }
    }
    
    private void updateQsPanelStyle(Context context) {
        ContentResolver resolver = context.getContentResolver();

        int qsPanelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT);

        String qsPanelStyleCategory = "android.theme.customization.qs_panel";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.systemui";

        switch (qsPanelStyle) {
            case 1:
              overlayThemePackage = "com.android.system.qs.outline";
              break;
            case 2:
            case 3:
              overlayThemePackage = "com.android.system.qs.twotoneaccent";
              break;
            case 4:
              overlayThemePackage = "com.android.system.qs.shaded";
              break;
            case 5:
              overlayThemePackage = "com.android.system.qs.cyberpunk";
              break;
            case 6:
              overlayThemePackage = "com.android.system.qs.neumorph";
              break;
            case 7:
              overlayThemePackage = "com.android.system.qs.reflected";
              break;
            case 8:
              overlayThemePackage = "com.android.system.qs.surround";
              break;
            case 9:
              overlayThemePackage = "com.android.system.qs.thin";
              break;
            default:
              break;
        }

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }

        // reset all overlays before applying
        mThemeUtils.setOverlayEnabled(qsPanelStyleCategory, overlayThemeTarget, overlayThemeTarget);

        if (qsPanelStyle > 0) {
            mThemeUtils.setOverlayEnabled(qsPanelStyleCategory, overlayThemePackage, overlayThemeTarget);
        }
    }

    private void checkQSOverlays(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE , 0, UserHandle.USER_CURRENT);
        int qsPanelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE , 0, UserHandle.USER_CURRENT);

        // Update summaries
        int index = mQsUI.findIndexOfValue(Integer.toString(isA11Style));
        mQsUI.setValue(Integer.toString(isA11Style));
        mQsUI.setSummary(mQsUI.getEntries()[index]);

        index = mQsPanelStyle.findIndexOfValue(Integer.toString(qsPanelStyle));
        mQsPanelStyle.setValue(Integer.toString(qsPanelStyle));
        mQsPanelStyle.setSummary(mQsPanelStyle.getEntries()[index]);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_qs) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
