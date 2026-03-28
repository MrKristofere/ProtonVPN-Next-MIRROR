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

package ru.protonmod.next.ui.utils

import java.util.Locale

object CommonCountryUtils {

    /**
     * Generates an Emoji flag from an ISO country code (e.g., "US" -> 🇺🇸)
     */
    fun getFlagForCountry(countryCode: String?): String {
        if (countryCode == null || countryCode.length != 2) return "🌍"
        
        val code = countryCode.uppercase()
        val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    /**
     * Returns the localized country name using standard Java Locale.
     */
    fun getCountryName(countryCode: String?): String {
        if (countryCode == null || countryCode.equals("null", ignoreCase = true) || countryCode.isBlank()) return ""

        return try {
            val locale = Locale.Builder().setRegion(countryCode.uppercase()).build()
            val displayName = locale.getDisplayCountry(Locale.getDefault())
            if (displayName.isNotEmpty() && !displayName.equals(countryCode, ignoreCase = true)) {
                displayName
            } else {
                countryCode
            }
        } catch (e: Exception) {
            countryCode
        }
    }
}
