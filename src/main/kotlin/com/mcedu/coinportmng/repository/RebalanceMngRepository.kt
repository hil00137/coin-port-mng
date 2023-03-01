package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.RebalanceMng
import org.springframework.data.jpa.repository.JpaRepository

interface RebalanceMngRepository : JpaRepository<RebalanceMng, Long> {
    fun findRebalanceMngByAccessInfo(accessInfo: AccessInfo): RebalanceMng?
}