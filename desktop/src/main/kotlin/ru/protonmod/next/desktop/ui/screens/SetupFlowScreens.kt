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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.DesktopLoginUiState
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings

@Composable
fun SetupLanguageScreen(
    settingsManager: DesktopSettingsManager,
    onNext: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val settings by settingsManager.settings.collectAsState()
    var showLanguagePicker by remember { mutableStateOf(false) }

    SetupStepContainer(
        title = "Choose Language",
        subtitle = "Select your preferred language to continue.",
        onNext = onNext,
        nextButtonText = "Continue"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.3f)
                .clickable { showLanguagePicker = true }
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Language, null, tint = colors.brandNorm, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text("Current Language", style = MaterialTheme.typography.labelSmall, color = colors.textWeak)
                    Text(getLanguageName(settings.language), style = MaterialTheme.typography.headlineSmall, color = colors.textNorm)
                }
            }
        }
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentLanguage = settings.language,
            onDismiss = { showLanguagePicker = false },
            onSelect = { code ->
                settingsManager.setLanguage(code)
                showLanguagePicker = false
            }
        )
    }
}

@Composable
fun SetupObfuscationScreen(
    settingsManager: DesktopSettingsManager,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val settings by settingsManager.settings.collectAsState()
    val colors = ProtonNextTheme.colors

    SetupStepContainer(
        title = "VPN Obfuscation",
        subtitle = "Protect your connection from censorship and deep packet inspection.",
        onNext = onNext,
        onBack = onBack,
        nextButtonText = "Continue"
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(24.dp),
                    alpha = if (settings.obfuscationEnabled) 0.3f else 0.5f
                )
                .clickable { settingsManager.setObfuscationEnabled(!settings.obfuscationEnabled) }
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, tint = colors.brandNorm, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable AmneziaWG", style = MaterialTheme.typography.headlineSmall, color = colors.textNorm)
                    Text("Advanced obfuscation to bypass firewalls.", style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
                }
                Switch(
                    checked = settings.obfuscationEnabled,
                    onCheckedChange = { settingsManager.setObfuscationEnabled(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.brandNorm)
                )
            }
        }
    }
}

@Composable
fun SetupAuthScreen(
    uiState: DesktopLoginUiState,
    onLogin: (String, String) -> Unit,
    onGuest: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    SetupStepContainer(
        title = "Secure Access",
        subtitle = "Sign in to your account or continue as a guest.",
        onNext = null, // No "Next" button, use WelcomeButtons
        onBack = onBack
    ) {
        WelcomeButtons(
            uiState = uiState,
            onLoginAnonymous = onGuest,
            onNavigateToLogin = onNavigateToLogin
        )
    }
}

@Composable
private fun SetupStepContainer(
    title: String,
    subtitle: String,
    onNext: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
    nextButtonText: String = "Next",
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ProtonNextTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource("drawable/vpn_welcome_globe.webp"),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = colors.textNorm,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textWeak,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Box(modifier = Modifier.widthIn(max = 480.dp).padding(horizontal = 24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
                
                if (onNext != null || onBack != null) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (onBack != null) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.separatorNorm)
                            ) {
                                Text("Back", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (onNext != null) {
                            Button(
                                onClick = onNext,
                                modifier = Modifier.weight(if (onBack != null) 1.5f else 1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.brandNorm)
                            ) {
                                Text(nextButtonText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
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
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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

        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textNorm),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.separatorNorm)
        ) {
            Text(text = Strings.btn_login(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = ProtonNextTheme.colors
    val languages = listOf(
        "en" to "English",
        "ru" to "Русский",
        "uk" to "Українська",
        "be" to "Беларуская",
        "fa" to "فарсі",
        "zh" to "中文"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language", color = colors.textNorm) },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = code == currentLanguage,
                            onClick = { onSelect(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.brandNorm)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(name, color = colors.textNorm)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.brandNorm)
            }
        },
        containerColor = colors.backgroundSecondary
    )
}

private fun getLanguageName(code: String): String = when (code) {
    "en" -> "English"
    "ru" -> "Русский"
    "uk" -> "Українська"
    "be" -> "Беларуская"
    "fa" -> "فарсі"
    "zh" -> "中文"
    else -> code
}
