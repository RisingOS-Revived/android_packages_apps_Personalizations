/*
 * Copyright (C) 2025 AxionOS
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

import android.app.WallpaperManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import com.android.settings.R
import kotlinx.coroutines.*
import kotlin.math.*
import java.util.*

@Composable
fun LockscreenPreview() {
    val context = LocalContext.current
    val wallpaper = WallpaperManager.getInstance(context).drawable
    var showPicker by remember { mutableStateOf(false) }
    var widgetItems by remember { mutableStateOf(load(context)) }
    
    fun updateWidgets(newItems: List<WidgetItem>) {
        widgetItems = newItems
        save(context, newItems)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        wallpaper?.toBitmap()?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Dimens.ClockTopPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PreviewClock()
            Spacer(modifier = Modifier.height(Dimens.ClockSpacer))
            WidgetGrid(
                widgetItems = widgetItems,
                onUpdate = { removedItem ->
                    val newItems = widgetItems.filter { it != removedItem }
                    updateWidgets(newItems)
                },
                onReorder = { newItems ->
                    updateWidgets(newItems)
                },
                onPickWidget = { showPicker = true }
            )
        }
        WidgetPickerBottomSheet(
            visible = showPicker,
            current = widgetItems,
            onDismiss = { showPicker = false },
            onSelect = { selected ->
                val newItems = widgetItems.toMutableList().apply { add(selected) }
                updateWidgets(newItems)
            }
        )
    }
}
