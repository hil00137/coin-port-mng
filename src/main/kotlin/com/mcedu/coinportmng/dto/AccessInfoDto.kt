package com.mcedu.coinportmng.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.mcedu.coinportmng.entity.AccessInfo
import java.time.LocalDateTime

data class AccessInfoDto(
    val seq: Long,
    val name: String,
    val accessKey: String,
    val secretKey: String,
    val repositoryType: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    val createdDate: LocalDateTime
) {
    companion object {
        fun fromEntity(accessInfo: AccessInfo): AccessInfoDto {
            return AccessInfoDto(
                seq = accessInfo.seq?:0,
                name = accessInfo.name,
                accessKey =  accessInfo.accessKey,
                secretKey = accessInfo.secretKey,
                repositoryType = accessInfo.repositoryType.krNm,
                createdDate = accessInfo.createdDate
            )
        }
    }
}
