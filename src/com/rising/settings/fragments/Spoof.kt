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
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.internal.util.android.SystemRestartUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.preferences.KeyboxDataPreference
import com.android.settings.preferences.SystemPropertySwitchPreference
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.utils.DeviceUtils
import com.android.settingslib.search.SearchIndexable
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

@SearchIndexable
class Spoof : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private const val TAG = "Spoof"
        
        private const val KEY_PIF_JSON_FILE_PREFERENCE = "pif_json_file_preference"
        private const val KEY_SYSTEM_WIDE_CATEGORY = "spoofing_system_wide_category"
        private const val KEY_UPDATE_JSON_BUTTON = "update_pif_json"
        private const val SYS_GMS_SPOOF = "persist.sys.pixelprops.gms"
        private const val SYS_GOOGLE_SPOOF = "persist.sys.pixelprops"
        private const val SYS_GAMEPROP_SPOOF = "persist.sys.pixelprops.games"
        private const val SYS_GPHOTOS_SPOOF = "persist.sys.pixelprops.gphotos"
        private const val SYS_QSB_SPOOF = "persist.sys.pixelprops.qsb"
        private const val SYS_SNAP_SPOOF = "persist.sys.pixelprops.snap"
        private const val SYS_TENSOR_SPOOF = "persist.sys.features.tensor"
        private const val KEYBOX_DATA_KEY = "keybox_data_setting"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_spoof) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }

    private lateinit var mKeyboxFilePickerLauncher: ActivityResultLauncher<Intent>
    private var mKeyboxDataPreference: KeyboxDataPreference? = null
    private var mPifJsonFilePreference: Preference? = null
    private var mUpdateJsonButton: Preference? = null
    private var mSystemWideCategory: PreferenceCategory? = null
    private var mGmsSpoof: SystemPropertySwitchPreference? = null
    private var mGoogleSpoof: SystemPropertySwitchPreference? = null
    private var mGamePropsSpoof: SystemPropertySwitchPreference? = null
    private var mGphotosSpoof: SystemPropertySwitchPreference? = null
    private var mQsbSpoof: SystemPropertySwitchPreference? = null
    private var mSnapSpoof: SystemPropertySwitchPreference? = null
    private var mTensorSpoof: SystemPropertySwitchPreference? = null
    private var mWikiLink: Preference? = null

    private lateinit var mHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mHandler = Handler(Looper.getMainLooper())
        addPreferencesFromResource(R.xml.rising_settings_spoof)

        val context = requireContext()
        val resolver = context.contentResolver
        val prefScreen = preferenceScreen
        val resources = context.resources

        mSystemWideCategory = findPreference(KEY_SYSTEM_WIDE_CATEGORY)
        mGamePropsSpoof = findPreference(SYS_GAMEPROP_SPOOF)
        mGphotosSpoof = findPreference(SYS_GPHOTOS_SPOOF)
        mGmsSpoof = findPreference(SYS_GMS_SPOOF)
        mGoogleSpoof = findPreference(SYS_GOOGLE_SPOOF)
        mPifJsonFilePreference = findPreference(KEY_PIF_JSON_FILE_PREFERENCE)
        mQsbSpoof = findPreference(SYS_QSB_SPOOF)
        mSnapSpoof = findPreference(SYS_SNAP_SPOOF)
        mTensorSpoof = findPreference(SYS_TENSOR_SPOOF)
        mUpdateJsonButton = findPreference(KEY_UPDATE_JSON_BUTTON)

        val model = SystemProperties.get("ro.product.model")
        val isTensorDevice = model.matches(Regex("Pixel (6|7|8|9|10)[a-zA-Z ]*"))
        val isPixelGmsEnabled = SystemProperties.getBoolean(SYS_GMS_SPOOF, true) // Default to Pixel GMS

        if (DeviceUtils.isCurrentlySupportedPixel()) {
            mGoogleSpoof?.setDefaultValue(false)
            if (isMainlineTensorModel(model)) {
                mGoogleSpoof?.let { mSystemWideCategory?.removePreference(it as Preference) }
            }
        }

        if (isTensorDevice) {
            mTensorSpoof?.let { mSystemWideCategory?.removePreference(it as Preference) }
        }

        mGmsSpoof?.onPreferenceChangeListener = this
        mGoogleSpoof?.onPreferenceChangeListener = this
        mGphotosSpoof?.onPreferenceChangeListener = this
        mGamePropsSpoof?.onPreferenceChangeListener = this
        mQsbSpoof?.onPreferenceChangeListener = this
        mSnapSpoof?.onPreferenceChangeListener = this
        mTensorSpoof?.onPreferenceChangeListener = this

        mKeyboxFilePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                val pref = findPreference<KeyboxDataPreference>(KEYBOX_DATA_KEY)
                if (uri != null && pref != null) {
                    pref.handleFileSelected(uri)
                }
            }
        }

        mPifJsonFilePreference?.setOnPreferenceClickListener {
            openFileSelector(10001)
            true
        }

        mUpdateJsonButton?.setOnPreferenceClickListener {
            updatePropertiesFromUrl("https://raw.githubusercontent.com/RisingOS-Revived/risingOS_wiki/refs/heads/fifteen/spoofing/PlayIntergrity/pif.json")
            true
        }

        mWikiLink = findPreference("wiki_link")
        mWikiLink?.setOnPreferenceClickListener {
            val uri = Uri.parse("https://github.com/RisingOS-Revived/risingOS_wiki")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
            true
        }

        val showPropertiesPref = findPreference<Preference>("show_pif_properties")
        showPropertiesPref?.setOnPreferenceClickListener {
            showPropertiesDialog()
            true
        }
    }

    private fun isMainlineTensorModel(model: String): Boolean {
        return model.matches(Regex("Pixel (8|9|10)[a-zA-Z ]*"))
    }

    private fun openFileSelector(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, requestCode)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mKeyboxDataPreference = findPreference(KEYBOX_DATA_KEY)
        mKeyboxDataPreference?.setFilePickerLauncher(mKeyboxFilePickerLauncher)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null && requestCode == 10001) {
                loadPifJson(uri)
            }
        }
    }

    private fun showPropertiesDialog() {
        val properties = StringBuilder()
        try {
            val jsonObject = JSONObject()
            val keys = arrayOf(
                "persist.sys.pihooks_ID",
                "persist.sys.pihooks_BRAND",
                "persist.sys.pihooks_DEVICE",
                "persist.sys.pihooks_FINGERPRINT",
                "persist.sys.pihooks_MANUFACTURER",
                "persist.sys.pihooks_MODEL",
                "persist.sys.pihooks_PRODUCT",
                "persist.sys.pihooks_SECURITY_PATCH",
                "persist.sys.pihooks_DEVICE_INITIAL_SDK_INT",
                "persist.sys.pihooks_RELEASE",
                "persist.sys.pihooks_SDK_INT"
            )
            
            for (key in keys) {
                val value = SystemProperties.get(key, null)
                if (value != null) {
                    val buildKey = key.replace("persist.sys.pihooks_", "")
                    jsonObject.put(buildKey, value)
                }
            }
            properties.append(jsonObject.toString(4))
        } catch (e: JSONException) {
            Log.e(TAG, "Error creating JSON from properties", e)
            properties.append(getString(R.string.error_loading_properties))
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.show_pif_properties_title)
            .setMessage(properties.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * Kill packages that need to be restarted to pick up new PIF properties
     */
    private fun killGMSPackages() {
        try {
            val am = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packages = arrayOf(
                "com.google.android.gms",
                "com.android.vending"
            )
            for (pkg in packages) {
                am.javaClass
                    .getMethod("forceStopPackage", String::class.java)
                    .invoke(am, pkg)
                Log.i(TAG, "$pkg process killed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill packages", e)
        }
    }

    private fun updatePropertiesFromUrl(urlString: String) {
        Thread {
            try {
                val url = URL(urlString)
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    urlConnection.inputStream.use { inputStream ->
                        val json = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                        Log.d(TAG, "Downloaded JSON data: $json")
                        val jsonObject = JSONObject(json)
                        val spoofedModel = jsonObject.optString("MODEL", "Unknown model")
                        
                        val keys = jsonObject.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObject.getString(key)
                            Log.d(TAG, "Setting property: persist.sys.pihooks_$key = $value")
                            SystemProperties.set("persist.sys.pihooks_$key", value)
                        }
                        
                        mHandler.post {
                            val toastMessage = getString(R.string.toast_spoofing_success, spoofedModel)
                            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show()
                            killGMSPackages()
                        }
                    }
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading JSON or setting properties", e)
                mHandler.post {
                    Toast.makeText(requireContext(), R.string.toast_spoofing_failure, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun loadPifJson(uri: Uri) {
        Log.d(TAG, "Loading PIF JSON from URI: $uri")
        try {
            requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                val json = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                Log.d(TAG, "PIF JSON data: $json")
                val jsonObject = JSONObject(json)
                
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = jsonObject.getString(key)
                    Log.d(TAG, "Setting PIF property: persist.sys.pihooks_$key = $value")
                    SystemProperties.set("persist.sys.pihooks_$key", value)
                }
                killGMSPackages()
                Toast.makeText(requireContext(), "PIF JSON loaded and packages refreshed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading PIF JSON or setting properties", e)
            Toast.makeText(requireContext(), "Error loading PIF JSON", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val context = requireContext()
        val resolver = context.contentResolver
        
        when (preference) {
            mGmsSpoof -> {
                killGMSPackages()
                return true
            }
            mGoogleSpoof, mGphotosSpoof, mGamePropsSpoof, 
            mQsbSpoof, mSnapSpoof -> {
                SystemRestartUtils.showSystemRestartDialog(requireContext())
                return true
            }
            mTensorSpoof -> {
                val enabled = newValue as Boolean
                SystemProperties.set(SYS_TENSOR_SPOOF, if (enabled) "true" else "false")
                SystemRestartUtils.showSystemRestartDialog(requireContext())
                return true
            }
        }
        return false
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }
}
