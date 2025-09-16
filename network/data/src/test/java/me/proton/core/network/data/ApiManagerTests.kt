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
package me.proton.core.network.data

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.util.MockApiClient
import me.proton.core.network.data.util.MockClientId
import me.proton.core.network.data.util.MockNetworkManager
import me.proton.core.network.data.util.MockNetworkPrefs
import me.proton.core.network.data.util.MockSession
import me.proton.core.network.data.util.MockSessionListener
import me.proton.core.network.data.util.TestResult
import me.proton.core.network.data.util.TestRetrofitApi
import me.proton.core.network.domain.ApiManager
import me.proton.core.network.domain.ApiManagerImpl
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.DohProvider
import me.proton.core.network.domain.DohService
import me.proton.core.network.domain.NetworkPrefs
import me.proton.core.network.domain.NetworkStatus
import me.proton.core.network.domain.ResponseCodes
import me.proton.core.network.domain.client.ClientId
import me.proton.core.network.domain.client.ClientIdProvider
import me.proton.core.network.domain.client.ClientVersionValidator
import me.proton.core.network.domain.deviceverification.DeviceVerificationListener
import me.proton.core.network.domain.deviceverification.DeviceVerificationProvider
import me.proton.core.network.domain.handlers.DohApiHandler
import me.proton.core.network.domain.humanverification.HumanVerificationListener
import me.proton.core.network.domain.humanverification.HumanVerificationProvider
import me.proton.core.network.domain.scopes.MissingScopeListener
import me.proton.core.network.domain.server.ServerTimeListener
import me.proton.core.network.domain.serverconnection.DohAlternativesListener
import me.proton.core.network.domain.session.Session
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

internal class ApiManagerTests {

    private val baseUrl = "https://primary.com/"
    private val proxy1url = "https://proxy1.com/"
    private val success5foo = ApiResult.Success(TestResult(5, "foo"))

    private lateinit var apiClient: MockApiClient

    private lateinit var networkManager: MockNetworkManager

    private lateinit var session: Session
    private lateinit var clientId: ClientId

    @MockK
    private lateinit var clientIdProvider: ClientIdProvider

    @MockK
    private lateinit var serverTimeListener: ServerTimeListener

    @MockK
    private lateinit var sessionProvider: SessionProvider

    private var sessionListener: SessionListener = MockSessionListener(
        onTokenRefreshed = { session -> this.session = session }
    )
    private val humanVerificationProvider = mockk<HumanVerificationProvider>()
    private val humanVerificationListener = mockk<HumanVerificationListener>()
    private val deviceVerificationProvider = mockk<DeviceVerificationProvider>()
    private val deviceVerificationListener = mockk<DeviceVerificationListener>()
    private val missingScopeListener = mockk<MissingScopeListener>(relaxed = true)

    private lateinit var apiManagerFactory: ApiManagerFactory
    private lateinit var apiManager: ApiManager<TestRetrofitApi>

    private lateinit var dohApiHandler: DohApiHandler<TestRetrofitApi>

    @MockK
    private lateinit var backend: ProtonApiBackend<TestRetrofitApi>

    @MockK
    private lateinit var altBackend1: ProtonApiBackend<TestRetrofitApi>

    @MockK
    private lateinit var dohService: DohService

    @MockK
    private lateinit var clientVersionValidator: ClientVersionValidator

    @MockK
    private lateinit var dohAlternativesListener: DohAlternativesListener

    @MockK
    private lateinit var protonDohService: DohService

    private var time = 0L
    private var wallTime = 0L

    private lateinit var prefs: NetworkPrefs
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @BeforeTest
    fun before() {
        MockKAnnotations.init(this)
        time = 0L

        prefs = MockNetworkPrefs()
        apiClient = MockApiClient()

        session = MockSession.getDefault()
        clientId = MockClientId.getForSession(session.sessionId)
        coEvery { clientIdProvider.getClientId(any()) } returns clientId
        coEvery { sessionProvider.getSessionId(any()) } answers {
            // If userId == null -> null (for unauthenticated session tests).
            if (firstArg<UserId?>() != null) session.sessionId else null
        }
        coEvery { sessionProvider.getSession(any()) } returns session
        DohProvider.lastAlternativesRefresh = Long.MIN_VALUE

        networkManager = MockNetworkManager()
        networkManager.networkStatus = NetworkStatus.Unmetered

        apiManagerFactory =
            ApiManagerFactory(
                baseUrl.toHttpUrl(),
                apiClient,
                clientIdProvider,
                serverTimeListener,
                networkManager,
                prefs,
                sessionProvider,
                sessionListener,
                humanVerificationProvider,
                humanVerificationListener,
                deviceVerificationProvider,
                deviceVerificationListener,
                missingScopeListener,
                mockk(),
                testScope,
                cache = { null },
                clientVersionValidator = clientVersionValidator,
                dohAlternativesListener = null,
                okHttpClient = mockk(relaxed = true),
                interceptors = emptySet(),
            )

        coEvery { dohService.getAlternativeBaseUrls(any(), any()) } returns listOf(proxy1url)
        val dohProvider = DohProvider(
            baseUrl,
            apiClient,
            listOf(dohService),
            emptyList(),
            { DohService { _, _ -> emptyList() } },
            protonDohService,
            testScope,
            prefs,
            ::time,
            null,
            dohAlternativesListener
        )
        DohProvider.lastAlternativesRefresh = Long.MIN_VALUE
        dohApiHandler = DohApiHandler(apiClient, backend, dohProvider, prefs, ::wallTime, ::time) {
            altBackend1
        }
        apiManager = ApiManagerImpl(
            apiClient, backend,
            apiManagerFactory.createBaseErrorHandlers(session.sessionId, ::time, dohApiHandler), ::time
        )

        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Success(TestResult(5, "foo"))
        every { altBackend1.baseUrl } returns proxy1url
    }

    @Test
    fun `test basic call`() = runTest(dispatcher) {
        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Success)
        assertEquals(5, result.value.number)
        assertEquals("foo", result.value.string)
    }

    @Test
    fun `test retry`() = runTest(dispatcher) {
        apiClient.shouldUseDoh = false
        apiClient.backoffRetryCount = 2
        coEvery { backend.invoke<TestResult>(any()) } returnsMany listOf(
            ApiResult.Error.Timeout(true),
            ApiResult.Error.Timeout(true),
            ApiResult.Success(TestResult(5, "foo"))
        )
        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Success)
        assertEquals(5, result.value.number)
    }

    @Test
    fun `test too many retries`() = runTest(dispatcher) {
        apiClient.shouldUseDoh = false
        apiClient.backoffRetryCount = 1
        coEvery { backend.invoke<TestResult>(any()) } returnsMany listOf(
            ApiResult.Error.Timeout(true),
            ApiResult.Error.Timeout(true),
            ApiResult.Success(TestResult(5, "foo"))
        )
        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error.Timeout)
    }

    @Test
    fun `test force no retry`() = runTest(dispatcher) {
        apiClient.shouldUseDoh = false
        coEvery { backend.invoke<TestResult>(any()) } returnsMany listOf(
            ApiResult.Error.Timeout(true),
            ApiResult.Success(TestResult(5, "foo"))
        )
        val result = apiManager.invoke(forceNoRetryOnConnectionErrors = true) { test() }
        assertTrue(result is ApiResult.Error.Timeout)
    }

    @Test
    fun `test force update app too old`() = runTest(dispatcher) {
        coEvery { backend.invoke<TestResult>(any()) } returns
            ApiResult.Error.Http(
                400, "",
                ApiResult.Error.ProtonData(ResponseCodes.APP_VERSION_BAD, "")
            )
        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error)
        assertEquals(true, apiClient.forceUpdated)
    }

    @Test
    fun `test force update api too old`() = runTest(dispatcher) {
        coEvery { backend.invoke<TestResult>(any()) } returns
            ApiResult.Error.Http(
                400, "",
                ApiResult.Error.ProtonData(ResponseCodes.API_VERSION_INVALID, "")
            )
        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error)
        assertEquals(true, apiClient.forceUpdated)
    }

    @Test
    fun `test too many requests recovered`() = runTest(dispatcher) {
        coEvery { backend.invoke<TestResult>(any()) } returnsMany listOf(
            ApiResult.Error.Http(429, "Too Many Request", retryAfter = 1.seconds),
            ApiResult.Error.Http(429, "Too Many Request", retryAfter = 2.seconds),
            ApiResult.Error.Http(429, "Too Many Request", retryAfter = 3.seconds),
            ApiResult.Success(TestResult(5, "foo"))
        )

        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error.Http)
        assertEquals(3.seconds, result.retryAfter)

        val result2 = apiManager.invoke { test() }
        assertTrue(result2 is ApiResult.Success)
    }

    @Test
    fun `service unavailable`() = runTest(dispatcher) {
        coEvery { backend.invoke<TestResult>(any()) } returnsMany listOf(
            ApiResult.Error.Http(503, "Service Unavailable", retryAfter = 60.seconds),
            ApiResult.Success(TestResult(5, "foo"))
        )

        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error.Http)
        assertEquals(60.seconds, result.retryAfter)

        val result2 = apiManager.invoke { test() }
        assertTrue(result2 is ApiResult.Success)
    }

    @Test
    fun `basic doh scenario`() = runTest(dispatcher) {
        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Error.Timeout(true)
        coEvery { backend.isPotentiallyBlocked() } returns true
        coEvery { altBackend1.invoke<TestResult>(any()) } returns success5foo

        val result1 = apiManager.invoke { test() }
        assertTrue(result1 is ApiResult.Success)
        assertEquals(altBackend1, dohApiHandler.getActiveAltBackend())

        val result2 = apiManager.invoke { test() }
        assertTrue(result2 is ApiResult.Success)
        // There was no call to primary backend as altBackend1 is active
        coVerify(exactly = 1) {
            backend.invoke<TestResult>(any())
        }

        // After proxy is no longer valid, attempt primary backend again
        wallTime += apiClient.proxyValidityPeriodMs
        assertNull(dohApiHandler.getActiveAltBackend())
        apiManager.invoke { test() }
        coVerify(exactly = 2) {
            backend.invoke<TestResult>(any())
        }
    }

    @Test
    fun `test doh ping ok`() = runTest(dispatcher) {
        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Error.Connection(true)
        // when isPotentiallyBlocked == false DoH logic won't be applied
        coEvery { backend.isPotentiallyBlocked() } returns false
        coEvery { altBackend1.invoke<TestResult>(any()) } returns success5foo

        val result = apiManager.invoke { test() }

        // Accept the error when pinging primary api succeeds
        assertTrue(result is ApiResult.Error.Connection)
    }

    @Test
    fun `test doh off`() = runTest(dispatcher) {
        apiClient.shouldUseDoh = false
        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Error.Connection(true)
        coEvery { backend.isPotentiallyBlocked() } returns true
        coEvery { altBackend1.invoke<TestResult>(any()) } returns success5foo

        val result = apiManager.invoke { test() }

        // Doh is off, no proxy should be called
        assertTrue(result is ApiResult.Error.Connection)
        coVerify(exactly = 0) { altBackend1.invoke<TestResult>(any()) }
    }

    @Test
    fun `test no DoH on client error`() = runTest(dispatcher) {
        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Error.Http(400, "")
        coEvery { backend.isPotentiallyBlocked() } returns true
        coEvery { altBackend1.invoke<TestResult>(any()) } returns success5foo

        val result = apiManager.invoke { test() }

        // HTTP 400 shouldn't trigger DoH
        assertTrue(result is ApiResult.Error.Http)
        coVerify(exactly = 0) { altBackend1.invoke<TestResult>(any()) }
    }

    @Test
    fun `test alternatives total timeout`() = runTest(dispatcher) {
        time = 100_000L // this will set the api call timestamp to 100K
        val error = ApiResult.Error.Connection(true)
        coEvery { backend.invoke<TestResult>(any()) } coAnswers { error }

        coEvery { dohService.getAlternativeBaseUrls(any(), any()) } returns
            listOf("https://proxy1.com/", "https://proxy2.com/")
        coEvery { backend.isPotentiallyBlocked() } returns true
        coEvery { altBackend1.invoke<TestResult>(any()) } coAnswers {
            time += apiClient.alternativesTotalTimeout + 1
            ApiResult.Error.Connection(true)
        }

        val result = apiManager.invoke(forceNoRetryOnConnectionErrors = true) { test() }
        coVerify(exactly = 1) {
            altBackend1.invoke<TestResult>(any())
        }
        assertEquals(error, result)
    }

    @Test
    fun `test doh proxy refresh throttling`() = runTest(dispatcher) {
        coEvery { dohAlternativesListener.onAlternativesUnblock(any()) } returns Unit
        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Error.Connection(true)
        coEvery { backend.isPotentiallyBlocked() } returns true
        coEvery { altBackend1.invoke<TestResult>(any()) } returns ApiResult.Error.Connection(true)

        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error.Connection)

        val result2 = apiManager.invoke { test() }
        assertTrue(result2 is ApiResult.Error.Connection)

        time += DohProvider.MIN_REFRESH_INTERVAL_MS
        val result3 = apiManager.invoke { test() }
        assertTrue(result3 is ApiResult.Error.Connection)

        coVerify(exactly = 2) {
            dohService.getAlternativeBaseUrls(any(), any())
        }
    }

    @Test
    fun `test doh alternatives failed but client supports gh`() = runTest(dispatcher) {
        val dohProvider = DohProvider(
            baseUrl,
            apiClient,
            listOf(dohService),
            emptyList(),
            { DohService { _, _ -> emptyList() } },
            protonDohService,
            testScope,
            prefs,
            ::time,
            null,
            dohAlternativesListener
        )
        dohApiHandler = DohApiHandler(apiClient, backend, dohProvider, prefs, ::wallTime, ::time) {
            altBackend1
        }
        apiManager = ApiManagerImpl(
            apiClient, backend,
            apiManagerFactory.createBaseErrorHandlers(session.sessionId, ::time, dohApiHandler), ::time
        )

        val lambda = slot<suspend () -> Unit>()
        coEvery { dohAlternativesListener.onAlternativesUnblock(capture(lambda)) } coAnswers {
            lambda.captured.invoke()
        }
        coEvery { dohService.getAlternativeBaseUrls(any(), any()) } returns null
        coEvery { protonDohService.getAlternativeBaseUrls(any(), any()) } returns listOf(proxy1url)
        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Error.Connection(true)
        coEvery { backend.isPotentiallyBlocked() } returns true
        coEvery { altBackend1.invoke<TestResult>(any()) } returns ApiResult.Error.Connection(true)

        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error.Connection)

        coVerify {
            dohService.getAlternativeBaseUrls(any(), any())
        }

        coVerify {
            protonDohService.getAlternativeBaseUrls(any(), any())
        }
    }

    @Test
    fun `test exception thrown doh is working properly`() = runTest(dispatcher) {
        val dohProvider = DohProvider(
            baseUrl,
            apiClient,
            listOf(dohService),
            emptyList(),
            { DohService { _, _ -> emptyList() } },
            protonDohService,
            testScope,
            prefs,
            ::time,
            null,
            dohAlternativesListener
        )
        dohApiHandler = DohApiHandler(apiClient, backend, dohProvider, prefs, ::wallTime, ::time) {
            altBackend1
        }
        apiManager = ApiManagerImpl(
            apiClient, backend,
            apiManagerFactory.createBaseErrorHandlers(session.sessionId, ::time, dohApiHandler), ::time
        )

        val lambda = slot<suspend () -> Unit>()
        coEvery { dohAlternativesListener.onAlternativesUnblock(capture(lambda)) } coAnswers {
            lambda.captured.invoke()
        }
        coEvery { dohService.getAlternativeBaseUrls(any(), any()) } returns null
        coEvery { protonDohService.getAlternativeBaseUrls(any(), any()) } returns listOf(proxy1url)
        coEvery { backend.invoke<TestResult>(any()) } returns ApiResult.Error.Parse(RuntimeException("test"))
        coEvery { backend.isPotentiallyBlocked() } returns true
        coEvery { altBackend1.invoke<TestResult>(any()) } returns ApiResult.Error.Connection(true)

        val result = apiManager.invoke { test() }
        assertTrue(result is ApiResult.Error.Parse)

        coVerify(exactly = 0) {
            dohService.getAlternativeBaseUrls(any(), any())
        }

        coVerify(exactly = 0) {
            protonDohService.getAlternativeBaseUrls(any(), any())
        }
    }
}
