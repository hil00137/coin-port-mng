package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.common.ResponseDto
import com.mcedu.coinportmng.dto.AccessInfoDto
import com.mcedu.coinportmng.dto.UpbitInfoSaveRequest
import com.mcedu.coinportmng.service.RepositoryInfoService
import com.mcedu.coinportmng.type.CoinRepositoryType
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import java.lang.NumberFormatException
import java.lang.RuntimeException

@RestController
@RequestMapping("/api")
class UpbitInfoController(
    private val repositoryInfoService: RepositoryInfoService
) {

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/upbit/access-info")
    fun getAccessInfos(): List<AccessInfoDto> {
        return repositoryInfoService.getRepositoryInfos()
    }

    @PostMapping("/v1/upbit/access-info")
    fun saveAccessInfo(@RequestBody request: UpbitInfoSaveRequest): Long {
        log.info("{}", request)
        return repositoryInfoService.saveAccessInfo(request, CoinRepositoryType.UPBIT)
    }

    @DeleteMapping("/v1/upbit/access-info/{seq}")
    fun deleteAccessInfo(@PathVariable seq: String?): ResponseDto<Nothing> {
        val infoSeq = try {
            seq?.toLong() ?: throw NumberFormatException()
        } catch (ex: NumberFormatException) {
            log.error("", ex)
            throw RuntimeException("잘못된 번호입니다.")
        }
        repositoryInfoService.deleteAccessInfo(infoSeq)
        return ResponseDto.success("삭제에 성공하였습니다.")
    }
}