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

package me.proton.core.auth.presentation.ui.signup

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.auth.domain.entity.BillingDetails
import me.proton.core.auth.domain.usecase.PostLoginAccountSetup
import me.proton.core.auth.presentation.R
import me.proton.core.auth.presentation.databinding.ActivitySignupBinding
import me.proton.core.auth.presentation.entity.signup.SignUpInput
import me.proton.core.auth.presentation.entity.signup.SignUpResult
import me.proton.core.auth.presentation.entity.signup.SubscriptionDetails
import me.proton.core.auth.presentation.observability.toUnlockUserStatus
import me.proton.core.auth.presentation.observability.toUserCheckStatus
import me.proton.core.auth.presentation.ui.AuthActivity
import me.proton.core.auth.presentation.ui.removeCreatingUser
import me.proton.core.auth.presentation.ui.showCongrats
import me.proton.core.auth.presentation.ui.showCreatingUser
import me.proton.core.auth.presentation.viewmodel.LoginViewModel
import me.proton.core.auth.presentation.viewmodel.signup.SignupViewModel
import me.proton.core.domain.entity.Product
import me.proton.core.domain.entity.UserId
import me.proton.core.observability.domain.metrics.SignupLoginTotal
import me.proton.core.observability.domain.metrics.SignupUnlockUserTotalV1
import me.proton.core.observability.domain.metrics.SignupUserCheckTotalV1
import me.proton.core.observability.domain.metrics.common.toHttpApiStatus
import me.proton.core.observability.domain.metrics.common.toObservabilityAccountType
import me.proton.core.payment.domain.entity.ProtonPaymentToken
import me.proton.core.payment.presentation.entity.BillingResult
import me.proton.core.plan.presentation.entity.PlanInput
import me.proton.core.plan.presentation.entity.SelectedPlan
import me.proton.core.plan.presentation.ui.BasePlansFragment.Companion.BUNDLE_KEY_BILLING_DETAILS
import me.proton.core.plan.presentation.ui.BasePlansFragment.Companion.BUNDLE_KEY_PLAN
import me.proton.core.plan.presentation.ui.BasePlansFragment.Companion.KEY_PLAN_SELECTED
import me.proton.core.plan.presentation.ui.hasPlanSignupFragment
import me.proton.core.plan.presentation.ui.removePlansSignup
import me.proton.core.plan.presentation.ui.showPlansSignup
import me.proton.core.presentation.utils.getUserMessage
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@AndroidEntryPoint
class SignupActivity : AuthActivity<ActivitySignupBinding>(ActivitySignupBinding::inflate) {

    private val signUpViewModel by viewModels<SignupViewModel>()
    private val loginViewModel by viewModels<LoginViewModel>()

    @Inject
    lateinit var product: Product

    private val input: SignUpInput by lazy {
        requireNotNull(intent?.extras?.getParcelable(ARG_INPUT)) as SignUpInput
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        signUpViewModel.register(this)

        if (savedInstanceState == null) {
            with(supportFragmentManager) {
                when (input.creatableAccountType) {
                    AccountType.Username -> showUsernameChooser()
                    AccountType.Internal -> showInternalEmailChooser(input.creatableAccountType)
                    AccountType.External -> showExternalEmailChooser(input.creatableAccountType)
                }
            }
        }

        observeSignupViewModelState()
        observeLoginViewModelState()
        setPlanResultListener()
    }

    private fun observeSignupViewModelState() {
        signUpViewModel.state
            .flowWithLifecycle(lifecycle)
            .distinctUntilChanged()
            .onEach {
                when (it) {
                    is SignupViewModel.State.Idle -> Unit
                    is SignupViewModel.State.CreateUserInputReady -> onCreateUserInputReady(it.paidOptionAvailable)
                    is SignupViewModel.State.CreateUserProcessing -> onCreateUserProcessing()
                    is SignupViewModel.State.CreateUserSuccess -> onCreateUserSuccess(it)
                    is SignupViewModel.State.Error.CreateUserCanceled -> Unit
                    is SignupViewModel.State.Error.PlanChooserCanceled -> Unit
                    is SignupViewModel.State.Error.Message -> onCreateUserError(it.message)
                }.exhaustive
            }
            .launchIn(lifecycleScope)
    }

    private fun observeLoginViewModelState() {
        loginViewModel.state
            .flowWithLifecycle(lifecycle)
            .distinctUntilChanged()
            .onEach {
                when (it) {
                    is LoginViewModel.State.Idle -> showLoading(false)
                    is LoginViewModel.State.Processing -> showLoading(true)
                    is LoginViewModel.State.Error -> onLoginError(it.error.getUserMessage(resources))
                    is LoginViewModel.State.AccountSetupResult -> onPostLoginAccountSetup(it.result)
                    is LoginViewModel.State.InvalidPassword -> onLoginError(it.error.getUserMessage(resources))
                    is LoginViewModel.State.ExternalAccountNotSupported -> {}
                    is LoginViewModel.State.SignInWithSso -> {}
                }.exhaustive
            }
            .launchIn(lifecycleScope)
    }

    private fun setPlanResultListener() {
        supportFragmentManager.setFragmentResultListener(KEY_PLAN_SELECTED, this) { _, bundle ->
            val plan = bundle.getParcelable<SelectedPlan>(BUNDLE_KEY_PLAN)
            val billingResult = bundle.getParcelable<BillingResult>(BUNDLE_KEY_BILLING_DETAILS)
            if (plan != null) {
                signUpViewModel.subscriptionDetails = SubscriptionDetails(
                    billingResult = billingResult,
                    planName = plan.planName,
                    planDisplayName = plan.planDisplayName,
                    cycle = plan.cycle.toSubscriptionCycle()
                )
                signUpViewModel.startCreateUserWorkflow()
            } else {
                supportFragmentManager.removePlansSignup()
                signUpViewModel.onPlanChooserCancel()
            }
        }
    }

    private fun onPostLoginAccountSetup(result: PostLoginAccountSetup.Result) {
        when (result) {
            is PostLoginAccountSetup.Result.Error.UnlockPrimaryKeyError -> onUnlockUserError(result.error)
            is PostLoginAccountSetup.Result.Error.UserCheckError -> onLoginError(result.error.localizedMessage)
            is PostLoginAccountSetup.Result.UserUnlocked -> onLoginSuccess(result.userId)
            is PostLoginAccountSetup.Result.Need.ChangePassword,
            is PostLoginAccountSetup.Result.Need.ChooseUsername,
            is PostLoginAccountSetup.Result.Need.SecondFactor,
            is PostLoginAccountSetup.Result.Need.TwoPassMode -> Unit // Ignored.
        }.exhaustive
    }

    private fun onCreateUserInputReady(paidOptionAvailable: Boolean) {
        if (paidOptionAvailable && !supportFragmentManager.hasPlanSignupFragment()) {
            supportFragmentManager.showPlansSignup(planInput = PlanInput())
        } else if (!paidOptionAvailable) {
            val selectedPlan = SelectedPlan.free(getString(R.string.plans_free_name))
            signUpViewModel.subscriptionDetails = SubscriptionDetails(
                billingResult = null,
                planName = selectedPlan.planName,
                planDisplayName = selectedPlan.planDisplayName,
                cycle = selectedPlan.cycle.toSubscriptionCycle()
            )
            signUpViewModel.startCreateUserWorkflow()
        }
    }

    private fun onCreateUserProcessing() {
        showLoading(true)

        supportFragmentManager
            .showCreatingUser()
            .setFragmentResultListener(CreatingUserFragment.FRAGMENT_RESULT_REQUEST_KEY) { _, bundle ->
                if (bundle.getBoolean(CreatingUserFragment.KEY_CANCELLED, false)) {
                    signUpViewModel.onCreateUserCancelled()
                }
            }
    }

    private fun onCreateUserSuccess(state: SignupViewModel.State.CreateUserSuccess) {
        val subscriptionDetails = signUpViewModel.subscriptionDetails
        val billingDetails = subscriptionDetails?.billingResult?.let {
            BillingDetails(
                amount = it.amount,
                currency = it.currency,
                cycle = it.cycle,
                planName = subscriptionDetails.planName,
                token = it.token?.let { value -> ProtonPaymentToken(value) },
                subscriptionManagement = it.subscriptionManagement
            )
        }

        loginViewModel.startLoginWorkflowWithEncryptedPassword(
            state.username,
            state.password,
            signUpViewModel.currentAccountType,
            billingDetails,
            loginMetricData = {
                SignupLoginTotal(
                    it.toHttpApiStatus(),
                    signUpViewModel.currentAccountType.toObservabilityAccountType()
                )
            },
            unlockUserMetricData = { SignupUnlockUserTotalV1(it.toUnlockUserStatus()) },
            userCheckMetricData = { SignupUserCheckTotalV1(it.toUserCheckStatus()) }
        )

        signUpViewModel.onSignupCompleted()
    }

    private fun onCreateUserError(message: String?) {
        supportFragmentManager.removeCreatingUser()
        showError(message)
    }

    private fun onLoginError(message: String? = null) {
        MaterialAlertDialogBuilder(this)
            .setCancelable(false)
            .setTitle(R.string.presentation_alert_title)
            .setMessage(message ?: getString(R.string.auth_login_general_error))
            .setPositiveButton(R.string.presentation_alert_ok) { _: DialogInterface, _: Int -> finish() }
            .show()
    }

    private fun onLoginSuccess(userId: UserId) {
        if (product == Product.Vpn) {
            signupDone(userId)
        } else {
            supportFragmentManager.showCongrats()
            supportFragmentManager.setFragmentResultListener(
                SignupFinishedFragment.KEY_START_USING_SELECTED, this
            ) { _, _ ->
                signupDone(userId)
            }
        }
    }

    private fun signupDone(userId: UserId) {
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(
                    ARG_RESULT,
                    with(signUpViewModel) {
                        SignUpResult(
                            accountType = currentAccountType,
                            username = username,
                            domain = domain,
                            email = externalEmail,
                            userId = userId.id
                        )
                    }
                )
            }
        )

        finish()
    }

    companion object {
        const val ARG_INPUT = "arg.signUpInput"
        const val ARG_RESULT = "arg.signUpResult"
    }
}
