package com.mcedu.coinportmng.dto

import com.mcedu.coinportmng.entity.Portfolio

data class PortfolioDto(val ticker: String, val ratio: Double) {
    constructor(portfolio: Portfolio) : this(portfolio.ticker, portfolio.ratio)

}