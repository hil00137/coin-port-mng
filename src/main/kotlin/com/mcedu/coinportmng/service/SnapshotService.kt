package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.SnapshotDto
import com.mcedu.coinportmng.repository.DaySnapshotRepository
import com.mcedu.coinportmng.repository.HourSnapshotRepository
import com.mcedu.coinportmng.repository.MinuteSnapshotRepository
import org.springframework.stereotype.Service

@Service
class SnapshotService(
    private val minuteSnapshotRepository: MinuteSnapshotRepository,
    private val hourSnapshotRepository: HourSnapshotRepository,
    private val daySnapshotRepository: DaySnapshotRepository
) {

    fun getSnapshot(type: String): List<SnapshotDto> {
        return when (type) {
            "minute" -> minuteSnapshotRepository.findAll().map { SnapshotDto(time = it.time, totalMoney = it.totalMoney) }
            "hour" -> hourSnapshotRepository.findAll().map { SnapshotDto(time = it.time, totalMoney = it.totalMoney) }
            "day" -> daySnapshotRepository.findAll().map { SnapshotDto(time = it.time, totalMoney = it.totalMoney) }
            else -> throw RuntimeException("올바르지 않은 타입 [$type]입니다.\n가능한 타입은 'day' 입니다.")
        }
    }
}