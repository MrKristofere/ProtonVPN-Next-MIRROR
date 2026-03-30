/*
 * Copyright (C) 2026 SMH01
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.utils.SharedCountryUtils

@Composable
fun LoadIndicator(load: Int, displayMode: ServerLoadDisplayMode) {
    if (displayMode == ServerLoadDisplayMode.HIDDEN || displayMode == ServerLoadDisplayMode.LINE) return
    
    val colors = ProtonNextTheme.colors
    val loadColor = SharedCountryUtils.getColorForLoad(load)
    
    Surface(
        color = loadColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$load%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = loadColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LoadProgressBar(load: Int, displayMode: ServerLoadDisplayMode) {
    if (displayMode == ServerLoadDisplayMode.HIDDEN || displayMode == ServerLoadDisplayMode.PERCENT) return
    
    val colors = ProtonNextTheme.colors
    val loadColor = SharedCountryUtils.getColorForLoad(load)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(colors.backgroundSecondary.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(load / 100f)
                .fillMaxHeight()
                .background(loadColor)
        )
    }
}

@Composable
private fun Surface(
    color: androidx.compose.ui.graphics.Color,
    shape: androidx.compose.ui.graphics.Shape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(shape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
