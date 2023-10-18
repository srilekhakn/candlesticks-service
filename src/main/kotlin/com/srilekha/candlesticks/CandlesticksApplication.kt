package com.srilekha.candlesticks

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.srilekha.candlesticks.service.SaveEvents
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class CandlesticksApplication

fun main(args: Array<String>) {
	runApplication<CandlesticksApplication>(*args)
}

val jackson: ObjectMapper =
	jacksonObjectMapper()
		.registerModule(JavaTimeModule())
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
