package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.DaySnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface DaySnapshotRepository: JpaRepository<DaySnapshot, Long> {
    fun findAllByAccessInfo(accessInfo: AccessInfo): List<DaySnapshot>
    fun deleteAllByAccessInfoAndTimeBefore(accessInfo: AccessInfo, time: LocalDateTime): Int
    fun findByAccessInfoAndTime(accessInfo: AccessInfo, time: LocalDateTime): DaySnapshot?
}