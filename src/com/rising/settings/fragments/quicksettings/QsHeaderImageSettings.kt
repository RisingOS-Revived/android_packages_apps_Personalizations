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
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.SearchIndexableResource
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.ImageUtils
import com.android.settingslib.search.SearchIndexable
import com.rising.settings.fragments.OptimizedSettingsFragment

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SearchIndexable
class QsHeaderImageSettings : OptimizedSettingsFragment(), OnPreferenceChangeListener {

    companion object {
        private const val TAG = "QsHeaderImageSettings"

        private const val CUSTOM_HEADER_BROWSE = "custom_header_browse"
        private const val DAYLIGHT_HEADER_PACK = "daylight_header_pack"
        private const val CUSTOM_HEADER_PROVIDER = "qs_header_provider"
        private const val FILE_HEADER_SELECT = "file_header_select"
        private const val REQUEST_PICK_IMAGE = 10001

        private const val PROVIDER_DAYLIGHT = "daylight"
        private const val PROVIDER_FILE = "file"

        private const val OMNISTYLE_PACKAGE = "org.omnirom.omnistyle"
        private const val OMNISTYLE_PICK = "$OMNISTYLE_PACKAGE.PickHeaderActivity"
        private const val OMNISTYLE_BROWSE = "$OMNISTYLE_PACKAGE.BrowseHeaderActivity"
        /** Pack key (package or package/activity) OmniStyle opens on. */
        private const val EXTRA_HEADER_PACK = "header_pack"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider() {
            override fun getXmlResourcesToIndex(context: Context, enabled: Boolean): List<SearchIndexableResource> {
                val sir = SearchIndexableResource(context)
                sir.xmlResId = R.xml.qs_header_image_settings
                return arrayListOf(sir)
            }

            override fun getNonIndexableKeys(context: Context): List<String> = ArrayList()
        }
    }

    private val mExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mMainHandler = Handler(Looper.getMainLooper())

    private var mHeaderBrowse: Preference? = null
    private var mDaylightHeaderPack: ListPreference? = null
    private var mHeaderProvider: ListPreference? = null
    private var mFileHeader: Preference? = null
    private var mBrowseAvailable = false

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        addPreferencesFromResource(R.xml.qs_header_image_settings)

        mHeaderBrowse = findCachedPreference(CUSTOM_HEADER_BROWSE)
        mHeaderProvider = findCachedPreference(CUSTOM_HEADER_PROVIDER) as? ListPreference
        mDaylightHeaderPack = findCachedPreference(DAYLIGHT_HEADER_PACK) as? ListPreference
        mFileHeader = findCachedPreference(FILE_HEADER_SELECT)

        mHeaderProvider?.onPreferenceChangeListener = this
        mDaylightHeaderPack?.onPreferenceChangeListener = this
    }

    override fun onResume() {
        super.onResume()
        mBrowseAvailable = isOmniStyleAvailable()

        val entries = ArrayList<String>()
        val values = ArrayList<String>()
        getAvailableHeaderPacks(entries, values)
        mDaylightHeaderPack?.entries = entries.toTypedArray()
        mDaylightHeaderPack?.entryValues = values.toTypedArray()
        updateDaylightPackSummary()

        var providerName = Settings.System.getString(
            requireContext().contentResolver,
            Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER
        )
        if (providerName == null || (mHeaderProvider?.findIndexOfValue(providerName) ?: -1) < 0) {
            providerName = PROVIDER_DAYLIGHT
        }
        mHeaderProvider?.value = providerName
        updateProviderUi(providerName)
    }

    override fun onDestroy() {
        super.onDestroy()
        mExecutor.shutdown()
    }

    private fun updateDaylightPackSummary() {
        val pack = Settings.System.getString(
            requireContext().contentResolver,
            Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK
        )
        val index = mDaylightHeaderPack?.findIndexOfValue(pack) ?: -1
        if (index >= 0) {
            mDaylightHeaderPack?.setValueIndex(index)
            mDaylightHeaderPack?.summary = mDaylightHeaderPack?.entry
        } else {
            mDaylightHeaderPack?.summary = null
        }
    }

    private fun updateProviderUi(provider: String?) {
        val daylight = provider == PROVIDER_DAYLIGHT
        val file = provider == PROVIDER_FILE

        mHeaderProvider?.summary = mHeaderProvider?.entry
        mDaylightHeaderPack?.isEnabled = daylight
        mFileHeader?.isEnabled = file

        // Daylight: browse packs read-only. Static: pick an image, OmniStyle applies it.
        mHeaderBrowse?.isEnabled = mBrowseAvailable && !file
        mHeaderBrowse?.setTitle(if (daylight) R.string.qs_header_browse_title else R.string.qs_header_pick_title)
        mHeaderBrowse?.setSummary(if (daylight) R.string.qs_header_browse_summary else R.string.qs_header_pick_summary)

        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(OMNISTYLE_PACKAGE, if (daylight) OMNISTYLE_BROWSE else OMNISTYLE_PICK)
        val pack = mDaylightHeaderPack?.value
        if (daylight && !pack.isNullOrEmpty()) {
            intent.putExtra(EXTRA_HEADER_PACK, pack)
        }
        mHeaderBrowse?.intent = intent
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val resolver = requireContext().contentResolver
        return when (preference.key) {
            DAYLIGHT_HEADER_PACK -> {
                val value = newValue as? String ?: return false
                Settings.System.putString(resolver, Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value)
                val index = mDaylightHeaderPack?.findIndexOfValue(value) ?: -1
                if (index >= 0) {
                    mDaylightHeaderPack?.summary = mDaylightHeaderPack?.entries?.get(index)
                }
                // Keep the browse intent pointing at the newly chosen pack.
                mDaylightHeaderPack?.value = value
                updateProviderUi(mHeaderProvider?.value)
                true
            }

            CUSTOM_HEADER_PROVIDER -> {
                val value = newValue as? String ?: return false
                if ((mHeaderProvider?.findIndexOfValue(value) ?: -1) < 0) return false
                Settings.System.putString(resolver, Settings.System.STATUS_BAR_CUSTOM_HEADER_PROVIDER, value)
                mHeaderProvider?.value = value
                updateProviderUi(value)
                true
            }

            else -> false
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference == mFileHeader) {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_PICK_IMAGE)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.qs_header_needs_gallery, Toast.LENGTH_LONG).show()
            }
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun isOmniStyleAvailable(): Boolean {
        val pick = Intent().setClassName(OMNISTYLE_PACKAGE, OMNISTYLE_PICK)
        return requireContext().packageManager.resolveActivity(pick, 0) != null
    }

    private fun getAvailableHeaderPacks(entries: MutableList<String>, values: MutableList<String>) {
        val headerMap = HashMap<String, String>()
        val pm = requireContext().packageManager
        val intent = Intent("org.omnirom.DaylightHeaderPack")

        pm.queryIntentActivities(intent, 0).forEach { r ->
            headerMap[r.activityInfo.loadLabel(pm).toString()] = r.activityInfo.packageName
        }

        intent.action = "org.omnirom.DaylightHeaderPack1"
        pm.queryIntentActivities(intent, 0).forEach { r ->
            if (r.activityInfo.name.endsWith(".theme")) return@forEach
            headerMap[r.activityInfo.loadLabel(pm).toString()] =
                "${r.activityInfo.packageName}/${r.activityInfo.name}"
        }

        headerMap.keys.sortedWith(String.CASE_INSENSITIVE_ORDER).forEach { label ->
            entries.add(label)
            values.add(headerMap[label] ?: "")
        }
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.VIEW_UNKNOWN

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        val imgUri: Uri = result?.data ?: return
        if (requestCode != REQUEST_PICK_IMAGE || resultCode != Activity.RESULT_OK) return

        val appContext = requireContext().applicationContext

        mExecutor.execute {
            val savedPath = try {
                ImageUtils.saveImageToInternalStorage(
                    appContext, imgUri, "qs_header_image", "QS_HEADER_IMAGE"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save header image $imgUri", e)
                null
            }
            if (savedPath != null) {
                Settings.System.putStringForUser(
                    appContext.contentResolver,
                    Settings.System.STATUS_BAR_FILE_HEADER_IMAGE,
                    savedPath,
                    UserHandle.USER_CURRENT
                )
            } else {
                mMainHandler.post {
                    Toast.makeText(appContext, R.string.qs_header_file_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
