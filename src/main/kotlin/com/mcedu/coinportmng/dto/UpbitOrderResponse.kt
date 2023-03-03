package com.mcedu.coinportmng.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UpbitOrderResponse(
    val uuid: String,
    val side: String,
    @JsonProperty("ord_type")
    val ordType: String,
    val price: Double,
    val state: String,
    @JsonProperty("created_at")
    val createdAt: String,
    val volume: Double
)