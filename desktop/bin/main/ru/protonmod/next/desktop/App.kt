@file:OptIn(ExperimentalMaterial3Api::class)

package ru.protonmod.next.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.rounded.Logout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import ru.protonmod.next.desktop.ui.MainTarget
import ru.protonmod.next.desktop.ui.components.DesktopConnectionCard
import ru.protonmod.next.desktop.ui.components.LiquidGlassBottomBar
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.ProtonNextTheme as Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val authClient = remember { DesktopAuthClient() }
    val vpnClient = remember { DesktopVpnClient() }
    val viewModel = remember { DesktopLoginViewModel(authClient, vpnClient) }

    val uiState by viewModel.uiState.collectAsState()
    val servers by viewModel.servers.collectAsState()

    var showCaptcha by remember { mutableStateOf(false) }
    var captchaState by remember { mutableStateOf<DesktopLoginUiState.RequiresCaptcha?>(null) }
    var selectedTarget by remember { mutableStateOf(MainTarget.Home) }

    Theme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                is DesktopLoginUiState.Idle, is DesktopLoginUiState.Loading, is DesktopLoginUiState.Error -> {
                    WelcomeContent(
                        uiState = uiState,
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
                        selectedTarget = selectedTarget,
                        onTargetSelected = { selectedTarget = it },
                        onLogout = { viewModel.clearError() }
                    )
                }
            }
            // ... captcha dialog ...

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

@Composable
private fun WelcomeContent(
    uiState: DesktopLoginUiState,
    onGuest: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit
) {
    val isLoading = uiState is DesktopLoginUiState.Loading
    val errorMessage = (uiState as? DesktopLoginUiState.Error)?.message
    val colors = Theme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x6611D8CC), Color(0x006E4BFF))
                )
            )
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
            text = "Secure VPN access on desktop. Guest login works without the mobile libraries.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textWeak,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onGuest() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brandNorm,
                contentColor = colors.textInverted
            )
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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tip: If the captcha fails, try again.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textWeak.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DashboardScreen(
    servers: List<ServerEntry>,
    selectedTarget: MainTarget,
    onTargetSelected: (MainTarget) -> Unit,
    onLogout: () -> Unit
) {
    val colors = Theme.colors
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(colors.backgroundNorm)) {
        // Simple Top Bar for desktop
        TopAppBar(
            title = { Text("Proton VPN", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Logout, "Logout", tint = colors.interactionNorm)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp)
        ) {
            item {
                DesktopConnectionCard(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    serverName = if (isConnected) servers.firstOrNull()?.name ?: "Unknown" else "Quick Connect",
                    countryName = if (isConnected) servers.firstOrNull()?.country ?: "Unknown" else "Select Location",
                    ipAddress = if (isConnected) "1.2.3.4" else "0.0.0.0",
                    onToggle = {
                        if (isConnected) {
                            isConnected = false
                        } else {
                            scope.launch {
                                isConnecting = true
                                delay(1500)
                                isConnecting = false
                                isConnected = true
                            }
                        }
                    }
                )
            }

            if (selectedTarget == MainTarget.Home && servers.isNotEmpty()) {
                item {
                    Text(
                        "Recent Connections",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(servers.take(5)) { server ->
                    ServerCard(server, onClick = { /* TODO: Connect */ })
                }
            } else if (selectedTarget == MainTarget.Countries) {
                items(servers) { server ->
                    ServerCard(server, onClick = { /* TODO: Connect */ })
                }
            }
        }

        LiquidGlassBottomBar(
            selectedTarget = selectedTarget,
            navigateTo = onTargetSelected,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ServerCard(server: ServerEntry, onClick: () -> Unit) {
    val colors = Theme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.backgroundSecondary.copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.shade100.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp, 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.backgroundNorm),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Public, null, modifier = Modifier.size(20.dp), tint = colors.iconNorm)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(server.country, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(server.name, style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
            }
        }
    }
}

@Composable
private fun ServerListContent(servers: List<ServerEntry>, onLogout: () -> Unit) {
    val colors = Theme.colors
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Available Servers") },
            actions = {
                Text(
                    "Logout",
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clickable { onLogout() },
                    color = colors.interactionNorm
                )
            }
        )
        if (servers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading servers…")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                items(servers) { server ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(server.name, style = MaterialTheme.typography.titleMedium)
                            Text("${server.city}, ${server.country}", style = MaterialTheme.typography.bodySmall)
                            Text("Tier: ${server.tier}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

