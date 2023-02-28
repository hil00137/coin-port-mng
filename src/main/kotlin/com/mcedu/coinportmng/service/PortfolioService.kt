package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.PortfolioDto
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.repository.PortfolioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.RuntimeException

@Service
class PortfolioService(
    private val accessInfoRepository: AccessInfoRepository,
    private val portfolioRepository: PortfolioRepository
) {

    @Transactional(readOnly = true)
    fun getPortfolios(accessInfoSeq: Long): List<PortfolioDto> {
        val accessInfo =
            accessInfoRepository.findById(accessInfoSeq).orElseThrow { RuntimeException("존재하지 않는 저장소 정보입니다.") }
        val infos = portfolioRepository.findAllByAccessInfo(accessInfo)

        val dtos = infos.map { PortfolioDto(it) }.toMutableList()
        if (dtos.isEmpty()) {
            dtos.add(PortfolioDto("KRW", 1.0))
        }
        return dtos
    }
}