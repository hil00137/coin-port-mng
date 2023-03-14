package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.entity.RebalanceMng

data class RebalancePlanDto(
    val baseMonth: Int,
    val baseDay: Int,
    val baseHour: Int,
    val baseMinute: Int,
    val interval: String,
    val active: Boolean,
    val bandRebalance: Boolean,
    val bandCheck: Double,
    val absBandCheck: Double
) {
    constructor(rebalanceMng: RebalanceMng) : this(
        baseMonth = rebalanceMng.baseMonth,
        baseDay = rebalanceMng.baseDay,
        baseHour = rebalanceMng.baseTime / 3600,
        baseMinute = (rebalanceMng.baseTime % 3600) / 60,
        interval = rebalanceMng.interval,
        active = rebalanceMng.active,
        bandRebalance = rebalanceMng.bandRebalance,
        bandCheck = rebalanceMng.bandCheck,
        absBandCheck = rebalanceMng.absBandCheck
    )
}