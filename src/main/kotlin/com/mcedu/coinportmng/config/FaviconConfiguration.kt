package com.mcedu.coinportmng.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler

@Configuration
class FaviconConfiguration {

    @Bean
    fun faviconHandlerMapping(faviconRequestHandler: ResourceHttpRequestHandler): SimpleUrlHandlerMapping {
        return SimpleUrlHandlerMapping().also {
            it.order = Int.MIN_VALUE
            it.urlMap = mapOf(Pair("/favicon.ico", faviconRequestHandler))
        }
    }

    @Bean
    fun faviconRequestHandler(): ResourceHttpRequestHandler {
        return ResourceHttpRequestHandler().also {
            it.locations.add(ClassPathResource("static/"))
        }
    }
}