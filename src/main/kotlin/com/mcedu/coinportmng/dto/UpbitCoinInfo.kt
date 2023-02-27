package com.mcedu.coinportmng.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UpbitCoinInfo(val market: String, val koreanName: String, val englishName: String, val marketWarning: String)