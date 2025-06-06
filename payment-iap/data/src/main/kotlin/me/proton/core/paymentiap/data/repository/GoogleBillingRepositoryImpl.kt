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

package me.proton.core.paymentiap.data.repository

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.payment.domain.entity.GoogleBillingFlowParams
import me.proton.core.payment.domain.entity.GoogleBillingResult
import me.proton.core.payment.domain.entity.GoogleProductDetails
import me.proton.core.payment.domain.entity.GooglePurchase
import me.proton.core.payment.domain.entity.GooglePurchaseToken
import me.proton.core.payment.domain.entity.ProductId
import me.proton.core.payment.domain.repository.BillingClientError
import me.proton.core.payment.domain.repository.ConnectedBillingClientInterface
import me.proton.core.payment.domain.repository.GoogleBillingRepository
import me.proton.core.paymentiap.domain.BillingClientFactory
import me.proton.core.paymentiap.domain.LogTag
import me.proton.core.paymentiap.domain.entity.unwrap
import me.proton.core.paymentiap.domain.entity.wrap
import me.proton.core.paymentiap.domain.isLoggable
import me.proton.core.paymentiap.domain.isRetryable
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.coroutine.result
import me.proton.core.util.kotlin.retry
import javax.inject.Inject

public class GoogleBillingRepositoryImpl @Inject internal constructor(
    connectedBillingClientFactory: ConnectedBillingClientFactory,
    dispatcherProvider: DispatcherProvider,
) : GoogleBillingRepository<Activity> {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.Main)

    private val _purchaseUpdated =
        MutableSharedFlow<Pair<GoogleBillingResult, List<GooglePurchase>?>>(extraBufferCapacity = 10)
    public override val purchaseUpdated: Flow<Pair<GoogleBillingResult, List<GooglePurchase>?>> = _purchaseUpdated

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchaseList ->
        scope.launch {
            _purchaseUpdated.emit(billingResult.wrap() to purchaseList?.map { it.wrap() })
        }
    }

    private val connectedBillingClient = connectedBillingClientFactory(purchasesUpdatedListener)

    override suspend fun acknowledgePurchase(
        purchaseToken: GooglePurchaseToken
    ): Unit = result("acknowledgePurchase") {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken.value)
            .build()
        retry(predicate = ::isRetryable) {
            val result = connectedBillingClient.withClient { it.acknowledgePurchase(params) }
            result.checkOk(LogTag.GIAP_ERROR_ACK)
        }
    }

    override suspend fun canQueryProductDetails(): Boolean {
        return try {
            retry(predicate = ::isRetryable) {
                val result = connectedBillingClient.withClient { it.isFeatureSupported(FeatureType.PRODUCT_DETAILS) }
                result.checkOk(LogTag.GIAP_ERROR_QUERY_PRODUCT)
                true
            }
        } catch (_: BillingClientError) {
            false
        }
    }

    override fun destroy() {
        scope.cancel()
        connectedBillingClient.destroy()
    }

    override suspend fun getProductsDetails(
        googlePlayPlanNames: List<ProductId>
    ): List<GoogleProductDetails>? = result("getProductsDetails") {
        val products = googlePlayPlanNames.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId.id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        val result = retry(predicate = ::isRetryable) {
            connectedBillingClient
                .withClient { client ->
                    client.queryProductDetails(params).also { details ->
                        if (details.billingResult.responseCode == BillingResponseCode.OK && details.productDetailsList.isNullOrEmpty()) {
                            logProductsNotFound(client, googlePlayPlanNames, details)
                        }
                    }
                }
                .also {
                    it.billingResult.checkOk(
                        LogTag.GIAP_ERROR_QUERY_PRODUCT,
                        "Products query error: $googlePlayPlanNames."
                    )
                }
        }
        val productDetails = result.productDetailsList
        productDetails?.map { it.wrap() }
    }

    override suspend fun launchBillingFlow(
        block: suspend (ConnectedBillingClientInterface<Activity>) -> Unit
    ): Unit = result("launchBillingFlow") {
        block(connectedBillingClient)
    }

    override suspend fun querySubscriptionPurchases(): List<GooglePurchase> = result("querySubscriptionPurchases") {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        val result = retry(predicate = ::isRetryable) {
            connectedBillingClient
                .withClient { it.queryPurchasesAsync(params) }
                .also { it.billingResult.checkOk(LogTag.GIAP_ERROR_QUERY_PURCHASE) }
        }
        result.purchasesList.map { it.wrap() }
    }

    private fun BillingResult.checkOk(logTag: String, message: String? = null) {
        if (responseCode != BillingResponseCode.OK) {
            val error = BillingClientError(responseCode, debugMessage)
            if (error.isLoggable()) error.logError(logTag, message)
            throw error
        }
    }

    private fun isRetryable(error: Throwable) = when (error) {
        is BillingClientError -> error.isRetryable()
        else -> false
    }

    private fun BillingClientError.logError(logTag: String, message: String?) = when (message) {
        null -> CoreLogger.e(tag = logTag, e = this)
        else -> CoreLogger.e(tag = logTag, e = this, message = message)
    }

    private fun logProductsNotFound(
        client: BillingClient,
        googlePlayPlanNames: List<ProductId>,
        result: ProductDetailsResult
    ) {
        val connState = when (client.connectionState) {
            BillingClient.ConnectionState.DISCONNECTED -> "DISCONNECTED"
            BillingClient.ConnectionState.CONNECTING -> "CONNECTING"
            BillingClient.ConnectionState.CONNECTED -> "CONNECTED"
            BillingClient.ConnectionState.CLOSED -> "CLOSED"
            else -> "UNKNOWN(${client.connectionState})"
        }

        val isConnected = client.connectionState == BillingClient.ConnectionState.CONNECTED
        val productDetailsSupported = when {
            isConnected -> client.isFeatureSupported(FeatureType.PRODUCT_DETAILS).responseCode == BillingResponseCode.OK
            else -> null
        }
        val subscriptionsSupported = when {
            isConnected -> client.isFeatureSupported(FeatureType.SUBSCRIPTIONS).responseCode == BillingResponseCode.OK
            else -> null
        }

        CoreLogger.e(
            LogTag.GIAP_ERROR_QUERY_PRODUCT,
            "Products not found: $googlePlayPlanNames list=${result.productDetailsList} " +
                    "connectionState=$connState productDetailsSupported=$productDetailsSupported " +
                    "subscriptionsSupported=$subscriptionsSupported."
        )
    }
}

@AssistedFactory
internal interface ConnectedBillingClientFactory {
    operator fun invoke(purchasesUpdatedListener: PurchasesUpdatedListener): ConnectedBillingClient
}

/** Manages access to [BillingClient], ensuring we are connected, before calling any of its methods. */
internal class ConnectedBillingClient @AssistedInject constructor(
    billingClientFactory: BillingClientFactory,
    @Assisted private val purchasesUpdatedListener: PurchasesUpdatedListener
) : ConnectedBillingClientInterface<Activity>, BillingClientStateListener {
    private val billingClient = billingClientFactory(purchasesUpdatedListener)
    private val connectionState = MutableStateFlow<BillingClientConnectionState>(BillingClientConnectionState.Idle)

    fun destroy() {
        billingClient.endConnection()
        connectionState.value = BillingClientConnectionState.Destroyed
    }

    suspend fun <T> withClient(body: suspend (BillingClient) -> T): T {
        waitForConnection()
        return body(billingClient)
    }

    override suspend fun launchBilling(activity: Activity, billingFlowParams: GoogleBillingFlowParams) {
        withClient { it.launchBillingFlow(activity, billingFlowParams.unwrap()) }
    }

    override fun onBillingServiceDisconnected() {
        connectionState.value = BillingClientConnectionState.Error(
            BillingResponseCode.SERVICE_DISCONNECTED,
            "Service disconnected."
        )
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingResponseCode.OK) {
            connectionState.value = BillingClientConnectionState.Connected
        } else {
            connectionState.value =
                BillingClientConnectionState.Error(billingResult.responseCode, billingResult.debugMessage)
        }
    }

    private fun connect() {
        if (connectionState.value.isConnectingOrConnected()) return
        connectionState.value = BillingClientConnectionState.Connecting
        billingClient.startConnection(this)
    }

    private suspend fun waitForConnection() {
        val currentConnectionState = connectionState.value
        check(currentConnectionState != BillingClientConnectionState.Destroyed) {
            "Billing client has already been destroyed."
        }
        connect()
        connectionState
            .onEach {
                if (it is BillingClientConnectionState.Error) {
                    throw BillingClientError(it.responseCode, it.debugMessage)
                }
            }
            .first { it == BillingClientConnectionState.Connected }
    }

    private sealed class BillingClientConnectionState {
        object Idle : BillingClientConnectionState()
        object Connecting : BillingClientConnectionState()
        object Connected : BillingClientConnectionState()
        object Destroyed : BillingClientConnectionState()
        data class Error(val responseCode: Int?, val debugMessage: String?) : BillingClientConnectionState()

        fun isConnectingOrConnected(): Boolean = this is Connecting || this is Connected
    }
}
