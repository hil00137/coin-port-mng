package com.mcedu.coinportmng.entity

import com.mcedu.coinportmng.type.IsYN
import jakarta.persistence.*
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import java.time.LocalDateTime

@Entity
@Table
@DynamicUpdate
@DynamicInsert
data class Coin(
    @Id
    @Column(length = 10)
    var ticker: String,
    var englishName: String,
    var koreanName: String,
    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    var krwMarket: IsYN = IsYN.N,
    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    var btcMarket: IsYN = IsYN.N,
    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    var usdtMarket: IsYN = IsYN.N,
    var lastUpdateTime: LocalDateTime = LocalDateTime.now()
) {
    fun update(market: String) {
        when(market) {
            "KRW" -> this.krwMarket = IsYN.Y
            "BTC" -> this.btcMarket = IsYN.Y
            "USDT" -> this.usdtMarket = IsYN.Y
        }
        lastUpdateTime = LocalDateTime.now()
    }
}