package com.mcedu.coinportmng.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mcedu.coinportmng.repository.AccessInfoRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.*

@Service
class UpbitService(
    private val accessInfoRepository: AccessInfoRepository
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${url.upbit}")
    private lateinit var apiUrl: String
    private val restTemplate: RestTemplate = RestTemplate()

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
}