package com.mcedu.coinportmng.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class UpbitInfoSaveRequest(
    val name: String,
    val accessKey: String,
    val secretKey: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val expireDateTime: LocalDateTime
)