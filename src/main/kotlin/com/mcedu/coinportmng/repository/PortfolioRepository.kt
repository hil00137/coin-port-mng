package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.Portfolio
import org.springframework.data.jpa.repository.JpaRepository

interface PortfolioRepository: JpaRepository<Portfolio, Long> {
    fun findAllByAccessInfo(accessInfo: AccessInfo): List<Portfolio>
}