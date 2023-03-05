package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.dto.Command
import com.mcedu.coinportmng.dto.PortfolioJobDto
import com.mcedu.coinportmng.repository.PortfolioRebalanceJobRepository
import com.mcedu.coinportmng.service.PortfolioService
import com.mcedu.coinportmng.service.UpbitIndexService
import com.mcedu.coinportmng.service.UpbitService
import com.mcedu.coinportmng.type.CommandType
import com.mcedu.coinportmng.type.ReblanceJobStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
        if (job.status == ReblanceJobStatus.DOING) {
            if (currentJob.jobSeq == null) {
                currentJob.jobStatus = ReblanceJobStatus.DOING
                currentJob.jobSeq = job.seq
                currentJob.infoSeq = job.accessInfo.seq
                val portfolios = portfolioService.getPortfolios(job.accessInfo.seq ?: 0).associateBy { it.ticker }
                currentJob.portfolios = portfolios
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

    @Scheduled(fixedDelay = 1000)
    @Transactional
    fun checkTarget() {
        val jobSeq = currentJob.jobSeq ?: return
        val infoSeq = currentJob.infoSeq ?: return
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
                log.info("sell")
                currentJob.response = upbitService.sell(infoSeq, currentJob.command)
                currentJob.jobStatus = ReblanceJobStatus.ORDER
            }
            ReblanceJobStatus.ORDER -> {
                log.info("ORDER")
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
                log.info("buy")
                currentJob.response = upbitService.buy(infoSeq, currentJob.command)
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
        portfolios = upbitIndexService.changeIndexRatio(portfolios, totalMoney)

        val pairMap = currentPortfolio.mapValues { Pair(it.value.price, it.value.price / totalMoney) }
        val tempBuyCommand = hashSetOf<Command>()

        for ((key, pair) in pairMap) {
            if (key == "KRW") {
                log.info("$key > is money skip")
                continue
            }
            if (pair.first < 6000) {
                log.info("$key > not enough money skip")
                continue
            }
            val plan = portfolios[key]
            val balance = currentPortfolio[key]?.balance ?: 0.0
            if (plan == null) {
                log.info("$key > sell target")
                return Command(CommandType.SELL, key, volume = balance)
            }

            if (plan < pair.second) {
                val sellPercent = pair.second - plan
                val overMoney = sellPercent * totalMoney
                if (overMoney < 6000) {
                    log.info("$key > not enough money skip")
                    continue
                }

                return Command(CommandType.SELL, key, volume = (balance * sellPercent))
            }

            if (plan > pair.second) {
                val notEnoughMoney = (plan - pair.second) * totalMoney
                if (notEnoughMoney < 6000) {
                    log.info("$key > not enough money skip")
                    continue
                }
                tempBuyCommand.add(Command(CommandType.BUY, key, price = notEnoughMoney))
            }
        }

        for ((planKey, plan) in portfolios) {
            if (!currentPortfolio.containsKey(planKey) && planKey != "KRW") {
                log.info("$planKey > add target")
                tempBuyCommand.add(Command(CommandType.BUY, planKey, price =  (plan * totalMoney)))
            }
        }

        return tempBuyCommand.maxByOrNull { it.price } ?: Command()
    }
}