package com.quera.imageservice.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
@EnableWebFlux
class WebTestConfig {
    @Bean
    fun webClient(): WebClient = WebClient.builder().build()
} 