/*
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
import android.provider.Settings
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.android.settings.utils.SystemRestartUtils
import kotlinx.coroutines.launch

data class NowPlayingConfig(
    val enabled: Boolean = false,
    val iconStyle: Int = 0,
    val iconSize: Int = 24,
    val lyricsMode: Boolean = false,
    val compactStyle: Boolean = false,
    val tapToExpand: Boolean = true,
    val colorMode: Int = 0,
    val verticalPosition: Int = 88,
    val trackTextSize: Int = 14,
    val artistTextSize: Int = 12,
    val showOnLockscreen: Boolean = true,
    val showOnAod: Boolean = true
) {
    companion object {
        fun load(context: Context): NowPlayingConfig {
            val resolver = context.contentResolver
            return NowPlayingConfig(
                enabled = Settings.System.getInt(resolver, "nowplaying_enabled", 0) == 1,
                iconStyle = Settings.System.getInt(resolver, "nowplaying_icon_style", 0),
                iconSize = Settings.System.getInt(resolver, "nowplaying_icon_size", 24),
                lyricsMode = Settings.System.getInt(resolver, "nowplaying_lyrics_mode", 0) == 1,
                compactStyle = Settings.System.getInt(resolver, "nowplaying_use_compact_style", 0) == 1,
                tapToExpand = Settings.System.getInt(resolver, "nowplaying_tap_to_expand", 1) == 1,
                colorMode = Settings.System.getInt(resolver, "nowplaying_color_mode", 0),
                verticalPosition = Settings.System.getInt(resolver, "nowplaying_vertical_position", 88),
                trackTextSize = Settings.System.getInt(resolver, "nowplaying_track_text_size", 14),
                artistTextSize = Settings.System.getInt(resolver, "nowplaying_artist_text_size", 12),
                showOnLockscreen = Settings.System.getInt(resolver, "nowplaying_show_on_lockscreen", 1) == 1,
                showOnAod = Settings.System.getInt(resolver, "nowplaying_show_on_aod", 1) == 1
            )
        }
    }
    
    fun save(context: Context) {
        val resolver = context.contentResolver
        Settings.System.putInt(resolver, "nowplaying_enabled", if (enabled) 1 else 0)
        Settings.System.putInt(resolver, "nowplaying_icon_style", iconStyle)
        Settings.System.putInt(resolver, "nowplaying_icon_size", iconSize)
        Settings.System.putInt(resolver, "nowplaying_lyrics_mode", if (lyricsMode) 1 else 0)
        Settings.System.putInt(resolver, "nowplaying_use_compact_style", if (compactStyle) 1 else 0)
        Settings.System.putInt(resolver, "nowplaying_tap_to_expand", if (tapToExpand) 1 else 0)
        Settings.System.putInt(resolver, "nowplaying_color_mode", colorMode)
        Settings.System.putInt(resolver, "nowplaying_vertical_position", verticalPosition)
        Settings.System.putInt(resolver, "nowplaying_track_text_size", trackTextSize)
        Settings.System.putInt(resolver, "nowplaying_artist_text_size", artistTextSize)
        Settings.System.putInt(resolver, "nowplaying_show_on_lockscreen", if (showOnLockscreen) 1 else 0)
        Settings.System.putInt(resolver, "nowplaying_show_on_aod", if (showOnAod) 1 else 0)
    }
}

@Composable
fun NowPlayingConfigContent(
    config: NowPlayingConfig,
    onUpdate: (NowPlayingConfig) -> Unit,
    isDarkTheme: Boolean,
    currentClockStyle: Int
) {
    val iconStyleOptions = listOf("Disabled", "App icon", "Music note icon")
    val colorModeOptions = listOf("White", "Accent color", "Album art")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Customize lockscreen music recognition",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) 
                Color.White.copy(alpha = 0.7f) 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        ConfigCard(
            title = "Enable Now Playing",
            subtitle = if (config.enabled) "Active" else "Disabled",
            icon = Icons.Default.ViewCarousel,
            enabled = config.enabled,
            isDarkTheme = isDarkTheme
        ) {
            Switch(
                checked = config.enabled,
                onCheckedChange = { onUpdate(config.copy(enabled = it)) },
                enabled = true,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                    disabledCheckedThumbColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    disabledCheckedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
        }
        
        if (config.enabled) {
            OptionCard(
                title = "Icon Style",
                value = iconStyleOptions.getOrElse(config.iconStyle) { iconStyleOptions.first() },
                options = iconStyleOptions,
                selectedIndex = config.iconStyle.coerceIn(iconStyleOptions.indices),
                onSelected = { onUpdate(config.copy(iconStyle = it)) },
                isDarkTheme = isDarkTheme
            )

            SliderCard(
                title = "Icon Size",
                value = config.iconSize,
                valueRange = 5f..40f,
                unit = "dp",
                onValueChange = { onUpdate(config.copy(iconSize = it)) },
                isDarkTheme = isDarkTheme
            )

            NowPlayingSwitchCard(
                title = "Lyrics Mode",
                subtitle = if (config.lyricsMode) "Enabled" else "Disabled",
                checked = config.lyricsMode,
                onCheckedChange = { onUpdate(config.copy(lyricsMode = it)) },
                isDarkTheme = isDarkTheme
            )

            NowPlayingSwitchCard(
                title = "Compact Style",
                subtitle = if (config.compactStyle) "Enabled" else "Disabled",
                checked = config.compactStyle,
                onCheckedChange = { onUpdate(config.copy(compactStyle = it)) },
                isDarkTheme = isDarkTheme
            )

            NowPlayingSwitchCard(
                title = "Tap To Expand",
                subtitle = if (config.tapToExpand) "Enabled" else "Disabled",
                checked = config.tapToExpand,
                onCheckedChange = { onUpdate(config.copy(tapToExpand = it)) },
                isDarkTheme = isDarkTheme
            )

            OptionCard(
                title = "Color Mode",
                value = colorModeOptions.getOrElse(config.colorMode) { colorModeOptions.first() },
                options = colorModeOptions,
                selectedIndex = config.colorMode.coerceIn(colorModeOptions.indices),
                onSelected = { onUpdate(config.copy(colorMode = it)) },
                isDarkTheme = isDarkTheme
            )

            SliderCard(
                title = "Vertical Position",
                value = config.verticalPosition,
                valueRange = 10f..100f,
                unit = "%",
                onValueChange = { onUpdate(config.copy(verticalPosition = it)) },
                isDarkTheme = isDarkTheme
            )

            SliderCard(
                title = "Track Text Size",
                value = config.trackTextSize,
                valueRange = 8f..24f,
                unit = "sp",
                onValueChange = { onUpdate(config.copy(trackTextSize = it)) },
                isDarkTheme = isDarkTheme
            )

            if (!config.compactStyle) {
                SliderCard(
                    title = "Artist Text Size",
                    value = config.artistTextSize,
                    valueRange = 5f..20f,
                    unit = "sp",
                    onValueChange = { onUpdate(config.copy(artistTextSize = it)) },
                    isDarkTheme = isDarkTheme
                )
            }

            Text(
                text = "Display",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
            )

            NowPlayingSwitchCard(
                title = "Show On Lockscreen",
                subtitle = if (config.showOnLockscreen) "Visible" else "Hidden",
                checked = config.showOnLockscreen,
                onCheckedChange = { onUpdate(config.copy(showOnLockscreen = it)) },
                isDarkTheme = isDarkTheme
            )

            NowPlayingSwitchCard(
                title = "Show On AOD",
                subtitle = if (config.showOnAod) "Visible" else "Hidden",
                checked = config.showOnAod,
                onCheckedChange = { onUpdate(config.copy(showOnAod = it)) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun NowPlayingSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    ConfigCard(
        title = title,
        subtitle = subtitle,
        icon = Icons.Default.Settings,
        enabled = checked,
        isDarkTheme = isDarkTheme
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun OptionCard(
    title: String,
    value: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    isDarkTheme: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (isDarkTheme)
            Color.White.copy(alpha = 0.05f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme)
                            Color.White.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSelected(index)
                                    expanded = false
                                }
                                .background(
                                    if (index == selectedIndex) {
                                        if (isDarkTheme)
                                            Color.White.copy(alpha = 0.12f)
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (index == selectedIndex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class WeatherConfig(
    val enabled: Boolean = false,
    val showLocation: Boolean = false,
    val showText: Boolean = true
) {
    companion object {
        fun load(context: Context): WeatherConfig {
            return WeatherConfig(
                enabled = Settings.System.getInt(
                    context.contentResolver,
                    "lockscreen_weather_enabled",
                    0
                ) == 1,
                showLocation = Settings.System.getInt(
                    context.contentResolver,
                    "lockscreen_weather_location",
                    0
                ) == 1,
                showText = Settings.System.getInt(
                    context.contentResolver,
                    "lockscreen_weather_text",
                    1
                ) == 1
            )
        }
    }
    
    fun save(context: Context) {
        Settings.System.putInt(
            context.contentResolver,
            "lockscreen_weather_enabled",
            if (enabled) 1 else 0
        )
        Settings.System.putInt(
            context.contentResolver,
            "lockscreen_weather_location",
            if (showLocation) 1 else 0
        )
        Settings.System.putInt(
            context.contentResolver,
            "lockscreen_weather_text",
            if (showText) 1 else 0
        )
    }
}

@Composable
fun WeatherConfigContent(
    config: WeatherConfig,
    onUpdate: (WeatherConfig) -> Unit,
    weatherTextView: DummyWeatherTextView? = null,
    isDarkTheme: Boolean,
    currentClockStyle: Int
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Weather",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Weather info on lockscreen",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) 
                Color.White.copy(alpha = 0.7f) 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        

        ConfigCard(
            title = "Enable Weather",
            subtitle = if (config.enabled) "Active" else "Disabled",
            icon = Icons.Default.Cloud,
            enabled = config.enabled,
            isDarkTheme = isDarkTheme
        ) {
            Switch(
                checked = config.enabled,
                onCheckedChange = { 
                    val newConfig = config.copy(enabled = it)
                    onUpdate(newConfig)
                    weatherTextView?.let { view ->
                        updateWeatherPreview(view, newConfig)
                    }
                },
                enabled = true,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary,
                    disabledCheckedThumbColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    disabledCheckedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
        }
        
        if (config.enabled) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = if (isDarkTheme)
                    Color.White.copy(alpha = 0.05f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isDarkTheme -> Color.White.copy(alpha = 0.1f)
                                config.showLocation -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isDarkTheme)
                                        Color.White
                                    else if (config.showLocation)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Location",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (config.showLocation) "Visible" else "Hidden",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme)
                                    Color.White.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.showLocation,
                            onCheckedChange = { 
                                val newConfig = config.copy(showLocation = it)
                                onUpdate(newConfig)
                                weatherTextView?.let { view ->
                                    updateWeatherPreview(view, newConfig)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    
                    Divider(
                        color = if (isDarkTheme) 
                            Color.White.copy(alpha = 0.1f) 
                        else 
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isDarkTheme -> Color.White.copy(alpha = 0.1f)
                                config.showText -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextFields,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isDarkTheme)
                                        Color.White
                                    else if (config.showText)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Weather Text",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (config.showText) "Visible" else "Hidden",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme)
                                    Color.White.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.showText,
                            onCheckedChange = { 
                                val newConfig = config.copy(showText = it)
                                onUpdate(newConfig)
                                weatherTextView?.let { view ->
                                    updateWeatherPreview(view, newConfig)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    
                    Divider(
                        color = if (isDarkTheme) 
                            Color.White.copy(alpha = 0.1f) 
                        else 
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 48.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                        setClassName("org.omnirom.omnijaws", "org.omnirom.omnijaws.SettingsActivity")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Weather provider not installed",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
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
                                    imageVector = Icons.Default.Settings,
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
                                text = "Weather Settings",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Configure weather provider",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme)
                                    Color.White.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Settings",
                            tint = if (isDarkTheme)
                                Color.White.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

private fun updateWeatherPreview(weatherTextView: DummyWeatherTextView, config: WeatherConfig) {
    weatherTextView.setWeatherEnabled(config.enabled)
    weatherTextView.setShowLocation(config.showLocation)
    weatherTextView.setShowText(config.showText)
}

object WidgetSettingsManager {
    private const val LOCKSCREEN_WIDGETS_KEY = "lockscreen_widgets"
    private const val LOCKSCREEN_WIDGETS_EXTRAS_KEY = "lockscreen_widgets_extras"
    
    fun loadWidgets(context: Context): List<WidgetItem> {
        val widgets = mutableListOf<WidgetItem>()
        val resolver = context.contentResolver
        val mainWidgetsString = Settings.System.getString(resolver, LOCKSCREEN_WIDGETS_KEY)
        
        if (!mainWidgetsString.isNullOrEmpty() && mainWidgetsString != "none") {
            mainWidgetsString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != "none" }
                .forEach { widget ->
                    widgets.add(WidgetItem(name = widget, span = 2))
                }
        }
        
        val extraWidgetsString = Settings.System.getString(resolver, LOCKSCREEN_WIDGETS_EXTRAS_KEY)
        
        if (!extraWidgetsString.isNullOrEmpty() && extraWidgetsString != "none") {
            extraWidgetsString.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != "none" }
                .forEach { widget ->
                    widgets.add(WidgetItem(name = widget, span = 1))
                }
        }
        return widgets
    }
    
    fun saveWidgets(context: Context, widgets: List<WidgetItem>) {
        val resolver = context.contentResolver
        val bigWidgets = widgets.filter { it.span == 2 }.map { it.name }
        val smallWidgets = widgets.filter { it.span == 1 }.map { it.name }
        val mainWidgetsString = bigWidgets.joinToString(",").ifEmpty { "" }
        val extraWidgetsString = smallWidgets.joinToString(",").ifEmpty { "" }
        
        Settings.System.putString(resolver, LOCKSCREEN_WIDGETS_KEY, mainWidgetsString)
        Settings.System.putString(resolver, LOCKSCREEN_WIDGETS_EXTRAS_KEY, extraWidgetsString)
        
        val enabled = if (widgets.isEmpty()) 0 else 1
        Settings.System.putInt(resolver, "lockscreen_widgets_enabled", enabled)
    }
}

@Composable
fun WidgetConfigContent(
    widgetItems: List<WidgetItem>,
    onUpdate: (List<WidgetItem>) -> Unit,
    currentClockStyle: Int,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val available = WidgetsList().filter { it !in widgetItems.map { widget -> widget.name } }

    var pendingWidgets by remember(widgetItems) { mutableStateOf(widgetItems) }
    var isApplying by remember { mutableStateOf(false) }
    
    LaunchedEffect(widgetItems) {
        pendingWidgets = widgetItems
    }
    
    val hasChanges = pendingWidgets != widgetItems
    
    val bigWidgets = pendingWidgets.filter { it.span == 2 }
    val smallWidgets = pendingWidgets.filter { it.span == 1 }
    
    val canAddBig = bigWidgets.size < 2
    val canAddSmall = smallWidgets.size < 4
    
    suspend fun applyWidgets(newWidgets: List<WidgetItem>) {
        try {
            WidgetSettingsManager.saveWidgets(context, newWidgets)
            onUpdate(newWidgets)
            pendingWidgets = newWidgets
            
            kotlinx.coroutines.delay(500)
            
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    SystemRestartUtils.restartSystemUI(context)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(
                            context,
                            "Widgets applied successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }, 500)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Settings saved. Please restart SystemUI manually if changes don't appear.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }, 100)
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Error applying widgets: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Widgets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Customize lockscreen widgets",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) 
                Color.White.copy(alpha = 0.7f) 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme)
                Color(0xFF1E3A8A).copy(alpha = 0.3f)
            else
                Color(0xFFDCEBFE)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isDarkTheme)
                        Color(0xFF93C5FD)
                    else
                        Color(0xFF1E40AF)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Top row: Big widgets (2 max) • Bottom row: Small widgets (4 max)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme)
                        Color(0xFF93C5FD)
                    else
                        Color(0xFF1E40AF)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (pendingWidgets.isNotEmpty()) {
            if (bigWidgets.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    shape = RoundedCornerShape(28.dp),
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.05f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewModule,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Big Widgets",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Row 1: ${bigWidgets.size}/2",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme) 
                                    Color.White.copy(alpha = 0.6f) 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        bigWidgets.forEach { widget ->
                            WidgetItemCard(
                                widget = widget,
                                onRemove = {
                                    pendingWidgets = pendingWidgets.filter { it != widget }
                                },
                                isDarkTheme = isDarkTheme,
                                enabled = true
                            )
                        }
                    }
                }
            }
            
            if (smallWidgets.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                    shape = RoundedCornerShape(28.dp),
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.05f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Small Widgets",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Row 2: ${smallWidgets.size}/4",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme) 
                                    Color.White.copy(alpha = 0.6f) 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        smallWidgets.forEach { widget ->
                            WidgetItemCard(
                                widget = widget,
                                onRemove = {
                                    pendingWidgets = pendingWidgets.filter { it != widget }
                                },
                                isDarkTheme = isDarkTheme,
                                enabled = true
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = if (isDarkTheme)
                    Color.White.copy(alpha = 0.03f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = if (isDarkTheme)
                                Color.White.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No widgets added",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDarkTheme)
                                Color.White.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        var showWidgetPicker by remember { mutableStateOf(false) }
        
        val buttonEnabled = available.isNotEmpty() && (canAddBig || canAddSmall)
        val buttonText = when {
            available.isEmpty() -> "All Widgets Added"
            !canAddBig && !canAddSmall -> "Both Rows Full"
            !canAddBig -> "Row 1 Full (Row 2 Available)"
            !canAddSmall -> "Row 2 Full (Row 1 Available)"
            else -> "Add Widget"
        }
        
        Button(
            onClick = { showWidgetPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme)
                    Color.White.copy(alpha = if (buttonEnabled) 0.9f else 0.1f)
                else
                    if (buttonEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = if (isDarkTheme)
                    Color.White.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            enabled = buttonEnabled
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (buttonEnabled) {
                    if (isDarkTheme) Color.Black else Color.White
                } else {
                    if (isDarkTheme) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (buttonEnabled) {
                    if (isDarkTheme) Color.Black else Color.White
                } else {
                    if (isDarkTheme) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        if (hasChanges) {
            Button(
                onClick = { 
                    if (!isApplying) {
                        isApplying = true
                        scope.launch {
                            applyWidgets(pendingWidgets)
                            kotlinx.coroutines.delay(1000)
                            isApplying = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                enabled = !isApplying
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isApplying) "Applying..." else "Apply Widget Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            OutlinedButton(
                onClick = { 
                    pendingWidgets = widgetItems
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                ),
                enabled = !isApplying
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancel Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        WidgetPickerBottomSheet(
            visible = showWidgetPicker,
            current = pendingWidgets,
            canAddBig = canAddBig,
            canAddSmall = canAddSmall,
            onDismiss = { showWidgetPicker = false },
            onSelect = { selectedWidget ->
                val canAdd = if (selectedWidget.span == 2) canAddBig else canAddSmall
                if (!canAdd) {
                    Toast.makeText(
                        context,
                        if (selectedWidget.span == 2) "Row 1 is full (max 2 big widgets)" else "Row 2 is full (max 4 small widgets)",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val newWidgets = pendingWidgets + selectedWidget
                    pendingWidgets = newWidgets
                    showWidgetPicker = false
                }
            }
        )
    }
}

@Composable
private fun WidgetItemCard(
    widget: WidgetItem,
    onRemove: () -> Unit,
    isDarkTheme: Boolean,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme)
            Color.White.copy(alpha = 0.08f)
        else
            MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDarkTheme)
                    Color.White.copy(alpha = 0.15f)
                else
                    MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = WidgetIcon(widget.name),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = WidgetLabel(widget.name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (widget.span == 2) "Big Widget" else "Small Widget",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRemove,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = if (enabled) {
                        if (isDarkTheme)
                            Color.Red.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.error
                    } else {
                        if (isDarkTheme)
                            Color.White.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }
        }
    }
}

@Composable
fun ConfigCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = false,
    isDarkTheme: Boolean,
    trailing: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = when {
            enabled && isDarkTheme -> Color.White.copy(alpha = 0.08f)
            enabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            isDarkTheme -> Color.White.copy(alpha = 0.05f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isDarkTheme -> Color.White.copy(alpha = 0.1f)
                    enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isDarkTheme)
                            Color.White
                        else if (enabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkTheme)
                        Color.White.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing()
        }
    }
}

@Composable
fun SliderCard(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Int) -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$value$unit",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = if (isDarkTheme)
                        Color.White.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}