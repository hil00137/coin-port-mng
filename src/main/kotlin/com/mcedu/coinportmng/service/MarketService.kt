package com.mcedu.coinportmng.service

import com.mcedu.coinportmng.dto.UpbitWalletInfo
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.scheduler.HelloScheduler
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarketService(
    private val accessInfoRepository: AccessInfoRepository,
    private val upbitService: UpbitService,
    private val helloScheduler: HelloScheduler
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private fun <T> withHealthCheck(block: () -> T): T {
        return try {
            val result = block()
            helloScheduler.market(true)
            result
        } catch (ex: Exception) {
            helloScheduler.market(false)
            log.error("", ex)
            throw ex
        }
    }

    @Transactional(readOnly = true)
    fun getMyAccounts(seq: Long): List<UpbitWalletInfo> {
        val accessInfo = accessInfoRepository.findByIdOrNull(seq) ?: throw RuntimeException("존재하지 않는 저장소 정보입니다.")
        return this.withHealthCheck {
            this.upbitService.getMyAccounts(accessInfo)
        }
    }
}