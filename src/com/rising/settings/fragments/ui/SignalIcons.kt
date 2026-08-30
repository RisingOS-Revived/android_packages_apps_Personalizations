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
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
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

class SignalIcons : OptimizedSettingsFragment() {

    companion object {
        private const val TAG = "SignalIcons"
    }

    private var mRecyclerView: RecyclerView? = null
    override var mThemeUtils: ThemeUtils? = null
    private val mCategory = "android.theme.customization.signal_icon"

    private var mPkgs: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.theme_customization_signal_icon_title)

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
            mAppliedPkg = mThemeUtils?.getOverlayInfos(mCategory, "android")
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

            if (pkg == "android") {
                holder.image1?.background = QPR1SignalDrawable(context, 0)
                holder.image2?.background = QPR1SignalDrawable(context, 1)
                holder.image3?.background = QPR1SignalDrawable(context, 2)
                holder.image4?.background = QPR1SignalDrawable(context, 4)
            } else {
                holder.image1?.background = getDrawable(context, pkg, "ic_signal_cellular_0_5_bar")
                holder.image2?.background = getDrawable(context, pkg, "ic_signal_cellular_1_5_bar")
                holder.image3?.background = getDrawable(context, pkg, "ic_signal_cellular_3_5_bar")
                holder.image4?.background = getDrawable(context, pkg, "ic_signal_cellular_5_5_bar")
            }

            val label = getLabel(context, pkg)
            holder.name?.text = if (pkg == "android") "Default" else label
            holder.itemView.isActivated = pkg == mSelectedPkg

            holder.itemView.setOnClickListener {
                if (pkg != mSelectedPkg) {
                    val oldPkg = mSelectedPkg
                    mSelectedPkg = pkg

                    if (pkg == "android") {
                        Settings.System.putInt(context.contentResolver, "signal_icon_use_overlays", 0)
                    } else {
                        Settings.System.putInt(context.contentResolver, "signal_icon_use_overlays", 1)
                    }

                    mThemeUtils?.setOverlayEnabled(mCategory, pkg, "android")
                    updateActivatedStatus(oldPkg)
                    updateActivatedStatus(mSelectedPkg)
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

    private class QPR1SignalDrawable(context: Context, private val level: Int) : Drawable() {
        private val activePaint: Paint
        private val inactivePaint: Paint
        private val density: Float = context.resources.displayMetrics.density

        init {
            val nightMode = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val isDarkMode = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

            val activeColor = if (isDarkMode) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            val inactiveColor = if (isDarkMode) 0xFF555555.toInt() else 0xFFAAAAAA.toInt()

            activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = activeColor
            }

            inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = inactiveColor
            }
        }

        override fun draw(canvas: Canvas) {
            val width = bounds.width()
            val height = bounds.height()

            val barWidth = 2.5f * density
            val barSpacing = 1.5f * density
            val dotRadius = 1.5f * density

            val totalWidth = (barWidth * 4) + (barSpacing * 3)
            val startX = (width - totalWidth) / 2f
            val baseY = height * 0.7f
            val minBarHeight = 3f * density

            for (i in 0 until 4) {
                val x = startX + (i * (barWidth + barSpacing))
                val barHeight = maxOf(minBarHeight, ((i + 1) / 4f) * (height * 0.45f))
                val barPaint = if (i < level) activePaint else inactivePaint
                canvas.drawRoundRect(
                    x,
                    baseY - barHeight,
                    x + barWidth,
                    baseY,
                    barWidth / 2f,
                    barWidth / 2f,
                    barPaint
                )

                val dotY = baseY + (dotRadius * 2.5f)
                val dotPaint = if (i < level) activePaint else inactivePaint
                canvas.drawCircle(x + barWidth / 2f, dotY, dotRadius, dotPaint)
            }
        }

        override fun setAlpha(alpha: Int) {
            activePaint.alpha = alpha
            inactivePaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            activePaint.colorFilter = colorFilter
            inactivePaint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }

        override fun getIntrinsicWidth(): Int {
            return (20 * density).toInt()
        }

        override fun getIntrinsicHeight(): Int {
            return (20 * density).toInt()
        }
    }
}
