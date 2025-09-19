/*
 * SPDX-FileCopyrightText: 2022 Project Kaleidoscope
 * SPDX-License-Identifier: Apache-2.0
 */
package com.rising.settings.fragments.statusbar

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class StatusbarLyricSettings : OptimizedSettingsFragment() {

    companion object {
        const val TAG = "StatusbarLyricSettings"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.status_bar_lyric_settings) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context)
                return keys
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.status_bar_lyric_settings)
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
