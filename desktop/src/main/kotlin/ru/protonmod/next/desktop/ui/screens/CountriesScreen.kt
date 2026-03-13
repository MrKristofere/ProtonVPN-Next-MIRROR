package ru.protonmod.next.desktop.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.protonmod.next.desktop.ServerEntry
import ru.protonmod.next.ui.theme.ProtonNextTheme

sealed class CountriesNavigation {
    object CountriesList : CountriesNavigation()
    data class CityList(val countryCode: String) : CountriesNavigation()
    data class ServerList(val countryCode: String, val city: String) : CountriesNavigation()
}

data class CountryDisplayItem(
    val code: String,
    val name: String,
    val averageLoad: Int = (20..80).random(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesScreen(
    servers: List<ServerEntry>,
    connectedServer: ServerEntry?,
    onBack: () -> Unit,
    onConnect: (ServerEntry) -> Unit
) {
    val colors = ProtonNextTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var currentNav by remember { mutableStateOf<CountriesNavigation>(CountriesNavigation.CountriesList) }
    
    val countries = remember(servers) {
        servers.asSequence()
            .map { it.country }
            .distinct()
            .map { code -> CountryDisplayItem(code = code, name = getCountryName(code)) }
            .sortedBy { it.name }
            .toList()
    }

    val filteredCountries = remember(countries, searchQuery) {
        if (searchQuery.isEmpty()) countries
        else countries.filter { it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = when (val nav = currentNav) {
                        is CountriesNavigation.CountriesList -> "Countries"
                        is CountriesNavigation.CityList -> getCountryName(nav.countryCode)
                        is CountriesNavigation.ServerList -> nav.city
                    }
                    
                    if (currentNav is CountriesNavigation.CountriesList) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search countries...", color = colors.textWeak) },
                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = colors.textNorm,
                                unfocusedTextColor = colors.textNorm
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.iconWeak) },
                            singleLine = true
                        )
                    } else {
                        Text(titleText, fontWeight = FontWeight.Bold, color = colors.textNorm)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentNav) {
                            is CountriesNavigation.CountriesList -> onBack()
                            is CountriesNavigation.CityList -> currentNav = CountriesNavigation.CountriesList
                            is CountriesNavigation.ServerList -> {
                                val nav = currentNav as CountriesNavigation.ServerList
                                currentNav = CountriesNavigation.CityList(nav.countryCode)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textNorm
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.backgroundNorm)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundNorm)
                .padding(paddingValues)
        ) {
            if (servers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.brandNorm)
                }
            } else {
                AnimatedContent(targetState = currentNav) { nav ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (nav) {
                            is CountriesNavigation.CountriesList -> {
                                items(filteredCountries) { country ->
                                    CountryCard(
                                        country = country,
                                        isConnected = connectedServer?.country == country.code,
                                        onClick = { 
                                            currentNav = CountriesNavigation.CityList(country.code)
                                        }
                                    )
                                }
                            }
                            is CountriesNavigation.CityList -> {
                                val cities = servers.filter { it.country == nav.countryCode }
                                    .map { it.city }.distinct().sorted()
                                
                                items(cities) { city ->
                                    NavigationCard(
                                        title = city,
                                        icon = Icons.Rounded.LocationCity,
                                        onClick = {
                                            currentNav = CountriesNavigation.ServerList(nav.countryCode, city)
                                        }
                                    )
                                }
                            }
                            is CountriesNavigation.ServerList -> {
                                val cityServers = servers.filter { (it.country == nav.countryCode) && (it.city == nav.city) }
                                    .sortedBy { it.name }
                                
                                items(cityServers) { server ->
                                    NavigationCard(
                                        title = server.name,
                                        icon = Icons.Rounded.Settings,
                                        isConnected = connectedServer?.id == server.id,
                                        onClick = { onConnect(server) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationCard(
    title: String,
    icon: ImageVector,
    isConnected: Boolean = false,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) colors.brandNorm.copy(alpha = 0.1f) else colors.backgroundSecondary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (isConnected) colors.brandNorm else colors.iconWeak)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.textNorm, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            if (isConnected) {
                Box(modifier = Modifier.size(8.dp).background(colors.notificationSuccess, CircleShape))
            }
        }
    }
}

@Composable
fun CountryCard(
    country: CountryDisplayItem,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) colors.brandNorm.copy(alpha = 0.1f) else colors.backgroundSecondary.copy(alpha = 0.5f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val flagName = "flag_${country.code.lowercase()}"
                val flagResource = "drawable/$flagName.xml"
                val hasFlag = remember(flagResource) {
                    Thread.currentThread().contextClassLoader.getResource(flagResource) != null
                }

                Box(contentAlignment = Alignment.BottomEnd) {
                    if (hasFlag) {
                        Icon(
                            painter = painterResource(flagResource),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp, 24.dp).clip(RoundedCornerShape(4.dp)),
                            tint = Color.Unspecified
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp, 24.dp),
                            tint = colors.iconWeak
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
                    text = country.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textNorm,
                    modifier = Modifier.weight(1f)
                )

                LoadIndicator(load = country.averageLoad)
            }
            
            LinearProgressIndicator(
                progress = { country.averageLoad / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = if (country.averageLoad > 80) colors.notificationError else if (country.averageLoad > 50) colors.notificationWarning else colors.notificationSuccess,
                trackColor = Color.Transparent
            )
        }
    }
}

@Composable
fun LoadIndicator(load: Int) {
    val colors = ProtonNextTheme.colors
    val color = when {
        load < 40 -> colors.notificationSuccess
        load < 70 -> colors.notificationWarning
        else -> colors.notificationError
    }

    Text(
        text = "$load%",
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

// Simple country name mapper for now
fun getCountryName(code: String): String {
    return when(code.uppercase()) {
        "US" -> "United States"
        "RU" -> "Russia"
        "NL" -> "Netherlands"
        "DE" -> "Germany"
        "JP" -> "Japan"
        "CH" -> "Switzerland"
        "CA" -> "Canada"
        "FR" -> "France"
        "GB" -> "United Kingdom"
        "AU" -> "Australia"
        else -> code
    }
}
