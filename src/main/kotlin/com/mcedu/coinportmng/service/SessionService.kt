package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.repository.AccessInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionService(private val accessInfoRepository: AccessInfoRepository) {

    private val log = LoggerFactory.getLogger(this::class.java)
    private var accessInfo: AccessInfoDto? = null

    @Transactional(readOnly = true)
    fun login(infoSeq: Long) {
        this.accessInfo = findAccessInfo(infoSeq).let {
            AccessInfoDto.fromEntity(it)
        }
        log.info("로그인 성공 : ${accessInfo?.name}")
    }

    fun logout() {
        val name = accessInfo?.name
        accessInfo = null
        log.info("로그아웃 성공 : $name")
    }

    @Transactional(readOnly = true)
    fun getAccessInfo(): AccessInfo {
        return findAccessInfo(accessInfoSeq = this.accessInfo?.seq)
    }

    private fun findAccessInfo(accessInfoSeq: Long?): AccessInfo {
        return accessInfoSeq?.let { accessInfoRepository.findByIdOrNull(it) }?: throw RuntimeException("존재하지 않는 정보입니다.")
    }
}