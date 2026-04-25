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

@file:OptIn(ExperimentalMaterial3Api::class)

package ru.protonmod.next.desktop

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.ui.MainTarget
import ru.protonmod.next.desktop.ui.components.DesktopConnectionCard
import ru.protonmod.next.desktop.ui.components.LiquidGlassBottomBar
import ru.protonmod.next.desktop.ui.screens.*
import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.data.network.LogicalServer
import ru.protonmod.next.desktop.data.*
import ru.protonmod.next.desktop.ui.components.*
import ru.protonmod.next.desktop.ui.utils.*
import ru.protonmod.next.ui.utils.CommonCountryUtils
import ru.protonmod.next.ui.theme.ProtonNextTheme as Theme
import ru.protonmod.next.ui.theme.liquidGlass
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings

@Composable
fun App() {
    val dataManager = remember { DesktopVpnDataManager() }
    
    // Initialize data layer on start
    LaunchedEffect(Unit) {
        dataManager.initialize()
    }

    val settingsManager = dataManager.settingsManager
    val authClient = remember { DesktopAuthClient(dataManager.sentryManager) }
    val vpnClient = remember { 
        DesktopVpnClient(
            database = dataManager.database,
            vpnRepository = dataManager.vpnRepository,
            certificateManager = dataManager.certificateManager,
            settingsManager = settingsManager,
            splitTunnelingManager = dataManager.splitTunnelingManager,
            dnsManager = dataManager.dnsManager,
            sentryManager = dataManager.sentryManager
        ) 
    }
    val viewModel = remember { 
        DesktopLoginViewModel(
            authClient = authClient,
            vpnClient = vpnClient,
            database = dataManager.database,
            vpnRepository = dataManager.vpnRepository,
            certificateManager = dataManager.certificateManager,
            sentryManager = dataManager.sentryManager
        ) 
    }

    val uiState by viewModel.uiState.collectAsState()
    val certificateState by viewModel.certificateState.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val recentConnections by viewModel.recentConnections.collectAsState()
    val connectedServer by viewModel.connectedServer.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()

    var showCaptcha by remember { mutableStateOf(false) }
    var captchaState by remember { mutableStateOf<DesktopLoginUiState.RequiresCaptcha?>(null) }
    
    val settings by settingsManager.settings.collectAsState()
    var selectedTarget by remember { mutableStateOf(MainTarget.Home) }
    var authTarget by remember { 
        mutableStateOf(
            if (settings.isFirstRun) MainTarget.SetupLanguage 
            else if (settings.isLoggedIn || settings.accessToken != null) MainTarget.Home
            else MainTarget.Welcome 
        ) 
    }

    // Initialize data layer and restore session on start
    LaunchedEffect(Unit) {
        dataManager.initialize()
        
        // Restore session from secure database if it exists
        val session = dataManager.database.getSession()
        if (session != null && uiState is DesktopLoginUiState.Idle) {
            viewModel.restoreSession(session.accessToken, session.sessionId)
            authTarget = MainTarget.Home
        }
    }

    // Handle Captcha
    LaunchedEffect(uiState) {
        if (uiState is DesktopLoginUiState.RequiresCaptcha) {
            val state = uiState as DesktopLoginUiState.RequiresCaptcha
            captchaState = state
            showCaptcha = true
        }
    }

    // Restore session on success
    LaunchedEffect(uiState) {
        if (uiState is DesktopLoginUiState.Success) {
            val state = uiState as DesktopLoginUiState.Success
            // Update the login flag in settings (tokens are already in database)
            settingsManager.saveSession(state.accessToken, state.sessionId)
        }
    }

    val countriesViewModel = remember(servers, connectedServer) {
        DesktopCountriesViewModel(vpnClient, settingsManager, viewModel.servers, viewModel.connectedServer)
    }

    val profilesViewModel = remember(servers, connectedServer) {
        DesktopProfilesViewModel(vpnClient, settingsManager, viewModel.servers, viewModel.connectedServer)
    }

    BoxWithConstraints {
        val windowWidth = maxWidth
        ProvideDeviceType(windowWidth) {
            Theme(appTheme = settings.appTheme) {
                val colors = Theme.colors
                Surface(color = colors.backgroundNorm) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Immersive gradient background
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
                            targetState = if (uiState is DesktopLoginUiState.Success && !settings.isFirstRun) 1 else 0,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                            },
                            label = "root_auth_transition"
                        ) { target ->
                            if (target == 1) {
                                DashboardScreen(
                                    servers = servers,
                                    recentConnections = recentConnections,
                                    connectedServer = connectedServer,
                                    isConnecting = isConnecting,
                                    selectedTarget = selectedTarget,
                                    onTargetSelected = { selectedTarget = it },
                                    onLogout = { 
                                        viewModel.clearError() 
                                        viewModel.logout()
                                        settingsManager.clearSession()
                                        authTarget = MainTarget.Welcome
                                    },
                                    onConnect = { server ->
                                        viewModel.connectToServer(server)
                                    },
                                    onQuickConnect = { strategy, targetId ->
                                        viewModel.quickConnect(strategy, targetId)
                                    },
                                    onDisconnect = {
                                        viewModel.disconnect()
                                    },
                                    certificateState = certificateState,
                                    onRefreshCert = { viewModel.refreshCertificate() },
                                    settingsManager = settingsManager,
                                    loadDisplayMode = settings.serverLoadDisplayMode,
                                    countriesViewModel = countriesViewModel,
                                    profilesViewModel = profilesViewModel,
                                    dataManager = dataManager,
                                    viewModel = viewModel
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AnimatedContent(
                                        targetState = authTarget,
                                        transitionSpec = {
                                            if (targetState.ordinal > initialState.ordinal) {
                                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                                    slideOutHorizontally { width -> -width } + fadeOut())
                                            } else {
                                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                                    slideOutHorizontally { width -> width } + fadeOut())
                                            }
                                        },
                                        label = "auth_flow_transition"
                                    ) { currentAuthTarget ->
                                        when (currentAuthTarget) {
                                            MainTarget.SetupLanguage -> {
                                                SetupLanguageScreen(
                                                    settingsManager = settingsManager,
                                                    onNext = { authTarget = MainTarget.SetupObfuscation }
                                                )
                                            }
                                            MainTarget.SetupObfuscation -> {
                                                SetupObfuscationScreen(
                                                    settingsManager = settingsManager,
                                                    onNext = { authTarget = MainTarget.Welcome },
                                                    onBack = { authTarget = MainTarget.SetupLanguage }
                                                )
                                            }
                                            MainTarget.Welcome -> {
                                                SetupAuthScreen(
                                                    uiState = uiState,
                                                    onLogin = { u, p -> viewModel.login(u, p) },
                                                    onGuest = { viewModel.loginAnonymous() },
                                                    onNavigateToLogin = { authTarget = MainTarget.Login },
                                                    onBack = { if (settings.isFirstRun) authTarget = MainTarget.SetupObfuscation else authTarget = MainTarget.Welcome }
                                                )
                                            }
                                            MainTarget.Login -> {
                                                LoginScreen(
                                                    uiState = uiState,
                                                    onBackClick = { authTarget = MainTarget.Welcome },
                                                    onLogin = { u, p -> viewModel.login(u, p) }
                                                )
                                            }
                                            MainTarget.Onboarding -> {
                                                OnboardingScreen(
                                                    onComplete = {
                                                        settingsManager.setFirstRunComplete()
                                                        authTarget = MainTarget.Home
                                                    }
                                                )
                                            }
                                            else -> {}
                                        }
                                    }
                                }

                                if (uiState is DesktopLoginUiState.Success && settings.isFirstRun) {
                                    authTarget = MainTarget.Onboarding
                                }
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
private fun DashboardScreen(
    servers: List<LogicalServer>,
    recentConnections: List<LogicalServer>,
    connectedServer: LogicalServer?,
    isConnecting: Boolean,
    selectedTarget: MainTarget,
    onTargetSelected: (MainTarget) -> Unit,
    onLogout: () -> Unit,
    onConnect: (LogicalServer) -> Unit,
    onQuickConnect: (String, String?) -> Unit,
    onDisconnect: () -> Unit,
    certificateState: ru.protonmod.next.desktop.data.local.CertificateState,
    onRefreshCert: () -> Unit,
    settingsManager: DesktopSettingsManager,
    loadDisplayMode: ServerLoadDisplayMode,
    countriesViewModel: DesktopCountriesViewModel,
    profilesViewModel: DesktopProfilesViewModel,
    dataManager: DesktopVpnDataManager,
    viewModel: DesktopLoginViewModel
) {
    val isTablet = isTablet()
    val settings by settingsManager.settings.collectAsState()
    var showQuickConnectConfig by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedTarget,
            transitionSpec = {
                if (targetState == MainTarget.Home || initialState == MainTarget.Settings) {
                    (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width } + fadeOut())
                } else {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut())
                }
            },
            label = "dashboard_nav_transition"
        ) { target ->
            when (target) {
                MainTarget.Home -> {
                    HomeScreen(
                        servers = servers,
                        recentConnections = recentConnections,
                        connectedServer = connectedServer,
                        isConnecting = isConnecting,
                        onLogout = onLogout,
                        onConnect = onConnect,
                        onQuickConnect = { 
                            onQuickConnect(settings.quickConnectStrategy, settings.quickConnectTargetId)
                        },
                        onChangeQuickConnect = { showQuickConnectConfig = true },
                        onDisconnect = onDisconnect,
                        certificateState = certificateState,
                        onRefreshCert = onRefreshCert,
                        isTablet = isTablet,
                        loadDisplayMode = loadDisplayMode,
                        quickConnectStrategy = settings.quickConnectStrategy,
                        quickConnectTargetId = settings.quickConnectTargetId
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
                        onProtocolSelected = { /* Already AmneziaWG */ },
                        onNavigateToObfuscation = { onTargetSelected(MainTarget.ObfuscationSettings) }
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
                MainTarget.SplitTunneling -> {
                    SplitTunnelingScreen(
                        onBack = { onTargetSelected(MainTarget.Settings) },
                        manager = dataManager.splitTunnelingManager
                    )
                }
                MainTarget.CustomDns -> {
                    CustomDnsScreen(
                        onBack = { onTargetSelected(MainTarget.Settings) },
                        manager = dataManager.dnsManager
                    )
                }
                MainTarget.ErrorReporting -> {
                    ErrorReportingScreen(
                        onBack = { onTargetSelected(MainTarget.Settings) },
                        settingsManager = settingsManager
                    )
                }
                MainTarget.Debug -> {
                    DebugSettingsScreen(
                        onBack = { onTargetSelected(MainTarget.Settings) },
                        viewModel = viewModel,
                        dataManager = dataManager
                    )
                }
                MainTarget.Profiles -> {
                    ProfilesScreen(
                        viewModel = profilesViewModel,
                        onConnect = onConnect
                    )
                }
                else -> {}
            }
        }

        LiquidGlassBottomBar(
            selectedTarget = selectedTarget,
            navigateTo = onTargetSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = if (isTablet) 400.dp else 600.dp)
                .padding(bottom = 24.dp)
        )

        if (showQuickConnectConfig) {
            QuickConnectBottomSheet(
                onDismiss = { showQuickConnectConfig = false },
                currentStrategy = settings.quickConnectStrategy,
                currentTargetId = settings.quickConnectTargetId,
                recentServers = recentConnections,
                onStrategySelect = { strategy, targetId ->
                    settingsManager.setQuickConnectStrategy(strategy, targetId)
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    servers: List<LogicalServer>,
    recentConnections: List<LogicalServer>,
    connectedServer: LogicalServer?,
    isConnecting: Boolean,
    onLogout: () -> Unit,
    onConnect: (LogicalServer) -> Unit,
    onQuickConnect: () -> Unit,
    onChangeQuickConnect: () -> Unit,
    onDisconnect: () -> Unit,
    certificateState: ru.protonmod.next.desktop.data.local.CertificateState,
    onRefreshCert: () -> Unit,
    isTablet: Boolean,
    loadDisplayMode: ServerLoadDisplayMode,
    quickConnectStrategy: String,
    quickConnectTargetId: String?
) {
    val colors = Theme.colors
    
    Box(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(Strings.app_name(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
            actions = {
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, "Logout", tint = colors.interactionNorm)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        // Desktop always uses split layout (tablet-like)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Left Column: Status and Connection
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CertificateBanner(
                    state = certificateState,
                    onRefresh = onRefreshCert
                )

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
                    serverName = connectedServer?.name ?: getQuickConnectName(quickConnectStrategy, quickConnectTargetId, servers),
                    countryCode = connectedServer?.exitCountry ?: getQuickConnectCountry(quickConnectStrategy, quickConnectTargetId, servers),
                    cityName = connectedServer?.city ?: "",
                    ipAddress = if (connectedServer != null) "10.2.0.2" else "0.0.0.0",
                    onToggle = {
                        if (connectedServer != null) {
                            onDisconnect()
                        } else {
                            onQuickConnect()
                        }
                    },
                    onChangeStrategy = onChangeQuickConnect,
                    quickConnectStrategy = quickConnectStrategy
                )
            }

            // Right Column: Lists
            LazyColumn(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (recentConnections.isNotEmpty()) {
                    item {
                        Text(
                            Strings.get("title_recent_connections"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textNorm,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(recentConnections) { server ->
                        ServerCard(server, isConnected = connectedServer?.id == server.id, isConnecting = isConnecting && connectedServer?.id == server.id, onClick = { onConnect(server) }, displayMode = loadDisplayMode)
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
                        ServerCard(server, isConnected = connectedServer?.id == server.id, isConnecting = isConnecting && connectedServer?.id == server.id, onClick = { onConnect(server) }, displayMode = loadDisplayMode)
                    }
                }
            }
        }
    }
}

@Composable
private fun getQuickConnectName(strategy: String, targetId: String?, servers: List<LogicalServer>): String {
    return when (strategy) {
        "fastest" -> Strings.qc_fastest()
        "recent" -> Strings.qc_recent()
        "server" -> servers.find { it.id == targetId }?.name ?: Strings.btn_quick_connect()
        else -> Strings.btn_quick_connect()
    }
}

private fun getQuickConnectCountry(strategy: String, targetId: String?, servers: List<LogicalServer>): String {
    return when (strategy) {
        "server" -> servers.find { it.id == targetId }?.exitCountry ?: ""
        "fastest", "recent" -> "fastest"
        else -> ""
    }
}

@Composable
private fun ServerCard(
    server: LogicalServer, 
    isConnected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit, 
    displayMode: ServerLoadDisplayMode = ServerLoadDisplayMode.ALL
) {
    val colors = Theme.colors
    val load = server.averageLoad
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                alpha = if (isConnected) 0.3f else 0.4f,
                shadowElevation = 0.dp
            )
            .clickable(enabled = !isConnecting) { onClick() }
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp, 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colors.brandNorm,
                            strokeWidth = 2.dp
                        )
                    } else {
                        FlagIcon(countryCode = server.exitCountry, size = DpSize(36.dp, 24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val countryName = CommonCountryUtils.getCountryName(server.exitCountry).ifBlank { server.exitCountry }
                    Text(countryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textNorm)
                    Text(server.name, style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
                }
                LoadIndicator(load = load, displayMode = displayMode)
            }
            LoadProgressBar(load = load, displayMode = displayMode)
        }
    }
}
