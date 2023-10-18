package com.srilekha.candlesticks.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.srilekha.candlesticks.jackson
import com.srilekha.candlesticks.model.InstrumentEvent
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
class InstrumentStream {
  private val logger = LoggerFactory.getLogger(javaClass)

  private val uri = Uri.of("ws://localhost:8032/instruments")

  private lateinit var ws: Websocket

  @Autowired
  private lateinit var saveEvents: SaveEvents

  @PostConstruct
  fun connect() {
    ws = WebsocketClient.nonBlocking(uri) { logger.info("Connected instrument stream")}

    ws.onMessage {
      val event = jackson.readValue<InstrumentEvent>(it.body.stream)
      logger.info("Instrument Stream Event: ${event.data.isin}")
      saveEvents.saveInstrumentToDatabase(event)
    }

    ws.onClose {
      logger.info("Disconnected instrument stream: ${it.code}; ${it.description}")
      runBlocking {
        launch {
          delay(5000L)
          logger.info("Attempting reconnect for instrument stream")
          connect()
        }
      }
    }

    ws.onError {
      logger.info("Exception in instrument stream: $it")
    }
  }
}