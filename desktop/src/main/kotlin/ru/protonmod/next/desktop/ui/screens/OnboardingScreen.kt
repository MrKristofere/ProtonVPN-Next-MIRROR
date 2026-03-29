/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val colors = ProtonNextTheme.colors

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.widthIn(min = 200.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.brandNorm)
                ) {
                    Text("Get Started", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Ready to go!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textNorm,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                OnboardingItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Security,
                    title = "Strong Encryption",
                    description = "Your data is protected by military-grade AES-256 or ChaCha20 encryption."
                )
                Spacer(modifier = Modifier.width(24.dp))
                OnboardingItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Public,
                    title = "Global Network",
                    description = "Access high-speed servers in over 60 countries around the world."
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                OnboardingItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.FlashOn,
                    title = "VPN Accelerator",
                    description = "Increase VPN speeds by over 400% with our unique technology."
                )
                Spacer(modifier = Modifier.width(24.dp))
                OnboardingItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Shield,
                    title = "AmneziaWG",
                    description = "Advanced obfuscation protocol to bypass censorship and restrictive firewalls."
                )
            }
        }
    }
}

@Composable
private fun OnboardingItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = ProtonNextTheme.colors
    Box(
        modifier = modifier
            .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.3f)
            .padding(20.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.brandNorm,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textNorm
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textWeak
            )
        }
    }
}
