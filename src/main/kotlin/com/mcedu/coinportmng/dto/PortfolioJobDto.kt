package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.common.ReblanceJobStatus

data class PortfolioJobDto(
    var jobSeq: Long? = null,
    var infoSeq: Long? = null,
    var jobStatus: ReblanceJobStatus = ReblanceJobStatus.NONE,
    var portfolios: Map<String, PortfolioDto> = emptyMap(),
    var command: Command = Command(),
    var response: UpbitOrderResponse? = null
) {
    fun reset() {
        this.jobSeq = null
        this.infoSeq = null
        this.jobStatus = ReblanceJobStatus.NONE
        this.portfolios = emptyMap()
        this.command = Command()
        this.response = null
    }
}