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
package com.rising.settings.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.rising.settings.fragments.OptimizedSettingsFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

@SearchIndexable
public class Assistant extends OptimizedSettingsFragment {

    private static final String TAG = "Assistant";
    
    private static final String KEY_AI_ASSISTANT_GEMINI_KEY = "ai_assistant_gemini_key";

    private Preference mAiAssistantKeyPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rising_settings_ai_assistant);
        mAiAssistantKeyPreference = findCachedPreference(KEY_AI_ASSISTANT_GEMINI_KEY);
        if (mAiAssistantKeyPreference != null) {
            Context context = getSafeContext();
            String currentKey = context != null ? Settings.System.getString(context.getContentResolver(), KEY_AI_ASSISTANT_GEMINI_KEY) : null;
            mAiAssistantKeyPreference.setSummary(TextUtils.isEmpty(currentKey) ? getString(R.string.ai_assistant_gemini_key_summary) : currentKey);
            mAiAssistantKeyPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showApiKeyDialog();
                    return true;
                }
            });
        }
        
        Preference mWikiLink = findCachedPreference("wiki_link_assistant");
        if (mWikiLink != null) {
            mWikiLink.setOnPreferenceClickListener(preference -> {
                Uri uri = Uri.parse("https://github.com/RisingOS-Revived/risingOS_wiki/blob/fifteen/assistant/Risa/README.md");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            });
        }
    }

    private void showApiKeyDialog() {
        Context context = getSafeContext();
        if (context == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.ai_assistant_gemini_key_dialog_title);

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        String currentKey = Settings.System.getString(context.getContentResolver(), KEY_AI_ASSISTANT_GEMINI_KEY);
        if (!TextUtils.isEmpty(currentKey)) {
            input.setText(currentKey);
            input.setSelection(currentKey.length());
        }

        builder.setView(input);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newApiKey = input.getText().toString();
                if (!TextUtils.isEmpty(newApiKey)) {
                    Context ctx = getSafeContext();
                    if (ctx != null) {
                        Settings.System.putString(ctx.getContentResolver(), KEY_AI_ASSISTANT_GEMINI_KEY, newApiKey);
                    }
                    mAiAssistantKeyPreference.setSummary(newApiKey);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.rising_settings_ai_assistant) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
