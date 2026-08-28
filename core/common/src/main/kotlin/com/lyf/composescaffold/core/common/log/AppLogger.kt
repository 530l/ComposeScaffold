package com.lyf.composescaffold.core.common.log

import co.touchlab.kermit.Logger

/** 统一日志入口，业务代码不直接依赖具体日志实现。 */
object AppLogger {
    fun debug(tag: String, message: () -> String) = Logger.d(tag = tag, message = message)

    fun info(tag: String, message: () -> String) = Logger.i(tag = tag, message = message)

    fun warning(tag: String, message: () -> String) = Logger.w(tag = tag, message = message)

    fun error(tag: String, throwable: Throwable? = null, message: () -> String) =
        Logger.e(throwable = throwable, tag = tag, message = message)
}
