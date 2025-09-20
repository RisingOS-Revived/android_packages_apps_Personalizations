/*
 * Copyright (C) 2023 crDroid Android Project
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
package com.rising.settings.fragments.lockscreen.doze

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

import com.android.internal.logging.nano.MetricsProto

import androidx.preference.Preference

import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.utils.ImageUtils

class AODSettings : OptimizedSettingsFragment() {

    companion object {
        private const val CUSTOM_IMAGE_REQUEST_CODE_KEY = "lockscreen_custom_image"
        private const val CUSTOM_IMAGE_REQUEST_CODE = 1001
    }

    private lateinit var mCustomImagePreference: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_aod_settings)
        mCustomImagePreference = findCachedPreference(CUSTOM_IMAGE_REQUEST_CODE_KEY)!!
        val context = getSafeContext()
        val clockStyle = context?.let { ctx ->
            Settings.Secure.getIntForUser(ctx.contentResolver, "clock_style", 0, UserHandle.USER_CURRENT) 
        } ?: 0
        val imagePath = Settings.System.getString(context?.contentResolver, "custom_aod_image_uri")
        if (imagePath != null && clockStyle.compareTo(0) > 0) {
            mCustomImagePreference.summary = imagePath
            mCustomImagePreference.isEnabled = true
        } else if (clockStyle == 0) {
            mCustomImagePreference.summary = context?.getString(R.string.custom_aod_image_not_supported)
            mCustomImagePreference.isEnabled = false
        }
    }
    
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference == mCustomImagePreference) {
            try {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                startActivityForResult(intent, CUSTOM_IMAGE_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(context, R.string.qs_header_needs_gallery, Toast.LENGTH_LONG).show()
            }
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        if (requestCode == CUSTOM_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && result != null) {
            val imgUri = result.data
            if (imgUri != null) {
                val savedImagePath = ImageUtils.saveImageToInternalStorage(context, imgUri, "lockscreen_aod_image", "LOCKSCREEN_CUSTOM_AOD_IMAGE")
                if (savedImagePath != null) {
                    val resolver = context?.contentResolver
                    Settings.System.putStringForUser(resolver, "custom_aod_image_uri", savedImagePath, UserHandle.USER_CURRENT)
                    mCustomImagePreference.summary = savedImagePath
                }
            }
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
