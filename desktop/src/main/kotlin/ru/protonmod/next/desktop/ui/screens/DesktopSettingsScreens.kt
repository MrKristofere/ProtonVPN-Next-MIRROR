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

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.protonmod.next.data.model.ObfuscationProfile
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.desktop.ui.utils.isTablet
import ru.protonmod.next.ui.theme.AppTheme
import ru.protonmod.next.ui.theme.LocalColors
import ru.protonmod.next.ui.theme.ProtonColors
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass

import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.desktop.ServerEntry
import ru.protonmod.next.desktop.ui.components.FlagIcon
import ru.protonmod.next.desktop.ui.components.LoadIndicator
import ru.protonmod.next.desktop.ui.components.LoadProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLoadDisplayModeScreen(
    onBack: () -> Unit,
    settingsManager: DesktopSettingsManager
) {
    val settings by settingsManager.settings.collectAsState()
    val colors = ProtonNextTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Server Load Display", fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(ServerLoadDisplayMode.entries) { mode ->
                    LoadModePreviewCard(
                        mode = mode,
                        isSelected = settings.serverLoadDisplayMode == mode,
                        onClick = { settingsManager.setServerLoadDisplayMode(mode) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadModePreviewCard(
    mode: ServerLoadDisplayMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val modeName = mode.name.lowercase().replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) colors.brandNorm else Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            val mockServer = remember {
                ServerEntry(
                    id = "preview",
                    name = "US-FREE #1",
                    city = "New York",
                    country = "US",
                    tier = 0,
                    physicalServer = ru.protonmod.next.data.network.PhysicalServer(
                        id = "preview_phys",
                        domain = "preview",
                        status = 1,
                        load = 45
                    )
                )
            }
            
            // Reusing the ServerCard logic for preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.backgroundSecondary.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, colors.shade100.copy(alpha = 0.05f))
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlagIcon(countryCode = mockServer.country, size = androidx.compose.ui.unit.DpSize(36.dp, 24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mockServer.country, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(mockServer.name, style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
                        }
                        LoadIndicator(load = 45, displayMode = mode)
                    }
                    LoadProgressBar(load = 45, displayMode = mode)
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .background(colors.brandNorm, CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = modeName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.brandNorm else colors.textNorm
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    onBack: () -> Unit,
    settingsManager: DesktopSettingsManager
) {
    val settings by settingsManager.settings.collectAsState()
    val colors = ProtonNextTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("App Theme", fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(AppTheme.entries) { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        isSelected = settings.appTheme == theme,
                        onClick = { settingsManager.setAppTheme(theme) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemePreviewCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val themeName = theme.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.7f)
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(16.dp),
                    alpha = 0.95f,
                    shadowElevation = if (isSelected) 8.dp else 0.dp
                )
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) colors.brandNorm else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MiniDashboardPreview(theme = theme)

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .background(colors.brandNorm, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = themeName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) colors.brandNorm else colors.textNorm
        )
    }
}

@Composable
fun MiniDashboardPreview(theme: AppTheme) {
    val themeColors = when (theme) {
        AppTheme.LIGHT -> ProtonColors.Light
        AppTheme.DARK -> ProtonColors.Dark
        AppTheme.AMOLED -> ProtonColors.Amoled
        AppTheme.GOLD_LIGHT -> ProtonColors.GoldLight
        AppTheme.GOLD_DARK -> ProtonColors.GoldDark
        AppTheme.GOLD_AMOLED -> ProtonColors.GoldAmoled
        AppTheme.SURFSHARK -> ProtonColors.Surfshark
        AppTheme.NORD -> ProtonColors.Nord
        AppTheme.IPVANISH -> ProtonColors.IPVanish
        AppTheme.PUREVPN -> ProtonColors.PureVPN
        AppTheme.MULLVAD -> ProtonColors.Mullvad
        AppTheme.WINDSCRIBE -> ProtonColors.Windscribe
    }

    CompositionLocalProvider(LocalColors provides themeColors) {
        val colors = ProtonNextTheme.colors
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundNorm)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.brandNorm.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    color = colors.backgroundSecondary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Box(modifier = Modifier.width(36.dp).height(6.dp).background(colors.textNorm.copy(alpha = 0.6f), CircleShape))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp, 16.dp).background(colors.shade20, RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Box(modifier = Modifier.width(60.dp).height(8.dp).background(colors.textNorm, CircleShape))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.width(40.dp).height(6.dp).background(colors.textWeak, CircleShape))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .background(colors.brandNorm, RoundedCornerShape(8.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtocolSelectionScreen(
    currentProtocol: String,
    onBack: () -> Unit,
    onProtocolSelected: (String) -> Unit,
    onNavigateToObfuscation: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val protocols = listOf("AmneziaWG")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Select Protocol", fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(protocols) { protocol ->
                    val isSelected = protocol == currentProtocol
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                alpha = if (isSelected) 0.6f else 0.4f
                            )
                            .clickable { onProtocolSelected(protocol) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = protocol,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.brandNorm else colors.textNorm
                                )
                                Text(
                                    text = if (protocol == "AmneziaWG") "High-performance protocol with advanced obfuscation." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textWeak
                                )
                            }

                            if (protocol == "AmneziaWG") {
                                IconButton(
                                    onClick = onNavigateToObfuscation,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Obfuscation Settings",
                                        tint = colors.brandNorm
                                    )
                                }
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObfuscationSettingsScreen(
    onBack: () -> Unit,
    settingsManager: DesktopSettingsManager
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
                title = { Text("Obfuscation", fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textNorm)
                    }
                },
                actions = {
                    IconButton(onClick = { settingsManager.setObfuscationParams(ru.protonmod.next.vpn.VpnConstants.DEFAULT_OBFUSCATION_PARAMS) }) {
                        Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Reset", tint = colors.brandNorm)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = if (isTablet) Alignment.CenterHorizontally else Alignment.Start,
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val contentModifier = if (isTablet) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()

                item {
                    Box(
                        modifier = contentModifier
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                alpha = if (settings.obfuscationEnabled) 0.3f else 0.5f
                            )
                            .clickable { settingsManager.setObfuscationEnabled(!settings.obfuscationEnabled) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Obfuscation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textNorm
                                )
                                Text(
                                    text = "Hide VPN traffic using AmneziaWG parameters.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textWeak
                                )
                            }
                            Switch(
                                checked = settings.obfuscationEnabled,
                                onCheckedChange = { settingsManager.setObfuscationEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.textInverted,
                                    checkedTrackColor = colors.brandNorm
                                )
                            )
                        }
                    }
                }

                if (settings.obfuscationEnabled) {
                    item {
                        Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            
                            CategoryHeader(title = "Packet Magic (H1-H4)")
                            SettingsCard {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "H1",
                                        value = settings.h1,
                                        isNumeric = false,
                                        onValueChange = { settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(h1 = it)) }
                                    )
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "H2",
                                        value = settings.h2,
                                        isNumeric = false,
                                        onValueChange = { settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(h2 = it)) }
                                    )
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "H3",
                                        value = settings.h3,
                                        isNumeric = false,
                                        onValueChange = { settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(h3 = it)) }
                                    )
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "H4",
                                        value = settings.h4,
                                        isNumeric = false,
                                        onValueChange = { settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(h4 = it)) }
                                    )
                                }
                            }

                            CategoryHeader(title = "Junk Packets")
                            SettingsCard {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "Count (Jc)",
                                        value = settings.jc.toString(),
                                        onValueChange = { val v = it.toIntOrNull() ?: 0; settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(jc = v)) }
                                    )
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "Min Size (Jmin)",
                                        value = settings.jmin.toString(),
                                        onValueChange = { val v = it.toIntOrNull() ?: 0; settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(jmin = v)) }
                                    )
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "Max Size (Jmax)",
                                        value = settings.jmax.toString(),
                                        onValueChange = { val v = it.toIntOrNull() ?: 0; settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(jmax = v)) }
                                    )
                                }
                            }

                            CategoryHeader(title = "Scrambling (S1-S2)")
                            SettingsCard {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "S1",
                                        value = settings.s1.toString(),
                                        onValueChange = { val v = it.toIntOrNull() ?: 0; settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(s1 = v)) }
                                    )
                                    ObfuscationParamField(
                                        modifier = Modifier.weight(1f),
                                        label = "S2",
                                        value = settings.s2.toString(),
                                        onValueChange = { val v = it.toIntOrNull() ?: 0; settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(s2 = v)) }
                                    )
                                }
                            }

                            CategoryHeader(title = "Advanced Scrambling (I1)")
                            SettingsCard {
                                ObfuscationParamField(
                                    label = "Init Magic (I1)",
                                    value = settings.i1,
                                    isNumeric = false,
                                    onValueChange = { settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(i1 = it)) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { settingsManager.setObfuscationParams(settingsManager.getObfuscationParams().copy(i1 = java.util.UUID.randomUUID().toString())) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.brandNorm)
                                    ) {
                                        Text("Randomize")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = ProtonNextTheme.colors.brandNorm,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(20.dp), alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ObfuscationParamField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isEnabled: Boolean = true,
    isNumeric: Boolean = true,
    onValueChange: (String) -> Unit
) {
    val colors = ProtonNextTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textWeak
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEnabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isNumeric) KeyboardType.Number else KeyboardType.Text
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.brandNorm,
                unfocusedBorderColor = colors.shade20,
                focusedTextColor = colors.textNorm,
                unfocusedTextColor = colors.textNorm
            )
        )
    }
}
