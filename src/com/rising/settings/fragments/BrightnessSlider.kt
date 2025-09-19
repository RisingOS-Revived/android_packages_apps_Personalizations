/*
 * Copyright (C) 2021 AospExtended ROM Project
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

package com.rising.settings.fragments

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.internal.util.android.ThemeUtils
import com.android.settings.R

class BrightnessSlider : OptimizedSettingsFragment() {

    private var mRecyclerView: RecyclerView? = null
    override var mThemeUtils: ThemeUtils? = null
    private val mCategory = "android.theme.customization.brightness_slider"

    private var mPkgs: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.theme_customization_brightness_slider_title)

        val context = getSafeContext()
        if (context != null) {
            mThemeUtils = ThemeUtils.getInstance(context)
        }
        mThemeUtils?.let { themeUtils ->
            mPkgs = themeUtils.getOverlayPackagesForCategory(mCategory, "com.android.systemui")
        }
    }

    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?,
            @Nullable savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.item_view, container, false)

        mRecyclerView = view.findViewById(R.id.recycler_view)
        val gridLayoutManager = GridLayoutManager(activity, 1)
        mRecyclerView?.layoutManager = gridLayoutManager
        val mAdapter = Adapter(activity)
        mRecyclerView?.adapter = mAdapter

        return view
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }

    override fun onResume() {
        super.onResume()
    }

    inner class Adapter(private val context: Context?) : RecyclerView.Adapter<Adapter.CustomViewHolder>() {
        private var mSelectedPkg: String? = null
        private var mAppliedPkg: String? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.brightness_slider_option, parent, false)
            return CustomViewHolder(v)
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val pkgs = mPkgs ?: return
            val navPkg = pkgs[position]

            val currentPackageName = mThemeUtils?.getOverlayInfos(mCategory, "com.android.systemui")
                ?.filter { it.isEnabled }
                ?.map { it.packageName }
                ?.firstOrNull() ?: "com.android.systemui"

            holder.name.text = if ("com.android.systemui" == navPkg) "Default" else getLabel(holder.name.context, navPkg)
            holder.name.textSize = 24f

            if (currentPackageName == navPkg) {
                mAppliedPkg = navPkg
                if (mSelectedPkg == null) {
                    mSelectedPkg = navPkg
                }
            }

            holder.itemView.isActivated = navPkg == mSelectedPkg
            holder.itemView.setOnClickListener {
                updateActivatedStatus(mSelectedPkg, false)
                updateActivatedStatus(navPkg, true)
                mSelectedPkg = navPkg
                enableOverlays(position)
            }
        }

        override fun getItemCount(): Int {
            return mPkgs?.size ?: 0
        }

        inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.option_label)
        }

        private fun updateActivatedStatus(pkg: String?, isActivated: Boolean) {
            val pkgs = mPkgs ?: return
            val index = pkgs.indexOf(pkg)
            if (index < 0) {
                return
            }
            val holder = mRecyclerView?.findViewHolderForAdapterPosition(index)
            holder?.itemView?.isActivated = isActivated
        }
    }

    private fun getDrawable(context: Context, pkg: String, drawableName: String): Drawable? {
        return try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(pkg)
            val resId = res.getIdentifier(drawableName, "drawable", pkg)
            res.getDrawable(resId)
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle silently - package not found
            null
        }
    }

    private fun getLabel(context: Context, pkg: String): String {
        val pm = context.packageManager
        return try {
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle silently - package not found
            pkg
        }
    }

    private fun enableOverlays(position: Int) {
        val pkgs = mPkgs
        if (mThemeUtils != null && pkgs != null && position < pkgs.size) {
            mThemeUtils?.setOverlayEnabled(mCategory, pkgs[position], "com.android.systemui")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mThemeUtils = null
        mPkgs = null
    }
}
