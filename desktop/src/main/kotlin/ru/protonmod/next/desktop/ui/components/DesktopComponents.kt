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

package ru.protonmod.next.desktop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.ui.MainTarget
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass
import ru.protonmod.next.ui.utils.CommonCountryUtils
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings

@Composable
fun LiquidGlassBottomBar(
    selectedTarget: MainTarget?,
    navigateTo: (MainTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProtonNextTheme.colors
    val glassShape = RoundedCornerShape(32.dp)
    val glassBackgroundColor = colors.backgroundNorm.copy(alpha = 0.85f)

    val glassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.2f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .shadow(15.dp, glassShape)
            .clip(glassShape)
            .background(glassBackgroundColor)
            .border(1.dp, glassBorderBrush, glassShape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(MainTarget.Home, MainTarget.Countries, MainTarget.Profiles, MainTarget.Settings).forEach { target ->
                val isSelected = target == selectedTarget
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) colors.brandNorm else colors.iconWeak,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { navigateTo(target) }
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier.size(40.dp)
                                .background(iconColor.copy(alpha = 0.15f), CircleShape)
                        )
                    }
                    Icon(
                        imageVector = getIconForTarget(target),
                        contentDescription = target.name,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun getIconForTarget(target: MainTarget): ImageVector = when (target) {
    MainTarget.Home -> Icons.Rounded.Home
    MainTarget.Profiles -> Icons.Rounded.Terminal
    MainTarget.Countries -> Icons.Rounded.Public
    MainTarget.Settings -> Icons.Rounded.Settings
    else -> Icons.Rounded.Settings
}

@Composable
fun DesktopConnectionCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    serverName: String,
    countryCode: String,
    cityName: String = "",
    ipAddress: String,
    onToggle: () -> Unit,
    onChangeStrategy: () -> Unit = {},
    quickConnectStrategy: String = "fastest"
) {
    val colors = ProtonNextTheme.colors
    val countryName: String = if (countryCode == "fastest" || countryCode.isEmpty()) Strings.get("label_fastest_server") 
                      else CommonCountryUtils.getCountryName(countryCode).ifBlank { countryCode }
    val displayLocation: String = if (cityName.isNotEmpty()) "$countryName, $cityName" else countryName
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .liquidGlass(
                shape = RoundedCornerShape(32.dp),
                alpha = if (isConnected) 0.2f else 0.4f,
                shadowElevation = 0.dp
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        isConnected -> Strings.status_connected()
                        isConnecting -> Strings.status_connecting()
                        else -> Strings.status_not_connected()
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isConnected) colors.notificationSuccess else colors.textNorm.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(12.dp))
                
                Surface(
                    color = colors.backgroundSecondary.copy(alpha = 0.86F),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "$displayLocation • $ipAddress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textWeak,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isConnecting) { onChangeStrategy() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp, 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.backgroundNorm),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnected || isConnecting || (countryCode.isNotEmpty() && countryCode != "fastest")) {
                        FlagIcon(countryCode = countryCode, size = DpSize(48.dp, 32.dp))
                    } else {
                        val iconVector = when (quickConnectStrategy) {
                            "recent" -> Icons.Rounded.History
                            else -> Icons.Rounded.Speed
                        }
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = colors.brandNorm,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayLocation.ifBlank { Strings.get("label_fastest_server") },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textNorm,
                        maxLines = 1
                    )
                    Text(
                        text = if (isConnected || isConnecting) serverName else Strings.get("label_select_location"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textWeak
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colors.iconWeak.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) colors.shade20 else colors.brandNorm,
                    contentColor = if (isConnected) colors.textNorm else colors.textInverted
                ),
                enabled = !isConnecting
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = colors.textInverted,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isConnected) Strings.btn_disconnect() else Strings.btn_quick_connect(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
