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

package ru.protonmod.next.ui.screens.countries

import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.data.network.LogicalServer

data class CountryDisplayItem(val code: String, val averageLoad: Int)
data class CityDisplayItem(val name: String, val averageLoad: Int)

sealed class CountriesUiState {
    data object Loading : CountriesUiState()
    data class CountriesList(
        val countries: List<CountryDisplayItem>,
        val loadDisplayMode: ServerLoadDisplayMode = ServerLoadDisplayMode.ALL
    ) : CountriesUiState()
    data class CitiesList(
        val country: String,
        val cities: List<CityDisplayItem>,
        val loadDisplayMode: ServerLoadDisplayMode = ServerLoadDisplayMode.ALL
    ) : CountriesUiState()
    data class ServersList(
        val country: String,
        val city: String,
        val servers: List<LogicalServer>,
        val loadDisplayMode: ServerLoadDisplayMode = ServerLoadDisplayMode.ALL
    ) : CountriesUiState()
    data class Error(val message: String) : CountriesUiState()
}

sealed class NavigationState {
    data object Countries : NavigationState()
    data class Cities(val countryCode: String) : NavigationState()
    data class Servers(val countryCode: String, val cityName: String) : NavigationState()
}
