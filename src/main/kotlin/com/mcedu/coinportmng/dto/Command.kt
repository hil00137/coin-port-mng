package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.type.CommandType
import com.mcedu.coinportmng.type.Market

data class Command(val commandType: CommandType = CommandType.NONE, val ticker: String = "", val volume: Double = 0.0, val price: Double = 0.0) {
    companion object {
        fun forRebalance(key: String): Command {
            return Command(CommandType.BUY, key, price = Market.KRW.getExtraBalance())
        }

        fun sell(key: String, volume: Double): Command {
            return Command(CommandType.SELL, key, volume = volume)
        }

        fun buy(key: String, price: Double): Command {
            return Command(CommandType.BUY, key, price = price)
        }
    }
}