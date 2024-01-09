/*
 * Copyright (c) 2020 Proton Technologies AG
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

package me.proton.core.payment.presentation.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.viewbinding.ViewBinding
import me.proton.core.payment.domain.entity.PaymentTokenResult
import me.proton.core.payment.domain.entity.ProtonPaymentToken
import me.proton.core.payment.domain.usecase.PaymentProvider
import me.proton.core.payment.presentation.R
import me.proton.core.payment.presentation.entity.BillingInput
import me.proton.core.payment.presentation.entity.BillingResult
import me.proton.core.payment.presentation.entity.CurrentSubscribedPlanDetails
import me.proton.core.payment.presentation.entity.PaymentOptionsResult
import me.proton.core.payment.presentation.entity.PaymentTokenApprovalInput
import me.proton.core.payment.presentation.entity.PlanShortDetails
import me.proton.core.presentation.ui.ProtonViewBindingActivity
import me.proton.core.presentation.utils.errorSnack
import me.proton.core.presentation.utils.showToast

internal abstract class PaymentsActivity<ViewBindingT : ViewBinding>(
    bindingInflater: (LayoutInflater) -> ViewBindingT
) : ProtonViewBindingActivity<ViewBindingT>(bindingInflater) {

    private var tokenApprovalLauncher: ActivityResultLauncher<PaymentTokenApprovalInput>? = null
    private var newBillingLauncher: ActivityResultLauncher<BillingInput>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenApprovalLauncher = registerForPaymentTokenApproval()
        newBillingLauncher = registerForNewBilling()
    }

    private fun registerForPaymentTokenApproval(): ActivityResultLauncher<PaymentTokenApprovalInput> =
        registerForActivityResult(
            StartPaymentTokenApproval
        ) {
            it?.apply {
                if (!approved) {
                    showToast(R.string.payments_3ds_not_approved)
                }
                onThreeDSApprovalResult(amount, ProtonPaymentToken(token), approved)
            }
        }

    private fun registerForNewBilling(): ActivityResultLauncher<BillingInput> =
        registerForActivityResult(
            StartBilling
        ) {
            onPaymentResult(it)
        }

    protected fun onTokenApprovalNeeded(
        userId: String?,
        paymentTokenResult: PaymentTokenResult.CreatePaymentTokenResult,
        amount: Long
    ) {
        tokenApprovalLauncher?.launch(
            PaymentTokenApprovalInput(
                userId,
                paymentTokenResult.token.value,
                paymentTokenResult.returnHost!!,
                paymentTokenResult.approvalUrl!!,
                amount
            )
        )
    }

    /**
     * Starts the billing flow which has support for multiple various payment providers that
     * the API decides which are currently available.
     * By default, the user can switch between all payment providers.
     *
     * @param singlePaymentProvider the flow could be invoked with a single payment provider even though
     * currently maybe more than one is available.
     */
    protected fun startBilling(
        userId: String?,
        currentPlans: List<CurrentSubscribedPlanDetails>,
        plan: PlanShortDetails,
        codes: List<String>?,
        singlePaymentProvider: PaymentProvider? = null
    ) {
        newBillingLauncher?.launch(
            BillingInput(userId, currentPlans, plan, codes, null, singlePaymentProvider)
        )
    }

    protected fun onPaymentResult(billingResult: BillingResult?) {
        if (billingResult != null) {
            val intent = Intent()
                .putExtra(ARG_RESULT, PaymentOptionsResult(billingResult.paySuccess, billingResult))
            setResult(RESULT_OK, intent)
            finish()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    open fun onThreeDSApprovalResult(amount: Long, token: ProtonPaymentToken, success: Boolean) {
        // no default operation
    }

    open fun showLoading(loading: Boolean) {
        // no default operation
    }

    open fun onError(message: String? = null) {
        // no default operation
    }

    open fun showError(message: String?) {
        showLoading(false)
        binding.root.errorSnack(message = message ?: getString(R.string.payments_general_error))
    }

    companion object {
        const val ARG_RESULT = "arg.billingResult"
    }
}
