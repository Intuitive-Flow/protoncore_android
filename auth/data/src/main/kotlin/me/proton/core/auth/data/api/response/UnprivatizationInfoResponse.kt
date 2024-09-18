/*
 * Copyright (c) 2024 Proton Technologies AG
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

package me.proton.core.auth.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.auth.domain.entity.UnprivatizationInfo
import me.proton.core.auth.domain.entity.UnprivatizeState

@Serializable
data class UnprivatizationInfoResponse(
    @SerialName("State")
    val state: Int,
    @SerialName("AdminEmail")
    val adminEmail: String,
    @SerialName("OrgKeyFingerprintSignature")
    val orgKeyFingerprintSignature: String,
    @SerialName("OrgPublicKey")
    val orgPublicKey: String,
) {
    fun toUnprivatizationInfo(): UnprivatizationInfo =
        UnprivatizationInfo(
            state = UnprivatizeState.map[state] ?: UnprivatizeState.Declined,
            adminEmail = adminEmail,
            orgKeyFingerprintSignature = orgKeyFingerprintSignature,
            orgPublicKey = orgPublicKey
        )
}
