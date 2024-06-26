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

package me.proton.core.userrecovery.data.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import me.proton.core.domain.entity.UserId
import me.proton.core.userrecovery.domain.repository.DeviceRecoveryRepository
import me.proton.core.userrecovery.domain.usecase.GetUnlockedUserKeys
import javax.inject.Inject

class ObserveUsersWithRecoverySecretButNoFile @Inject constructor(
    private val deviceRecoveryRepository: DeviceRecoveryRepository,
    private val getUnlockedUserKeys: GetUnlockedUserKeys,
    private val observeUserDeviceRecovery: ObserveUserDeviceRecovery
) {
    operator fun invoke(): Flow<UserId> = observeUserDeviceRecovery()
        .filter { (_, deviceRecovery) -> deviceRecovery == true }
        .mapNotNull { (user, _) ->
            val primaryRecoverySecretHash = user.keys
                .firstOrNull { it.privateKey.isPrimary }
                ?.recoverySecretHash
            primaryRecoverySecretHash?.let { Pair(user, it) }
        }
        .filter { (user, primaryRecoverySecretHash) ->
            val recoveryFiles = deviceRecoveryRepository.getRecoveryFiles(user.userId)
            val recoveryFile = recoveryFiles.firstOrNull {
                it.recoverySecretHash.equals(primaryRecoverySecretHash, ignoreCase = true)
            }

            // If there is no recovery file corresponding to the recovery secret,
            // or the keyCount of the recovery file is different than expected
            // (e.g. if a new key has been activated after the recovery file has been created),
            // then return this user, so we can create a new recovery file.
            recoveryFile == null || getUnlockedUserKeyCount(user.userId) > recoveryFile.keyCount
        }.map { (user, _) ->
            user.userId
        }

    private suspend fun getUnlockedUserKeyCount(userId: UserId): Int {
        val keys = getUnlockedUserKeys(userId).map { it.close() }
        return keys.size
    }
}
