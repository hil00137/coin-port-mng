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
    return if (this.minute == 0) {
        this
    } else {
        this.plusHours(1).withMinute(0)
    }
}

fun LocalDateTime.getNextDay(): LocalDateTime {
    return if (this.hour == 9 && this.minute == 0) {
        this
    } else {
        if (this.hour > 9) {
            this.plusDays(1)
        } else {
            this
        }.withHour(9).withMinute(0)
    }
}