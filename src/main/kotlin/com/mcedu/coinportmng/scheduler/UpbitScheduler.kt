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
import com.mcedu.coinportmng.dto.CoinPrice
import com.mcedu.coinportmng.dto.IndexMarket
import com.mcedu.coinportmng.dto.PortfolioRatio
import com.mcedu.coinportmng.dto.Ratio
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
import java.util.LinkedList

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
                    val textList : MutableList<String> = mutableListOf("| ${upbitIndex.desc} 변경", "| ${checkChange.joinToString(" | ")}")
                    val hyphen = "-" * (textList.maxOfOrNull { it.length } ?: 0)
                    textList.add(0, hyphen)
                    textList.add(hyphen)
                    log.info("\n" + textList.joinToString("\n"))
                }
            }
            upbitIndexInfo.detailJson = detail
            upbitIndexInfoRepository.save(upbitIndexInfo)
        }
    }


    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun rebalanceTargetCheck() {
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
            .mapValues { Ratio(it.value.ratio) }
        val currentPortfolio = portfolioService.getCurrentPortfolio(rebalanceMng.accessInfo.seq ?: 0)
        val planSum = orgPortfolios.values.sumOf { it.value }
        var portfolios= orgPortfolios.mapValues { Ratio(it.value.value / planSum) }
        portfolios = upbitIndexService.changeIndexRatio(currentPortfolio, portfolios)
        bandCheckLog(rebalanceMng, orgPortfolios, portfolios, currentPortfolio)

        for (planKey in portfolios.keys) {
            if (!currentPortfolio.containsKey(planKey)) {
                log.info("$planKey > add target")
                return true
            }
        }

        val totalMoney = currentPortfolio.values.sumOf { it.price }
        val currentPortfolioRatios = currentPortfolio.mapValues { PortfolioRatio(it.value.price, Ratio(it.value.price / totalMoney)) }

        for ((key, portfolioRatio) in currentPortfolioRatios) {
            if (portfolioRatio.price < 5000) {
                log.info("$key > not enough money skip")
                continue
            }
            val plan = portfolios[key]
            if (plan == null) {
                log.info("$key > remove target")
                return true
            }

            val diff = portfolioRatio.ratio - plan
            val diffPercent = diff.abs().toPercent(2)
            if (diffPercent > rebalanceMng.absBandCheck) {
                log.info("$key > plan : ${plan.toPercent(2)}% , now : ${portfolioRatio.ratio.toPercent(2)}% , diff : $diffPercent%")
                return true
            }

            val overPercent = (diff / portfolioRatio.ratio).abs().toPercent(2)

            if(overPercent > rebalanceMng.bandCheck) {
                log.info("$key > plan : ${plan.toPercent(2)}% , now : ${portfolioRatio.ratio.toPercent(2)}% , diff : $overPercent%")
                return true
            }
        }

        return false
    }

    private fun bandCheckLog(
        rebalanceMng: RebalanceMng,
        orgPortfolios: Map<String, Ratio>,
        portfolios: Map<String, Ratio>,
        currentPortfolio: MutableMap<String, CoinPrice>
    ) {
        val planPortPercentage = portfolios.mapValues { it.value.value }
        val totalMoney = currentPortfolio.values.sumOf { it.price }
        val orgPortPercent = orgPortfolios.mapValues { it.value.value / 100.0 }
        val currentPortPercentage = currentPortfolio.mapValues { (it.value.price / totalMoney) }
        val diff = currentPortPercentage.diff(planPortPercentage)
        val lines = LinkedList<String>()
        lines.add("| ${rebalanceMng.accessInfo.name} 포트폴리오 체크")
        lines.add("| 금      액 : ${totalMoney.toCurrency()}")
        lines.add("| 목 표 밴 드 : 상대 - ${rebalanceMng.bandCheck}%, 절대 - ${rebalanceMng.absBandCheck}")
        lines.add("| 목 표 계 획 : ${orgPortPercent.logForm()}")
        lines.add("| 수정목표계획 : ${planPortPercentage.logForm()}")
        lines.add("| 현      재 : ${currentPortPercentage.logForm()}")
        lines.add("| 차      이 : ${diff.logForm()}")
        lines.add("| 밴      드 : ${currentPortPercentage.calcGap(planPortPercentage).logForm()}")

        val hyphenSize = lines.maxOfOrNull { it.length } ?: 0
        val bracket = "-" * hyphenSize
        lines.addFirst(bracket)
        lines.add(bracket)
        log.info(lines.joinToString("\n", prefix = "\n"))
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