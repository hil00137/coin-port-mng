package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.entity.Coin
import com.mcedu.coinportmng.repository.CoinRepository
import com.mcedu.coinportmng.service.UpbitService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpbitScheduler(
    private val upbitService: UpbitService,
    private val coinRepository: CoinRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    @Scheduled(cron = "0 0 */1 * * *")
    @Transactional
    fun updateCoinInfos() {
        log.info("코인 정보 업데이트")
        val coinMap = coinRepository.findAll().associateBy { it.ticker }.toMutableMap()
        val newCoinList = mutableListOf<Coin>()
        upbitService.getMarketAll().forEach {
            val strs = it.market.split("-")
            val market = strs[0]
            val ticker = strs[1]
            var coin = coinMap[ticker]
            if (coin == null) {
                coin = Coin(ticker = ticker, englishName = it.englishName, koreanName = it.koreanName)
                coinMap[ticker] = coin
                newCoinList.add(coin)
            }
            coin.update(market)
        }
        if (newCoinList.isNotEmpty()) {
            log.info("신규 코인 등록 : ${newCoinList.map { it.koreanName }}")
        }
        coinRepository.saveAll(newCoinList)
    }
}