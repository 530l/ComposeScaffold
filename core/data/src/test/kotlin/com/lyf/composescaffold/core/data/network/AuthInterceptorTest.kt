package com.lyf.composescaffold.core.data.network

import com.google.common.truth.Truth.assertThat
import com.lyf.composescaffold.core.common.config.AppConfig
import com.lyf.composescaffold.core.data.di.DataModule
import com.lyf.composescaffold.core.data.storage.InMemorySecureCredentialStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class AuthInterceptorTest {
    private val apiOrigin = "https://example.com/".toHttpUrl()

    @Test
    fun attachesBearerTokenWhenAvailable() {
        val interceptor = AuthInterceptor(apiOrigin, tokenProvider = { "test_token" })
        execute(interceptor, "https://example.com/api") { request ->
            assertThat(request.header("Authorization")).isEqualTo("Bearer test_token")
        }
    }

    @Test
    fun doesNotAttachHeaderWhenTokenNull() {
        val interceptor = AuthInterceptor(apiOrigin, tokenProvider = { null })
        execute(interceptor, "https://example.com/api") { request ->
            assertThat(request.header("Authorization")).isNull()
        }
    }

    @Test
    fun triggersSessionExpiredOn401() {
        val store = InMemorySecureCredentialStore().apply { saveAuthToken("token") }
        val manager = SessionEventManager(store)
        val interceptor = AuthInterceptor(apiOrigin, manager::getAuthToken, manager)
        execute(interceptor, "https://example.com/api", code = 401)

        assertThat(manager.state.value).isEqualTo(SessionState.Expired(credentialCleanupFailed = false))
        assertThat(store.getAuthToken()).isNull()
    }

    @Test
    fun foreignOriginsNeverReadTokenOrInvalidateSession() {
        val store = InMemorySecureCredentialStore().apply { saveAuthToken("token") }
        val manager = SessionEventManager(store)
        val interceptor = AuthInterceptor(apiOrigin, tokenProvider = { error("不应读取凭据") }, manager)
        listOf("https://cdn.example.com/image", "https://example.com:8443/image", "http://example.com/image")
            .forEach { url ->
                execute(interceptor, url, code = 401) { request ->
                    assertThat(request.header("Authorization")).isNull()
                }
            }
        assertThat(manager.state.value).isEqualTo(SessionState.Available)
        assertThat(store.getAuthToken()).isEqualTo("token")
    }

    @Test
    fun explicitAuthorizationIsPreservedWithoutInvalidatingAppSession() {
        val store = InMemorySecureCredentialStore().apply { saveAuthToken("token") }
        val manager = SessionEventManager(store)
        val interceptor = AuthInterceptor(apiOrigin, manager::getAuthToken, manager)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor { chain ->
            assertThat(chain.request().header("Authorization")).isEqualTo("Basic explicit")
            response(chain.request(), 401)
        }.build()
        val request = Request.Builder().url(apiOrigin).header("Authorization", "Basic explicit").build()
        client.newCall(request).execute().use { }
        assertThat(manager.state.value).isEqualTo(SessionState.Available)
    }

    @Test
    fun redirectedForeign401DoesNotInvalidateSession() {
        val store = InMemorySecureCredentialStore().apply { saveAuthToken("token") }
        val manager = SessionEventManager(store)
        val interceptor = AuthInterceptor(apiOrigin, manager::getAuthToken, manager)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor { chain ->
            val redirected = chain.request().newBuilder().url("https://cdn.example.com/image").build()
            response(redirected, 401)
        }.build()
        client.newCall(Request.Builder().url(apiOrigin).build()).execute().use { }
        assertThat(manager.state.value).isEqualTo(SessionState.Available)
    }

    @Test
    fun publicClientDoesNotInheritApiAuthentication() {
        val publicClient = DataModule.providePublicOkHttpClient(AppConfig())
        val interceptor = AuthInterceptor(apiOrigin, tokenProvider = { "token" })
        val apiClient = DataModule.provideApiOkHttpClient(publicClient, interceptor)
        listOf(publicClient to null, apiClient to "Bearer token").forEach { (client, expected) ->
            val transport = client.newBuilder().addInterceptor { chain ->
                assertThat(chain.request().header("Authorization")).isEqualTo(expected)
                response(chain.request(), 200)
            }.build()
            transport.newCall(Request.Builder().url(apiOrigin).build()).execute().use { }
        }
    }

    private fun execute(
        interceptor: Interceptor,
        url: String,
        code: Int = 200,
        inspect: (Request) -> Unit = {},
    ) {
        val client = OkHttpClient.Builder().addInterceptor(interceptor).addInterceptor { chain ->
            inspect(chain.request())
            response(chain.request(), code)
        }.build()
        client.newCall(Request.Builder().url(url).build()).execute().use { }
    }

    private fun response(request: Request, code: Int): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("测试响应")
        .body("{}".toResponseBody())
        .build()
}
