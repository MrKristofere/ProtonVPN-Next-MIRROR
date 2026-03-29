/*
 * Copyright (C) 2026 SMH01
 */

package ru.protonmod.next.desktop.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DeviceType {
    Phone, Tablet
}

val LocalDeviceType = compositionLocalOf { DeviceType.Phone }

@Composable
fun ProvideDeviceType(windowWidth: Dp, content: @Composable () -> Unit) {
    val deviceType = if (windowWidth >= 600.dp) DeviceType.Tablet else DeviceType.Phone
    CompositionLocalProvider(LocalDeviceType provides deviceType, content = content)
}

@Composable
fun isTablet(): Boolean = LocalDeviceType.current == DeviceType.Tablet
