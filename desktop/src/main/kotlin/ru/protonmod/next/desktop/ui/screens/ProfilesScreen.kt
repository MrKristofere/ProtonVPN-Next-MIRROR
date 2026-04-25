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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.protonmod.next.data.model.VpnProfileUiModel
import ru.protonmod.next.data.network.LogicalServer
import ru.protonmod.next.desktop.ui.utils.DesktopStrings as Strings
import ru.protonmod.next.ui.theme.ProtonNextTheme
import ru.protonmod.next.ui.theme.liquidGlass
import ru.protonmod.next.ui.utils.CommonCountryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: DesktopProfilesViewModel,
    onConnect: (LogicalServer) -> Unit
) {
    val colors = ProtonNextTheme.colors
    val profiles by viewModel.profiles.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(Strings.profiles_title(), fontWeight = FontWeight.Bold, color = colors.textNorm) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Open Create Profile Dialog */ },
                containerColor = colors.brandNorm,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 100.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = Strings.desc_create_profile())
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (profiles.isEmpty()) {
                EmptyProfilesContent()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            onClick = { viewModel.connectWithProfile(profile, onConnect) },
                            onEdit = { /* TODO */ },
                            onDelete = { viewModel.deleteProfile(profile.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: VpnProfileUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = ProtonNextTheme.colors
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), alpha = 0.4f)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp)
                    .background(colors.brandNorm.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Terminal, null, tint = colors.brandNorm)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textNorm)
                
                val locationText = when {
                    profile.targetServerId != null -> profile.targetServerName ?: profile.targetServerId!!
                    profile.targetCountry != null -> {
                        val countryName = CommonCountryUtils.getCountryName(profile.targetCountry!!).ifBlank { profile.targetCountry!! }
                        if (profile.targetCity != null) "$countryName, ${profile.targetCity}" else countryName
                    }
                    else -> Strings.get("label_fastest_server")
                }
                
                Text(
                    text = "$locationText • ${profile.protocol}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textWeak
                )
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, null, tint = colors.iconWeak)
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null, tint = colors.notificationError.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun EmptyProfilesContent() {
    val colors = ProtonNextTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.VpnKey,
            contentDescription = null,
            tint = colors.iconWeak.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = Strings.profiles_empty_title(),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textNorm,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = Strings.profiles_empty_desc(),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textWeak,
            modifier = Modifier.padding(horizontal = 48.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
