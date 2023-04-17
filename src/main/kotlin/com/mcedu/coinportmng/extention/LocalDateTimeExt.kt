package com.mcedu.coinportmng.extention

import java.time.LocalDateTime
import java.time.LocalTime

fun LocalDateTime.getSecondsOfDay(): Int {
    return this.toLocalTime().getSecondsOfDay()
}

fun LocalTime.getSecondsOfDay(): Int {
    return ((this.hour * 60) + this.minute) * 60 + this.second
}

fun LocalDateTime.getNextHour(): LocalDateTime {
    return this.withMinute(0)
}

fun LocalDateTime.getNextDay(): LocalDateTime {
    var resultTime = this
    if (this.hour <9) {
        resultTime = resultTime.minusDays(1)
    }
    return resultTime.withHour(9).withMinute(0)
}