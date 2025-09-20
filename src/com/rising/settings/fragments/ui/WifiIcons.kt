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

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment

import com.android.internal.util.android.ThemeUtils

class WifiIcons : OptimizedSettingsFragment() {

    private var mRecyclerView: RecyclerView? = null
    override var mThemeUtils: ThemeUtils? = null
    private val mCategory = "android.theme.customization.wifi_icon"

    private var mPkgs: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.theme_customization_wifi_icon_title)

        mThemeUtils = ThemeUtils.getInstance(activity)
        mPkgs = mThemeUtils?.getOverlayPackagesForCategory(mCategory, "android")
    }

    override fun onCreateView(
        @NonNull inflater: LayoutInflater, 
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.item_view, container, false)

        mRecyclerView = view.findViewById(R.id.recycler_view)
        val gridLayoutManager = GridLayoutManager(activity, 3)
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
            val v = LayoutInflater.from(parent.context).inflate(R.layout.icon_option, parent, false)
            return CustomViewHolder(v)
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val iconPkg = mPkgs?.get(position) ?: return

            holder.image1?.background = getDrawable(holder.image1?.context, iconPkg, "ic_wifi_signal_0")
            holder.image2?.background = getDrawable(holder.image2?.context, iconPkg, "ic_wifi_signal_2")
            holder.image3?.background = getDrawable(holder.image3?.context, iconPkg, "ic_wifi_signal_3")
            holder.image4?.background = getDrawable(holder.image4?.context, iconPkg, "ic_wifi_signal_4")

            val currentPackageName = mThemeUtils?.getOverlayInfos(mCategory)
                ?.filter { it.isEnabled }
                ?.map { it.packageName }
                ?.firstOrNull() ?: "android"

            holder.name?.text = if ("android" == iconPkg) "Default" else getLabel(holder.name?.context, iconPkg)

            if (currentPackageName == iconPkg) {
                mAppliedPkg = iconPkg
                if (mSelectedPkg == null) {
                    mSelectedPkg = iconPkg
                }
            }

            holder.itemView.isActivated = iconPkg == mSelectedPkg
            holder.itemView.setOnClickListener {
                updateActivatedStatus(mSelectedPkg, false)
                updateActivatedStatus(iconPkg, true)
                mSelectedPkg = iconPkg
                enableOverlays(position)
            }
        }

        override fun getItemCount(): Int {
            return mPkgs?.size ?: 0
        }

        inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView? = itemView.findViewById(R.id.option_label)
            val image1: ImageView? = itemView.findViewById(R.id.image1)
            val image2: ImageView? = itemView.findViewById(R.id.image2)
            val image3: ImageView? = itemView.findViewById(R.id.image3)
            val image4: ImageView? = itemView.findViewById(R.id.image4)
        }

        private fun updateActivatedStatus(pkg: String?, isActivated: Boolean) {
            val index = mPkgs?.indexOf(pkg) ?: -1
            if (index < 0) {
                return
            }
            val holder = mRecyclerView?.findViewHolderForAdapterPosition(index)
            holder?.itemView?.isActivated = isActivated
        }
    }

    private fun getDrawable(context: Context?, pkg: String, drawableName: String): Drawable? {
        return try {
            val pm = context?.packageManager
            val res = if (pkg == "android") {
                Resources.getSystem()
            } else {
                pm?.getResourcesForApplication(pkg)
            }
            val resId = res?.getIdentifier(drawableName, "drawable", pkg) ?: 0
            res?.getDrawable(resId)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun getLabel(context: Context?, pkg: String): String {
        val pm = context?.packageManager
        return try {
            pm?.getApplicationInfo(pkg, 0)?.loadLabel(pm)?.toString() ?: pkg
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            pkg
        }
    }

    private fun enableOverlays(position: Int) {
        val pkg = mPkgs?.get(position)
        if (pkg != null) {
            mThemeUtils?.setOverlayEnabled(mCategory, pkg, "android")
        }
    }
}
