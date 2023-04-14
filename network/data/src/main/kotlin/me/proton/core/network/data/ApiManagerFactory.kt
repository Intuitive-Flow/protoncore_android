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
package me.proton.core.network.data

import android.content.Context
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.plus
import me.proton.core.domain.entity.Product
import me.proton.core.network.data.di.Constants
import me.proton.core.network.data.doh.DnsOverHttpsProviderRFC8484
import me.proton.core.network.data.protonApi.BaseRetrofitApi
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.ApiErrorHandler
import me.proton.core.network.domain.ApiManager
import me.proton.core.network.domain.ApiManagerImpl
import me.proton.core.network.domain.DohProvider
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkPrefs
import me.proton.core.network.domain.client.ClientIdProvider
import me.proton.core.network.domain.client.ClientVersionValidator
import me.proton.core.network.domain.client.ExtraHeaderProvider
import me.proton.core.network.domain.deviceverification.DeviceVerificationListener
import me.proton.core.network.domain.deviceverification.DeviceVerificationProvider
import me.proton.core.network.domain.handlers.DeviceVerificationNeededHandler
import me.proton.core.network.domain.handlers.DohApiHandler
import me.proton.core.network.domain.handlers.HumanVerificationInvalidHandler
import me.proton.core.network.domain.handlers.HumanVerificationNeededHandler
import me.proton.core.network.domain.handlers.MissingScopeHandler
import me.proton.core.network.domain.handlers.ProtonForceUpdateHandler
import me.proton.core.network.domain.handlers.TokenErrorHandler
import me.proton.core.network.domain.humanverification.HumanVerificationListener
import me.proton.core.network.domain.humanverification.HumanVerificationProvider
import me.proton.core.network.domain.scopes.MissingScopeListener
import me.proton.core.network.domain.server.ServerTimeListener
import me.proton.core.network.domain.serverconnection.DohAlternativesListener
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.util.kotlin.ProtonCoreConfig
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Factory for creating [ApiManager] instances. There should be a single instance per [baseUrl].
 *
 * @param baseUrl Base url for the api e.g. "https://api.protonvpn.ch/"
 * @param cookieStore The storage for cookies.
 * @param cache [Cache] shared across all user, session, api or call.
 */
@Suppress("LongParameterList")
class ApiManagerFactory(
    private val context: Context,
    private val product: Product,
    private val baseUrl: HttpUrl,
    private val apiClient: ApiClient,
    private val clientIdProvider: ClientIdProvider,
    private val serverTimeListener: ServerTimeListener,
    private val networkManager: NetworkManager,
    private val prefs: NetworkPrefs,
    private val sessionProvider: SessionProvider,
    private val sessionListener: SessionListener,
    private val humanVerificationProvider: HumanVerificationProvider,
    private val humanVerificationListener: HumanVerificationListener,
    private val deviceVerificationProvider: DeviceVerificationProvider,
    private val deviceVerificationListener: DeviceVerificationListener,
    private val missingScopeListener: MissingScopeListener,
    private val cookieStore: ProtonCookieStore,
    scope: CoroutineScope,
    private val certificatePins: Array<String> = Constants.DEFAULT_SPKI_PINS,
    private val alternativeApiPins: List<String> = Constants.ALTERNATIVE_API_SPKI_PINS,
    private val cache: () -> Cache? = { null },
    private val extraHeaderProvider: ExtraHeaderProvider? = null,
    private val clientVersionValidator: ClientVersionValidator,
    private val dohAlternativesListener: DohAlternativesListener?,
    private val dohProviderUrls: Array<String> = Constants.DOH_PROVIDERS_URLS,
    private val okHttpClient: OkHttpClient
) {

    @OptIn(ObsoleteCoroutinesApi::class)
    private val mainScope = scope + newSingleThreadContext("core.network.main")

    internal val jsonConverter = ProtonCoreConfig
        .defaultJsonStringFormat
        .asConverterFactory("application/json".toMediaType())

    @VisibleForTesting
    val baseOkHttpClient by lazy {
        require(clientVersionValidator.validate(apiClient.appVersionHeader)) {
            "Invalid app version code: ${apiClient.appVersionHeader}."
        }
        okHttpClient.newBuilder()
            .cache(cache())
            .callTimeout(apiClient.callTimeoutSeconds, TimeUnit.SECONDS)
            .connectTimeout(apiClient.connectTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(apiClient.writeTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(apiClient.readTimeoutSeconds, TimeUnit.SECONDS)
            .cookieJar(cookieStore)
            .build()
    }

    private val dohServices by lazy {
        dohProviderUrls.map { serviceUrl ->
            DnsOverHttpsProviderRFC8484(baseOkHttpClient, serviceUrl, apiClient, networkManager)
        }
    }

    private val protonDohService by lazy {
        val url = requireNotNull(baseUrl.resolve("/dns-query/")?.toString())
        DnsOverHttpsProviderRFC8484(baseOkHttpClient, url, apiClient, networkManager)
    }

    private fun javaMonoClockMs(): Long = SystemClock.elapsedRealtime()
    private fun javaWallClockMs(): Long = System.currentTimeMillis()

    internal fun <Api> createBaseErrorHandlers(
        sessionId: SessionId?,
        monoClockMs: () -> Long,
        dohApiHandler: DohApiHandler<Api>,
    ): List<ApiErrorHandler<Api>> {
        val tokenErrorHandler = TokenErrorHandler<Api>(sessionId, sessionProvider, sessionListener, monoClockMs)
        val missingScopeHandler =
            MissingScopeHandler<Api>(sessionId, sessionProvider, missingScopeListener)
        val forceUpdateHandler = ProtonForceUpdateHandler<Api>(apiClient)
        val humanVerificationNeededHandler =
            HumanVerificationNeededHandler<Api>(sessionId, clientIdProvider, humanVerificationListener, monoClockMs)
        val humanVerificationInvalidHandler =
            HumanVerificationInvalidHandler<Api>(sessionId, clientIdProvider, humanVerificationListener)
        val deviceVerificationErrorHandler =
            DeviceVerificationNeededHandler<Api>(sessionId, sessionProvider, deviceVerificationListener)
        return listOf(
            dohApiHandler,
            missingScopeHandler,
            tokenErrorHandler,
            forceUpdateHandler,
            humanVerificationInvalidHandler,
            humanVerificationNeededHandler,
            deviceVerificationErrorHandler,
        )
    }

    /**
     * Instantiates ApiManager for given [Api] interface and user.
     *
     * @param Api Retrofit interface defined by the client, must inherit from [BaseRetrofitApi].
     * @param sessionId [SessionId] to be used in the [create].
     * @param interfaceClass Kotlin class for [Api] interface.
     * @param clientErrorHandlers Extra error handlers provided by the client.
     * @param certificatePins Overrides [Constants.DEFAULT_SPKI_PINS]
     * @param alternativeApiPins Overrides [Constants.ALTERNATIVE_API_SPKI_PINS]
     * @return Created instance.
     */
    fun <Api : BaseRetrofitApi> create(
        sessionId: SessionId? = null,
        interfaceClass: KClass<Api>,
        clientErrorHandlers: List<ApiErrorHandler<Api>> = emptyList(),
        certificatePins: Array<String> = this@ApiManagerFactory.certificatePins,
        alternativeApiPins: List<String> = this@ApiManagerFactory.alternativeApiPins
    ): ApiManager<Api> {
        val pinningStrategy = { builder: OkHttpClient.Builder ->
            val mainPinningMethod = if (apiClient.useAltRoutingCertVerificationForMainRoute)
                PinningMethod.LeafSPKI else PinningMethod.Regular
            initPinning(mainPinningMethod, builder, baseUrl.host, certificatePins.toList())
        }
        val primaryBackend = ProtonApiBackend(
            context,
            product,
            baseUrl.toString(),
            apiClient,
            clientIdProvider,
            serverTimeListener,
            sessionId,
            sessionProvider,
            humanVerificationProvider,
            deviceVerificationProvider,
            baseOkHttpClient,
            listOf(jsonConverter),
            interfaceClass,
            networkManager,
            pinningStrategy,
            prefs,
            cookieStore,
            extraHeaderProvider,
        )

        val alternativePinningStrategy = { builder: OkHttpClient.Builder ->
            initSPKIleafPinning(builder, alternativeApiPins)
        }

        val dohProvider = DohProvider(
            baseUrl.toString(),
            apiClient,
            dohServices,
            protonDohService,
            mainScope,
            prefs,
            ::javaMonoClockMs,
            sessionId,
            dohAlternativesListener
        )
        val dohApiHandler = DohApiHandler(
            apiClient = apiClient,
            primaryBackend = primaryBackend,
            dohProvider = dohProvider,
            prefs = prefs,
            wallClockMs = ::javaWallClockMs,
            monoClockMs = ::javaMonoClockMs,
            dohAlternativesListener,
        ) { baseUrl ->
            ProtonApiBackend(
                context,
                product,
                baseUrl,
                apiClient,
                clientIdProvider,
                serverTimeListener,
                sessionId,
                sessionProvider,
                humanVerificationProvider,
                deviceVerificationProvider,
                baseOkHttpClient,
                listOf(jsonConverter),
                interfaceClass,
                networkManager,
                alternativePinningStrategy,
                prefs,
                cookieStore,
                extraHeaderProvider,
            )
        }
        val errorHandlers = createBaseErrorHandlers(sessionId, ::javaMonoClockMs, dohApiHandler) + clientErrorHandlers

        return ApiManagerImpl(
            apiClient, primaryBackend, errorHandlers, ::javaMonoClockMs
        )
    }
}

/**
 * Factory method for [NetworkManager] allowing tracking of connectivity changes.
 */
fun NetworkManager(context: Context): NetworkManager =
    NetworkManagerImpl(context.applicationContext)

/**
 * Factory method to create persistent storage of preferences for network module.
 */
fun NetworkPrefs(context: Context): NetworkPrefs =
    NetworkPrefsImpl(context.applicationContext)
