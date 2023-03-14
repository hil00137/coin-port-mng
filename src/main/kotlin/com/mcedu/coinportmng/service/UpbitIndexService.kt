package com.mcedu.coinportmng.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mcedu.coinportmng.dto.CoinPrice
import com.mcedu.coinportmng.dto.IndexMarket
import com.mcedu.coinportmng.dto.Ratio
import com.mcedu.coinportmng.repository.UpbitIndexInfoRepository
import com.mcedu.coinportmng.type.UpbitIndex
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpbitIndexService(
    private val upbitIndexInfoRepository: UpbitIndexInfoRepository
) {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Transactional(readOnly = true)
    fun changeIndexRatio(
        currentPortfolio: MutableMap<String, CoinPrice>,
        portfolios: Map<String, Ratio>
    ): Map<String, Ratio> {
        val totalMoney = currentPortfolio.values.sumOf { it.price }
        val resultPortfolios = portfolios.toMutableMap()
        val removeKey= hashMapOf<String, Map<String, Double>>()
        for ((key, ratio) in portfolios) {
            if (key.startsWith("IDX.")) {
                val idxMoney = totalMoney * ratio.value
                val upbitIndex = UpbitIndex.valueOf(key.removePrefix("IDX."))
                var upbitIndexInfo = objectMapper.readValue(
                    upbitIndexInfoRepository.findByName(upbitIndex)?.detailJson,
                    object : TypeReference<List<IndexMarket>>() {})
                    .filter {
                        if (currentPortfolio.containsKey(it.code)) {
                            it.componentRatio * idxMoney > 7000
                        } else {
                            it.componentRatio * idxMoney > 10000
                        }
                    }.associate {
                        Pair(it.code, it.componentRatio)
                    }

                val indexSum = upbitIndexInfo.values.sum()

                upbitIndexInfo = upbitIndexInfo.mapValues { it.value / indexSum }.mapValues { it.value * ratio.value }
                removeKey[key] = upbitIndexInfo
            }
        }

        for ((key, upbitIndexInfo) in removeKey) {
            resultPortfolios.remove(key)
            for ((ticker, ratio) in upbitIndexInfo) {
                val orgRatio = portfolios[ticker] ?: Ratio()
                resultPortfolios[ticker] = orgRatio.add(ratio)
            }
        }
        return resultPortfolios
    }
}