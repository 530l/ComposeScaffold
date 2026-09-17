package com.lyf.small.data.content.dto

import com.skydoves.sandwich.envelope.ApiEnvelope
import kotlinx.serialization.Serializable

/** 服务端业务错误：业务失败时作为 Failure.Error 的 payload 携带错误码。 */
internal data class AppError(
    val code: Int,
    val message: String?,
) {
    // 脱敏：服务端文案不随 toString 进入日志，只保留错误码。
    override fun toString(): String = "AppError(code=$code)"
}

/** 服务端通用响应包装，errorCode 非 0 表示业务失败；实现 ApiEnvelope 交给 Sandwich 统一判定。 */
@Serializable
internal data class AppEnvelope<T>(
    val data: T? = null,
    val errorCode: Int = 0,
    val errorMsg: String = "",
) : ApiEnvelope<T?, AppError> {

    /**
     * Sandwich 的 ApiEnvelopeMapper 在响应分类阶段统一判定业务成败：
     * errorCode != 0（业务失败，HTTP 仍是 200）会被转成 Failure.Error（payload 为 AppError），
     * 所以接口返回时，业务失败的响应已经不是 Success。
     */
    override val isEnvelopeSuccessful: Boolean get() = errorCode == 0

    // 业务成功但 data 缺失时保持可空，由 Repository 兜底，保证数据层永不抛异常。
    override val envelopeBody: T? get() = data

    override val envelopeError: AppError
        get() = AppError(errorCode, errorMsg.ifBlank { null })
}

@Serializable
internal data class BannerDto(
    val id: Long,
    val title: String = "",
    val imagePath: String = "",
)

@Serializable
internal data class ArticlePageDto(
    val curPage: Int = 0,
    val datas: List<ArticleDto> = emptyList(),
    val over: Boolean = true,
    val pageCount: Int = 0,
)

@Serializable
internal data class ArticleDto(
    val id: Long,
    val title: String = "",
    val author: String = "",
    val shareUser: String = "",
    val chapterName: String = "",
    val superChapterName: String = "",
    val niceDate: String = "",
    val fresh: Boolean = false,
)
