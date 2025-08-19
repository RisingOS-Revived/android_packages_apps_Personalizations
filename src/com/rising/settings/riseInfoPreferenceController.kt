/*
 * Copyright (C) 2023 the RisingOS Android Project
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
package com.rising.settings

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.SystemProperties
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference
import com.android.settings.utils.DeviceInfoUtil

class InOutExpoInterpolator : TimeInterpolator {
    override fun getInterpolation(t: Float): Float {
        return when {
            t == 0f -> 0f
            t == 1f -> 1f
            t < 0.5f -> (Math.pow(2.0, (20 * t - 10).toDouble()) / 2).toFloat()
            else -> ((2 - Math.pow(2.0, (-20 * t + 10).toDouble())) / 2f).toFloat()
        }
    }
}

class riseInfoPreferenceController(context: Context) : AbstractPreferenceController(context) {

    private val defaultFallback = mContext.getString(R.string.device_info_default)
    private var versionTextView1: TextView? = null
    private var versionTextView2: TextView? = null
    private var isTextView1Visible = true

    private val handler = Handler()
    private val updateTextRunnable = object : Runnable {
        override fun run() {
            animateTextChange()
            handler.postDelayed(this, 3000)
        }
    }
    private var currentMessageIndex = 0

    private val versionMessages = listOf(
        "#${getProp(PROP_RISING_CODE)}",
        "v ${getRisingVersion()}"
    )

    private fun getProp(propName: String): String {
        return SystemProperties.get(propName, defaultFallback)
    }

    private fun getProp(propName: String, customFallback: String): String {
        val propValue = SystemProperties.get(propName)
        return if (propValue.isNotEmpty()) propValue else SystemProperties.get(customFallback, "Unknown")
    }

    private fun getRisingChipset(): String {
        return getProp(PROP_RISING_CHIPSET, "ro.board.platform")
    }

    private fun getDeviceName(): String {
        val deviceName = "${Build.DEVICE}"
        return deviceName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun getRisingBuildVersion(): String {
        return getProp(PROP_RISING_BUILD_VERSION)
    }

    private fun getRisingSecurity(): String {
        return getProp(PROP_RISING_SECURITY)
    }

    private fun getRisingVersion(): String {
        return SystemProperties.get(PROP_RISING_VERSION, "2.0")
    }

    private fun getRisingBuildStatus(releaseType: String): String {
        return mContext.getString(if (releaseType == "official") R.string.build_is_official_title else R.string.build_is_community_title)
    }

    private fun getRisingMaintainer(releaseType: String): String {
        val risingMaintainer = getProp(PROP_RISING_MAINTAINER)
        return if (risingMaintainer.equals("Unknown", ignoreCase = true)) {
            mContext.getString(R.string.unknown_maintainer)
        } else {
            mContext.getString(R.string.maintainer_summary, risingMaintainer)
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val releaseType = getProp(PROP_RISING_RELEASETYPE).lowercase()
        val risingMaintainer = getRisingMaintainer(releaseType)
        val isOfficial = releaseType == "official"

        val hwInfoPreference = screen.findPreference<LayoutPreference>(KEY_HW_INFO)
        val swInfoPreference = screen.findPreference<LayoutPreference>(KEY_SW_INFO)

        swInfoPreference?.let { swPref ->
            val maintainerTextView: TextView? = swPref.findViewById(R.id.firmware_maintainer)
            maintainerTextView?.text = risingMaintainer
            maintainerTextView?.isSelected = true

            versionTextView1 = swPref.findViewById(R.id.firmware_version_1)
            versionTextView2 = swPref.findViewById(R.id.firmware_version_2)

            // Set the initial text and visibility
            versionTextView1?.text = versionMessages[currentMessageIndex]
            versionTextView1?.visibility = View.VISIBLE
            versionTextView2?.visibility = View.GONE

            swPref.findViewById<TextView>(R.id.firmware_status)?.text = getRisingBuildStatus(releaseType).lowercase()
            swPref.findViewById<ImageView>(R.id.firmware_status_icon)?.setImageResource(
                if (isOfficial) R.drawable.verified else R.drawable.unverified
            )
        }

        hwInfoPreference?.apply {
            findViewById<TextView>(R.id.device_chipset)?.text = getRisingChipset()
            findViewById<TextView>(R.id.device_storage)?.text =
                "${DeviceInfoUtil.getTotalRam()} | ${DeviceInfoUtil.getStorageTotal(mContext)}"
            findViewById<TextView>(R.id.device_battery_capacity)?.text = DeviceInfoUtil.getBatteryCapacity(mContext)
            findViewById<TextView>(R.id.device_resolution)?.text = DeviceInfoUtil.getScreenResolution(mContext)
            findViewById<TextView>(R.id.device_showcase)?.text = getDeviceName()
        }

        handler.post(updateTextRunnable)
    }

    private fun animateTextChange() {
        val outView = if (isTextView1Visible) versionTextView1 else versionTextView2
        val inView = if (isTextView1Visible) versionTextView2 else versionTextView1
        val inOutExpoInterpolator = InOutExpoInterpolator()

        outView?.let { outTv ->
            inView?.let { inTv ->
                val width = outTv.width.takeIf { it > 0 }?.toFloat() ?: 200f // fallback width if not measured yet
                currentMessageIndex = (currentMessageIndex + 1) % versionMessages.size
                inTv.text = versionMessages[currentMessageIndex]

                // Prepare the incoming view
                inTv.alpha = 0f
                inTv.translationX = width
                inTv.visibility = View.VISIBLE

                // Animate outgoing view: fade out and move left
                outTv.animate()
                .alpha(0f)
                .translationX(-width / 2)
                .setDuration(600)
                .setInterpolator(inOutExpoInterpolator)
                .start()

                // Animate incoming view: fade in and move to center from right
                inTv.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(600)
                .setInterpolator(inOutExpoInterpolator)
                .withEndAction {
                    outTv.visibility = View.GONE
                    outTv.alpha = 1f
                    outTv.translationX = 0f
                    isTextView1Visible = !isTextView1Visible
                }
                .start()
            }
        }
    }

    override fun isAvailable(): Boolean {
        return true
    }

    override fun getPreferenceKey(): String {
        return KEY_DEVICE_INFO
    }

    companion object {
        private const val FIRMWARE_NAME = "RisingUI"
        private const val KEY_KERNEL_INFO = "kernel_version_sw"
        private const val KEY_SW_INFO = "my_device_sw_header"
        private const val KEY_HW_INFO = "my_device_hw_header"
        private const val KEY_DEVICE_INFO = "my_device_info_header"
        private const val KEY_BUILD_BANNER = "banner_logo"

        private const val PROP_RISING_CODE = "ro.rising.code"
        private const val PROP_RISING_VERSION = "ro.rising.version"
        private const val PROP_RISING_RELEASETYPE = "ro.rising.releasetype"
        private const val PROP_RISING_MAINTAINER = "ro.rising.maintainer"
        private const val PROP_RISING_BUILD_VERSION = "ro.rising.build.version"
        private const val PROP_RISING_CHIPSET = "ro.rising.chipset"
        private const val PROP_RISING_SECURITY = "ro.build.version.security_patch"
    }
}
