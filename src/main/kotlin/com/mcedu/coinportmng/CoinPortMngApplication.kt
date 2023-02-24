package com.mcedu.coinportmng

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class CoinPortMngApplication

fun main(args: Array<String>) {
    runApplication<CoinPortMngApplication>(*args)
}
