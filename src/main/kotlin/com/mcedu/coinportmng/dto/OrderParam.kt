package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.type.CommandType

@JvmInline
value class OrderParam(val params: MutableMap<String, String> = hashMapOf()) {
    fun setMarket(market: String) {
        this.params["market"] = market
    }

    fun setCommand(command: Command) {
        if (command.commandType == CommandType.SELL) {
            params["side"] = "ask"
            params["volume"] = "${command.volume}"
            params["ord_type"] = "market"
        } else if (command.commandType == CommandType.BUY) {
            params["side"] = "bid"
            params["price"] = "${command.price}"
            params["ord_type"] = "price"
        }
    }

    fun getQueryElements(): String {
        val queryElements = ArrayList<String>()
        for ((key, value) in params) {
            queryElements.add("$key=$value")
        }
        return queryElements.joinToString("&")
    }
}