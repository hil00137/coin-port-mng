package com.mcedu.coinportmng.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.gson.Gson
import com.mcedu.coinportmng.dto.UpbitCoinInfo
import com.mcedu.coinportmng.dto.UpbitCoinPriceDto
import com.mcedu.coinportmng.dto.UpbitOrderResponse
import com.mcedu.coinportmng.dto.UpbitWalletInfo
import com.mcedu.coinportmng.repository.AccessInfoRepository
import com.mcedu.coinportmng.scheduler.Command
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.*
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
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
    fun getMyAccounts(seq: Long): List<UpbitWalletInfo> {
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
            object :ParameterizedTypeReference<List<UpbitWalletInfo>>() {}
        )

        return exchange.body ?: emptyList()
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

    fun getCurrentPrice(market: HashSet<String>): List<UpbitCoinPriceDto> {
        if (market.isEmpty()) {
            return emptyList()
        }
        val markets = market.joinToString(separator = ",")
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val uri =
            UriComponentsBuilder.fromUriString("$apiUrl/v1/ticker").queryParam("markets", markets).build().toUri()
        val exchange = restTemplate.exchange(
            RequestEntity<String>(headers, HttpMethod.GET, uri),
            object : ParameterizedTypeReference<List<UpbitCoinPriceDto>>() {}
        )
        return exchange.body?: emptyList()
    }

    @Transactional(readOnly = true)
    fun sell(infoSeq: Long, command: Command): UpbitOrderResponse? {
        val accessInfo = accessInfoRepository.findByIdOrNull(infoSeq) ?: throw RuntimeException("존재하지 않는 저장소 정보입니다.")
        val accessKey = accessInfo.accessKey
        val secretKey = accessInfo.secretKey

        val params = HashMap<String, String>()
        params["market"] = "KRW-${command.ticker}"
        params["side"] = "ask"
        params["volume"] = "${command.price}"
        params["ord_type"] = "market"

        val queryElements = ArrayList<String>()
        for ((key, value) in params) {
            queryElements.add("$key=$value")
        }

        val queryString = queryElements.joinToString("&")

        val md: MessageDigest = MessageDigest.getInstance("SHA-512")
        md.update(queryString.toByteArray(charset("UTF-8")))

        val queryHash = String.format("%0128x", BigInteger(1, md.digest()))

        val algorithm = Algorithm.HMAC256(secretKey)
        val jwtToken = JWT.create()
            .withClaim("access_key", accessKey)
            .withClaim("nonce", UUID.randomUUID().toString())
            .withClaim("query_hash", queryHash)
            .withClaim("query_hash_alg", "SHA512")
            .sign(algorithm)



        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBearerAuth(jwtToken)
        val exchange = restTemplate.exchange(
            RequestEntity<String>(Gson().toJson(params), headers, HttpMethod.POST, URI("$apiUrl/v1/orders")),
            UpbitOrderResponse::class.java
        )
        return exchange.body
    }
}