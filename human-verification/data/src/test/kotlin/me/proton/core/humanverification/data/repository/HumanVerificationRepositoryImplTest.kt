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

package me.proton.core.humanverification.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.EncryptedString
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.domain.entity.UserId
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.humanverification.data.db.HumanVerificationDetailsDao
import me.proton.core.humanverification.data.entity.HumanVerificationEntity
import me.proton.core.humanverification.domain.repository.HumanVerificationRepository
import me.proton.core.network.domain.humanverification.HumanVerificationDetails
import me.proton.core.network.domain.humanverification.HumanVerificationState
import me.proton.core.network.domain.client.ClientId
import me.proton.core.network.domain.client.ClientIdType
import me.proton.core.network.domain.humanverification.VerificationMethod
import me.proton.core.network.domain.session.Session
import me.proton.core.network.domain.session.SessionId
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class HumanVerificationRepositoryImplTest {

    private lateinit var humanVerificationRepository: HumanVerificationRepository

    private val db = mockk<HumanVerificationDatabase>()
    private val humanVerificationDetailsDao = mockk<HumanVerificationDetailsDao>(relaxed = true)

    private val userId = UserId("userId")
    private val session1 = Session.Authenticated(
        userId = userId,
        sessionId = SessionId("session1"),
        accessToken = "accessToken",
        refreshToken = "refreshToken",
        scopes = listOf("full", "calendar", "mail")
    )

    private val clientId = ClientId.AccountSession(session1.sessionId)

    private val simpleCrypto = object : KeyStoreCrypto {
        override fun isUsingKeyStore(): Boolean = false
        override fun encrypt(value: String): EncryptedString = "encrypted-$value"
        override fun encrypt(value: PlainByteArray): EncryptedByteArray = EncryptedByteArray(value.array)
        override fun decrypt(value: EncryptedString): String = "decrypted-$value"
        override fun decrypt(value: EncryptedByteArray): PlainByteArray = PlainByteArray(value.array)
    }

    @Before
    fun beforeEveryTest() {
        every { db.humanVerificationDetailsDao() } returns humanVerificationDetailsDao

        humanVerificationRepository = HumanVerificationRepositoryImpl(db, simpleCrypto)

        mockkStatic("androidx.room.RoomDatabaseKt")
        val transactionLambda = slot<suspend () -> Unit>()
        coEvery { db.inTransaction(capture(transactionLambda)) } coAnswers {
            transactionLambda.captured.invoke()
        }
    }

    @After
    fun afterEveryTest() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun `cannot insert invalid entity`() = runTest {
        val details = HumanVerificationDetails(
            mockk(),
            listOf("captcha"),
            verificationToken = null,
            state = HumanVerificationState.HumanVerificationNeeded
        )
        assertFailsWith<IllegalArgumentException> {
            humanVerificationRepository.insertHumanVerificationDetails(details)
        }
    }

    @Test
    fun `set human verification details`() = runTest {
        val humanVerificationDetails = HumanVerificationDetails(
            clientId = clientId,
            verificationMethods = listOf(VerificationMethod.EMAIL),
            verificationToken = "token-123",
            state = HumanVerificationState.HumanVerificationNeeded,
            tokenType = null,
            tokenCode = null
        )

        val humanVerificationEntity = HumanVerificationEntity(
            clientId = clientId.id,
            clientIdType = ClientIdType.SESSION,
            verificationMethods = listOf(VerificationMethod.EMAIL),
            verificationToken = "token-123",
            state = HumanVerificationState.HumanVerificationNeeded,
            humanHeaderTokenType = null,
            humanHeaderTokenCode = null
        )

        coEvery { humanVerificationDetailsDao.getByClientId(clientId.id) } returns humanVerificationEntity

        humanVerificationRepository.insertHumanVerificationDetails(details = humanVerificationDetails)

        coVerify(exactly = 1) { humanVerificationDetailsDao.insertOrUpdate(humanVerificationEntity) }
        coVerify(exactly = 1) { humanVerificationDetailsDao.insertOrUpdate(humanVerificationEntity) }
        coVerify(exactly = 1) { humanVerificationDetailsDao.getByClientId(clientId.id) }
    }

    @Test
    fun `set human verification details with success state`() = runTest {
        val state = HumanVerificationState.HumanVerificationSuccess
        val humanVerificationDetails = HumanVerificationDetails(
            clientId = clientId,
            verificationMethods = listOf(VerificationMethod.EMAIL),
            verificationToken = null,
            state = state,
            tokenType = null,
            tokenCode = null
        )
        val humanVerificationEntity = HumanVerificationEntity(
            clientId = clientId.id,
            clientIdType = ClientIdType.SESSION,
            verificationMethods = listOf(VerificationMethod.EMAIL),
            verificationToken = null,
            state = state,
            humanHeaderTokenType = null,
            humanHeaderTokenCode = null
        )
        coEvery { humanVerificationDetailsDao.getByClientId(clientId.id) } returns humanVerificationEntity

        humanVerificationRepository.insertHumanVerificationDetails(details = humanVerificationDetails)

        coVerify(exactly = 1) { humanVerificationDetailsDao.insertOrUpdate(humanVerificationEntity) }
        coVerify(exactly = 1) { humanVerificationDetailsDao.getByClientId(clientId.id) }
    }

    @Test
    fun `set human verification details with success state and token details`() = runTest {
        val state = HumanVerificationState.HumanVerificationSuccess
        val tokenType = "token-type"
        val tokenCode = "token-code"
        val humanVerificationDetails = HumanVerificationDetails(
            clientId = clientId,
            verificationMethods = listOf(VerificationMethod.EMAIL),
            verificationToken = null,
            state = state,
            tokenType = tokenType,
            tokenCode = tokenCode
        )
        val humanVerificationEntity = HumanVerificationEntity(
            clientId = clientId.id,
            clientIdType = ClientIdType.SESSION,
            verificationMethods = listOf(VerificationMethod.EMAIL),
            verificationToken = null,
            state = state,
            humanHeaderTokenType = "encrypted-$tokenType",
            humanHeaderTokenCode = "encrypted-$tokenCode"
        )
        coEvery { humanVerificationDetailsDao.getByClientId(clientId.id) } returns humanVerificationEntity

        humanVerificationRepository.insertHumanVerificationDetails(details = humanVerificationDetails)

        coVerify(exactly = 1) { humanVerificationDetailsDao.insertOrUpdate(humanVerificationEntity) }
        coVerify(exactly = 1) { humanVerificationDetailsDao.getByClientId(clientId.id) }
    }

    @Test
    fun `update state`() = runTest {
        val state = HumanVerificationState.HumanVerificationSuccess
        val tokenType = "token-type"
        val tokenCode = "token-code"

        val humanVerificationEntity = HumanVerificationEntity(
            clientId = clientId.id,
            clientIdType = ClientIdType.SESSION,
            verificationMethods = listOf(VerificationMethod.EMAIL),
            verificationToken = null,
            state = state,
            humanHeaderTokenType = null,
            humanHeaderTokenCode = null
        )
        coEvery { humanVerificationDetailsDao.getByClientId(clientId.id) } returns humanVerificationEntity
        coEvery { humanVerificationDetailsDao.getByClientId(clientId.id) } returns humanVerificationEntity

        humanVerificationRepository.updateHumanVerificationState(
            clientId = clientId,
            state = state,
            tokenType = tokenType,
            tokenCode = tokenCode
        )

        coVerify(exactly = 1) {
            humanVerificationDetailsDao.updateStateAndToken(
                clientId.id,
                state,
                "encrypted-$tokenType",
                "encrypted-$tokenCode"
            )
        }
        coVerify(exactly = 1) { humanVerificationDetailsDao.getByClientId(clientId.id) }
    }
}
