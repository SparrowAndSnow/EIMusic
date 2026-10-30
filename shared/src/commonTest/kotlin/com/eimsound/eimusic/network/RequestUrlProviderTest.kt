package com.eimsound.eimusic.network

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestUrlProviderTest {
    @Test
    fun updateUrlsReplacesActiveUrl() {
        val provider = RequestUrlProvider(listOf("https://a.example"))
        assertEquals("a.example", provider.url.value.host)

        provider.updateUrls(listOf("https://b.example", "https://c.example"))
        assertEquals("b.example", provider.url.value.host)

        val next = provider.next(Url("https://b.example"))
        assertEquals("c.example", next.host)
        assertEquals("c.example", provider.url.value.host)
    }

    @Test
    fun rejectsEmptyUrls() {
        assertFailsWith<IllegalArgumentException> {
            RequestUrlProvider(emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            RequestUrlProvider(listOf("https://a.example")).updateUrls(emptyList())
        }
    }
}
