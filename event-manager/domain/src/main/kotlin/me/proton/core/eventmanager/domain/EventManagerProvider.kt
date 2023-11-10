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

package me.proton.core.eventmanager.domain

import me.proton.core.domain.entity.UserId

interface EventManagerProvider {
    /**
     * Get an [EventManager] associated with the given [config].
     */
    suspend fun get(config: EventManagerConfig): EventManager

    /**
     * Get all [EventManager] associated with an existing [EventManagerConfig], by [userId].
     */
    suspend fun getAll(userId: UserId): List<EventManager>
}
