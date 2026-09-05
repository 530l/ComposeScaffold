package com.lyf.composescaffold.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun constructor_rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(-1)
        }
    }

    @Test
    fun plus_addsMinorUnits_forSameCurrency() {
        val a = Money(100)
        val b = Money(250)
        val sum = a + b
        assertThat(sum.minorUnits).isEqualTo(350)
        assertThat(sum.currency).isEqualTo(CurrencyCode.CNY)
    }

    @Test
    fun times_multipliesAmount_byNonNegativeInt() {
        val m = Money(120)
        val product = m * 3
        assertThat(product.minorUnits).isEqualTo(360)

        val zeroProduct = m * 0
        assertThat(zeroProduct.minorUnits).isEqualTo(0)
    }

    @Test
    fun times_rejectsNegativeQuantity() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(100) * -1
        }
    }

    @Test
    fun zero_returnsZeroMinorUnits() {
        val zero = Money.zero()
        assertThat(zero.minorUnits).isEqualTo(0)
    }
}
