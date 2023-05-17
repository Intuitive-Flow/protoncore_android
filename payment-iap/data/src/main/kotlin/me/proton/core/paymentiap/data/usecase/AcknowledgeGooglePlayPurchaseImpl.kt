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

package me.proton.core.paymentiap.data.usecase

import me.proton.core.observability.domain.ObservabilityManager
import me.proton.core.observability.domain.metrics.CheckoutGiapBillingAcknowledgeTotal
import me.proton.core.observability.domain.metrics.ObservabilityData
import me.proton.core.observability.domain.runWithObservability
import me.proton.core.payment.domain.entity.GooglePurchaseToken
import me.proton.core.payment.domain.entity.ProtonPaymentToken
import me.proton.core.payment.domain.repository.GooglePurchaseRepository
import me.proton.core.payment.domain.usecase.AcknowledgeGooglePlayPurchase
import me.proton.core.paymentiap.domain.repository.GoogleBillingRepository
import me.proton.core.paymentiap.domain.toGiapStatus
import javax.inject.Inject
import javax.inject.Provider

public class AcknowledgeGooglePlayPurchaseImpl @Inject constructor(
    private val googleBillingRepositoryProvider: Provider<GoogleBillingRepository>,
    private val googlePurchaseRepository: GooglePurchaseRepository,
    private val observabilityManager: ObservabilityManager
) : AcknowledgeGooglePlayPurchase {
    override suspend fun invoke(paymentToken: ProtonPaymentToken) {
        val googlePurchaseToken = requireNotNull(googlePurchaseRepository.findGooglePurchaseToken(paymentToken)) {
            "Could not find corresponding Google purchase token."
        }
        invoke(googlePurchaseToken)
    }

    override suspend fun invoke(purchaseToken: GooglePurchaseToken) {
        val metricData: ((Result<Unit>) -> ObservabilityData?) =
            { result -> result.toGiapStatus()?.let { CheckoutGiapBillingAcknowledgeTotal(it) } }

        // Create a new instance of `GoogleBillingRepository` and clean up after it:
        googleBillingRepositoryProvider.get().use { googleBillingRepository ->
            googleBillingRepository.runWithObservability(observabilityManager, metricData) {
                acknowledgePurchase(purchaseToken)
            }
        }
        googlePurchaseRepository.deleteByGooglePurchaseToken(purchaseToken)
    }
}
