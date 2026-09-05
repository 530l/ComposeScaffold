package com.lyf.composescaffold.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PriceUtilsTest {

    @Test
    fun formatMoney_formatsCnyCorrectly() {
        assertThat(formatMoney(Money(0))).isEqualTo("¥0.00")
        assertThat(formatMoney(Money(5))).isEqualTo("¥0.05")
        assertThat(formatMoney(Money(50))).isEqualTo("¥0.50")
        assertThat(formatMoney(Money(1990))).isEqualTo("¥19.90")
        assertThat(formatMoney(Money(10000))).isEqualTo("¥100.00")
    }
}
