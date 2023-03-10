package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.dto.Command
import com.mcedu.coinportmng.dto.PortfolioJobDto
import com.mcedu.coinportmng.repository.PortfolioRebalanceJobRepository
import com.mcedu.coinportmng.service.PortfolioService
import com.mcedu.coinportmng.service.UpbitIndexService
import com.mcedu.coinportmng.service.UpbitService
import com.mcedu.coinportmng.type.CommandType
import com.mcedu.coinportmng.type.Market
import com.mcedu.coinportmng.type.ReblanceJobStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

@Component
class RebalanceScheduler(
    private val portfolioRebalanceJobRepository: PortfolioRebalanceJobRepository,
    private val portfolioService: PortfolioService,
    private val upbitService: UpbitService,
    private val upbitIndexService: UpbitIndexService
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val currentJob = PortfolioJobDto()

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun execute() {
        val job = portfolioRebalanceJobRepository.findFirstByOrderBySeq() ?: return
        currentJob.jobSeq = job.seq
        currentJob.infoSeq = job.accessInfo.seq
        val accessInfoSeq = job.accessInfo.seq ?: 0
        if (currentJob.portfolios.isEmpty()) {
            currentJob.jobStatus = ReblanceJobStatus.DOING
            val portfolios = portfolioService.getPortfolios(accessInfoSeq).associateBy { it.ticker }
            currentJob.portfolios = portfolios
        }
        if (job.status == ReblanceJobStatus.DOING) {
            return
        }
        currentJob.jobStatus = ReblanceJobStatus.DOING
        job.status = ReblanceJobStatus.DOING
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun checkTarget() {
        val jobSeq = currentJob.jobSeq ?: return
        val infoSeq = currentJob.infoSeq ?: return
        val currentJobCommand = currentJob.command
        when (currentJob.jobStatus) {
            ReblanceJobStatus.DOING -> {
                val command = portfolioCheck()
                when (command.commandType) {
                    CommandType.SELL -> {
                        currentJob.jobStatus = ReblanceJobStatus.SELL
                        currentJob.command = command
                    }
                    CommandType.BUY -> {
                        currentJob.jobStatus = ReblanceJobStatus.BUY
                        currentJob.command = command
                    }
                    CommandType.NONE -> {
                        currentJob.reset()
                        portfolioRebalanceJobRepository.deleteById(jobSeq)
                    }
                }
            }
            ReblanceJobStatus.SELL -> {
                currentJob.response = upbitService.sell(infoSeq, currentJobCommand)
                currentJob.jobStatus = ReblanceJobStatus.ORDER
            }
            ReblanceJobStatus.ORDER -> {
                log.info("Order wait > ${currentJobCommand.ticker}")
                val response = currentJob.response
                if (response == null) {
                    currentJob.reset()
                    currentJob.jobStatus = ReblanceJobStatus.DOING
                    return
                }
                val checkOrder = upbitService.checkOrder(infoSeq, response)
                if (checkOrder?.state == "cancel" || checkOrder?.state == "done") {
                    currentJob.reset()
                    currentJob.jobStatus = ReblanceJobStatus.DOING
                }
                log.info(checkOrder?.toString())
            }
            ReblanceJobStatus.BUY -> {
                currentJob.response = upbitService.buy(infoSeq, currentJobCommand)
                currentJob.jobStatus = ReblanceJobStatus.ORDER
            }
            ReblanceJobStatus.RECOVERY -> {
                log.info("recovery")
            }
            else -> {
                log.info(currentJob.toString())
            }
        }
    }

    private fun portfolioCheck(): Command {
        var portfolios = currentJob.portfolios.mapValues { it.value.ratio }
        val currentPortfolio = portfolioService.getCurrentPortfolio(currentJob.infoSeq ?: 0)
        val planSum = portfolios.values.sum()
        portfolios = portfolios.mapValues { it.value / planSum }

        val totalMoney = currentPortfolio.values.sumOf { it.price }
        portfolios = upbitIndexService.changeIndexRatio(currentPortfolio, portfolios, totalMoney)

        val pairMap = currentPortfolio.mapValues { Pair(it.value.price, it.value.price / totalMoney) }
        val tempBuyCommand = hashSetOf<Command>()
        val tempSellCommands = hashSetOf<Command>()
        val rebalanceCommands = hashSetOf<Command>()
        for ((key, pair) in pairMap) {
            if (key == "KRW") {
                continue
            }
            val plan = portfolios[key]
            val balance = currentPortfolio[key]?.balance ?: 0.0
            if (plan == null) {
                log.info("$key - 포트폴리오 대상 아님 판매")
                if (pair.first < Market.KRW.getExtraBalance()) {
                    rebalanceCommands.add(Command.forRebalance(key))
                } else {
                    tempSellCommands.add(Command(CommandType.SELL, key, volume = balance, price = pair.first))
                }
                break
            }

            val diff = abs(pair.second - plan)
            if (diff * 100 < 0.1) {
                continue
            }


            if (plan < pair.second) {
                val overMoney = diff * totalMoney
                if (overMoney >= Market.KRW.getExtraBalance()) {
                    val volume = balance * ((overMoney) / (currentPortfolio[key]?.price ?: 0.0))
                    tempSellCommands.add(Command(CommandType.SELL, key, volume = volume, price = overMoney))
                } else {
                    rebalanceCommands.add(Command.forRebalance(key))
                }
                continue
            }

            if (plan > pair.second) {
                val notEnoughMoney = diff * totalMoney
                if (notEnoughMoney >= Market.KRW.getExtraBalance()) {
                    tempBuyCommand.add(Command.buy(key, price = notEnoughMoney))
                } else {
                    rebalanceCommands.add(Command.forRebalance(key))
                }
            }
        }

        if (tempSellCommands.isNotEmpty()) {
            val command = tempSellCommands.maxBy { it.price }
            log.info("${command.ticker} - ${command.volume}${command.ticker} 시장가 매도")
            return command
        }

        if (rebalanceCommands.isNotEmpty()) {
            val command = rebalanceCommands.first()
            log.info("${command.ticker} - 대상 제거를 위한 추가 ${command.price}원 시장가 매수")
            return command
        }

        for ((planKey, plan) in portfolios) {
            if (!currentPortfolio.containsKey(planKey) && planKey != "KRW") {
                log.info("$planKey > add target")
                tempBuyCommand.add(Command.buy(planKey, price =  (plan * totalMoney)))
            }
        }

        val command = tempBuyCommand.maxBy { it.price }
        log.info("${command.ticker} - 대상 신규 구매 ${command.price}원 시장가 매수")
        return command
    }
}