package com.lyf.small.data.content.dto

import com.skydoves.sandwich.envelope.ApiEnvelope
import kotlinx.serialization.Serializable

/** WanAndroid 业务错误：信封失败时作为 Failure.Error 的 payload 携带错误码。 */
internal data class WanAndroidError(
    val code: Int,
    val message: String?,
) {
    // 脱敏：服务端文案不随 toString 进入日志，只保留错误码。
    override fun toString(): String = "WanAndroidError(code=$code)"
}

/** WanAndroid 通用响应包装，errorCode 非 0 表示业务失败；实现 ApiEnvelope 交给 Sandwich 统一判定。 */
@Serializable
internal data class WanAndroidEnvelope<T>(
    val data: T? = null,
    val errorCode: Int = 0,
    val errorMsg: String = "",
) : ApiEnvelope<T?, WanAndroidError> {
    override val isEnvelopeSuccessful: Boolean
        get() = errorCode == 0

    // 信封成功但 data 缺失时保持可空，由 Repository 兜底，保证数据层永不抛异常。
    override val envelopeBody: T?
        get() = data

    override val envelopeError: WanAndroidError
        get() = WanAndroidError(errorCode, errorMsg.ifBlank { null })
}

@Serializable
internal data class WanBannerDto(
    val id: Long,
    val title: String = "",
    val imagePath: String = "",
)

@Serializable
internal data class WanArticlePageDto(
    val curPage: Int = 0,
    val datas: List<WanArticleDto> = emptyList(),
    val over: Boolean = true,
    val pageCount: Int = 0,
)

@Serializable
internal data class WanArticleDto(
    val id: Long,
    val title: String = "",
    val author: String = "",
    val shareUser: String = "",
    val chapterName: String = "",
    val superChapterName: String = "",
    val niceDate: String = "",
    val fresh: Boolean = false,
)
