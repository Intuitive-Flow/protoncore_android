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

package me.proton.core.test.android.uitests.tests.large.usersettings

import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.test.android.robots.auth.AccountSwitcherRobot
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.robot.CoreexampleRobot
import me.proton.core.test.android.uitests.tests.BaseTest
import org.junit.Test

@HiltAndroidTest
class PasswordManagementTest : BaseTest() {

    private val createUserResult = quark.userCreate()

    @Test
    fun changePasswordAndLogin() {
        val user = createUserResult.first
        val newPassword = "newPassword"

        login(user)

        CoreexampleRobot()
            .settingsPasswordManagement()
            .changePassword<CoreexampleRobot>(user.password, newPassword)
            .accountSwitcher()
            .userAction<AddAccountRobot>(user, AccountSwitcherRobot.UserAction.Remove)
            .signIn()
            .username(user.name)
            .password(newPassword)
            .signIn<CoreexampleRobot>()
            .verify { primaryUserIs(user) }
    }
}
