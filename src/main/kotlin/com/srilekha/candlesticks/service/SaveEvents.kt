package com.srilekha.candlesticks.service

import com.srilekha.candlesticks.model.InstrumentEvent
import com.srilekha.candlesticks.model.QuoteEvent
import com.srilekha.candlesticks.repository.QuoteProducer
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.stereotype.Service

@Service
@EnableKafka
class SaveEvents(var quoteProducer: QuoteProducer) {

    fun saveEventsToKafkaTopic(quoteEvent: QuoteEvent) {
        quoteProducer.send(quoteEvent.data)
    }

    fun saveInstrumentToDatabase(data: InstrumentEvent) {

    }
}