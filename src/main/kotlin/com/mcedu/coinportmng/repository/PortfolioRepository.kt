package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.entity.Portfolio
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PortfolioRepository: JpaRepository<Portfolio, Long> {
    @Query("select p from Portfolio p join fetch p.accessInfo where p.accessInfo = :accessInfo")
    fun findAllByAccessInfo(accessInfo: AccessInfo): List<Portfolio>
}