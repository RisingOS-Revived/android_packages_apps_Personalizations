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
package com.rising.settings.fragments

import android.content.Context
import android.os.Bundle
import android.os.SystemProperties
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settings.preferences.CustomSeekBarPreference
import com.android.settings.utils.SystemRestartUtils

@SearchIndexable
class Wallpaper : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private var mBlurWpPref: Preference? = null
    private var mBlurWpStylePref: Preference? = null
    private var mEffectTypePref: Preference? = null
    private var mEffectTargetPref: Preference? = null
    private var mSaturationPref: Preference? = null
    private var mPixelationPref: Preference? = null
    private var mVignettePref: Preference? = null
    private var mPosterizePref: Preference? = null
    private var mDimPref: Preference? = null
    private var mDimLvlPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.rising_settings_wallpaper)

        activity ?: return

        mBlurWpPref = findPreference<Preference>(KEY_BLUR_ENABLED)?.also {
            it.onPreferenceChangeListener = this
        }

        mBlurWpStylePref = findPreference<Preference>(KEY_BLUR_TYPE)?.also {
            it.onPreferenceChangeListener = this
        }

        mEffectTypePref = findPreference<Preference>(KEY_EFFECT_TYPE)?.also {
            it.onPreferenceChangeListener = this
        }

        mEffectTargetPref = findPreference(KEY_EFFECT_TARGET)

        mSaturationPref = findPreference<Preference>(KEY_SATURATION)?.also {
            it.onPreferenceChangeListener = this
        }

        mPixelationPref = findPreference<Preference>(KEY_PIXELATION)?.also {
            it.onPreferenceChangeListener = this
        }

        mVignettePref = findPreference<Preference>(KEY_VIGNETTE)?.also {
            it.onPreferenceChangeListener = this
        }

        mPosterizePref = findPreference<Preference>(KEY_POSTERIZE)?.also {
            it.onPreferenceChangeListener = this
        }

        mDimPref = findPreference<Preference>(KEY_DIM_ENABLED)?.also {
            it.onPreferenceChangeListener = this
        }

        mDimLvlPref = findPreference(KEY_DIM_LEVEL)

        updateEffectDependencies()
        updateBlurDependencies()
        updateDimDependencies()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val context = activity ?: return false

        return when (preference) {
            mBlurWpPref, mBlurWpStylePref, mEffectTypePref, mEffectTargetPref,
            mSaturationPref, mPixelationPref, mVignettePref, mPosterizePref,
            mDimPref, mDimLvlPref -> {
                when (preference) {
                    mDimLvlPref -> SystemProperties.set(KEY_DIM_LEVEL, newValue.toString())
                    mSaturationPref -> SystemProperties.set(KEY_SATURATION, newValue.toString())
                    mPixelationPref -> SystemProperties.set(KEY_PIXELATION, newValue.toString())
                    mVignettePref -> SystemProperties.set(KEY_VIGNETTE, newValue.toString())
                    mPosterizePref -> SystemProperties.set(KEY_POSTERIZE, newValue.toString())
                    else -> {} // No-op for other preferences
                }

                when (preference) {
                    mEffectTypePref -> updateEffectDependencies()
                    mBlurWpPref -> updateBlurDependencies()
                    mDimPref -> updateDimDependencies()
                    else -> {} // No-op
                }

                SystemRestartUtils.showSystemUIRestartDialog(context)
                true
            }
            else -> false
        }
    }

    private fun updateEffectDependencies() {
        val effectType = SystemProperties.getInt(KEY_EFFECT_TYPE, 0)
        mEffectTargetPref?.isVisible = effectType > 0
        mVignettePref?.isVisible = effectType == 2
        mPixelationPref?.isVisible = effectType == 3
        mSaturationPref?.isVisible = effectType == 4
        mPosterizePref?.isVisible = effectType == 10
    }

    private fun updateBlurDependencies() {
        val blurEnabled = SystemProperties.getInt(KEY_BLUR_ENABLED, 0)
        mBlurWpStylePref?.isVisible = blurEnabled > 0
    }

    private fun updateDimDependencies() {
        val dimEnabled = SystemProperties.getInt(KEY_DIM_ENABLED, 0)
        mDimLvlPref?.isVisible = dimEnabled > 0
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN

    companion object {
        const val TAG = "Wallpaper"

        private const val KEY_BLUR_ENABLED = "persist.sys.wallpaper.blur_enabled"
        private const val KEY_BLUR_TYPE = "persist.sys.wallpaper.blur_type"
        private const val KEY_EFFECT_TYPE = "persist.sys.wallpaper.effect_type"
        private const val KEY_EFFECT_TARGET = "persist.sys.wallpaper.effect_target"
        private const val KEY_SATURATION = "persist.sys.wallpaper.saturation_level"
        private const val KEY_PIXELATION = "persist.sys.wallpaper.pixelation_size"
        private const val KEY_VIGNETTE = "persist.sys.wallpaper.vignette_intensity"
        private const val KEY_POSTERIZE = "persist.sys.wallpaper.posterize_levels"
        private const val KEY_DIM_ENABLED = "persist.sys.wallpaper.dim_enabled"
        private const val KEY_DIM_LEVEL = "persist.sys.wallpaper.dim_level"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.rising_settings_wallpaper) {
                override fun getNonIndexableKeys(context: Context): List<String> {
                    return super.getNonIndexableKeys(context)
                }
            }
    }
}
