/*
 * Copyright (C) 2022-2025 crDroid Android Project
 * Copyright (C) 2025-2026 RisingOS (revived) Android Project
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
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
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

import java.lang.ref.WeakReference

class WifiIcons : OptimizedSettingsFragment() {

    companion object {
        private const val TAG = "WifiIcons"
        private const val ACTION_RELOAD_WIFI_ICONS = "com.android.systemui.RELOAD_WIFI_ICONS"
        private const val WIFI_ICON_USE_OVERLAYS = "wifi_icon_use_overlays"

        private fun sendReloadBroadcast(context: Context) {
            try {
                val intent = Intent(ACTION_RELOAD_WIFI_ICONS)
                context.sendBroadcast(intent)
                Log.d(TAG, "Sent reload broadcast to SystemUI")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send reload broadcast", e)
            }
        }

        private fun updateOverlaySetting(context: Context, useOverlay: Boolean) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    WIFI_ICON_USE_OVERLAYS,
                    if (useOverlay) 1 else 0
                )
                Log.d(TAG, "Updated wifi_icon_use_overlays to: $useOverlay")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update overlay setting", e)
            }
        }
    }

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
        return inflater.inflate(R.layout.item_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler().post {
            mRecyclerView = view.findViewById(R.id.recycler_view)
            mRecyclerView?.let {
                it.layoutManager = GridLayoutManager(activity, 3)
                it.adapter = Adapter(activity, mPkgs, mThemeUtils, mCategory)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRecyclerView?.adapter = null
        mRecyclerView = null
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }

    override fun onResume() {
        super.onResume()
    }

    class Adapter(
        context: Context?,
        private val mPkgs: List<String>?,
        private val mThemeUtils: ThemeUtils?,
        private val mCategory: String
    ) : RecyclerView.Adapter<Adapter.CustomViewHolder>() {

        private val contextRef = WeakReference(context)
        private val mAppliedPkg: String
        private var mSelectedPkg: String

        init {
            mAppliedPkg = mThemeUtils?.getOverlayInfos(mCategory)
                ?.filter { it.isEnabled }
                ?.map { it.packageName }
                ?.firstOrNull() ?: "android"
            mSelectedPkg = mAppliedPkg
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.icon_option, parent, false)
            return CustomViewHolder(v)
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val context = contextRef.get() ?: return
            val pkg = mPkgs?.get(position) ?: return

            holder.image1?.background = getDrawable(context, pkg, "ic_wifi_signal_0")
            holder.image2?.background = getDrawable(context, pkg, "ic_wifi_signal_2")
            holder.image3?.background = getDrawable(context, pkg, "ic_wifi_signal_3")
            holder.image4?.background = getDrawable(context, pkg, "ic_wifi_signal_4")

            val label = getLabel(context, pkg)
            holder.name?.text = if (pkg == "android") "Default" else label
            holder.itemView.isActivated = pkg == mSelectedPkg

            holder.itemView.setOnClickListener {
                if (pkg != mSelectedPkg) {
                    val oldPkg = mSelectedPkg
                    mSelectedPkg = pkg

                    val isDefault = pkg == "android"

                    updateOverlaySetting(context, !isDefault)
                    mThemeUtils?.setOverlayEnabled(mCategory, pkg, "android")

                    updateActivatedStatus(oldPkg)
                    updateActivatedStatus(mSelectedPkg)

                    Handler().postDelayed({
                        sendReloadBroadcast(context)
                    }, 300)

                    Log.d(TAG, "Applied wifi icon overlay: $pkg, use_overlays=${!isDefault}")
                }
            }
        }

        override fun getItemCount(): Int {
            return mPkgs?.size ?: 0
        }

        private fun updateActivatedStatus(pkg: String?) {
            val index = mPkgs?.indexOf(pkg) ?: -1
            if (index >= 0) {
                notifyItemChanged(index)
            }
        }

        class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView? = itemView.findViewById(R.id.option_label)
            val image1: ImageView? = itemView.findViewById(R.id.image1)
            val image2: ImageView? = itemView.findViewById(R.id.image2)
            val image3: ImageView? = itemView.findViewById(R.id.image3)
            val image4: ImageView? = itemView.findViewById(R.id.image4)
        }

        private fun getDrawable(context: Context, pkg: String, drawableName: String): Drawable? {
            return try {
                val pm = context.packageManager
                val res = if (pkg == "android") Resources.getSystem() else pm.getResourcesForApplication(pkg)
                val resId = res.getIdentifier(drawableName, "drawable", pkg)
                if (resId != 0) res.getDrawable(resId, context.theme) else null
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Drawable load failed for pkg: $pkg, name: $drawableName", e)
                null
            }
        }

        private fun getLabel(context: Context, pkg: String): String {
            val pm = context.packageManager
            return try {
                pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Label load failed for pkg: $pkg", e)
                pkg
            }
        }
    }
}
