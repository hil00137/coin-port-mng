package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.service.SlackService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class HelloScheduler(
    private val slackService: SlackService
) {
    private var marketStatus = true

    @Scheduled(cron = "0 0/10 * * * *")
    fun hello() {
        val message = if (marketStatus) {
            "UP"
        } else {
            "DOWN"
        }

        slackService.sendMessage(title = "health-check" , message = message)
    }

    fun market(isUp: Boolean) {
        marketStatus = isUp
    }

}