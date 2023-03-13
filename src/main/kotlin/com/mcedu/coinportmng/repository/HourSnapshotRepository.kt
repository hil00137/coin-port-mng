package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.HourSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface HourSnapshotRepository: JpaRepository<HourSnapshot, Long> {
    fun deleteAllByAccessInfoAndTimeBefore(accessInfo: AccessInfo, time: LocalDateTime): Int
    fun findByAccessInfoAndTime(accessInfo: AccessInfo, time: LocalDateTime): HourSnapshot?
}