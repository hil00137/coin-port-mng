package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.common.ReblanceJobStatus

data class PortfolioJobDto(
    var jobSeq: Long?,
    var jobStatus: ReblanceJobStatus = ReblanceJobStatus.NONE,
    var portfolios: Map<String, PortfolioDto> = emptyMap()
)