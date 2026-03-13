package ru.protonmod.next.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.protonmod.next.ui.theme.ProtonNextTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    var killSwitchEnabled by remember { mutableStateOf(false) }
    var vpnAcceleratorEnabled by remember { mutableStateOf(true) }
    var netShieldEnabled by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.backgroundNorm)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundNorm)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SettingCategory("Connection") }
            item {
                SettingItem(
                    icon = Icons.Rounded.Security,
                    title = "Protocol",
                    subtitle = "Smart (WireGuard)",
                    onClick = {}
                )
            }
            item {
                SettingItem(
                    icon = Icons.Rounded.Dns,
                    title = "DNS",
                    subtitle = "Proton DNS",
                    onClick = {}
                )
            }
            item {
                SettingItem(
                    icon = Icons.Rounded.FlashOn,
                    title = "VPN Accelerator",
                    subtitle = if (vpnAcceleratorEnabled) "Enabled" else "Disabled",
                    trailingContent = {
                        Switch(
                            checked = vpnAcceleratorEnabled,
                            onCheckedChange = { vpnAcceleratorEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.textInverted,
                                checkedTrackColor = colors.brandNorm,
                                uncheckedThumbColor = colors.textWeak,
                                uncheckedTrackColor = colors.backgroundSecondary
                            )
                        )
                    },
                    onClick = { vpnAcceleratorEnabled = !vpnAcceleratorEnabled }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { SettingCategory("Security") }
            item {
                SettingItem(
                    icon = Icons.Rounded.Block,
                    title = "NetShield",
                    subtitle = if (netShieldEnabled) "Enabled" else "Disabled",
                    trailingContent = {
                        Switch(
                            checked = netShieldEnabled,
                            onCheckedChange = { netShieldEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.textInverted,
                                checkedTrackColor = colors.brandNorm,
                                uncheckedThumbColor = colors.textWeak,
                                uncheckedTrackColor = colors.backgroundSecondary
                            )
                        )
                    },
                    onClick = { netShieldEnabled = !netShieldEnabled }
                )
            }
            item {
                SettingItem(
                    icon = Icons.Rounded.GppGood,
                    title = "Kill Switch",
                    subtitle = if (killSwitchEnabled) "Enabled" else "Disabled",
                    trailingContent = {
                        Switch(
                            checked = killSwitchEnabled,
                            onCheckedChange = { killSwitchEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.textInverted,
                                checkedTrackColor = colors.brandNorm,
                                uncheckedThumbColor = colors.textWeak,
                                uncheckedTrackColor = colors.backgroundSecondary
                            )
                        )
                    },
                    onClick = { killSwitchEnabled = !killSwitchEnabled }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { SettingCategory("App") }
            item {
                SettingItem(
                    icon = Icons.Rounded.Info,
                    title = "Version",
                    subtitle = "1.0.0-desktop",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun SettingCategory(title: String) {
    val colors = ProtonNextTheme.colors
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = colors.brandNorm,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundSecondary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
