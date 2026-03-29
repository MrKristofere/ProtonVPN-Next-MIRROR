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
import ru.protonmod.next.desktop.data.*
import ru.protonmod.next.desktop.ui.components.*
import ru.protonmod.next.desktop.ui.utils.*
import ru.protonmod.next.ui.utils.CommonCountryUtils
import ru.protonmod.next.ui.theme.ProtonNextTheme as Theme
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
            else if (settings.accessToken == null) MainTarget.Welcome 
            else MainTarget.Home 
        ) 
    }

    // Auto-login if session exists
    LaunchedEffect(settings.accessToken) {
        if (settings.accessToken != null && uiState is DesktopLoginUiState.Idle) {
            viewModel.restoreSession(settings.accessToken!!, settings.sessionId!!)
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

    // Persist session on success
    LaunchedEffect(uiState) {
        if (uiState is DesktopLoginUiState.Success) {
            val state = uiState as DesktopLoginUiState.Success
            settingsManager.saveSession(state.accessToken, state.sessionId)
        }
    }

    val countriesViewModel = remember(servers, connectedServer) {
        DesktopCountriesViewModel(vpnClient, settingsManager, viewModel.servers, viewModel.connectedServer)
    }

    BoxWithConstraints {
        val windowWidth = maxWidth
        ProvideDeviceType(windowWidth) {
            Theme(appTheme = settings.appTheme) {
                val colors = Theme.colors
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
                                        settingsManager.clearSession()
                                        authTarget = MainTarget.Welcome
                                    },
                                    onConnect = { server ->
                                        viewModel.connectToServer(server)
                                    },
                                    onDisconnect = {
                                        viewModel.disconnect()
                                    },
                                    certificateState = certificateState,
                                    onRefreshCert = { viewModel.refreshCertificate() },
                                    settingsManager = settingsManager,
                                    loadDisplayMode = settings.serverLoadDisplayMode,
                                    countriesViewModel = countriesViewModel,
                                    dataManager = dataManager
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
    servers: List<ServerEntry>,
    recentConnections: List<ServerEntry>,
    connectedServer: ServerEntry?,
    isConnecting: Boolean,
    selectedTarget: MainTarget,
    onTargetSelected: (MainTarget) -> Unit,
    onLogout: () -> Unit,
    onConnect: (ServerEntry) -> Unit,
    onDisconnect: () -> Unit,
    certificateState: ru.protonmod.next.desktop.data.local.CertificateState,
    onRefreshCert: () -> Unit,
    settingsManager: DesktopSettingsManager,
    loadDisplayMode: ServerLoadDisplayMode,
    countriesViewModel: DesktopCountriesViewModel,
    dataManager: DesktopVpnDataManager
) {
    val isTablet = isTablet()

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
                        onDisconnect = onDisconnect,
                        certificateState = certificateState,
                        onRefreshCert = onRefreshCert,
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
                MainTarget.Profiles -> {
                    ProfilesScreen()
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
    certificateState: ru.protonmod.next.desktop.data.local.CertificateState,
    onRefreshCert: () -> Unit,
    isTablet: Boolean,
    loadDisplayMode: ServerLoadDisplayMode
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

        if (isTablet) {
            // Tablet Layout: Split connection (Left) and recent connections (Right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, start = 24.dp, end = 24.dp),
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
                        serverName = connectedServer?.name ?: Strings.btn_quick_connect(),
                        countryCode = connectedServer?.country ?: "",
                        cityName = connectedServer?.city ?: "",
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
                contentPadding = PaddingValues(top = 80.dp)
            ) {
                item {
                    CertificateBanner(
                        state = certificateState,
                        onRefresh = onRefreshCert,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                    )
                }

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
                        serverName = connectedServer?.name ?: Strings.btn_quick_connect(),
                        countryCode = connectedServer?.country ?: "",
                        cityName = connectedServer?.city ?: "",
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
                    val countryName = CommonCountryUtils.getCountryName(server.country).ifBlank { server.country }
                    Text(countryName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(server.name, style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
                }
                LoadIndicator(load = load, displayMode = displayMode)
            }
            LoadProgressBar(load = load, displayMode = displayMode)
        }
    }
}
