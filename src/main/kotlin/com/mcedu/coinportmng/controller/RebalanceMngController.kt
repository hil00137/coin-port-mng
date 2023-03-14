package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.common.ResponseDto
import com.mcedu.coinportmng.dto.RebalancePlanDto
import com.mcedu.coinportmng.service.RebalanceMngService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class RebalanceMngController(
    private val rebalanceMngService: RebalanceMngService
){

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/reblance-info")
    fun getRebalanceMng(): RebalancePlanDto? {
        return rebalanceMngService.getRebalanceMng()
    }

    @PostMapping("/v1/rebalance-info")
    fun upsertRebalanceMng(@RequestBody planDto: RebalancePlanDto): ResponseDto<Nothing> {
        rebalanceMngService.upsertRebalnaceMng(planDto)
        return ResponseDto.success("저장하였습니다.")
    }
}