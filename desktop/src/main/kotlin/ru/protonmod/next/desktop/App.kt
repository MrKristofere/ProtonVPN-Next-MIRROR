@file:OptIn(ExperimentalMaterial3Api::class)

package ru.protonmod.next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.ui.MainTarget
import ru.protonmod.next.desktop.ui.components.DesktopConnectionCard
import ru.protonmod.next.desktop.ui.components.LiquidGlassBottomBar
import ru.protonmod.next.desktop.ui.screens.*
import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.desktop.ui.components.*
import ru.protonmod.next.desktop.ui.utils.*
import ru.protonmod.next.ui.theme.ProtonNextTheme as Theme

@Composable
fun App() {
    val settingsManager = remember { DesktopSettingsManager() }
    val authClient = remember { DesktopAuthClient() }
    val vpnClient = remember { DesktopVpnClient(settingsManager) }
    val viewModel = remember { DesktopLoginViewModel(authClient, vpnClient) }

    val uiState by viewModel.uiState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val recentConnections by viewModel.recentConnections.collectAsState()
    val connectedServer by viewModel.connectedServer.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()

    var showCaptcha by remember { mutableStateOf(false) }
    var captchaState by remember { mutableStateOf<DesktopLoginUiState.RequiresCaptcha?>(null) }
    var selectedTarget by remember { mutableStateOf(MainTarget.Home) }

    val countriesViewModel = remember(servers, connectedServer) {
        DesktopCountriesViewModel(vpnClient, settingsManager, viewModel.servers, viewModel.connectedServer)
    }

    BoxWithConstraints {
        val windowWidth = maxWidth
        ProvideDeviceType(windowWidth) {
            val settings by settingsManager.settings.collectAsState()
            Theme(appTheme = settings.appTheme) {
                // Background gradient (moved to top level)
                val colors = Theme.colors
                // Force root Surface to be transparent on Desktop to allow gradient visibility
                Surface(color = Color.Transparent) {
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
                    ) {
                        when (uiState) {
                            is DesktopLoginUiState.Idle, is DesktopLoginUiState.Loading, is DesktopLoginUiState.Error -> {
                                WelcomeContent(
                                    uiState = uiState,
                                    onLogin = { u, p -> viewModel.login(u, p) },
                                    onGuest = { viewModel.loginAnonymous() },
                                    onRetry = { viewModel.loginAnonymous() },
                                    onClearError = { viewModel.clearError() }
                                )
                            }
                            is DesktopLoginUiState.RequiresCaptcha -> {
                                val state = uiState as DesktopLoginUiState.RequiresCaptcha
                                captchaState = state
                                showCaptcha = true
                            }
                            is DesktopLoginUiState.Success -> {
                                DashboardScreen(
                                    servers = servers,
                                    recentConnections = recentConnections,
                                    connectedServer = connectedServer,
                                    isConnecting = isConnecting,
                                    selectedTarget = selectedTarget,
                                    onTargetSelected = { selectedTarget = it },
                                    onLogout = { viewModel.clearError() },
                                    onConnect = { server ->
                                        viewModel.connectToServer(server)
                                    },
                                    onDisconnect = {
                                        viewModel.disconnect()
                                    },
                                    settingsManager = settingsManager,
                                    loadDisplayMode = settings.serverLoadDisplayMode,
                                    countriesViewModel = countriesViewModel
                                )
                            }
                        }

                        if (showCaptcha && captchaState != null) {
                            CaptchaDialog(
                                webUrl = captchaState!!.webUrl,
                                onDismiss = {
                                    showCaptcha = false
                                    viewModel.clearError()
                                },
                                onCaptchaSolved = { token ->
                                    showCaptcha = false
                                    captchaState = null
                                    viewModel.retryWithCaptcha(token)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    uiState: DesktopLoginUiState,
    onLogin: (String, String) -> Unit,
    onGuest: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit
) {
    val isLoading = uiState is DesktopLoginUiState.Loading
    val errorMessage = (uiState as? DesktopLoginUiState.Error)?.message
    val colors = Theme.colors

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ProtonVPN Next",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textInverted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Secure VPN access on desktop. Powered by AmneziaWG Go.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textWeak,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLogin(username, password) },
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brandNorm,
                contentColor = colors.textInverted
            )
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { onGuest() },
            enabled = !isLoading,
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = colors.textInverted)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colors.textInverted,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Continue as Guest",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = colors.notificationError,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { onClearError() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "If you see a captcha prompt, follow the instructions in the popup.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textWeak.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DashboardScreen(
    servers: List<ServerEntry>,
    recentConnections: List<ServerEntry>,
    connectedServer: ServerEntry?,
    isConnecting: Boolean,
    selectedTarget: MainTarget,
    onTargetSelected: (MainTarget) -> Unit,
    onLogout: () -> Unit,
    onConnect: (ServerEntry) -> Unit,
    onDisconnect: () -> Unit,
    settingsManager: DesktopSettingsManager,
    loadDisplayMode: ServerLoadDisplayMode,
    countriesViewModel: DesktopCountriesViewModel
) {
    val colors = Theme.colors
    val isTablet = isTablet()

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTarget) {
            MainTarget.Home -> {
                HomeScreen(
                    servers = servers,
                    recentConnections = recentConnections,
                    connectedServer = connectedServer,
                    isConnecting = isConnecting,
                    onLogout = onLogout,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    isTablet = isTablet,
                    loadDisplayMode = loadDisplayMode
                )
            }
            MainTarget.Countries -> {
                CountriesScreen(
                    viewModel = countriesViewModel,
                    onConnect = onConnect
                )
            }
            MainTarget.Settings -> {
                SettingsScreen(
                    settingsManager = settingsManager,
                    navigateTo = onTargetSelected
                )
            }
            MainTarget.ThemeSelection -> {
                ThemeSelectionScreen(
                    onBack = { onTargetSelected(MainTarget.Settings) },
                    settingsManager = settingsManager
                )
            }
            MainTarget.ProtocolSelection -> {
                ProtocolSelectionScreen(
                    currentProtocol = "AmneziaWG",
                    onBack = { onTargetSelected(MainTarget.Settings) },
                    onProtocolSelected = { /* Already AmneziaWG */ }
                )
            }
            MainTarget.ObfuscationSettings -> {
                ObfuscationSettingsScreen(
                    onBack = { onTargetSelected(MainTarget.Settings) },
                    settingsManager = settingsManager
                )
            }
            MainTarget.ServerLoadSelection -> {
                ServerLoadDisplayModeScreen(
                    onBack = { onTargetSelected(MainTarget.Settings) },
                    settingsManager = settingsManager
                )
            }
            MainTarget.Profiles -> {
                ProfilesScreen()
            }
        }

        LiquidGlassBottomBar(
            selectedTarget = selectedTarget,
            navigateTo = onTargetSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = if (isTablet) 400.dp else 600.dp)
        )
    }
}

@Composable
private fun HomeScreen(
    servers: List<ServerEntry>,
    recentConnections: List<ServerEntry>,
    connectedServer: ServerEntry?,
    isConnecting: Boolean,
    onLogout: () -> Unit,
    onConnect: (ServerEntry) -> Unit,
    onDisconnect: () -> Unit,
    isTablet: Boolean,
    loadDisplayMode: ServerLoadDisplayMode
) {
    val colors = Theme.colors
    
    Box(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Proton VPN", fontWeight = FontWeight.Bold, color = colors.textNorm) },
            actions = {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Logout, "Logout", tint = colors.interactionNorm)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        if (isTablet) {
            // Tablet Layout: Split connection (Left) and recent connections (Right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 120.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left Column: Status and Connection
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.backgroundSecondary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Map,
                            null,
                            modifier = Modifier.size(160.dp),
                            tint = colors.brandNorm.copy(alpha = 0.2f)
                        )
                    }

                    DesktopConnectionCard(
                        isConnected = connectedServer != null,
                        isConnecting = isConnecting,
                        serverName = connectedServer?.name ?: "Quick Connect",
                        countryName = connectedServer?.country ?: "Select Location",
                        ipAddress = if (connectedServer != null) "10.2.0.2" else "0.0.0.0",
                        onToggle = {
                            if (connectedServer != null) {
                                onDisconnect()
                            } else if (servers.isNotEmpty()) {
                                onConnect(servers.first())
                            }
                        }
                    )
                }

                // Right Column: Lists
                LazyColumn(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recentConnections.isNotEmpty()) {
                        item {
                            Text(
                                "Recent Connections",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textNorm,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(recentConnections) { server ->
                            ServerCard(server, onClick = { onConnect(server) }, displayMode = loadDisplayMode)
                        }
                    } else if (servers.isNotEmpty()) {
                        item {
                            Text(
                                "Recommended",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textNorm,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(servers.take(10)) { server ->
                            ServerCard(server, onClick = { onConnect(server) }, displayMode = loadDisplayMode)
                        }
                    }
                }
            }
        } else {
            // Phone Layout
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.backgroundSecondary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Map,
                            null,
                            modifier = Modifier.size(120.dp),
                            tint = colors.brandNorm.copy(alpha = 0.2f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    DesktopConnectionCard(
                        isConnected = connectedServer != null,
                        isConnecting = isConnecting,
                        serverName = connectedServer?.name ?: "Quick Connect",
                        countryName = connectedServer?.country ?: "Select Location",
                        ipAddress = if (connectedServer != null) "10.2.0.2" else "0.0.0.0",
                        onToggle = {
                            if (connectedServer != null) {
                                onDisconnect()
                            } else if (servers.isNotEmpty()) {
                                onConnect(servers.first())
                            }
                        }
                    )
                }

                if (recentConnections.isNotEmpty()) {
                    item {
                        Text(
                            "Recent Connections",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textNorm
                        )
                    }
                    items(recentConnections) { server ->
                        ServerCard(server, onClick = { onConnect(server) }, displayMode = loadDisplayMode)
                    }
                } else if (servers.isNotEmpty()) {
                    item {
                        Text(
                            "Recommended",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textNorm
                        )
                    }
                    items(servers.take(3)) { server ->
                        ServerCard(server, onClick = { onConnect(server) }, displayMode = loadDisplayMode)
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(server: ServerEntry, onClick: () -> Unit, displayMode: ServerLoadDisplayMode = ServerLoadDisplayMode.ALL) {
    val colors = Theme.colors
    val load = server.physicalServer?.load ?: 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundSecondary.copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.shade100.copy(alpha = 0.05f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlagIcon(countryCode = server.country, size = DpSize(36.dp, 24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(server.country, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(server.name, style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
                }
                LoadIndicator(load = load, displayMode = displayMode)
            }
            LoadProgressBar(load = load, displayMode = displayMode)
        }
    }
}
