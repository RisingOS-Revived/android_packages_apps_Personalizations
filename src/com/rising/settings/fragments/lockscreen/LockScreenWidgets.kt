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
package com.rising.settings.fragments.lockscreen

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.Toast

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreferenceCompat

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.SystemRestartUtils
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.LayoutPreference

@SearchIndexable
class LockScreenWidgets : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        const val TAG = "LockScreenWidgets"

        private const val MAIN_WIDGET_1_KEY = "main_custom_widgets1"
        private const val MAIN_WIDGET_2_KEY = "main_custom_widgets2"
        private const val EXTRA_WIDGET_1_KEY = "custom_widgets1"
        private const val EXTRA_WIDGET_2_KEY = "custom_widgets2"
        private const val EXTRA_WIDGET_3_KEY = "custom_widgets3"
        private const val EXTRA_WIDGET_4_KEY = "custom_widgets4"
        private const val KEY_APPLY_CHANGE_BUTTON = "apply_change_button"

        private const val LOCKSCREEN_WIDGETS_KEY = "lockscreen_widgets"
        private const val LOCKSCREEN_WIDGETS_EXTRAS_KEY = "lockscreen_widgets_extras"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_lockscreen_widgets) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }

    private lateinit var mMainWidget1: Preference
    private lateinit var mMainWidget2: Preference
    private lateinit var mExtraWidget1: Preference
    private lateinit var mExtraWidget2: Preference
    private lateinit var mExtraWidget3: Preference
    private lateinit var mExtraWidget4: Preference
    private lateinit var mApplyChange: Button

    private lateinit var mLockScreenWidgetsEnabledPref: SwitchPreferenceCompat
    private lateinit var mWidgetPreferences: List<Preference>

    private val widgetKeysMap = mutableMapOf<Preference, String>()
    private val initialWidgetKeysMap = mutableMapOf<Preference, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_lockscreen_widgets)

        initializePreferences()
        setupListeners()

        val isLsWidgetsEnabled = Settings.System.getIntForUser(
            activity?.contentResolver,
            "lockscreen_widgets_enabled",
            0,
            UserHandle.USER_CURRENT
        ) != 0

        mLockScreenWidgetsEnabledPref.isChecked = isLsWidgetsEnabled
        showWidgetPreferences(isLsWidgetsEnabled)

        loadInitialPreferences()
        saveInitialPreferences()
        mApplyChange.isEnabled = false
    }

    private fun initializePreferences() {
        mMainWidget1 = findPreference(MAIN_WIDGET_1_KEY)!!
        mMainWidget2 = findPreference(MAIN_WIDGET_2_KEY)!!
        mExtraWidget1 = findPreference(EXTRA_WIDGET_1_KEY)!!
        mExtraWidget2 = findPreference(EXTRA_WIDGET_2_KEY)!!
        mExtraWidget3 = findPreference(EXTRA_WIDGET_3_KEY)!!
        mExtraWidget4 = findPreference(EXTRA_WIDGET_4_KEY)!!

        mWidgetPreferences = listOf(
            mMainWidget1,
            mMainWidget2,
            mExtraWidget1,
            mExtraWidget2,
            mExtraWidget3,
            mExtraWidget4
        )

        mLockScreenWidgetsEnabledPref = findPreference("lockscreen_widgets_enabled")!!

        val layoutPreference = findPreference<LayoutPreference>(KEY_APPLY_CHANGE_BUTTON)!!
        mApplyChange = layoutPreference.findViewById(R.id.apply_change)
    }

    private fun setupListeners() {
        for (widgetPref in mWidgetPreferences) {
            widgetPref.onPreferenceChangeListener = this
            widgetKeysMap[widgetPref] = ""
        }
        mLockScreenWidgetsEnabledPref.onPreferenceChangeListener = this

        mApplyChange.setOnClickListener {
            updateWidgetPreferences()
            saveInitialPreferences()
            mApplyChange.isEnabled = false

            // Restart SystemUI to apply changes
            context?.let { ctx ->
                SystemRestartUtils.restartSystemUI(ctx)
            }
        }
    }

    private fun showWidgetPreferences(isEnabled: Boolean) {
        for (widgetPref in mWidgetPreferences) {
            widgetPref.isVisible = isEnabled
        }
    }

    private fun loadInitialPreferences() {
        val resolver = activity?.contentResolver
        val mainWidgets = Settings.System.getString(resolver, LOCKSCREEN_WIDGETS_KEY)
        setWidgetAndPreferenceValues(mainWidgets, mMainWidget1, mMainWidget2)
        val extraWidgets = Settings.System.getString(resolver, LOCKSCREEN_WIDGETS_EXTRAS_KEY)
        setWidgetAndPreferenceValues(extraWidgets, mExtraWidget1, mExtraWidget2, mExtraWidget3, mExtraWidget4)
    }

    private fun setWidgetAndPreferenceValues(widgets: String?, vararg preferences: Preference) {
        if (widgets == null) {
            return
        }
        val widgetList = widgets.split(",")
        for (i in preferences.indices) {
            if (i < widgetList.size) {
                val value = widgetList[i].trim()
                val pref = preferences[i]
                widgetKeysMap[pref] = value
                if (pref is ListPreference) {
                    pref.value = value
                }
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return when {
            widgetKeysMap.containsKey(preference) -> {
                widgetKeysMap[preference] = newValue.toString()
                mApplyChange.isEnabled = hasChanges()
                true
            }
            preference == mLockScreenWidgetsEnabledPref -> {
                val isEnabled = newValue as Boolean
                showWidgetPreferences(isEnabled)
                mLockScreenWidgetsEnabledPref.isChecked = isEnabled
                true
            }
            else -> false
        }
    }

    private fun updateWidgetPreferences() {
        var mainWidgetsList = listOf(widgetKeysMap[mMainWidget1], widgetKeysMap[mMainWidget2])
        var extraWidgetsList = listOf(
            widgetKeysMap[mExtraWidget1],
            widgetKeysMap[mExtraWidget2],
            widgetKeysMap[mExtraWidget3],
            widgetKeysMap[mExtraWidget4]
        )

        mainWidgetsList = replaceEmptyWithNone(mainWidgetsList)
        extraWidgetsList = replaceEmptyWithNone(extraWidgetsList)

        val mainWidgets = TextUtils.join(",", mainWidgetsList)
        val extraWidgets = TextUtils.join(",", extraWidgetsList)

        val resolver = activity?.contentResolver
        Settings.System.putString(resolver, LOCKSCREEN_WIDGETS_KEY, mainWidgets)
        Settings.System.putString(resolver, LOCKSCREEN_WIDGETS_EXTRAS_KEY, extraWidgets)
    }

    private fun replaceEmptyWithNone(inputList: List<String?>): List<String> {
        return inputList.map { s -> if (TextUtils.isEmpty(s)) "none" else s!! }
    }

    private fun saveInitialPreferences() {
        initialWidgetKeysMap.clear()
        for (widgetPref in mWidgetPreferences) {
            val value = widgetKeysMap[widgetPref] ?: ""
            initialWidgetKeysMap[widgetPref] = value
        }
    }

    private fun hasChanges(): Boolean {
        for ((pref, initialValue) in initialWidgetKeysMap) {
            val currentValue = widgetKeysMap[pref]
            if (!TextUtils.equals(initialValue, currentValue)) {
                return true
            }
        }
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
