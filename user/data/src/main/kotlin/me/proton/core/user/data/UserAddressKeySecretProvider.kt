/*
 * Copyright (c) 2020 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.user.data

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.decrypt
import me.proton.core.crypto.common.keystore.encrypt
import me.proton.core.crypto.common.keystore.use
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.decryptNestedKeyOrNull
import me.proton.core.key.domain.encryptData
import me.proton.core.key.domain.entity.key.KeyFlags
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.key.domain.signData
import me.proton.core.user.domain.entity.AddressType
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.entity.UserAddressKey
import me.proton.core.user.domain.entity.emailSplit
import me.proton.core.user.domain.repository.PassphraseRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provide User Address secret according old vs new format.
 */
@Singleton
class UserAddressKeySecretProvider @Inject constructor(
    private val passphraseRepository: PassphraseRepository,
    private val cryptoContext: CryptoContext,
) {
    private val keyStoreCrypto = cryptoContext.keyStoreCrypto

    suspend fun getPassphrase(
        userId: UserId,
        userContext: KeyHolderContext,
        key: UserAddressKey
    ): EncryptedByteArray? {
        return when {
            // Inactive don't need to be unlocked.
            !key.active -> null
            // Old address key format -> user passphrase.
            key.token == null || key.signature == null -> passphraseRepository.getPassphrase(userId)
            // New address key format -> user keys encrypt token + signature -> address passphrase.
            else -> userContext.decryptNestedKeyOrNull(
                key = key.privateKey.key,
                passphrase = requireNotNull(key.token),
                signature = requireNotNull(key.signature),
                validTokenPredicate = UserAddressKeySecretProvider::tokenHasValidFormat
            )?.takeIf { nestedPrivateKey ->
                nestedPrivateKey.status == VerificationStatus.Success
            }?.privateKey?.passphrase

            // If the passphrase can't be decrypted, null is returned, key will be set as inactive locally.
        }
    }

    data class UserAddressKeySecret(
        val passphrase: EncryptedByteArray,
        val token: Armored? = null,
        val signature: Armored? = null
    )

    private fun generateUserAddressKeySecret(
        userPrivateKey: PrivateKey,
        generateNewKeyFormat: Boolean
    ): UserAddressKeySecret {
        return if (generateNewKeyFormat) {
            // New address key format -> user keys encrypt token + signature -> address passphrase.
            cryptoContext.pgpCrypto.generateNewToken().use { passphrase ->
                UserAddressKeySecret(
                    passphrase = passphrase.encrypt(keyStoreCrypto),
                    token = userPrivateKey.encryptData(cryptoContext, passphrase.array),
                    signature = userPrivateKey.signData(cryptoContext, passphrase.array)
                )
            }
        } else {
            // Old address key format -> user passphrase.
            UserAddressKeySecret(
                passphrase = checkNotNull(userPrivateKey.passphrase) { "Passphrase cannot be null." },
                token = null,
                signature = null
            )
        }
    }

    @Suppress("LongParameterList")
    fun generateUserAddressKey(
        generateNewKeyFormat: Boolean,
        userAddress: UserAddress,
        userPrivateKey: PrivateKey,
        isPrimary: Boolean
    ): UserAddressKey {
        val secret = generateUserAddressKeySecret(userPrivateKey, generateNewKeyFormat)
        secret.passphrase.decrypt(keyStoreCrypto).use { decryptedPassphrase ->
            val email = userAddress.emailSplit
            val privateKey = PrivateKey(
                key = cryptoContext.pgpCrypto.generateNewPrivateKey(
                    username = email.username,
                    domain = email.domain,
                    passphrase = decryptedPassphrase.array
                ),
                isPrimary = isPrimary,
                isActive = true,
                passphrase = secret.passphrase,
            )
            val defaultKeyFlags = KeyFlags.NotCompromised or KeyFlags.NotObsolete
            val keyFlags = when (userAddress.type) {
                AddressType.External -> defaultKeyFlags or KeyFlags.EmailNoEncrypt or KeyFlags.EmailNoSign
                else -> defaultKeyFlags
            }
            return UserAddressKey(
                addressId = userAddress.addressId,
                version = 3,
                flags = keyFlags,
                token = secret.token,
                signature = secret.signature,
                active = true,
                keyId = KeyId("temp"),
                privateKey = privateKey
            )
        }
    }

    companion object {
        private const val HEX_DIGITS = "0123456789abcdefABCDEF"
        private const val EXPECTED_TOKEN_LENGTH = 64

        /**
         * Check the format of address key tokens.
         * They must be 64 char long hexadecimal strings.
         */
        private fun tokenHasValidFormat(decryptedToken: ByteArray): Boolean {
            if (decryptedToken.size != EXPECTED_TOKEN_LENGTH) {
                return false
            }
            for (char in decryptedToken) {
                if (!HEX_DIGITS.contains(char.toInt().toChar())) {
                    return false
                }
            }
            return true
        }
    }
}
