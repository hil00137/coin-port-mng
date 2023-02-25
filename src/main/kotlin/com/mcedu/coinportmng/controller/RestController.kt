package com.mcedu.coinportmng.controller

import com.fasterxml.jackson.annotation.JsonFormat
import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.service.RepositoryInfoService
import com.mcedu.coinportmng.type.CoinRepositoryType
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api")
class RestController(
    private val repositoryInfoService: RepositoryInfoService
) {

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/upbit/access-info")
    fun getAccessInfos(): List<AccessInfoDto> {
        return repositoryInfoService.getRepositoryInfos()
    }

    @PostMapping("/v1/upbit/access-info")
    fun saveAccessInfo(@RequestBody request: Request): Long {
        log.info("{}", request)
        return repositoryInfoService.saveAccessInfo(request, CoinRepositoryType.UPBIT)
    }
}

data class Request(
    val name: String,
    val accessKey: String,
    val secretKey: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val expireDateTime: LocalDateTime
)