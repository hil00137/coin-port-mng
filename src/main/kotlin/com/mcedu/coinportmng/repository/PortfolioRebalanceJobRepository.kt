package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.PortfolioRebalanceJob
import org.springframework.data.jpa.repository.JpaRepository

interface PortfolioRebalanceJobRepository: JpaRepository<PortfolioRebalanceJob, Long> {
    fun findByAccessInfo(accessInfo: AccessInfo) : PortfolioRebalanceJob?
}