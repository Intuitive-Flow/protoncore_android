/*
 * Copyright (c) 2022 Proton Technologies AG
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

package me.proton.core.network.data.cookie

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.network.data.ProtonCookieStore
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

@RunWith(RobolectricTestRunner::class)
class ProtonCookieStoreTest {
    private lateinit var diskStorage: DiskCookieStorage
    private lateinit var memoryStorage: MemoryCookieStorage
    private lateinit var tested: ProtonCookieStore

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        diskStorage = DiskCookieStorage(
            context,
            "test-prefs-cookie-store",
            TestCoroutineScopeProvider(TestDispatcherProvider(UnconfinedTestDispatcher()))
        )
        memoryStorage = MemoryCookieStorage()
        tested = ProtonCookieStore(persistentStorage = diskStorage, sessionStorage = memoryStorage)
    }

    @Test
    fun `empty cookie jar`() = runTest {
        assertContentEquals(emptyList(), tested.all().toList())
        assertContentEquals(emptyList(), tested.loadForRequest(plainUrlA))
    }

    @Test
    fun `set a single cookie`() = runTest {
        val cookies = listOf(makeCookie(url = secureUrlA))
        tested.saveFromResponse(secureUrlA, cookies)

        assertContentEquals(cookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(cookies, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(cookies, diskStorage.all().toList())

        tested.removeCookies(cookies)
    }

    @Test
    fun `set a persistent and non-persistent cookie`() = runTest {
        val persistentCookies = listOf(makeCookie(name = "c1", url = secureUrlA))
        val sessionCookies = listOf(makeCookie(name = "c2", url = secureUrlA, expiresAt = null))
        val cookies = persistentCookies + sessionCookies
        tested.saveFromResponse(secureUrlA, cookies)

        assertContentEquals(cookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(cookies, tested.all().toList())
        assertContentEquals(sessionCookies, memoryStorage.all().toList())
        assertContentEquals(persistentCookies, diskStorage.all().toList())

        tested.removeCookies(sessionCookies)
        tested.removeCookies(persistentCookies)
        tested.removeCookies(cookies)
    }

    @Test
    fun `set multiple cookies`() = runTest {
        val cookies = listOf(makeCookie(url = secureUrlA), makeCookie(url = secureUrlA, name = "test-cookie-2"))
        tested.saveFromResponse(plainUrlA, cookies)

        assertContentEquals(cookies, tested.loadForRequest(secureUrlA).sortedBy { it.name })
        assertContentEquals(cookies, tested.all().toList().sortedBy { it.name })
        assertContentEquals(emptyList(), memoryStorage.all().toList().sortedBy { it.name })
        assertContentEquals(cookies, diskStorage.all().toList().sortedBy { it.name })

        tested.removeCookies(cookies)
    }

    @Test
    fun `update a single cookie`() = runTest {
        tested.saveFromResponse(secureUrlA, listOf(makeCookie(url = secureUrlA)))

        val updatedCookies = listOf(makeCookie(url = secureUrlA, value = "updated-value"))
        tested.saveFromResponse(secureUrlA, updatedCookies)

        assertContentEquals(updatedCookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(updatedCookies, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(updatedCookies, diskStorage.all().toList())

        tested.removeCookies(updatedCookies)
    }

    @Test
    fun `set a cookie with same name, but different domain`() = runTest {
        val cookiesA = listOf(makeCookie(url = secureUrlA))
        val cookiesB = listOf(makeCookie(url = secureUrlB))
        tested.saveFromResponse(secureUrlA, cookiesA)
        tested.saveFromResponse(secureUrlB, cookiesB)

        assertContentEquals(cookiesA, tested.loadForRequest(secureUrlA))
        assertContentEquals(cookiesB, tested.loadForRequest(secureUrlB))
        assertContentEquals(cookiesA + cookiesB, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(cookiesA + cookiesB, diskStorage.all().toList())

        tested.removeCookies(cookiesA)
        tested.removeCookies(cookiesB)
    }

    @Test
    fun `update a cookie with same name, but different url scheme`() = runTest {
        tested.saveFromResponse(plainUrlA, listOf(makeCookie(url = secureUrlA)))
        val updatedCookies = listOf(makeCookie(url = plainUrlA))
        tested.saveFromResponse(secureUrlA, updatedCookies)

        assertContentEquals(updatedCookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(updatedCookies, tested.loadForRequest(plainUrlA))
        assertContentEquals(updatedCookies, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(updatedCookies, diskStorage.all().toList())

        tested.removeCookies(updatedCookies)
    }

    @Test
    fun `update a cookie with same name, but different path`() = runTest {
        val rootCookies = listOf(makeCookie(url = secureUrlA))
        val subPathCookies = listOf(makeCookie(url = secureUrlASubPath))
        tested.saveFromResponse(secureUrlA, rootCookies)
        tested.saveFromResponse(secureUrlASubPath, subPathCookies)

        assertContentEquals(rootCookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(rootCookies + subPathCookies, tested.loadForRequest(secureUrlASubPath).sortedBy { it.path })
        assertContentEquals(rootCookies + subPathCookies, tested.all().toList().sortedBy { it.path })
        assertContentEquals(emptyList(), memoryStorage.all().toList().sortedBy { it.path })
        assertContentEquals(rootCookies + subPathCookies, diskStorage.all().toList().sortedBy { it.path })

        tested.removeCookies(rootCookies)
        tested.removeCookies(subPathCookies)
    }

    @Test
    fun `non-secure cookie can be obtained for http and https urls`() = runTest {
        val cookies = listOf(makeCookie(url = plainUrlA))
        tested.saveFromResponse(plainUrlA, cookies)

        assertContentEquals(cookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(cookies, tested.loadForRequest(plainUrlA))
        assertContentEquals(cookies, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(cookies, diskStorage.all().toList())

        tested.removeCookies(cookies)
    }

    @Test
    fun `secure cookie can be obtained only for https urls`() = runTest {
        val cookies = listOf(makeCookie(url = secureUrlA))
        tested.saveFromResponse(secureUrlA, cookies)

        assertContentEquals(emptyList(), tested.loadForRequest(plainUrlA))
        assertContentEquals(cookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(cookies, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(cookies, diskStorage.all().toList())

        tested.removeCookies(cookies)
    }

    @Test
    fun `expired cookie is deleted`() = runTest {
        val cookies = listOf(makeCookie(url = plainUrlA))
        tested.saveFromResponse(plainUrlA, cookies)
        assertContentEquals(cookies, tested.loadForRequest(plainUrlA))

        val expiredCookies = listOf(makeCookie(url = plainUrlA, expiresAt = 0))
        tested.saveFromResponse(plainUrlA, expiredCookies)
        assertContentEquals(emptyList(), tested.loadForRequest(plainUrlA))
        assertContentEquals(emptyList(), tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(emptyList(), diskStorage.all().toList())
    }

    @Test
    fun `non-persistent cookie is only stored in memory`() = runTest {
        val cookies = listOf(makeCookie(url = plainUrlA, expiresAt = null))
        tested.saveFromResponse(plainUrlA, cookies)

        assertContentEquals(emptyList(), diskStorage.all().toList())
        assertContentEquals(cookies, memoryStorage.all().toList())
        assertContentEquals(cookies, tested.loadForRequest(plainUrlA))
        assertContentEquals(cookies, tested.all().toList())
    }

    @Test
    fun `change from persistent to session cookie`() = runTest {
        val persistentCookies = listOf(makeCookie(url = plainUrlA))
        tested.saveFromResponse(plainUrlA, persistentCookies)

        assertContentEquals(persistentCookies, diskStorage.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(persistentCookies, tested.loadForRequest(plainUrlA))
        assertContentEquals(persistentCookies, tested.all().toList())

        val sessionCookies = listOf(makeCookie(url = plainUrlA, expiresAt = null))
        tested.saveFromResponse(plainUrlA, sessionCookies)

        assertContentEquals(emptyList(), diskStorage.all().toList())
        assertContentEquals(sessionCookies, memoryStorage.all().toList())
        assertContentEquals(sessionCookies, tested.loadForRequest(plainUrlA))
        assertContentEquals(sessionCookies, tested.all().toList())
    }

    @Test
    fun `change from session to persistent cookie`() = runTest {
        val sessionCookies = listOf(makeCookie(url = plainUrlA, expiresAt = null))
        tested.saveFromResponse(plainUrlA, sessionCookies)

        assertContentEquals(emptyList(), diskStorage.all().toList())
        assertContentEquals(sessionCookies, memoryStorage.all().toList())
        assertContentEquals(sessionCookies, tested.loadForRequest(plainUrlA))
        assertContentEquals(sessionCookies, tested.all().toList())

        val persistentCookies = listOf(makeCookie(url = plainUrlA))
        tested.saveFromResponse(plainUrlA, persistentCookies)

        assertContentEquals(persistentCookies, diskStorage.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(persistentCookies, tested.loadForRequest(plainUrlA))
        assertContentEquals(persistentCookies, tested.all().toList())
    }

    @Test
    fun `load cookies from disk store`() = runTest {
        val cookies = listOf(makeCookie(url = secureUrlA))
        diskStorage.set(cookies.first())

        assertContentEquals(cookies, tested.loadForRequest(secureUrlA))
        assertContentEquals(cookies, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(cookies, diskStorage.all().toList())
    }

    @Test
    fun `load expired cookie from disk store`() = runTest {
        val cookies = listOf(makeCookie(url = secureUrlA, expiresAt = System.currentTimeMillis() - 1))
        diskStorage.set(cookies.first())

        assertContentEquals(emptyList(), tested.loadForRequest(secureUrlA))
        assertContentEquals(cookies, tested.all().toList())
        assertContentEquals(emptyList(), memoryStorage.all().toList())
        assertContentEquals(cookies, diskStorage.all().toList())
    }

    @Test
    fun `matches sub-domain host`() {
        val cookies = listOf(makeCookie(url = secureUrlA, hostOnly = false))
        tested.saveFromResponse(secureUrlA, cookies)

        assertContentEquals(cookies, tested.loadForRequest(secureUrlASubdomain))
        assertContentEquals(cookies, tested.loadForRequest(secureUrlA))
    }

    @Test
    fun `host-only domain cookie`() {
        val cookies = listOf(makeCookie(url = secureUrlA, hostOnly = true))
        tested.saveFromResponse(secureUrlA, cookies)

        assertContentEquals(emptyList(), tested.loadForRequest(secureUrlASubdomain))
        assertContentEquals(cookies, tested.loadForRequest(secureUrlA))
    }

    private fun makeCookie(
        name: String = "test-cookie",
        value: String = "test-value",
        url: HttpUrl,
        expiresAt: Long? = System.currentTimeMillis() + 1000,
        hostOnly: Boolean = false
    ): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .path(url.encodedPath)
        .apply {
            if (expiresAt != null) expiresAt(expiresAt)
            if (hostOnly) hostOnlyDomain(url.host) else domain(url.host)
            if (url.isHttps) secure()
        }
        .build()

    companion object {
        private const val domainA = "domain-a.com"
        private const val domainB = "domain-b.com"

        private val plainUrlA = "http://$domainA".toHttpUrl()
        private val secureUrlA = "https://$domainA".toHttpUrl()
        private val secureUrlASubPath = "https://$domainA/sub/path".toHttpUrl()
        private val secureUrlASubdomain = "https://subdomain.$domainA".toHttpUrl()

        private val secureUrlB = "https://$domainB".toHttpUrl()
    }
}
