/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun FlagIcon(
    countryCode: String,
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(30.dp, 20.dp)
) {
    val normalizedCode = when (val code = countryCode.lowercase()) {
        "uk" -> "gb"
        else -> code
    }
    val resourcePath = "drawable/flag_$normalizedCode.xml"

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Compose Desktop loads resources from classpath. 
        // We use painterResource which handles XML vectors on desktop.
        Image(
            painter = painterResource(resourcePath),
            contentDescription = countryCode,
            modifier = Modifier.size(size)
        )
    }
}
