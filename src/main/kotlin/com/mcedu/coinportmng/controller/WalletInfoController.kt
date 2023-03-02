package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.dto.UpbitWalletInfo
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
    fun getMyWallet(@PathVariable seq: String?): List<UpbitWalletInfo> {
        return upbitService.getMyAccounts(seq.getInfoSeq())
    }
}