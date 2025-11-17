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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*

data class WidgetItem(
    val name: String,
    val span: Int = 1,
    val iconSize: Dp = 20.dp,
    val shape: Shape = CircleShape,
) {
    @Composable
    fun Render(
        onRemove: () -> Unit,
        showRemove: Boolean = true
    ) {
        val small = span == 1
        val padding = if (small) 0.dp else 32.dp
        val alignment = if (small) Alignment.Center else Alignment.CenterStart
        val arrangement = if (small) Arrangement.Center else Arrangement.Start

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(surfaceVariant(), CircleShape),
            contentAlignment = alignment
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = arrangement,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = padding, end = padding)
            ) {
                Icon(
                    imageVector = WidgetIcon(name),
                    contentDescription = WidgetLabel(name),
                    tint = onSurface(),
                    modifier = Modifier.size(iconSize)
                )
                if (!small) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = WidgetLabel(name),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = onSurface(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showRemove) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(iconSize)
                        .background(Color.Red, CircleShape)
                        .clip(CircleShape)
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "−",
                        color = Color.White,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                }
            }
        }
    }
}

fun load(context: Context): List<WidgetItem> {
    return WidgetSettingsManager.loadWidgets(context)
}

fun save(context: Context, widgets: List<WidgetItem>) {
    WidgetSettingsManager.saveWidgets(context, widgets)
}
