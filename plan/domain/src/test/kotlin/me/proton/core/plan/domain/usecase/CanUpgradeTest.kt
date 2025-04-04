/*
 * Copyright (c) 2024 Proton Technologies AG
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

package me.proton.core.plan.domain.usecase

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.AppStore
import me.proton.core.domain.type.IntEnum
import me.proton.core.payment.domain.usecase.GetAvailablePaymentProviders
import me.proton.core.payment.domain.usecase.GoogleServicesAvailability
import me.proton.core.payment.domain.usecase.GoogleServicesUtils
import me.proton.core.payment.domain.usecase.PaymentProvider
import me.proton.core.plan.domain.entity.DynamicPlan
import me.proton.core.plan.domain.entity.DynamicPlanInstance
import me.proton.core.plan.domain.entity.DynamicPlanPrice
import me.proton.core.plan.domain.entity.DynamicPlanState
import me.proton.core.plan.domain.entity.DynamicPlanType
import me.proton.core.plan.domain.entity.DynamicPlanVendor
import me.proton.core.plan.domain.entity.DynamicPlans
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanUpgradeTest {
    // region mocks
    @MockK
    private lateinit var getPlans: GetDynamicPlans

    @MockK
    private lateinit var getAvailablePaymentProviders: GetAvailablePaymentProviders

    @MockK
    private lateinit var googleServicesUtils: GoogleServicesUtils

    @MockK
    private lateinit var optionalGoogleServicesUtils: Optional<GoogleServicesUtils>
    // endregion

    // region test data
    private fun instanceFor(cycle: Int, vararg currency: String) = DynamicPlanInstance(
        cycle = cycle,
        description = "description-$cycle-$currency",
        periodEnd = Instant.now(),
        price = currency.map {
            DynamicPlanPrice(
                id = "id-$cycle-$currency",
                currency = it,
                current = 100
            )
        }.associateBy { it.currency },
        vendors = mapOf(
            AppStore.GooglePlay to DynamicPlanVendor(
                productId = "googlemail_plus_${cycle}_renewing",
                customerId = "cus_google_fAx9TIdL63UmeYDmUo3l"
            )
        )
    )

    private val planFree = DynamicPlan(
        name = "free",
        order = 0,
        state = DynamicPlanState.Available,
        title = "Free",
        type = null,
        instances = emptyMap(), // No instances.
    )

    private val plan1 = DynamicPlan(
        name = "test1",
        order = 0,
        state = DynamicPlanState.Available,
        title = "title",
        type = IntEnum(DynamicPlanType.Primary.code, DynamicPlanType.Primary),
        instances = mapOf(
            12 to instanceFor(12, "CHF", "USD", "EUR"),
            24 to instanceFor(24, "CHF")
        ),
    )
    // endregion

    private lateinit var useCase: CanUpgrade

    @Before
    fun beforeEveryTest() {
        MockKAnnotations.init(this)
        useCase = CanUpgrade(
            supportPaidPlans = true,
            getPlans = getPlans,
            getAvailablePaymentProviders = getAvailablePaymentProviders,
            googleServicesUtils = optionalGoogleServicesUtils
        )
    }

    @Test
    fun `can upgrade returns false when support paid is false`() = runTest {
        // GIVEN
        useCase = CanUpgrade(
            supportPaidPlans = false,
            getPlans = getPlans,
            getAvailablePaymentProviders = getAvailablePaymentProviders,
            googleServicesUtils = optionalGoogleServicesUtils
        )
        // WHEN
        val result = useCase()
        // THEN
        assertFalse(result)
    }

    @Test
    fun `can upgrade returns false when no payment providers available`() = runTest {
        // GIVEN
        coEvery { getAvailablePaymentProviders() } returns emptySet()
        coEvery { getPlans(any()) } returns DynamicPlans(defaultCycle = 12, plans = listOf(plan1))
        // WHEN
        val result = useCase()
        // THEN
        assertFalse(result)
    }

    @Test
    fun `can upgrade returns false when only PayPal payment provider is available`() = runTest {
        // GIVEN
        coEvery { getAvailablePaymentProviders() } returns setOf(PaymentProvider.PayPal)
        coEvery { getPlans(any()) } returns DynamicPlans(defaultCycle = 12, plans = listOf(plan1))
        // WHEN
        val result = useCase()
        // THEN
        assertFalse(result)
    }

    @Test
    fun `can upgrade returns false when no paid plans available`() = runTest {
        // GIVEN
        every { optionalGoogleServicesUtils.getOrNull() } returns googleServicesUtils
        every { googleServicesUtils.isGooglePlayServicesAvailable() } returns GoogleServicesAvailability.Success
        coEvery { getAvailablePaymentProviders() } returns setOf(
            PaymentProvider.CardPayment,
            PaymentProvider.GoogleInAppPurchase
        )
        coEvery { getPlans(any()) } returns DynamicPlans(12, emptyList())
        // WHEN
        val result = useCase()
        // THEN
        assertFalse(result)
    }

    @Test
    fun `can upgrade returns true when paid plans available and payment providers available`() = runTest {
        // GIVEN
        every { optionalGoogleServicesUtils.getOrNull() } returns googleServicesUtils
        every { googleServicesUtils.isGooglePlayServicesAvailable() } returns GoogleServicesAvailability.Success
        coEvery { getAvailablePaymentProviders() } returns setOf(
            PaymentProvider.CardPayment,
            PaymentProvider.GoogleInAppPurchase
        )
        coEvery { getPlans(any()) } returns DynamicPlans(defaultCycle = 12, plans = listOf(plan1))
        // WHEN
        val result = useCase()
        // THEN
        assertTrue(result)
    }

    @Test
    fun `cannot upgrade if google services not available`() = runTest {
        // GIVEN
        every { optionalGoogleServicesUtils.getOrNull() } returns googleServicesUtils
        every { googleServicesUtils.isGooglePlayServicesAvailable() } returns GoogleServicesAvailability.ServiceInvalid
        coEvery { getAvailablePaymentProviders() } returns setOf(
            PaymentProvider.GoogleInAppPurchase
        )
        // WHEN
        val result = useCase()
        // THEN
        assertFalse(result)
    }
}