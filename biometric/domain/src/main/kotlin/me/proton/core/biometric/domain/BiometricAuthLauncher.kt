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

package me.proton.core.biometric.domain

public interface BiometricAuthLauncher {
    @Suppress("LongParameterList")
    public fun launch(
        title: CharSequence,
        subtitle: CharSequence? = null,
        cancelButton: CharSequence,
        confirmationRequired: Boolean = true,
        allowedAuthenticators: Set<BiometricAuthenticator> = BiometricAuthenticator.DEFAULT_ALLOWED_AUTHENTICATORS,
        authenticatorsResolver: AuthenticatorsResolver
    )
}
