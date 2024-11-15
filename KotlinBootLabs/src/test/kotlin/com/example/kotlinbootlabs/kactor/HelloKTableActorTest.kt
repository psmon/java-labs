package com.example.kotlinbootlabs.kactor

import com.example.kotlinbootlabs.kafka.createHelloKStreams
import com.example.kotlinbootlabs.kafka.createKafkaProducer
import com.example.kotlinbootlabs.kafka.getStateStoreWithRetries
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class HelloKTableActorTest {
    companion object {
        private const val TIMEOUT_DURATION = 3000L // Timeout duration in milliseconds
    }

    private lateinit var streams: KafkaStreams
    private lateinit var actor: HelloKTableActor
    private lateinit var producer: KafkaProducer<String, HelloKTableState>

    @BeforeTest
    fun setUp() {
        producer = createKafkaProducer()
        streams = createHelloKStreams()
        val latch = CountDownLatch(1)

        var isRunning = false

        streams.setStateListener { newState, _ ->
            if (newState == KafkaStreams.State.RUNNING) {
                latch.countDown()
                isRunning = true
            }
        }

        streams.start()
        latch.await(10, TimeUnit.SECONDS)

        if (!isRunning) {
            throw IllegalStateException("Kafka Streams application did not start")
        }

        var testPersistId = "test-persistence-id-02"

        val readStateStore: ReadOnlyKeyValueStore<String, HelloKTableState> =
            getStateStoreWithRetries(streams, "hello-state-store")

        var curState: HelloKTableState

        try {
            curState = readStateStore[testPersistId] ?: HelloKTableState(HelloKState.HAPPY, 0, 0)
            println("Found state in store: $curState")
        }
        catch (e: InvalidStateStoreException) {
            testPersistId = "test-persistence-id-01"
            curState = HelloKTableState(HelloKState.HAPPY, 0, 0)
            println("State not found in store, creating new state with persistence ID: $testPersistId")
        }

        println("Creating actor with persistence ID: $testPersistId ")

        actor = HelloKTableActor(testPersistId, streams, curState, producer)
    }

    @AfterTest
    fun tearDown() {
        producer.close(Duration.ofSeconds(3))
        streams.close(Duration.ofSeconds(3))
        if(::actor.isInitialized) {
            actor.stop()
        }
    }

    @Test
    fun testHelloCommand() = runBlocking {
        val response = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(HelloKtable("Hello", response))
        val result = withTimeout(TIMEOUT_DURATION) { response.await() } as HelloKStateResponse

        // The response message should be 'Kotlin'
        assertEquals("Kotlin", result.message)
    }

    @Test
    fun testGetHelloCountCommand() = runBlocking {
        val response1 = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(HelloKtable("Hello", response1))
        withTimeout(TIMEOUT_DURATION) { response1.await() }

        val response2 = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(GetHelloKtableCount(response2))
        val result = withTimeout(TIMEOUT_DURATION) { response2.await() } as HelloKStateCountResponse
        assertEquals(1, result.count)
    }

    @Test
    fun testChangeStateCommand() = runBlocking {
        actor.send(ChangeStateKtable(HelloKTableState(HelloKState.ANGRY, 0, 0)))
        val response = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(HelloKtable("Hello", response))
        val result = withTimeout(TIMEOUT_DURATION) { response.await() } as HelloKStateResponse
        assertEquals("Don't talk to me!", result.message)
    }

    @Test
    fun testResetHelloCountCommand() = runBlocking {
        actor.send(ResetKTableHelloCount)
        val response = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(GetHelloKtableCount(response))
        val result = withTimeout(TIMEOUT_DURATION) { response.await() } as HelloKStateCountResponse
        assertEquals(0, result.count)
    }
}