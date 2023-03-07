package com.mcedu.coinportmng.extention

import kotlin.math.abs

fun Map<String, Double>.logForm(): String {
    val entryList = this.mapValues { "${it.value}%" }.entries.sortedWith(Comparator { o1, o2 ->
        val indexer: (String) -> Int = {
            if (it == "KRW") {
                1
            } else if (it.startsWith("IDX.")) {
                3
            } else {
                2
            }
        }
        val index1 = indexer(o1.key)
        val index2 = indexer(o2.key)
        return@Comparator if (index1 == index2) o1.key.compareTo(o2.key) else index1.compareTo(index2)
    })
    return entryList.toString().replace("[", "").replace("]", "")
}

fun Map<String, Double>.diff(other: Map<String, Double>): Map<String, Double> {
    val resultMap = hashMapOf<String, Double>()

    for ((ticker, percent) in this) {
        resultMap[ticker] = ((percent - other.getOrDefault(ticker, 0.0)) / 100.0).toPercent(2)
    }
    for ((ticker, percent) in other) {
        if (resultMap.contains(ticker)) continue
        resultMap[ticker] = - percent
    }

    return resultMap
}

fun Map<String, Double>.calcGap(other: Map<String, Double>): Map<String, Double> {
    val resultMap = hashMapOf<String, Double>()

    for ((ticker, percent) in this) {
        val gap = other[ticker]
        resultMap[ticker] = if (gap == null) {
             abs(percent)
        } else {
            abs(gap / percent).toPercent(2)
        }
    }

    return resultMap
}