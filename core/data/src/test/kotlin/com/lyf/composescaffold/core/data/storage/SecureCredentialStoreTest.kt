package com.lyf.composescaffold.core.data.storage

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SecureCredentialStoreTest {

    @Test
    fun simpleBase64EncodesAndDecodesCorrectly() {
        val testStrings = listOf(
            "",
            "f",
            "fo",
            "foo",
            "foob",
            "fooba",
            "foobar",
            "Hello, Commercial Compose Scaffold! 🔐 1234567890",
        )
        for (str in testStrings) {
            val bytes = str.toByteArray(Charsets.UTF_8)
            val encoded = SimpleBase64.encode(bytes)
            val decoded = SimpleBase64.decode(encoded)
            assertThat(String(decoded, Charsets.UTF_8)).isEqualTo(str)
        }
    }

    @Test
    fun inMemoryTestDoubleSavesAndRetrievesTokens() {
        val store = InMemorySecureCredentialStore()
        assertThat(store.getAuthToken()).isNull()

        store.saveAuthToken("jwt_token_sample_123")
        assertThat(store.getAuthToken()).isEqualTo("jwt_token_sample_123")

        store.clearAuthToken()
        assertThat(store.getAuthToken()).isNull()

        store.saveCredential("refresh_token", "ref_999")
        assertThat(store.getCredential("refresh_token")).isEqualTo("ref_999")

        store.clearAll()
        assertThat(store.getCredential("refresh_token")).isNull()
    }
}
