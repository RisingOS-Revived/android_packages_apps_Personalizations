/*
 * Copyright (C) 2023 The risingOS Android Project
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
package com.rising.settings.fragments.ui.fonts

import android.content.Context
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import android.text.SpannableString
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.TypedValue

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class FontPickerPreview : OptimizedSettingsFragment() {

    private lateinit var fontSelector: TextView
    private lateinit var previewText: TextView
    private lateinit var fontManager: FontManager
    private lateinit var applyFab: ExtendedFloatingActionButton
    private var currentFontPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fontManager = FontManager(requireActivity(), false)
        activity?.title = activity?.getString(R.string.font_styles_title)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.font_picker_preview, container, false)
        
        fontSelector = rootView.findViewById(R.id.font_selector)
        previewText = rootView.findViewById(R.id.font_preview_text)
        
        val text = previewText.text.toString()
        val spannableString = SpannableString(text)
        val typedValue = TypedValue()
        activity?.theme?.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        val colorAccent = typedValue.data
        val startIndex = text.indexOf("A")
        val endIndex = text.length
        spannableString.setSpan(
            ForegroundColorSpan(colorAccent), 
            startIndex, 
            endIndex, 
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        previewText.text = spannableString
        
        val fontPackageNames = fontManager.allFontPackages

        val backgroundColor = ContextCompat.getColor(requireContext(), 
                if (isNightMode()) R.color.font_drop_down_bg_dark else R.color.font_drop_down_bg_light)
        fontSelector.setTextColor(ContextCompat.getColor(requireContext(),
                if (isNightMode()) R.color.font_drop_down_bg_light else R.color.font_drop_down_bg_dark))
        fontSelector.backgroundTintList = ColorStateList.valueOf(backgroundColor)

        fontSelector.setOnClickListener { v ->
            val popupView = LayoutInflater.from(activity).inflate(R.layout.popup_font_selector, null)
            val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

            val fontListView = popupView.findViewById<ListView>(R.id.font_list_view)
            val fontAdapter = FontArrayAdapter(
                    activity!!,
                    android.R.layout.simple_list_item_1,
                    fontPackageNames,
                    fontManager,
                    isNightMode()
            )
            fontListView.adapter = fontAdapter

            fontListView.setOnItemClickListener { _, _, position, _ ->
                currentFontPosition = position
                val fontPackage = fontPackageNames[currentFontPosition]
                applyFontToPreview(fontPackage)
                fontSelector.text = fontManager.getLabel(requireContext(), fontPackage)
                popupWindow.dismiss()
            }

            popupView.setBackgroundResource(R.drawable.custom_background)
            val backgroundDrawable = popupView.background
            backgroundDrawable?.setColorFilter(backgroundColor, PorterDuff.Mode.SRC_ATOP)
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
            popupWindow.showAsDropDown(v, 0, 10)
        }

        applyFab = rootView.findViewById(R.id.apply_extended_fab)
        applyFab.setOnClickListener {
            if (currentFontPosition != -1) {
                fontManager.enableFontPackage(currentFontPosition)
            }
        }

        val currentFontPackage = fontManager.currentFontPackage
        currentFontPosition = fontPackageNames.indexOf(currentFontPackage)
        if (currentFontPosition != -1) {
            fontSelector.text = fontManager.getLabel(requireContext(), fontPackageNames[currentFontPosition])
            applyFontToPreview(fontPackageNames[currentFontPosition])
        }

        return rootView
    }

    private fun isNightMode(): Boolean {
        val nightModeFlags = requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
        return nightModeFlags?.equals(Configuration.UI_MODE_NIGHT_YES) == true
    }

    private fun applyFontToPreview(fontPackage: String) {
        val typeface = fontManager.getTypeface(requireContext(), fontPackage)
        if (typeface != null) {
            previewText.typeface = typeface
        } else {
            previewText.typeface = Typeface.create("googlesans", Typeface.NORMAL)
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
