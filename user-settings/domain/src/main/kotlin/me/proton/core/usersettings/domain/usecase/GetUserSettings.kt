/*
 * Copyright (c) 2021 Proton Technologies AG
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

package me.proton.core.usersettings.domain.usecase

import me.proton.core.account.domain.repository.AccountRepository
import me.proton.core.domain.entity.SessionUserId
import me.proton.core.network.domain.session.SessionId
import me.proton.core.usersettings.domain.entity.UserSettings
import me.proton.core.usersettings.domain.repository.UserSettingsRepository
import javax.inject.Inject

class GetUserSettings @Inject constructor(
    private val accountRepository: AccountRepository,
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(
        sessionId: SessionId,
        refresh: Boolean
    ): UserSettings {
        val account = requireNotNull(accountRepository.getAccountOrNull(sessionId))
        return invoke(account.userId, refresh)
    }

    suspend operator fun invoke(
        sessionUserId: SessionUserId,
        refresh: Boolean
    ) = userSettingsRepository.getUserSettings(sessionUserId, refresh = refresh)
}
