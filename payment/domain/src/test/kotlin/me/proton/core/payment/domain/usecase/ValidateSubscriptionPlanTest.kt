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
import me.proton.core.observability.domain.metrics.CheckoutCardBillingValidatePlanTotal
import me.proton.core.observability.domain.metrics.common.HttpApiStatus
import me.proton.core.observability.domain.metrics.common.toHttpApiStatus
import me.proton.core.payment.domain.entity.Currency
import me.proton.core.payment.domain.entity.SubscriptionCycle
import me.proton.core.payment.domain.entity.SubscriptionStatus
import me.proton.core.payment.domain.repository.PaymentsRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ValidateSubscriptionPlanTest {

    // region mocks
    private val repository = mockk<PaymentsRepository>(relaxed = true)
    private val observabilityManager = mockk<ObservabilityManager>(relaxed = true)
    // endregion

    // region test data
    private val testUserId = UserId("test-user-id")
    private val testAmount = 5L
    private val testAmountDue = 3L
    private val testCredit = 2L
    private val testPlanName = "test-plan-name"

    private val defaultSubscriptionStatus = SubscriptionStatus(
        amount = testAmount,
        amountDue = testAmountDue,
        proration = 0,
        couponDiscount = 0,
        coupon = null,
        credit = testCredit,
        currency = Currency.CHF,
        cycle = SubscriptionCycle.YEARLY,
        gift = null
    )
    // endregion

    private lateinit var useCase: ValidateSubscriptionPlan

    @Before
    fun beforeEveryTest() {
        useCase = ValidateSubscriptionPlan(repository, observabilityManager)
        coEvery { repository.validateSubscription(any(), any(), any(), any(), any()) } returns defaultSubscriptionStatus
    }

    @Test
    fun `upgrade plan success test`() = runTest {
        val result = useCase.invoke(
            userId = testUserId,
            codes = null,
            plans = listOf(testPlanName),
            currency = Currency.CHF,
            cycle = SubscriptionCycle.YEARLY
        )
        coVerify(exactly = 1) {
            repository.validateSubscription(
                testUserId,
                null,
                mapOf(testPlanName to 1),
                Currency.CHF,
                SubscriptionCycle.YEARLY
            )
        }
        assertNotNull(result)
        assertEquals(testAmountDue, result.amountDue)
    }

    @Test
    fun `sign up payment success test`() = runTest {
        val user = null
        val result = useCase.invoke(
            userId = user,
            codes = null,
            plans = listOf(testPlanName),
            currency = Currency.CHF,
            cycle = SubscriptionCycle.YEARLY
        )
        coVerify(exactly = 1) {
            repository.validateSubscription(
                user,
                null,
                mapOf(testPlanName to 1),
                Currency.CHF,
                SubscriptionCycle.YEARLY
            )
        }
        assertNotNull(result)
        assertEquals(testAmountDue, result.amountDue)
    }

    @Test
    fun `sign up payment no plans error handled correctly`() = runTest {
        val user = null
        assertFailsWith(IllegalArgumentException::class) {
            useCase.invoke(
                userId = user,
                codes = null,
                plans = listOf(),
                currency = Currency.CHF,
                cycle = SubscriptionCycle.YEARLY
            )
        }
    }

    @Test
    fun `observability data is recorded`() = runTest {
        useCase.invoke(
            userId = null,
            codes = null,
            plans = listOf(testPlanName),
            currency = Currency.CHF,
            cycle = SubscriptionCycle.YEARLY,
            metricData = { CheckoutCardBillingValidatePlanTotal(it.toHttpApiStatus()) }
        )

        val dataSlot = slot<CheckoutCardBillingValidatePlanTotal>()
        verify { observabilityManager.enqueue(capture(dataSlot), any()) }
        assertEquals(HttpApiStatus.http2xx, dataSlot.captured.Labels.status)
    }
}
