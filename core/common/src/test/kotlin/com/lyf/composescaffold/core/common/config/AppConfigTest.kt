package com.lyf.composescaffold.core.common.config

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppConfigTest {

    @Test
    fun validHttpsBaseUrlIsAccepted() {
        val config = AppConfig(apiBaseUrl = "https://api.example.com/v1/")

        assertThat(config.apiBaseUrl).isEqualTo("https://api.example.com/v1/")
    }

    @Test
    fun nonHttpsBaseUrlIsRejected() {
        assertInvalid("http://api.example.com/")
    }

    @Test
    fun baseUrlWithCredentialsIsRejected() {
        assertInvalid("https://user:password@api.example.com/")
    }

    @Test
    fun baseUrlWithQueryIsRejected() {
        assertInvalid("https://api.example.com/?tenant=demo")
    }

    @Test
    fun baseUrlWithoutTrailingSlashIsRejected() {
        assertInvalid("https://api.example.com/v1")
    }

    private fun assertInvalid(url: String) {
        val error = runCatching { AppConfig(apiBaseUrl = url) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }
}
