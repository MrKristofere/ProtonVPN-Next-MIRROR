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

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    // Apply Compose Compiler plugin for Kotlin 2.x compatibility
    alias(libs.plugins.kotlin.compose)
    // Apply Compose Multiplatform plugin to properly resolve desktop artifacts
    id("org.jetbrains.compose") version "1.10.0"
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ru.protonmod.next.desktop.MainKt")
}

dependencies {
    // Use the Compose plugin extensions to add proper desktop dependencies
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.material3)
    implementation(compose.runtime)
    // Use the specific JetBrains version of extended icons for Desktop
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    // Coroutines Swing is required for Compose Desktop UI thread management
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // Networking and serialization
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.kotlinx.serialization.json)


    // Import shared module but EXCLUDE AndroidX Compose to prevent classpath clashes
    implementation(project(":shared")) {
        exclude(group = "androidx.compose.runtime")
        exclude(group = "androidx.compose.ui")
        exclude(group = "androidx.compose.material")
        exclude(group = "androidx.compose.material3")
        exclude(group = "androidx.compose.foundation")
    }
}