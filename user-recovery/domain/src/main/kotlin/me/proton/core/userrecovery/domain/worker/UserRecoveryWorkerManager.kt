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

package me.proton.core.userrecovery.domain.worker

import me.proton.core.domain.entity.UserId

interface UserRecoveryWorkerManager {
    /**
     * Set the user primary key recovery secret, remotely, in background.
     *
     * Note: Once remotely set, local will be updated asap.
     */
    suspend fun enqueueSetRecoverySecret(userId: UserId)

    /**
     * Enqueues a worker that will try to recover user's inactive private keys.
     *
     * Note: the user object will be automatically refreshed, after the keys are recovered.
     */
    suspend fun enqueueRecoverInactivePrivateKeys(userId: UserId)
}
