/*
 * Copyright (C) 2022-2024 crDroid Android Project
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

package com.rising.settings.fragments.lockscreen

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import android.net.Uri
import android.provider.SearchIndexableResource
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.res.ResourcesCompat
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.rising.settings.fragments.OptimizedSettingsFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.Indexable

import java.util.ArrayList
import java.util.Arrays
import java.util.List

import org.json.JSONException
import org.json.JSONObject

class UdfpsIconPicker : OptimizedSettingsFragment() {

    private lateinit var mRecyclerView: RecyclerView
    private var udfpsRes: Resources? = null
    private val mPkg = "com.crdroid.udfps.icons"
    private lateinit var mIcons: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.udfps_icon_picker_title)

        loadResources()
    }

    private fun loadResources() {
        try {
            val context = getSafeContext()
            if (context != null) {
                udfpsRes = context.packageManager?.getResourcesForApplication(mPkg)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle silently - package not found
        }

        udfpsRes?.let { res ->
            mIcons = res.getStringArray(res.getIdentifier("udfps_icons", "array", mPkg))
        }
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
        val mUdfpsIconAdapter = UdfpsIconAdapter(activity!!)
        mRecyclerView.adapter = mUdfpsIconAdapter

        return view
    }

    companion object {
        @JvmStatic
        fun reset(mContext: Context) {
            val resolver = mContext.contentResolver
            Settings.System.putIntForUser(resolver,
                "udfps_icon", 0, UserHandle.USER_CURRENT)
        }
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }

    override fun onResume() {
        super.onResume()
    }

    inner class UdfpsIconAdapter(val context: Context) : RecyclerView.Adapter<UdfpsIconAdapter.UdfpsIconViewHolder>() {
        private var mSelectedIcon: String? = null
        private var mAppliedIcon: String? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UdfpsIconViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_option, parent, false)
            return UdfpsIconViewHolder(v)
        }

        override fun onBindViewHolder(holder: UdfpsIconViewHolder, position: Int) {
            val iconRes = mIcons[position]

            Glide.with(holder.image.context)
                .load("")
                .placeholder(getDrawable(holder.image.context, mIcons[position]))
                .into(holder.image)

            holder.image.setPadding(20, 20, 20, 20)
            holder.name.visibility = View.GONE

            if (position == Settings.System.getInt(context.contentResolver, "udfps_icon", 0)) {
                mAppliedIcon = iconRes
                if (mSelectedIcon == null) {
                    mSelectedIcon = iconRes
                }
            }
            holder.itemView.isActivated = iconRes == mSelectedIcon
            holder.itemView.setOnClickListener {
                updateActivatedStatus(mSelectedIcon, false)
                updateActivatedStatus(iconRes, true)
                mSelectedIcon = iconRes
                Settings.System.putInt(activity?.contentResolver,
                    "udfps_icon", position)
            }
        }

        override fun getItemCount(): Int {
            return mIcons.size
        }

        inner class UdfpsIconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.option_label)
            val image: ImageView = itemView.findViewById(R.id.option_thumbnail)
        }

        private fun updateActivatedStatus(icon: String?, isActivated: Boolean) {
            val index = mIcons.indexOf(icon)
            if (index < 0) {
                return
            }
            val holder = mRecyclerView.findViewHolderForAdapterPosition(index)
            holder?.itemView?.isActivated = isActivated
        }
    }

    private fun getDrawable(context: Context, drawableName: String): Drawable? {
        return try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(mPkg)
            val ctx = context.createPackageContext(mPkg, Context.CONTEXT_IGNORE_SECURITY)
            ctx.getDrawable(res.getIdentifier(drawableName, "drawable", mPkg))
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle silently - package not found
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        udfpsRes = null
    }
}
