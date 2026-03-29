/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.ui.utils

import androidx.compose.ui.graphics.Color

object SharedCountryUtils {
    /**
     * Returns a color based on the load:
     * 0-60% -> Green (Low)
     * 60-85% -> Yellow (Medium)
     * 85-100% -> Red (High)
     */
    fun getColorForLoad(load: Int): Color {
        return when {
            load < 60 -> Color(0xFF007B58) // Apple
            load < 85 -> Color(0xFFE65200) // Sunglow
            else -> Color(0xFFCC2D4F) // Pomegranate
        }
    }
}
