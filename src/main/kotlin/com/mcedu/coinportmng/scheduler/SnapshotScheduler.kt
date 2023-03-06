package com.mcedu.coinportmng.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.mcedu.coinportmng.entity.DaySnapshot
import com.mcedu.coinportmng.entity.HourSnapshot
import com.mcedu.coinportmng.entity.MinuteSnapshot
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.repository.DaySnapshotRepository
import com.mcedu.coinportmng.repository.HourSnapshotRepository
import com.mcedu.coinportmng.repository.MinuteSnapshotRepository
import com.mcedu.coinportmng.service.PortfolioService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

@Service
class SnapshotScheduler(
    private val accessInfoRepository: AccessInfoRepository,
    private val portfolioService: PortfolioService,
    private val minuteSnapshotRepository: MinuteSnapshotRepository,
    private val hourSnapshotRepository: HourSnapshotRepository,
    private val daySnapshotRepository: DaySnapshotRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper()
    private val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")


    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun snapShot() {
        val accessInfo = accessInfoRepository.findById(3).orElseThrow { RuntimeException("존재하지 않는 저장소 정보입니다.") }
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        val priceMap = portfolioService.getCurrentPortfolio(accessInfo.seq ?: 0)
            .mapValues { it.value.copy(price = it.value.price.roundToLong().toDouble()) }
        val prices = objectMapper.writeValueAsString(priceMap)
        val totalMoney = priceMap.values.sumOf { it.price }

        val minuteSnapshot = MinuteSnapshot(
            accessInfo = accessInfo,
            time = now,
            snapshot = prices,
            totalMoney = totalMoney.roundToLong()
        )

        log.info("SAVE ${now.format(pattern)} snapshot")
        minuteSnapshotRepository.save(minuteSnapshot)
        if (minuteSnapshotRepository.deleteAllByAccessInfoAndTimeBefore(accessInfo, now.minusDays(1)) > 0) {
            log.info("DELETE ${now.minusDays(1).format(pattern)} minute snapshot")
        }

        if (now.minute != 0) {
            return
        }
        hourSnapshotRepository.save(HourSnapshot(minuteSnapshot))
        if (hourSnapshotRepository.deleteAllByAccessInfoAndTimeBefore(accessInfo, now.minusMonths(1)) > 0) {
            log.info("DELETE ${now.minusMonths(1).format(pattern)} hour snapshot")
        }

        if (now.hour != 9) {
            return
        }
        daySnapshotRepository.save(DaySnapshot(minuteSnapshot))
        if (daySnapshotRepository.deleteAllByAccessInfoAndTimeBefore(accessInfo, now.minusYears(1)) > 0) {
            log.info("DELETE ${now.minusYears(1).format(pattern)} day snapshot")
        }
    }
}