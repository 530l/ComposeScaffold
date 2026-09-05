package com.lyf.composescaffold.core.data.network

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class AuthInterceptorTest {

    @Test
    fun attachesBearerTokenWhenAvailable() {
        var recordedAuthHeader: String? = null
        val tokenProvider = AuthTokenProvider { "test_secret_token_123" }
        val authInterceptor = AuthInterceptor(tokenProvider = tokenProvider)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                recordedAuthHeader = chain.request().header("Authorization")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()

        client.newCall(Request.Builder().url("https://example.com/api").build()).execute()

        assertThat(recordedAuthHeader).isEqualTo("Bearer test_secret_token_123")
    }

    @Test
    fun doesNotAttachHeaderWhenTokenNull() {
        var recordedAuthHeader: String? = null
        val tokenProvider = AuthTokenProvider { null }
        val authInterceptor = AuthInterceptor(tokenProvider = tokenProvider)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                recordedAuthHeader = chain.request().header("Authorization")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()

        client.newCall(Request.Builder().url("https://example.com/api").build()).execute()

        assertThat(recordedAuthHeader).isNull()
    }

    @Test
    fun triggersSessionExpiredOn401() {
        var eventEmitted = false
        val sessionEventManager = SessionEventManager()
        val authInterceptor = AuthInterceptor(sessionEventManager = sessionEventManager)

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()

        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            sessionEventManager.sessionExpiredEvents.collect {
                eventEmitted = true
            }
        }

        client.newCall(Request.Builder().url("https://example.com/api").build()).execute()

        assertThat(eventEmitted).isTrue()
        job.cancel()
    }
}
