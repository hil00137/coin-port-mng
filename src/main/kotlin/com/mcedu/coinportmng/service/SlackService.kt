package com.mcedu.coinportmng.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Service
class SlackService {
    private val slackUrl: String = "https://hooks.slack.com/services/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"
    private val restTemplate: RestTemplate = RestTemplate()

    private val log = LoggerFactory.getLogger(this::class.java)
    fun sendMessage(title: String = "", message: String = "") {
        val resultMessage = listOf(title, message).filter { it.isNotEmpty() }.joinToString("\n")
        val exchange = restTemplate.exchange<String>(slackUrl, HttpMethod.POST, HttpEntity(mapOf("text" to resultMessage)))
        log.info("send $title : ${exchange.body}")
    }
}