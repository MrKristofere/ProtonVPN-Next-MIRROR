package ru.protonmod.next.desktop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.ui.MainTarget
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.utils.CommonCountryUtils

@Composable
fun LiquidGlassBottomBar(
    selectedTarget: MainTarget?,
    navigateTo: (MainTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ProtonNextTheme.colors
    val glassShape = RoundedCornerShape(32.dp)
    val glassBackgroundColor = colors.backgroundNorm.copy(alpha = 0.85f)

    val glassBorderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.2f),
            Color.White.copy(alpha = 0.05f)
        )
    )

    Box(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .shadow(15.dp, glassShape)
            .clip(glassShape)
            .background(glassBackgroundColor)
            .border(1.dp, glassBorderBrush, glassShape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(MainTarget.Home, MainTarget.Countries, MainTarget.Profiles, MainTarget.Settings).forEach { target ->
                val isSelected = target == selectedTarget
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) colors.brandNorm else colors.iconWeak,
                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { navigateTo(target) }
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier.size(40.dp)
                                .background(iconColor.copy(alpha = 0.15f), CircleShape)
                        )
                    }
                    Icon(
                        imageVector = getIconForTarget(target),
                        contentDescription = target.name,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun getIconForTarget(target: MainTarget): ImageVector = when (target) {
    MainTarget.Home -> Icons.Rounded.Home
    MainTarget.Profiles -> Icons.Rounded.Terminal
    MainTarget.Countries -> Icons.Rounded.Public
    MainTarget.Settings -> Icons.Rounded.Settings
    else -> Icons.Rounded.Settings
}

@Composable
fun DesktopConnectionCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    serverName: String,
    countryCode: String,
    cityName: String = "",
    ipAddress: String,
    onToggle: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val countryName = CommonCountryUtils.getCountryName(countryCode).ifBlank { countryCode }
    val displayLocation = if (cityName.isNotEmpty()) "$countryName, $cityName" else countryName
    
    val cardContainerColor = when {
        isConnected -> colors.notificationSuccess.copy(alpha = 0.18f)
        isConnecting -> colors.backgroundSecondary
        else -> colors.backgroundSecondary.copy(alpha = 0.92f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isConnected) colors.notificationSuccess.copy(alpha = 0.25f)
            else colors.shade100.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isConnected) "CONNECTED" else if (isConnecting) "CONNECTING..." else "NOT CONNECTED",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isConnected) colors.notificationSuccess else colors.textNorm.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    color = colors.backgroundSecondary.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "$displayLocation • $ipAddress",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textWeak
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp, 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.backgroundNorm),
                    contentAlignment = Alignment.Center
                ) {
                    FlagIcon(countryCode = countryCode, size = DpSize(48.dp, 32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(displayLocation, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(serverName, style = MaterialTheme.typography.bodyMedium, color = colors.textWeak)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) colors.shade20 else colors.brandNorm,
                    contentColor = if (isConnected) colors.textNorm else colors.textInverted
                ),
                enabled = !isConnecting
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.textInverted, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (isConnected) "Disconnect" else "Quick Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
