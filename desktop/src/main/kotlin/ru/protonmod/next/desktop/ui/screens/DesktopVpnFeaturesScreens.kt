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

package ru.protonmod.next.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.network.DesktopSplitTunnelingManager
import ru.protonmod.next.desktop.network.DesktopDnsManager
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass
import ru.protonmod.next.desktop.ui.utils.isTablet
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelingScreen(
    onBack: () -> Unit,
    manager: DesktopSplitTunnelingManager
) {
    val config by manager.config.collectAsState()
    val colors = ProtonNextTheme.colors
    val isTablet = isTablet()

    var showAddAppDialog by remember { mutableStateOf(false) }
    var showAddIpDialog by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.settings_split_tunneling(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.desc_back(), tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = if (isTablet) Alignment.CenterHorizontally else Alignment.Start
        ) {
            val contentModifier = if (isTablet) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()

            item {
                ToggleCard(
                    modifier = contentModifier,
                    title = Strings.st_enable(),
                    subtitle = Strings.st_enable_desc(),
                    checked = config.enabled,
                    onCheckedChange = { manager.setEnabled(it) }
                )
            }

            if (config.enabled) {
                item {
                    ModeSelectionCard(
                        modifier = contentModifier,
                        currentMode = config.mode,
                        onModeSelected = { manager.setMode(it) }
                    )
                }

                item { CategoryHeader(title = Strings.st_apps_header()) }
                items(config.excludedApps.toList()) { app ->
                    ItemRow(
                        modifier = contentModifier,
                        title = app,
                        onRemove = { manager.removeExcludedApp(app) }
                    )
                }
                item {
                    AddButton(modifier = contentModifier, label = Strings.st_add_app(), onClick = { showAddAppDialog = true })
                }

                item { CategoryHeader(title = Strings.st_ips_header()) }
                items(config.excludedIps.toList()) { ip ->
                    ItemRow(
                        modifier = contentModifier,
                        title = ip,
                        onRemove = { manager.removeExcludedIp(ip) }
                    )
                }
                item {
                    AddButton(modifier = contentModifier, label = Strings.st_add_ip(), onClick = { showAddIpDialog = true })
                }

                item { CategoryHeader(title = Strings.st_domains_header()) }
                items(config.excludedDomains.toList()) { domain ->
                    ItemRow(
                        modifier = contentModifier,
                        title = domain,
                        onRemove = { manager.removeExcludedDomain(domain) }
                    )
                }
                item {
                    AddButton(modifier = contentModifier, label = Strings.st_add_domain(), onClick = { showAddDomainDialog = true })
                }
            }
        }
    }

    if (showAddAppDialog) {
        InputDialog(
            title = Strings.st_add_app(),
            label = Strings.st_input_app_label(),
            onDismiss = { showAddAppDialog = false },
            onConfirm = { 
                manager.addExcludedApp(it)
                showAddAppDialog = false
            }
        )
    }

    if (showAddIpDialog) {
        InputDialog(
            title = Strings.st_add_ip(),
            label = Strings.st_input_ip_label(),
            onDismiss = { showAddIpDialog = false },
            onConfirm = { 
                if (manager.addExcludedIp(it)) showAddIpDialog = false
            }
        )
    }

    if (showAddDomainDialog) {
        InputDialog(
            title = Strings.st_add_domain(),
            label = Strings.st_input_domain_label(),
            onDismiss = { showAddDomainDialog = false },
            onConfirm = { 
                if (manager.addExcludedDomain(it)) showAddDomainDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDnsScreen(
    onBack: () -> Unit,
    manager: DesktopDnsManager
) {
    val config by manager.config.collectAsState()
    val colors = ProtonNextTheme.colors
    val isTablet = isTablet()

    var showCustomDnsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.settings_custom_dns(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.desc_back(), tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = if (isTablet) Alignment.CenterHorizontally else Alignment.Start
        ) {
            val contentModifier = if (isTablet) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()

            item {
                ToggleCard(
                    modifier = contentModifier,
                    title = Strings.dns_use_custom(),
                    subtitle = Strings.dns_use_custom_desc(),
                    checked = config.useCustomDns,
                    onCheckedChange = { manager.setUseCustomDns(it) }
                )
            }

            if (config.useCustomDns) {
                item { CategoryHeader(title = Strings.dns_presets_header()) }
                item {
                    Column(modifier = contentModifier.liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.4f)) {
                        PresetDnsRow(Strings.dns_preset_proton(), manager.getActiveDns() == DesktopDnsManager.PROTON_DNS_IPv4) { manager.resetToDefault() }
                        PresetDnsRow(Strings.dns_preset_cloudflare(), manager.getActiveDns() == DesktopDnsManager.CLOUDFLARE_IPv4) { manager.setToCloudflare() }
                        PresetDnsRow(Strings.dns_preset_google(), manager.getActiveDns() == DesktopDnsManager.GOOGLE_IPv4) { manager.setToGoogle() }
                        PresetDnsRow(Strings.dns_preset_quad9(), manager.getActiveDns() == DesktopDnsManager.QUAD9_IPv4) { manager.setToQuad9() }
                    }
                }

                item { CategoryHeader(title = Strings.dns_custom_header()) }
                item {
                    ItemRow(
                        modifier = contentModifier,
                        title = if (manager.getPresetName().startsWith("Custom")) config.customDns else Strings.dns_manual_ip(),
                        onRemove = null,
                        onClick = { showCustomDnsDialog = true }
                    )
                }
            }
        }
    }

    if (showCustomDnsDialog) {
        InputDialog(
            title = Strings.settings_custom_dns_title(),
            label = Strings.dns_input_label(),
            initialValue = if (manager.getPresetName().startsWith("Custom")) config.customDns else "",
            onDismiss = { showCustomDnsDialog = false },
            onConfirm = { 
                if (manager.setCustomDns(it)) showCustomDnsDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorReportingScreen(
    onBack: () -> Unit,
    settingsManager: ru.protonmod.next.desktop.data.DesktopSettingsManager
) {
    val settings by settingsManager.settings.collectAsState()
    val colors = ProtonNextTheme.colors
    val isTablet = isTablet()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.settings_error_reporting(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.desc_back(), tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = if (isTablet) Alignment.CenterHorizontally else Alignment.Start
        ) {
            val contentModifier = if (isTablet) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()

            item {
                ToggleCard(
                    modifier = contentModifier,
                    title = Strings.sentry_crash_reports(),
                    subtitle = Strings.sentry_crash_reports_desc(),
                    checked = settings.sentryCrashReportingEnabled,
                    onCheckedChange = { settingsManager.setSentryCrashReportingEnabled(it) }
                )
            }

            item {
                ToggleCard(
                    modifier = contentModifier,
                    title = Strings.sentry_analytics(),
                    subtitle = Strings.sentry_analytics_desc(),
                    checked = settings.sentryMetricsEnabled,
                    onCheckedChange = { settingsManager.setSentryMetricsEnabled(it) }
                )
            }

            item {
                ToggleCard(
                    modifier = contentModifier,
                    title = Strings.sentry_breadcrumbs(),
                    subtitle = Strings.sentry_breadcrumbs_desc(),
                    checked = settings.sentryAnalyticsEnabled,
                    onCheckedChange = { settingsManager.setSentryAnalyticsEnabled(it) }
                )
            }
        }
    }
}

@Composable
private fun ToggleCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ProtonNextTheme.colors
    Box(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.4f)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textNorm)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textWeak)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.brandNorm
                )
            )
        }
    }
}

@Composable
private fun ModeSelectionCard(
    modifier: Modifier = Modifier,
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    val colors = ProtonNextTheme.colors
    Column(modifier = modifier) {
        CategoryHeader(title = Strings.settings_st_mode())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.4f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModeOption(
                modifier = Modifier.weight(1f),
                title = Strings.st_mode_exclude(),
                isSelected = currentMode == "exclude",
                onClick = { onModeSelected("exclude") }
            )
            ModeOption(
                modifier = Modifier.weight(1f),
                title = Strings.st_mode_include(),
                isSelected = currentMode == "include",
                onClick = { onModeSelected("include") }
            )
        }
    }
}

@Composable
private fun ModeOption(
    modifier: Modifier = Modifier,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) colors.brandNorm.copy(alpha = 0.2f) else Color.Transparent)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.brandNorm else colors.textNorm
        )
    }
}

@Composable
private fun ItemRow(
    modifier: Modifier = Modifier,
    title: String,
    onRemove: (() -> Unit)?,
    onClick: (() -> Unit)? = null
) {
    val colors = ProtonNextTheme.colors
    Box(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(16.dp), alpha = 0.3f)
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f), color = colors.textNorm)
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Delete, Strings.st_remove_ip_desc(), tint = colors.notificationError)
                }
            } else if (onClick != null) {
                Icon(Icons.Rounded.ChevronRight, null, tint = colors.iconWeak)
            }
        }
    }
}

@Composable
private fun AddButton(modifier: Modifier = Modifier, label: String, onClick: () -> Unit) {
    val colors = ProtonNextTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.brandNorm.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brandNorm)
    ) {
        Icon(Icons.Rounded.Add, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun PresetDnsRow(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = ProtonNextTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), color = colors.textNorm)
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = colors.brandNorm)
        }
    }
}

@Composable
private fun InputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text(Strings.btn_ok()) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.btn_cancel()) }
        }
    )
}
