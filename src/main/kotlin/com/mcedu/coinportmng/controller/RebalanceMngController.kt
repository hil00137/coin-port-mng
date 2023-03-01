package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.common.ResponseDto
import com.mcedu.coinportmng.dto.RebalancePlanDto
import com.mcedu.coinportmng.extention.getInfoSeq
import com.mcedu.coinportmng.service.RebalanceMngService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
        return rebalanceMngService.getRebalanceMng(seq.getInfoSeq())
    }

    @PostMapping("/v1/rebalance-info/{seq}")
    fun upsertRebalanceMng(@PathVariable seq: String?, @RequestBody planDto: RebalancePlanDto): ResponseDto<Nothing> {
        rebalanceMngService.upsertRebalnaceMng(seq.getInfoSeq(), planDto)
        return ResponseDto.success("저장하였습니다.")
    }
}