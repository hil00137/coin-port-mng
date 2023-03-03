package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.common.IntervalConstant.DAILY
import com.mcedu.coinportmng.common.IntervalConstant.FIVE_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.HALF_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.MONTHLY
import com.mcedu.coinportmng.common.IntervalConstant.QUARTER_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.TEN_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.YEARLY
import com.mcedu.coinportmng.common.ReblanceJobStatus
import com.mcedu.coinportmng.entity.Coin
import com.mcedu.coinportmng.entity.PortfolioRebalanceJob
import com.mcedu.coinportmng.entity.RebalanceMng
import com.mcedu.coinportmng.extention.getSecondsOfDay
import com.mcedu.coinportmng.repository.CoinRepository
import com.mcedu.coinportmng.repository.PortfolioRebalanceJobRepository
import com.mcedu.coinportmng.repository.PortfolioRepository
import com.mcedu.coinportmng.repository.RebalanceMngRepository
import com.mcedu.coinportmng.service.PortfolioService
import com.mcedu.coinportmng.service.UpbitService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.abs

@Component
class UpbitScheduler(
    private val upbitService: UpbitService,
    private val coinRepository: CoinRepository,
    private val portfolioRepository: PortfolioRepository,
    private val portfolioService: PortfolioService,
    private val rebalanceMngRepository: RebalanceMngRepository,
    private val portfolioRebalanceJobRepository: PortfolioRebalanceJobRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    fun updateCoinInfos() {
        log.info("코인 정보 업데이트")
        val coinMap = coinRepository.findAll().map {
            it.resetMarket()
            it
        }.associateBy { it.ticker }.toMutableMap()
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
        val rebalanceMngs = rebalanceMngRepository.findAllByActive()
        val executeSet = mutableSetOf<RebalanceMng>()
        rebalanceMngs.forEach {
            if (intervalCheck(it, now) || (it.bandRebalance && bandCheck(it))) {
                executeSet.add(it)
            }
        }
        executeSet.forEach {
            val rebalanceJob = portfolioRebalanceJobRepository.findByAccessInfo(it.accessInfo)
            if (rebalanceJob == null) {
                log.info("insert job")
                portfolioRebalanceJobRepository.save(PortfolioRebalanceJob(accessInfo = it.accessInfo, status = ReblanceJobStatus.READY))
            }
        }
    }

    fun intervalCheck(rebalanceMng: RebalanceMng, now: LocalDateTime): Boolean {
        return when (rebalanceMng.interval) {
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
    }

    private fun bandCheck(rebalanceMng: RebalanceMng): Boolean {
        val accounts = upbitService.getMyAccounts(rebalanceMng.accessInfo.seq ?: 0)
        val currencyStrs = accounts.map { it.currency }
        val coinMap = coinRepository.findAllById(currencyStrs).associateBy { it.ticker }
        val hasMarketWallet = accounts.filter { coinMap.containsKey(it.currency) || it.currency == "KRW" }
        var portfolios = portfolioRepository.findAllByAccessInfo(rebalanceMng.accessInfo).associateBy { it.ticker }
            .mapValues { it.value.ratio }
        val currentPortfolio = portfolioService.getCurrentPortfolio(hasMarketWallet, coinMap)

        for (planKey in portfolios.keys) {
            if (!currentPortfolio.containsKey(planKey)) {
                log.info("$planKey > add target")
                return true
            }
        }

        val planSum = portfolios.values.sum()
        portfolios = portfolios.mapValues { it.value / planSum }

        val currentSum = currentPortfolio.values.sum()
        val pairMap = currentPortfolio.mapValues { Pair(it.value, it.value / currentSum) }

        for ((key, pair) in pairMap) {
            if (pair.first < 5000) {
                log.info("$key > not enough money skip")
                continue
            }
            val plan = portfolios[key]
            if (plan == null) {
                log.info("$key > remove target")
                return true
            }

            val overPercent = (abs(pair.second - plan) / plan) * 100.0
            if(overPercent > rebalanceMng.bandCheck) {
                log.info("$key > plan : ${plan * 100} , now : ${pair.second * 100} , over : $overPercent")
                return true
            }
        }

        return false
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