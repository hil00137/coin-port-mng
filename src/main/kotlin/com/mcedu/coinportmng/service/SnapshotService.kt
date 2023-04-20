package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.SnapshotDto
import com.mcedu.coinportmng.repository.DaySnapshotRepository
import com.mcedu.coinportmng.repository.HourSnapshotRepository
import com.mcedu.coinportmng.repository.MinuteSnapshotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SnapshotService(
    private val sessionService: SessionService,
    private val minuteSnapshotRepository: MinuteSnapshotRepository,
    private val hourSnapshotRepository: HourSnapshotRepository,
    private val daySnapshotRepository: DaySnapshotRepository
) {

    @Transactional(readOnly = true)
    fun getSnapshot(type: String): List<SnapshotDto> {
        val accessInfo = sessionService.getAccessInfo()
        return when (type) {
            "minute" -> minuteSnapshotRepository.findAllByAccessInfo(accessInfo).map { SnapshotDto(time = it.time, totalMoney = it.totalMoney, snapshot = it.snapshot) }
            "hour" -> hourSnapshotRepository.findAllByAccessInfo(accessInfo).map { SnapshotDto(time = it.time, totalMoney = it.totalMoney, snapshot = it.snapshot) }
            "day" -> daySnapshotRepository.findAllByAccessInfo(accessInfo).map { SnapshotDto(time = it.time, totalMoney = it.totalMoney, snapshot = it.snapshot) }
            else -> throw RuntimeException("올바르지 않은 타입 [$type]입니다.\n가능한 타입은 'minute', 'hour', 'day' 입니다.")
        }.sortedBy { it.time }
    }
}