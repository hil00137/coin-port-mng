package com.mcedu.coinportmng.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mcedu.coinportmng.extention.getInfoSeq
import com.mcedu.coinportmng.service.UpbitService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class WalletInfoController(private val upbitService: UpbitService) {

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/get-wallet/{seq}")
    fun getMyWallet(@PathVariable seq: String?): JsonNode {
        return ObjectMapper().readTree(upbitService.getMyAccounts(seq.getInfoSeq()))
    }
}