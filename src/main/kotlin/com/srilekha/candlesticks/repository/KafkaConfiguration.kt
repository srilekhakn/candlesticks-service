package com.srilekha.candlesticks.repository

import com.srilekha.candlesticks.model.Candlestick
import com.srilekha.candlesticks.model.Quote
import com.srilekha.candlesticks.model.QuoteSerde
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.*
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer


@Configuration
@EnableKafka
@EnableKafkaStreams
class KafkaConfiguration {

    @Bean
    fun appTopics(): KafkaAdmin.NewTopics {
        return KafkaAdmin.NewTopics(
            TopicBuilder.name(STOCK_QUOTES_TOPIC).build(),
            TopicBuilder.name(CANDLE_TICKS_TOPIC).build()
            )
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, Quote> {
        val configProps: MutableMap<String, Any> = java.util.HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KAFKA_HOSTS
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun producerFactoryCandleStick(): ProducerFactory<String, Candlestick> {
        val configProps: MutableMap<String, Any> = java.util.HashMap()
        configProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KAFKA_HOSTS
        configProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        configProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Quote> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun kafkaTemplateCandleStick(): KafkaTemplate<String, Candlestick> {
        return KafkaTemplate(producerFactoryCandleStick())
    }

    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun defaultKafkaStreamsConfig(): KafkaStreamsConfiguration {
        val configProps: MutableMap<String, Any> = HashMap()
        configProps[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = KAFKA_HOSTS
        configProps[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            LogAndContinueExceptionHandler::class.java
        configProps[StreamsConfig.APPLICATION_ID_CONFIG] = "quote-stream"
        configProps[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass.name
        configProps[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = QuoteSerde::class.java.name
        configProps[ConsumerConfig.GROUP_ID_CONFIG] = "stock-quotes-stream-group"
        return KafkaStreamsConfiguration(configProps)
    }

}

// constants for topics and global configuration
const val STOCK_QUOTES_TOPIC = "stock-quotes-topic"
const val CANDLE_TICKS_TOPIC = "candle-ticks-topic"
val KAFKA_HOSTS: List<String> = listOf("localhost:9092")
const val QUOTES_BY_WINDOW_TABLE = "quotes-by-window-table"
const val WINDOWED_CANDLE_PRICE_SUPPRESS_NODE_NAME = "window-candle-price"
const val CANDLE_PRICE_TABLE = "candle-price-table"

