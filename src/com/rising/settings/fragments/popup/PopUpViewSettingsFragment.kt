/*
 * Copyright (C) 2023-2024 The Nameless-AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.rising.settings.fragments.popup

import android.os.Bundle
import androidx.preference.Preference

import com.android.internal.logging.nano.MetricsProto

import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.SystemSettingListPreference
import com.android.settings.preferences.SystemSettingSwitchPreference

class PopUpViewSettingsFragment : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val KEY_KEEP_MUTE = "pop_up_keep_mute_in_mini"
        private const val KEY_SINGLE_TAP_ACTION = "pop_up_single_tap_action"
        private const val KEY_DOUBLE_TAP_ACTION = "pop_up_double_tap_action"
        private const val KEY_NOTIFICATION_PORTRAIT = "pop_up_notification_jump_portrait"
        private const val KEY_NOTIFICATION_LANDSCAPE = "pop_up_notification_jump_landscape"
        private const val KEY_NOTIFICATION_BLACKLIST = "pop_up_notification_blacklist"
    }

    private var mKeepMuteInMini: SystemSettingSwitchPreference? = null
    private var mSingleTapAction: SystemSettingListPreference? = null
    private var mDoubleTapAction: SystemSettingListPreference? = null
    private var mNotifPortrait: SystemSettingSwitchPreference? = null
    private var mNotifLandscape: SystemSettingSwitchPreference? = null
    private var mNotifBlacklist: Preference? = null

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        addPreferencesFromResource(R.xml.pop_up_view_settings)

        mKeepMuteInMini = findCachedPreference(KEY_KEEP_MUTE) as? SystemSettingSwitchPreference
        mSingleTapAction = findCachedPreference(KEY_SINGLE_TAP_ACTION) as? SystemSettingListPreference
        mDoubleTapAction = findCachedPreference(KEY_DOUBLE_TAP_ACTION) as? SystemSettingListPreference
        mNotifPortrait = findCachedPreference(KEY_NOTIFICATION_PORTRAIT) as? SystemSettingSwitchPreference
        mNotifLandscape = findCachedPreference(KEY_NOTIFICATION_LANDSCAPE) as? SystemSettingSwitchPreference
        mNotifBlacklist = findCachedPreference(KEY_NOTIFICATION_BLACKLIST)

        mSingleTapAction?.onPreferenceChangeListener = this
        mDoubleTapAction?.onPreferenceChangeListener = this
        mNotifPortrait?.onPreferenceChangeListener = this
        mNotifLandscape?.onPreferenceChangeListener = this

        mNotifBlacklist?.isEnabled = (mNotifPortrait?.isChecked == true) || (mNotifLandscape?.isChecked == true)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference) {
            mSingleTapAction -> {
                val index = (newValue as? String)?.toIntOrNull() ?: 0
                mSingleTapAction?.entries?.getOrNull(index)?.let { entry ->
                    mSingleTapAction?.summary = entry
                }
            }
            mDoubleTapAction -> {
                val index = (newValue as? String)?.toIntOrNull() ?: 0
                mDoubleTapAction?.entries?.getOrNull(index)?.let { entry ->
                    mDoubleTapAction?.summary = entry
                }
            }
            mNotifPortrait -> {
                mNotifBlacklist?.isEnabled = (newValue as? Boolean == true) || (mNotifLandscape?.isChecked == true)
            }
            mNotifLandscape -> {
                mNotifBlacklist?.isEnabled = (mNotifPortrait?.isChecked == true) || (newValue as? Boolean == true)
            }
        }
        return true
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
