package com.srilekha.candlesticks.repository

import com.srilekha.candlesticks.model.Candlestick
import com.srilekha.candlesticks.model.CandlestickManager
import com.srilekha.candlesticks.model.Quote
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.state.*
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerde
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.Instant


@Repository
class CandlestickManagerStick(val candlestickProducer: KafkaTemplate<String, Candlestick>): CandlestickManager {
    private lateinit var candlesticks: ReadOnlyWindowStore<String, Candlestick>

    @Bean
    fun afterCandleStickGeneration(sbfb: StreamsBuilderFactoryBean): StreamsBuilderFactoryBean.Listener {
        val listener: StreamsBuilderFactoryBean.Listener = object : StreamsBuilderFactoryBean.Listener {
            override fun streamsAdded(id: String, streams: KafkaStreams) {
                candlesticks =streams
                    .store(
                        StoreQueryParameters
                            .fromNameAndType(CANDLE_PRICE_TABLE, QueryableStoreTypes.windowStore())
                    )
            }
        }
        sbfb.addListener(listener)
        return listener
    }


    @Bean
    fun quoteKStream(streamsBuilder: StreamsBuilder): KGroupedStream<String, Quote> {
        val jsonSerde: JsonSerde<Candlestick> = JsonSerde(Candlestick::class.java)

        val stream: KStream<String, Quote> = streamsBuilder.stream(STOCK_QUOTES_TOPIC)
        // group by key
        val groupedBySymbol: KGroupedStream<String, Quote> = stream.groupByKey()

        // we could transform it materialize it to be able to query directly from kafka
         groupedBySymbol
            .windowedBy(SlidingWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(60)))
            .aggregate(
                this::initialize,
                this::candlestick,
                Materialized.`as`<String, Candlestick, WindowStore<Bytes, ByteArray>>(QUOTES_BY_WINDOW_TABLE)
                    .withValueSerde(jsonSerde)
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()).withName(WINDOWED_CANDLE_PRICE_SUPPRESS_NODE_NAME))
             .toStream().map { key, value ->
                 candlestickProducer.send(CANDLE_TICKS_TOPIC,key.key(),value)
                 KeyValue(key,value)
             }
        return groupedBySymbol
    }


    @Bean
    fun candleSticksKStream(streamsBuilder: StreamsBuilder): KStream<String, Candlestick> {
        val jsonSerde: JsonSerde<Candlestick> = JsonSerde(Candlestick::class.java)

        // we could transform it materialize it to be able to query directly from kafka
        val stream: KStream<String, Candlestick> = streamsBuilder.stream(CANDLE_TICKS_TOPIC)
        stream.groupByKey()
            .windowedBy(SlidingWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(60)))
            .aggregate(this::initialize,this::candlesticksAggregator,Materialized.`as`<String, Candlestick, WindowStore<Bytes, ByteArray>>(CANDLE_PRICE_TABLE)
                .withValueSerde(jsonSerde)
                .withRetention(Duration.ofMinutes(30)))
        return stream
    }


    private fun initialize(): Candlestick {
        return Candlestick(Instant.now(),Instant.now(),null,null,null,null)
    }

    private fun candlestick(key: String, quote: Quote, aggregator: Candlestick): Candlestick {
        val currentPrice = quote.price
        // initialize open price
        if(aggregator.openPrice == null){
            aggregator.openPrice = currentPrice
        }
        // check for high price
        if (aggregator.highPrice == null){
            aggregator.highPrice = quote.price
        }else if(aggregator.highPrice!! < currentPrice){
            aggregator.highPrice = currentPrice
        }
        // check for low price
        if (aggregator.lowPrice == null){
            aggregator.lowPrice = quote.price
        }else if(aggregator.lowPrice!! > currentPrice){
            aggregator.lowPrice = currentPrice
        }
        aggregator.closingPrice = quote.price
        return aggregator
    }

    private fun candlesticksAggregator(key: String, candlestick: Candlestick, aggregator: Candlestick): Candlestick {
        if(candlestick.openPrice == null){
            candlestick.openPrice = aggregator.openPrice
        }
        if(candlestick.highPrice == null){
            candlestick.highPrice = aggregator.highPrice
        }
        if(candlestick.lowPrice == null){
            candlestick.lowPrice = aggregator.lowPrice
        }
        if(candlestick.closingPrice == null){
            candlestick.closeTimestamp = aggregator.closeTimestamp
        }
        return candlestick
    }

    override fun getCandlesticks(isin: String): List<Candlestick> {

        val candlestickList = ArrayList<Candlestick>()
        candlesticks.fetch(isin, Instant.now().minusSeconds(60 * 30), Instant.now())?.forEach {
            candlestickList.add(it.value)
        }
        return candlestickList
    }
}
