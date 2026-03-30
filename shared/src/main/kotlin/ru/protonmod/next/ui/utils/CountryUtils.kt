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
