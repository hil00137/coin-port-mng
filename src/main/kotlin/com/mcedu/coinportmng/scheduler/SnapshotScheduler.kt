package com.mcedu.coinportmng.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.mcedu.coinportmng.entity.MinuteSnapshot
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.repository.MinuteSnapshotRepository
import com.mcedu.coinportmng.service.PortfolioService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.roundToLong

@Service
class SnapshotScheduler(
    private val accessInfoRepository: AccessInfoRepository,
    private val portfolioService: PortfolioService,
    private val minuteSnapshotRepository: MinuteSnapshotRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val objectMapper = ObjectMapper()

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun snapShot() {
        val accessInfo = accessInfoRepository.findById(3).orElseThrow { RuntimeException("존재하지 않는 저장소 정보입니다.") }
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        val priceMap = portfolioService.getCurrentPortfolio(accessInfo.seq ?: 0)
            .mapValues { it.value.copy(price = it.value.price.roundToLong().toDouble()) }
        val prices = objectMapper.writeValueAsString(priceMap)
        val totalMoney = priceMap.values.sumOf { it.price }

        minuteSnapshotRepository.save(MinuteSnapshot(accessInfo = accessInfo, time = now, snapshot = prices, totalMoney = totalMoney.roundToLong()))
        minuteSnapshotRepository.deleteAllByAccessInfoAndTimeBefore(accessInfo, now.minusDays(1))
    }
}