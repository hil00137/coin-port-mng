package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.common.ResponseDto
import com.mcedu.coinportmng.dto.PortfolioDto
import com.mcedu.coinportmng.extention.getInfoSeq
import com.mcedu.coinportmng.service.PortfolioService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api")
class CoinPortfolioController(
    private val portfolioService: PortfolioService
) {

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/portfolio")
    fun getPortfolio(): List<PortfolioDto> {
        return portfolioService.getPortfolios()
    }

    @PostMapping("/v1/portfolio/{seq}")
    fun savePortfolio(@PathVariable seq: String?, @RequestBody list: List<PortfolioDto>): ResponseDto<Nothing> {
        portfolioService.savePortfolios(seq.getInfoSeq(), list)
        return ResponseDto.success("포트폴리오가 저장되었습니다.")
    }
}