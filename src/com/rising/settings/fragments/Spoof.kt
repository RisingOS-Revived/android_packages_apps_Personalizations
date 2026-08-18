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

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.preferences.SecureSettingSwitchPreference
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.PreferenceUtils
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Spoof : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val TAG = "Spoof"

        private const val KEY_FEATURES_CATEGORY = "spoofing_features_category"
        private const val KEY_APP_SPECIFIC_CATEGORY = "spoofing_app_specific_category"
        private const val PI_PHOTOS_SPOOF = "pi_photos_spoof"
        private const val PI_SNAPCHAT_SPOOF = "pi_snapchat_spoof"
        private const val KEY_TENSOR_TARGETS = "tensor_targets_settings"

        private const val PHOTOS_PACKAGE = "com.google.android.apps.photos"
        private const val SNAPCHAT_PACKAGE = "com.snapchat.android"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.rising_settings_spoof)
    }

    private var mFeaturesCategory: PreferenceCategory? = null
    private var mAppSpecificCategory: PreferenceCategory? = null
    private var mPhotosSpoof: SecureSettingSwitchPreference? = null
    private var mSnapchatSpoof: SecureSettingSwitchPreference? = null
    private var mTensorTargets: Preference? = null

    private lateinit var mHandler: Handler
    private var mPendingKill: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_spoof)

        mHandler = Handler(Looper.getMainLooper())

        mFeaturesCategory = findPreference(KEY_FEATURES_CATEGORY)
        mAppSpecificCategory = findPreference(KEY_APP_SPECIFIC_CATEGORY)
        mPhotosSpoof = findPreference(PI_PHOTOS_SPOOF)
        mSnapchatSpoof = findPreference(PI_SNAPCHAT_SPOOF)
        mTensorTargets = findPreference(KEY_TENSOR_TARGETS)

        val model = SystemProperties.get("ro.product.model")
        val isTensorDevice = model.matches(Regex("Pixel (6|7|8|9|10)[a-zA-Z ]*"))
        if (mTensorTargets != null && isTensorDevice) {
            mFeaturesCategory?.removePreference(mTensorTargets!!)
        }
        mPhotosSpoof = initAppSpoof(mPhotosSpoof, PHOTOS_PACKAGE)
        mSnapchatSpoof = initAppSpoof(mSnapchatSpoof, SNAPCHAT_PACKAGE)
    }

    /**
     * Checks whether [pkg] is installed. If not, removes [pref] from the
     * app-specific category and returns null. If installed, syncs the checked state from
     * Settings.Secure and registers the change listener, then returns the preference unchanged.
     */
    private fun initAppSpoof(
        pref: SecureSettingSwitchPreference?,
        pkg: String
    ): SecureSettingSwitchPreference? {
        if (pref == null) return null
        try {
            requireContext().packageManager.getPackageInfo(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            mAppSpecificCategory?.removePreference(pref)
            return null
        }
        pref.onPreferenceChangeListener = this
        return pref
    }

    private fun scheduleKill(pkg: String) {
        mPendingKill?.let { mHandler.removeCallbacks(it) }
        Toast.makeText(context, R.string.spoofing_applying_changes, Toast.LENGTH_SHORT).show()
        val kill = Runnable { killIfRunning(pkg) }
        mPendingKill = kill
        mHandler.postDelayed(kill, 500)
    }

    private fun killIfRunning(pkg: String) {
        try {
            val am = requireContext()
                .getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            for (proc in am.runningAppProcesses.orEmpty()) {
                val pkgList = proc.pkgList ?: continue
                for (p in pkgList) {
                    if (pkg == p) {
                        am.forceStopPackage(pkg)
                        Log.d(TAG, "Killed: $pkg")
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kill failed: $pkg", e)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (mSnapchatSpoof == null && mPhotosSpoof == null) return true
        when (preference) {
            mSnapchatSpoof -> scheduleKill(SNAPCHAT_PACKAGE)
            mPhotosSpoof -> scheduleKill(PHOTOS_PACKAGE)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mPendingKill?.let { mHandler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        PreferenceUtils.reloadCustomPrimarySwitches(preferenceScreen)
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }
}