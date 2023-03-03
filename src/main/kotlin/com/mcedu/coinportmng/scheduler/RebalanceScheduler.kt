package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.common.ReblanceJobStatus
import com.mcedu.coinportmng.dto.PortfolioJobDto
import com.mcedu.coinportmng.repository.PortfolioRebalanceJobRepository
import com.mcedu.coinportmng.service.PortfolioService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RebalanceScheduler(
    private val portfolioRebalanceJobRepository: PortfolioRebalanceJobRepository,
    private val portfolioService: PortfolioService
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
        currentJob.portfolios = portfolios
        currentJob.jobSeq = job.seq
    }
}