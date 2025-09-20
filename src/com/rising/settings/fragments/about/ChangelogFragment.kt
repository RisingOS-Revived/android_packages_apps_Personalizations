/*
 * Copyright (C) 2016-2021 crDroid Android Project
 * Copyright (C) 2023-2024 the risingOS Android Project
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
package com.rising.settings.fragments.about

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.preference.PreferenceFragment

import com.android.settings.R

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class ChangelogFragment : PreferenceFragment() {

    private lateinit var textView: TextView

    companion object {
        private const val README_URL = "https://raw.githubusercontent.com/RisingOS-Revived/risingOS_changelogs/fifteen/README.md"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.changelog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView = view.findViewById(R.id.changelog_text)
        fetchReadmeContent(README_URL)
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private fun fetchReadmeContent(readmeUrl: String) {
        executor.execute {
            val content = try {
                val url = URL(readmeUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val inputStreamReader = InputStreamReader(connection.inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val stringBuilder = StringBuilder()
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n")
                }
                bufferedReader.close()
                inputStreamReader.close()
                stringBuilder.toString()
            } catch (e: IOException) {
                ""
            }
            
            // Update UI on main thread
            activity?.runOnUiThread {
                if (isAdded && content.isNotEmpty()) {
                    updateTextView(content)
                }
            }
        }
    }
    
    private fun updateTextView(content: String) {
        val pattern = Pattern.compile("\\*\\*(.*?)\\*\\*")
        val matcher = pattern.matcher(content)
        val spannableBuilder = SpannableStringBuilder()
        var lastEnd = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            val matchedText = matcher.group(1)
            spannableBuilder.append(content.subSequence(lastEnd, start))
            spannableBuilder.append(
                matchedText,
                StyleSpan(android.graphics.Typeface.BOLD),
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            lastEnd = end
        }
        spannableBuilder.append(content.subSequence(lastEnd, content.length))
        textView.text = spannableBuilder
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }
}
