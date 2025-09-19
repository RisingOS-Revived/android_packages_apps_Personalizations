/*
 * Copyright (C) 2023 the risingOS Android Project
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

package com.rising.settings

import android.content.Context
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.Lifecycle

class PersonalizationsFragment : DashboardFragment() {

    companion object {
        const val CATEGORY_KEY = "com.android.settings.category.ia.personalizations"
        private const val LOG_TAG = "Personalization"
        
        /**
         * For Search.
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_dashboard) {
            override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
                return buildPreferenceControllers(context, null, null)
            }
        }
        
        private fun buildPreferenceControllers(
            context: Context, 
            fragment: PersonalizationsFragment?, 
            lifecycle: Lifecycle?
        ): List<AbstractPreferenceController> {
            val controllers = ArrayList<AbstractPreferenceController>()
            controllers.add(PersonalizationSettingsController(context))
            return controllers
        }
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.rising_dashboard
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun getHelpResource(): Int {
        return R.string.help_uri_about
    }
    
    override fun onStart() {
        super.onStart()
    }
    
    override fun getLogTag(): String {
        return LOG_TAG
    }
    
    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
        return buildPreferenceControllers(context, this, settingsLifecycle)
    }
}
