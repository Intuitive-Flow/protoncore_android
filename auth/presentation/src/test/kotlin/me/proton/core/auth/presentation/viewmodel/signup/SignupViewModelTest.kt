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

package me.proton.core.auth.presentation.viewmodel.signup

import app.cash.turbine.test
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.auth.domain.usecase.PerformLogin
import me.proton.core.auth.domain.usecase.signup.PerformCreateExternalEmailUser
import me.proton.core.auth.domain.usecase.signup.PerformCreateUser
import me.proton.core.auth.domain.usecase.signup.SetCreateAccountSuccess
import me.proton.core.auth.domain.usecase.signup.SignupChallengeConfig
import me.proton.core.auth.presentation.entity.signup.RecoveryMethod
import me.proton.core.auth.presentation.entity.signup.RecoveryMethodType
import me.proton.core.auth.presentation.entity.signup.SubscriptionDetails
import me.proton.core.challenge.domain.ChallengeManager
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.srp.SrpCrypto
import me.proton.core.domain.entity.UserId
import me.proton.core.humanverification.domain.HumanVerificationExternalInput
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.ResponseCodes
import me.proton.core.network.domain.client.ClientId
import me.proton.core.network.domain.client.ClientIdProvider
import me.proton.core.network.domain.client.CookieSessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.observability.domain.ObservabilityManager
import me.proton.core.observability.domain.metrics.SignupAccountCreationTotal
import me.proton.core.payment.domain.entity.Currency
import me.proton.core.payment.domain.entity.SubscriptionCycle
import me.proton.core.payment.presentation.PaymentsOrchestrator
import me.proton.core.payment.presentation.entity.BillingResult
import me.proton.core.plan.domain.entity.SubscriptionManagement
import me.proton.core.plan.domain.usecase.CanUpgrade
import me.proton.core.plan.presentation.PlansOrchestrator
import me.proton.core.presentation.utils.InputValidationResult
import me.proton.core.presentation.utils.ValidationType
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryEvent
import me.proton.core.telemetry.presentation.ProductMetricsDelegate
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.user.domain.entity.CreateUserType
import me.proton.core.user.domain.entity.Type
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.repository.UserRepository
import me.proton.core.util.kotlin.coroutine.result
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class SignupViewModelTest : ArchTest by ArchTest(), CoroutinesTest by CoroutinesTest() {

    // region mocks
    @MockK(relaxed = true)
    private lateinit var humanVerificationExternalInput: HumanVerificationExternalInput

    @MockK(relaxed = true)
    private lateinit var keyStoreCrypto: KeyStoreCrypto

    @MockK(relaxed = true)
    private lateinit var plansOrchestrator: PlansOrchestrator

    @MockK(relaxed = true)
    private lateinit var paymentsOrchestrator: PaymentsOrchestrator

    @MockK(relaxed = true)
    private lateinit var clientIdProvider: ClientIdProvider

    @MockK
    private lateinit var performLogin: PerformLogin

    @MockK(relaxed = true)
    private lateinit var setCreateAccountSuccess: SetCreateAccountSuccess

    @MockK(relaxed = true)
    private lateinit var challengeManager: ChallengeManager

    @MockK(relaxed = true)
    private lateinit var observabilityManager: ObservabilityManager

    @MockK(relaxed = true)
    private lateinit var telemetryManager: TelemetryManager

    @MockK(relaxed = true)
    private lateinit var canUpgradeToPaid: CanUpgrade

    @MockK(relaxed = true)
    private lateinit var authRepository: AuthRepository

    @MockK(relaxed = true)
    private lateinit var userRepository: UserRepository

    @MockK(relaxed = true)
    private lateinit var sessionProvider: SessionProvider

    @MockK(relaxed = true)
    private lateinit var srpCrypto: SrpCrypto

    @MockK(relaxed = true)
    private lateinit var getPrimaryUser: GetPrimaryUser
    // endregion

    private lateinit var performCreateUser: PerformCreateUser
    private lateinit var performCreateExternalUser: PerformCreateExternalEmailUser


    // region test data
    private val testUsername = "test-username"
    private val testClientIdString = "test-clientId"
    private val testClientId = ClientId.CookieSession(CookieSessionId(testClientIdString))
    private val testPassword = "test-password"
    private val testEmail = "test-email"
    private val testPhone = "test-phone"

    private val testUser = User(
        userId = UserId("test-user-id"),
        email = null,
        name = testUsername,
        displayName = null,
        currency = "test-curr",
        credit = 0,
        createdAtUtc = 1000L,
        usedSpace = 0,
        maxSpace = 100,
        maxUpload = 100,
        role = null,
        private = true,
        services = 1,
        subscribed = 0,
        delinquent = null,
        recovery = null,
        keys = emptyList(),
        flags = emptyMap(),
        type = Type.Proton
    )

    private val usernameTakenError: ApiException
        get() = ApiException(
            ApiResult.Error.Http(
                409,
                "Conflict",
                ApiResult.Error.ProtonData(ResponseCodes.USER_CREATE_NAME_INVALID, "Username taken")
            )
        )

    private val signupChallengeConfig = SignupChallengeConfig()
    // endregion

    private lateinit var viewModel: SignupViewModel

    @Before
    fun beforeEveryTest() {
        MockKAnnotations.init(this)

        performCreateUser = spyk(
            PerformCreateUser(
                authRepository,
                userRepository,
                sessionProvider,
                srpCrypto,
                keyStoreCrypto,
                challengeManager,
                signupChallengeConfig,
                getPrimaryUser
            )
        )

        performCreateExternalUser = spyk(
            PerformCreateExternalEmailUser(
                authRepository,
                userRepository,
                sessionProvider,
                srpCrypto,
                keyStoreCrypto,
                challengeManager,
                signupChallengeConfig,
                getPrimaryUser
            )
        )

        viewModel = SignupViewModel(
            humanVerificationExternalInput,
            performCreateUser,
            performCreateExternalUser,
            setCreateAccountSuccess,
            keyStoreCrypto,
            plansOrchestrator,
            paymentsOrchestrator,
            performLogin,
            challengeManager,
            signupChallengeConfig,
            observabilityManager,
            canUpgradeToPaid,
            telemetryManager,
            mockk(relaxed = true)
        )
        coEvery { clientIdProvider.getClientId(any()) } returns testClientId
        every { keyStoreCrypto.decrypt(any<String>()) } returns testPassword
        every { keyStoreCrypto.encrypt(any<String>()) } returns "encrypted-$testPassword"

        coEvery {
            userRepository.createUser(
                username = testUsername,
                domain = any(),
                password = any(),
                recoveryEmail = any(),
                recoveryPhone = any(),
                referrer = any(),
                type = any(),
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } returns testUser

        coEvery {
            userRepository.createExternalEmailUser(
                email = testEmail,
                password = any(),
                type = any(),
                referrer = null,
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } returns testUser
    }

    @Test
    fun `create Internal user no recovery method set paid available`() = coroutinesTest {
        // GIVEN
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)

        viewModel.state.test {
            // WHEN
            viewModel.startCreateUserWorkflow()
            // THEN
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            assertTrue(awaitItem() is SignupViewModel.State.CreateUserProcessing)
            val successItem = awaitItem()
            assertTrue(successItem is SignupViewModel.State.CreateUserSuccess)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateUser(
                    username = testUsername,
                    password = "encrypted-$testPassword",
                    recoveryEmail = null,
                    recoveryPhone = null,
                    referrer = null,
                    type = CreateUserType.Normal,
                    domain = any()
                )
            }
            coVerify { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `create Internal user email recovery method set paid available`() = coroutinesTest {
        // GIVEN
        val emailRecovery = RecoveryMethod(RecoveryMethodType.EMAIL, testEmail)
        coEvery { canUpgradeToPaid() } returns true
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)

        viewModel.state.test {
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            // WHEN
            viewModel.setRecoveryMethod(emailRecovery)
            assertTrue(awaitItem() is SignupViewModel.State.PreloadingPlans)
            val createUserInputReady = awaitItem()
            assertIs<SignupViewModel.State.CreateUserInputReady>(createUserInputReady)
            assertTrue(createUserInputReady.paidOptionAvailable)
            viewModel.startCreateUserWorkflow()
            // THEN
            assertIs<SignupViewModel.State.CreateUserProcessing>(awaitItem())
            val successItem = awaitItem()
            assertIs<SignupViewModel.State.CreateUserSuccess>(successItem)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateUser(
                    username = testUsername,
                    password = "encrypted-$testPassword",
                    recoveryEmail = testEmail,
                    recoveryPhone = null,
                    referrer = null,
                    type = CreateUserType.Normal,
                    domain = any()
                )
            }
            coVerify { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `create Internal user email recovery method set paid unavailable`() = coroutinesTest {
        // GIVEN
        val emailRecovery = RecoveryMethod(RecoveryMethodType.EMAIL, testEmail)
        coEvery { canUpgradeToPaid() } returns false
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)

        viewModel.state.test {
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            // WHEN
            viewModel.setRecoveryMethod(emailRecovery)
            assertTrue(awaitItem() is SignupViewModel.State.PreloadingPlans)
            val createUserInputReady = awaitItem()
            assertIs<SignupViewModel.State.CreateUserInputReady>(createUserInputReady)
            assertFalse(createUserInputReady.paidOptionAvailable)
            viewModel.startCreateUserWorkflow()
            // THEN
            assertIs<SignupViewModel.State.CreateUserProcessing>(awaitItem())
            val successItem = awaitItem()
            assertIs<SignupViewModel.State.CreateUserSuccess>(successItem)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateUser(
                    username = testUsername,
                    password = "encrypted-$testPassword",
                    recoveryEmail = testEmail,
                    recoveryPhone = null,
                    referrer = null,
                    type = CreateUserType.Normal,
                    domain = any()
                )
            }
            coVerify { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    @Ignore
    fun `create Internal user phone recovery method set paid available`() = coroutinesTest {
        // GIVEN
        val emailRecovery = RecoveryMethod(RecoveryMethodType.SMS, testPhone)
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)
        coEvery { canUpgradeToPaid() } returns true

        viewModel.state.test {
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            // WHEN
            viewModel.setRecoveryMethod(emailRecovery)
            val createUserInputReady = awaitItem()
            assertIs<SignupViewModel.State.CreateUserInputReady>(createUserInputReady)
            assertTrue(createUserInputReady.paidOptionAvailable)
            viewModel.startCreateUserWorkflow()
            // THEN
            assertIs<SignupViewModel.State.CreateUserProcessing>(awaitItem())
            val successItem = awaitItem()
            assertIs<SignupViewModel.State.CreateUserSuccess>(successItem)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateUser(
                    username = testUsername,
                    password = "encrypted-$testPassword",
                    recoveryEmail = null,
                    recoveryPhone = testPhone,
                    referrer = null,
                    type = CreateUserType.Normal,
                    domain = any()
                )
            }
            coVerify { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `create Internal user phone recovery method set paid unavailable`() = coroutinesTest {
        // GIVEN
        val emailRecovery = RecoveryMethod(RecoveryMethodType.SMS, testPhone)
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)
        coEvery { canUpgradeToPaid() } returns false

        viewModel.state.test {
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            // WHEN
            viewModel.setRecoveryMethod(emailRecovery)
            assertTrue(awaitItem() is SignupViewModel.State.PreloadingPlans)
            val createUserInputReady = awaitItem()
            assertIs<SignupViewModel.State.CreateUserInputReady>(createUserInputReady)
            assertFalse(createUserInputReady.paidOptionAvailable)

            viewModel.startCreateUserWorkflow()
            // THEN
            assertIs<SignupViewModel.State.CreateUserProcessing>(awaitItem())
            val successItem = awaitItem()
            assertIs<SignupViewModel.State.CreateUserSuccess>(successItem)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateUser(
                    username = testUsername,
                    password = "encrypted-$testPassword",
                    recoveryEmail = null,
                    recoveryPhone = testPhone,
                    referrer = null,
                    type = CreateUserType.Normal,
                    domain = any()
                )
            }
            coVerify { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `create Internal user API error`() = coroutinesTest {
        // GIVEN
        coEvery {
            userRepository.createUser(
                username = testUsername,
                domain = any(),
                password = any(),
                recoveryEmail = any(),
                recoveryPhone = any(),
                referrer = null,
                type = any(),
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } throws ApiException(
            ApiResult.Error.Http(
                httpCode = 123,
                "http error",
                ApiResult.Error.ProtonData(
                    code = 12106,
                    error = "create user error"
                )
            )
        )
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)
        viewModel.state.test(timeout = 5.seconds) {
            // WHEN
            viewModel.startCreateUserWorkflow()
            // THEN
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            assertTrue(awaitItem() is SignupViewModel.State.CreateUserProcessing)
            val errorItem = awaitItem()
            assertTrue(errorItem is SignupViewModel.State.Error.Message)
            assertEquals("create user error", errorItem.message)

            coVerify(exactly = 1) {
                performCreateUser(
                    username = testUsername,
                    password = "encrypted-$testPassword",
                    recoveryEmail = null,
                    recoveryPhone = null,
                    referrer = null,
                    type = CreateUserType.Normal,
                    domain = any()
                )
            }
            coVerify(exactly = 0) { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `create External user success`() = coroutinesTest {
        // GIVEN
        viewModel.currentAccountType = AccountType.External
        viewModel.externalEmail = testEmail
        viewModel.setPassword(testPassword)
        viewModel.state.test {
            // WHEN
            viewModel.startCreateUserWorkflow()
            // THEN
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            assertTrue(awaitItem() is SignupViewModel.State.CreateUserProcessing)
            val successItem = awaitItem()
            assertTrue(successItem is SignupViewModel.State.CreateUserSuccess)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateExternalUser(
                    email = testEmail,
                    password = "encrypted-$testPassword",
                    referrer = null
                )
            }
            coVerify { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `create External user error`() = coroutinesTest {
        // GIVEN
        coEvery {
            userRepository.createExternalEmailUser(
                email = testEmail,
                password = any(),
                referrer = null,
                type = any(),
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } throws ApiException(
            ApiResult.Error.Http(
                httpCode = 123,
                "http error",
                ApiResult.Error.ProtonData(
                    code = 12106,
                    error = "create user error"
                )
            )
        )

        viewModel.currentAccountType = AccountType.External
        viewModel.externalEmail = testEmail
        viewModel.setPassword(testPassword)
        viewModel.state.test {
            // WHEN
            viewModel.startCreateUserWorkflow()
            // THEN
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            assertTrue(awaitItem() is SignupViewModel.State.CreateUserProcessing)
            val errorItem = awaitItem()
            assertTrue(errorItem is SignupViewModel.State.Error.Message)
            assertEquals("create user error", errorItem.message)

            coVerify(exactly = 1) {
                performCreateExternalUser(
                    email = testEmail,
                    password = "encrypted-$testPassword",
                    referrer = null
                )
            }
            coVerify(exactly = 0) { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `create External user with payment info success`() = coroutinesTest {
        // GIVEN
        viewModel.subscriptionDetails = SubscriptionDetails(
            planName = "vpnplus",
            planDisplayName = "VPN Plus",
            cycle = SubscriptionCycle.YEARLY,
            billingResult = BillingResult(
                paySuccess = true,
                token = "test-token",
                subscriptionCreated = false,
                amount = 499,
                currency = Currency.CHF,
                cycle = SubscriptionCycle.YEARLY,
                subscriptionManagement = SubscriptionManagement.GOOGLE_MANAGED
            )
        )
        viewModel.currentAccountType = AccountType.External
        viewModel.externalEmail = testEmail
        viewModel.setPassword(testPassword)
        viewModel.state.test {
            // WHEN
            viewModel.startCreateUserWorkflow()
            // THEN
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            assertTrue(awaitItem() is SignupViewModel.State.CreateUserProcessing)
            val successItem = awaitItem()
            assertTrue(successItem is SignupViewModel.State.CreateUserSuccess)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateExternalUser(
                    email = testEmail,
                    password = "encrypted-$testPassword",
                    referrer = null
                )
            }
            coVerify { setCreateAccountSuccess.invoke() }
        }
    }

    @Test
    fun `tries login if internal username taken`() = coroutinesTest {
        coEvery {
            userRepository.createUser(
                username = testUsername,
                domain = any(),
                password = any(),
                recoveryEmail = any(),
                recoveryPhone = any(),
                referrer = null,
                type = any(),
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } throws usernameTakenError

        coEvery { performLogin.invoke(testUsername, any()) } returns mockk {
            every { userId } returns testUser.userId
        }

        // GIVEN
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)

        viewModel.state.test {
            // WHEN
            viewModel.startCreateUserWorkflow()
            // THEN
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            assertTrue(awaitItem() is SignupViewModel.State.CreateUserProcessing)
            val successItem = awaitItem()
            assertTrue(successItem is SignupViewModel.State.CreateUserSuccess)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateUser(
                    username = testUsername,
                    password = "encrypted-$testPassword",
                    recoveryEmail = null,
                    recoveryPhone = null,
                    referrer = null,
                    type = CreateUserType.Normal,
                    domain = any()
                )
            }

            coVerify(exactly = 1) {
                performLogin(
                    username = testUsername,
                    password = "encrypted-$testPassword"
                )
            }
        }
    }

    @Test
    fun `tries login if External username taken`() = coroutinesTest {
        coEvery {
            userRepository.createExternalEmailUser(
                email = testEmail,
                password = any(),
                referrer = null,
                type = any(),
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } throws usernameTakenError

        coEvery { performLogin.invoke(testEmail, any()) } returns mockk {
            every { userId } returns testUser.userId
        }

        // GIVEN
        viewModel.currentAccountType = AccountType.External
        viewModel.externalEmail = testEmail
        viewModel.setPassword(testPassword)
        viewModel.state.test {
            // WHEN
            viewModel.startCreateUserWorkflow()
            // THEN
            assertTrue(awaitItem() is SignupViewModel.State.Idle)
            assertTrue(awaitItem() is SignupViewModel.State.CreateUserProcessing)
            val successItem = awaitItem()
            assertTrue(successItem is SignupViewModel.State.CreateUserSuccess)
            assertEquals(testUser.userId.id, successItem.userId)

            coVerify(exactly = 1) {
                performCreateExternalUser(
                    email = testEmail,
                    password = "encrypted-$testPassword",
                    referrer = null
                )
            }

            coVerify(exactly = 1) {
                performLogin(
                    username = testEmail,
                    password = "encrypted-$testPassword"
                )
            }
        }
    }

    @Test
    fun `observability data for internal accounts`() = coroutinesTest {
        // GIVEN
        coEvery {
            userRepository.createUser(
                username = testUsername,
                domain = any(),
                password = any(),
                recoveryEmail = any(),
                recoveryPhone = any(),
                referrer = any(),
                type = any(),
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } coAnswers {
            result("createUser") { testUser }
        }

        viewModel.currentAccountType = AccountType.Internal
        viewModel.username = testUsername
        viewModel.setPassword(testPassword)

        // WHEN
        viewModel.startCreateUserWorkflow().join()

        // THEN
        val accountCreationEventSlot = slot<SignupAccountCreationTotal>()
        verify { observabilityManager.enqueue(capture(accountCreationEventSlot), any()) }
        assertEquals(
            SignupAccountCreationTotal.ApiStatus.http2xx,
            accountCreationEventSlot.captured.Labels.status
        )
    }

    @Test
    fun `observability data for external accounts`() = coroutinesTest {
        // GIVEN
        coEvery {
            userRepository.createExternalEmailUser(
                email = testEmail,
                password = any(),
                referrer = any(),
                type = any(),
                auth = any(),
                frames = any(),
                sessionUserId = any()
            )
        } coAnswers {
            result("createExternalEmailUser") { testUser }
        }

        viewModel.currentAccountType = AccountType.External
        viewModel.externalEmail = testEmail
        viewModel.setPassword(testPassword)

        // WHEN
        viewModel.startCreateUserWorkflow().join()

        // THEN
        val accountCreationEventSlot = slot<SignupAccountCreationTotal>()
        verify { observabilityManager.enqueue(capture(accountCreationEventSlot), any()) }
        assertEquals(
            SignupAccountCreationTotal.ApiStatus.http2xx,
            accountCreationEventSlot.captured.Labels.status
        )
    }

    @Test
    fun `telemetry data for pass validation`() = coroutinesTest {
        // GIVEN
        val result = InputValidationResult("test-pass", ValidationType.PasswordMinLength)
        // WHEN
        viewModel.onInputValidationResult(result).join()

        // THEN
        val eventSlot = slot<TelemetryEvent>()
        verify { telemetryManager.enqueue(null, capture(eventSlot)) }
        val event = eventSlot.captured
        assertEquals(
            "fe.signup_password.validate",
            event.name
        )
        assertEquals("success", event.dimensions[ProductMetricsDelegate.KEY_RESULT])
    }

    @Test
    fun `telemetry data for pass validation invalid`() = coroutinesTest {
        // GIVEN
        val result = InputValidationResult("pass", ValidationType.PasswordMinLength)
        // WHEN
        viewModel.onInputValidationResult(result).join()

        // THEN
        val eventSlot = slot<TelemetryEvent>()
        verify { telemetryManager.enqueue(null, capture(eventSlot)) }
        val event = eventSlot.captured
        assertEquals(
            "fe.signup_password.validate",
            event.name
        )
        assertEquals("password_too_weak", event.dimensions[ProductMetricsDelegate.KEY_RESULT])
    }

    @Test
    fun `telemetry data for pass validation match`() = coroutinesTest {
        // GIVEN
        val result = InputValidationResult("test-pass", ValidationType.PasswordMatch, "test-pass")
        // WHEN
        viewModel.onInputValidationResult(result).join()

        // THEN
        val eventSlot = slot<TelemetryEvent>()
        verify { telemetryManager.enqueue(null, capture(eventSlot)) }
        val event = eventSlot.captured
        assertEquals(
            "fe.signup_password.validate",
            event.name
        )
        assertEquals("success", event.dimensions[ProductMetricsDelegate.KEY_RESULT])
    }

    @Test
    fun `telemetry data for pass validation not match`() = coroutinesTest {
        // GIVEN
        val result = InputValidationResult("test-pass", ValidationType.PasswordMatch, "test-pass2")
        // WHEN
        viewModel.onInputValidationResult(result).join()

        // THEN
        val eventSlot = slot<TelemetryEvent>()
        verify { telemetryManager.enqueue(null, capture(eventSlot)) }
        val event = eventSlot.captured
        assertEquals(
            "fe.signup_password.validate",
            event.name
        )
        assertEquals("password_mismatch", event.dimensions[ProductMetricsDelegate.KEY_RESULT])
    }
}
