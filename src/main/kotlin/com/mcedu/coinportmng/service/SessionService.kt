package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.repository.AccessInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionService(private val accessInfoRepository: AccessInfoRepository) {

    private val log = LoggerFactory.getLogger(this::class.java)
    private var accessInfo: AccessInfoDto? = null

    @Transactional(readOnly = true)
    fun login(infoSeq: Long) {
        this.accessInfo = accessInfoRepository.findById(infoSeq).orElseThrow { RuntimeException("존재하지 않는 정보입니다.") }.let {
            AccessInfoDto.fromEntity(it)
        }
        log.info("로그인 성공 : ${accessInfo?.name}")
    }
}