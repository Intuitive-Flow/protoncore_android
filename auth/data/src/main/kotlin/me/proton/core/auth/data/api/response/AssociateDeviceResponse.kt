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
import me.proton.core.crypto.common.pgp.Based64Encoded

@Serializable
data class AssociateDeviceResponse(
    @SerialName("AuthDevice")
    val device: AssociateAuthDeviceOutput
)

@Serializable
data class AssociateAuthDeviceOutput(
    /** Encrypted ID. */
    @SerialName("ID")
    val id: String,
    /** Base64-encoded, AES-GCM-encrypted secret using the DeviceSecret as key. */
    @SerialName("EncryptedSecret")
    val encryptedSecret: Based64Encoded
)
