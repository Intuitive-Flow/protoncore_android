
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

package me.proton.core.auth.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.auth.data.api.fido2.AuthenticationOptionsData
import me.proton.core.auth.data.api.fido2.Fido2RegisteredKeyData
import me.proton.core.auth.domain.entity.Fido2Info
import me.proton.core.auth.domain.entity.SecondFactor
import me.proton.core.auth.domain.entity.SecondFactorMethod
import me.proton.core.util.kotlin.hasFlag

@Serializable
data class SecondFactorInfoResponse(
    @SerialName("Enabled")
    val enabled: Int,

    @SerialName("FIDO2")
    val fido2: Fido2Response
) {
    fun toSecondFactor(): SecondFactor {
        return if (enabled != 0) {
            SecondFactor.Enabled(mapSupportedMethods(enabled), fido2.toFido2Info())
        } else {
            SecondFactor.Disabled
        }
    }

    private fun mapSupportedMethods(enabled: Int): Set<SecondFactorMethod> {
        return mutableSetOf<SecondFactorMethod>().apply {
            if (enabled.hasFlag(0b01)) add(SecondFactorMethod.Totp)
            if (enabled.hasFlag(0b10)) add(SecondFactorMethod.Authenticator)
        }.toSet()
    }
}

@Serializable
data class Fido2Response(
    @SerialName("AuthenticationOptions")
    val authenticationOptions: AuthenticationOptionsData? = null,

    @SerialName("RegisteredKeys")
    val registeredKeys: List<Fido2RegisteredKeyData> = emptyList()
) {
    fun toFido2Info(): Fido2Info = Fido2Info(
        authenticationOptions = authenticationOptions?.toFido2AuthenticationOptions(),
        registeredKeys = registeredKeys.map { it.toFido2RegisteredKey() }
    )
}
