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
import android.content.Context
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

@SearchIndexable
class Backup : SettingsPreferenceFragment() {

    companion object {
        private const val TAG = "Backup"
        private const val BACKUP_PERSONALIZATION_SETTINGS = "backup_personalization_settings"
        private const val RESTORE_PERSONALIZATION_SETTINGS = "restore_personalization_settings"
        private const val UPLOAD_BACKUP_TO_DRIVE = "upload_backup_to_drive"
        private const val DOWNLOAD_BACKUP_FROM_DRIVE = "download_backup_from_drive"

        /**
         * For search
         */
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_backup) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                return super.getNonIndexableKeys(context)
            }
        }
    }

    private lateinit var backupLauncher: ActivityResultLauncher<Intent>
    private lateinit var restoreLauncher: ActivityResultLauncher<Intent>
    private lateinit var uploadLauncher: ActivityResultLauncher<Intent>
    private lateinit var downloadLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.rising_settings_backup)

        val prefScreen: PreferenceScreen = preferenceScreen
        val mContext: Context = requireActivity().applicationContext

        // Initialize ActivityResultLaunchers for file selection
        backupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    Log.d(TAG, "Backup URI: $uri")
                    backupSettings(mContext, uri)
                } else {
                    Log.e(TAG, "Backup URI is null")
                }
            } else {
                Log.e(TAG, "Backup activity result not OK or data is null")
            }
        }

        restoreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    Log.d(TAG, "Restore URI: $uri")
                    restoreSettings(mContext, uri)
                } else {
                    Log.e(TAG, "Restore URI is null")
                }
            } else {
                Log.e(TAG, "Restore activity result not OK or data is null")
            }
        }

        // Initialize ActivityResultLaunchers for file selection
        uploadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    Log.d(TAG, "Selected file URI: $uri")
                    uploadFileToDrive(mContext, uri)
                } else {
                    Log.e(TAG, "Selected file URI is null")
                }
            } else {
                Log.e(TAG, "Upload activity result not OK or data is null")
            }
        }

        downloadLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    Log.d(TAG, "Download URI: $uri")
                    restoreSettings(mContext, uri)
                } else {
                    Log.e(TAG, "Download URI is null")
                }
            } else {
                Log.e(TAG, "Download activity result not OK or data is null")
            }
        }

        // Backup settings
        val backupPref: Preference? = findPreference(BACKUP_PERSONALIZATION_SETTINGS)
        backupPref?.setOnPreferenceClickListener {
            Log.d(TAG, "Backup option clicked")
            chooseFileLocationForBackup()
            true
        }

        // Restore settings
        val restorePref: Preference? = findPreference(RESTORE_PERSONALIZATION_SETTINGS)
        restorePref?.setOnPreferenceClickListener {
            Log.d(TAG, "Restore option clicked")
            chooseFileForRestore()
            true
        }

        // Upload backup to Google Drive
        val uploadToDrivePref: Preference? = findPreference(UPLOAD_BACKUP_TO_DRIVE)
        uploadToDrivePref?.setOnPreferenceClickListener {
            Log.d(TAG, "Upload to Drive option clicked")
            chooseFileForUpload()
            true
        }

        // Download backup from Google Drive
        val downloadFromDrivePref: Preference? = findPreference(DOWNLOAD_BACKUP_FROM_DRIVE)
        downloadFromDrivePref?.setOnPreferenceClickListener {
            Log.d(TAG, "Download from Drive option clicked")
            downloadBackupFromDrive()
            true
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }

    private fun chooseFileLocationForBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "personalization_settings_backup.json")
        }
        Log.d(TAG, "Launching file picker for backup")
        backupLauncher.launch(intent)
    }

    private fun chooseFileForRestore() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
        }
        Log.d(TAG, "Launching file picker for restore")
        restoreLauncher.launch(intent)
    }

    private fun chooseFileForUpload() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
        }
        Log.d(TAG, "Launching file picker for upload")
        uploadLauncher.launch(intent)
    }

    private fun uploadFileToDrive(context: Context, fileUri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                fileUri, 
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val uploadIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                setPackage("com.google.android.apps.docs")
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or 
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            if (uploadIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(uploadIntent)
            } else {
                Toast.makeText(context, "Google Drive app not installed.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file to Google Drive: ${e.message}", e)
            Toast.makeText(context, "Failed to upload file to Google Drive.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadBackupFromDrive() {
        val downloadIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
            setPackage("com.google.android.apps.docs")
        }

        if (downloadIntent.resolveActivity(requireContext().packageManager) != null) {
            downloadLauncher.launch(downloadIntent)
        } else {
            Toast.makeText(requireContext(), "Google Drive app not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun backupSettings(context: Context, uri: Uri) {
        try {
            val json = JSONObject()

            // Back up System settings
            backupSettingsProvider(json, Settings.System::class.java, context.contentResolver)
            
            // Back up Secure settings
            backupSettingsProvider(json, Settings.Secure::class.java, context.contentResolver)
            
            // Back up Global settings
            backupSettingsProvider(json, Settings.Global::class.java, context.contentResolver)

            // Write the JSON object to the backup file
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toString().toByteArray(StandardCharsets.UTF_8))
                
                // Save the backup locally
                val backupFile = File(context.cacheDir, "personalization_settings_backup.json")
                FileOutputStream(backupFile).use { os ->
                    os.write(json.toString().toByteArray(StandardCharsets.UTF_8))
                }
                Toast.makeText(requireActivity(), "Personalization settings backed up successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireActivity(), "Failed to backup settings", Toast.LENGTH_SHORT).show()
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(requireActivity(), "Failed to backup settings", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(JSONException::class)
    private fun backupSettingsProvider(json: JSONObject, settingsClass: Class<*>, resolver: ContentResolver) {
        try {
            // Determine which Settings class we're working with
            val (uri, methodName) = when (settingsClass) {
                Settings.System::class.java -> Settings.System.CONTENT_URI to "System"
                Settings.Secure::class.java -> Settings.Secure.CONTENT_URI to "Secure"
                Settings.Global::class.java -> Settings.Global.CONTENT_URI to "Global"
                else -> return // Unsupported settings type
            }
            
            // Query all settings from this provider
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                // Create a JSON object for this settings type
                val settingsJson = JSONObject()
                
                // Get column indices
                val nameIndex = cursor.getColumnIndex("name")
                val valueIndex = cursor.getColumnIndex("value")
                
                // Extract all settings
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    val value = cursor.getString(valueIndex)
                    
                    if (name != null && value != null) {
                        settingsJson.put(name, value)
                    }
                }
                
                // Add this settings type to the main JSON object
                json.put(methodName, settingsJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error backing up ${settingsClass.simpleName} settings", e)
        }
    }

    private fun restoreSettings(context: Context, uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val builder = StringBuilder()
                var ch: Int
                while (inputStream.read().also { ch = it } != -1) {
                    builder.append(ch.toChar())
                }

                val json = JSONObject(builder.toString())
                val resolver = context.contentResolver
                
                // Restore System settings
                if (json.has("System")) {
                    restoreSettingsProvider(json.getJSONObject("System"), Settings.System::class.java, resolver)
                }
                
                // Restore Secure settings
                if (json.has("Secure")) {
                    restoreSettingsProvider(json.getJSONObject("Secure"), Settings.Secure::class.java, resolver)
                }
                
                // Restore Global settings
                if (json.has("Global")) {
                    restoreSettingsProvider(json.getJSONObject("Global"), Settings.Global::class.java, resolver)
                }

                // Force refresh settings
                context.contentResolver.notifyChange(Settings.System.CONTENT_URI, null)
                context.contentResolver.notifyChange(Settings.Secure.CONTENT_URI, null)
                context.contentResolver.notifyChange(Settings.Global.CONTENT_URI, null)

                Toast.makeText(requireActivity(), "Personalization settings restored successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireActivity(), "Failed to restore settings", Toast.LENGTH_SHORT).show()
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(requireActivity(), "Failed to restore settings", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(JSONException::class)
    private fun restoreSettingsProvider(settingsJson: JSONObject, settingsClass: Class<*>, resolver: ContentResolver) {
        try {
            val keys = settingsJson.keys()
            
            while (keys.hasNext()) {
                val key = keys.next()
                val value = settingsJson.getString(key)
                
                when (settingsClass) {
                    Settings.System::class.java -> Settings.System.putString(resolver, key, value)
                    Settings.Secure::class.java -> Settings.Secure.putString(resolver, key, value)
                    Settings.Global::class.java -> Settings.Global.putString(resolver, key, value)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring ${settingsClass.simpleName} settings", e)
        }
    }
}
