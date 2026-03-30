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

package ru.protonmod.next.desktop.ui.screens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.protonmod.next.data.network.LogicalServer
import ru.protonmod.next.desktop.DesktopVpnClient
import ru.protonmod.next.desktop.ServerEntry
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.ui.screens.countries.*

class DesktopCountriesViewModel(
    private val vpnClient: DesktopVpnClient,
    private val settingsManager: DesktopSettingsManager,
    private val serversFlow: StateFlow<List<ServerEntry>>,
    private val connectedServerFlow: StateFlow<ServerEntry?>
) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val _navState = MutableStateFlow<NavigationState>(NavigationState.Countries)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CountriesUiState> = combine(
        serversFlow,
        _navState,
        settingsManager.settings,
        _error
    ) { servers, nav, settings, error ->
        if (error != null) {
            return@combine CountriesUiState.Error(error)
        }

        // Convert ServerEntry to a lightweight LogicalServer representation for navigation logic
        val logicalServers = servers.groupBy { it.id }.map { (id, entries) ->
            val entry = entries.first()
            LogicalServer(
                id = id,
                name = entry.name,
                tier = entry.tier,
                features = 0,
                entryCountry = entry.country,
                exitCountry = entry.country,
                city = entry.city,
                servers = emptyList()
            ).apply {
                averageLoad = entry.physicalServer?.load ?: 0
            }
        }

        when (nav) {
            is NavigationState.Countries -> {
                val countries = logicalServers.groupBy { it.exitCountry }
                    .map { (code, countryServers) ->
                        val avg = if (countryServers.isEmpty()) 0 else countryServers.map { it.averageLoad }.average().toInt()
                        CountryDisplayItem(code, avg)
                    }
                    .sortedBy { it.code }
                CountriesUiState.CountriesList(countries, settings.serverLoadDisplayMode)
            }
            is NavigationState.Cities -> {
                val cities = logicalServers.filter { it.exitCountry == nav.countryCode }
                    .groupBy { it.city }
                    .map { (name, cityServers) ->
                        val avg = if (cityServers.isEmpty()) 0 else cityServers.map { it.averageLoad }.average().toInt()
                        CityDisplayItem(name, avg)
                    }
                    .sortedBy { it.name }
                CountriesUiState.CitiesList(nav.countryCode, cities, settings.serverLoadDisplayMode)
            }
            is NavigationState.Servers -> {
                val cityServers = logicalServers.filter { it.exitCountry == nav.countryCode && it.city == nav.cityName }
                    .sortedBy { it.name }
                CountriesUiState.ServersList(nav.countryCode, nav.cityName, cityServers, settings.serverLoadDisplayMode)
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), CountriesUiState.Loading)

    val connectedServer: StateFlow<ServerEntry?> = connectedServerFlow

    fun selectCountry(countryCode: String, onConnect: (ServerEntry) -> Unit) {
        val servers = serversFlow.value.filter { it.country == countryCode }
        if (servers.isNotEmpty()) {
            val bestServer = servers.minByOrNull { it.physicalServer?.load ?: 100 }
            bestServer?.let { onConnect(it) }
        }
    }

    fun selectCity(cityName: String, onConnect: (ServerEntry) -> Unit) {
        val nav = _navState.value
        if (nav is NavigationState.Cities) {
            val servers = serversFlow.value.filter { it.country == nav.countryCode && it.city == cityName }
            if (servers.isNotEmpty()) {
                val bestServer = servers.minByOrNull { it.physicalServer?.load ?: 100 }
                bestServer?.let { onConnect(it) }
            }
        }
    }

    fun selectServer(serverId: String, onConnect: (ServerEntry) -> Unit) {
        val server = serversFlow.value.find { it.id == serverId }
        server?.let { onConnect(it) }
    }

    fun expandCitiesForCountry(country: String) {
        _navState.value = NavigationState.Cities(country)
    }

    fun expandServersForCity(city: String) {
        val nav = _navState.value
        if (nav is NavigationState.Cities) {
            _navState.value = NavigationState.Servers(nav.countryCode, city)
        }
    }

    fun backToCountries() {
        _navState.value = NavigationState.Countries
    }

    fun backToCities() {
        val nav = _navState.value
        if (nav is NavigationState.Servers) {
            _navState.value = NavigationState.Cities(nav.countryCode)
        }
    }

    fun dispose() {
        job.cancel()
    }
}
