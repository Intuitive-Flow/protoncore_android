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

package me.proton.core.crypto.android.context

import me.proton.core.crypto.android.aead.AndroidAeadCrypto
import me.proton.core.crypto.android.pgp.GOpenPGPCrypto
import me.proton.core.crypto.android.keystore.AndroidKeyStoreCrypto
import me.proton.core.crypto.android.srp.GOpenPGPSrpCrypto
import me.proton.core.crypto.common.aead.AeadCrypto
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.PGPCrypto
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.srp.SrpCrypto
import me.proton.core.util.kotlin.DefaultDispatcherProvider

/**
 * [CryptoContext] for Android platform.
 *
 * @see AndroidKeyStoreCrypto
 * @see AndroidAeadCrypto
 * @see GOpenPGPCrypto
 * @see SrpCrypto
 */
class AndroidCryptoContext(
    override val keyStoreCrypto: KeyStoreCrypto = AndroidKeyStoreCrypto.default,
    override val aeadCrypto: AeadCrypto = AndroidAeadCrypto.default,
    override val pgpCrypto: PGPCrypto = GOpenPGPCrypto(),
    override val srpCrypto: SrpCrypto = GOpenPGPSrpCrypto(DefaultDispatcherProvider())
) : CryptoContext
