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

package ru.protonmod.next.shared

import kotlinx.coroutines.delay

sealed interface LoginResult {
    data class Success(val token: String) : LoginResult
    data class Error(val message: String) : LoginResult
}

suspend fun guestLogin(email: String, password: String): LoginResult {
    // Minimal stub for desktop testing.
    // Replace this with a real API call when you know the endpoint and request format.
    delay(800)

    if (email.isBlank() || password.isBlank()) {
        return LoginResult.Error("Email and password must not be empty")
    }

    // Fake token to show login succeeded.
    return LoginResult.Success("guest-token-${email.hashCode()}")
}
