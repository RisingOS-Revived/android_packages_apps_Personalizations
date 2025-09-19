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

package com.rising.settings.fragments.lockscreen;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;

public class UdfpsAnimation extends SettingsPreferenceFragment {

    private RecyclerView mRecyclerView;
    private String mPkg = "com.crdroid.udfps.animations";
    private AnimationDrawable animation;

    private Resources udfpsRes;

    private String[] mAnims;
    private String[] mAnimPreviews;
    private String[] mTitles;

    private UdfpsAnimAdapter mUdfpsAnimAdapter;
    
    // Cache for drawable resources to prevent repeated loading
    private final Map<String, Drawable> mDrawableCache = new ConcurrentHashMap<>();
    
    // Handler for animation cleanup
    private Handler mAnimationHandler;
    
    // Track active animations to prevent memory leaks
    private AnimationDrawable mCurrentAnimation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            getActivity().setTitle(R.string.udfps_recog_animation_effect_title);
        }
        
        // Initialize handler for animation management
        mAnimationHandler = new Handler(Looper.getMainLooper());

        loadResources();
    }

    private void loadResources() {
        if (getActivity() == null) {
            return;
        }
        
        try {
            PackageManager pm = getActivity().getPackageManager();
            udfpsRes = pm.getResourcesForApplication(mPkg);
            
            if (udfpsRes != null) {
                mAnims = udfpsRes.getStringArray(udfpsRes.getIdentifier("udfps_animation_styles",
                        "array", mPkg));
                mAnimPreviews = udfpsRes.getStringArray(udfpsRes.getIdentifier("udfps_animation_previews",
                        "array", mPkg));
                mTitles = udfpsRes.getStringArray(udfpsRes.getIdentifier("udfps_animation_titles",
                        "array", mPkg));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.item_view, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 3);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mUdfpsAnimAdapter = new UdfpsAnimAdapter(getActivity());
        mRecyclerView.setAdapter(mUdfpsAnimAdapter);

        return view;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.UDFPS_ANIM_STYLE, 0, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }

    public class UdfpsAnimAdapter extends RecyclerView.Adapter<UdfpsAnimAdapter.UdfpsAnimViewHolder> {
        Context context;
        String mSelectedAnim;
        String mAppliedAnim;

        public UdfpsAnimAdapter(Context context) {
            this.context = context;
        }

        @Override
        public UdfpsAnimViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option, parent, false);
            UdfpsAnimViewHolder vh = new UdfpsAnimViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(UdfpsAnimViewHolder holder, final int position) {
            if (position >= mAnims.length || position >= mAnimPreviews.length || position >= mTitles.length) {
                return;
            }
            
            String animName = mAnims[position];

            // Use cached drawable if available
            Drawable previewDrawable = getCachedDrawable(holder.image.getContext(), mAnimPreviews[position]);
            if (previewDrawable != null) {
                Glide.with(holder.image.getContext())
                        .load("")
                        .placeholder(previewDrawable)
                        .into(holder.image);
            }

            holder.name.setText(mTitles[position]);

            if (position == Settings.System.getInt(context.getContentResolver(),
                Settings.System.UDFPS_ANIM_STYLE, 0)) {
                mAppliedAnim = animName;
                if (mSelectedAnim == null) {
                    mSelectedAnim = animName;
                }
            }

            holder.itemView.setActivated(animName.equals(mSelectedAnim));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Stop current animation before starting new one
                    stopCurrentAnimation();
                    
                    updateActivatedStatus(mSelectedAnim, false);
                    updateActivatedStatus(animName, true);
                    mSelectedAnim = animName;
                    
                    Drawable animDrawable = getCachedDrawable(v.getContext(), mAnims[position]);
                    if (animDrawable != null) {
                        holder.image.setBackgroundDrawable(animDrawable);
                        mCurrentAnimation = (AnimationDrawable) holder.image.getBackground();
                        if (mCurrentAnimation != null) {
                            mCurrentAnimation.setOneShot(true);
                            mCurrentAnimation.start();
                            
                            // Auto-stop animation after reasonable time to prevent memory leaks
                            mAnimationHandler.postDelayed(() -> stopCurrentAnimation(), 5000);
                        }
                    }
                    
                    if (getActivity() != null) {
                        Settings.System.putInt(getActivity().getContentResolver(),
                                Settings.System.UDFPS_ANIM_STYLE, position);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mAnims.length;
        }

        public class UdfpsAnimViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image;
            public UdfpsAnimViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.option_label);
                image = (ImageView) itemView.findViewById(R.id.option_thumbnail);
            }
        }

        private void updateActivatedStatus(String anim, boolean isActivated) {
            int index = Arrays.asList(mAnims).indexOf(anim);
            if (index < 0) {
                return;
            }
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(index);
            if (holder != null && holder.itemView != null) {
                holder.itemView.setActivated(isActivated);
            }
        }
    }

    // Optimized drawable loading with caching
    private Drawable getCachedDrawable(Context context, String drawableName) {
        if (drawableName == null || context == null) {
            return null;
        }
        
        // Check cache first
        Drawable cached = mDrawableCache.get(drawableName);
        if (cached != null) {
            return cached;
        }
        
        // Load and cache drawable
        Drawable drawable = getDrawable(context, drawableName);
        if (drawable != null) {
            mDrawableCache.put(drawableName, drawable);
        }
        return drawable;
    }
    
    public Drawable getDrawable(Context context, String drawableName) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pm.getResourcesForApplication(mPkg);
            return res.getDrawable(res.getIdentifier(drawableName, "drawable", mPkg));
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void stopCurrentAnimation() {
        if (mCurrentAnimation != null && mCurrentAnimation.isRunning()) {
            mCurrentAnimation.stop();
        }
        mCurrentAnimation = null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup animations and caches
        stopCurrentAnimation();
        if (mAnimationHandler != null) {
            mAnimationHandler.removeCallbacksAndMessages(null);
        }
        mDrawableCache.clear();
        udfpsRes = null;
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        // Additional cleanup
        stopCurrentAnimation();
        mDrawableCache.clear();
    }
}
