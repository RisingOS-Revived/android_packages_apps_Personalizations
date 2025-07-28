/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.rising.settings.fragments.popup;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.android.PopUpSettingsHelper;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.preferences.SystemSettingListPreference;
import com.android.settings.preferences.SystemSettingSwitchPreference;

public class PopUpViewSettingsFragment extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_KEEP_MUTE = "pop_up_keep_mute_in_mini";
    private static final String KEY_SINGLE_TAP_ACTION = "pop_up_single_tap_action";
    private static final String KEY_DOUBLE_TAP_ACTION = "pop_up_double_tap_action";
    private static final String KEY_NOTIFICATION_PORTRAIT = "pop_up_notification_jump_portrait";
    private static final String KEY_NOTIFICATION_LANDSCAPE = "pop_up_notification_jump_landscape";
    private static final String KEY_NOTIFICATION_BLACKLIST = "pop_up_notification_blacklist";


    private SystemSettingSwitchPreference mKeepMuteInMini;
    private SystemSettingListPreference mSingleTapAction;
    private SystemSettingListPreference mDoubleTapAction;
    private SystemSettingSwitchPreference mNotifPortrait;
    private SystemSettingSwitchPreference mNotifLandscape;
    private Preference mNotifBlacklist;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.pop_up_view_settings);


        mKeepMuteInMini = (SystemSettingSwitchPreference) findPreference(KEY_KEEP_MUTE);
        mSingleTapAction = (SystemSettingListPreference) findPreference(KEY_SINGLE_TAP_ACTION);
        mDoubleTapAction = (SystemSettingListPreference) findPreference(KEY_DOUBLE_TAP_ACTION);
        mNotifPortrait = (SystemSettingSwitchPreference) findPreference(KEY_NOTIFICATION_PORTRAIT);
        mNotifLandscape = (SystemSettingSwitchPreference) findPreference(KEY_NOTIFICATION_LANDSCAPE);
        mNotifBlacklist = (Preference) findPreference(KEY_NOTIFICATION_BLACKLIST);

        mSingleTapAction.setOnPreferenceChangeListener(this);

        mDoubleTapAction.setOnPreferenceChangeListener(this);

        mNotifPortrait.setOnPreferenceChangeListener(this);

        mNotifLandscape.setOnPreferenceChangeListener(this);

        mNotifBlacklist.setEnabled(mNotifPortrait.isChecked() || mNotifLandscape.isChecked());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSingleTapAction) {
            mSingleTapAction.setSummary(mSingleTapAction.getEntries()[Integer.parseInt((String) newValue)]);
        } else if (preference == mDoubleTapAction) {
            mDoubleTapAction.setSummary(mDoubleTapAction.getEntries()[Integer.parseInt((String) newValue)]);
        } else if (preference == mNotifPortrait) {
            mNotifBlacklist.setEnabled((Boolean) newValue || mNotifLandscape.isChecked());
        } else if (preference == mNotifLandscape) {
            mNotifBlacklist.setEnabled(mNotifPortrait.isChecked() || (Boolean) newValue);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }
}
