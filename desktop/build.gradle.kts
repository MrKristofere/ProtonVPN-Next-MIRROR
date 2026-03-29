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

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    // Apply Compose Compiler plugin for Kotlin 2.x compatibility
    alias(libs.plugins.kotlin.compose)
    // Apply Compose Multiplatform plugin to properly resolve desktop artifacts
    id("org.jetbrains.compose") version "1.10.0"
}

compose.desktop {
    application {
        mainClass = "ru.protonmod.next.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            packageName = "ProtonVPN-Next"
            packageVersion = "1.0.0"

            linux {
                shortcut = true
                appCategory = "Network"
                menuGroup = "Network"
                iconFile.set(project.file("src/main/resources/drawable/ic_launcher.png"))
            }
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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

    // Native access for Go bridge
    implementation("net.java.dev.jna:jna:5.16.0")


    // Import shared module but EXCLUDE AndroidX Compose to prevent classpath clashes
    implementation(project(":shared")) {
        exclude(group = "androidx.compose.runtime")
        exclude(group = "androidx.compose.ui")
        exclude(group = "androidx.compose.material")
        exclude(group = "androidx.compose.material3")
        exclude(group = "androidx.compose.foundation")
    }
}

// Fixed portable task using createDistributable as base
tasks.register<Tar>("packagePortable") {
    group = "distribution"
    description = "Packages the application as a portable .tar.gz archive"
    dependsOn(":desktop:createDistributable")

    // Target the output of createDistributable
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    
    // Explicitly add libs folder to ensure vpn-helper and libgovpn.so are included
    from(project.file("libs")) {
        into("ProtonVPN-Next/lib/app/resources")
    }

    archiveFileName.set("ProtonVPN-Next-Portable.tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    compression = Compression.GZIP
}

// Task to wrap the distributable into a single AppImage file using system appimagetool
tasks.register<Exec>("packageAppImageFile") {
    group = "distribution"
    description = "Packages the application as a single .AppImage file"
    dependsOn(":desktop:createDistributable")

    val appDir = layout.buildDirectory.dir("compose/binaries/main/app/ProtonVPN-Next")
    val outputDir = layout.buildDirectory.dir("distributions")
    val outputFile = outputDir.get().file("ProtonVPN-Next-x86_64.AppImage")

    workingDir(project.projectDir)
    
    doFirst {
        outputDir.get().asFile.mkdirs()
        
        // Ensure resources directory exists in AppDir
        val resDir = file("${appDir.get().asFile.absolutePath}/lib/app/resources")
        resDir.mkdirs()
        
        // Copy native libs to AppDir resources
        val libsDir = file("libs")
        if (libsDir.exists()) {
            libsDir.copyRecursively(resDir, overwrite = true)
        }

        // Create necessary AppDir structure
        val runScript = file("${appDir.get().asFile.absolutePath}/AppRun")
        val ds = '$'
        runScript.writeText("""
            #!/bin/sh
            SELF=${ds}(readlink -f "${ds}0")
            HERE=${ds}(dirname "${ds}SELF")
            export LD_LIBRARY_PATH="${ds}HERE/lib:${ds}LD_LIBRARY_PATH"
            exec "${ds}HERE/bin/ProtonVPN-Next" "${ds}@"
        """.trimIndent())
        runScript.setExecutable(true)
        
        val desktopFile = file("${appDir.get().asFile.absolutePath}/ProtonVPN-Next.desktop")
        desktopFile.writeText("""
            [Desktop Entry]
            Type=Application
            Name=ProtonVPN-Next
            Exec=ProtonVPN-Next
            Icon=ProtonVPN-Next
            Categories=Network;
        """.trimIndent())
        
        // Copy icon to root of AppDir
        val iconSrc = file("src/main/resources/drawable/ic_launcher.png")
        if (iconSrc.exists()) {
            iconSrc.copyTo(file("${appDir.get().asFile.absolutePath}/ProtonVPN-Next.png"), overwrite = true)
        }
    }

    commandLine("appimagetool", appDir.get().asFile.absolutePath, outputFile.asFile.absolutePath)
}
