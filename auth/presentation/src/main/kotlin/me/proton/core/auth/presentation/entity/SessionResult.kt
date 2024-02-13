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

package me.proton.core.auth.presentation.entity

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import me.proton.core.auth.domain.entity.SessionInfo

@Parcelize
data class SessionResult(
    val username: String?,
    val accessToken: String,
    val tokenType: String,
    val scopes: List<String>,
    val sessionId: String,
    val userId: String,
    val refreshToken: String,
    val eventId: String,
    val serverProof: String?,
    val localId: Int,
    val passwordMode: Int,
    val isSecondFactorNeeded: Boolean
) : Parcelable {

    @IgnoredOnParcel
    val isTwoPassModeNeeded = passwordMode == 2

    companion object {
        fun from(sessionInfo: SessionInfo): SessionResult = SessionResult(
            username = sessionInfo.username,
            accessToken = sessionInfo.accessToken,
            tokenType = sessionInfo.tokenType,
            scopes = sessionInfo.scopes,
            sessionId = sessionInfo.sessionId.id,
            userId = sessionInfo.userId.id,
            refreshToken = sessionInfo.refreshToken,
            eventId = sessionInfo.eventId,
            serverProof = sessionInfo.serverProof,
            localId = sessionInfo.localId,
            passwordMode = sessionInfo.passwordMode,
            isSecondFactorNeeded = sessionInfo.isSecondFactorNeeded
        )
    }
}
