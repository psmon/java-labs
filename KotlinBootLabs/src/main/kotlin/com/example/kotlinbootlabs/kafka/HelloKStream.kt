package com.example.kotlinbootlabs.kafka

import com.example.kotlinbootlabs.kactor.HelloKTableState
import com.example.kotlinbootlabs.kactor.HelloKTableStateDeserializer
import com.example.kotlinbootlabs.kactor.HelloKTableStateSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.Properties

fun createHelloKStreams(): KafkaStreams {
    val props = Properties().apply {
        put("bootstrap.servers", "localhost:9092")
        put("application.id", "hello-ktable-actor")
        put("default.key.serde", Serdes.String().javaClass.name)
        put("default.value.serde", Serdes.String().javaClass.name)
    }

    val builder = StreamsBuilder()
    val storeBuilder = Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore("hello-state-store"),
        Serdes.String(),
        Serdes.serdeFrom(HelloKTableStateSerializer(), HelloKTableStateDeserializer())
    )
    builder.addStateStore(storeBuilder)
    val table: KTable<String, HelloKTableState> = builder.table("hello-state-store")

    val streams = KafkaStreams(builder.build(), props)

    streams.setUncaughtExceptionHandler { exception ->
        // Log the exception
        println("Uncaught exception in Kafka Streams: ${exception.message}")
        // Decide on the action: SHUTDOWN_CLIENT, REPLACE_THREAD, or SHUTDOWN_APPLICATION
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT
    }

    return streams
}

fun createKafkaProducer(): KafkaProducer<String, HelloKTableState> {
    val props = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, HelloKTableStateSerializer::class.java.name)
    }
    return KafkaProducer(props)
}

fun <K, V> getStateStoreWithRetries(
    streams: KafkaStreams,
    storeName: String,
    maxRetries: Int = 10,
    retryIntervalMs: Long = 1000
): ReadOnlyKeyValueStore<K, V> {
    var retries = 0
    while (retries < maxRetries) {
        try {
            return streams.store(
                StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore<K, V>())
            )
        } catch (e: InvalidStateStoreException) {
            println("State store is not yet ready, waiting...")
            retries++
            Thread.sleep(retryIntervalMs)
        }
    }
    throw IllegalStateException("Could not get state store after $maxRetries retries")
}