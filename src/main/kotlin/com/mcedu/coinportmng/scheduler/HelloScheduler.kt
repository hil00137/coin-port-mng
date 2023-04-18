package com.mcedu.coinportmng.scheduler

import com.mcedu.coinportmng.service.SlackService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class HelloScheduler(
    private val slackService: SlackService
) {

    @Scheduled(cron = "0 0/10 * * * *")
    fun hello() {
        slackService.sendMessage(title = "health-check" , message = "UP")
    }

}