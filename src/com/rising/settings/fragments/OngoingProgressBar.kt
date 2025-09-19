package com.rising.settings.fragments

import android.content.Context
import android.os.Bundle

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class OngoingProgressBar : OptimizedSettingsFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.ongoing_progress_settings)
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    companion object {
        const val TAG = "OngoingProgressBar"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.ongoing_progress_settings) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context)
                return keys
            }
        }
    }
}
