package com.mcedu.coinportmng.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UpbitCoinPriceDto(
    val market: String,
    @JsonProperty("trade_price")
    val tradePrice: Double
)