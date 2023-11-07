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

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.auth.presentation.R
import me.proton.core.auth.presentation.databinding.FragmentSignupChooseInternalEmailBinding
import me.proton.core.auth.presentation.ui.onLongState
import me.proton.core.auth.presentation.viewmodel.signup.ChooseInternalEmailViewModel
import me.proton.core.auth.presentation.viewmodel.signup.ChooseInternalEmailViewModel.State
import me.proton.core.auth.presentation.viewmodel.signup.ChooseUsernameViewModel
import me.proton.core.auth.presentation.viewmodel.signup.SignupViewModel
import me.proton.core.observability.domain.metrics.SignupScreenViewTotalV1
import me.proton.core.presentation.utils.getUserMessage
import me.proton.core.presentation.utils.hideKeyboard
import me.proton.core.presentation.utils.launchOnScreenView
import me.proton.core.presentation.utils.onClick
import me.proton.core.presentation.utils.onFailure
import me.proton.core.presentation.utils.onSuccess
import me.proton.core.presentation.utils.showToast
import me.proton.core.presentation.utils.validateUsername
import me.proton.core.presentation.utils.viewBinding
import me.proton.core.telemetry.presentation.annotation.ProductMetrics
import me.proton.core.telemetry.presentation.annotation.ViewClicked
import me.proton.core.telemetry.presentation.annotation.ViewFocused
import me.proton.core.user.domain.entity.Domain
import me.proton.core.util.kotlin.exhaustive

@AndroidEntryPoint
@ProductMetrics(
    group = "account.android.signup",
    flow = "mobile_signup_full"
)
@ViewClicked(
    event = "user.signup.clicked",
    viewIds = ["nextButton", "switchButton", "domainInput"]
)
@ViewFocused(
    event = "user.signup.focused",
    viewIds = ["usernameInput"]
)
class ChooseInternalEmailFragment : SignupFragment(R.layout.fragment_signup_choose_internal_email) {

    private val viewModel by viewModels<ChooseInternalEmailViewModel>()
    private val signupViewModel by activityViewModels<SignupViewModel>()
    private val binding by viewBinding(FragmentSignupChooseInternalEmailBinding::bind)

    private val creatableAccountType by lazy {
        AccountType.valueOf(requireNotNull(requireArguments().getString(ARG_INPUT_ACCOUNT_TYPE)))
    }

    private val username by lazy { requireArguments().getString(ARG_INPUT_USERNAME) }
    private val domain by lazy { requireArguments().getString(ARG_INPUT_DOMAIN) }

    override fun onBackPressed() {
        signupViewModel.onFinish()
        activity?.finish()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

            usernameInput.apply {
                setOnFocusLostListener { _, _ ->
                    validateUsername()
                        .onFailure { setInputError() }
                        .onSuccess { clearInputError() }
                }
            }

            nextButton.onClick(::onNextClicked)
            switchButton.onClick(::onSwitchClicked)

            when (creatableAccountType) {
                AccountType.Username -> Unit
                AccountType.Internal -> {
                    switchButton.visibility = View.GONE
                    separatorView.visibility = View.GONE
                    footnoteText.visibility = View.GONE
                }
                AccountType.External -> {
                    switchButton.visibility = View.VISIBLE
                    separatorView.visibility = View.VISIBLE
                    footnoteText.visibility = View.VISIBLE
                }
            }
        }

        viewModel.preFill(username, domain)
        viewModel.state
            .flowWithLifecycle(lifecycle)
            .distinctUntilChanged()
            .onEach {
                when (it) {
                    is State.Idle -> showLoading(false)
                    is State.Processing -> showLoading(true)
                    is State.Ready -> onReady(it.username, it.domain, it.domains)
                    is State.Success -> onUsernameAvailable(it.username, it.domain)
                    is State.Error.DomainsNotAvailable -> onDomainsNotAvailable(it.error.getUserMessage(resources))
                    is State.Error.Message -> onError(it.error.getUserMessage(resources))
                }.exhaustive
            }
            .onLongState(ChooseUsernameViewModel.State.Processing) {
                requireContext().showToast(getString(R.string.auth_long_signup))
            }
            .launchIn(lifecycleScope)

        launchOnScreenView {
            signupViewModel.onScreenView(SignupScreenViewTotalV1.ScreenId.chooseInternalEmail)
        }
    }

    private fun onNextClicked() {
        with(binding.usernameInput) {
            lifecycleScope.launch { flush() }
            hideKeyboard()
            validateUsername()
                .onFailure { setInputError(getString(R.string.auth_signup_error_username_blank)) }
                .onSuccess { username ->
                    val domain = binding.domainInput.text.toString().replace("@", "")
                    viewModel.checkUsername(username, domain)
                }
        }
    }

    private fun onSwitchClicked() {
        parentFragmentManager.replaceByExternalEmailChooser(creatableAccountType)
    }

    private fun onReady(username: String?, domain: String?, domains: List<Domain>) {
        val isLoading = domains.isEmpty()
        showLoading(isLoading)
        with(binding) {
            nextButton.isEnabled = !isLoading
            switchButton.isEnabled = true

            usernameInput.text = username
            val items = domains.map { "@$it" }
            domainInput.text = items.firstOrNull { it.contains(domain.orEmpty()) }
            domainInput.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.list_item_domain,
                    R.id.title,
                    items
                )
            )
        }
    }

    private fun onUsernameAvailable(username: String, domain: String) {
        showLoading(false)
        binding.nextButton.isEnabled = true
        signupViewModel.currentAccountType = AccountType.Internal
        signupViewModel.username = username
        signupViewModel.domain = domain
        parentFragmentManager.showPasswordChooser()
    }

    private fun onDomainsNotAvailable(message: String?) {
        showLoading(false)
        binding.nextButton.isEnabled = false
        showIndefiniteError(message, getString(R.string.presentation_retry)) {
            onRetryClicked()
        }
    }

    private fun onRetryClicked() {
        viewModel.getDomains()
    }

    private fun onError(message: String?) {
        binding.nextButton.setIdle()
        showError(message)
    }

    override fun showLoading(loading: Boolean) = with(binding) {
        if (loading) {
            nextButton.setLoading()
        } else {
            nextButton.setIdle()
        }
    }

    companion object {
        const val ARG_INPUT_ACCOUNT_TYPE = "arg.accountType"
        const val ARG_INPUT_USERNAME = "arg.username"
        const val ARG_INPUT_DOMAIN = "arg.domain"

        operator fun invoke(
            creatableAccountType: AccountType,
            username: String?,
            domain: String?
        ) = ChooseInternalEmailFragment().apply {
            arguments = bundleOf(
                ARG_INPUT_ACCOUNT_TYPE to creatableAccountType.name,
                ARG_INPUT_USERNAME to username,
                ARG_INPUT_DOMAIN to domain
            )
        }
    }
}
