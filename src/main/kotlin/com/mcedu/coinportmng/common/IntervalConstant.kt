package com.mcedu.coinportmng.common

object IntervalConstant {
    const val YEARLY = "y"
    const val MONTHLY = "M"
    const val DAILY = "d"
    const val HOURLY = "h"
    const val HALF_HOURLY = "30m"
    const val QUARTER_HOURLY = "15m"
    const val TEN_MINUTELY = "10m"
    const val FIVE_MINUTELY = "5m"

    val all = listOf(YEARLY, MONTHLY, DAILY, HOURLY, HALF_HOURLY, QUARTER_HOURLY, TEN_MINUTELY, FIVE_MINUTELY)
}