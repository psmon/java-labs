package com.example.kotlinbootlabs.kactor

import com.example.kotlinbootlabs.kafka.createKafkaProducer
import com.example.kotlinbootlabs.service.RedisService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExtendWith(SpringExtension::class)
@SpringBootTest
class HelloKTableActorTest {
    companion object {
        private const val TIMEOUT_DURATION = 3000L // Timeout duration in milliseconds
    }

    private lateinit var actor: HelloKTableActor
    private lateinit var producer: KafkaProducer<String, HelloKTableState>

    @Autowired
    lateinit var redisService: RedisService

    @BeforeTest
    fun setUp() {

        var testPersistId = "test-persistence-id-02"

        producer = createKafkaProducer()

        println("Creating actor with persistence ID: $testPersistId ")

        actor = HelloKTableActor(testPersistId, producer, redisService)


    }

    @AfterTest
    fun tearDown() {
        if (::producer.isInitialized) {
            producer.close(Duration.ofSeconds(3))
        }

        if (::actor.isInitialized) {
            actor.stop()
        }
    }

    @Test
    fun testHelloCommand() = runBlocking {
        actor.send(ChangeStateKtable(HelloKTableState(HelloKState.HAPPY, 0, 0)))
        CompletableDeferred<HelloKTableActorResponse>()

        val response = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(HelloKtable("Hello", response))
        val result = withTimeout(TIMEOUT_DURATION) { response.await() } as HelloKStateResponse

        // The response message should be 'Kotlin'
        assertEquals("Kotlin", result.message)
    }

    @Test
    fun testHelloCommandAndReadCountPerform() = runBlocking {
        actor.send(ChangeStateKtable(HelloKTableState(HelloKState.HAPPY, 0, 0)))
        CompletableDeferred<HelloKTableActorResponse>()

        val response = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(HelloKtable("Hello", response))
        val result = withTimeout(TIMEOUT_DURATION) { response.await() } as HelloKStateResponse

        // The response message should be 'Kotlin'
        assertEquals("Kotlin", result.message)


        // Verify Curren count
        val initialCountResponse = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(GetHelloKtableCount(initialCountResponse))
        val initialCountResult =
            withTimeout(TIMEOUT_DURATION) { initialCountResponse.await() } as HelloKStateCountResponse
        assertEquals(true, initialCountResult.count > 0)

        // Send Hello command 100 times
        repeat(100) {
            val response = CompletableDeferred<HelloKTableActorResponse>()
            actor.send(HelloKtable("Hello", response))
            withTimeout(TIMEOUT_DURATION) { response.await() }
        }

        var finalTotalcount = initialCountResult.count + 100

        // Verify count after 100 Hello commands
        val countAfter100Response = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(GetHelloKtableCount(countAfter100Response))
        val countAfter100Result =
            withTimeout(TIMEOUT_DURATION) { countAfter100Response.await() } as HelloKStateCountResponse
        assertEquals(finalTotalcount, countAfter100Result.count)

        // Measure time for 1000 Hello commands
        val startTime = System.currentTimeMillis()
        repeat(1000) {
            val countAfter100Response = CompletableDeferred<HelloKTableActorResponse>()
            actor.send(GetHelloKtableCount(countAfter100Response))
            val countAfter100Result =
                withTimeout(TIMEOUT_DURATION) { countAfter100Response.await() } as HelloKStateCountResponse
            assertEquals(finalTotalcount, countAfter100Result.count)
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val averageTime = totalTime / 1000.0

        println("Total time for 1000 Hello commands: $totalTime ms")
        println("Average time per Hello command: $averageTime ms")

        // Verify count after 1000 Hello commands
        val finalCountResponse = CompletableDeferred<HelloKTableActorResponse>()
        actor.send(GetHelloKtableCount(finalCountResponse))
        val finalCountResult = withTimeout(TIMEOUT_DURATION) { finalCountResponse.await() } as HelloKStateCountResponse
        assertEquals(finalTotalcount, finalCountResult.count)

    }

    @Test
    fun testGetHelloCountCommand() = runBlocking {
        actor.send(ChangeStateKtable(HelloKTableState(HelloKState.HAPPY, 0, 0)))
        CompletableDeferred<HelloKTableActorResponse>()

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