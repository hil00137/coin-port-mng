package com.mcedu.coinportmng.type

enum class Market(val fee: Double, val minBalance: Double) {
    KRW(0.0005, 5000.0),
    BTC(0.0025, 0.00005),
    USDT(0.0025, 0.00005);

    fun changeMoney(money: Double): Double {
        return money * (1 - fee)
    }

    fun getExtraBalance(): Double {
        return this.minBalance * 1.05
    }
}