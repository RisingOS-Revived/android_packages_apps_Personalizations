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
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.settings.preferences.colorpicker.ColorPickerPreference
import com.android.settings.utils.DeviceUtils

class PulseSettings : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private val TAG = PulseSettings::class.java.simpleName

        private val KEY_PULSE_SHOW_ON_MEDIA_PLAYER = Settings.Secure.PULSE_SHOW_ON_MEDIA_PLAYER
        private val KEY_PULSE_SHOW_ON_AMBIENT = Settings.Secure.PULSE_SHOW_ON_AMBIENT
        private val KEY_PULSE_BASS_HAPTICS = Settings.Secure.PULSE_BASS_HAPTICS
        private val KEY_PULSE_RENDERER = Settings.Secure.PULSE_RENDERER
        private val KEY_PULSE_COLOR = Settings.Secure.PULSE_COLOR
        private val KEY_PULSE_CUSTOM_COLOR = Settings.Secure.PULSE_CUSTOM_COLOR
        private val KEY_PULSE_CAPTURE_MODE = Settings.Secure.PULSE_CAPTURE_MODE
        private val KEY_PULSE_ROUND_OUTPUT = Settings.Secure.PULSE_ROUNDED_BARS
        private val KEY_PULSE_HEIGHT = Settings.Secure.PULSE_HEIGHT_MULTIPLIER
        private val KEY_PULSE_BAR_COUNT = Settings.Secure.PULSE_BAR_COUNT
    }

    private var mPulseShowOnMediaPlayer: SwitchPreferenceCompat? = null
    private var mPulseShowOnAmbient: SwitchPreferenceCompat? = null
    private lateinit var mPulseRenderer: ListPreference
    private lateinit var mPulseColor: ListPreference
    private lateinit var mPulseBassHaptics: ListPreference
    private lateinit var mPulseCaptureMode: ListPreference
    private lateinit var mPulseCustomColor: ColorPickerPreference
    private lateinit var mPulseRoundOutput: SwitchPreferenceCompat
    private lateinit var mPulseHeight: SeekBarPreference
    private lateinit var mPulseBarCount: SeekBarPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.pulse_settings)

        mPulseShowOnMediaPlayer = findCachedPreference<SwitchPreferenceCompat>(KEY_PULSE_SHOW_ON_MEDIA_PLAYER)
        mPulseShowOnAmbient = findCachedPreference<SwitchPreferenceCompat>(KEY_PULSE_SHOW_ON_AMBIENT)
        mPulseRenderer = findCachedPreference<ListPreference>(KEY_PULSE_RENDERER)!!
        mPulseColor = findCachedPreference<ListPreference>(KEY_PULSE_COLOR)!!
        mPulseCustomColor = findCachedPreference<ColorPickerPreference>(KEY_PULSE_CUSTOM_COLOR)!!
        mPulseCaptureMode = findCachedPreference<ListPreference>(KEY_PULSE_CAPTURE_MODE)!!
        mPulseBassHaptics = findCachedPreference<ListPreference>(KEY_PULSE_BASS_HAPTICS)!!
        mPulseRoundOutput = findCachedPreference<SwitchPreferenceCompat>(KEY_PULSE_ROUND_OUTPUT)!!
        mPulseHeight = findCachedPreference<SeekBarPreference>(KEY_PULSE_HEIGHT)!!
        mPulseBarCount = findCachedPreference<SeekBarPreference>(KEY_PULSE_BAR_COUNT)!!

        mPulseRenderer.onPreferenceChangeListener = this

        mPulseColor.onPreferenceChangeListener = this

        updatePreferenceVisibility()

        val hapticAvailable = DeviceUtils.hasVibrator(requireContext())
        if (!hapticAvailable) {
            mPulseBassHaptics.isVisible = false
        }

        mPulseCaptureMode.onPreferenceChangeListener = this

        mPulseShowOnMediaPlayer?.onPreferenceChangeListener = this
        mPulseShowOnAmbient?.let {
            updateAmbientPreference(!willShowOnMediaPlayer())
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference) {
            mPulseRenderer -> {
                val value = newValue as String
                updatePreferenceVisibility(
                        value,
                        getCurrentColorMode(),
                        getCurrentCaptureMode(),
                        willShowOnMediaPlayer())
                return true
            }
            mPulseColor -> {
                val value = newValue as String
                updatePreferenceVisibility(
                        getCurrentRenderer(),
                        value,
                        getCurrentCaptureMode(),
                        willShowOnMediaPlayer())
                return true
            }
            mPulseCaptureMode -> {
                val value = newValue as String
                updatePreferenceVisibility(
                        getCurrentRenderer(),
                        getCurrentColorMode(),
                        value,
                        willShowOnMediaPlayer())
                return true
            }
            mPulseShowOnMediaPlayer -> {
                val mediaPlayerState = newValue as Boolean
                updateAmbientPreference(!mediaPlayerState)
                updatePreferenceVisibility(
                        getCurrentRenderer(),
                        getCurrentColorMode(),
                        getCurrentCaptureMode(),
                        mediaPlayerState)
                return true
            }
        }
        return false
    }

    private fun updateAmbientPreference(state: Boolean) {
        mPulseShowOnAmbient?.isEnabled = state
        mPulseShowOnAmbient?.setSummary(
                if (state) R.string.pulse_show_on_ambient_summary
                else R.string.pulse_show_on_ambient_disabled_player)
    }

    private fun updatePreferenceVisibility(
        rendererValue: String?,
        colorValue: String?,
        captureModeValue: String?,
        showOnMediaPlayerEnabled: Boolean
    ) {
        if (captureModeValue != null) {
            setBassHapticPreference(captureModeValue != "1")
        }

        if (rendererValue != null) {
            var supportsColoring = false
            var supportsRounding = false
            var heightFixed = false
            var samplesFixed = false
            when (rendererValue) {
                "minimal", "solid" -> {
                    supportsRounding = true
                    supportsColoring = true
                }
                "fading", "matrix", "neon",
                "sparkle", "waveform", "dotwave" -> {
                    supportsColoring = true
                }
                "particle" -> {
                    heightFixed = true
                    samplesFixed = true
                    supportsColoring = true
                }
            }
            mPulseHeight.isVisible = !showOnMediaPlayerEnabled && !heightFixed
            mPulseBarCount.isVisible = !samplesFixed
            mPulseRoundOutput.isVisible = supportsRounding
            mPulseColor.isVisible = supportsColoring
            mPulseCustomColor.isVisible = supportsColoring && colorValue == "custom"
        }
    }

    private fun updatePreferenceVisibility() {
        updatePreferenceVisibility(
                getCurrentRenderer(),
                getCurrentColorMode(),
                getCurrentCaptureMode(),
                willShowOnMediaPlayer())
    }

    private fun getCurrentRenderer(): String? {
        return Settings.Secure.getStringForUser(
                requireContext().contentResolver,
                KEY_PULSE_RENDERER,
                UserHandle.USER_CURRENT)
    }

    private fun getCurrentColorMode(): String? {
        return Settings.Secure.getStringForUser(
                requireContext().contentResolver,
                KEY_PULSE_COLOR,
                UserHandle.USER_CURRENT)
    }

    private fun getCurrentCaptureMode(): String? {
        return Settings.Secure.getStringForUser(
                requireContext().contentResolver,
                KEY_PULSE_CAPTURE_MODE,
                UserHandle.USER_CURRENT)
    }

    private fun willShowOnMediaPlayer(): Boolean {
        return Settings.Secure.getIntForUser(
                requireContext().contentResolver,
                KEY_PULSE_SHOW_ON_MEDIA_PLAYER,
                UserHandle.USER_CURRENT, 0) == 1
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