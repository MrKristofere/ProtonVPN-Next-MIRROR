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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.network.DesktopSplitTunnelingManager
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelingScreen(
    onBack: () -> Unit,
    manager: DesktopSplitTunnelingManager
) {
    val config by manager.config.collectAsState()
    var currentSubScreen by remember { mutableStateOf("main") }
    val colors = ProtonNextTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.brandNorm.copy(alpha = 0.25f),
                            colors.backgroundNorm.copy(alpha = 0.1f),
                            colors.backgroundNorm
                        )
                    )
                )
        )

        AnimatedContent(
            targetState = currentSubScreen,
            transitionSpec = {
                if (targetState != "main") {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                }
            },
            label = "split_tunneling_navigation"
        ) { subScreen ->
            when (subScreen) {
                "main" -> SplitTunnelingMainView(
                    config = config,
                    onBack = onBack,
                    onNavigateToApps = { currentSubScreen = "apps" },
                    onNavigateToIps = { currentSubScreen = "ips" },
                    onNavigateToDomains = { currentSubScreen = "domains" },
                    onToggleEnabled = { manager.setEnabled(it) },
                    onModeSelect = { manager.setMode(it) }
                )
                "apps" -> SplitTunnelingListView(
                    title = if (config.mode == "exclude") Strings.settings_excluded_apps() else Strings.settings_included_apps(),
                    placeholder = Strings.st_input_app_label(),
                    items = config.excludedApps,
                    icon = Icons.Rounded.Apps,
                    onBack = { currentSubScreen = "main" },
                    onAdd = { manager.addExcludedApp(it) },
                    onRemove = { manager.removeExcludedApp(it) }
                )
                "ips" -> SplitTunnelingListView(
                    title = if (config.mode == "exclude") Strings.settings_excluded_ips() else Strings.settings_included_ips(),
                    placeholder = Strings.st_input_ip_label(),
                    items = config.excludedIps,
                    icon = Icons.Rounded.Dns,
                    onBack = { currentSubScreen = "main" },
                    onAdd = { manager.addExcludedIp(it) },
                    onRemove = { manager.removeExcludedIp(it) }
                )
                "domains" -> SplitTunnelingListView(
                    title = if (config.mode == "exclude") Strings.settings_excluded_domains() else Strings.settings_included_domains(),
                    placeholder = Strings.st_input_domain_label(),
                    items = config.excludedDomains,
                    icon = Icons.Rounded.Public,
                    onBack = { currentSubScreen = "main" },
                    onAdd = { manager.addExcludedDomain(it) },
                    onRemove = { manager.removeExcludedDomain(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitTunnelingMainView(
    config: ru.protonmod.next.desktop.network.SplitTunnelingConfig,
    onBack: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToIps: () -> Unit,
    onNavigateToDomains: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onModeSelect: (String) -> Unit
) {
    val colors = ProtonNextTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.settings_split_tunneling(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Icon
            Box(
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(colors.brandNorm.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.AltRoute,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = colors.brandNorm
                )
            }

            Text(
                text = Strings.settings_split_tunneling(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.textNorm
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Strings.settings_split_tunneling_desc(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textWeak,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Settings Section
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingToggleRow(
                        icon = Icons.AutoMirrored.Rounded.AltRoute,
                        title = Strings.settings_split_tunneling(),
                        subtitle = if (config.enabled) Strings.settings_on() else Strings.settings_off(),
                        checked = config.enabled,
                        onCheckedChange = onToggleEnabled
                    )

                    AnimatedVisibility(
                        visible = config.enabled,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = colors.shade20.copy(alpha = 0.1f)
                            )

                            Text(
                                text = Strings.settings_st_mode().uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.brandNorm,
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                            )

                            ModeRow(
                                title = Strings.st_mode_exclude(),
                                subtitle = Strings.st_mode_exclude_desc(),
                                isSelected = config.mode == "exclude",
                                onClick = { onModeSelect("exclude") }
                            )

                            ModeRow(
                                title = Strings.st_mode_include(),
                                subtitle = Strings.st_mode_include_desc(),
                                isSelected = config.mode == "include",
                                onClick = { onModeSelect("include") }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = colors.shade20.copy(alpha = 0.1f)
                            )

                            SubSettingRow(
                                icon = Icons.Rounded.Apps,
                                title = if (config.mode == "exclude") Strings.settings_excluded_apps() else Strings.settings_included_apps(),
                                subtitle = "${config.excludedApps.size} apps",
                                onClick = onNavigateToApps
                            )

                            SubSettingRow(
                                icon = Icons.Rounded.Dns,
                                title = if (config.mode == "exclude") Strings.settings_excluded_ips() else Strings.settings_included_ips(),
                                subtitle = "${config.excludedIps.size} IPs",
                                onClick = onNavigateToIps
                            )

                            SubSettingRow(
                                icon = Icons.Rounded.Public,
                                title = if (config.mode == "exclude") Strings.settings_excluded_domains() else Strings.settings_included_domains(),
                                subtitle = "${config.excludedDomains.size} domains",
                                onClick = onNavigateToDomains
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitTunnelingListView(
    title: String,
    placeholder: String,
    items: Set<String>,
    icon: ImageVector,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val colors = ProtonNextTheme.colors
    var inputValue by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = colors.textNorm) },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(placeholder, color = colors.textWeak) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandNorm,
                            unfocusedBorderColor = colors.shade20
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (inputValue.isNotBlank()) {
                                onAdd(inputValue)
                                inputValue = ""
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (inputValue.isNotBlank()) colors.brandNorm else colors.backgroundSecondary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Add, null, tint = if (inputValue.isNotBlank()) Color.White else colors.iconWeak)
                    }
                }
            }

            if (items.isNotEmpty()) {
                item {
                    Text(
                        text = "SELECTED (${items.size})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.brandNorm,
                        modifier = Modifier.fillMaxWidth(0.9f).padding(top = 16.dp)
                    )
                }

                items(items.toList()) { item ->
                    ListItemRow(
                        text = item,
                        icon = icon,
                        onRemove = { onRemove(item) }
                    )
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                        Text("No entries yet", color = colors.textWeak)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (isSelected) colors.brandNorm else colors.textNorm)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textWeak)
        }
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = colors.brandNorm)
        )
    }
}

@Composable
private fun SubSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
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
        Icon(icon, null, tint = colors.iconNorm, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textNorm)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textWeak)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.iconWeak, modifier = Modifier.size(20.dp))
    }
}

// Helper to rotate the close icon to look like a chevron/arrow if needed, or just use ChevronRight
@Composable
private fun ListItemRow(
    text: String,
    icon: ImageVector,
    onRemove: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .liquidGlass(shape = RoundedCornerShape(12.dp), alpha = 0.2f)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.iconWeak, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textNorm,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, null, tint = colors.notificationError, modifier = Modifier.size(16.dp))
        }
    }
}

