package com.mcedu.coinportmng.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.DaySnapshot
import com.mcedu.coinportmng.entity.HourSnapshot
import com.mcedu.coinportmng.entity.MinuteSnapshot
import com.mcedu.coinportmng.extention.getNextDay
import com.mcedu.coinportmng.extention.getNextHour
import com.mcedu.coinportmng.extention.toCurrency
import com.mcedu.coinportmng.extention.toPercent
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.repository.DaySnapshotRepository
import com.mcedu.coinportmng.repository.HourSnapshotRepository
import com.mcedu.coinportmng.repository.MinuteSnapshotRepository
import com.mcedu.coinportmng.service.PortfolioService
import com.mcedu.coinportmng.service.SlackService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

@Service
class SnapshotScheduler(
    private val slackService: SlackService,
    private val accessInfoRepository: AccessInfoRepository,
    private val portfolioService: PortfolioService,
    private val minuteSnapshotRepository: MinuteSnapshotRepository,
    private val hourSnapshotRepository: HourSnapshotRepository,
    private val daySnapshotRepository: DaySnapshotRepository
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper()
    private val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")


    @Scheduled(cron = "1 * * * * *")
    @Transactional
    fun snapShot() {
        val now = LocalDateTime.now().withSecond(0).withNano(0).minusMinutes(1)
        val isSnapshotTime = now.hour == 9 && now.minute == 0
        for (accessInfo in accessInfoRepository.findAll()) {
            innerSnapshot(accessInfo, now)
            if (isSnapshotTime) {
                sendSnapshot(accessInfo, now)
            }
        }
    }

    private fun sendSnapshot(accessInfo: AccessInfo, now: LocalDateTime) {
        val target = now.minusDays(1)
        val daySnapshot = daySnapshotRepository.findByAccessInfoAndTime(accessInfo, target) ?: return
        val yesterdaySnapshot = daySnapshotRepository.findByAccessInfoAndTime(accessInfo, target.minusDays(1))

        val title = daySnapshot.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        var message = daySnapshot.totalMoney.toDouble().toCurrency() + "원"
        if(yesterdaySnapshot != null) {
            val percent =
                ((daySnapshot.totalMoney - yesterdaySnapshot.totalMoney) / yesterdaySnapshot.totalMoney.toDouble()).toPercent(
                    2
                )

            if (percent > 0) {
                message += "(${percent}%)"
            }
        }
        slackService.sendMessage(title = title, message = message)
    }

    private fun innerSnapshot(accessInfo: AccessInfo, now: LocalDateTime) {
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

        val nextHour = now.getNextHour()
        val hourSnapshot = hourSnapshotRepository.findByAccessInfoAndTime(accessInfo, nextHour)
            ?: HourSnapshot(minuteSnapshot)
        hourSnapshot.update(minuteSnapshot, nextHour)
        hourSnapshotRepository.save(hourSnapshot)
        if (hourSnapshotRepository.deleteAllByAccessInfoAndTimeBefore(accessInfo, now.minusMonths(1)) > 0) {
            log.info("DELETE ${now.minusMonths(1).format(pattern)} hour snapshot")
        }

        val nextDay = now.getNextDay()
        val daySnapshot =
            daySnapshotRepository.findByAccessInfoAndTime(accessInfo, nextDay) ?: DaySnapshot(minuteSnapshot)
        daySnapshot.update(minuteSnapshot, nextDay)
        daySnapshotRepository.save(daySnapshot)
        if (daySnapshotRepository.deleteAllByAccessInfoAndTimeBefore(accessInfo, now.minusYears(1)) > 0) {
            log.info("DELETE ${now.minusYears(1).format(pattern)} day snapshot")
        }
    }
}