/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.ui.utils

import android.content.Context
import androidx.compose.ui.graphics.Color

object CountryUtils {

    /**
     * Returns a drawable resource ID for a country flag, or 0 if not found.
     */
    fun getFlagResource(context: Context, countryCode: String?): Int {
        return AndroidCountryUtils.getFlagResource(context, countryCode)
    }

    /**
     * Generates an Emoji flag from an ISO country code (e.g., "US" -> 🇺🇸)
     */
    fun getFlagForCountry(countryCode: String?): String {
        return CommonCountryUtils.getFlagForCountry(countryCode)
    }

    /**
     * Returns the localized country name.
     */
    fun getCountryName(context: Context, countryCode: String?): String {
        return AndroidCountryUtils.getCountryName(context, countryCode)
    }

    /**
     * Returns a color based on the load.
     */
    fun getColorForLoad(load: Int): Color {
        return SharedCountryUtils.getColorForLoad(load)
    }
}
