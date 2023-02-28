package com.mcedu.coinportmng.controller

import com.mcedu.coinportmng.common.ResponseDto
import com.mcedu.coinportmng.dto.PortfolioDto
import com.mcedu.coinportmng.service.PortfolioService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.NumberFormatException
import java.lang.RuntimeException


@RestController
@RequestMapping("/api")
class CoinPortfolioController(
    private val portfolioService: PortfolioService
) {

    private val log = LoggerFactory.getLogger(this::class.java)
    @GetMapping("/v1/portfolio/{seq}")
    fun getPortfolio(@PathVariable seq: String?): List<PortfolioDto> {
        val infoSeq = try {
            seq?.toLong() ?: throw NumberFormatException()
        } catch (ex: NumberFormatException) {
            log.error("", ex)
            throw RuntimeException("잘못된 번호입니다.")
        }
        return portfolioService.getPortfolios(infoSeq)
    }

    @PostMapping("/v1/portfolio/{seq}")
    fun savePortfolio(@PathVariable seq: String?, @RequestBody list: List<PortfolioDto>): ResponseDto<Nothing> {
        val infoSeq = try {
            seq?.toLong() ?: throw NumberFormatException()
        } catch (ex: NumberFormatException) {
            log.error("", ex)
            throw RuntimeException("잘못된 번호입니다.")
        }
        portfolioService.savePortfolios(infoSeq, list)
        return ResponseDto.success("포트폴리오가 저장되었습니다.")
    }
}