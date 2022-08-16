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

package me.proton.core.paymentcommon.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.country.domain.usecase.GetCountry
import me.proton.core.domain.entity.UserId
import me.proton.core.humanverification.domain.HumanVerificationManager
import me.proton.core.network.domain.client.ClientIdProvider
import me.proton.core.paymentcommon.domain.entity.Card
import me.proton.core.paymentcommon.domain.entity.Currency
import me.proton.core.paymentcommon.domain.entity.PaymentToken
import me.proton.core.paymentcommon.domain.entity.PaymentTokenStatus
import me.proton.core.paymentcommon.domain.entity.PaymentType
import me.proton.core.paymentcommon.domain.entity.Subscription
import me.proton.core.paymentcommon.domain.entity.SubscriptionCycle
import me.proton.core.paymentcommon.domain.entity.SubscriptionStatus
import me.proton.core.paymentcommon.domain.usecase.CreatePaymentTokenWithExistingPaymentMethod
import me.proton.core.paymentcommon.domain.usecase.CreatePaymentTokenWithNewCreditCard
import me.proton.core.paymentcommon.domain.usecase.CreatePaymentTokenWithNewPayPal
import me.proton.core.paymentcommon.domain.usecase.PerformSubscribe
import me.proton.core.paymentcommon.domain.usecase.ValidateSubscriptionPlan
import me.proton.core.paymentcommon.presentation.adjustExpirationYear
import me.proton.core.paymentcommon.presentation.entity.BillingResult
import me.proton.core.paymentcommon.presentation.entity.CurrentSubscribedPlanDetails
import me.proton.core.plan.domain.entity.MASK_MAIL
import me.proton.core.plan.domain.entity.MASK_VPN
import me.proton.core.plan.domain.entity.PLAN_ADDON
import me.proton.core.plan.domain.entity.PLAN_PRODUCT
import me.proton.core.plan.domain.entity.Plan
import me.proton.core.presentation.viewmodel.ProtonViewModel
import me.proton.core.util.kotlin.exhaustive
import me.proton.core.util.kotlin.hasFlag
import javax.inject.Inject

/**
 * ViewModel to serve the billing activity or fragment.
 * It's responsibility is to provide payments functionality.
 */
public class BillingCommonViewModel @Inject constructor(
    private val validatePlanSubscription: ValidateSubscriptionPlan,
    private val createPaymentTokenWithNewCreditCard: CreatePaymentTokenWithNewCreditCard,
    private val createPaymentTokenWithNewPayPal: CreatePaymentTokenWithNewPayPal,
    private val createPaymentTokenWithExistingPaymentMethod: CreatePaymentTokenWithExistingPaymentMethod,
    private val performSubscribe: PerformSubscribe,
    private val getCountry: GetCountry,
    private val humanVerificationManager: HumanVerificationManager,
    private val clientIdProvider: ClientIdProvider,
) : ProtonViewModel() {

    private val _subscriptionState = MutableStateFlow<State>(State.Idle)
    private val _plansValidationState = MutableStateFlow<PlansValidationState>(PlansValidationState.Idle)

    public val subscriptionResult: StateFlow<State> = _subscriptionState.asStateFlow()
    public val plansValidationState: StateFlow<PlansValidationState> = _plansValidationState.asStateFlow()

    /**
     * Represents main subscription state sealed class with all possible outcomes as subclasses needed to inform the
     * call site for what is go
     * ing on in the process, as well as the outcome of it.
     */
    public sealed class State {
        public object Idle : State()
        public object Processing : State()

        public sealed class Success : State() {
            public data class SubscriptionPlanValidated(val subscriptionStatus: SubscriptionStatus) : Success()
            public data class SubscriptionCreated(
                val amount: Long,
                val currency: Currency,
                val cycle: SubscriptionCycle,
                val subscriptionStatus: Subscription,
                val paymentToken: String?
            ) : Success()

            public data class TokenCreated(val paymentToken: PaymentToken.CreatePaymentTokenResult) : Success()
            public data class SignUpTokenReady(
                val amount: Long,
                val currency: Currency,
                val cycle: SubscriptionCycle,
                val paymentToken: String
            ) : Success()
        }

        public sealed class Incomplete : State() {
            public data class TokenApprovalNeeded(
                val paymentToken: PaymentToken.CreatePaymentTokenResult,
                val amount: Long
            ) :
                Incomplete()
        }

        public sealed class Error : State() {
            public data class General(val error: Throwable) : Error()
            public object SignUpWithPaymentMethodUnsupported : Error()
        }
    }

    public sealed class PlansValidationState {
        public object Idle : PlansValidationState()
        public object Processing : PlansValidationState()
        public data class Success(val subscription: SubscriptionStatus) : PlansValidationState()

        public sealed class Error : PlansValidationState() {
            public data class Message(val message: String?) : Error()
        }
    }

    /**
     * Creates new subscription with a completely new credit card that user will enter details for.
     * If you want to pay for a subscription during sign up, please use:
     *  - [PaymentType.CreditCard] this is only supported.
     * If you want to pay with existing payment method id (previously saved), please use:
     *  - [PaymentType.PaymentMethod]
     *  Otherwise if you want to pay for an upgrade when the user is logged in, please use:
     *  - [PaymentType.CreditCard] or [PaymentType.PayPal]
     */
    public fun subscribe(
        userId: UserId?,
        planNames: List<String>,
        codes: List<String>? = null,
        currency: Currency,
        cycle: SubscriptionCycle,
        paymentType: PaymentType
    ): Job = flow {
        emit(State.Processing)
        val signUp = userId == null

        val subscription = validatePlanSubscription(userId, codes, planNames, currency, cycle)
        emit(State.Success.SubscriptionPlanValidated(subscription))

        if (!signUp && subscription.amountDue == 0L) {
            // directly create the subscription
            val subscriptionResult =
                performSubscribe(userId!!, 0, currency, cycle, planNames, codes, paymentToken = null)
            emit(State.Success.SubscriptionCreated(0, currency, cycle, subscriptionResult, null))
        } else {
            if (signUp && paymentType is PaymentType.PaymentMethod) {
                emit(State.Error.SignUpWithPaymentMethodUnsupported)
                return@flow
            }
            // region create payment token with the amountDue (will most probably lead to a token approval screen).
            val paymentTokenResult = when (paymentType) {
                is PaymentType.PaymentMethod -> {
                    createPaymentTokenWithExistingPaymentMethod(
                        userId = userId,
                        amount = subscription.amountDue,
                        currency = currency,
                        paymentMethodId = paymentType.paymentMethodId
                    )
                }
                is PaymentType.CreditCard -> {
                    val countryName = paymentType.card.country
                    val country = getCountry(countryName)
                    val paymentTypeRefined =
                        paymentType.copy(
                            card = (paymentType.card as Card.CardWithPaymentDetails).copy(
                                country = country?.code ?: countryName,
                                expirationYear = paymentType.card.expirationYear.adjustExpirationYear()
                            )
                        )
                    createPaymentTokenWithNewCreditCard(userId, subscription.amountDue, currency, paymentTypeRefined)
                }
                is PaymentType.PayPal -> createPaymentTokenWithNewPayPal(
                    userId,
                    subscription.amountDue,
                    currency,
                    paymentType
                )
            }.exhaustive
            emit(State.Success.TokenCreated(paymentTokenResult))

            if (paymentTokenResult.status == PaymentTokenStatus.CHARGEABLE) {
                val amount = subscription.amountDue
                val token = paymentTokenResult.token
                emit(onTokenApproved(userId, planNames, codes, amount, currency, cycle, token))
            } else {
                // should show 3DS approval URL WebView
                emit(State.Incomplete.TokenApprovalNeeded(paymentTokenResult, subscription.amountDue))
            }
            // endregion
        }
    }.catch {
        _subscriptionState.tryEmit(State.Error.General(it))
    }.onEach {
        _subscriptionState.tryEmit(it)
    }.launchIn(viewModelScope)

    /**
     * Completes the subscription after payment is 3DS approved in a WebView.
     * Should be called only for 3DS credit cards and only after the token has been approved.
     */
    public fun onThreeDSTokenApproved(
        userId: UserId?,
        planIds: List<String>,
        codes: List<String>? = null,
        amount: Long,
        currency: Currency,
        cycle: SubscriptionCycle,
        token: String
    ): Job = flow {
        emit(onTokenApproved(userId, planIds, codes, amount, currency, cycle, token))
    }.catch {
        _subscriptionState.tryEmit(State.Error.General(it))
    }.onEach {
        _subscriptionState.tryEmit(it)
    }.launchIn(viewModelScope)

    /**
     * Validates the subscription plan with the API. It should return more info for later payment processing, such as
     * the real amount (amountDue) which is the plan amount minus any coupons or credits (already on the account).
     */
    public fun validatePlan(
        userId: UserId?,
        plans: List<String>,
        codes: List<String>? = null,
        currency: Currency,
        cycle: SubscriptionCycle
    ): Job = flow {
        emit(PlansValidationState.Processing)
        emit(PlansValidationState.Success(validatePlanSubscription(userId, codes, plans, currency, cycle)))
    }.catch { error ->
        _plansValidationState.tryEmit(PlansValidationState.Error.Message(error.message))
    }.onEach { subscriptionState ->
        this._plansValidationState.tryEmit(subscriptionState)
    }.launchIn(viewModelScope)

    private suspend fun onTokenApproved(
        userId: UserId?,
        planNames: List<String>,
        codes: List<String>? = null,
        amount: Long,
        currency: Currency,
        cycle: SubscriptionCycle,
        token: String
    ): State =
        if (userId == null) {
            // Token will be used during sign up (create user), as part of HumanVerification headers.
            // Subscription will be performed during login, just after create user.
            // Token will be cleared by PerformSubscribe.
            val clientId = requireNotNull(clientIdProvider.getClientId(sessionId = null))
            humanVerificationManager.addDetails(BillingResult.paymentDetails(clientId = clientId, token = token))
            State.Success.SignUpTokenReady(amount, currency, cycle, token)
        } else {
            State.Success.SubscriptionCreated(
                amount = amount,
                currency = currency,
                cycle = cycle,
                subscriptionStatus = performSubscribe(userId, amount, currency, cycle, planNames, codes, token),
                paymentToken = token
            )
        }

    public companion object {
        public fun List<Plan>.createSubscriptionPlansList(planName: String, services: Int, type: Int): List<String> {
            return map {
                CurrentSubscribedPlanDetails(it.name, it.services, it.type)
            }.buildPlansList(planName, services, type)
        }

        public fun List<CurrentSubscribedPlanDetails>.buildPlansList(
            planName: String,
            services: Int,
            type: Int
        ): List<String> {
            val currentPlanHasProductPlan = any { it.type == PLAN_PRODUCT }
            val currentPlanNames = map { it.name }

            // there could be only single type 1 (product) in the list, except mail & vpn
            return when {
                currentPlanHasProductPlan && type == PLAN_PRODUCT -> {
                    if (canAppendNewPlan(services)) currentPlanNames.plus(planName) else listOf(planName)
                }
                currentPlanHasProductPlan && type == PLAN_ADDON -> {
                    currentPlanNames.plus(planName)
                }
                else -> currentPlanNames.plus(planName)
            }.distinct()
        }

        private fun List<CurrentSubscribedPlanDetails>.canAppendNewPlan(services: Int): Boolean {
            val hasCurrentVpnProductPlan = hasServiceFor(MASK_VPN)
            val hasCurrentMailProductPlan = hasServiceFor(MASK_MAIL)

            val newPlanIsVpnProductPlan = MASK_VPN.and(services) == MASK_VPN
            val newPlanIsMailProductPlan = MASK_MAIL.and(services) == MASK_MAIL

            return when {
                newPlanIsMailProductPlan && hasCurrentMailProductPlan -> false
                newPlanIsVpnProductPlan && hasCurrentVpnProductPlan -> false
                else -> true
            }
        }

        private fun List<CurrentSubscribedPlanDetails>.hasServiceFor(mask: Int) = any { it.hasServiceFor(mask) }

        private fun CurrentSubscribedPlanDetails.hasServiceFor(mask: Int): Boolean = (services ?: 0).hasFlag(mask)
    }
}