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

package me.proton.core.payment.data.api.request

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representative request body for calling to create a Proton Payment Token via the /tokens
 * endpoint.
 *
 * Please note: [amount] and [currency] are no longer used in the Back End system, therefore they
 * are marked as deprecated. The API has not remove these properties as required fields so for now
 * we pass along hard-coded values.
 */
@Deprecated("Use CreateOmnichannelPaymentToken instead")
@Serializable
internal data class CreatePaymentToken(
    @Deprecated("No longer used by the back end system")
    @EncodeDefault
    @SerialName("Amount")
    val amount: Long = 0L,
    @Deprecated("No longer used by the back end system")
    @EncodeDefault
    @SerialName("Currency")
    val currency: String = "CHF",
    @SerialName("Payment")
    val paymentEntity: PaymentTypeEntity?,
    @SerialName("PaymentMethodID")
    val paymentMethodId: String?
)
