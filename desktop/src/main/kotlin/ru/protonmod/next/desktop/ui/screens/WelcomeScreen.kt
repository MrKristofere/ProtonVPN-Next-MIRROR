/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.protonmod.next.desktop.DesktopLoginUiState
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.desktop.ui.utils.isTablet
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings

@Composable
fun WelcomeScreen(
    uiState: DesktopLoginUiState,
    onLogin: (String, String) -> Unit,
    onGuest: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val isTablet = isTablet()

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    if (isTablet) {
        WelcomeTabletContent(
            isVisible = isVisible,
            uiState = uiState,
            onLoginAnonymous = onGuest,
            onNavigateToLogin = onNavigateToLogin
        )
    } else {
        WelcomePhoneContent(
            isVisible = isVisible,
            uiState = uiState,
            onLoginAnonymous = onGuest,
            onNavigateToLogin = onNavigateToLogin
        )
    }
}

@Composable
private fun WelcomePhoneContent(
    isVisible: Boolean,
    uiState: DesktopLoginUiState,
    onLoginAnonymous: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource("drawable/vpn_welcome_globe.webp"),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.9f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(tween(800, delayMillis = 200)) { it / 4 }
            ) {
                Text(
                    text = Strings.welcome_title(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textNorm,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 400)) + slideInVertically(tween(800, delayMillis = 400)) { it / 4 }
            ) {
                Text(
                    text = Strings.welcome_subtitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textWeak,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 600)) + slideInVertically(tween(800, delayMillis = 600)) { it / 4 }
            ) {
                WelcomeButtons(
                    uiState = uiState,
                    onLoginAnonymous = onLoginAnonymous,
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }
    }
}

@Composable
private fun WelcomeTabletContent(
    isVisible: Boolean,
    uiState: DesktopLoginUiState,
    onLoginAnonymous: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource("drawable/vpn_welcome_globe.webp"),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.8f)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(tween(800, delayMillis = 200)) { it / 4 }
            ) {
                Text(
                    text = Strings.welcome_title(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textNorm
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 400)) + slideInVertically(tween(800, delayMillis = 400)) { it / 4 }
            ) {
                Text(
                    text = Strings.welcome_subtitle(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textWeak
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 600)) + slideInVertically(tween(800, delayMillis = 600)) { it / 4 }
            ) {
                Box(modifier = Modifier.widthIn(max = 400.dp)) {
                    WelcomeButtons(
                        uiState = uiState,
                        onLoginAnonymous = onLoginAnonymous,
                        onNavigateToLogin = onNavigateToLogin
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WelcomeButtons(
    uiState: DesktopLoginUiState,
    onLoginAnonymous: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val isLoading = uiState is DesktopLoginUiState.Loading
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onLoginAnonymous,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brandNorm, contentColor = colors.textInverted)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.textInverted, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            } else {
                Text(text = Strings.btn_continue_guest(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = { /* TODO: Register */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = colors.interactionNorm, contentColor = colors.textInverted)
        ) {
            Text(text = Strings.btn_create_account(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textNorm),
            border = BorderStroke(1.dp, colors.separatorNorm)
        ) {
            Text(text = Strings.btn_login(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        if (uiState is DesktopLoginUiState.Error) {
            Text(
                text = uiState.message,
                color = colors.notificationError,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
