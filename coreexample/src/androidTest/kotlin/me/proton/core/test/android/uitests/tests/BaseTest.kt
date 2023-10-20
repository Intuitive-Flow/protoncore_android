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

package me.proton.core.test.android.uitests.tests

import android.app.Application
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import me.proton.android.core.coreexample.Constants
import me.proton.android.core.coreexample.MainActivity
import me.proton.core.auth.presentation.testing.ProtonTestEntryPoint
import me.proton.core.domain.entity.UserId
import me.proton.core.payment.domain.usecase.PaymentProvider
import me.proton.core.test.android.instrumented.ProtonTest
import me.proton.core.test.android.instrumented.utils.Shell.setupDeviceForAutomation
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.uitests.robot.CoreexampleRobot
import me.proton.core.test.quark.Quark
import me.proton.core.test.quark.data.Plan
import me.proton.core.test.quark.data.User
import me.proton.core.test.quark.data.User.Users
import org.junit.After
import org.junit.BeforeClass

open class BaseTest(
    private val logoutAllAfterTest: Boolean = true,
    defaultTimeout: Long = 30_000L,
) : ProtonTest(MainActivity::class.java, defaultTimeout, 2) {

    @After
    fun logoutUsers() {
        if (logoutAllAfterTest) {
            Log.d(testTag, "Logging out users")
            authHelper.logoutAll()
        }
    }

    fun login(user: User) {
        Log.d(testTag, "Login user: ${user.name}")
        AddAccountRobot().back<CoreexampleRobot>().verify { accountSwitcherDisplayed() }
        val sessionInfo = authHelper.login(user.name, user.password)
        fetchUnleashFeatureFlags(sessionInfo.userId)
        Log.d(testTag, "Login done.")
    }

    companion object {
        val users = Users.fromDefaultResources()
        val quark = Quark.fromDefaultResources(
            Constants.QUARK_HOST,
            Constants.PROXY_TOKEN
        )

        val authHelper by lazy { protonTestEntryPoint.loginTestHelper }

        private val protonTestEntryPoint by lazy {
            EntryPointAccessors.fromApplication(
                ApplicationProvider.getApplicationContext<Application>(),
                ProtonTestEntryPoint::class.java
            )
        }

        @JvmStatic
        @BeforeClass
        fun prepare() {
            setupDeviceForAutomation(true)
            authHelper.logoutAll()
            fetchUnleashFeatureFlags(userId = null)
            Plan.Dev.text = Plan.MailPlus.text
            Plan.Dev.planName = Plan.MailPlus.planName
        }

        /** Generally available payment providers, which are at least partially supported by Core Android. */
        @JvmStatic
        protected fun availablePaymentProviders(): Set<PaymentProvider> = runBlocking {
            protonTestEntryPoint.getAvailablePaymentProviders()
        }

        /** Payment providers that can be used during the signup. */
        @JvmStatic
        protected fun paymentProvidersForSignup(): Set<PaymentProvider> =
            availablePaymentProviders().filter {
                it != PaymentProvider.PayPal
            }.toSet()

        private fun fetchUnleashFeatureFlags(userId: UserId? = null) = runBlocking {
            protonTestEntryPoint.featureFlagRepository.getAll(userId)
        }
    }
}
