package com.lyf.composescaffold.core.model

enum class CurrencyCode(
    val symbol: String,
    val fractionDigits: Int,
) {
    CNY(symbol = "¥", fractionDigits = 2),
}

/** 金额统一用最小货币单位存储，禁止浮点误差和负价格。 */
data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode = CurrencyCode.CNY,
) {
    init {
        require(minorUnits >= 0) { "金额不能为负数" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "不同币种不能直接相加" }
        require(other.minorUnits <= Long.MAX_VALUE - minorUnits) { "金额相加溢出" }
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun times(quantity: Int): Money {
        require(quantity >= 0) { "数量不能为负数" }
        require(quantity == 0 || minorUnits <= Long.MAX_VALUE / quantity) { "金额相乘溢出" }
        return copy(minorUnits = minorUnits * quantity)
    }

    companion object {
        fun zero(currency: CurrencyCode = CurrencyCode.CNY) = Money(0, currency)
    }
}
