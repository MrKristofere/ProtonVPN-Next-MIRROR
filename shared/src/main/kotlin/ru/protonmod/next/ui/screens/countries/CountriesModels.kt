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
