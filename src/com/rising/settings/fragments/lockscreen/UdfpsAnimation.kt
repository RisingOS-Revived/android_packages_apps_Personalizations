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

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
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
import com.android.settings.SettingsActivity
import com.android.settings.SettingsPreferenceFragment

import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

class UdfpsAnimation : SettingsPreferenceFragment() {

    private var mRecyclerView: RecyclerView? = null
    private val mPkg = "com.crdroid.udfps.animations"
    private var animation: AnimationDrawable? = null

    private var udfpsRes: Resources? = null

    private var mAnims: Array<String>? = null
    private var mAnimPreviews: Array<String>? = null
    private var mTitles: Array<String>? = null

    private var mUdfpsAnimAdapter: UdfpsAnimAdapter? = null
    
    // Cache for drawable resources to prevent repeated loading
    private val mDrawableCache: MutableMap<String, Drawable> = ConcurrentHashMap()
    
    // Handler for animation cleanup
    private var mAnimationHandler: Handler? = null
    
    // Track active animations to prevent memory leaks
    private var mCurrentAnimation: AnimationDrawable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.setTitle(R.string.udfps_recog_animation_effect_title)
        
        // Initialize handler for animation management
        mAnimationHandler = Handler(Looper.getMainLooper())

        loadResources()
    }

    private fun loadResources() {
        val activity = activity ?: return
        
        try {
            val pm = activity.packageManager
            udfpsRes = pm.getResourcesForApplication(mPkg)
            
            udfpsRes?.let { res ->
                mAnims = res.getStringArray(res.getIdentifier("udfps_animation_styles", "array", mPkg))
                mAnimPreviews = res.getStringArray(res.getIdentifier("udfps_animation_previews", "array", mPkg))
                mTitles = res.getStringArray(res.getIdentifier("udfps_animation_titles", "array", mPkg))
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        @NonNull inflater: LayoutInflater, 
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.item_view, container, false)

        mRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)?.apply {
            val gridLayoutManager = GridLayoutManager(activity, 3)
            layoutManager = gridLayoutManager
            mUdfpsAnimAdapter = UdfpsAnimAdapter(activity!!)
            adapter = mUdfpsAnimAdapter
        }

        return view
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.VIEW_UNKNOWN
    }

    inner class UdfpsAnimAdapter(private val context: Context) : RecyclerView.Adapter<UdfpsAnimAdapter.UdfpsAnimViewHolder>() {
        private var mSelectedAnim: String? = null
        private var mAppliedAnim: String? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UdfpsAnimViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_option, parent, false)
            return UdfpsAnimViewHolder(v)
        }

        override fun onBindViewHolder(holder: UdfpsAnimViewHolder, position: Int) {
            val anims = mAnims ?: return
            val animPreviews = mAnimPreviews ?: return
            val titles = mTitles ?: return
            
            if (position >= anims.size || position >= animPreviews.size || position >= titles.size) {
                return
            }
            
            val animName = anims[position]

            // Use cached drawable if available
            val previewDrawable = getCachedDrawable(holder.image.context, animPreviews[position])
            if (previewDrawable != null) {
                Glide.with(holder.image.context)
                    .load("")
                    .placeholder(previewDrawable)
                    .into(holder.image)
            }

            holder.name.text = titles[position]

            if (position == Settings.System.getInt(context.contentResolver,
                Settings.System.UDFPS_ANIM_STYLE, 0)) {
                mAppliedAnim = animName
                if (mSelectedAnim == null) {
                    mSelectedAnim = animName
                }
            }

            holder.itemView.isActivated = animName == mSelectedAnim
            holder.itemView.setOnClickListener { v ->
                // Stop current animation before starting new one
                stopCurrentAnimation()
                
                updateActivatedStatus(mSelectedAnim, false)
                updateActivatedStatus(animName, true)
                mSelectedAnim = animName
                
                val animDrawable = getCachedDrawable(v.context, anims[position])
                if (animDrawable != null) {
                    holder.image.background = animDrawable
                    mCurrentAnimation = holder.image.background as? AnimationDrawable
                    mCurrentAnimation?.let { animation ->
                        animation.setOneShot(true)
                        animation.start()
                        
                        // Auto-stop animation after reasonable time to prevent memory leaks
                        mAnimationHandler?.postDelayed({ stopCurrentAnimation() }, 5000)
                    }
                }
                
                activity?.let { activity ->
                    Settings.System.putInt(activity.contentResolver,
                        Settings.System.UDFPS_ANIM_STYLE, position)
                }
            }
        }

        override fun getItemCount(): Int {
            return mAnims?.size ?: 0
        }

        inner class UdfpsAnimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.option_label)
            val image: ImageView = itemView.findViewById(R.id.option_thumbnail)
        }

        private fun updateActivatedStatus(anim: String?, isActivated: Boolean) {
            val anims = mAnims ?: return
            val index = anims.indexOf(anim)
            if (index < 0) {
                return
            }
            val holder = mRecyclerView?.findViewHolderForAdapterPosition(index)
            holder?.itemView?.isActivated = isActivated
        }
    }

    // Optimized drawable loading with caching
    private fun getCachedDrawable(context: Context?, drawableName: String?): Drawable? {
        if (drawableName == null || context == null) {
            return null
        }
        
        // Check cache first
        val cached = mDrawableCache[drawableName]
        if (cached != null) {
            return cached
        }
        
        // Load and cache drawable
        val drawable = getDrawable(context, drawableName)
        if (drawable != null) {
            mDrawableCache[drawableName] = drawable
        }
        return drawable
    }
    
    private fun getDrawable(context: Context, drawableName: String): Drawable? {
        return try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(mPkg)
            res.getDrawable(res.getIdentifier(drawableName, "drawable", mPkg))
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }
    
    private fun stopCurrentAnimation() {
        mCurrentAnimation?.let { animation ->
            if (animation.isRunning) {
                animation.stop()
            }
        }
        mCurrentAnimation = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup animations and caches
        stopCurrentAnimation()
        mAnimationHandler?.removeCallbacksAndMessages(null)
        mDrawableCache.clear()
        udfpsRes = null
    }
    
    override fun onDetach() {
        super.onDetach()
        // Additional cleanup
        stopCurrentAnimation()
        mDrawableCache.clear()
    }

    companion object {
        @JvmStatic
        fun reset(mContext: Context) {
            val resolver = mContext.contentResolver
            Settings.System.putIntForUser(resolver,
                Settings.System.UDFPS_ANIM_STYLE, 0, UserHandle.USER_CURRENT)
        }
    }
}
