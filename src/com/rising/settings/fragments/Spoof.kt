/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rising.settings.fragments

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.core.SubSettingLauncher
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.spa.framework.theme.SettingsSpace

/**
 * Spoofing hub. Compose host for the same entries the old preference screen had: the two
 * Play Integrity switches (TrickyStore, PIF) with their own sub-pages, device identity pages,
 * Tensor targets (non-Tensor devices only), the Photos / Snapchat overrides (installed apps
 * only) and a reset action. `res/xml/rising_settings_spoof.xml` is kept only as the search
 * index source.
 */
@SearchIndexable
class Spoof : SettingsPreferenceFragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingKill: Runnable? = null
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().title = getString(R.string.spoofing_title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ExpressiveTheme {
                SpoofContent(
                    refreshKey = resumeTick,
                    onOpen = ::openPage,
                    onAppSpoofChanged = ::scheduleKill,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-read the master switches: they can be flipped from the sub-pages.
        resumeTick++
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingKill?.let { handler.removeCallbacks(it) }
    }

    override fun getMetricsCategory(): Int = MetricsEvent.VIEW_UNKNOWN

    private fun openPage(destination: Class<*>, titleRes: Int) {
        SubSettingLauncher(requireContext())
            .setDestination(destination.name)
            .setTitleRes(titleRes)
            .setSourceMetricsCategory(getMetricsCategory())
            .launch()
    }

    private fun scheduleKill(pkg: String) {
        pendingKill?.let { handler.removeCallbacks(it) }
        Toast.makeText(requireContext(), R.string.spoofing_applying_changes, Toast.LENGTH_SHORT)
            .show()
        val kill = Runnable { killIfRunning(pkg) }
        pendingKill = kill
        handler.postDelayed(kill, KILL_DELAY_MS)
    }

    private fun killIfRunning(pkg: String) {
        try {
            val am = requireContext()
                .getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
            for (proc in am.runningAppProcesses.orEmpty()) {
                val pkgList = proc.pkgList ?: continue
                for (p in pkgList) {
                    if (pkg == p) {
                        am.forceStopPackage(pkg)
                        Log.d(TAG, "Killed: $pkg")
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Kill failed: $pkg", e)
        }
    }

    companion object {
        private const val TAG = "Spoof"
        private const val KILL_DELAY_MS = 500L

        private const val TRICKYSTORE_ENABLED_KEY = "spoof_trickystore_enabled"
        private const val PIF_ENABLED_KEY = "spoof_pif_enabled"
        private const val PI_PHOTOS_SPOOF = "pi_photos_spoof"
        private const val PI_SNAPCHAT_SPOOF = "pi_snapchat_spoof"
        private const val KEY_TENSOR_TARGETS = "tensor_targets_settings"

        private const val PHOTOS_PACKAGE = "com.google.android.apps.photos"
        private const val SNAPCHAT_PACKAGE = "com.snapchat.android"

        private const val WIKI_URL = "https://github.com/RisingOS-Revived/risingOS_wiki"

        private fun isInstalled(context: Context, pkg: String): Boolean = try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        private fun isTensorDevice(): Boolean {
            val model = SystemProperties.get("ro.product.model")
            return model.matches(Regex("Pixel (6|7|8|9|10)[a-zA-Z ]*"))
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = object : BaseSearchIndexProvider(R.xml.rising_settings_spoof) {
            override fun getNonIndexableKeys(context: Context): List<String> {
                val keys = super.getNonIndexableKeys(context).toMutableList()
                if (isTensorDevice()) keys.add(KEY_TENSOR_TARGETS)
                if (!isInstalled(context, PHOTOS_PACKAGE)) keys.add(PI_PHOTOS_SPOOF)
                if (!isInstalled(context, SNAPCHAT_PACKAGE)) keys.add(PI_SNAPCHAT_SPOOF)
                return keys
            }
        }

        @Composable
        private fun SpoofContent(
            refreshKey: Int,
            onOpen: (Class<*>, Int) -> Unit,
            onAppSpoofChanged: (String) -> Unit,
        ) {
            val context = LocalContext.current
            val resolver = context.contentResolver

            val isTensor = remember { isTensorDevice() }
            val photosInstalled = remember { isInstalled(context, PHOTOS_PACKAGE) }
            val snapInstalled = remember { isInstalled(context, SNAPCHAT_PACKAGE) }

            var tsEnabled by remember(refreshKey) {
                mutableStateOf(Settings.System.getInt(resolver, TRICKYSTORE_ENABLED_KEY, 1) != 0)
            }
            var pifEnabled by remember(refreshKey) {
                mutableStateOf(Settings.System.getInt(resolver, PIF_ENABLED_KEY, 1) != 0)
            }
            var photosSpoof by remember(refreshKey) {
                mutableStateOf(Settings.Secure.getInt(resolver, PI_PHOTOS_SPOOF, 1) != 0)
            }
            var snapSpoof by remember(refreshKey) {
                mutableStateOf(Settings.Secure.getInt(resolver, PI_SNAPCHAT_SPOOF, 0) != 0)
            }
            var showResetDialog by remember { mutableStateOf(false) }

            if (showResetDialog) {
                ExpressiveConfirmDialog(
                    title = stringResource(R.string.spoofing_reset_defaults_dialog_title),
                    message = stringResource(R.string.spoofing_reset_defaults_dialog_message),
                    confirmText = stringResource(R.string.spoofing_reset_defaults_confirm),
                    destructive = true,
                    onConfirm = {
                        try {
                            TrickyStore.resetAllSpoofDefaults(context)
                            Toast.makeText(
                                context, R.string.spoofing_reset_defaults_done, Toast.LENGTH_SHORT,
                            ).show()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to reset spoof defaults", e)
                        }
                    },
                    onDismiss = { showResetDialog = false },
                    dismissText = stringResource(R.string.cancel),
                )
            }

            ExpressiveScreen {
                ExpressiveGroup(title = stringResource(R.string.spoofing_integrity_category)) {
                    item {
                        ExpressiveSwitchNavRow(
                            title = stringResource(R.string.tricky_store_title),
                            summary = stringResource(R.string.tricky_store_summary),
                            checked = tsEnabled,
                            onCheckedChange = {
                                tsEnabled = it
                                Settings.System.putInt(
                                    resolver, TRICKYSTORE_ENABLED_KEY, if (it) 1 else 0)
                            },
                            onClick = { onOpen(TrickyStore::class.java, R.string.tricky_store_title) },
                        )
                    }
                    item {
                        ExpressiveSwitchNavRow(
                            title = stringResource(R.string.play_integrity_fix_title),
                            summary = stringResource(R.string.play_integrity_fix_summary),
                            checked = pifEnabled,
                            onCheckedChange = {
                                pifEnabled = it
                                Settings.System.putInt(resolver, PIF_ENABLED_KEY, if (it) 1 else 0)
                            },
                            onClick = {
                                onOpen(PlayIntegrityFix::class.java, R.string.play_integrity_fix_title)
                            },
                        )
                    }
                }

                ExpressiveGroup(title = stringResource(R.string.spoofing_identity_category)) {
                    item {
                        ExpressiveNavRow(
                            title = stringResource(R.string.user_selectable_app_spoofing_title),
                            summary = stringResource(R.string.user_selectable_app_spoofing_summary),
                            onClick = {
                                onOpen(
                                    UserSelectedAppSpoofSettings::class.java,
                                    R.string.user_selectable_app_spoofing_title,
                                )
                            },
                        )
                    }
                    item {
                        ExpressiveNavRow(
                            title = stringResource(R.string.spoofing_pixel_props_title),
                            summary = stringResource(R.string.spoofing_pixel_props_summary),
                            onClick = {
                                onOpen(
                                    PixelPropsSettings::class.java,
                                    R.string.spoofing_pixel_props_title,
                                )
                            },
                        )
                    }
                }

                // Tensor targets only matter on non-Tensor devices
                if (!isTensor) {
                    ExpressiveGroup(title = stringResource(R.string.spoofing_features_category)) {
                        item {
                            ExpressiveNavRow(
                                title = stringResource(R.string.tensor_spoof_title),
                                summary = stringResource(R.string.tensor_spoof_summary),
                                onClick = {
                                    onOpen(
                                        TensorTargetsSettings::class.java,
                                        R.string.tensor_spoof_title,
                                    )
                                },
                            )
                        }
                    }
                }

                if (photosInstalled || snapInstalled) {
                    ExpressiveGroup(title = stringResource(R.string.spoofing_app_specific_category)) {
                        if (photosInstalled) {
                            item {
                                ExpressiveSwitchRow(
                                    titleText = stringResource(R.string.spoofing_photos_title),
                                    summaryText = stringResource(R.string.spoofing_photos_summary),
                                    isChecked = photosSpoof,
                                    onChange = {
                                        photosSpoof = it
                                        Settings.Secure.putInt(
                                            resolver, PI_PHOTOS_SPOOF, if (it) 1 else 0)
                                        onAppSpoofChanged(PHOTOS_PACKAGE)
                                    },
                                )
                            }
                        }
                        if (snapInstalled) {
                            item {
                                ExpressiveSwitchRow(
                                    titleText = stringResource(R.string.spoofing_snap_title),
                                    summaryText = stringResource(R.string.spoofing_snap_summary),
                                    isChecked = snapSpoof,
                                    onChange = {
                                        snapSpoof = it
                                        Settings.Secure.putInt(
                                            resolver, PI_SNAPCHAT_SPOOF, if (it) 1 else 0)
                                        onAppSpoofChanged(SNAPCHAT_PACKAGE)
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(SettingsSpace.small1))
                ExpressiveActionRow(
                    actions = listOf(
                        ExpressiveAction(
                            text = stringResource(R.string.spoofing_reset_defaults_title),
                            icon = Icons.Rounded.History,
                            onClick = { showResetDialog = true },
                        ),
                    ),
                )

                ExpressiveGroup(title = stringResource(R.string.help_section_title)) {
                    item {
                        ExpressiveNavRow(
                            title = stringResource(R.string.help_footer_title),
                            summary = stringResource(R.string.help_footer_summary),
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(WIKI_URL)))
                            },
                        )
                    }
                }
            }
        }
    }
}
