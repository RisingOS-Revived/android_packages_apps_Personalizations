/*
 * Copyright (C) 2025 AxionOS
 * Copyright (C) 2025 Rising Revived Android Project
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

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import android.widget.TextClock
import android.widget.TextView
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import com.android.settings.R
import com.rising.settings.fragments.ui.fonts.FontManager
import com.android.internal.util.android.ThemeUtils
import com.android.settings.utils.SystemRestartUtils
import kotlinx.coroutines.launch

data class ClockConfig(
    val style: Int = 0,
    val fontPackage: String = "default",
    // Color
    val useAlbumArtColor: Boolean = false,
    val colorMode: String = "default",
    val customColorArgb: Int = DEFAULT_CLOCK_COLOR,
    // Gradient
    val gradientEnabled: Boolean = false,
    val gradientColorStartArgb: Int = DEFAULT_GRADIENT_START,
    val gradientColorEndArgb: Int = DEFAULT_GRADIENT_END,
    val gradientAnchorY: Int = 50,
    val gradientRadius: Int = 100,
    // Layout
    val sizeScale: Int = 100,
    val opacity: Int = 100,
    val marginTop: Int = 15,
    val marginStart: Int = 0,
    // Animation
    val wobbleOnCharge: Boolean = true,
    val aodAnim: Boolean = true
) {
    companion object {
        const val DEFAULT_CLOCK_COLOR = -0x1 // 0xFFFFFFFF, matches ClockColorPickerDialogFragment.DEFAULT_COLOR_ARGB
        const val DEFAULT_GRADIENT_START = -0xFF1A01 // 0xFF00E5FF
        const val DEFAULT_GRADIENT_END = -0xD256 // 0xFFFF2DAA

        val COLOR_MODE_OPTIONS = listOf(
            "default" to "Default",
            "accent" to "Accent",
            "custom" to "Custom"
        )

        private val CLOCK_LAYOUTS: IntArray
            get() = com.android.settings.utils.ClockUtils.CLOCK_LAYOUTS
        private val CLOCK_NAMES: Array<String>
            get() = com.android.settings.utils.ClockUtils.getClockNames()

        fun load(context: Context): ClockConfig {
            val cr = context.contentResolver
            val style = Settings.Secure.getInt(cr, "lock_screen_custom_clock_style", 0)
            val fontPackage = Settings.Secure.getString(cr, "lock_screen_clock_font_package") ?: "default"

            val useAlbumArtColor = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_album_art_color", 0
            ) == 1
            val colorMode = Settings.Secure.getString(
                cr, "lock_screen_custom_clock_color_mode"
            ) ?: "default"
            val customColorArgb = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_custom_color", DEFAULT_CLOCK_COLOR
            )

            val gradientEnabled = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_gradient_enabled", 0
            ) == 1
            val gradientColorStartArgb = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_gradient_color_start", DEFAULT_GRADIENT_START
            )
            val gradientColorEndArgb = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_gradient_color_end", DEFAULT_GRADIENT_END
            )
            val gradientAnchorY = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_gradient_anchor_y", 50
            )
            val gradientRadius = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_gradient_radius", 100
            )

            val sizeScale = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_size_scale", 100
            )
            val opacity = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_opacity", 100
            )
            val marginTop = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_margin_top", 15
            )
            val marginStart = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_margin_start", 0
            )

            val wobbleOnCharge = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_wobble_on_charge", 1
            ) == 1
            val aodAnim = Settings.Secure.getInt(
                cr, "lock_screen_custom_clock_aod_anim", 1
            ) == 1

            return ClockConfig(
                style = style,
                fontPackage = fontPackage,
                useAlbumArtColor = useAlbumArtColor,
                colorMode = colorMode,
                customColorArgb = customColorArgb,
                gradientEnabled = gradientEnabled,
                gradientColorStartArgb = gradientColorStartArgb,
                gradientColorEndArgb = gradientColorEndArgb,
                gradientAnchorY = gradientAnchorY,
                gradientRadius = gradientRadius,
                sizeScale = sizeScale,
                opacity = opacity,
                marginTop = marginTop,
                marginStart = marginStart,
                wobbleOnCharge = wobbleOnCharge,
                aodAnim = aodAnim
            )
        }
        
        fun getClockName(style: Int): String {
            return if (style in CLOCK_NAMES.indices) CLOCK_NAMES[style] else "Unknown"
        }
        
        fun getLayoutId(style: Int): Int {
            return if (style in CLOCK_LAYOUTS.indices) CLOCK_LAYOUTS[style] else CLOCK_LAYOUTS[0]
        }
        
        fun getTotalStyles(): Int = CLOCK_LAYOUTS.size
    }
    
    fun save(context: Context) {
        val cr = context.contentResolver
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_style", style)
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_face", 0)

        Settings.Secure.putInt(
            cr, "lock_screen_custom_clock_album_art_color", if (useAlbumArtColor) 1 else 0
        )
        Settings.Secure.putString(cr, "lock_screen_custom_clock_color_mode", colorMode)
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_custom_color", customColorArgb)

        Settings.Secure.putInt(
            cr, "lock_screen_custom_clock_gradient_enabled", if (gradientEnabled) 1 else 0
        )
        Settings.Secure.putInt(
            cr, "lock_screen_custom_clock_gradient_color_start", gradientColorStartArgb
        )
        Settings.Secure.putInt(
            cr, "lock_screen_custom_clock_gradient_color_end", gradientColorEndArgb
        )
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_gradient_anchor_y", gradientAnchorY)
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_gradient_radius", gradientRadius)

        Settings.Secure.putInt(cr, "lock_screen_custom_clock_size_scale", sizeScale)
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_opacity", opacity)
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_margin_top", marginTop)
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_margin_start", marginStart)

        Settings.Secure.putInt(
            cr, "lock_screen_custom_clock_wobble_on_charge", if (wobbleOnCharge) 1 else 0
        )
        Settings.Secure.putInt(cr, "lock_screen_custom_clock_aod_anim", if (aodAnim) 1 else 0)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClockConfigContent(
    config: ClockConfig,
    currentPagerPage: Int,
    onUpdate: (ClockConfig) -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fontManager = remember { FontManager(context as android.app.Activity, true) }
    
    var selectedFontIndex by remember { mutableStateOf(-1) }
    var showFontPicker by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    
    val selectedStyle = currentPagerPage
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Clock Style",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Swipe on the preview to browse styles",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) 
                Color.White.copy(alpha = 0.7f) 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (isDarkTheme)
                Color.White.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isDarkTheme)
                                Color.White
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current Style",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDarkTheme)
                            Color.White.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ClockConfig.getClockName(selectedStyle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme)
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${selectedStyle + 1}/${ClockConfig.getTotalStyles()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Surface(
                onClick = { showFontPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = if (isDarkTheme)
                    Color.White.copy(alpha = 0.05f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme)
                            Color.White.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FontDownload,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isDarkTheme)
                                    Color.White.copy(alpha = 0.8f)
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clock Font",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedFontIndex >= 0) {
                                fontManager.getLabel(
                                    context,
                                    fontManager.allFontPackages[selectedFontIndex]
                                )
                            } else "System Default",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme)
                                Color.White.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Font",
                        tint = if (isDarkTheme)
                            Color.White.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

        ClockColorSection(
            config = config,
            onUpdate = onUpdate,
            isDarkTheme = isDarkTheme
        )

        ClockGradientSection(
            config = config,
            onUpdate = onUpdate,
            isDarkTheme = isDarkTheme
        )

        ClockLayoutSection(
            config = config,
            onUpdate = onUpdate,
            isDarkTheme = isDarkTheme
        )

        ClockAnimationSection(
            config = config,
            onUpdate = onUpdate,
            isDarkTheme = isDarkTheme
        )
        
        Button(
            onClick = {
                if (!isApplying) {
                    isApplying = true
                    scope.launch {
                        applyClockChangesAndRestart(
                            context = context,
                            config = config.copy(style = selectedStyle),
                            onSuccess = {
                                isApplying = false
                            },
                            onFailure = {
                                isApplying = false
                            }
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme)
                    Color.White
                else
                    MaterialTheme.colorScheme.primary,
                contentColor = if (isDarkTheme)
                    Color.Black
                else
                    MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !isApplying
        ) {
            if (isApplying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isApplying) "Applying..." else "Apply Clock Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    
    if (showFontPicker) {
        AlertDialog(
            onDismissRequest = { showFontPicker = false },
            title = { 
                Text(
                    "Select Font",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    fontManager.allFontPackages.forEachIndexed { index, fontPackage ->
                        Surface(
                            onClick = {
                                selectedFontIndex = index
                                showFontPicker = false
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                selectedFontIndex == index && isDarkTheme -> Color.White.copy(alpha = 0.15f)
                                selectedFontIndex == index -> MaterialTheme.colorScheme.primaryContainer
                                isDarkTheme -> Color.White.copy(alpha = 0.05f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fontManager.getLabel(context, fontPackage),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedFontIndex == index) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (isDarkTheme)
                                            Color.White
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFontPicker = false },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Close",
                        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            },
            containerColor = if (isDarkTheme) Color(0xFF121212) else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, isDarkTheme: Boolean) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ClockColorSection(
    config: ClockConfig,
    onUpdate: (ClockConfig) -> Unit,
    isDarkTheme: Boolean
) {
    var showCustomColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Clock Color", isDarkTheme)

        ConfigCard(
            title = "Album Art Color",
            subtitle = "Tint the clock using the current album art",
            icon = Icons.Default.Palette,
            enabled = config.useAlbumArtColor,
            isDarkTheme = isDarkTheme
        ) {
            Switch(
                checked = config.useAlbumArtColor,
                onCheckedChange = { onUpdate(config.copy(useAlbumArtColor = it)) },
                colors = switchColors(isDarkTheme)
            )
        }

        if (!config.useAlbumArtColor) {
            ColorModePicker(
                selected = config.colorMode,
                onSelect = { onUpdate(config.copy(colorMode = it)) },
                isDarkTheme = isDarkTheme
            )

            if (config.colorMode == "custom") {
                ColorSwatchRow(
                    title = "Custom Color",
                    argb = config.customColorArgb,
                    onClick = { showCustomColorPicker = true },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }

    if (showCustomColorPicker) {
        com.android.settings.utils.ClockColorPickerDialog(
            initialColor = String.format("%06X", config.customColorArgb and 0x00FFFFFF),
            title = "Clock Color",
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { color ->
                val argb = (color.toArgb() and 0x00FFFFFF) or 0xFF000000.toInt()
                onUpdate(config.copy(customColorArgb = argb))
                showCustomColorPicker = false
            }
        )
    }
}

@Composable
private fun ClockGradientSection(
    config: ClockConfig,
    onUpdate: (ClockConfig) -> Unit,
    isDarkTheme: Boolean
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Gradient", isDarkTheme)

        ConfigCard(
            title = "Enable Gradient",
            subtitle = if (config.gradientEnabled) "Active" else "Disabled",
            icon = Icons.Default.Gradient,
            enabled = config.gradientEnabled,
            isDarkTheme = isDarkTheme
        ) {
            Switch(
                checked = config.gradientEnabled,
                onCheckedChange = { onUpdate(config.copy(gradientEnabled = it)) },
                colors = switchColors(isDarkTheme)
            )
        }

        if (config.gradientEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ColorSwatchRow(
                        title = "Start",
                        argb = config.gradientColorStartArgb,
                        onClick = { showStartPicker = true },
                        isDarkTheme = isDarkTheme
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ColorSwatchRow(
                        title = "End",
                        argb = config.gradientColorEndArgb,
                        onClick = { showEndPicker = true },
                        isDarkTheme = isDarkTheme
                    )
                }
            }

            SliderCard(
                title = "Anchor Y",
                value = config.gradientAnchorY,
                valueRange = 0f..100f,
                unit = "%",
                onValueChange = { onUpdate(config.copy(gradientAnchorY = it)) },
                isDarkTheme = isDarkTheme
            )

            SliderCard(
                title = "Radius",
                value = config.gradientRadius,
                valueRange = 25f..200f,
                unit = "%",
                onValueChange = { onUpdate(config.copy(gradientRadius = it)) },
                isDarkTheme = isDarkTheme
            )
        }
    }

    if (showStartPicker) {
        com.android.settings.utils.ClockColorPickerDialog(
            initialColor = String.format("%06X", config.gradientColorStartArgb and 0x00FFFFFF),
            title = "Gradient Start",
            onDismiss = { showStartPicker = false },
            onColorSelected = { color ->
                val argb = (color.toArgb() and 0x00FFFFFF) or 0xFF000000.toInt()
                onUpdate(config.copy(gradientColorStartArgb = argb))
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        com.android.settings.utils.ClockColorPickerDialog(
            initialColor = String.format("%06X", config.gradientColorEndArgb and 0x00FFFFFF),
            title = "Gradient End",
            onDismiss = { showEndPicker = false },
            onColorSelected = { color ->
                val argb = (color.toArgb() and 0x00FFFFFF) or 0xFF000000.toInt()
                onUpdate(config.copy(gradientColorEndArgb = argb))
                showEndPicker = false
            }
        )
    }
}

@Composable
private fun ClockLayoutSection(
    config: ClockConfig,
    onUpdate: (ClockConfig) -> Unit,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Layout", isDarkTheme)

        SliderCard(
            title = "Clock Size",
            value = config.sizeScale,
            valueRange = 50f..150f,
            unit = "%",
            onValueChange = { onUpdate(config.copy(sizeScale = it)) },
            isDarkTheme = isDarkTheme
        )

        SliderCard(
            title = "Opacity",
            value = config.opacity,
            valueRange = 10f..100f,
            unit = "%",
            onValueChange = { onUpdate(config.copy(opacity = it)) },
            isDarkTheme = isDarkTheme
        )

        SliderCard(
            title = "Top Margin",
            value = config.marginTop,
            valueRange = 0f..100f,
            unit = "px",
            onValueChange = { onUpdate(config.copy(marginTop = it)) },
            isDarkTheme = isDarkTheme
        )

        SliderCard(
            title = "Start Margin",
            value = config.marginStart,
            valueRange = -200f..200f,
            unit = "px",
            onValueChange = { onUpdate(config.copy(marginStart = it)) },
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun ClockAnimationSection(
    config: ClockConfig,
    onUpdate: (ClockConfig) -> Unit,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Animation", isDarkTheme)

        ConfigCard(
            title = "Wobble on Charge",
            subtitle = "Clock wobbles when plugged in",
            icon = Icons.Default.BatteryChargingFull,
            enabled = config.wobbleOnCharge,
            isDarkTheme = isDarkTheme
        ) {
            Switch(
                checked = config.wobbleOnCharge,
                onCheckedChange = { onUpdate(config.copy(wobbleOnCharge = it)) },
                colors = switchColors(isDarkTheme)
            )
        }

        ConfigCard(
            title = "AOD Animation",
            subtitle = "Animate clock in Always-On Display",
            icon = Icons.Default.Nightlight,
            enabled = config.aodAnim,
            isDarkTheme = isDarkTheme
        ) {
            Switch(
                checked = config.aodAnim,
                onCheckedChange = { onUpdate(config.copy(aodAnim = it)) },
                colors = switchColors(isDarkTheme)
            )
        }
    }
}

@Composable
private fun switchColors(isDarkTheme: Boolean) = SwitchDefaults.colors(
    checkedThumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
)

@Composable
private fun ColorModePicker(
    selected: String,
    onSelect: (String) -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (isDarkTheme)
            Color.White.copy(alpha = 0.05f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Color Mode",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClockConfig.COLOR_MODE_OPTIONS.forEach { (value, label) ->
                    val isSelected = selected == value
                    Surface(
                        onClick = { onSelect(value) },
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isSelected && isDarkTheme -> Color.White.copy(alpha = 0.2f)
                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            isDarkTheme -> Color.White.copy(alpha = 0.05f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected && isDarkTheme -> Color.White
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isDarkTheme -> Color.White.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    argb: Int,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (isDarkTheme)
            Color.White.copy(alpha = 0.05f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format("#%06X", argb and 0x00FFFFFF),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun applyClockChangesAndRestart(
    context: android.content.Context,
    config: ClockConfig,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val clockPosition = config.style
    try {
        config.save(context)

        val themeUtils = ThemeUtils.getInstance(context)

        themeUtils.setOverlayEnabled(
            "android.theme.customization.smartspace",
            if (clockPosition != 0) "com.android.systemui.hide.smartspace" else "com.android.systemui",
            "com.android.systemui"
        )
        
        kotlinx.coroutines.delay(1000)
        
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                SystemRestartUtils.restartSystemUI(context)
                Handler(Looper.getMainLooper()).postDelayed({
                    android.widget.Toast.makeText(
                        context,
                        "Settings applied successfully!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    onSuccess()
                }, 500)
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Settings saved. Please restart SystemUI manually if changes don't appear.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                onFailure()
            }
        }, 100)
    } catch (e: Exception) {
        Handler(Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                context,
                "Settings saved. Please restart SystemUI manually if changes don't appear.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            onFailure()
        }
    }
}
