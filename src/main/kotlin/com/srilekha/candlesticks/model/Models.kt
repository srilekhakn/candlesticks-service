package com.srilekha.candlesticks.model

import org.apache.kafka.common.serialization.Serdes.WrapperSerde
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import java.time.Instant


data class InstrumentEvent(val type: Type, val data: Instrument) {
  enum class Type {
    ADD,
    DELETE
  }
}

data class QuoteEvent(val data: Quote)

data class Instrument(val isin: ISIN, val description: String)
typealias ISIN = String

data class Quote(val isin: ISIN, val price: Price)
typealias Price = Double


interface CandlestickManager {
  fun getCandlesticks(isin: String): List<Candlestick>
}

data class Candlestick(
    val openTimestamp: Instant,
    var closeTimestamp: Instant,
    var openPrice: Price?,
    var highPrice: Price?,
    var lowPrice: Price?,
    var closingPrice: Price?
)

data class CandlestickPerMinute(
    var openPrice: Price?,
    var highPrice: Price?,
    var lowPrice: Price?,
    var closingPrice: Price?
)


class QuoteSerde : WrapperSerde<Quote>(JsonSerializer(), JsonDeserializer(Quote::class.java))