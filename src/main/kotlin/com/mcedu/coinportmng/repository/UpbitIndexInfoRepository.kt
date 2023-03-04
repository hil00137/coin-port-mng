package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.UpbitIndexInfo
import com.mcedu.coinportmng.type.UpbitIndex
import org.springframework.data.jpa.repository.JpaRepository

interface UpbitIndexInfoRepository: JpaRepository<UpbitIndexInfo, Long> {
    fun findByName(name: UpbitIndex): UpbitIndexInfo?
}