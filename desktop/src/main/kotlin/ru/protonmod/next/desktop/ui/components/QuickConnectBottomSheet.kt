/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.protonmod.next.desktop.ServerEntry
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings

@Composable
fun QuickConnectBottomSheet(
    onDismiss: () -> Unit,
    currentStrategy: String,
    currentTargetId: String?,
    recentServers: List<ServerEntry>,
    onStrategySelect: (String, String?) -> Unit
) {
    val colors = ProtonNextTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) {} // Prevent closing when clicking inside
                    .liquidGlass(shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), alpha = 0.95f)
                    .padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Handle/Indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp, 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.iconWeak.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = Strings.qc_title(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textNorm
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            StrategyRow(
                                title = Strings.qc_fastest(),
                                subtitle = Strings.qc_fastest_desc(),
                                icon = Icons.Rounded.Speed,
                                isSelected = currentStrategy == "fastest",
                                onClick = { 
                                    onStrategySelect("fastest", null)
                                    onDismiss()
                                }
                            )
                        }
                        
                        item {
                            StrategyRow(
                                title = Strings.qc_recent(),
                                subtitle = Strings.qc_recent_desc(),
                                icon = Icons.Rounded.History,
                                isSelected = currentStrategy == "recent",
                                onClick = { 
                                    onStrategySelect("recent", null)
                                    onDismiss()
                                }
                            )
                        }
                        
                        if (recentServers.isNotEmpty()) {
                            item {
                                Text(
                                    text = Strings.qc_header_recent(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.brandNorm,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                                )
                            }
                            
                            items(recentServers) { server ->
                                StrategyRow(
                                    title = server.country,
                                    subtitle = server.name,
                                    icon = Icons.Rounded.Place,
                                    isSelected = currentStrategy == "server" && currentTargetId == server.id,
                                    countryCode = server.country,
                                    onClick = {
                                        onStrategySelect("server", server.id)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StrategyRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    countryCode: String? = null,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) colors.brandNorm.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) colors.brandNorm.copy(alpha = 0.2f) else colors.backgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                if (countryCode != null) {
                    FlagIcon(countryCode = countryCode, size = androidx.compose.ui.unit.DpSize(24.dp, 16.dp))
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.brandNorm else colors.iconNorm,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) colors.brandNorm else colors.textNorm
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textWeak
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = colors.brandNorm,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
