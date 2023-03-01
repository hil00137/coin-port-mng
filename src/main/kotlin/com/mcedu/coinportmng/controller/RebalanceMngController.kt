package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.dto.RebalancePlanDto
import com.mcedu.coinportmng.service.RebalanceMngService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class RebalanceMngController(
    private val rebalanceMngService: RebalanceMngService
){

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/reblance-info/{seq}")
    fun getRebalanceMng(@PathVariable seq: String?): RebalancePlanDto? {
        val infoSeq = try {
            seq?.toLong() ?: throw NumberFormatException()
        } catch (ex: NumberFormatException) {
            log.error("", ex)
            throw RuntimeException("잘못된 번호입니다.")
        }
        return rebalanceMngService.getRebalanceMng(infoSeq)
    }
}