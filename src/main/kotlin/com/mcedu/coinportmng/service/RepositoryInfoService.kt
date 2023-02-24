package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.repository.AccessInfoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RepositoryInfoService(
    private val accessInfoRepository: AccessInfoRepository
){
    @Transactional(readOnly = true)
    fun getRepositoryInfos(): List<AccessInfoDto> {
        return accessInfoRepository.findAll().map { AccessInfoDto.fromEntity(it) }
    }
}