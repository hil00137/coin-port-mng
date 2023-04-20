package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.dto.UpbitWalletInfo
import com.mcedu.coinportmng.extention.getInfoSeq
import com.mcedu.coinportmng.service.MarketService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class WalletInfoController(private val marketService: MarketService) {

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/get-wallet/{seq}")
    fun getMyWallet(@PathVariable seq: String?): List<UpbitWalletInfo> {
        return marketService.getMyAccounts(seq.getInfoSeq())
    }
}