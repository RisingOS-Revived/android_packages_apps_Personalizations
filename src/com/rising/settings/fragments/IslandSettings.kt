/*
 * Copyright (C) 2019-2024 The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.rising.settings.fragments

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class IslandSettings : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.island_settings)

        val context = getSafeContext() ?: return
        val resolver = context.contentResolver
        val prefScreen = preferenceScreen
        val resources = context.resources
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val context = getSafeContext() ?: return false
        val resolver = context.contentResolver
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }

    companion object {
        private const val TAG = "IslandSettings"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.island_settings) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context)
                val resources = context.resources
                return keys
            }
        }
    }
}
