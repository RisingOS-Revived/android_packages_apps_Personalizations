/*
 * Copyright (C) 2022 crDroid Android Project
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

package com.rising.settings.fragments.ui

import com.android.internal.util.android.ThemeUtils.ICON_SHAPE_KEY

import android.content.ContentResolver
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.os.Bundle
import android.provider.SearchIndexableResource
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.TextView
import android.text.TextUtils
import androidx.preference.PreferenceViewHolder
import android.view.ViewGroup.LayoutParams

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.RecyclerView
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceScreen

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.Indexable
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settingslib.Utils

import com.bumptech.glide.Glide

import com.android.internal.util.android.ThemeUtils

import org.json.JSONObject
import org.json.JSONException

class IconShapes : OptimizedSettingsFragment() {

    private lateinit var mRecyclerView: RecyclerView
    override var mThemeUtils: ThemeUtils? = null
    private val mCategory = ICON_SHAPE_KEY
    private lateinit var mPkgs: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.theme_customization_icon_shape_title)

        mThemeUtils = ThemeUtils.getInstance(activity)
        mPkgs = mThemeUtils?.getOverlayPackagesForCategory(mCategory, "android") ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.item_view, container, false)

        mRecyclerView = view.findViewById(R.id.recycler_view)
        val gridLayoutManager = GridLayoutManager(activity, 3)
        mRecyclerView.layoutManager = gridLayoutManager
        val mAdapter = Adapter(activity!!)
        mRecyclerView.adapter = mAdapter

        return view
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }

    override fun onResume() {
        super.onResume()
    }

    inner class Adapter(val context: Context) : RecyclerView.Adapter<Adapter.CustomViewHolder>() {
        private var mSelectedPkg: String? = null
        private var mAppliedPkg: String? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_option, parent, false)
            return CustomViewHolder(v)
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val pkg = mPkgs[position]

            holder.image.background = mThemeUtils?.createShapeDrawable(pkg)

            val currentPackageName = mThemeUtils?.getOverlayInfos(mCategory)
                ?.filter { it.isEnabled }
                ?.map { it.packageName }
                ?.firstOrNull() ?: "android"

            holder.name.text = if (pkg == "android") "Default" else getLabel(holder.name.context, pkg)

            val isDefault = currentPackageName == "android" && pkg == "android"
            val color = ColorUtils.setAlphaComponent(
                Utils.getColorAttrDefaultColor(context, android.R.attr.colorAccent),
                if (pkg == currentPackageName || isDefault) 170 else 75
            )
            holder.image.backgroundTintList = ColorStateList.valueOf(color)

            holder.itemView.findViewById<View>(R.id.option_tile).background = null
            holder.itemView.setOnClickListener {
                enableOverlays(position)
            }
        }

        override fun getItemCount(): Int {
            return mPkgs.size
        }

        inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.option_label)
            val image: ImageView = itemView.findViewById(R.id.option_thumbnail)
        }
    }

    private fun getDrawable(context: Context, pkg: String, drawableName: String): Drawable? {
        return try {
            val pm = context.packageManager
            val res = if (pkg == "android") {
                Resources.getSystem()
            } else {
                pm.getResourcesForApplication(pkg)
            }
            res.getDrawable(res.getIdentifier(drawableName, "drawable", pkg))
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun getLabel(context: Context, pkg: String): String {
        val pm = context.packageManager
        return try {
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            pkg
        }
    }

    private fun enableOverlays(position: Int) {
        mThemeUtils?.setOverlayEnabled(mCategory, mPkgs[position], "android")
    }
}
