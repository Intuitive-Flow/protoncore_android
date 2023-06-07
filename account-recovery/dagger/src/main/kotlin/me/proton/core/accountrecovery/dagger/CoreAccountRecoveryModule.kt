/*
 * Copyright (c) 2023 Proton AG
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

package me.proton.core.accountrecovery.dagger

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountrecovery.data.IsAccountRecoveryEnabledImpl
import me.proton.core.accountrecovery.domain.IsAccountRecoveryEnabled
import me.proton.core.accountrecovery.domain.ConfigureAccountRecoveryChannel
import me.proton.core.accountrecovery.presentation.ConfigureAccountRecoveryChannelImpl
import me.proton.core.accountrecovery.presentation.GetAccountRecoveryChannelIdImpl
import me.proton.core.accountrecovery.domain.GetAccountRecoveryChannelId

@Module
@InstallIn(SingletonComponent::class)
public interface CoreAccountRecoveryModule {
    @Binds
    public fun bindConfigureAccountRecoveryChannel(
        impl: ConfigureAccountRecoveryChannelImpl
    ): ConfigureAccountRecoveryChannel

    @Binds
    public fun bindGetAccountRecoveryChannelId(
        impl: GetAccountRecoveryChannelIdImpl
    ): GetAccountRecoveryChannelId

    @Binds
    public fun bindIsAccountRecoveryEnabled(
        impl: IsAccountRecoveryEnabledImpl
    ): IsAccountRecoveryEnabled
}
