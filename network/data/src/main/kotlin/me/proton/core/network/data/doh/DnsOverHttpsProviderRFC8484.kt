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
package me.proton.core.network.data.doh

import android.util.Base64
import io.matthewnelson.encoding.base32.Base32Default
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import me.proton.core.network.data.initLogging
import me.proton.core.network.data.safeCall
import me.proton.core.network.domain.ApiClient
import me.proton.core.network.domain.ApiResult
import me.proton.core.network.domain.DohService
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.session.SessionId
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.minidns.dnsmessage.DnsMessage
import org.minidns.dnsmessage.Question
import org.minidns.record.A
import org.minidns.record.Record
import org.minidns.record.TXT
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit

class DnsOverHttpsProviderRFC8484(
    baseOkHttpClient: OkHttpClient,
    private val baseUrl: String,
    private val client: ApiClient,
    private val networkManager: NetworkManager
) : DohService {

    private val api: DnsOverHttpsRetrofitApi

    private val okClient by lazy {
        baseOkHttpClient.newBuilder()
            .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .initLogging(client)
            .build()
    }
    private val hostnameBase32 = Base32Default {
        isLenient = false
        encodeToLowercase = false
        padEncoded = false
    }

    init {
        require(baseUrl.endsWith('/'))

        val converterFactory = object : Converter.Factory() {
            override fun responseBodyConverter(
                type: Type,
                annotations: Array<Annotation>,
                retrofit: Retrofit
            ): Converter<ResponseBody, *> = Converter<ResponseBody, DnsMessage> { body ->
                body.use {
                    DnsMessage(it.bytes())
                }
            }
        }

        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .callFactory { okClient.newCall(it) }
            .addConverterFactory(converterFactory)
            .build()
            .create(DnsOverHttpsRetrofitApi::class.java)
    }

    override suspend fun getAlternativeBaseUrls(sessionId: SessionId?, primaryBaseUrl: String): List<String>? {
        val primaryURI = URI(primaryBaseUrl)
        val base32domain = primaryURI.host.toByteArray().encodeToString(hostnameBase32)
        val sessionPrefix = sessionId?.let { "${sessionId.id}." } ?: ""
        val recordType = when (client.dohRecordType) {
            ApiClient.DohRecordType.TXT -> Record.TYPE.TXT
            ApiClient.DohRecordType.A -> Record.TYPE.A
        }
        val question = Question("${sessionPrefix}d$base32domain.protonpro.xyz", recordType)
        val queryMessage = DnsMessage.builder()
            .setRecursionDesired(true)
            .setQuestion(question)
            .build()
        val queryMessageBase64 = Base64.encodeToString(
            queryMessage.toArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        val response = api.safeCall(networkManager) {
            getServers(baseUrl.removeSuffix("/"), queryMessageBase64)
        }
        if (response is ApiResult.Success) {
            val answers = response.value.answerSection
            return try {
                answers
                    .mapNotNull {
                        when (client.dohRecordType) {
                            ApiClient.DohRecordType.TXT -> (it.payload as? TXT)?.text
                            ApiClient.DohRecordType.A -> (it.payload as? A)?.toString()
                        }
                    }
                    .map { URI("https", it, primaryURI.path, null).toString() }
                    .takeIf { it.isNotEmpty() }
            } catch (e: URISyntaxException) {
                null
            }
        }
        return null
    }

    companion object {
        private const val TIMEOUT_S = 10L
    }
}
