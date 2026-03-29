/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.utils

import ru.protonmod.next.ui.utils.CommonCountryUtils

object DesktopCountryUtils {
    /**
     * Returns the localized country name for desktop.
     */
    fun getCountryName(countryCode: String?): String {
        if (countryCode == null || countryCode.isBlank()) return ""
        val displayName = CommonCountryUtils.getCountryName(countryCode)
        return if (displayName.isNotEmpty() && !displayName.equals(countryCode, ignoreCase = true)) {
            displayName
        } else {
            countryCode
        }
    }
}
