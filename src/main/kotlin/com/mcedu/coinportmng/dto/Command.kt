package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.type.CommandType

data class Command(val commandType: CommandType = CommandType.NONE, val ticker: String = "", val volume: Double = 0.0, val price: Double = 0.0)