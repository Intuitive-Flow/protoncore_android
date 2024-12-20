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

package me.proton.core.report.test

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.report.test.robot.ReportRobot
import me.proton.core.test.rule.annotation.PrepareUser
import org.junit.Test

/**
 * Minimal Report Tests for app providing [AccountType.Internal].
 */
@HiltAndroidTest
public interface MinimalReportInternalTests {

    public fun startReport()

    public fun verifyAfter()

    @Test
    @PrepareUser(loginBefore = true)
    public fun fillAndSendReportHappyPath() {
        startReport()

        ReportRobot
            .fillSubject("Test Subject")
            .fillDescription("Test Description")
            .clickSend()
            .apply {
                verifyAfter()
            }
    }
}
