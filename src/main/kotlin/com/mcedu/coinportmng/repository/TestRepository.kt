package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.TempEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TestRepository: JpaRepository<TempEntity, Long> {
}