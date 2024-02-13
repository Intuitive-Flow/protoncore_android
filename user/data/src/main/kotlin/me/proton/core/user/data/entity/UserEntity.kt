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

package me.proton.core.user.data.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.domain.entity.UserId

@Entity(
    primaryKeys = ["userId"],
    indices = [
        Index("userId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserEntity(
    val userId: UserId,
    val email: String?,
    val name: String?,
    val displayName: String?,
    val currency: String,
    val credit: Int,
    val createdAtUtc: Long,
    val usedSpace: Long,
    val maxSpace: Long,
    val maxUpload: Long,
    val type: Int?,
    val role: Int?,
    @ColumnInfo(name = "private")
    val isPrivate: Boolean,
    val subscribed: Int,
    val services: Int,
    val delinquent: Int?,
    @Embedded(prefix = "recovery_")
    val recovery: UserRecoveryEntity?,
    val passphrase: EncryptedByteArray?,
    val maxBaseSpace: Long?,
    val maxDriveSpace: Long?,
    val usedBaseSpace: Long?,
    val usedDriveSpace: Long?
)
