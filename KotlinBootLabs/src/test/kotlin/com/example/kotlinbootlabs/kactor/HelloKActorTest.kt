package com.example.kotlinbootlabs.kactor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import com.example.kotlinbootlabs.kactor.HelloKActor.*


class HelloKActorTest {
    companion object {
        private const val TIMEOUT_DURATION = 3000L // Timeout duration in milliseconds
    }

    private val actor = HelloKActor()

    @AfterEach
    fun tearDown() {
        actor.stop()
    }

    @Test
    fun testHelloCommand() = runBlocking {

        val response = CompletableDeferred<HelloKActorResponse>()
        actor.send(Hello("Hello", response))
        val result = withTimeout(TIMEOUT_DURATION) { response.await() } as HelloResponse

        //The response message should be 'Kotlin'
        assertEquals("Kotlin", result.message, "Kotlin")
    }

    @Test
    fun testGetHelloCountCommand() = runBlocking {

        val response1 = CompletableDeferred<HelloKActorResponse>()
        actor.send(Hello("Hello", response1))
        withTimeout(TIMEOUT_DURATION) { response1.await() }

        val response2 = CompletableDeferred<HelloKActorResponse>()
        actor.send(GetHelloCount(response2))
        val result = withTimeout(TIMEOUT_DURATION) { response2.await() } as HelloCountResponse
        assertEquals(1, result.count, "The hello count should be 1")
    }

    @Test
    fun testInvalidMessage() {

        val response = CompletableDeferred<HelloKActorResponse>()
        try {
            runBlocking {
                withTimeout(TIMEOUT_DURATION) {
                    actor.send(Hello("InvalidMessage", response))
                }
            }
        } catch (e: RuntimeException) {
            assertEquals("Invalid message received!", e.message, "The exception message should be 'Invalid message received!'")
        }
    }
}