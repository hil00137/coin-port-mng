package com.mcedu.coinportmng.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mcedu.coinportmng.dto.UpbitCoinInfo
import com.mcedu.coinportmng.repository.AccessInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriBuilderFactory
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.util.*

@Service
class UpbitService(
    private val accessInfoRepository: AccessInfoRepository
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${url.upbit}")
    private lateinit var apiUrl: String
    private val restTemplate: RestTemplate = RestTemplate().also {
        it.messageConverters.add(MappingJackson2HttpMessageConverter().apply {
            this.objectMapper = ObjectMapper().apply {
                this.registerKotlinModule()
            }
        })
    }

    @Transactional(readOnly = true)
    fun getMyAccounts(seq: Long): String {
        val accessInfo = accessInfoRepository.findByIdOrNull(seq) ?: throw RuntimeException("존재하지 않는 저장소 정보입니다.")
        val algorithm = Algorithm.HMAC256(accessInfo.secretKey)
        val jwtToken = JWT.create()
            .withClaim("access_key", accessInfo.accessKey)
            .withClaim("nonce", UUID.randomUUID().toString())
            .sign(algorithm)


        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(jwtToken)

        val exchange = restTemplate.exchange(
            RequestEntity<String>(headers, HttpMethod.GET, URI("$apiUrl/v1/accounts")),
            String::class.java
        )

        return exchange.body ?: ""
    }

    fun getMarketAll(): List<UpbitCoinInfo> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val uri =
            UriComponentsBuilder.fromUriString("$apiUrl/v1/market/all").queryParam("isDetails", true).build().toUri()
        val exchange = restTemplate.exchange(
            RequestEntity<String>(headers, HttpMethod.GET, uri),
            object : ParameterizedTypeReference<List<UpbitCoinInfo>>() {}
        )
        return exchange.body ?: emptyList()
    }
}