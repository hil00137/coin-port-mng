package com.mcedu.coinportmng.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class SnapshotDto(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    val time: LocalDateTime,
    val totalMoney: Long
)
