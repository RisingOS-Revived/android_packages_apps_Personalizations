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
import android.provider.SearchIndexableResource
import android.provider.Settings
import android.text.TextUtils
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
import com.android.settingslib.search.SearchIndexable

import com.android.settings.utils.ImageUtils

@SearchIndexable
class WallpaperDepth : OptimizedSettingsFragment(), Preference.OnPreferenceChangeListener {

    private var mDepthWallpaperCustomImagePicker: Preference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_wallpaper_depth)
        
        mDepthWallpaperCustomImagePicker = findPreference("depth_wallpaper_subject_image_uri")
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference == mDepthWallpaperCustomImagePicker) {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                startActivityForResult(intent, 10001)
            } catch (e: Exception) {
                Toast.makeText(context, R.string.qs_header_needs_gallery, Toast.LENGTH_LONG).show()
            }
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (requestCode == 10001) {
            if (resultCode != Activity.RESULT_OK) {
                return
            }

            val imgUri = result?.data
            if (imgUri != null) {
                val savedImagePath = ImageUtils.saveImageToInternalStorage(
                    context, imgUri, "depthwallpaper", "DEPTH_WALLPAPER_SUBJECT"
                )
                if (savedImagePath != null) {
                    val resolver = context?.contentResolver
                    resolver?.let {
                        Settings.System.putStringForUser(
                            it, "depth_wallpaper_subject_image_uri", 
                            savedImagePath, UserHandle.USER_CURRENT
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "WallpaperDepth"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_wallpaper_depth) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context)
                return keys
            }
        }
    }
}
