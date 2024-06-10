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

package me.proton.core.eventmanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import me.proton.core.domain.entity.UserId
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.RefreshType
import me.proton.core.eventmanager.domain.entity.State
import me.proton.core.user.data.entity.UserEntity

@Entity(
    primaryKeys = ["userId", "config"],
    indices = [
        Index("userId"),
        Index("config"),
        Index("createdAt"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EventMetadataEntity(
    val userId: UserId,
    val config: EventManagerConfig,
    val eventId: String?,
    val nextEventId: String?,
    val refresh: RefreshType?,
    val more: Boolean?,
    val retry: Int,
    val state: State,
    val createdAt: Long,
    val updatedAt: Long?,
    val fetchedAt: Long?,
)
