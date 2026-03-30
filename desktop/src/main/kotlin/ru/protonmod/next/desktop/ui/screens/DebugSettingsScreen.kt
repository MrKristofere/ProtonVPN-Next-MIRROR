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

package ru.protonmod.next.desktop.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.protonmod.next.desktop.DesktopLoginViewModel
import ru.protonmod.next.desktop.ServerEntry
import ru.protonmod.next.desktop.data.DesktopVpnDataManager
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    onBack: () -> Unit,
    viewModel: DesktopLoginViewModel,
    dataManager: DesktopVpnDataManager
) {
    val colors = ProtonNextTheme.colors
    val scope = rememberCoroutineScope()
    var showNukeConfirm by remember { mutableStateOf(false) }
    var showServerSelect by remember { mutableStateOf(false) }
    
    val servers by viewModel.servers.collectAsState()
    
    // In Desktop, we don't have exactly the same session state management in ViewModel yet
    // but we can query from dataManager
    var sessionInfo by remember { mutableStateOf<ru.protonmod.next.desktop.data.local.DesktopSessionEntity?>(null) }
    
    LaunchedEffect(Unit) {
        sessionInfo = dataManager.database.getSession()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.debug_title(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.desc_back(), tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Immersive background highlight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Red.copy(alpha = 0.1f),
                                colors.backgroundNorm.copy(alpha = 0.1f),
                                colors.backgroundNorm
                            )
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val contentModifier = Modifier.widthIn(max = 800.dp).fillMaxWidth()

                // Session Info
                item {
                    DebugSection(modifier = contentModifier, title = Strings.debug_session_header()) {
                        sessionInfo?.let { session ->
                            DebugInfoRow("User ID", session.userId)
                            DebugInfoRow("Session ID", session.sessionId)
                            DebugInfoRow("Tier", when (session.userTier) {
                                1 -> "Basic"
                                2 -> "Plus"
                                else -> "Free"
                            })
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = colors.shade20.copy(alpha = 0.1f)
                            )
                            
                            Button(
                                onClick = { viewModel.refreshCertificate() },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandNorm),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.debug_btn_refresh_cert())
                            }
                        } ?: Text("No session found", color = colors.textWeak, modifier = Modifier.padding(16.dp))
                    }
                }

                // Exports & Diagnostics
                item {
                    DebugSection(modifier = contentModifier, title = Strings.debug_exports_header()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DebugActionRow(
                                icon = Icons.Rounded.History,
                                title = Strings.debug_btn_export_logs(),
                                onClick = { /* Log export logic */ }
                            )
                            DebugActionRow(
                                icon = Icons.Rounded.FileDownload,
                                title = Strings.debug_btn_export_config(),
                                onClick = { showServerSelect = true }
                            )
                        }
                    }
                }

                // Danger Zone
                item {
                    DebugSection(
                        modifier = contentModifier,
                        title = Strings.debug_danger_header(),
                        titleColor = colors.notificationError
                    ) {
                        Button(
                            onClick = { showNukeConfirm = true },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.notificationError),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CleaningServices, null)
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.debug_btn_nuke())
                        }
                    }
                }
            }
        }
    }

    if (showNukeConfirm) {
        AlertDialog(
            onDismissRequest = { showNukeConfirm = false },
            title = { Text(Strings.debug_btn_nuke()) },
            text = { Text(Strings.debug_nuke_confirm()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNukeConfirm = false
                        scope.launch {
                            dataManager.database.clearSession()
                            dataManager.database.clearAllServers()
                            dataManager.database.clearCacheInfo()
                            // Also clear settings
                            dataManager.settingsManager.clearSession()
                            viewModel.disconnect()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.notificationError)
                ) {
                    Text("NUKE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNukeConfirm = false }) {
                    Text(Strings.btn_cancel())
                }
            }
        )
    }

    if (showServerSelect) {
        AlertDialog(
            onDismissRequest = { showServerSelect = false },
            title = { Text(Strings.debug_select_server()) },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(servers) { server ->
                        ListItem(
                            headlineContent = { Text(server.name) },
                            supportingContent = { Text(server.country) },
                            modifier = Modifier.clickable {
                                // Logic to export config
                                showServerSelect = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showServerSelect = false }) { Text(Strings.btn_close()) }
            }
        )
    }
}

@Composable
private fun DebugSection(
    modifier: Modifier = Modifier,
    title: String,
    titleColor: Color = ProtonNextTheme.colors.brandNorm,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.3f)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun DebugInfoRow(label: String, value: String) {
    val colors = ProtonNextTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.textNorm, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DebugActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.brandNorm)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textNorm)
    }
}
