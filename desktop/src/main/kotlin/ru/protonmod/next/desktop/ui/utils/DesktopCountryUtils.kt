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
