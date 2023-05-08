package com.mcedu.coinportmng.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Service
class SlackService {

    @Value("\${slack.send}")
    private lateinit var sendSlackMessage: String
    private val slackUrl: String = "https://hooks.slack.com/services/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"
    private val restTemplate: RestTemplate = RestTemplate()

    private val log = LoggerFactory.getLogger(this::class.java)
    fun sendMessage(title: String = "", message: String = "", channel: String? = null) {
        val resultMessage = listOf(title, message).filter { it.isNotEmpty() }.joinToString("\n")
        if (sendSlackMessage == "true") {
            val body = hashMapOf("text" to resultMessage)
            if (channel != null) {
                body["channel"] = "#$channel"
            }
            val exchange = restTemplate.exchange<String>(slackUrl, HttpMethod.POST, HttpEntity(body))
            log.info("send $title : ${exchange.body}")
        } else {
            log.info("send $title : $message")
        }
    }
}