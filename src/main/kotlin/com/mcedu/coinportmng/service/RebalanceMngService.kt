package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.common.IntervalConstant
import com.mcedu.coinportmng.dto.RebalancePlanDto
import com.mcedu.coinportmng.entity.RebalanceMng
import com.mcedu.coinportmng.extention.getSecondsOfDay
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.repository.RebalanceMngRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
class RebalanceMngService(
    private val accessInfoRepository: AccessInfoRepository,
    private val rebalanceMngRepository: RebalanceMngRepository
) {

    @Transactional(readOnly = true)
    fun getRebalanceMng(infoSeq: Long): RebalancePlanDto? {
        val accessInfo = accessInfoRepository.findById(infoSeq).orElseThrow { RuntimeException("존재하지 않는 정보입니다.") }
        return rebalanceMngRepository.findRebalanceMngByAccessInfo(accessInfo)?.let {
            RebalancePlanDto(it)
        }
    }

    @Transactional
    fun upsertRebalnaceMng(infoSeq: Long, planDto: RebalancePlanDto) {
        validationCheck(planDto)
        val accessInfo = accessInfoRepository.findById(infoSeq).orElseThrow { RuntimeException("존재하지 않는 정보입니다.") }
        val rebalanceMng = rebalanceMngRepository.findRebalanceMngByAccessInfo(accessInfo)
        val baseTime = LocalTime.of(planDto.baseHour, planDto.baseMinute).getSecondsOfDay()
        if (rebalanceMng == null) {
            rebalanceMngRepository.save(
                RebalanceMng(
                    accessInfo = accessInfo,
                    baseMonth = planDto.baseMonth,
                    baseDay = planDto.baseDay,
                    baseTime = baseTime,
                    interval = planDto.interval,
                    active = planDto.active,
                    bandRebalance = planDto.bandRebalance,
                    bandCheck = planDto.bandCheck
                )
            )
        } else {
            rebalanceMng.apply {
                this.baseMonth = planDto.baseMonth
                this.baseDay = planDto.baseDay
                this.baseTime = baseTime
                this.interval = planDto.interval
                this.active = planDto.active
                this.bandRebalance = planDto.bandRebalance
                this.bandCheck = planDto.bandCheck
            }
        }
    }

    private fun validationCheck(planDto: RebalancePlanDto) {
        val montRange = 1..12
        if (!montRange.contains(planDto.baseMonth)) {
            throw RuntimeException("월은 1~12 사이어야 합니다.")
        }
        val localDate = LocalDate.of(2022, planDto.baseMonth, 1)
        val dayRange = 1..localDate.lengthOfMonth()
        if (!dayRange.contains(planDto.baseDay)) {
            throw RuntimeException("${planDto.baseMonth}월은 1~${dayRange.last} 사이어야 합니다.")
        }
        val hourRange = 0..23
        if (!hourRange.contains(planDto.baseHour)) {
            throw RuntimeException("시간은 0~23 사이어야 합니다.")
        }
        val minuteRange = 0..59
        if (!minuteRange.contains(planDto.baseMinute)) {
            throw RuntimeException("분은 0~59 사이어야 합니다.")
        }

        if (!IntervalConstant.all.contains(planDto.interval)) {
            throw RuntimeException("주기는 다음과 같습니다. ${IntervalConstant.all}")
        }

        if (planDto.bandCheck <= 0) {
            throw RuntimeException("밴드 체크는 0 초과여야합니다.")
        }
    }
}