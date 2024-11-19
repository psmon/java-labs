package com.example.kotlinbootlabs.kafka

import com.example.kotlinbootlabs.kactor.HelloKTableState
import com.example.kotlinbootlabs.kactor.HelloKTableStateDeserializer
import com.example.kotlinbootlabs.kactor.HelloKTableStateSerializer
import com.example.kotlinbootlabs.service.RedisService
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.apache.kafka.streams.state.Stores
import java.util.*

data class HelloKStreamsResult(
    val streams: KafkaStreams,
    val helloKTable: KTable<String, HelloKTableState>
)

fun createHelloKStreams( redisService: RedisService): HelloKStreamsResult {
    val props = Properties().apply {
        put("bootstrap.servers", "localhost:9092,localhost:9003,localhost:9004")
        put("application.id", "unique-hello-ktable-actor")
        put("default.key.serde", Serdes.String().javaClass.name)
        put("default.value.serde", Serdes.String().javaClass.name)
        put("processing.guarantee", "exactly_once") // Ensure exactly-once processing
        put("session.timeout.ms", "30000") // Increase session timeout
        put("heartbeat.interval.ms", "10000") // Adjust heartbeat interval
        put("group.instance.id", "unique-instance-id") // Enable static membership
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, HelloKTableStateSerializer::class.java.name)
        put(ProducerConfig.ACKS_CONFIG, "all") // Ensure all replicas acknowledge
        put(ProducerConfig.RETRIES_CONFIG, 3) // Retry up to 3 times
    }

    val builder = StreamsBuilder()
    val storeBuilder = Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore("hello-state-store"),
        Serdes.String(),
        Serdes.serdeFrom(HelloKTableStateSerializer(), HelloKTableStateDeserializer())
    )
    builder.addStateStore(storeBuilder)

    // Create a KTable from the hello-log-store topic
    val helloKTable = builder.table<String, HelloKTableState>(
        "hello-log-store",
        Consumed.with(Serdes.String(), Serdes.serdeFrom(HelloKTableStateSerializer(), HelloKTableStateDeserializer())),
        Materialized.with(
            Serdes.String(),
            Serdes.serdeFrom(HelloKTableStateSerializer(), HelloKTableStateDeserializer())
        )
    )

    // Sync the KTable to Redis
    syncHelloKTableToRedis(helloKTable, redisService)

    val streams = KafkaStreams(builder.build(), props)

    streams.setUncaughtExceptionHandler { exception ->
        // Log the exception
        println("Uncaught exception in Kafka Streams: ${exception.message}")
        // Decide on the action: SHUTDOWN_CLIENT, REPLACE_THREAD, or SHUTDOWN_APPLICATION
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT
    }

    return HelloKStreamsResult(streams, helloKTable)
}

fun createKafkaProducer(): KafkaProducer<String, HelloKTableState> {
    val props = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, HelloKTableStateSerializer::class.java.name)
    }
    return KafkaProducer(props)
}

fun syncHelloKTableToRedis(helloKTable: KTable<String, HelloKTableState>, redisService: RedisService) {
    helloKTable.toStream().foreach { key, value ->
        if (value != null) {
            println("Syncing $key to Redis - syncHelloKTableToRedis")
            redisService.setValue("hello-state-store", key, ObjectMapper().writeValueAsString(value)).subscribe()
        }
    }
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