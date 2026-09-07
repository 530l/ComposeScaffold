package com.lyf.composescaffold.core.data.network

import javax.inject.Qualifier

/** 只供受信任业务 API 使用的认证客户端。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiHttpClient

/** 公共图片等资源使用的无认证客户端。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicHttpClient
