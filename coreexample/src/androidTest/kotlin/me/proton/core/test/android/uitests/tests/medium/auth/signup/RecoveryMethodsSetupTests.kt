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

package me.proton.core.test.android.uitests.tests.medium.auth.signup

import me.proton.core.auth.R
import me.proton.core.test.android.robots.CoreRobot
import me.proton.core.test.android.robots.auth.signup.ChooseUsernameRobot
import me.proton.core.test.android.robots.auth.signup.CreatingUserRobot
import me.proton.core.test.android.robots.auth.signup.RecoveryMethodsRobot
import me.proton.core.test.android.robots.auth.signup.RecoveryMethodsRobot.RecoveryMethodType
import me.proton.core.test.android.robots.humanverification.HVRobot
import me.proton.core.test.android.uitests.robot.CoreexampleRobot
import me.proton.core.test.android.uitests.tests.BaseTest
import me.proton.core.util.kotlin.random
import org.junit.Before
import org.junit.Test

class RecoveryMethodsSetupTests : BaseTest() {

    private val recoveryMethodsRobot = RecoveryMethodsRobot()

    @Before
    fun goToPasswordSetup() {
        CoreRobot()
            .device
            .clickBackBtn()

        CoreexampleRobot()
            .signupInternal()
            .verify { domainInputDisplayed() }

        ChooseUsernameRobot()
            .username(String.random())
            .next()
            .password("12345678")
            .confirmPassword("12345678")
            .next<RecoveryMethodsRobot>()
            .verify { recoveryMethodsElementsDisplayed() }
    }

    @Test
    fun emailAndPhoneValidation() {
        recoveryMethodsRobot
            .recoveryMethod(RecoveryMethodType.EMAIL)
            .email("notEmail")
            .next<RecoveryMethodsRobot>()
            .verify { errorSnackbarDisplayed("Email address failed validation") }

        recoveryMethodsRobot
            .recoveryMethod(RecoveryMethodType.PHONE)
            .phone("11")
            .next<RecoveryMethodsRobot>()
            .verify { errorSnackbarDisplayed("Phone number failed validation") }
    }

    @Test
    fun skipRecoveryMethods() {
        val skipRecoveryRobot = recoveryMethodsRobot.skip()
        if (paymentProvidersForSignup().isNotEmpty()) {
            skipRecoveryRobot.skipConfirm<CreatingUserRobot>()
                .verify {
                    creatingUserDisplayed(R.string.auth_signup_your_account_is_being_setup)
                }
        } else {
            skipRecoveryRobot.skipConfirm<HVRobot>().verify {
                hvElementsDisplayed()
            }
        }
    }

    @Test
    fun emptyFieldsTriggerSkip() {
        val skipRecoveryRobot = recoveryMethodsRobot.next<RecoveryMethodsRobot.SkipRecoveryRobot>()
        if (paymentProvidersForSignup().isNotEmpty()) {
            skipRecoveryRobot.skipConfirm<CreatingUserRobot>()
                .verify {
                    creatingUserDisplayed(R.string.auth_signup_your_account_is_being_setup)
                }
        } else {
            skipRecoveryRobot.skipConfirm<HVRobot>().verify {
                hvElementsDisplayed()
            }
        }
    }
}
