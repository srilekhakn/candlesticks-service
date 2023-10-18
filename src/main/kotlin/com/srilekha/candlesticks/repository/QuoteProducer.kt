package com.srilekha.candlesticks.repository

import com.srilekha.candlesticks.model.Quote
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Repository

@Repository
class QuoteProducer(val quoteProducer: KafkaTemplate<String, Quote>) {
    fun send(message: Quote) {
        quoteProducer.send(STOCK_QUOTES_TOPIC, message.isin, message)
    }
}