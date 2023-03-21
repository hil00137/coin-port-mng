package com.mcedu.coinportmng.config

import com.mcedu.coinportmng.common.ResponseDto
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@RestControllerAdvice
class ResponseAdvice: ResponseBodyAdvice<Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val unSupportedTypes = setOf(ResponseEntity::class.java, ResponseDto::class.java)
    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return !unSupportedTypes.contains(returnType.parameterType)
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        return ResponseDto(data = body)
    }

    @ExceptionHandler(Exception::class)
    fun exceptionHandler(exception: Exception): ResponseDto<Nothing> {
        log.info(exception.message)
        return ResponseDto.fail(exception.message ?: "")
    }
}