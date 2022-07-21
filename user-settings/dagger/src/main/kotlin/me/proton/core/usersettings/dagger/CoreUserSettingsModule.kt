/*
 * Copyright (c) 2022 Proton Technologies AG
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

package me.proton.core.usersettings.dagger

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.network.data.ApiProvider
import me.proton.core.usersettings.data.db.OrganizationDatabase
import me.proton.core.usersettings.data.db.UserSettingsDatabase
import me.proton.core.usersettings.data.repository.OrganizationRepositoryImpl
import me.proton.core.usersettings.data.repository.UserSettingsRepositoryImpl
import me.proton.core.usersettings.domain.repository.OrganizationRepository
import me.proton.core.usersettings.domain.repository.UserSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
public object CoreUserSettingsModule {

    @Provides
    @Singleton
    public fun provideUserSettingsRepository(
        db: UserSettingsDatabase,
        apiProvider: ApiProvider
    ): UserSettingsRepository =
        UserSettingsRepositoryImpl(db, apiProvider)

    @Provides
    @Singleton
    public fun provideOrganizationRepository(
        db: OrganizationDatabase,
        apiProvider: ApiProvider
    ): OrganizationRepository =
        OrganizationRepositoryImpl(db, apiProvider)
}