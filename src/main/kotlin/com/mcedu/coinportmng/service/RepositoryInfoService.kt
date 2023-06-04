package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.dto.UpbitInfoSaveRequest
import com.mcedu.coinportmng.dto.UpbitInfoUpdateRequest
import com.mcedu.coinportmng.entity.AccessInfo
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.type.CoinRepositoryType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

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

    @Transactional
    fun updateAccessInfo(request: UpbitInfoUpdateRequest, upbit: CoinRepositoryType): String {
        val accessInfo = accessInfoRepository.findByIdOrNull(request.seq) ?: throw RuntimeException("존재하지 않는 정보입니다.")

        val name = request.name
        var hasUpdate = false
        if (!name.isNullOrBlank()) {
            accessInfo.name = name
            hasUpdate = true
        }


        val triple = validateKeyUpdate(request)
        var addMessage = ""
        if (triple != null) {
            val (accessKey, secretKey, expireDateTime) = triple
            hasUpdate = true
            accessInfo.accessKey = accessKey
            accessInfo.secretKey = secretKey
            accessInfo.expireDateTime = expireDateTime
            if (expireDateTime < LocalDateTime.now().plusMonths(1)) {
                addMessage = "(만료기간이 1달이 남지않은 키입니다.)"
            }
        }
        if (!hasUpdate) {
            throw RuntimeException("업데이트할 요청이 존재하지 않습니다.")
        }
        return "정보가 업데이트 되었습니다. $addMessage"
    }

    private fun validateKeyUpdate(request: UpbitInfoUpdateRequest): Triple<String, String, LocalDateTime>? {
        val accessKey = request.accessKey
        val secretKey = request.secretKey
        val expireDateTime = request.expireDateTime
        if (listOfNotNull(accessKey, secretKey, expireDateTime).isEmpty()) {
            return null
        }
        if (accessKey == null) {
            throw RuntimeException("access key 정보가 존재하지 않습니다.")
        }
        if (secretKey == null) {
            throw RuntimeException("secret key 정보가 존재하지 않습니다.")
        }
        if (expireDateTime == null) {
            throw RuntimeException("만료기간이 입력되지 않았습니다.")
        } else if (expireDateTime < LocalDateTime.now()) {
            throw RuntimeException("만료기간이 현재일 이전입니다.")
        }

        return Triple(accessKey, secretKey, expireDateTime)
    }
}