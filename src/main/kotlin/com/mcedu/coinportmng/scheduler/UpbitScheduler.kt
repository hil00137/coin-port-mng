package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.common.Currency
import com.mcedu.coinportmng.common.IntervalConstant.DAILY
import com.mcedu.coinportmng.common.IntervalConstant.FIVE_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.HALF_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.MONTHLY
import com.mcedu.coinportmng.common.IntervalConstant.QUARTER_HOURLY
import com.mcedu.coinportmng.common.IntervalConstant.TEN_MINUTELY
import com.mcedu.coinportmng.common.IntervalConstant.YEARLY
import com.mcedu.coinportmng.common.MainMarket
import com.mcedu.coinportmng.common.MainMarket.BTC_USDT
import com.mcedu.coinportmng.common.MainMarket.KRW_BTC
import com.mcedu.coinportmng.common.Market
import com.mcedu.coinportmng.dto.UpbitWalletInfo
import com.mcedu.coinportmng.entity.Coin
import com.mcedu.coinportmng.entity.RebalanceMng
import com.mcedu.coinportmng.extention.getSecondsOfDay
import com.mcedu.coinportmng.repository.CoinRepository
import com.mcedu.coinportmng.repository.RebalanceMngRepository
import com.mcedu.coinportmng.service.UpbitService
import com.mcedu.coinportmng.type.IsYN
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class UpbitScheduler(
    private val upbitService: UpbitService,
    private val coinRepository: CoinRepository,
    private val rebalanceMngRepository: RebalanceMngRepository
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
            if (intervalCheck(it, now)) {
                log.info("execute")
                executeSet.add(it)
            } else if (it.bandRebalance && bandCheck(it)) {
                log.info("execute")
                executeSet.add(it)
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
        val currentPortfolio = getCurrentPortfolio(hasMarketWallet, coinMap)
        log.info(currentPortfolio.toString())

        return false
    }

    private fun getCurrentPortfolio(
        hasMarketWallet: List<UpbitWalletInfo>,
        coinMap: Map<String, Coin>
    ): MutableMap<String, Double> {

        val setOfMarket = hashSetOf<String>()
        for ((ticker, coin) in coinMap) {
            if (coin.krwMarket == IsYN.Y) {
                setOfMarket.add("KRW-$ticker")
            } else if (coin.btcMarket == IsYN.Y) {
                setOfMarket.add("BTC-$ticker")
                setOfMarket.add(KRW_BTC)
            } else if (coin.usdtMarket == IsYN.Y) {
                setOfMarket.add("USDT-$ticker")
                setOfMarket.add(BTC_USDT)
                setOfMarket.add(KRW_BTC)
            }
        }
        val priceDtoMap = upbitService.getCurrentPrice(setOfMarket).associateBy { it.market }

        val currentPortfolio = mutableMapOf<String, Double>()
        val krwBtcPrice = priceDtoMap[KRW_BTC]?.tradePrice ?: 0.0
        val btcUsdtPrice = priceDtoMap[BTC_USDT]?.tradePrice ?: 0.0
        for (walletInfo in hasMarketWallet) {
            val ticker = walletInfo.currency
            if (ticker == Currency.KRW) {
                currentPortfolio[Currency.KRW] = walletInfo.balance
                continue
            }
            val coin = coinMap[ticker] ?: continue
            var money = 0.0
            if (coin.krwMarket == IsYN.Y) {
                val price = priceDtoMap["KRW-$ticker"]?.tradePrice ?: 0.0
                money = Market.KRW.changeMoney(price * walletInfo.balance)
            } else if (coin.btcMarket == IsYN.Y) {
                val btcPrice = priceDtoMap["BTC-$ticker"]?.tradePrice ?: 0.0
                val btcBalance = Market.BTC.changeMoney(btcPrice * walletInfo.balance)
                money = Market.KRW.changeMoney(krwBtcPrice * btcBalance)
            } else if (coin.usdtMarket == IsYN.Y) {
                val usdtPrice = priceDtoMap["USDT-$ticker"]?.tradePrice ?: 0.0
                val usdtBalance = Market.USDT.changeMoney(usdtPrice * walletInfo.balance)
                val btcBalance = Market.BTC.changeMoney(usdtBalance * btcUsdtPrice)
                money = Market.KRW.changeMoney(btcBalance * krwBtcPrice)
            }
            currentPortfolio[ticker] = money
        }
        return currentPortfolio
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