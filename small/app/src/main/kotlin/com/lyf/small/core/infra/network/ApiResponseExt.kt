package com.lyf.small.core.infra.network

import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.exceptions.isNetworkFailure
import com.skydoves.sandwich.exceptions.isTimeout

/** 断网与超时都按网络不可用反馈；依赖 SmallApplication 注册的 RetrofitExceptionClassifier。 */
internal fun ApiResponse.Failure<*>.isConnectivityFailure(): Boolean =
    this is ApiResponse.Failure.Exception && (isNetworkFailure || isTimeout)
