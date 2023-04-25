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

package me.proton.core.payment.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.observability.domain.ObservabilityManager
import me.proton.core.observability.domain.metrics.CheckoutPaymentMethodsCreatePaymentTokenTotal
import me.proton.core.observability.domain.metrics.common.HttpApiStatus
import me.proton.core.observability.domain.metrics.common.toHttpApiStatus
import me.proton.core.payment.domain.entity.Currency
import me.proton.core.payment.domain.entity.PaymentTokenResult
import me.proton.core.payment.domain.entity.PaymentTokenStatus
import me.proton.core.payment.domain.entity.ProtonPaymentToken
import me.proton.core.payment.domain.repository.PaymentsRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CreatePaymentTokenWithExistingPaymentMethodTestWithNewCreditCard {
    // region mocks
    private val observabilityManager = mockk<ObservabilityManager>(relaxed = true)
    private val repository = mockk<PaymentsRepository>(relaxed = true)
    // endregion

    // region test data
    private val testUserId = UserId("test-user-id")
    private val testAmount = 5L
    private val testCurrency = Currency.CHF
    private val testPaymentMethodId = "test-paymentMethodID"
    private val testToken = ProtonPaymentToken("test-token")
    private val testApprovalUrl = "test-approval-url"
    private val testReturnHost = "test-return-host"
    private val createTokenResult = PaymentTokenResult.CreatePaymentTokenResult(
        PaymentTokenStatus.PENDING, testApprovalUrl, testToken, testReturnHost
    )
    // endregion

    private lateinit var useCase: CreatePaymentTokenWithExistingPaymentMethod

    @Before
    fun beforeEveryTest() {
        useCase = CreatePaymentTokenWithExistingPaymentMethod(repository, observabilityManager)
        coEvery {
            repository.createPaymentTokenExistingPaymentMethod(any(), any(), any(), any())
        } returns createTokenResult
    }

    @Test
    fun `create payment token upgrade success response`() = runTest {
        val result = useCase.invoke(testUserId, testAmount, testCurrency, testPaymentMethodId)

        coVerify(exactly = 1) {
            repository.createPaymentTokenExistingPaymentMethod(
                sessionUserId = testUserId,
                amount = testAmount,
                currency = testCurrency,
                paymentMethodId = testPaymentMethodId
            )
        }

        assertNotNull(result)
        assertEquals(testToken, result.token)
        assertEquals(PaymentTokenStatus.PENDING, result.status)
        assertEquals(testApprovalUrl, result.approvalUrl)
        assertEquals(testReturnHost, result.returnHost)
    }

    @Test
    fun `create payment token sign up success response`() = runTest {
        val result = useCase.invoke(null, testAmount, testCurrency, testPaymentMethodId)

        coVerify(exactly = 1) {
            repository.createPaymentTokenExistingPaymentMethod(
                sessionUserId = null,
                amount = testAmount,
                currency = testCurrency,
                paymentMethodId = testPaymentMethodId
            )
        }

        assertNotNull(result)
        assertEquals(testToken, result.token)
        assertEquals(PaymentTokenStatus.PENDING, result.status)
        assertEquals(testApprovalUrl, result.approvalUrl)
        assertEquals(testReturnHost, result.returnHost)
    }

    @Test
    fun `create token returns chargeable`() = runTest {
        val createTokenChargeableResult = PaymentTokenResult.CreatePaymentTokenResult(
            PaymentTokenStatus.CHARGEABLE, null, testToken, null
        )
        coEvery {
            repository.createPaymentTokenExistingPaymentMethod(
                testUserId,
                testAmount,
                testCurrency,
                testPaymentMethodId
            )
        } returns createTokenChargeableResult

        val result = useCase.invoke(testUserId, testAmount, testCurrency, testPaymentMethodId)
        assertNotNull(result)
        assertEquals(testToken, result.token)
        assertEquals(PaymentTokenStatus.CHARGEABLE, result.status)
        assertNull(result.approvalUrl)
        assertNull(result.returnHost)
    }

    @Test
    fun `create payment token negative amount response`() = runTest {
        assertFailsWith(IllegalArgumentException::class) {
            useCase.invoke(testUserId, -1, testCurrency, testPaymentMethodId)
        }
    }

    @Test
    fun `observability metrics are recorded`() = runTest {
        useCase.invoke(
            userId = testUserId,
            amount = testAmount,
            currency = testCurrency,
            paymentMethodId = testPaymentMethodId,
            metricData = { CheckoutPaymentMethodsCreatePaymentTokenTotal(it.toHttpApiStatus()) }
        )

        val dataSlot = slot<CheckoutPaymentMethodsCreatePaymentTokenTotal>()
        verify { observabilityManager.enqueue(capture(dataSlot), any()) }
        assertEquals(HttpApiStatus.http2xx, dataSlot.captured.Labels.status)
    }
}
