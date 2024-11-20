package com.example.kotlinbootlabs.kafka

import com.example.kotlinbootlabs.kactor.HelloKState
import com.example.kotlinbootlabs.kactor.HelloKTableState
import com.example.kotlinbootlabs.service.RedisService
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExtendWith(SpringExtension::class)
@SpringBootTest
class HelloKStreamTest {

    private lateinit var producer: KafkaProducer<String, HelloKTableState>

    private lateinit var kstreams: HelloKStreamsResult

    @Autowired
    lateinit var redisService: RedisService

    @BeforeTest
    fun setUp() {

        producer = createKafkaProducer()

        kstreams = createHelloKStreams(redisService)

        val latch = CountDownLatch(1)

        kstreams.streams.setStateListener { newState, _ ->
            if (newState == KafkaStreams.State.RUNNING) {
                latch.countDown()
            }
        }

        kstreams.streams.start()

        latch.await()

        // Wait for the state store to be ready
        val stateStore: ReadOnlyKeyValueStore<String, HelloKTableState> = getStateStoreWithRetries(
            kstreams.streams, "hello-state-store"
        )

    }

    @AfterTest
    fun tearDown() {
        if (::producer.isInitialized) {
            producer.close(Duration.ofSeconds(3))
        }

        kstreams.streams.close()
    }

    @Test
    fun testHelloCommand() {

        var testPersistId = "testid-01"

        for(i in 1L..10L) {
            val curState = HelloKTableState(HelloKState.HAPPY, i, i * 10)
            producer.send(org.apache.kafka.clients.producer.ProducerRecord("hello-log-store", testPersistId, curState))
        }

        // Wait for the state store to be ready
        Thread.sleep(5000)
    }

}