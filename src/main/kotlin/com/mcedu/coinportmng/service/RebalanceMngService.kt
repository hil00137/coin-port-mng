package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.RebalancePlanDto
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.repository.RebalanceMngRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
}