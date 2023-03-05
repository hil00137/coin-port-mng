package com.mcedu.coinportmng.extention

import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToLong

fun Double.toPercent(underDigit : Int = 0): Double {
    var result = this * 100
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

fun Double.addSign(): String {
    return if (this > 0) {
        "+$this"
    } else {
        "$this"
    }
}

fun Double.toCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.KOREA).format(this.roundToLong())
}