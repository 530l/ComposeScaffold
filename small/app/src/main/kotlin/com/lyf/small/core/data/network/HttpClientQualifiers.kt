package com.lyf.small.core.data.network

import javax.inject.Qualifier

/** 只供受信任业务 API 使用的认证客户端。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiHttpClient

/** 公共图片等资源使用的无认证客户端。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicHttpClient

/** 只使用无认证客户端的 Retrofit，供无需会话的公开接口使用。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicRetrofit
