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

package me.proton.core.observability.data.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes

class ObservabilityWorkerManagerImplTest {
    private lateinit var tested: ObservabilityWorkerManagerImpl
    private lateinit var workManager: WorkManager

    @BeforeTest
    fun setUp() {
        workManager = mockk()
        tested = ObservabilityWorkerManagerImpl(workManager)
    }

    @Test
    fun scheduling() {
        val workNameSlot = slot<String>()
        val workPolicySlot = slot<ExistingWorkPolicy>()
        val requestSlot = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(
                capture(workNameSlot),
                capture(workPolicySlot),
                capture(requestSlot)
            )
        } returns mockk()
        every { workManager.cancelUniqueWork(any()) } returns mockk()

        // WHEN
        tested.enqueueOrKeep(ZERO)

        // THEN
        verify { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        assertEquals(ExistingWorkPolicy.KEEP, workPolicySlot.captured)
        assertEquals(0, requestSlot.captured.workSpec.initialDelay)

        // WHEN
        tested.cancel()

        // THEN
        verify { workManager.cancelUniqueWork(workNameSlot.captured) }

        // WHEN
        tested.enqueueOrKeep(2.minutes)

        // THEN
        assertEquals(ExistingWorkPolicy.KEEP, workPolicySlot.captured)
        assertEquals(2.minutes.inWholeMilliseconds, requestSlot.captured.workSpec.initialDelay)
    }
}