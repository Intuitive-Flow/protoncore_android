/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and ProtonCore.
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

package me.proton.core.auth.domain.usecase

import me.proton.core.auth.domain.entity.SessionInfo
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.challenge.domain.ChallengeManager
import me.proton.core.challenge.domain.useFlow
import me.proton.core.crypto.common.keystore.EncryptedString
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.keystore.decrypt
import me.proton.core.crypto.common.keystore.use
import me.proton.core.crypto.common.srp.SrpCrypto
import me.proton.core.crypto.common.srp.SrpProofs
import javax.inject.Inject

/**
 * Performs the login request along with the login info request which is always preceding it.
 */
class PerformLogin @Inject constructor(
    private val authRepository: AuthRepository,
    private val srpCrypto: SrpCrypto,
    private val keyStoreCrypto: KeyStoreCrypto,
    private val challengeManager: ChallengeManager,
    private val challengeConfig: LoginChallengeConfig,
) {
    suspend operator fun invoke(
        username: String,
        password: EncryptedString
    ): SessionInfo {
        val loginInfo = authRepository.getAuthInfoSrp(
            sessionId = null,
            username = username,
        )
        password.decrypt(keyStoreCrypto).toByteArray().use { decryptedPassword ->
            val srpProofs: SrpProofs = srpCrypto.generateSrpProofs(
                username = username,
                password = decryptedPassword.array,
                version = loginInfo.version.toLong(),
                salt = loginInfo.salt,
                modulus = loginInfo.modulus,
                serverEphemeral = loginInfo.serverEphemeral
            )
            return challengeManager.useFlow(challengeConfig.flowName) { frames ->
                authRepository.performLogin(
                    frames = frames,
                    username = username,
                    srpProofs = srpProofs,
                    srpSession = loginInfo.srpSession
                )
            }
        }
    }
}
