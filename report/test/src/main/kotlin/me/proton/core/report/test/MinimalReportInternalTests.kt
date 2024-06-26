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

import me.proton.core.account.domain.entity.AccountType
import me.proton.core.auth.test.flow.SignInFlow
import me.proton.core.auth.test.robot.AddAccountRobot
import me.proton.core.report.test.flow.ReportFlow
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.User
import org.junit.Test

/**
 * Minimal Report Tests for app providing [AccountType.Internal].
 */
public interface MinimalReportInternalTests {

    public val quark: Quark
    public val users: User.Users

    public fun verifyBefore()
    public fun startReport()
    public fun verifyAfter()

    @Test
    public fun fillAndSendReportHappyPath() {
        val user = users.getUser { it.name == "pro" }

        AddAccountRobot.clickSignIn()
        SignInFlow.signInInternal(user.name, user.password)
        verifyBefore()

        startReport()
        ReportFlow.fillAndSendReport()

        verifyAfter()
    }
}
