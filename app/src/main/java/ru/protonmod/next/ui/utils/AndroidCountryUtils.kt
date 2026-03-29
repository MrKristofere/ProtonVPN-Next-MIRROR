/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.ui.utils

import android.content.Context
import ru.protonmod.next.ui.theme.ProtonPalette

object AndroidCountryUtils {

    /**
     * Returns a drawable resource ID for a country flag, or 0 if not found.
     */
    fun getFlagResource(context: Context, countryCode: String?): Int {
        if (countryCode == null) return 0
        val normalizedCode = when (val code = countryCode.lowercase()) {
            "uk" -> "gb"
            else -> code
        }
        val resName = "flag_$normalizedCode"
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    /**
     * Returns the localized country name.
     */
    fun getCountryName(context: Context, countryCode: String?): String {
        if (countryCode == null || countryCode.equals("null", ignoreCase = true) || countryCode.isBlank()) return ""

        val displayName = CommonCountryUtils.getCountryName(countryCode)
        
        return if (displayName.isNotEmpty() && !displayName.equals(countryCode, ignoreCase = true)) {
            displayName
        } else {
            val resourceName = "country_${countryCode.lowercase()}"
            val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId).takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: countryCode
            } else countryCode
        }
    }
}
