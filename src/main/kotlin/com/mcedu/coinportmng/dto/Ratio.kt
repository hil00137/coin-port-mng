package com.mcedu.coinportmng.dto

import kotlin.math.roundToLong

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

    fun toPercent(underDigit: Int): Double {
        var result = this.value * 100
        if (underDigit > 0) {
            var multiple = 1
            for (i in 0 until  underDigit) {
                multiple *= 10
            }
            result = (result * multiple).roundToLong().toDouble()
            result /= multiple
        }
        return result
    }

    operator fun div(otherValue: Double): Ratio {
        return Ratio(this.value / otherValue)
    }

    operator fun compareTo(other: Ratio): Int {
        return this.value.compareTo(other.value)
    }
}