package com.mcedu.coinportmng.type

enum class UpbitIndex(val desc: String, val indexCode : String) {
    UTTI("Upbit Top 10 - 시가총액가중방식", "IDX.UPBIT.UTTI"),
    UTTI_EW("Upbit Top 10 - 동일가중방식", "IDX.UPBIT.UTTI_EW"),
    UBSI002("로우볼 Top 5", "IDX.UPBIT.UBSI002");

    fun getTicker(): String {
        return "IDX.$name"
    }
}