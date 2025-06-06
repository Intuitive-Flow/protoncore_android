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

package me.proton.core.crypto.common.context

import me.proton.core.crypto.common.aead.AeadCrypto
import me.proton.core.crypto.common.aead.AeadCryptoFactory
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.srp.SrpCrypto

/**
 * Context providing any needed dependencies for Crypto functions.
 *
 * @see [KeyStoreCrypto]
 * @see [AeadCrypto]
 * @see [PGPCrypto]
 * @see [SrpCrypto]
 */
interface CryptoContext {
    val keyStoreCrypto: KeyStoreCrypto
    val aeadCryptoFactory: AeadCryptoFactory
    val pgpCrypto: PGPCrypto
    val srpCrypto: SrpCrypto
}
