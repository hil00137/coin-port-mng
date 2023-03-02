package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.RebalanceMng
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RebalanceMngRepository : JpaRepository<RebalanceMng, Long> {
    fun findRebalanceMngByAccessInfo(accessInfo: AccessInfo): RebalanceMng?

    @Query("select r from RebalanceMng r join fetch r.accessInfo where r.active = :active")
    fun findAllByActive(active: Boolean = true): List<RebalanceMng>
}