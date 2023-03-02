package com.mcedu.coinportmng.common

enum class Market(val fee: Double) {
    KRW(0.0005),
    BTC(0.0025),
    USDT(0.0025);

    fun changeMoney(money: Double): Double {
        return money * (1 - fee)
    }
}