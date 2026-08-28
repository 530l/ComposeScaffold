package com.lyf.composescaffold.core.data.util

import com.lyf.composescaffold.core.data.model.Money

/** 不依赖平台 Locale 的稳定金额展示；接入多语言后可在 presentation 层替换 formatter。 */
fun formatMoney(money: Money): String {
    val divisor = powerOfTen(money.currency.fractionDigits)
    val integerPart = money.minorUnits / divisor
    if (money.currency.fractionDigits == 0) return "${money.currency.symbol}$integerPart"

    val fractionPart = (money.minorUnits % divisor)
        .toString()
        .padStart(money.currency.fractionDigits, '0')
    return "${money.currency.symbol}$integerPart.$fractionPart"
}

private fun powerOfTen(exponent: Int): Long {
    require(exponent in 0..9) { "暂不支持超过 9 位的小数精度" }
    var result = 1L
    repeat(exponent) { result *= 10 }
    return result
}
