package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.MinuteSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface MinuteSnapshotRepository: JpaRepository<MinuteSnapshot, Long> {
    fun findAllByAccessInfo(accessInfo: AccessInfo): List<MinuteSnapshot>
    fun deleteAllByAccessInfoAndTimeBefore(accessInfo: AccessInfo, time: LocalDateTime): Int
}