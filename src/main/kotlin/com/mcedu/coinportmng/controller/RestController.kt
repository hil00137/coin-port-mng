package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.service.RepositoryInfoService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class RestController(
    private val repositoryInfoService: RepositoryInfoService
) {

    @GetMapping("/v1/upbit/access-info")
    fun getAccessInfos(): List<AccessInfoDto> {
        return repositoryInfoService.getRepositoryInfos()
    }
}