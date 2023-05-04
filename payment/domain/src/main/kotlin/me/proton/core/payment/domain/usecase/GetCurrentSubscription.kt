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

package me.proton.core.payment.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.HttpResponseCodes
import me.proton.core.network.domain.ResponseCodes
import me.proton.core.network.domain.ResponseCodes.PAYMENTS_SUBSCRIPTION_NOT_EXISTS
import me.proton.core.observability.domain.ObservabilityManager
import me.proton.core.observability.domain.metrics.CheckoutGetSubscriptionTotal
import me.proton.core.observability.domain.metrics.common.toHttpApiStatus
import me.proton.core.observability.domain.metrics.toGetSubscriptionApiStatus
import me.proton.core.payment.domain.entity.Subscription
import me.proton.core.payment.domain.repository.PaymentsRepository
import javax.inject.Inject

/**
 * Gets current active subscription a user has.
 * For free users this will return Null.
 * Authorized. This means that it could only be used for upgrades. New accounts created during sign ups logically do not
 * have existing subscriptions.
 */
public class GetCurrentSubscription @Inject constructor(
    private val paymentsRepository: PaymentsRepository,
    private val observabilityManager: ObservabilityManager
) {
    public suspend operator fun invoke(userId: UserId): Subscription? {
        val result = try {
            paymentsRepository.getSubscription(userId)
        } catch (exception: ApiException) {
            val error = exception.error
            if (error is ApiResult.Error.Http && error.proton?.code == PAYMENTS_SUBSCRIPTION_NOT_EXISTS) {
                null
            } else {
                val status = exception.toHttpApiStatus().toGetSubscriptionApiStatus()
                observabilityManager.enqueue(
                    CheckoutGetSubscriptionTotal(status)
                )
                throw exception
            }
        }
        val status = if (result != null) {
            CheckoutGetSubscriptionTotal.ApiStatus.http2xx
        } else {
            CheckoutGetSubscriptionTotal.ApiStatus.failureNoSubscription
        }
        observabilityManager.enqueue(CheckoutGetSubscriptionTotal(status))
        return result
    }
}

public fun Result<*>.toProtonErrorCode(filterByHttpCode: Int): Int? =
    ((exceptionOrNull() as? ApiException)?.error as? ApiResult.Error.Http)
        ?.takeIf { it.httpCode == filterByHttpCode }
        ?.proton
        ?.code
