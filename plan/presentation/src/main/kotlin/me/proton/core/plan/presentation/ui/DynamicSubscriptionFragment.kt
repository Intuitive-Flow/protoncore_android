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

package me.proton.core.plan.presentation.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onEach
import me.proton.core.network.presentation.util.getUserMessage
import me.proton.core.plan.domain.entity.DynamicDecoration
import me.proton.core.plan.domain.entity.DynamicSubscription
import me.proton.core.plan.presentation.R
import me.proton.core.plan.presentation.databinding.FragmentDynamicSubscriptionBinding
import me.proton.core.plan.presentation.entity.DynamicUser
import me.proton.core.plan.presentation.entity.PlanCycle
import me.proton.core.plan.presentation.entity.toStringRes
import me.proton.core.plan.presentation.view.formatRenew
import me.proton.core.plan.presentation.view.toView
import me.proton.core.plan.presentation.viewmodel.DynamicSubscriptionViewModel
import me.proton.core.plan.presentation.viewmodel.DynamicSubscriptionViewModel.Action
import me.proton.core.plan.presentation.viewmodel.DynamicSubscriptionViewModel.State
import me.proton.core.presentation.ui.ProtonFragment
import me.proton.core.presentation.utils.formatCentsPriceDefaultLocale
import me.proton.core.presentation.utils.launchOnScreenView
import me.proton.core.presentation.utils.onClick
import me.proton.core.presentation.utils.openMarketSubscription
import me.proton.core.presentation.utils.viewBinding

@Suppress("TooManyFunctions")
@AndroidEntryPoint
class DynamicSubscriptionFragment : ProtonFragment(R.layout.fragment_dynamic_subscription) {

    private val binding by viewBinding(FragmentDynamicSubscriptionBinding::bind)
    private val viewModel by viewModels<DynamicSubscriptionViewModel>()

    fun setUser(user: DynamicUser) {
        viewModel.perform(Action.SetUser(user))
    }

    fun reload() {
        viewModel.perform(Action.Load)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.onEach {
            when (it) {
                is State.Loading -> onLoading()
                is State.Error -> onError(it.error)
                is State.UserNotExist -> onNoPrimaryUser()
                is State.Success -> onSuccess(it)
            }
        }.launchInViewLifecycleScope()

        binding.retry.onClick { viewModel.perform(Action.Load) }

        launchOnScreenView {
            viewModel.onScreenView()
        }
    }

    private fun onLoading() {
        showLoading(true)
    }

    private fun onError(error: Throwable?) {
        showLoading(false)
        val message = error?.getUserMessage(resources)
        showError(message ?: getString(R.string.presentation_error_general))
    }

    private fun onNoPrimaryUser() {
        showLoading(true)
        showError(getString(R.string.presentation_error_general))
    }

    private fun onSuccess(state: State.Success) {
        showLoading(false)
        showSubscription(
            state.dynamicSubscription,
            state.canUpgradeFromMobile,
            state.hasCredits,
            state.userCurrency,
            state.currentStoreProductId
        )
    }

    private fun showLoading(loading: Boolean) = with(binding) {
        progress.isVisible = loading
        errorLayout.isVisible = false
    }

    private fun showError(message: String) = with(binding) {
        errorLayout.isVisible = true
        error.text = message
    }

    private fun showSubscription(
        subscription: DynamicSubscription,
        canUpgradeFromMobile: Boolean,
        hasCredits: Boolean,
        userCurrency: String,
        currentStoreProductId: String?
    ) = with(binding.dynamicPlan) {
        title = subscription.title
        description = subscription.description
        starred = subscription.decorations.filterIsInstance<DynamicDecoration.Starred>().isNotEmpty()
        val price = subscription.amount?.toDouble()
        val renewPrice = subscription.renewAmount?.toDouble() ?: price // if renew price (from google) is null, the assumption is they are equal
        val currency = subscription.currency ?: userCurrency
        priceText = price?.formatCentsPriceDefaultLocale(currency)
        val renewPriceText = renewPrice?.formatCentsPriceDefaultLocale(currency)
        priceCycle = subscription.cycleDescription
        renewalText = when {
            subscription.renew == null -> null
            subscription.periodEnd == null -> null
            else -> formatRenew(
                context,
                subscription.renew ?: false,
                requireNotNull(subscription.periodEnd),
                priceText,
                renewPriceText,
                PlanCycle.map[subscription.cycleMonths] ?: PlanCycle.FREE
            )
        }
        isCollapsable = false
        entitlements.removeAllViews()
        subscription.entitlements.forEach { entitlements.addView(it.toView(context)) }
    }.also {
        with(binding) {
            subscription.external.toStringRes(canUpgradeFromMobile)?.let {
                managementInfo.setText(it)
                managementInfo.isVisible = true
            }
            playStoreSubscriptionManagement.isVisible = canUpgradeFromMobile
            creditsInfo.isVisible = hasCredits
            playStoreSubscriptionManagement.onClick {
                requireContext().openMarketSubscription(currentStoreProductId)
            }
        }
    }
}
