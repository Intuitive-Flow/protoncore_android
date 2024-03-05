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

package me.proton.core.featureflag.dagger

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.featureflag.data.FeatureFlagManagerImpl
import me.proton.core.featureflag.data.local.FeatureFlagLocalDataSourceImpl
import me.proton.core.featureflag.data.remote.FeatureFlagRemoteDataSourceImpl
import me.proton.core.featureflag.data.repository.FeatureFlagRepositoryImpl
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.repository.FeatureFlagContextProvider
import me.proton.core.featureflag.domain.repository.FeatureFlagLocalDataSource
import me.proton.core.featureflag.domain.repository.FeatureFlagRemoteDataSource
import me.proton.core.featureflag.domain.repository.FeatureFlagRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CoreFeatureFlagModule {

    @Binds
    @Singleton
    abstract fun bindFeatureFlagLocalDataSource(impl: FeatureFlagLocalDataSourceImpl): FeatureFlagLocalDataSource

    @Binds
    @Singleton
    abstract fun bindFeatureFlagRemoteDataSource(impl: FeatureFlagRemoteDataSourceImpl): FeatureFlagRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindRepository(featureFlagRepositoryImpl: FeatureFlagRepositoryImpl): FeatureFlagRepository

    @Binds
    @Singleton
    abstract fun bindManager(featureFlagManagerImpl: FeatureFlagManagerImpl): FeatureFlagManager

    @BindsOptionalOf
    abstract fun optionalFeatureFlagContextProvider(): FeatureFlagContextProvider
}
