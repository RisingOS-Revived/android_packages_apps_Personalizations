/*
 * Copyright (C) 2024 crDroid Android Project
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
package com.rising.settings.fragments.quicksettings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.SearchIndexableResource
import android.provider.Settings
import android.widget.Toast

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settingslib.search.SearchIndexable

import com.android.settings.utils.ImageUtils

@SearchIndexable
class QsHeaderImageSettings : OptimizedSettingsFragment(), OnPreferenceChangeListener {

    companion object {
        private const val CUSTOM_HEADER_BROWSE = "custom_header_browse"
        private const val DAYLIGHT_HEADER_PACK = "daylight_header_pack"
        private const val CUSTOM_HEADER_PROVIDER = "qs_header_provider"
        private const val STATUS_BAR_CUSTOM_HEADER = "status_bar_custom_header"
        private const val FILE_HEADER_SELECT = "file_header_select"
        private const val REQUEST_PICK_IMAGE = 0

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider() {
            override fun getXmlResourcesToIndex(context: Context, enabled: Boolean): List<SearchIndexableResource> {
                val result = ArrayList<SearchIndexableResource>()
                val sir = SearchIndexableResource(context)
                sir.xmlResId = R.xml.qs_header_image_settings
                result.add(sir)
                return result
            }

            override fun getNonIndexableKeys(context: Context): List<String> {
                return ArrayList()
            }
        }
    }

    private var mHeaderBrowse: Preference? = null
    private var mDaylightHeaderPack: ListPreference? = null
    private var mHeaderProvider: ListPreference? = null
    private val mDaylightHeaderProvider = "daylight"
    private var mFileHeader: Preference? = null
    private val mFileHeaderProvider = "file"

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        addPreferencesFromResource(R.xml.qs_header_image_settings)

        val resolver = activity?.contentResolver

        mHeaderBrowse = findCachedPreference(CUSTOM_HEADER_BROWSE)
        mHeaderBrowse?.isEnabled = isBrowseHeaderAvailable()

        mDaylightHeaderPack = findCachedPreference(DAYLIGHT_HEADER_PACK) as? ListPreference

        val entries = ArrayList<String>()
        val values = ArrayList<String>()
        getAvailableHeaderPacks(entries, values)
        mDaylightHeaderPack?.entries = entries.toTypedArray()
        mDaylightHeaderPack?.entryValues = values.toTypedArray()
        updateHeaderProviderSummary()
        mDaylightHeaderPack?.onPreferenceChangeListener = this

        val providerName = Settings.System.getString(resolver, Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER)
            ?: mDaylightHeaderProvider
        mHeaderBrowse?.isEnabled = isBrowseHeaderAvailable() && providerName != mFileHeaderProvider

        mHeaderProvider = findCachedPreference(CUSTOM_HEADER_PROVIDER) as? ListPreference
        val valueIndex = mHeaderProvider?.findIndexOfValue(providerName) ?: -1
        mHeaderProvider?.setValueIndex(if (valueIndex >= 0) valueIndex else 0)
        mHeaderProvider?.summary = mHeaderProvider?.entry
        mHeaderProvider?.onPreferenceChangeListener = this
        mDaylightHeaderPack?.isEnabled = providerName == mDaylightHeaderProvider

        mFileHeader = findCachedPreference(FILE_HEADER_SELECT)
        mFileHeader?.isEnabled = providerName == mFileHeaderProvider
    }

    private fun updateHeaderProviderSummary() {
        val settingHeaderPackage = Settings.System.getString(
            activity?.contentResolver,
            Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK
        )
        val valueIndex = mDaylightHeaderPack?.findIndexOfValue(settingHeaderPackage) ?: -1
        if (valueIndex >= 0) {
            mDaylightHeaderPack?.setValueIndex(valueIndex)
            mDaylightHeaderPack?.summary = mDaylightHeaderPack?.entry
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = activity?.contentResolver
        when (preference?.key) {
            DAYLIGHT_HEADER_PACK -> {
                val dhvalue = newValue as? String ?: return false
                Settings.System.putString(resolver, Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, dhvalue)
                val dhvalueIndex = mDaylightHeaderPack?.findIndexOfValue(dhvalue) ?: -1
                if (dhvalueIndex >= 0) {
                    mDaylightHeaderPack?.summary = mDaylightHeaderPack?.entries?.get(dhvalueIndex)
                }
                return true
            }

            CUSTOM_HEADER_PROVIDER -> {
                val value = newValue as? String ?: return false
                Settings.System.putString(resolver, Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, value)
                val valueIndex = mHeaderProvider?.findIndexOfValue(value) ?: -1
                if (valueIndex >= 0) {
                    mHeaderProvider?.summary = mHeaderProvider?.entries?.get(valueIndex)
                }
                mDaylightHeaderPack?.isEnabled = value == mDaylightHeaderProvider
                mHeaderBrowse?.isEnabled = value != mFileHeaderProvider
                mHeaderBrowse?.setTitle(if (valueIndex == 0) R.string.qs_header_browse_title else R.string.qs_header_pick_title)
                mHeaderBrowse?.setSummary(if (valueIndex == 0) R.string.qs_header_browse_summary else R.string.qs_header_pick_summary)
                mFileHeader?.isEnabled = value == mFileHeaderProvider
                return true
            }

            else -> return false
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference == mFileHeader) {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                startActivityForResult(intent, 10001)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.qs_header_needs_gallery, Toast.LENGTH_LONG).show()
            }
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun isBrowseHeaderAvailable(): Boolean {
        val pm = activity?.packageManager
        val browse = Intent()
        browse.setClassName("org.omnirom.omnistyle", "org.omnirom.omnistyle.PickHeaderActivity")
        return pm?.resolveActivity(browse, 0) != null
    }

    private fun getAvailableHeaderPacks(entries: MutableList<String>, values: MutableList<String>) {
        val headerMap = HashMap<String, String>()
        val intent = Intent()
        val packageManager = activity?.packageManager

        intent.action = "org.omnirom.DaylightHeaderPack"
        packageManager?.queryIntentActivities(intent, 0)?.forEach { r ->
            val packageName = r.activityInfo.packageName
            val label = r.activityInfo.loadLabel(packageManager).toString().takeIf { it.isNotEmpty() } ?: packageName
            headerMap[label] = packageName
        }

        intent.action = "org.omnirom.DaylightHeaderPack1"
        packageManager?.queryIntentActivities(intent, 0)?.forEach { r ->
            val packageName = r.activityInfo.packageName
            if (r.activityInfo.name.endsWith(".theme")) {
                return@forEach
            }
            val label = r.activityInfo.loadLabel(packageManager).toString().takeIf { it.isNotEmpty() } ?: packageName
            headerMap[label] = "$packageName/${r.activityInfo.name}"
        }

        val labelList = headerMap.keys.sorted()
        labelList.forEach { label ->
            entries.add(label)
            values.add(headerMap[label] ?: "")
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (requestCode == 10001) {
            if (resultCode != Activity.RESULT_OK) {
                return
            }
            val imgUri = result?.data
            if (imgUri != null) {
                val savedImagePath = ImageUtils.saveImageToInternalStorage(
                    requireContext(), imgUri, "qs_header_image", "QS_HEADER_IMAGE"
                )
                if (savedImagePath != null) {
                    val resolver = requireContext().contentResolver
                    Settings.System.putStringForUser(
                        resolver, 
                        Settings.System.STATUS_BAR_FILE_HEADER_IMAGE, 
                        savedImagePath, 
                        UserHandle.USER_CURRENT
                    )
                }
            }
        }
    }
}
