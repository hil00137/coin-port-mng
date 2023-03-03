package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.common.Currency
import com.mcedu.coinportmng.common.MainMarket
import com.mcedu.coinportmng.common.Market
import com.mcedu.coinportmng.common.ReblanceJobStatus
import com.mcedu.coinportmng.dto.PortfolioJobDto
import com.mcedu.coinportmng.dto.UpbitWalletInfo
import com.mcedu.coinportmng.entity.Coin
import com.mcedu.coinportmng.repository.CoinRepository
import com.mcedu.coinportmng.repository.PortfolioRebalanceJobRepository
import com.mcedu.coinportmng.service.PortfolioService
import com.mcedu.coinportmng.service.UpbitService
import com.mcedu.coinportmng.type.IsYN
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RebalanceScheduler(
    private val portfolioRebalanceJobRepository: PortfolioRebalanceJobRepository,
    private val portfolioService: PortfolioService,
    private val coinRepository: CoinRepository,
    private val upbitService: UpbitService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val currentJob = PortfolioJobDto(null,)

    @Scheduled(fixedDelay = 10000)
    @Transactional(readOnly = true)
    fun execute() {
        log.info("execute")
        val job = portfolioRebalanceJobRepository.findFirstByOrderBySeq()
        log.info(job?.toString())
        if (job == null) {
            return
        }
        if ( job.status == ReblanceJobStatus.DOING) {
            if (currentJob.jobSeq == null) {
                currentJob.jobSeq = job.seq
                currentJob.jobStatus = ReblanceJobStatus.RECOVERY
            }
            return
        }
        val portfolios = portfolioService.getPortfolios(job.accessInfo.seq ?: 0).associateBy { it.ticker }
        currentJob.jobStatus = ReblanceJobStatus.DOING
        currentJob.portfolios = portfolios
        currentJob.jobSeq = job.seq
        currentJob.infoSeq = job.accessInfo.seq
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional(readOnly = true)
    fun checkTarget() {
        val jobSeq = currentJob.jobSeq ?: return
        if (currentJob.jobStatus == ReblanceJobStatus.DOING) {
            val command = portfolioCheck()
            when (command.commandType) {
                CommandType.SELL -> log.info("sell")
                CommandType.BUY -> log.info("buy")
                CommandType.NONE -> log.info("stop")
            }
        }
    }

    private fun portfolioCheck(): Command {
        val accounts = upbitService.getMyAccounts(currentJob.infoSeq ?: 0)
        val currencyStrs = accounts.map { it.currency }
        val coinMap = coinRepository.findAllById(currencyStrs).associateBy { it.ticker }
        val hasMarketWallet = accounts.filter { coinMap.containsKey(it.currency) || it.currency == "KRW" }
        var portfolios = currentJob.portfolios.mapValues { it.value.ratio }
        val currentPortfolio = getCurrentPortfolio(hasMarketWallet, coinMap)
        val planSum = portfolios.values.sum()
        portfolios = portfolios.mapValues { it.value / planSum }

        val currentSum = currentPortfolio.values.sum()
        val pairMap = currentPortfolio.mapValues { Pair(it.value, it.value / currentSum) }

        val tempBuyCommand = hashSetOf<Command>()

        for ((key, pair) in pairMap) {
            if (pair.first < 7000) {
                log.info("$key > not enough money skip")
                continue
            }
            val plan = portfolios[key]?.let { it / 100.0 }
            if (plan == null) {
                log.info("$key > sell target")
                return Command(CommandType.SELL, key, "all")
            }

            if (plan < pair.second) {
                val overMoney = (pair.second - plan) * currentSum
                if (overMoney < 7000) {
                    log.info("$key > not enough money skip")
                    continue
                }
                return Command(CommandType.SELL, key, overMoney.toString())
            }

            if (plan > pair.second) {
                val notEnoughMoney = (plan - pair.second) * currentSum
                if (notEnoughMoney < 7000) {
                    log.info("$key > not enough money skip")
                    continue
                }
                tempBuyCommand.add(Command(CommandType.BUY, key, notEnoughMoney.toString()))
            }
        }

        for ((planKey, plan) in portfolios) {
            if (!currentPortfolio.containsKey(planKey) && planKey != "KRW") {
                log.info("$planKey > add target")
                tempBuyCommand.add(Command(CommandType.BUY, planKey, ((plan / 100.0) * currentSum).toString()))
            }
        }

        return tempBuyCommand.maxByOrNull { it.price.toDouble() } ?: Command()
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
                setOfMarket.add(MainMarket.KRW_BTC)
            } else if (coin.usdtMarket == IsYN.Y) {
                setOfMarket.add("USDT-$ticker")
                setOfMarket.add(MainMarket.BTC_USDT)
                setOfMarket.add(MainMarket.KRW_BTC)
            }
        }
        val priceDtoMap = upbitService.getCurrentPrice(setOfMarket).associateBy { it.market }

        val currentPortfolio = mutableMapOf<String, Double>()
        val krwBtcPrice = priceDtoMap[MainMarket.KRW_BTC]?.tradePrice ?: 0.0
        val btcUsdtPrice = priceDtoMap[MainMarket.BTC_USDT]?.tradePrice ?: 0.0
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
}

data class Command(val commandType: CommandType = CommandType.NONE, val ticker: String = "", val price: String = "")

enum class CommandType {
    SELL, BUY, NONE
}