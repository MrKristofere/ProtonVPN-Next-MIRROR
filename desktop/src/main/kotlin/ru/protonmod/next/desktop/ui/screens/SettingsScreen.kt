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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.ui.theme.AppTheme
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass
import ru.protonmod.next.desktop.ui.utils.isTablet
import ru.protonmod.next.desktop.ui.MainTarget

import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: DesktopSettingsManager,
    navigateTo: (MainTarget) -> Unit
) {
    val colors = ProtonNextTheme.colors
    val settings by settingsManager.settings.collectAsState()
    val isTablet = isTablet()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(Strings.settings_title(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = if (isTablet) Alignment.CenterHorizontally else Alignment.Start,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (isTablet) 140.dp else 120.dp
            )
        ) {
            if (isTablet) {
                item {
                    Row(
                        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FeatureCategory(
                                isTablet = true,
                                onNavigateToSplitTunneling = { navigateTo(MainTarget.SplitTunneling) },
                                onNavigateToProtocol = { navigateTo(MainTarget.ProtocolSelection) }
                            )

                            ConnectionSettingsSection(
                                settings = settings,
                                settingsManager = settingsManager,
                                navigateTo = navigateTo
                            )

                            CustomizationSettingsSection(
                                settings = settings,
                                navigateTo = navigateTo
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            PrivacySettingsSection(
                                settings = settings,
                                settingsManager = settingsManager,
                                navigateTo = navigateTo
                            )

                            AboutSettingsSection()
                        }
                    }
                }
            } else {
                // Phone Layout
                item {
                    FeatureCategory(
                        isTablet = false,
                        onNavigateToSplitTunneling = { navigateTo(MainTarget.SplitTunneling) },
                        onNavigateToProtocol = { navigateTo(MainTarget.ProtocolSelection) }
                    )
                }

                item {
                    ConnectionSettingsSection(
                        settings = settings,
                        settingsManager = settingsManager,
                        navigateTo = navigateTo
                    )
                }

                item {
                    CustomizationSettingsSection(
                        settings = settings,
                        navigateTo = navigateTo
                    )
                }

                item {
                    PrivacySettingsSection(
                        settings = settings,
                        settingsManager = settingsManager,
                        navigateTo = navigateTo
                    )
                }

                item {
                    AboutSettingsSection()
                }
            }
        }
    }
}

@Composable
private fun FeatureCategory(
    isTablet: Boolean,
    onNavigateToSplitTunneling: () -> Unit,
    onNavigateToProtocol: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = if (isTablet) Arrangement.Start else Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tileModifier = if (isTablet) Modifier.size(160.dp) else Modifier.weight(1f)

        FeatureTile(
            modifier = tileModifier,
            title = Strings.settings_split_tunneling(),
            subtitle = Strings.settings_split_tunneling_desc(),
            icon = Icons.AutoMirrored.Rounded.AltRoute,
            isActive = true,
            onClick = onNavigateToSplitTunneling
        )

        if (isTablet) Spacer(modifier = Modifier.width(16.dp))

        FeatureTile(
            modifier = tileModifier,
            title = Strings.protocol_title(),
            subtitle = "AmneziaWG",
            icon = Icons.Rounded.Security,
            isActive = true,
            onClick = onNavigateToProtocol
        )
    }
}

@Composable
private fun ConnectionSettingsSection(
    settings: ru.protonmod.next.desktop.data.DesktopSettings,
    settingsManager: DesktopSettingsManager,
    navigateTo: (MainTarget) -> Unit
) {
    Category(title = Strings.settings_connection()) {
        SettingToggleRow(
            icon = Icons.Rounded.Autorenew,
            title = Strings.settings_auto_connect(),
            subtitle = Strings.settings_auto_connect_desc(),
            checked = settings.autoConnectEnabled,
            onCheckedChange = { settingsManager.setAutoConnectEnabled(it) }
        )

        SettingRowWithIcon(
            icon = Icons.Rounded.CloudSync,
            title = Strings.settings_api_bypass(),
            subtitle = Strings.settings_api_bypass_desc(),
            onClick = { /* Coming soon */ }
        )

        SettingRowWithIcon(
            icon = Icons.Rounded.Numbers,
            title = Strings.settings_port(),
            subtitle = if (settings.vpnPort == 0) Strings.settings_port_auto() else settings.vpnPort.toString(),
            onClick = { /* Implement port selection if needed */ }
        )
    }
}

@Composable
private fun CustomizationSettingsSection(
    settings: ru.protonmod.next.desktop.data.DesktopSettings,
    navigateTo: (MainTarget) -> Unit
) {
    Category(title = Strings.settings_customization()) {
        SettingRowWithIcon(
            icon = Icons.Rounded.Palette,
            title = Strings.settings_app_theme(),
            subtitle = settings.appTheme.name,
            onClick = { navigateTo(MainTarget.ThemeSelection) }
        )

        SettingRowWithIcon(
            icon = Icons.Rounded.BarChart,
            title = Strings.settings_load_display_mode(),
            subtitle = settings.serverLoadDisplayMode.name,
            onClick = { navigateTo(MainTarget.ServerLoadSelection) }
        )
    }
}

@Composable
private fun PrivacySettingsSection(
    settings: ru.protonmod.next.desktop.data.DesktopSettings,
    settingsManager: DesktopSettingsManager,
    navigateTo: (MainTarget) -> Unit
) {
    Category(title = Strings.settings_privacy()) {
        SettingRowWithIcon(
            icon = Icons.Rounded.Dns,
            title = Strings.settings_custom_dns(),
            subtitle = if (settings.useCustomDns) settings.customDns else Strings.settings_custom_dns_default(),
            onClick = { navigateTo(MainTarget.CustomDns) }
        )

        SettingRowWithIcon(
            icon = Icons.Rounded.GppMaybe,
            title = Strings.settings_kill_switch(),
            subtitle = Strings.kill_switch_desc(),
            onClick = { /* Functionality handled by toggle below, or dedicated screen */ }
        )
        
        SettingToggleRow(
            icon = Icons.Rounded.GppGood,
            title = Strings.settings_kill_switch(),
            subtitle = if (settings.killSwitchEnabled) Strings.settings_on() else Strings.settings_off(),
            checked = settings.killSwitchEnabled,
            onCheckedChange = { settingsManager.setKillSwitchEnabled(it) }
        )

        SettingRowWithIcon(
            icon = Icons.Rounded.BugReport,
            title = Strings.settings_error_reporting(),
            subtitle = Strings.settings_error_reporting_desc(),
            onClick = { navigateTo(MainTarget.ErrorReporting) }
        )

        SettingToggleRow(
            icon = Icons.Rounded.Notifications,
            title = Strings.settings_notifications(),
            subtitle = if (settings.notificationsEnabled) Strings.settings_on() else Strings.settings_off(),
            checked = settings.notificationsEnabled,
            onCheckedChange = { /* Implement in manager */ }
        )
    }
}

@Composable
private fun AboutSettingsSection() {
    Category(title = Strings.settings_about()) {
        SettingRowWithIcon(
            icon = Icons.Rounded.Info,
            title = "Version",
            subtitle = Strings.settings_version("1.0.0-desktop"),
            onClick = {}
        )
        
        SettingRowWithIcon(
            icon = Icons.Rounded.BugReport,
            title = Strings.settings_debug(),
            subtitle = Strings.debug_title(),
            onClick = { /* Coming soon */ }
        )
    }
}

@Composable
fun Category(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ProtonNextTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.brandNorm,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.2f)
        ) {
            content()
        }
    }
}

@Composable
fun SettingRowWithIcon(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = ProtonNextTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.iconNorm,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textNorm,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textWeak
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.iconWeak,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ProtonNextTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.iconNorm,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textNorm,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textWeak
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.textInverted,
                checkedTrackColor = colors.brandNorm,
                uncheckedThumbColor = colors.textWeak,
                uncheckedTrackColor = colors.backgroundSecondary
            )
        )
    }
}

@Composable
fun FeatureTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .liquidGlass(
                shape = RoundedCornerShape(16.dp),
                alpha = if (isActive) 0.3f else 0.4f,
                shadowElevation = 0.dp
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) colors.brandNorm.copy(alpha = 0.15f)
                        else colors.backgroundSecondary.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) colors.brandNorm else colors.iconWeak,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.textNorm
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textWeak,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
