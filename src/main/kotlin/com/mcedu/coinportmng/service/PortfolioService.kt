package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.common.Currency
import com.mcedu.coinportmng.common.MainMarket
import com.mcedu.coinportmng.type.Market
import com.mcedu.coinportmng.dto.PortfolioDto
import com.mcedu.coinportmng.dto.UpbitWalletInfo
import com.mcedu.coinportmng.entity.Coin
import com.mcedu.coinportmng.entity.Portfolio
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.repository.PortfolioRepository
import com.mcedu.coinportmng.type.IsYN
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PortfolioService(
    private val accessInfoRepository: AccessInfoRepository,
    private val portfolioRepository: PortfolioRepository,
    private val upbitService: UpbitService
) {

    @Transactional(readOnly = true)
    fun getPortfolios(accessInfoSeq: Long): List<PortfolioDto> {
        val accessInfo =
            accessInfoRepository.findById(accessInfoSeq).orElseThrow { RuntimeException("존재하지 않는 저장소 정보입니다.") }
        val infos = portfolioRepository.findAllByAccessInfo(accessInfo)

        val dtos = infos.map { PortfolioDto(it) }.toMutableList()
        if (dtos.isEmpty()) {
            dtos.add(PortfolioDto("KRW", 1.0))
        }
        return dtos
    }

    @Transactional
    fun savePortfolios(accessInfoSeq: Long, list: List<PortfolioDto>) {
        val accessInfo =
            accessInfoRepository.findById(accessInfoSeq).orElseThrow { RuntimeException("존재하지 않는 저장소 정보입니다.") }
        val infos = portfolioRepository.findAllByAccessInfo(accessInfo).associateBy { it.ticker }.toMutableMap()
        val newInfos = mutableListOf<Portfolio>()
        list.forEach {
            val portfolio = infos[it.ticker]
            if (portfolio == null) {
                newInfos.add(Portfolio(accessInfo = accessInfo, ticker = it.ticker, ratio = it.ratio))
            } else {
                infos.remove(it.ticker)
                portfolio.ratio = it.ratio
            }
        }
        if (newInfos.isNotEmpty()) {
            portfolioRepository.saveAll(newInfos)
        }
        if (infos.isNotEmpty()) {
            portfolioRepository.deleteAll(infos.values)
        }
    }

    fun getCurrentPortfolio(
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