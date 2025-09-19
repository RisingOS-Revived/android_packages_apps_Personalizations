/*
 * Copyright (C) 2016-2024 crDroid Android Project
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
package com.rising.settings.fragments.sound

import android.content.Context
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.res.Resources
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreferenceCompat

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.colorpicker.ColorPickerPreference

class PulseSettings : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private val TAG = PulseSettings::class.java.simpleName

        private const val LOCKSCREEN_PULSE_ENABLED_KEY = "lockscreen_pulse_enabled"
        private const val AMBIENT_PULSE_ENABLED_KEY = "ambient_pulse_enabled"
        private const val PULSE_SMOOTHING_KEY = "pulse_smoothing_enabled"
        private const val PULSE_COLOR_MODE_KEY = "pulse_color_mode"
        private const val PULSE_COLOR_MODE_CHOOSER_KEY = "pulse_color_user"
        private const val PULSE_COLOR_MODE_LAVA_SPEED_KEY = "pulse_lavalamp_speed"
        private const val PULSE_RENDER_CATEGORY_SOLID = "pulse_2"
        private const val PULSE_RENDER_CATEGORY_FADING = "pulse_fading_bars_category"
        private const val PULSE_RENDER_MODE_KEY = "pulse_render_style"
        private const val RENDER_STYLE_FADING_BARS = 0
        private const val RENDER_STYLE_SOLID_LINES = 1
        private const val COLOR_TYPE_ACCENT = 0
        private const val COLOR_TYPE_USER = 1
        private const val COLOR_TYPE_LAVALAMP = 2
        private const val COLOR_TYPE_AUTO = 3

        private const val PULSE_SETTINGS_FOOTER = "pulse_settings_footer"
    }

    private lateinit var mLockscreenPulse: SwitchPreferenceCompat
    private lateinit var mAmbientPulse: SwitchPreferenceCompat
    private lateinit var mPulseSmoothing: SwitchPreferenceCompat
    private lateinit var mRenderMode: Preference
    private lateinit var mColorModePref: ListPreference
    private lateinit var mColorPickerPref: ColorPickerPreference
    private lateinit var mLavaSpeedPref: Preference
    private lateinit var mFooterPref: Preference

    private lateinit var mFadingBarsCat: PreferenceCategory
    private lateinit var mSolidBarsCat: PreferenceCategory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.pulse_settings)

        val resolver = requireContext().contentResolver

        mLockscreenPulse = findCachedPreference<SwitchPreferenceCompat>(LOCKSCREEN_PULSE_ENABLED_KEY)!!
        val lockscreenPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.LOCKSCREEN_PULSE_ENABLED, 1, UserHandle.USER_CURRENT) != 0
        mLockscreenPulse.isChecked = lockscreenPulse
        mLockscreenPulse.onPreferenceChangeListener = this

        mAmbientPulse = findCachedPreference<SwitchPreferenceCompat>(AMBIENT_PULSE_ENABLED_KEY)!!
        val ambientPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0
        mAmbientPulse.isChecked = ambientPulse
        mAmbientPulse.onPreferenceChangeListener = this

        mColorModePref = findCachedPreference<ListPreference>(PULSE_COLOR_MODE_KEY)!!
        mColorPickerPref = findCachedPreference<ColorPickerPreference>(PULSE_COLOR_MODE_CHOOSER_KEY)!!
        mLavaSpeedPref = findCachedPreference(PULSE_COLOR_MODE_LAVA_SPEED_KEY)!!
        mColorModePref.onPreferenceChangeListener = this

        mRenderMode = findCachedPreference(PULSE_RENDER_MODE_KEY)!!
        mRenderMode.onPreferenceChangeListener = this

        mFadingBarsCat = findCachedPreference<PreferenceCategory>(PULSE_RENDER_CATEGORY_FADING)!!
        mSolidBarsCat = findCachedPreference<PreferenceCategory>(PULSE_RENDER_CATEGORY_SOLID)!!

        mPulseSmoothing = findCachedPreference<SwitchPreferenceCompat>(PULSE_SMOOTHING_KEY)!!

        mFooterPref = findCachedPreference(PULSE_SETTINGS_FOOTER)!!
        mFooterPref.setTitle(R.string.pulse_help_policy_notice_summary)

        updateAllPrefs()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = requireContext().contentResolver
        when (preference) {
            mLockscreenPulse -> {
                val value = newValue as Boolean
                Settings.Secure.putIntForUser(resolver,
                    Settings.Secure.LOCKSCREEN_PULSE_ENABLED, if (value) 1 else 0, UserHandle.USER_CURRENT)
                updateAllPrefs()
                return true
            }
            mAmbientPulse -> {
                val value = newValue as Boolean
                Settings.Secure.putIntForUser(resolver,
                    Settings.Secure.AMBIENT_PULSE_ENABLED, if (value) 1 else 0, UserHandle.USER_CURRENT)
                updateAllPrefs()
                return true
            }
            mColorModePref -> {
                updateColorPrefs(newValue.toString().toInt())
                return true
            }
            mRenderMode -> {
                updateRenderCategories(newValue.toString().toInt())
                return true
            }
        }
        return false
    }

    private fun updateAllPrefs() {
        val resolver = getSafeContext()?.contentResolver

        val lockscreenPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.LOCKSCREEN_PULSE_ENABLED, 1, UserHandle.USER_CURRENT) != 0

        val ambientPulse = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.AMBIENT_PULSE_ENABLED, 0, UserHandle.USER_CURRENT) != 0

        mPulseSmoothing.isEnabled = lockscreenPulse || ambientPulse

        mColorModePref.isEnabled = lockscreenPulse || ambientPulse
        if (lockscreenPulse || ambientPulse) {
            val colorMode = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_COLOR_MODE, COLOR_TYPE_LAVALAMP, UserHandle.USER_CURRENT)
            updateColorPrefs(colorMode)
        } else {
            mColorPickerPref.isEnabled = false
            mLavaSpeedPref.isEnabled = false
        }

        mRenderMode.isEnabled = lockscreenPulse || ambientPulse
        if (lockscreenPulse || ambientPulse) {
            val renderMode = Settings.Secure.getIntForUser(resolver,
                Settings.Secure.PULSE_RENDER_STYLE, RENDER_STYLE_SOLID_LINES, UserHandle.USER_CURRENT)
            updateRenderCategories(renderMode)
        } else {
            mFadingBarsCat.isEnabled = false
            mSolidBarsCat.isEnabled = false
        }

        mFooterPref.isEnabled = lockscreenPulse || ambientPulse
    }

    private fun updateColorPrefs(value: Int) {
        when (value) {
            COLOR_TYPE_ACCENT -> {
                mColorPickerPref.isEnabled = false
                mLavaSpeedPref.isEnabled = false
            }
            COLOR_TYPE_USER -> {
                mColorPickerPref.isEnabled = true
                mLavaSpeedPref.isEnabled = false
            }
            COLOR_TYPE_LAVALAMP -> {
                mColorPickerPref.isEnabled = false
                mLavaSpeedPref.isEnabled = true
            }
            COLOR_TYPE_AUTO -> {
                mColorPickerPref.isEnabled = false
                mLavaSpeedPref.isEnabled = false
            }
        }
    }

    private fun updateRenderCategories(mode: Int) {
        mFadingBarsCat.isEnabled = mode == RENDER_STYLE_FADING_BARS
        mSolidBarsCat.isEnabled = mode == RENDER_STYLE_SOLID_LINES
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
