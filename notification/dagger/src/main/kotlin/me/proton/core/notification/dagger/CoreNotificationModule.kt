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

package me.proton.core.notification.dagger

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.notification.data.local.NotificationLocalDataSourceImpl
import me.proton.core.notification.data.remote.NotificationRemoteDataSourceImpl
import me.proton.core.notification.data.repository.NotificationRepositoryImpl
import me.proton.core.notification.domain.repository.NotificationLocalDataSource
import me.proton.core.notification.domain.repository.NotificationRemoteDataSource
import me.proton.core.notification.domain.repository.NotificationRepository
import me.proton.core.notification.domain.usecase.CancelNotificationView
import me.proton.core.notification.domain.usecase.ConfigureNotificationChannel
import me.proton.core.notification.domain.usecase.GetNotificationChannelId
import me.proton.core.notification.domain.usecase.IsNotificationsEnabled
import me.proton.core.notification.domain.usecase.IsNotificationsPermissionRequestEnabled
import me.proton.core.notification.domain.usecase.IsNotificationsPermissionShowRationale
import me.proton.core.notification.domain.usecase.ObservePushNotifications
import me.proton.core.notification.domain.usecase.ReplaceNotificationViews
import me.proton.core.notification.domain.usecase.ShowNotificationView
import me.proton.core.notification.presentation.deeplink.DeeplinkIntentProvider
import me.proton.core.notification.presentation.deeplink.DeeplinkIntentProviderImpl
import me.proton.core.notification.presentation.usecase.CancelNotificationViewImpl
import me.proton.core.notification.presentation.usecase.ConfigureNotificationChannelImpl
import me.proton.core.notification.presentation.usecase.GetNotificationChannelIdImpl
import me.proton.core.notification.presentation.usecase.IsNotificationsEnabledImpl
import me.proton.core.notification.presentation.usecase.IsNotificationsPermissionRequestEnabledImpl
import me.proton.core.notification.presentation.usecase.IsNotificationsPermissionShowRationaleImpl
import me.proton.core.notification.presentation.usecase.ObservePushNotificationsImpl
import me.proton.core.notification.presentation.usecase.ReplaceNotificationViewsImpl
import me.proton.core.notification.presentation.usecase.ShowNotificationViewImpl

@Module
@InstallIn(SingletonComponent::class)
public interface CoreNotificationModule {
    @Binds
    public fun bindCancelNotification(impl: CancelNotificationViewImpl): CancelNotificationView

    @Binds
    public fun bindConfigureNotificationChannel(
        impl: ConfigureNotificationChannelImpl
    ): ConfigureNotificationChannel

    @Binds
    public fun bindGetNotificationChannelId(
        impl: GetNotificationChannelIdImpl
    ): GetNotificationChannelId

    @Binds
    public fun bindIsNotificationsEnabled(
        impl: IsNotificationsEnabledImpl
    ): IsNotificationsEnabled

    @Binds
    public fun bindIsNotificationsPermissionRequestEnabled(
        impl: IsNotificationsPermissionRequestEnabledImpl
    ): IsNotificationsPermissionRequestEnabled

    @Binds
    public fun bindIsNotificationsPermissionShowRationale(
        impl: IsNotificationsPermissionShowRationaleImpl
    ): IsNotificationsPermissionShowRationale

    @Binds
    public fun bindNotificationLocalDataSource(impl: NotificationLocalDataSourceImpl): NotificationLocalDataSource

    @Binds
    public fun bindNotificationRemoteDataSource(impl: NotificationRemoteDataSourceImpl): NotificationRemoteDataSource

    @Binds
    public fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    public fun bindObservePushNotifications(
        impl: ObservePushNotificationsImpl
    ): ObservePushNotifications

    @Binds
    public fun bindReplaceNotifications(
        impl: ReplaceNotificationViewsImpl
    ): ReplaceNotificationViews

    @Binds
    public fun bindShowNotification(impl: ShowNotificationViewImpl): ShowNotificationView
}

@Module
@InstallIn(SingletonComponent::class)
public interface CoreNotificationDeeplinkModule {
    @Binds
    public fun bindDeeplinkIntentProvider(impl: DeeplinkIntentProviderImpl): DeeplinkIntentProvider
}
