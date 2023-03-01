package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.common.IntervalConstant.DAILY
import com.mcedu.coinportmng.common.IntervalConstant.FIVE_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.HALF_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.MONTHLY
import com.mcedu.coinportmng.common.IntervalConstant.QUARTER_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.TEN_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.YEARLY
import com.mcedu.coinportmng.entity.Coin
import com.mcedu.coinportmng.entity.RebalanceMng
import com.mcedu.coinportmng.repository.CoinRepository
import com.mcedu.coinportmng.service.UpbitService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class UpbitScheduler(
    private val upbitService: UpbitService,
    private val coinRepository: CoinRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    @Scheduled(cron = "0 0 * * * *")
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


    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun rebalnceTargetCheck() {
        val now = LocalDateTime.now().withSecond(0)
        // TODO: getRebalanceMng
        // TODO: for innerCheck
    }

    fun innerCheck(rebalanceMng: RebalanceMng, now: LocalDateTime) {
        val isExecute = when (rebalanceMng.interval) {
            YEARLY -> checkIntervalYear(rebalanceMng, now)
            MONTHLY -> checkIntervalMonth(rebalanceMng, now)
            DAILY -> checkIntervalDay(rebalanceMng, now)
            HOURLY -> checkIntervalSeconds(rebalanceMng, now, 3600)
            HALF_HOURLY -> checkIntervalSeconds(rebalanceMng, now, 1800)
            QUARTER_HOURLY -> checkIntervalSeconds(rebalanceMng, now, 900)
            TEN_MINUTELY -> checkIntervalSeconds(rebalanceMng, now, 600)
            FIVE_MINUTELY -> checkIntervalSeconds(rebalanceMng, now, 300)
            else -> false
        }
        if (isExecute) {
            //TODO: execute
        }
    }

    private fun checkIntervalSeconds(rebalanceMng: RebalanceMng, now: LocalDateTime, modulation: Int): Boolean =
        (rebalanceMng.baseTime % modulation) == (now.getSecondsOfDay() % modulation)

    private fun checkIntervalDay(rebalanceMng: RebalanceMng, now: LocalDateTime): Boolean =
        rebalanceMng.baseTime == now.getSecondsOfDay()

    private fun checkIntervalMonth(rebalanceMng: RebalanceMng, now: LocalDateTime): Boolean =
        rebalanceMng.baseDay == now.dayOfMonth && rebalanceMng.baseTime == now.getSecondsOfDay()

    private fun checkIntervalYear(rebalanceMng: RebalanceMng, now: LocalDateTime): Boolean =
        rebalanceMng.baseMonth == now.monthValue && rebalanceMng.baseDay == now.dayOfMonth && rebalanceMng.baseTime == now.getSecondsOfDay()
}

fun LocalDateTime.getSecondsOfDay(): Int {
    return ((this.hour * 60) + this.minute) * 60 + this.second
}