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
package com.rising.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.preferences.BootAnimationPreviewPreference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import android.os.AsyncTask;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SearchIndexable
public class BootAnimationSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String BOOTANIMATION_STYLE_KEY = "persist.sys.bootanimation_style";
    private static final String TAG = "BootAnimationSettings";
    private static final int REQUEST_CODE_PICK_ZIP = 1001;
    private static final String CUSTOM_BOOTANIMATION_FILE = "/data/misc/bootanim/bootanimation.zip";

    private static final String[] PRODUCT_BOOT_ANIMATION_FILES = {
        "/product/media/bootanimation_rising.zip",
        "/product/media/bootanimation_cyberpunk.zip",
        "/product/media/bootanimation_google.zip",
        "/product/media/bootanimation_google_monet.zip",
        "/product/media/bootanimation_valorant.zip"
    };

    private ListPreference mBootAnimationStyle;
    
    // Background executor for file operations
    private ExecutorService mFileExecutor;
    
    // Cache for file existence checks
    private final java.util.Map<String, Boolean> mFileExistenceCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rising_settings_bootanimation);
        
        // Initialize background executor
        mFileExecutor = Executors.newSingleThreadExecutor();

        mBootAnimationStyle = (ListPreference) findPreference(BOOTANIMATION_STYLE_KEY);
        if (mBootAnimationStyle != null) {
            mBootAnimationStyle.setOnPreferenceChangeListener(this);

            // Set the current value from the system property
            int currentStyle = SystemProperties.getInt(BOOTANIMATION_STYLE_KEY, 0);
            mBootAnimationStyle.setValue(String.valueOf(currentStyle));
            updateBootAnimationPreview();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mBootAnimationStyle) {
            int style = Integer.parseInt((String) newValue);
            if (style == 5) { // Custom option selected
                launchFilePicker();
                return false; // Return false to prevent immediate property update
            } else {
                copyProductFile(style);
                return true;
            }
        }
        return false;
    }

    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        startActivityForResult(intent, REQUEST_CODE_PICK_ZIP);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_ZIP && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                handleSelectedFile(uri);
            }
        }
    }

    private void handleSelectedFile(Uri uri) {
        // Perform file operations in background to avoid blocking UI
        new CopyFileTask(this, uri, true).execute();
    }
    
    private void copyProductFile(int style) {
        // Validate input
        if (style < 0 || style >= PRODUCT_BOOT_ANIMATION_FILES.length) {
            Log.e(TAG, "Invalid style index");
            return;
        }
        
        String productFilePath = PRODUCT_BOOT_ANIMATION_FILES[style];
        
        // Check cache first
        Boolean fileExists = mFileExistenceCache.get(productFilePath);
        if (fileExists == null) {
            fileExists = new File(productFilePath).exists();
            mFileExistenceCache.put(productFilePath, fileExists);
        }
        
        if (!fileExists) {
            Log.e(TAG, "Product file does not exist: " + productFilePath);
            return;
        }
        
        // Perform file operations in background
        new CopyFileTask(this, productFilePath, style, false).execute();
    }

    private void updateBootAnimationPreview() {
        BootAnimationPreviewPreference previewPreference = (BootAnimationPreviewPreference) findPreference("bootanimation_preview");
        if (previewPreference != null) {
            previewPreference.loadBootAnimationPreview();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rising_settings_bootanimation;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };

    // Background task for file operations
    private static class CopyFileTask extends AsyncTask<Void, Void, Boolean> {
        private final WeakReference<BootAnimationSettings> mFragmentRef;
        private final Object mSource; // Uri or String path
        private final int mStyle;
        private final boolean mIsCustomFile;
        private String mErrorMessage;
        
        // Constructor for custom file (Uri)
        CopyFileTask(BootAnimationSettings fragment, Uri uri, boolean isCustom) {
            mFragmentRef = new WeakReference<>(fragment);
            mSource = uri;
            mStyle = 5; // Custom style
            mIsCustomFile = isCustom;
        }
        
        // Constructor for product file (String path)
        CopyFileTask(BootAnimationSettings fragment, String path, int style, boolean isCustom) {
            mFragmentRef = new WeakReference<>(fragment);
            mSource = path;
            mStyle = style;
            mIsCustomFile = isCustom;
        }
        
        @Override
        protected Boolean doInBackground(Void... params) {
            BootAnimationSettings fragment = mFragmentRef.get();
            if (fragment == null || !fragment.isAdded()) {
                return false;
            }
            
            try {
                InputStream inputStream;
                if (mSource instanceof Uri) {
                    inputStream = fragment.getContext().getContentResolver().openInputStream((Uri) mSource);
                } else {
                    inputStream = new FileInputStream(new File((String) mSource));
                }
                
                if (inputStream == null) {
                    mErrorMessage = "Failed to open input stream";
                    return false;
                }
                
                File customBootAnimation = new File(CUSTOM_BOOTANIMATION_FILE);
                customBootAnimation.getParentFile().mkdirs();
                
                try (OutputStream outputStream = new FileOutputStream(customBootAnimation)) {
                    byte[] buffer = new byte[8192]; // Larger buffer for better performance
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    outputStream.flush();
                }
                inputStream.close();
                
                return true;
            } catch (Exception e) {
                mErrorMessage = "Error copying bootanimation: " + e.getMessage();
                Log.e(TAG, mErrorMessage, e);
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            BootAnimationSettings fragment = mFragmentRef.get();
            if (fragment == null || !fragment.isAdded()) {
                return;
            }
            
            if (success) {
                SystemProperties.set(BOOTANIMATION_STYLE_KEY, String.valueOf(mStyle));
                fragment.updateBootAnimationPreview();
                fragment.mBootAnimationStyle.setValue(String.valueOf(mStyle));
                Toast.makeText(fragment.getContext(), R.string.boot_animation_applied, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(fragment.getContext(), "Failed to apply boot animation: " + mErrorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup executor and cache
        if (mFileExecutor != null && !mFileExecutor.isShutdown()) {
            mFileExecutor.shutdown();
        }
        mFileExistenceCache.clear();
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        // Additional cleanup
        mFileExistenceCache.clear();
    }
}
