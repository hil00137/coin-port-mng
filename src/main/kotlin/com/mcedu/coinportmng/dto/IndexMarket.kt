package com.mcedu.coinportmng.dto

data class IndexMarket(val code: String, val koreanName: String, val englishName: String, val componentRatio: Double) {
    fun toSaveForm(): IndexMarket {
        return this.copy(code = this.code.replace("CRIX.UPBIT.KRW-", ""))
    }
}