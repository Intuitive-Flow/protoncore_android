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

package me.proton.core.account.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.crypto.common.keystore.EncryptedString
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.keystore.decryptOrElse
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.Session
import me.proton.core.network.domain.session.Session.Authenticated
import me.proton.core.network.domain.session.Session.Unauthenticated
import me.proton.core.network.domain.session.SessionId

@Entity(
    primaryKeys = ["sessionId"],
    indices = [
        Index("sessionId"),
        Index("userId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionEntity(
    val userId: UserId?,
    val sessionId: SessionId,
    val accessToken: EncryptedString,
    val refreshToken: EncryptedString,
    val scopes: List<String>,
    val product: Product
) {
    fun toSession(keyStoreCrypto: KeyStoreCrypto): Session {
        // Fall back to invalid tokens to force delete session on decryption failure.
        // See RefreshTokenHandler and sessionListener.onSessionForceLogout.
        val access = requireNotNull(accessToken.decryptOrElse(keyStoreCrypto) { "invalid" })
        val refresh = requireNotNull(refreshToken.decryptOrElse(keyStoreCrypto) { "invalid" })
        return when (userId) {
            null -> Unauthenticated(sessionId, access, refresh, scopes)
            else -> Authenticated(userId, sessionId, access, refresh, scopes)
        }
    }
}
