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

package me.proton.core.paymentiap.data.repository

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.payment.domain.repository.BillingClientError
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class ConnectedBillingClientTest {
    private lateinit var billingClient: BillingClient
    private lateinit var tested: ConnectedBillingClient

    @BeforeTest
    fun setUp() {
        billingClient = mockk(relaxed = true)
        tested = ConnectedBillingClient({ billingClient }, mockk())
    }

    @Test
    fun `connection established on demand`() = runTest {
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            tested.withClient { it.queryPurchasesAsync(mockk<QueryPurchasesParams>(), mockk()) }
        }
        verify(exactly = 1) { billingClient.startConnection(tested) }

        tested.onBillingSetupFinished(BillingResult())
        job.join()
        coVerify(exactly = 1) {
            billingClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                any()
            )
        }
    }

    @Test
    fun `connection already established`() = runTest {
        tested.onBillingSetupFinished(BillingResult())
        tested.withClient { it.queryPurchasesAsync(mockk<QueryPurchasesParams>(), mockk()) }

        verify(exactly = 0) { billingClient.startConnection(tested) }
        coVerify(exactly = 1) {
            billingClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                any()
            )
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `cannot use destroyed client`() = runTest {
        tested.destroy()
        tested.withClient { it.queryPurchasesAsync(mockk<QueryPurchasesParams>()) }
    }

    @Test
    fun `cannot establish connection`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(SupervisorJob()) {
            tested.withClient { it.queryPurchasesAsync(mockk<QueryPurchasesParams>()) }
        }

        tested.onBillingSetupFinished(
            BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.ERROR)
                .build()
        )
        assertFailsWith<BillingClientError> {
            result.await()
        }
    }

    @Test
    fun `reconnect after connection is lost`() = runTest {
        tested.onBillingSetupFinished(BillingResult())
        tested.onBillingServiceDisconnected()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            tested.withClient { it.queryPurchasesAsync(mockk<QueryPurchasesParams>(), mockk()) }
        }
        verify(exactly = 1) { billingClient.startConnection(tested) }

        tested.onBillingSetupFinished(BillingResult())
        job.join()

        coVerify(exactly = 1) {
            billingClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                any()
            )
        }
    }

    @Test
    fun `connection is lost`() = runTest {
        val deferredResult = async(UnconfinedTestDispatcher(testScheduler)) {
            assertFailsWith<BillingClientError> {
                tested.withClient { it.launchBillingFlow(mockk(), mockk()) }
            }
        }
        tested.onBillingServiceDisconnected()
        val error = deferredResult.await()

        assertEquals(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED, error.responseCode)
    }

    @Test
    fun `is already connecting`() = runTest {
        // GIVEN
        val job1 = launch(start = CoroutineStart.UNDISPATCHED) {
            tested.withClient { it.queryPurchasesAsync(mockk<QueryPurchasesParams>(), mockk()) }
        }
        val job2 = launch(start = CoroutineStart.UNDISPATCHED) {
            tested.withClient { it.launchBillingFlow(mockk(), mockk()) }
        }

        // THEN
        verify(exactly = 1) { billingClient.startConnection(tested) }

        tested.onBillingSetupFinished(BillingResult())
        job1.join()
        job2.join()

        coVerify(exactly = 1) {
            billingClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                any()
            )
        }
        coVerify(exactly = 1) { billingClient.launchBillingFlow(any(), any()) }
    }
}