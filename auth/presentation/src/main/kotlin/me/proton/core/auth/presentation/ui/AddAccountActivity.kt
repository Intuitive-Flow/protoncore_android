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

package me.proton.core.auth.presentation.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.databinding.ActivityAddAccountBinding
import me.proton.core.auth.presentation.entity.AddAccountInput
import me.proton.core.auth.presentation.entity.AddAccountResult
import me.proton.core.auth.presentation.entity.AddAccountWorkflow
import me.proton.core.auth.presentation.onLoginResult
import me.proton.core.auth.presentation.onOnSignUpResult
import me.proton.core.presentation.ui.ProtonViewBindingActivity
import me.proton.core.presentation.utils.addOnBackPressedCallback
import me.proton.core.presentation.utils.onClick
import me.proton.core.telemetry.presentation.annotation.ProductMetrics
import me.proton.core.telemetry.presentation.annotation.ScreenClosed
import me.proton.core.telemetry.presentation.annotation.ScreenDisplayed
import me.proton.core.telemetry.presentation.annotation.ViewClicked
import javax.inject.Inject

@AndroidEntryPoint
@ProductMetrics(
    group = "account.android.signup",
    flow = "mobile_signup_full"
)
@ScreenDisplayed(event = "fe.add_account.displayed")
@ScreenClosed(event = "user.add_account.closed")
@ViewClicked(
    event = "user.add_account.clicked",
    viewIds = ["sign_up", "sign_in"]
)
class AddAccountActivity :
    ProtonViewBindingActivity<ActivityAddAccountBinding>(ActivityAddAccountBinding::inflate) {

    @Inject
    lateinit var authOrchestrator: AuthOrchestrator

    @Inject
    lateinit var accountManager: AccountManager

    private val input: AddAccountInput? by lazy {
        intent?.extras?.getParcelable(ARG_INPUT)
    }

    private val requiredAccountType: AccountType by lazy {
        input?.requiredAccountType ?: AccountType.Internal
    }

    private val creatableAccountType: AccountType by lazy {
        input?.creatableAccountType ?: AccountType.Internal
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addOnBackPressedCallback { onClose() }
        authOrchestrator.register(this)

        // A Lottie animation will be added in the next iteration.
        // binding.lottie.addAnimatorUpdateListener {
        //     it.doOnEnd { binding.lottie.animate().alpha(0f).setListener(null) }
        // }

        binding.signIn.onClick { authOrchestrator.startLoginWorkflow(requiredAccountType, input?.loginUsername) }
        authOrchestrator.onLoginResult { if (it != null) onSuccess(it.userId, AddAccountWorkflow.SignIn) }

        binding.signUp.onClick { authOrchestrator.startSignupWorkflow(creatableAccountType) }
        authOrchestrator.onOnSignUpResult { if (it != null) onSuccess(it.userId, AddAccountWorkflow.SignUp) }
    }

    override fun onDestroy() {
        authOrchestrator.unregister()
        super.onDestroy()
    }

    private fun onSuccess(userId: String, workflow: AddAccountWorkflow) {
        val intent = Intent().putExtra(ARG_RESULT, AddAccountResult(userId = userId, workflow = workflow))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun onClose() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    companion object {
        const val ARG_INPUT = "arg.addAccountInput"
        const val ARG_RESULT = "arg.addAccountResult"
    }
}
