package com.mcedu.coinportmng

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
class CoinPortMngApplication

fun main(args: Array<String>) {
    runApplication<CoinPortMngApplication>(*args)
}
