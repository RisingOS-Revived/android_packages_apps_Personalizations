/*
 * Copyright (C) 2024 risingOS
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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemProperties
import android.provider.SearchIndexableResource
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast

import androidx.documentfile.provider.DocumentFile
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceScreen

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.Indexable
import com.android.settingslib.search.SearchIndexable
import com.android.settings.preferences.BootAnimationPreviewPreference

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import android.os.AsyncTask
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap

@SearchIndexable
class BootAnimationSettings : SettingsPreferenceFragment(), OnPreferenceChangeListener {

    private var mBootAnimationStyle: ListPreference? = null
    
    // Background executor for file operations
    private var mFileExecutor: ExecutorService? = null
    
    // Cache for file existence checks
    private val mFileExistenceCache: MutableMap<String, Boolean> = ConcurrentHashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.rising_settings_bootanimation)
        
        // Initialize background executor
        mFileExecutor = Executors.newSingleThreadExecutor()

        mBootAnimationStyle = findPreference<ListPreference>(BOOTANIMATION_STYLE_KEY)?.apply {
            setOnPreferenceChangeListener(this@BootAnimationSettings)

            // Set the current value from the system property
            val currentStyle = SystemProperties.getInt(BOOTANIMATION_STYLE_KEY, 0)
            value = currentStyle.toString()
            updateBootAnimationPreview()
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference == mBootAnimationStyle) {
            val style = (newValue as String).toInt()
            if (style == 5) { // Custom option selected
                launchFilePicker()
                return false // Return false to prevent immediate property update
            } else {
                copyProductFile(style)
                return true
            }
        }
        return false
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_ZIP)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_ZIP && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                handleSelectedFile(uri)
            }
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        // Perform file operations in background to avoid blocking UI
        CopyFileTask(this, uri, true).execute()
    }
    
    private fun copyProductFile(style: Int) {
        // Validate input
        if (style < 0 || style >= PRODUCT_BOOT_ANIMATION_FILES.size) {
            Log.e(TAG, "Invalid style index")
            return
        }
        
        val productFilePath = PRODUCT_BOOT_ANIMATION_FILES[style]
        
        // Check cache first
        var fileExists = mFileExistenceCache[productFilePath]
        if (fileExists == null) {
            fileExists = File(productFilePath).exists()
            mFileExistenceCache[productFilePath] = fileExists
        }
        
        if (!fileExists) {
            Log.e(TAG, "Product file does not exist: $productFilePath")
            return
        }
        
        // Perform file operations in background
        CopyFileTask(this, productFilePath, style, false).execute()
    }

    private fun updateBootAnimationPreview() {
        val previewPreference = findPreference<BootAnimationPreviewPreference>("bootanimation_preview")
        previewPreference?.loadBootAnimationPreview()
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    // Background task for file operations
    private class CopyFileTask : AsyncTask<Void, Void, Boolean> {
        private val mFragmentRef: WeakReference<BootAnimationSettings>
        private val mSource: Any // Uri or String path
        private val mStyle: Int
        private val mIsCustomFile: Boolean
        private var mErrorMessage: String? = null
        
        // Constructor for custom file (Uri)
        constructor(fragment: BootAnimationSettings, uri: Uri, isCustom: Boolean) {
            mFragmentRef = WeakReference(fragment)
            mSource = uri
            mStyle = 5 // Custom style
            mIsCustomFile = isCustom
        }
        
        // Constructor for product file (String path)
        constructor(fragment: BootAnimationSettings, path: String, style: Int, isCustom: Boolean) {
            mFragmentRef = WeakReference(fragment)
            mSource = path
            mStyle = style
            mIsCustomFile = isCustom
        }
        
        override fun doInBackground(vararg params: Void): Boolean {
            val fragment = mFragmentRef.get()
            if (fragment == null || !fragment.isAdded) {
                return false
            }
            
            try {
                val inputStream: InputStream? = when (mSource) {
                    is Uri -> fragment.context?.contentResolver?.openInputStream(mSource)
                    is String -> FileInputStream(File(mSource))
                    else -> null
                }
                
                if (inputStream == null) {
                    mErrorMessage = "Failed to open input stream"
                    return false
                }
                
                val customBootAnimation = File(CUSTOM_BOOTANIMATION_FILE)
                customBootAnimation.parentFile?.mkdirs()
                
                FileOutputStream(customBootAnimation).use { outputStream ->
                    val buffer = ByteArray(8192) // Larger buffer for better performance
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    outputStream.flush()
                }
                inputStream.close()
                
                return true
            } catch (e: Exception) {
                mErrorMessage = "Error copying bootanimation: ${e.message}"
                Log.e(TAG, mErrorMessage, e)
                return false
            }
        }
        
        override fun onPostExecute(success: Boolean) {
            val fragment = mFragmentRef.get()
            if (fragment == null || !fragment.isAdded) {
                return
            }
            
            if (success) {
                SystemProperties.set(BOOTANIMATION_STYLE_KEY, mStyle.toString())
                fragment.updateBootAnimationPreview()
                fragment.mBootAnimationStyle?.value = mStyle.toString()
                Toast.makeText(fragment.context, R.string.boot_animation_applied, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(fragment.context, "Failed to apply boot animation: $mErrorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup executor and cache
        mFileExecutor?.let { executor ->
            if (!executor.isShutdown) {
                executor.shutdown()
            }
        }
        mFileExistenceCache.clear()
    }
    
    override fun onDetach() {
        super.onDetach()
        // Additional cleanup
        mFileExistenceCache.clear()
    }

    companion object {
        private const val BOOTANIMATION_STYLE_KEY = "persist.sys.bootanimation_style"
        private const val TAG = "BootAnimationSettings"
        private const val REQUEST_CODE_PICK_ZIP = 1001
        private const val CUSTOM_BOOTANIMATION_FILE = "/data/misc/bootanim/bootanimation.zip"

        private val PRODUCT_BOOT_ANIMATION_FILES = arrayOf(
            "/product/media/bootanimation_rising.zip",
            "/product/media/bootanimation_cyberpunk.zip",
            "/product/media/bootanimation_google.zip",
            "/product/media/bootanimation_google_monet.zip",
            "/product/media/bootanimation_valorant.zip"
        )

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider() {
            override fun getXmlResourcesToIndex(context: Context, enabled: Boolean): List<SearchIndexableResource> {
                val result = ArrayList<SearchIndexableResource>()
                val sir = SearchIndexableResource(context).apply {
                    xmlResId = R.xml.rising_settings_bootanimation
                }
                result.add(sir)
                return result
            }

            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }
}
