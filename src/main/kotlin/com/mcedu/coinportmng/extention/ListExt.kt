package com.mcedu.coinportmng.extention

import com.mcedu.coinportmng.dto.IndexMarket
import kotlin.math.abs

fun List<IndexMarket>.checkChange(newList: List<IndexMarket>): List<Pair<String, Double>> {
    val newIndexMarkets = newList.associateBy { it.code }
    val changeMarkets = hashMapOf<String, Double>()
    for (indexMarket in this) {
        val ticker = indexMarket.code
        val newMarket = newIndexMarkets[ticker]
        val newMarketRatio = newMarket?.componentRatio ?: 0.0
        changeMarkets[ticker] = newMarketRatio - indexMarket.componentRatio
    }
    for (newIndexMarket in newIndexMarkets) {
        if (changeMarkets.contains(newIndexMarket.key)) {
            continue
        }
        changeMarkets[newIndexMarket.key] = newIndexMarket.value.componentRatio
    }
    val sortedIndex = newList.sortedByDescending { it.componentRatio }
    return changeMarkets.mapValues { it.value.toPercent(2) }.filter { abs(it.value) > 0 }.toList().sortedBy { pair ->
        sortedIndex.indexOfFirst { it.code == pair.first }
    }
}