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

package ru.protonmod.next.desktop.ui.screens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.protonmod.next.data.network.LogicalServer
import ru.protonmod.next.desktop.DesktopVpnClient
import ru.protonmod.next.desktop.data.DesktopSettingsManager
import ru.protonmod.next.ui.screens.countries.CountryDisplayItem
import ru.protonmod.next.data.model.VpnProfileUiModel

class DesktopProfilesViewModel(
    private val vpnClient: DesktopVpnClient,
    private val settingsManager: DesktopSettingsManager,
    private val serversFlow: StateFlow<List<LogicalServer>>,
    private val connectedServerFlow: StateFlow<LogicalServer?>
) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    // In a real implementation, this would come from a ProfileDao in DesktopDatabase
    // For now, we mirror the structure to enable UI development
    private val _profiles = MutableStateFlow<List<VpnProfileUiModel>>(emptyList())
    val profiles: StateFlow<List<VpnProfileUiModel>> = _profiles.asStateFlow()

    private val _countries = MutableStateFlow<List<CountryDisplayItem>>(emptyList())
    val countries: StateFlow<List<CountryDisplayItem>> = _countries.asStateFlow()

    init {
        observeServersForCountries()
    }

    private fun observeServersForCountries() {
        scope.launch {
            serversFlow.collect { servers ->
                if (servers.isNotEmpty()) {
                    _countries.value = servers
                        .groupBy { it.exitCountry }
                        .map { (countryCode, countryServers) ->
                            val avgLoad = if (countryServers.isEmpty()) 0 else countryServers.map { it.averageLoad }.average().toInt()
                            CountryDisplayItem(code = countryCode, averageLoad = avgLoad)
                        }
                        .sortedBy { it.code }
                }
            }
        }
    }

    fun saveProfile(uiModel: VpnProfileUiModel) {
        // Mock save logic
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == uiModel.id }
        if (index != -1) {
            current[index] = uiModel
        } else {
            current.add(uiModel)
        }
        _profiles.value = current
    }

    fun deleteProfile(id: String) {
        _profiles.value = _profiles.value.filter { it.id != id }
    }

    fun connectWithProfile(profile: VpnProfileUiModel, onConnect: (LogicalServer) -> Unit) {
        val servers = serversFlow.value
        val targetServer = findBestServerForProfile(profile, servers)
        targetServer?.let { onConnect(it) }
    }

    private fun findBestServerForProfile(
        profile: VpnProfileUiModel,
        allServers: List<LogicalServer>
    ): LogicalServer? {
        if (profile.targetServerId != null) {
            return allServers.find { it.id == profile.targetServerId }
        }

        if (profile.targetCity != null && profile.targetCountry != null) {
            val cityServers = allServers.filter { it.exitCountry == profile.targetCountry && it.city == profile.targetCity }
            if (cityServers.isNotEmpty()) {
                return cityServers.minByOrNull { it.averageLoad }
            }
        }

        if (profile.targetCountry != null) {
            val countryServers = allServers.filter { it.exitCountry == profile.targetCountry }
            if (countryServers.isNotEmpty()) {
                return countryServers.minByOrNull { it.averageLoad }
            }
        }

        return allServers.minByOrNull { it.averageLoad }
    }

    fun dispose() {
        job.cancel()
    }
}
