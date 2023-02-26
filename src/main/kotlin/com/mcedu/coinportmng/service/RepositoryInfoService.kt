package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.dto.UpbitInfoSaveRequest
import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.type.CoinRepositoryType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.RuntimeException

@Service
class RepositoryInfoService(
    private val accessInfoRepository: AccessInfoRepository
){
    @Transactional(readOnly = true)
    fun getRepositoryInfos(): List<AccessInfoDto> {
        return accessInfoRepository.findAll().map { AccessInfoDto.fromEntity(it) }
    }

    @Transactional
    fun saveAccessInfo(request: UpbitInfoSaveRequest, type: CoinRepositoryType): Long {
        val info = accessInfoRepository.save(
            AccessInfo(
                null,
                request.name,
                request.accessKey,
                request.secretKey,
                request.expireDateTime,
                type
            )
        )
        return info.seq ?: 0
    }

    @Transactional
    fun deleteAccessInfo(infoSeq: Long) {
        val accessInfo = accessInfoRepository.findByIdOrNull(infoSeq) ?: throw RuntimeException("존재하지 않는 정보입니다.")
        accessInfoRepository.delete(accessInfo)
    }
}