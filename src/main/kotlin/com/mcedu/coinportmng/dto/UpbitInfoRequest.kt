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

data class UpbitInfoUpdateRequest(
    var seq: Long,
    var name: String? = null,
    val accessKey: String? = null,
    val secretKey: String? = null,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val expireDateTime: LocalDateTime? = null
)