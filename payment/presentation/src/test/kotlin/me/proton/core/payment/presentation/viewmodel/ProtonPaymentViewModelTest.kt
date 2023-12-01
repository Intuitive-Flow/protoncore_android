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

package me.proton.core.payment.presentation.viewmodel

import android.app.Activity
import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.launch
import me.proton.core.observability.domain.ObservabilityManager
import me.proton.core.payment.domain.entity.GooglePurchase
import me.proton.core.payment.domain.entity.ProtonPaymentToken
import me.proton.core.payment.domain.usecase.GetPreferredPaymentProvider
import me.proton.core.payment.domain.usecase.PaymentProvider
import me.proton.core.payment.presentation.viewmodel.ProtonPaymentViewModel.ButtonState
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.usecase.PerformGiapPurchase
import me.proton.core.presentation.app.ActivityProvider
import me.proton.core.test.kotlin.CoroutinesTest
import java.util.Optional
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProtonPaymentViewModelTest : CoroutinesTest by CoroutinesTest() {
    @MockK
    private lateinit var activityProvider: ActivityProvider

    @MockK
    private lateinit var getPreferredPaymentProvider: GetPreferredPaymentProvider

    @MockK
    private lateinit var observabilityManager: ObservabilityManager

    @MockK
    private lateinit var performGiapPurchase: PerformGiapPurchase<Activity>

    private lateinit var tested: ProtonPaymentViewModel

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        tested = ProtonPaymentViewModel(
            activityProvider,
            getPreferredPaymentProvider,
            observabilityManager,
            Optional.of(performGiapPurchase)
        )
    }

    @Test
    fun `cannot attach two buttons with the same id`() {
        tested.onButtonAttached(1)
        val error = assertFailsWith<IllegalArgumentException> {
            tested.onButtonAttached(1)
        }
        assertTrue(error.message?.startsWith("Cannot attach two buttons with the same ID") == true)
    }

    @Test
    fun `attaching and detaching buttons`() {
        tested.onButtonAttached(1)
        tested.onButtonDetached(1)
        tested.onButtonAttached(1)
    }

    @Test
    fun `initial state`() {
        assertEquals(
            ButtonState.Idle,
            tested.buttonStates(1).value
        )
    }

    @Test
    fun `paypal is not supported`() = coroutinesTest {
        // GIVEN
        val events = tested.paymentEvents(1)

        events.test {
            // WHEN
            tested.onPayClicked(1, "CHF", 12, PaymentProvider.PayPal, mockk(), null).join()

            // THEN
            assertEquals(ProtonPaymentEvent.Loading, awaitItem())
            assertIs<ProtonPaymentEvent.Error.UnsupportedPaymentProvider>(awaitItem())
        }
    }

    @Test
    fun `emits event to start regular payment flow`() = coroutinesTest {
        // GIVEN
        val plan = mockk<DynamicPlan>()
        val cycle = 12
        val currency = "CHF"
        val events = tested.paymentEvents(1)

        events.test {
            // WHEN
            tested.onPayClicked(1, currency, cycle, PaymentProvider.CardPayment, plan, null).join()

            // THEN
            assertEquals(ProtonPaymentEvent.Loading, awaitItem())
            assertEquals(
                ProtonPaymentEvent.StartRegularBillingFlow(plan, cycle, currency),
                awaitItem()
            )
        }
    }

    @Test
    fun `successful giap payment`() = coroutinesTest {
        // GIVEN
        val button1StatesFlow = tested.buttonStates(1)
        val button2StatesFlow = tested.buttonStates(2)
        val button1EventsFlow = tested.paymentEvents(1)
        val button2EventsFlow = tested.paymentEvents(2)

        val button1States = mutableListOf<ButtonState>()
        val button2States = mutableListOf<ButtonState>()
        val button1Events = mutableListOf<ProtonPaymentEvent>()
        val button2Events = mutableListOf<ProtonPaymentEvent>()

        val plan = mockk<DynamicPlan>()
        val amount = 499L
        val cycle = 12
        val currency = "CHF"
        val purchase = mockk<GooglePurchase>()
        val token = ProtonPaymentToken("payment-token")

        every { activityProvider.lastResumed } returns mockk()
        coEvery { performGiapPurchase(any(), any(), any(), any()) } returns
                PerformGiapPurchase.Result.GiapSuccess(
                    purchase,
                    amount,
                    currency,
                    subscriptionCreated = true,
                    token
                )

        val jobs = listOf(
            launch { button1StatesFlow.collect { button1States.add(it) } },
            launch { button2StatesFlow.collect { button2States.add(it) } },
            launch { button1EventsFlow.collect { button1Events.add(it) } },
            launch { button2EventsFlow.collect { button2Events.add(it) } }
        )

        // WHEN
        tested.onPayClicked(1, currency, cycle, PaymentProvider.GoogleInAppPurchase, plan, null)
            .join()
        jobs.map { it.cancel() }

        // THEN
        assertContentEquals(
            listOf(ButtonState.Idle, ButtonState.Loading, ButtonState.Idle),
            button1States
        )
        assertContentEquals(
            listOf(ButtonState.Idle, ButtonState.Disabled, ButtonState.Idle),
            button2States
        )
        assertContentEquals(
            listOf(
                ProtonPaymentEvent.Loading,
                ProtonPaymentEvent.GiapSuccess(
                    plan,
                    purchase,
                    amount,
                    currency,
                    cycle,
                    subscriptionCreated = true,
                    token
                )
            ),
            button1Events
        )
        assertTrue(button2Events.isEmpty())
    }
}