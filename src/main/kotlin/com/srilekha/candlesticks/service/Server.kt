package com.srilekha.candlesticks.service

import com.srilekha.candlesticks.model.CandlestickManager
import com.srilekha.candlesticks.jackson
import jakarta.annotation.PostConstruct
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class Server {

  @Autowired
  lateinit var  service : CandlestickManager

  private val routes = routes(
    "candlesticks" bind Method.GET to { getCandlesticks(it) }
  )

  private val server: Http4kServer = routes.asServer(Netty(9000))

  @PostConstruct
  fun start() {
    server.start()
  }

  private fun getCandlesticks(req: Request): Response {
    val isin = req.query("isin")
      ?: return Response(Status.BAD_REQUEST).body("{'reason': 'missing_isin'}")

    val body = jackson.writeValueAsBytes(service.getCandlesticks(isin))

    return Response(Status.OK).body(body.inputStream())
  }
}
