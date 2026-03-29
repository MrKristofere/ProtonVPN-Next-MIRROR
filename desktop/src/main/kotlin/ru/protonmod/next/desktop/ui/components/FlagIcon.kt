/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.protonmod.next.ui.theme.ProtonNextTheme

@Composable
fun FlagIcon(
    countryCode: String?,
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(30.dp, 20.dp)
) {
    val colors = ProtonNextTheme.colors
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (countryCode == null || countryCode.isBlank() || countryCode.length != 2) {
            // Fallback for missing/invalid code
            Box(
                modifier = Modifier.fillMaxSize().background(colors.backgroundNorm),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = null,
                    tint = colors.iconWeak,
                    modifier = Modifier.size(size.height * 0.8f)
                )
            }
            return@Box
        }

        val normalizedCode = when (val code = countryCode.lowercase()) {
            "uk" -> "gb"
            "fastest" -> "fastest"
            else -> code
        }
        val resourcePath = "drawable/flag_$normalizedCode.xml"

        // On Desktop we can check resource existence via ClassLoader
        val resourceExists = object {}.javaClass.classLoader.getResource(resourcePath) != null

        if (resourceExists) {
            Image(
                painter = painterResource(resourcePath),
                contentDescription = countryCode,
                modifier = Modifier.size(size)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(colors.backgroundNorm),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = null,
                    tint = colors.iconWeak,
                    modifier = Modifier.size(size.height * 0.8f)
                )
            }
        }
    }
}
