/*
 * Copyright (C) 2021-2024 crDroid Android Project
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
package com.rising.settings.fragments

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.preferences.CustomSeekBarPreference
import com.android.settings.preferences.SecureSettingListPreference
import com.android.settings.preferences.SecureSettingSwitchPreference
import com.android.settings.preferences.colorpicker.ColorPickerPreference
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

@SearchIndexable
class MonetSettings : DashboardFragment(), OnPreferenceChangeListener {

    override fun getLogTag(): String {
        return TAG
    }

    companion object {
        private const val TAG = "MonetSettings"
        private const val OVERLAY_CATEGORY_ACCENT_COLOR = "android.theme.customization.accent_color"
        private const val OVERLAY_CATEGORY_SYSTEM_PALETTE = "android.theme.customization.system_palette"
        private const val OVERLAY_CATEGORY_THEME_STYLE = "android.theme.customization.theme_style"
        private const val OVERLAY_CATEGORY_BG_COLOR = "android.theme.customization.bg_color"
        private const val OVERLAY_COLOR_SOURCE = "android.theme.customization.color_source"
        private const val OVERLAY_COLOR_BOTH = "android.theme.customization.color_both"
        private const val OVERLAY_LUMINANCE_FACTOR = "android.theme.customization.luminance_factor"
        private const val OVERLAY_CHROMA_FACTOR = "android.theme.customization.chroma_factor"
        private const val OVERLAY_WHOLE_PALETTE = "android.theme.customization.whole_palette"
        private const val OVERLAY_TINT_BACKGROUND = "android.theme.customization.tint_background"
        private const val COLOR_SOURCE_PRESET = "preset"
        private const val COLOR_SOURCE_HOME = "home_wallpaper"
        private const val COLOR_SOURCE_LOCK = "lock_wallpaper"
        private const val TIMESTAMP_FIELD = "_applied_timestamp"

        private const val PREF_THEME_STYLE = "theme_style"
        private const val PREF_COLOR_SOURCE = "color_source"
        private const val PREF_ACCENT_COLOR = "accent_color"
        private const val PREF_ACCENT_BACKGROUND = "accent_background"
        private const val PREF_BG_COLOR = "bg_color"
        private const val PREF_LUMINANCE_FACTOR = "luminance_factor"
        private const val PREF_CHROMA_FACTOR = "chroma_factor"
        private const val PREF_WHOLE_PALETTE = "whole_palette"
        private const val PREF_TINT_BACKGROUND = "tint_background"

        private const val DEFAULT_COLOR = 0xFF1b6ef3.toInt()
        
        // JSON cache timeout constant
        private const val JSON_CACHE_TIMEOUT = 1000L // 1 second cache

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.monet_engine)
    }

    private var mThemeStylePref: SecureSettingListPreference? = null
    private var mColorSourcePref: SecureSettingListPreference? = null
    private var mAccentColorPref: ColorPickerPreference? = null
    private var mAccentBackgroundPref: SecureSettingSwitchPreference? = null
    private var mBgColorPref: ColorPickerPreference? = null
    private var mLuminancePref: CustomSeekBarPreference? = null
    private var mChromaPref: CustomSeekBarPreference? = null
    private var mWholePalettePref: SecureSettingSwitchPreference? = null
    private var mTintBackgroundPref: SecureSettingSwitchPreference? = null

    private var mAccentColorValue: Int = 0
    private var mBgColorValue: Int = 0

    private var mSharedPreferences: SharedPreferences? = null
    
    // Cache for JSON operations to prevent redundant parsing
    private var mCachedSettingsJson: JSONObject? = null
    private var mLastJsonUpdateTime: Long = 0
    
    // Cache for preference states
    private val mPreferenceCache = ConcurrentHashMap<String, Any>()

    override fun getPreferenceScreenResId(): Int {
        return R.xml.monet_engine
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mThemeStylePref = findPreference(PREF_THEME_STYLE)
        mColorSourcePref = findPreference(PREF_COLOR_SOURCE)
        mAccentColorPref = findPreference(PREF_ACCENT_COLOR)
        mAccentBackgroundPref = findPreference(PREF_ACCENT_BACKGROUND)
        mBgColorPref = findPreference(PREF_BG_COLOR)
        mLuminancePref = findPreference(PREF_LUMINANCE_FACTOR)
        mChromaPref = findPreference(PREF_CHROMA_FACTOR)
        mWholePalettePref = findPreference(PREF_WHOLE_PALETTE)
        mTintBackgroundPref = findPreference(PREF_TINT_BACKGROUND)
        mSharedPreferences = activity?.getSharedPreferences(TAG, Context.MODE_PRIVATE)

        updatePreferences()

        mThemeStylePref?.onPreferenceChangeListener = this
        mColorSourcePref?.onPreferenceChangeListener = this
        mAccentColorPref?.onPreferenceChangeListener = this
        mAccentBackgroundPref?.onPreferenceChangeListener = this
        mBgColorPref?.onPreferenceChangeListener = this
        mLuminancePref?.onPreferenceChangeListener = this
        mChromaPref?.onPreferenceChangeListener = this
        mWholePalettePref?.onPreferenceChangeListener = this
        mTintBackgroundPref?.onPreferenceChangeListener = this
    }

    override fun onResume() {
        super.onResume()
        // Update preferences on resume
        updatePreferences()
    }

    private fun updatePreferences() {
        val overlayPackageJson = Settings.Secure.getStringForUser(
                activity?.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.USER_CURRENT)
        
        if (!overlayPackageJson.isNullOrEmpty()) {
            try {
                val obj = JSONObject(overlayPackageJson)
                val style = obj.optString(OVERLAY_CATEGORY_THEME_STYLE, "TONAL_SPOT")
                val source = obj.optString(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_HOME)
                
                val color = if (obj.has(OVERLAY_CATEGORY_SYSTEM_PALETTE)) {
                    val colorStr = obj.optString(OVERLAY_CATEGORY_SYSTEM_PALETTE)
                    mAccentColorValue = ColorPickerPreference.convertToColorInt(colorStr)
                    colorStr
                } else {
                    mAccentColorValue = mSharedPreferences?.getInt(PREF_ACCENT_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR
                    ColorPickerPreference.convertToRGB(mAccentColorValue).replace("#", "")
                }
                
                val hasBGColor = obj.has(OVERLAY_CATEGORY_BG_COLOR)
                mAccentBackgroundPref?.isChecked = mSharedPreferences?.getBoolean(PREF_ACCENT_BACKGROUND, hasBGColor) ?: hasBGColor
                
                mBgColorValue = if (hasBGColor) {
                    obj.optInt(OVERLAY_CATEGORY_BG_COLOR)
                } else {
                    mSharedPreferences?.getInt(PREF_BG_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR
                }
                
                val both = if (obj.has(OVERLAY_COLOR_BOTH)) {
                    obj.optInt(OVERLAY_COLOR_BOTH) == 1
                } else {
                    false
                }
                
                val wholePalette = obj.optInt(OVERLAY_WHOLE_PALETTE, 0) == 1
                val tintBG = obj.optInt(OVERLAY_TINT_BACKGROUND, 0) == 1
                val lumin = obj.optDouble(OVERLAY_LUMINANCE_FACTOR, 1.0).toFloat()
                val chroma = obj.optDouble(OVERLAY_CHROMA_FACTOR, 1.0).toFloat()
                
                // Style handling
                var styleUpdated = false
                if (style.isNotEmpty()) {
                    mThemeStylePref?.entryValues?.forEach { value ->
                        if (value.toString() == style) {
                            styleUpdated = true
                            return@forEach
                        }
                    }
                    if (styleUpdated) {
                        updateListByValue(mThemeStylePref, style)
                    }
                }
                if (!styleUpdated) {
                    mThemeStylePref?.entryValues?.get(0)?.toString()?.let { defaultStyle ->
                        updateListByValue(mThemeStylePref, defaultStyle)
                    }
                }
                
                // Color handling
                val sourceVal = if (source.isEmpty() || (source == COLOR_SOURCE_HOME && both)) "both" else source
                updateListByValue(mColorSourcePref, sourceVal)
                updateAccentEnablement(sourceVal)
                
                // Set preview color irrespective it is enabled
                if (color.isNotEmpty()) {
                    mAccentColorPref?.setNewPreviewColor(mAccentColorValue)
                }
                mBgColorPref?.setNewPreviewColor(mBgColorValue)
                
                // Luminance and chroma
                val luminV = when {
                    lumin > 1.0 -> Math.round((lumin - 1f) * 100f)
                    lumin < 1.0 -> -1 * Math.round((1f - lumin) * 100f)
                    else -> 0
                }
                mLuminancePref?.value = luminV
                
                val chromaV = when {
                    chroma > 1.0 -> Math.round((chroma - 1f) * 100f)
                    chroma < 1.0 -> -1 * Math.round((1f - chroma) * 100f)
                    else -> 0
                }
                mChromaPref?.value = chromaV
                
                mWholePalettePref?.isChecked = wholePalette
                mTintBackgroundPref?.isChecked = tintBG
                
            } catch (e: JSONException) {
                // Ignored
            } catch (e: IllegalArgumentException) {
                // Ignored
            }
        } else {
            // Reflect default values in every preference
            mThemeStylePref?.setValueIndex(0)
            mColorSourcePref?.setValueIndex(0)
            mLuminancePref?.value = 0
            mChromaPref?.value = 0
            mAccentBackgroundPref?.isChecked = false
            mWholePalettePref?.isChecked = false
            mTintBackgroundPref?.isChecked = false
            updateAccentEnablement("both")
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val resolver = activity?.contentResolver ?: return false
        
        return when (preference) {
            mThemeStylePref -> {
                val value = newValue as String
                setStyleValue(value)
                updateListByValue(mThemeStylePref, value, false)
                true
            }
            
            mColorSourcePref -> {
                val value = newValue as String
                setSourceValue(value)
                updateListByValue(mColorSourcePref, value, false)
                updateAccentEnablement(value)
                true
            }
            
            mAccentColorPref -> {
                mAccentColorValue = newValue as Int
                mSharedPreferences?.edit()?.putInt(PREF_ACCENT_COLOR, mAccentColorValue)?.apply()
                setColorValue()
                true
            }
            
            mAccentBackgroundPref -> {
                val value = newValue as Boolean
                mAccentBackgroundPref?.isChecked = value
                mSharedPreferences?.edit()?.putBoolean(PREF_ACCENT_BACKGROUND, value)?.apply()
                setBgColorValue()
                true
            }
            
            mBgColorPref -> {
                mBgColorValue = newValue as Int
                mSharedPreferences?.edit()?.putInt(PREF_BG_COLOR, mBgColorValue)?.apply()
                setBgColorValue()
                true
            }
            
            mLuminancePref -> {
                val value = newValue as Int
                setLuminanceValue(value)
                true
            }
            
            mChromaPref -> {
                val value = newValue as Int
                setChromaValue(value)
                true
            }
            
            mWholePalettePref -> {
                val value = newValue as Boolean
                setWholePaletteValue(value)
                true
            }
            
            mTintBackgroundPref -> {
                val value = newValue as Boolean
                setTintBackgroundValue(value)
                true
            }
            
            else -> false
        }
    }

    private fun updateListByValue(pref: SecureSettingListPreference?, value: String) {
        updateListByValue(pref, value, true)
    }

    private fun updateListByValue(pref: SecureSettingListPreference?, value: String, set: Boolean) {
        if (pref == null) return
        if (set) pref.value = value
        val index = pref.findIndexOfValue(value)
        pref.summary = pref.entries[index]
    }

    private fun updateAccentEnablement(source: String?) {
        val shouldEnable = source == COLOR_SOURCE_PRESET
        mAccentColorPref?.isEnabled = shouldEnable
        mAccentBackgroundPref?.isEnabled = shouldEnable
        setColorValue()
        setBgColorValue()
    }

    private fun getSettingsJson(): JSONObject {
        val currentTime = System.currentTimeMillis()
        
        // Use cached JSON if still valid
        mCachedSettingsJson?.let { cached ->
            if ((currentTime - mLastJsonUpdateTime) < JSON_CACHE_TIMEOUT) {
                return JSONObject(cached.toString()) // Return copy
            }
        }
        
        val overlayPackageJson = Settings.Secure.getStringForUser(
                activity?.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                UserHandle.USER_CURRENT)
        
        val obj = if (overlayPackageJson.isNullOrEmpty()) {
            JSONObject()
        } else {
            JSONObject(overlayPackageJson)
        }
        
        // Update cache
        mCachedSettingsJson = JSONObject(obj.toString())
        mLastJsonUpdateTime = currentTime
        
        return obj
    }

    private fun putSettingsJson(obj: JSONObject) {
        Settings.Secure.putStringForUser(
                activity?.contentResolver,
                Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES,
                obj.toString(), UserHandle.USER_CURRENT)
        
        // Update cache
        try {
            mCachedSettingsJson = JSONObject(obj.toString())
            mLastJsonUpdateTime = System.currentTimeMillis()
        } catch (e: JSONException) {
            // Clear cache on error
            mCachedSettingsJson = null
        }
    }

    private fun setStyleValue(style: String) {
        try {
            val obj = getSettingsJson()
            obj.putOpt(OVERLAY_CATEGORY_THEME_STYLE, style)
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    private fun setSourceValue(source: String) {
        try {
            val obj = getSettingsJson()
            if (source == "both") {
                obj.putOpt(OVERLAY_COLOR_BOTH, 1)
                obj.putOpt(OVERLAY_COLOR_SOURCE, COLOR_SOURCE_HOME)
            } else {
                obj.remove(OVERLAY_COLOR_BOTH)
                obj.putOpt(OVERLAY_COLOR_SOURCE, source)
            }
            obj.putOpt(TIMESTAMP_FIELD, System.currentTimeMillis())
            if (source != COLOR_SOURCE_PRESET) {
                obj.remove(OVERLAY_CATEGORY_ACCENT_COLOR)
                obj.remove(OVERLAY_CATEGORY_SYSTEM_PALETTE)
            }
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    private fun setColorValue() {
        try {
            val obj = getSettingsJson()

            if (mColorSourcePref?.value == COLOR_SOURCE_PRESET) {
                val rgbColor = ColorPickerPreference.convertToRGB(mAccentColorValue).replace("#", "")
                obj.putOpt(OVERLAY_CATEGORY_ACCENT_COLOR, rgbColor)
                obj.putOpt(OVERLAY_CATEGORY_SYSTEM_PALETTE, rgbColor)
            } else {
                obj.remove(OVERLAY_CATEGORY_ACCENT_COLOR)
                obj.remove(OVERLAY_CATEGORY_SYSTEM_PALETTE)
            }
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    private fun setBgColorValue() {
        try {
            val obj = getSettingsJson()
            if (mColorSourcePref?.value == COLOR_SOURCE_PRESET && mAccentBackgroundPref?.isChecked == true) {
                obj.putOpt(OVERLAY_CATEGORY_BG_COLOR, mBgColorValue)
            } else {
                obj.remove(OVERLAY_CATEGORY_BG_COLOR)
            }
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    private fun setLuminanceValue(lumin: Int) {
        try {
            val obj = getSettingsJson()
            if (lumin == 0) {
                obj.remove(OVERLAY_LUMINANCE_FACTOR)
            } else {
                obj.putOpt(OVERLAY_LUMINANCE_FACTOR, 1.0 + (lumin.toDouble() / 100.0))
            }
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    private fun setChromaValue(chroma: Int) {
        try {
            val obj = getSettingsJson()
            if (chroma == 0) {
                obj.remove(OVERLAY_CHROMA_FACTOR)
            } else {
                obj.putOpt(OVERLAY_CHROMA_FACTOR, 1.0 + (chroma.toDouble() / 100.0))
            }
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    private fun setWholePaletteValue(whole: Boolean) {
        try {
            val obj = getSettingsJson()
            if (!whole) {
                obj.remove(OVERLAY_WHOLE_PALETTE)
            } else {
                obj.putOpt(OVERLAY_WHOLE_PALETTE, 1)
            }
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    private fun setTintBackgroundValue(tint: Boolean) {
        try {
            val obj = getSettingsJson()
            if (!tint) {
                obj.remove(OVERLAY_TINT_BACKGROUND)
            } else {
                obj.putOpt(OVERLAY_TINT_BACKGROUND, 1)
            }
            putSettingsJson(obj)
        } catch (e: JSONException) {
            // Ignored
        } catch (e: IllegalArgumentException) {
            // Ignored
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
