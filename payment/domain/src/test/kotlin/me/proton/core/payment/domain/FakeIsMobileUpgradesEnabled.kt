/*
 * Copyright (c) 2024 Proton AG
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

package me.proton.core.payment.domain

import me.proton.core.domain.entity.UserId
import me.proton.core.payment.domain.features.IsMobileUpgradesEnabled

class FakeIsMobileUpgradesEnabled(var isEnabled: Boolean) : IsMobileUpgradesEnabled {
    override fun invoke(userId: UserId?): Boolean = isEnabled
    override fun isLocalEnabled(): Boolean = isEnabled
    override fun isRemoteEnabled(userId: UserId?): Boolean = isEnabled
}
