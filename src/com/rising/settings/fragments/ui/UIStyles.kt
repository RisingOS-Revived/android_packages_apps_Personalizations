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

package com.rising.settings.fragments.ui

import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import java.lang.ref.WeakReference

import com.android.internal.util.android.ThemeUtils

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import org.json.JSONObject
import org.json.JSONException

class UIStyles : OptimizedSettingsFragment() {

    private lateinit var mRecyclerView: RecyclerView
    override var mThemeUtils: ThemeUtils? = null
    private val mCategory = "android.theme.customization.style.android"
    private lateinit var mPkgs: List<String>

    private val mExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mApplyingOverlays = AtomicBoolean(false)

    private val overlayMap = mapOf(
        "com.android.settings" to "android.theme.customization.style.settings",
        "com.android.systemui" to "android.theme.customization.style.systemui"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.theme_customization_ui_style_title)

        activity?.let {
            mThemeUtils = ThemeUtils.getInstance(it)
        }
        mThemeUtils?.let {
            mPkgs = it.getOverlayPackagesForCategory(mCategory, "android")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.item_view, container, false)

        mRecyclerView = view.findViewById(R.id.recycler_view)
        val gridLayoutManager = GridLayoutManager(activity, 1)
        mRecyclerView.layoutManager = gridLayoutManager
        val mAdapter = Adapter(activity!!)
        mRecyclerView.adapter = mAdapter

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup executor
        if (!mExecutor.isShutdown) {
            mExecutor.shutdown()
        }
        mThemeUtils = null
        mApplyingOverlays.set(false)
    }

    override fun onDetach() {
        super.onDetach()
        // Additional cleanup
        mApplyingOverlays.set(false)
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
            val v = LayoutInflater.from(parent.context).inflate(R.layout.fonts_option, parent, false)
            return CustomViewHolder(v)
        }

        override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
            val pkg = mPkgs[position]
            val label = getLabel(holder.itemView.context, pkg)

            val currentPackageName = mThemeUtils?.getOverlayInfos(mCategory)
                ?.filter { it.isEnabled }
                ?.map { it.packageName }
                ?.firstOrNull() ?: "android"

            holder.title.text = if (pkg == "android") "Default" else label
            holder.title.textSize = 20f
            holder.name.visibility = View.GONE

            if (currentPackageName == pkg) {
                mAppliedPkg = pkg
                if (mSelectedPkg == null) {
                    mSelectedPkg = pkg
                }
            }

            holder.itemView.isActivated = pkg == mSelectedPkg
            holder.itemView.setOnClickListener {
                if (mApplyingOverlays.get()) return@setOnClickListener
                updateActivatedStatus(mSelectedPkg, false)
                updateActivatedStatus(pkg, true)
                mSelectedPkg = pkg
                enableOverlays(position)
            }
        }

        override fun getItemCount(): Int {
            return mPkgs.size
        }

        inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.option_label)
            val title: TextView = itemView.findViewById(R.id.option_title)
        }

        private fun updateActivatedStatus(pkg: String?, isActivated: Boolean) {
            val index = mPkgs.indexOf(pkg)
            if (index < 0) {
                return
            }
            val holder = mRecyclerView.findViewHolderForAdapterPosition(index)
            holder?.itemView?.isActivated = isActivated
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
        mApplyingOverlays.set(true)
        mExecutor.execute {
            mThemeUtils?.setOverlayEnabled(mCategory, mPkgs[position], "android")
            val pattern = if (mPkgs[position] == "android") {
                ""
            } else {
                mPkgs[position].split(".")[4]
            }
            for ((target, category) in overlayMap) {
                enableOverlay(category, target, pattern)
            }
            postDelayedSafe({ mApplyingOverlays.set(false) }, 0)
        }
    }

    private fun enableOverlay(category: String, target: String, pattern: String) {
        if (pattern.isEmpty()) {
            mThemeUtils?.setOverlayEnabled(category, "android", "android")
            return
        }
        mThemeUtils?.getOverlayPackagesForCategory(category, target)?.forEach { pkg ->
            if (pkg.contains(pattern)) {
                mThemeUtils?.setOverlayEnabled(category, pkg, target)
            }
        }
    }
}
