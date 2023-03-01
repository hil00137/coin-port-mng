package com.mcedu.coinportmng.extention

import java.time.LocalDateTime
import java.time.LocalTime

fun LocalDateTime.getSecondsOfDay(): Int {
    return this.toLocalTime().getSecondsOfDay()
}

fun LocalTime.getSecondsOfDay(): Int {
    return ((this.hour * 60) + this.minute) * 60 + this.second
}