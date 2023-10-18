package com.srilekha.candlesticks.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.srilekha.candlesticks.jackson
import com.srilekha.candlesticks.model.QuoteEvent
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.websocket.Websocket
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QuoteStream {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val wsURI = Uri.of("ws://localhost:8032/quotes")

  private lateinit var ws: Websocket

  @Autowired
  private lateinit var saveEvents: SaveEvents

  @PostConstruct
  fun connect() {
    ws = WebsocketClient.nonBlocking(wsURI) { logger.info("Connected quote stream") }

    ws.onMessage {
      val event = jackson.readValue<QuoteEvent>(it.body.stream)
      logger.info("Quote Stream Event: ${event.data.isin}; ${event.data.price}")
      saveEvents.saveEventsToKafkaTopic(event)
    }

    ws.onClose {
      logger.info("Disconnected quote stream: ${it.code}; ${it.description}")
      runBlocking {
        launch {
          delay(5000L)
          logger.info("Attempting reconnect for quote stream")
          connect()
        }
      }
    }

    ws.onError {
      logger.info("Exception in quote stream: $it")
    }
  }
}