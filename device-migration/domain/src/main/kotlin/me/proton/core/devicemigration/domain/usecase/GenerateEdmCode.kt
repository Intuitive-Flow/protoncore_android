/*
 * Copyright (c) 2025 Proton AG
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

package me.proton.core.devicemigration.domain.usecase

import me.proton.core.auth.domain.entity.SessionForkSelector
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.devicemigration.domain.entity.ChildClientId
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.clientId
import me.proton.core.network.domain.session.SessionId
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

public class GenerateEdmCode @Inject constructor(
    private val authRepository: AuthRepository,
    cryptoContext: CryptoContext,
    private val product: Product
) {
    private val pgpCrypto = cryptoContext.pgpCrypto

    @OptIn(ExperimentalEncodingApi::class)
    public suspend operator fun invoke(sessionId: SessionId?): Pair<String, SessionForkSelector> {
        val (selector, userCode) = authRepository.getSessionForks(sessionId)
        val encryptionKey = pgpCrypto.generateRandomBytes(size = 32)
        val encodedEncryptionKey = Base64.encode(encryptionKey)
        val childClientId = ChildClientId(product.clientId())
        return Pair(
            "${userCode.value}:${encodedEncryptionKey}:${childClientId.value}",
            selector
        )
    }
}
