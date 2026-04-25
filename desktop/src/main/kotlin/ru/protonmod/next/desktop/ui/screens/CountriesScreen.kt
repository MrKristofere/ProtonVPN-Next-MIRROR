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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.protonmod.next.data.local.ServerLoadDisplayMode
import ru.protonmod.next.data.network.LogicalServer
import ru.protonmod.next.desktop.ServerEntry
import ru.protonmod.next.desktop.ui.components.FlagIcon
import ru.protonmod.next.desktop.ui.components.LoadIndicator
import ru.protonmod.next.desktop.ui.components.LoadProgressBar
import ru.protonmod.next.desktop.ui.utils.DesktopCountryUtils
import ru.protonmod.next.desktop.ui.utils.isTablet
import ru.protonmod.next.ui.screens.countries.*
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesScreen(
    viewModel: DesktopCountriesViewModel,
    onConnect: (LogicalServer) -> Unit
) {
    val colors = ProtonNextTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val connectedServer by viewModel.connectedServer.collectAsState()
    val isTablet = isTablet()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            val title = when (val state = uiState) {
                is CountriesUiState.CountriesList -> "Countries"
                is CountriesUiState.CitiesList -> DesktopCountryUtils.getCountryName(state.country)
                is CountriesUiState.ServersList -> "${DesktopCountryUtils.getCountryName(state.country)}, ${state.city}"
                else -> "Countries"
            }
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = colors.textNorm) },
                navigationIcon = {
                    if (uiState !is CountriesUiState.CountriesList) {
                        IconButton(onClick = {
                            when (uiState) {
                                is CountriesUiState.CitiesList -> viewModel.backToCountries()
                                is CountriesUiState.ServersList -> viewModel.backToCities()
                                else -> {}
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textNorm)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(targetState = uiState, label = "countries_navigation") { state ->
                when (state) {
                    is CountriesUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.brandNorm)
                        }
                    }
                    is CountriesUiState.CountriesList -> {
                        CountriesListContent(
                            countries = state.countries,
                            connectedServer = connectedServer,
                            isTablet = isTablet,
                            loadDisplayMode = state.loadDisplayMode,
                            onCountryClick = { viewModel.selectCountry(it.code, onConnect) },
                            onCountryMore = { viewModel.expandCitiesForCountry(it.code) }
                        )
                    }
                    is CountriesUiState.CitiesList -> {
                        CitiesListContent(
                            cities = state.cities,
                            connectedServer = connectedServer,
                            countryCode = state.country,
                            isTablet = isTablet,
                            loadDisplayMode = state.loadDisplayMode,
                            onCityClick = { viewModel.selectCity(it.name, onConnect) },
                            onCityMore = { viewModel.expandServersForCity(it.name) }
                        )
                    }
                    is CountriesUiState.ServersList -> {
                        ServersListContent(
                            servers = state.servers,
                            connectedServer = connectedServer,
                            isTablet = isTablet,
                            loadDisplayMode = state.loadDisplayMode,
                            onServerClick = { viewModel.selectServer(it.id, onConnect) }
                        )
                    }
                    is CountriesUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(state.message, color = colors.notificationError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountriesListContent(
    countries: List<CountryDisplayItem>,
    connectedServer: LogicalServer?,
    isTablet: Boolean,
    loadDisplayMode: ServerLoadDisplayMode,
    onCountryClick: (CountryDisplayItem) -> Unit,
    onCountryMore: (CountryDisplayItem) -> Unit
) {
    if (isTablet) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(countries) { country ->
                CountryCard(
                    country = country,
                    isConnected = connectedServer?.exitCountry == country.code,
                    displayMode = loadDisplayMode,
                    onClick = { onCountryClick(country) },
                    onMoreClick = { onCountryMore(country) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(countries) { country ->
                CountryCard(
                    country = country,
                    isConnected = connectedServer?.exitCountry == country.code,
                    displayMode = loadDisplayMode,
                    onClick = { onCountryClick(country) },
                    onMoreClick = { onCountryMore(country) }
                )
            }
        }
    }
}

@Composable
private fun CitiesListContent(
    cities: List<CityDisplayItem>,
    connectedServer: LogicalServer?,
    countryCode: String,
    isTablet: Boolean,
    loadDisplayMode: ServerLoadDisplayMode,
    onCityClick: (CityDisplayItem) -> Unit,
    onCityMore: (CityDisplayItem) -> Unit
) {
    if (isTablet) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cities) { city ->
                CityCard(
                    city = city,
                    isConnected = connectedServer?.city == city.name && connectedServer.exitCountry == countryCode,
                    displayMode = loadDisplayMode,
                    onClick = { onCityClick(city) },
                    onMoreClick = { onCityMore(city) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cities) { city ->
                CityCard(
                    city = city,
                    isConnected = connectedServer?.city == city.name && connectedServer.exitCountry == countryCode,
                    displayMode = loadDisplayMode,
                    onClick = { onCityClick(city) },
                    onMoreClick = { onCityMore(city) }
                )
            }
        }
    }
}

@Composable
private fun ServersListContent(
    servers: List<LogicalServer>,
    connectedServer: LogicalServer?,
    isTablet: Boolean,
    loadDisplayMode: ServerLoadDisplayMode,
    onServerClick: (LogicalServer) -> Unit
) {
    if (isTablet) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(servers) { server ->
                ServerItemCard(
                    server = server,
                    isConnected = connectedServer?.id == server.id,
                    displayMode = loadDisplayMode,
                    onClick = { onServerClick(server) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(servers) { server ->
                ServerItemCard(
                    server = server,
                    isConnected = connectedServer?.id == server.id,
                    displayMode = loadDisplayMode,
                    onClick = { onServerClick(server) }
                )
            }
        }
    }
}

@Composable
private fun CountryCard(
    country: CountryDisplayItem,
    isConnected: Boolean,
    displayMode: ServerLoadDisplayMode,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val localizedName = DesktopCountryUtils.getCountryName(country.code)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                alpha = if (isConnected) 0.2f else 0.4f,
                shadowElevation = 0.dp
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    FlagIcon(countryCode = country.code, size = DpSize(36.dp, 24.dp))
                    if (isConnected) {
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = 4.dp)
                                .size(10.dp)
                                .background(colors.notificationSuccess, CircleShape)
                                .padding(2.dp)
                                .background(colors.backgroundNorm, CircleShape)
                                .padding(1.dp)
                                .background(colors.notificationSuccess, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = localizedName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textNorm,
                    modifier = Modifier.weight(1f)
                )

                LoadIndicator(load = country.averageLoad, displayMode = displayMode)

                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = colors.iconWeak
                    )
                }
            }

            LoadProgressBar(load = country.averageLoad, displayMode = displayMode)
        }
    }
}

@Composable
private fun CityCard(
    city: CityDisplayItem,
    isConnected: Boolean,
    displayMode: ServerLoadDisplayMode,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                alpha = if (isConnected) 0.2f else 0.4f,
                shadowElevation = 0.dp
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(36.dp, 24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.backgroundNorm),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = colors.iconNorm,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (isConnected) {
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = 4.dp)
                                .size(10.dp)
                                .background(colors.notificationSuccess, CircleShape)
                                .padding(2.dp)
                                .background(colors.backgroundNorm, CircleShape)
                                .padding(1.dp)
                                .background(colors.notificationSuccess, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textNorm,
                    modifier = Modifier.weight(1f)
                )

                LoadIndicator(load = city.averageLoad, displayMode = displayMode)

                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = colors.iconWeak
                    )
                }
            }

            LoadProgressBar(load = city.averageLoad, displayMode = displayMode)
        }
    }
}

@Composable
private fun ServerItemCard(
    server: LogicalServer,
    isConnected: Boolean,
    displayMode: ServerLoadDisplayMode,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                alpha = if (isConnected) 0.2f else 0.4f,
                shadowElevation = 0.dp
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textNorm
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadIndicator(load = server.averageLoad, displayMode = displayMode)
                    if (isConnected) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(colors.notificationSuccess, CircleShape)
                        )
                    }
                }
            }
            LoadProgressBar(load = server.averageLoad, displayMode = displayMode)
        }
    }
}
