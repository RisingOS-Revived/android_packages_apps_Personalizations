/*
 * Copyright (C) 2024-2025 Lunaris AOSP
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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast

import androidx.preference.Preference

import com.android.internal.logging.nano.MetricsProto
import com.android.internal.util.android.VibrationUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

import lineageos.preference.SystemSettingMainSwitchPreference

import java.io.File

import com.android.settings.preferences.SystemSettingListPreference
import com.android.settings.preferences.SystemSettingSwitchPreference
import com.android.settings.preferences.WallpaperPreviewPreference
import com.android.settings.utils.SystemRestartUtils

@SearchIndexable
class LockGlympsSettings : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private var mPreviewPreference: WallpaperPreviewPreference? = null
    private var mEnablePreference: SystemSettingMainSwitchPreference? = null
    private var mSourcePreference: SystemSettingListPreference? = null
    private var mWallpaperTargetPreference: SystemSettingListPreference? = null
    private var mChangeOnPreference: SystemSettingListPreference? = null
    private var mTimerIntervalPreference: SystemSettingListPreference? = null
    private var mWifiOnlyPreference: SystemSettingSwitchPreference? = null
    private var mCacheSizePreference: SystemSettingListPreference? = null
    private var mCustomUrlsPreference: Preference? = null
    private var mClearCachePreference: Preference? = null
    private var mFolderInfoPreference: Preference? = null

    private var mHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = Handler(Looper.getMainLooper())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.lock_glymps_settings)

        val context = activity ?: return

        mPreviewPreference = findPreference(KEY_PREVIEW)

        mEnablePreference = findPreference(KEY_ENABLE)
        mEnablePreference?.onPreferenceChangeListener = this

        mSourcePreference = findPreference(KEY_SOURCE)
        mSourcePreference?.let { pref ->
            pref.onPreferenceChangeListener = this
            pref.value?.let { updateSourceDependentPrefs(it) }
        }

        mWallpaperTargetPreference = findPreference(KEY_WALLPAPER_TARGET)
        mWallpaperTargetPreference?.onPreferenceChangeListener = this

        mChangeOnPreference = findPreference(KEY_CHANGE_ON)
        mChangeOnPreference?.let { pref ->
            pref.onPreferenceChangeListener = this
            pref.value?.let { updateTimerVisibility(it) }
        }

        mTimerIntervalPreference = findPreference(KEY_TIMER_INTERVAL)
        mTimerIntervalPreference?.onPreferenceChangeListener = this

        mWifiOnlyPreference = findPreference(KEY_WIFI_ONLY)

        mCacheSizePreference = findPreference(KEY_CACHE_SIZE)

        mCustomUrlsPreference = findPreference(KEY_CUSTOM_URLS)
        mCustomUrlsPreference?.setOnPreferenceClickListener {
            showCustomUrlsDialog()
            true
        }

        mFolderInfoPreference = findPreference(KEY_FOLDER_INFO)
        mFolderInfoPreference?.let {
            updateFolderInfo()
            it.setOnPreferenceClickListener {
                showFolderInfo()
                true
            }
        }

        mClearCachePreference = findPreference(KEY_CLEAR_CACHE)
        mClearCachePreference?.setOnPreferenceClickListener {
            clearCache()
            true
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val context = activity ?: return false

        when (preference.key) {
            KEY_ENABLE -> {
                val enabled = newValue as Boolean

                val serviceIntent = Intent().apply {
                    setClassName(
                        "com.android.systemui",
                        "com.android.systemui.lockglymps.LockGlympsService"
                    )
                }

                if (enabled) {
                    context.startService(serviceIntent)
                } else {
                    context.stopService(serviceIntent)
                }

                SystemRestartUtils.showSystemUIRestartDialog(context)
                return true
            }

            KEY_SOURCE -> {
                updateSourceDependentPrefs(newValue as String)
                notifyServiceToRefresh(context)
                SystemRestartUtils.showSystemUIRestartDialog(context)
                return true
            }

            KEY_WALLPAPER_TARGET -> {
                notifyServiceToRefresh(context)
                schedulePreviewRefresh()
                SystemRestartUtils.showSystemUIRestartDialog(context)
                return true
            }

            KEY_CHANGE_ON -> {
                updateTimerVisibility(newValue as String)
                notifyServiceToRefresh(context)
                SystemRestartUtils.showSystemUIRestartDialog(context)
                return true
            }

            KEY_TIMER_INTERVAL -> {
                notifyServiceToRefresh(context)
                return true
            }
        }

        notifyServiceToRefresh(context)
        return true
    }

    private fun schedulePreviewRefresh() {
        mHandler?.let { handler ->
            mPreviewPreference?.let {
                handler.postDelayed({
                    mPreviewPreference?.refreshPreviews()
                }, 1500)
            }
        }
    }

    private fun updateTimerVisibility(changeOnValue: String) {
        mTimerIntervalPreference?.isVisible = "2" == changeOnValue
    }

    private fun updateSourceDependentPrefs(sourceValue: String) {
        val isOnlineSource = sourceValue == "0" || sourceValue == "1"
        val isCustomUrls = sourceValue == "1"
        val isLocalFolder = sourceValue == "2"

        mWifiOnlyPreference?.isVisible = isOnlineSource
        mCacheSizePreference?.isVisible = isOnlineSource
        mCustomUrlsPreference?.isVisible = isCustomUrls

        mFolderInfoPreference?.let {
            it.isVisible = isLocalFolder
            if (isLocalFolder) {
                updateFolderInfo()
            }
        }

        mClearCachePreference?.isVisible = isOnlineSource
    }

    private fun updateFolderInfo() {
        val folderInfoPreference = mFolderInfoPreference ?: return

        val storageDir = File(Environment.getExternalStorageDirectory(), STORAGE_FOLDER)

        if (!storageDir.exists()) {
            folderInfoPreference.summary = "Folder not found. Tap to create."
        } else {
            val files = storageDir.listFiles { _, name ->
                val lower = name.lowercase()
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".webp")
            }

            val count = files?.size ?: 0
            folderInfoPreference.summary = "$count wallpapers found in ${storageDir.path}"
        }
    }

    private fun showFolderInfo() {
        val context = activity ?: return

        val storageDir = File(Environment.getExternalStorageDirectory(), STORAGE_FOLDER)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Local Wallpaper Folder")

        if (!storageDir.exists()) {
            builder.setMessage(
                "Folder does not exist yet.\n\nLocation: ${storageDir.path}" +
                    "\n\nWould you like to create it?"
            )

            builder.setPositiveButton("Create Folder") { _, _ ->
                if (storageDir.mkdirs()) {
                    Toast.makeText(
                        context,
                        "Folder created: ${storageDir.path}",
                        Toast.LENGTH_LONG
                    ).show()
                    updateFolderInfo()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to create folder",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            builder.setNegativeButton("Cancel", null)
        } else {
            val files = storageDir.listFiles { _, name ->
                val lower = name.lowercase()
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".webp")
            }

            val count = files?.size ?: 0

            builder.setMessage(
                "Folder location: ${storageDir.path}" +
                    "\n\nWallpapers found: $count" +
                    "\n\nSupported formats: JPG, PNG, WEBP" +
                    "\n\nPlace your wallpaper images in this folder and they will be used randomly."
            )

            builder.setPositiveButton("OK", null)
        }

        builder.show()
    }

    private fun notifyServiceToRefresh(context: Context) {
        val serviceIntent = Intent().apply {
            setClassName(
                "com.android.systemui",
                "com.android.systemui.lockglymps.LockGlympsService"
            )
            action = "REFRESH_SETTINGS"
        }
        context.startService(serviceIntent)
    }

    private fun showCustomUrlsDialog() {
        val context = activity ?: return

        val urls = Settings.System.getString(
            context.contentResolver,
            "lock_glymps_custom_urls"
        )

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Custom Wallpaper URLs")
        builder.setMessage("Enter direct image URLs, one per line")

        val input = EditText(context).apply {
            setText(urls?.replace(",", "\n") ?: "")
            setMinLines(5)
            setMaxLines(10)
            hint = "https://example.com/image1.jpg\nhttps://example.com/image2.png"
        }

        val padding = (16 * context.resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)

        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val inputText = input.text.toString()
            val lines = inputText.split("\n")
            val sb = StringBuilder()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append(",")
                    sb.append(trimmed)
                }
            }

            Settings.System.putString(
                context.contentResolver,
                "lock_glymps_custom_urls", sb.toString()
            )

            notifyServiceToRefresh(context)

            Toast.makeText(context, "Custom URLs saved", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun clearCache() {
        val context = activity ?: return

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Clear Cache")
        builder.setMessage("This will delete all cached wallpapers and they will be re-downloaded. Continue?")

        builder.setPositiveButton("Clear") { _, _ ->
            val intent = Intent().apply {
                setClassName(
                    "com.android.systemui",
                    "com.android.systemui.lockglymps.LockGlympsService"
                )
                action = "CLEAR_CACHE"
            }
            context.startService(intent)

            Toast.makeText(
                context,
                "Cache cleared. New wallpapers will be downloaded.",
                Toast.LENGTH_SHORT
            ).show()

            SystemRestartUtils.showSystemUIRestartDialog(context)
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler?.removeCallbacksAndMessages(null)
        mHandler = null
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key != null) {
            VibrationUtils.triggerVibration(context, 3)
        }
        return super.onPreferenceTreeClick(preference)
    }

    companion object {
        private const val TAG = "LockGlympsSettings"

        private const val KEY_PREVIEW = "lock_glymps_preview"
        private const val KEY_ENABLE = "lock_glymps_enabled"
        private const val KEY_SOURCE = "lock_glymps_source"
        private const val KEY_WALLPAPER_TARGET = "lock_glymps_wallpaper_target"
        private const val KEY_CHANGE_ON = "lock_glymps_change_on"
        private const val KEY_TIMER_INTERVAL = "lock_glymps_timer_interval"
        private const val KEY_WIFI_ONLY = "lock_glymps_wifi_only"
        private const val KEY_CACHE_SIZE = "lock_glymps_cache_size"
        private const val KEY_CUSTOM_URLS = "lock_glymps_custom_urls"
        private const val KEY_CLEAR_CACHE = "lock_glymps_clear_cache"
        private const val KEY_FOLDER_INFO = "lock_glymps_folder_info"

        private const val STORAGE_FOLDER = "Glymps"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            BaseSearchIndexProvider(R.xml.lock_glymps_settings)
    }
}