package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.common.ReblanceJobStatus
import com.mcedu.coinportmng.dto.PortfolioJobDto
import com.mcedu.coinportmng.repository.CoinRepository
import com.mcedu.coinportmng.repository.PortfolioRebalanceJobRepository
import com.mcedu.coinportmng.service.PortfolioService
import com.mcedu.coinportmng.service.UpbitService
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

    private val currentJob = PortfolioJobDto()

    @Scheduled(fixedDelay = 10000)
    @Transactional
    fun execute() {
        log.info("execute")
        val job = portfolioRebalanceJobRepository.findFirstByOrderBySeq()
        log.info(job?.toString())
        if (job == null) {
            return
        }
        if (job.status == ReblanceJobStatus.DOING) {
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
        job.status = ReblanceJobStatus.DOING
        // TODO : job status DOING
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional(readOnly = true)
    fun checkTarget() {
        val jobSeq = currentJob.jobSeq ?: return
        val infoSeq = currentJob.infoSeq ?: return
        if (currentJob.jobStatus == ReblanceJobStatus.DOING) {
            val command = portfolioCheck()
            when (command.commandType) {
                CommandType.SELL -> {
                    currentJob.jobStatus = ReblanceJobStatus.SELL
                    currentJob.command = command
                }
                CommandType.BUY -> log.info("buy")
                CommandType.NONE -> log.info("stop")
            }
        } else if (currentJob.jobStatus == ReblanceJobStatus.SELL) {
            log.info("sell")
            currentJob.response = upbitService.sell(infoSeq, currentJob.command)
            currentJob.jobStatus = ReblanceJobStatus.SELL_DONE
        } else if (currentJob.jobStatus == ReblanceJobStatus.SELL_DONE) {
            log.info("sell done")
        }
    }

    private fun portfolioCheck(): Command {
        val accounts = upbitService.getMyAccounts(currentJob.infoSeq ?: 0)
        val currencyStrs = accounts.map { it.currency }
        val coinMap = coinRepository.findAllById(currencyStrs).associateBy { it.ticker }
        val hasMarketWallet = accounts.filter { coinMap.containsKey(it.currency) || it.currency == "KRW" }
        val walletInfoMap = hasMarketWallet.associateBy { it.currency }
        var portfolios = currentJob.portfolios.mapValues { it.value.ratio }
        val currentPortfolio = portfolioService.getCurrentPortfolio(hasMarketWallet, coinMap)
        val planSum = portfolios.values.sum()
        portfolios = portfolios.mapValues { it.value / planSum }

        val currentSum = currentPortfolio.values.sum()
        val pairMap = currentPortfolio.mapValues { Pair(it.value, it.value / currentSum) }

        val tempBuyCommand = hashSetOf<Command>()

        for ((key, pair) in pairMap) {
            if (key == "KRW") {
                log.info("$key > is money skip")
                continue
            }
            if (pair.first < 7000) {
                log.info("$key > not enough money skip")
                continue
            }
            val plan = portfolios[key]?.let { it / 100.0 }
            val balance = walletInfoMap[key]?.balance ?: 0.0
            if (plan == null) {
                log.info("$key > sell target")
                return Command(CommandType.SELL, key, balance)
            }

            if (plan < pair.second) {
                val sellPercent = pair.second - plan
                val overMoney = sellPercent * currentSum
                if (overMoney < 7000) {
                    log.info("$key > not enough money skip")
                    continue
                }

                return Command(CommandType.SELL, key, (balance * sellPercent))
            }

            if (plan > pair.second) {
                val notEnoughMoney = (plan - pair.second) * currentSum
                if (notEnoughMoney < 7000) {
                    log.info("$key > not enough money skip")
                    continue
                }
                tempBuyCommand.add(Command(CommandType.BUY, key, notEnoughMoney))
            }
        }

        for ((planKey, plan) in portfolios) {
            if (!currentPortfolio.containsKey(planKey) && planKey != "KRW") {
                log.info("$planKey > add target")
                tempBuyCommand.add(Command(CommandType.BUY, planKey, ((plan / 100.0) * currentSum)))
            }
        }

        return tempBuyCommand.maxByOrNull { it.price } ?: Command()
    }
}

data class Command(val commandType: CommandType = CommandType.NONE, val ticker: String = "", val price: Double = 0.0)

enum class CommandType {
    SELL, BUY, NONE
}