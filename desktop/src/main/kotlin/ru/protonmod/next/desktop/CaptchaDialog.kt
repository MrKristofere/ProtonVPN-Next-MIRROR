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

package ru.protonmod.next.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.protonmod.next.ui.theme.ProtonNextTheme as Theme
import java.awt.Desktop
import java.net.URI

@Composable
fun CaptchaDialog(
    webUrl: String,
    onDismiss: () -> Unit,
    onCaptchaSolved: (String) -> Unit
) {
    var token by remember { mutableStateOf("") }
    var openedBrowser by remember { mutableStateOf(false) }
    val colors = Theme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Human verification", color = colors.textNorm) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("A browser window will open so you can complete the captcha.", color = colors.textNorm)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "After completing the verification, copy the token from the page and paste it below.",
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.textNorm
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Captcha token") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(URI(webUrl))
                            openedBrowser = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.brandNorm,
                        contentColor = colors.textInverted
                    )
                ) {
                    Text(if (openedBrowser) "Re-open captcha" else "Open captcha in browser")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (token.isNotBlank()) {
                        onCaptchaSolved(token.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brandNorm,
                    contentColor = colors.textInverted
                )
            ) {
                Text("Submit token")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
