package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.extention.toPercent

@JvmInline
value class Ratio(val value: Double = 0.0) {

    fun add(ratio: Double): Ratio {
        return Ratio(this.value + ratio)
    }

    operator fun minus(other: Ratio): Ratio {
        return Ratio(this.value - other.value)
    }

    operator fun div(other: Ratio): Ratio {
        return Ratio(this.value / other.value)
    }

    fun abs(): Double {
        return kotlin.math.abs(this.value)
    }

    fun toPercent(underDigit: Int): Double = this.value.toPercent(underDigit)

    operator fun div(otherValue: Double): Ratio {
        return Ratio(this.value / otherValue)
    }

    operator fun compareTo(other: Ratio): Int {
        return this.value.compareTo(other.value)
    }
}