package com.mcedu.coinportmng.scheduler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mcedu.coinportmng.common.IntervalConstant.DAILY
import com.mcedu.coinportmng.common.IntervalConstant.FIVE_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.HALF_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.MONTHLY
import com.mcedu.coinportmng.common.IntervalConstant.QUARTER_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.TEN_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.YEARLY
import com.mcedu.coinportmng.dto.IndexMarket
import com.mcedu.coinportmng.entity.Coin
import com.mcedu.coinportmng.entity.PortfolioRebalanceJob
import com.mcedu.coinportmng.entity.RebalanceMng
import com.mcedu.coinportmng.entity.UpbitIndexInfo
import com.mcedu.coinportmng.extention.*
import com.mcedu.coinportmng.repository.*
import com.mcedu.coinportmng.service.PortfolioService
import com.mcedu.coinportmng.service.UpbitIndexService
import com.mcedu.coinportmng.service.UpbitService
import com.mcedu.coinportmng.type.ReblanceJobStatus
import com.mcedu.coinportmng.type.UpbitIndex
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
    private val upbitIndexInfoRepository: UpbitIndexInfoRepository,
    private val portfolioRebalanceJobRepository: PortfolioRebalanceJobRepository,
    private val upbitIndexService: UpbitIndexService
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()

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
    fun updateIndexInfo() {
        for (upbitIndex in UpbitIndex.values()) {
            val indexMarkets = upbitService.getIndexInfo(upbitIndex).map { it.toSaveForm() }
            val detail = objectMapper.writeValueAsString(indexMarkets)
            var upbitIndexInfo = upbitIndexInfoRepository.findByName(upbitIndex)
            if (upbitIndexInfo == null) {
                upbitIndexInfo = UpbitIndexInfo(name = upbitIndex, detailJson = "")
            } else {
                val orgIndexMarkets = objectMapper.readValue(upbitIndexInfo.detailJson, object : TypeReference<List<IndexMarket>>() {})
                val checkChange = orgIndexMarkets.checkChange(indexMarkets).map {
                    "${it.first}: ${it.second.addSign()}%"
                }
                if (checkChange.isNotEmpty()) {
                    val hyphen = "---------------" * checkChange.size
                    log.info("\n" +
                            "$hyphen\n" +
                            "| ${upbitIndex.desc} 변경\n" +
                            "| ${checkChange.joinToString(" | ")}\n" +
                            hyphen
                    )
                }
            }
            upbitIndexInfo.detailJson = detail
            upbitIndexInfoRepository.save(upbitIndexInfo)
        }
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
        val orgPortfolios = portfolioRepository.findAllByAccessInfo(rebalanceMng.accessInfo).associateBy { it.ticker }
            .mapValues { it.value.ratio }
        val currentPortfolio = portfolioService.getCurrentPortfolio(rebalanceMng.accessInfo.seq ?: 0)
        val totalMoney = currentPortfolio.values.sumOf { it.price }
        val planSum = orgPortfolios.values.sum()
        var portfolios= orgPortfolios.mapValues { it.value / planSum }
        portfolios = upbitIndexService.changeIndexRatio(currentPortfolio, portfolios, totalMoney)

        val orgPortPercent = orgPortfolios.mapValues { it.value }
        val currentPortPercentage = currentPortfolio.mapValues { (it.value.price / totalMoney).toPercent(2) }
        val planPortPercentage = portfolios.mapValues { it.value.toPercent(2) }
        val hyphenSize = planPortPercentage.size
        val hyphen = "--------------"
        val diff = currentPortPercentage.diff(planPortPercentage)
        log.info("\n" +
                "${hyphen * hyphenSize}\n" +
                "| ${rebalanceMng.accessInfo.name} 포트폴리오 체크\n" +
                "| 금      액 : ${totalMoney.toCurrency()}\n"+
                "| 목표상대밴드 : ${rebalanceMng.bandCheck}%\n" +
                "| 목 표 계 획 : ${orgPortPercent.logForm()}\n" +
                "| 수정목표계획 : ${planPortPercentage.logForm()}\n" +
                "| 현      재 : ${currentPortPercentage.logForm()}\n" +
                "| 차      이 : ${diff.logForm()}\n" +
                "| 밴      드 : ${planPortPercentage.calcGap(diff).logForm()}\n" +
                (hyphen * hyphenSize)
        )

        for (planKey in portfolios.keys) {
            if (!currentPortfolio.containsKey(planKey)) {
                log.info("$planKey > add target")
                return true
            }
        }
        val pairMap = currentPortfolio.mapValues { Pair(it.value.price, it.value.price / totalMoney) }

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

            val overPercent = (abs(pair.second - plan) / plan).toPercent(2)
            if(overPercent > rebalanceMng.bandCheck) {
                log.info("$key > plan : ${plan.toPercent(2)}% , now : ${pair.second.toPercent(2)}% , diff : $overPercent%")
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