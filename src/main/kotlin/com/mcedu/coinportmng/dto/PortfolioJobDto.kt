package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.common.ReblanceJobStatus

data class PortfolioJobDto(
    var jobSeq: Long? = null,
    var infoSeq: Long? = null,
    var jobStatus: ReblanceJobStatus = ReblanceJobStatus.NONE,
    var portfolios: Map<String, PortfolioDto> = emptyMap()
) {
    fun reset() {
        this.jobSeq = null
        this.infoSeq = null
        this.jobStatus = ReblanceJobStatus.NONE
        this.portfolios = emptyMap()
    }
}