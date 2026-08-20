/*
 * Copyright (C) 2016-2026 crDroid Android Project
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

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.colorpicker.ColorPickerPreference
import com.android.settings.utils.DeviceUtils

class PulseSettings : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private val TAG = PulseSettings::class.java.simpleName

        private const val KEY_PULSE_BASS_HAPTICS = "pulse_bass_haptics"
        private const val KEY_PULSE_RENDERER = "pulse_renderer"
        private const val KEY_PULSE_COLOR = "pulse_color"
        private const val KEY_PULSE_CUSTOM_COLOR = "pulse_custom_color"
        private const val KEY_PULSE_CAPTURE_MODE = "pulse_capture_mode"
        private const val KEY_PULSE_ROUND_OUTPUT = "pulse_rounded_bars"
    }

    private lateinit var mPulseRenderer: ListPreference
    private lateinit var mPulseColor: ListPreference
    private lateinit var mPulseBassHaptics: ListPreference
    private lateinit var mPulseCaptureMode: ListPreference
    private lateinit var mPulseCustomColor: ColorPickerPreference
    private lateinit var mPulseRoundOutput: SwitchPreferenceCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.pulse_settings)

        mPulseRenderer = findCachedPreference<ListPreference>(KEY_PULSE_RENDERER)!!
        mPulseColor = findCachedPreference<ListPreference>(KEY_PULSE_COLOR)!!
        mPulseCustomColor = findCachedPreference<ColorPickerPreference>(KEY_PULSE_CUSTOM_COLOR)!!
        mPulseCaptureMode = findCachedPreference<ListPreference>(KEY_PULSE_CAPTURE_MODE)!!
        mPulseBassHaptics = findCachedPreference<ListPreference>(KEY_PULSE_BASS_HAPTICS)!!
        mPulseRoundOutput = findCachedPreference<SwitchPreferenceCompat>(KEY_PULSE_ROUND_OUTPUT)!!

        mPulseRenderer.onPreferenceChangeListener = this
        updatePreferenceVisibility(getCurrentRenderer(), getCurrentColorMode(), getCurrentCaptureMode())

        mPulseColor.onPreferenceChangeListener = this
        updatePreferenceVisibility(getCurrentRenderer(), getCurrentColorMode(), getCurrentCaptureMode())

        val hapticAvailable = DeviceUtils.hasVibrator(requireContext())
        if (!hapticAvailable) {
            mPulseBassHaptics.isVisible = false
        }

        mPulseCaptureMode.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference) {
            mPulseRenderer -> {
                val value = newValue as String
                updatePreferenceVisibility(value, getCurrentColorMode(), getCurrentCaptureMode())
                return true
            }
            mPulseColor -> {
                val value = newValue as String
                updatePreferenceVisibility(getCurrentRenderer(), value, getCurrentCaptureMode())
                return true
            }
            mPulseCaptureMode -> {
                val value = newValue as String
                updatePreferenceVisibility(getCurrentRenderer(), getCurrentColorMode(), value)
                return true
            }
        }
        return false
    }

    private fun updatePreferenceVisibility(
        rendererValue: String?,
        colorValue: String?,
        captureModeValue: String?
    ) {
        if (captureModeValue != null) {
            setBassHapticPreference(captureModeValue != "1")
        }

        if (rendererValue != null) {
            var supportsColoring = false
            var supportsRounding = false
            when (rendererValue) {
                "minimal", "solid" -> {
                    supportsRounding = true
                    supportsColoring = true
                }
                "fading", "matrix", "neon", "particle",
                "sparkle", "waveform", "dotwave" -> {
                    supportsColoring = true
                }
            }
            mPulseRoundOutput.isVisible = supportsRounding
            mPulseColor.isVisible = supportsColoring
            mPulseCustomColor.isVisible = supportsColoring && colorValue == "custom"
        }
    }

    private fun getCurrentRenderer(): String? {
        return Settings.Secure.getStringForUser(
                requireContext().contentResolver,
                Settings.Secure.PULSE_RENDERER,
                UserHandle.USER_CURRENT)
    }

    private fun getCurrentColorMode(): String? {
        return Settings.Secure.getStringForUser(
                requireContext().contentResolver,
                Settings.Secure.PULSE_COLOR,
                UserHandle.USER_CURRENT)
    }

    private fun getCurrentCaptureMode(): String? {
        return Settings.Secure.getStringForUser(
                requireContext().contentResolver,
                Settings.Secure.PULSE_CAPTURE_MODE,
                UserHandle.USER_CURRENT)
    }

    private fun setBassHapticPreference(enabled: Boolean) {
        mPulseBassHaptics.isEnabled = enabled
        if (enabled) {
            mPulseBassHaptics.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        } else {
            mPulseBassHaptics.summaryProvider = null
            mPulseBassHaptics.setSummary(R.string.pulse_bass_haptics_disabled_amplitude)
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
