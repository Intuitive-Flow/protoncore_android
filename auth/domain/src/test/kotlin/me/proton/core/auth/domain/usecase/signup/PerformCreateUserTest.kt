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

package me.proton.core.auth.domain.usecase.signup

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import me.proton.core.auth.domain.entity.Modulus
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.challenge.domain.ChallengeManager
import me.proton.core.crypto.common.keystore.EncryptedString
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.srp.Auth
import me.proton.core.crypto.common.srp.SrpCrypto
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiException
import me.proton.core.user.domain.entity.CreateUserType
import me.proton.core.user.domain.entity.Type
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.repository.UserRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class PerformCreateUserTest {
    // region mocks
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val srpCrypto = mockk<SrpCrypto>(relaxed = true)
    private val keyStoreCrypto = mockk<KeyStoreCrypto>(relaxed = true)
    private val challengeManager = mockk<ChallengeManager>(relaxed = true)

    @MockK(relaxed = true)
    private lateinit var getPrimaryUser: GetPrimaryUser
    // endregion

    // region test data
    private val testUsername = "test-username"
    private val testPassword = "test-password"
    private val testEncryptedPassword = "encrypted-$testPassword"
    private val testEmail = "test-email"
    private val testPhone = "test-phone"
    private val testModulus = Modulus(modulusId = "test-id", modulus = "test-modulus")
    private val testAuth = Auth(
        version = 0,
        modulusId = testModulus.modulusId,
        salt = "test-salt",
        verifier = "test-verifier"
    )
    private val testUserId = UserId("user_id")

    // endregion

    private val signupChallengeConfig = SignupChallengeConfig()
    private lateinit var useCase: PerformCreateUser

    @Before
    fun beforeEveryTest() {
        // GIVEN
        MockKAnnotations.init(this)

        useCase = PerformCreateUser(
            authRepository,
            userRepository,
            srpCrypto,
            keyStoreCrypto,
            challengeManager,
            signupChallengeConfig,
            getPrimaryUser
        )
        coEvery {
            srpCrypto.calculatePasswordVerifier(testUsername, any(), any(), any())
        } returns testAuth
        every { keyStoreCrypto.decrypt(any<String>()) } returns testPassword
        every { keyStoreCrypto.encrypt(any<String>()) } returns testEncryptedPassword

        coEvery { authRepository.randomModulus(null) } returns testModulus
        coEvery {
            userRepository.createUser(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns mockk {
            every { userId } returns testUserId
        }
    }

    @Test
    fun `create user no recovery success`() = runTest {
        // GIVEN
        coEvery { challengeManager.getFramesByFlowName("signup") } returns emptyList()

        // WHEN
        useCase.invoke(
            testUsername,
            domain = "proton.me",
            keyStoreCrypto.encrypt(testPassword),
            recoveryEmail = null,
            recoveryPhone = null,
            referrer = null,
            type = CreateUserType.Normal,
        )

        // THEN
        coVerify(exactly = 1) { authRepository.randomModulus(null) }
        coVerify(exactly = 1) {
            srpCrypto.calculatePasswordVerifier(
                username = testUsername,
                password = any(),
                modulusId = testModulus.modulusId,
                modulus = testModulus.modulus
            )
        }
        val usernameSlot = slot<String>()
        val passwordSlot = slot<EncryptedString>()
        val typeSlot = slot<CreateUserType>()

        coVerify(exactly = 1) {
            userRepository.createUser(
                capture(usernameSlot),
                any(),
                capture(passwordSlot),
                null,
                null,
                null,
                capture(typeSlot),
                any(),
                frames = emptyList(),
                sessionUserId = null
            )
        }
        assertEquals(testUsername, usernameSlot.captured)
        assertEquals("encrypted-$testPassword", passwordSlot.captured)
        assertEquals(CreateUserType.Normal, typeSlot.captured)
    }

    @Test
    fun `create user from credential-less`() = runTest {
        // GIVEN
        coEvery { challengeManager.getFramesByFlowName("signup") } returns emptyList()
        coEvery { getPrimaryUser() } returns mockk<User> {
            every { userId } returns testUserId
            every { type } returns Type.CredentialLess
        }

        // WHEN
        useCase.invoke(
            testUsername,
            domain = "proton.me",
            keyStoreCrypto.encrypt(testPassword),
            recoveryEmail = null,
            recoveryPhone = null,
            referrer = null,
            type = CreateUserType.Normal,
        )

        // THEN
        coVerify(exactly = 1) {
            userRepository.createUser(
                any(),
                any(),
                any(),
                null,
                null,
                null,
                any(),
                any(),
                frames = emptyList(),
                sessionUserId = testUserId // userId of credential-less is passed
            )
        }
    }

    @Test
    fun `create user email recovery success`() = runTest {
        // GIVEN
        coEvery { challengeManager.getFramesByFlowName("signup") } returns emptyList()

        // WHEN
        useCase.invoke(
            testUsername,
            domain = "proton.me",
            keyStoreCrypto.encrypt(testPassword),
            recoveryEmail = testEmail,
            recoveryPhone = null,
            referrer = null,
            type = CreateUserType.Normal,
        )

        // THEN
        coVerify(exactly = 1) { authRepository.randomModulus(null) }
        coVerify(exactly = 1) {
            srpCrypto.calculatePasswordVerifier(
                username = testUsername,
                password = any(),
                modulusId = testModulus.modulusId,
                modulus = testModulus.modulus
            )
        }
        val usernameSlot = slot<String>()
        val passwordSlot = slot<EncryptedString>()
        val typeSlot = slot<CreateUserType>()
        val emailSlot = slot<String>()
        coVerify(exactly = 1) {
            userRepository.createUser(
                capture(usernameSlot),
                any(),
                capture(passwordSlot),
                capture(emailSlot),
                null,
                null,
                capture(typeSlot),
                any(),
                frames = emptyList(),
                sessionUserId = null
            )
        }
        assertEquals(testUsername, usernameSlot.captured)
        assertEquals("encrypted-$testPassword", passwordSlot.captured)
        assertEquals(CreateUserType.Normal, typeSlot.captured)
        assertEquals(testEmail, emailSlot.captured)
    }

    @Test
    fun `create user phone recovery success`() = runTest {
        // GIVEN
        coEvery { challengeManager.getFramesByFlowName("signup") } returns emptyList()

        // WHEN
        useCase.invoke(
            testUsername,
            domain = "proton.me",
            keyStoreCrypto.encrypt(testPassword),
            recoveryEmail = null,
            recoveryPhone = testPhone,
            referrer = null,
            type = CreateUserType.Normal,
        )

        // THEN
        coVerify(exactly = 1) { authRepository.randomModulus(null) }
        coVerify(exactly = 1) {
            srpCrypto.calculatePasswordVerifier(
                username = testUsername,
                password = any(),
                modulusId = testModulus.modulusId,
                modulus = testModulus.modulus
            )
        }
        val usernameSlot = slot<String>()
        val passwordSlot = slot<EncryptedString>()
        val typeSlot = slot<CreateUserType>()
        val phoneSlot = slot<String>()
        coVerify(exactly = 1) {
            userRepository.createUser(
                capture(usernameSlot),
                any(),
                capture(passwordSlot),
                null,
                capture(phoneSlot),
                null,
                capture(typeSlot),
                any(),
                frames = emptyList(),
                sessionUserId = null
            )
        }
        assertEquals(testUsername, usernameSlot.captured)
        assertEquals("encrypted-$testPassword", passwordSlot.captured)
        assertEquals(CreateUserType.Normal, typeSlot.captured)
        assertEquals(testPhone, phoneSlot.captured)
    }

    @Test
    fun `create user phone and email recovery failure`() = runTest {
        val throwable = assertFailsWith<IllegalArgumentException> {
            useCase.invoke(
                testUsername,
                domain = "proton.me",
                keyStoreCrypto.encrypt(testPassword),
                recoveryEmail = testEmail,
                recoveryPhone = testPhone,
                referrer = null,
                type = CreateUserType.Normal,
            )
        }
        val expectedMessage = "Recovery Email and Phone could not be set together"
        assertNotNull(throwable)
        assertEquals(
            expectedMessage,
            throwable.message
        )
    }

    @Test
    fun `user already exists and cannot log in`() = runTest {
        val apiException = mockk<ApiException>()

        coEvery {
            userRepository.createUser(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws apiException

        val result = assertFailsWith(ApiException::class) {
            useCase.invoke(
                testEmail,
                domain = "proton.me",
                keyStoreCrypto.encrypt(testPassword),
                recoveryEmail = null,
                recoveryPhone = null,
                referrer = null,
                type = CreateUserType.Normal,
            )
        }
        assertSame(apiException, result)
    }
}
