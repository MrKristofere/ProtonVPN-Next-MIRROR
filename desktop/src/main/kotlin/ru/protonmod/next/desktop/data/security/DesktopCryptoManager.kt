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

package ru.protonmod.next.desktop.data.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Desktop Cryptographic Manager for securing sensitive data in local storage
 * Uses AES-GCM for encryption with a machine-specific derived key
 */
class DesktopCryptoManager {
    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATION_COUNT = 10000
        private const val KEY_LENGTH = 256
        private const val TAG_LENGTH_BIT = 128
        private const val IV_LENGTH_BYTE = 12
        
        // Machine-specific salt-like value to derive unique key per user/machine
        private val SYSTEM_SALT = (System.getProperty("user.home") + System.getProperty("user.name")).toByteArray()
        private const val HARDCODED_SALT = "ProtonVPN-Next-Secure-Storage-Salt"
    }

    private val secretKey: SecretKey by lazy {
        val password = HARDCODED_SALT.toCharArray()
        val spec = PBEKeySpec(password, SYSTEM_SALT, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val tmp = factory.generateSecret(spec)
        SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Encrypt a plain text string
     */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val cipherText = cipher.doFinal(plainText.toByteArray())
            
            // Format: IV:CipherText (Base64)
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            
            Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            println("Encryption failed: ${e.message}")
            plainText // Fallback to plain text if encryption fails (though it shouldn't)
        }
    }

    /**
     * Decrypt an encrypted string
     */
    fun decrypt(encryptedBase64: String): String {
        return try {
            val combined = Base64.getDecoder().decode(encryptedBase64)
            if (combined.size < IV_LENGTH_BYTE) return encryptedBase64
            
            val iv = ByteArray(IV_LENGTH_BYTE)
            val cipherText = ByteArray(combined.size - IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            System.arraycopy(combined, iv.size, cipherText, 0, cipherText.size)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            String(cipher.doFinal(cipherText))
        } catch (e: Exception) {
            // If decryption fails, it might be that the token was saved as plain text before this change
            // Or the key changed (e.g., home directory moved).
            println("Decryption failed: ${e.message}")
            encryptedBase64
        }
    }
}
