package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.service.RepositoryInfoService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    fun saveAccessInfo(@RequestBody request: Request) {
        log.info("{}", request)
    }
}

data class Request(
    val accessKey: String,
    val secretKey: String,
)