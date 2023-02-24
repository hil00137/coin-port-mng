package com.mcedu.coinportmng.repository

import com.mcedu.coinportmng.entity.AccessInfo
import org.springframework.data.jpa.repository.JpaRepository

interface AccessInfoRepository: JpaRepository<AccessInfo, Long>