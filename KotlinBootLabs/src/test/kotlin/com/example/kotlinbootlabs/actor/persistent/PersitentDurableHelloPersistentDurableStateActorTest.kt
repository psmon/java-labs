package com.example.kotlinbootlabs.actor.persistent

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.TestProbe

import akka.persistence.typed.PersistenceId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PersitentDurableHelloPersistentDurableStateActorTest {

    companion object {

        private lateinit var testKit: ActorTestKit

        @BeforeAll
        @JvmStatic
        fun setup() {
            testKit = ActorTestKit.create()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testSerialization() {
        val objectMapper = ObjectMapper()
        val state = HelloState(State.HAPPY, 123, 123)
        val json = objectMapper.writeValueAsString(state)
        val deserializedState = objectMapper.readValue(json, HelloState::class.java)
        assertEquals(state, deserializedState)
    }

    @Test
    fun testSerialization2() {
        val objectMapper = ObjectMapper().apply {
            configure(DeserializationFeature.USE_LONG_FOR_INTS, true)
        }
        val state = HelloState(State.HAPPY, 123, 123)
        val json = objectMapper.writeValueAsString(state)
        val deserializedState = objectMapper.readValue(json, HelloState::class.java)
        assertEquals(state, deserializedState)
    }

    @Test
    fun testHelloPersistentStateActorRespondsBasedOnState() {

        val probe: TestProbe<Any> = testKit.createTestProbe()
        val persistenceId = PersistenceId.ofUniqueId("HelloPersistentStateActor1")
        val helloPersistentDurableStateActor = testKit.spawn(HelloPersistentDurableStateActor.create(persistenceId))

        helloPersistentDurableStateActor.tell(GetHelloTotalCountPersitentDurable(probe.ref()))

        val response = probe.expectMessageClass(HelloCountResponse::class.java)

        val totalCount: Number = response.count

        // Test in HAPPY state
        helloPersistentDurableStateActor.tell(ChangeState(State.HAPPY))
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Kotlin"))

        val increaseCount :Number = totalCount.toInt() + 1

        helloPersistentDurableStateActor.tell(GetHelloTotalCountPersitentDurable(probe.ref()))
        probe.expectMessage(HelloCountResponse(increaseCount))

        // Change state to ANGRY
        helloPersistentDurableStateActor.tell(ChangeState(State.ANGRY))

        // Test in ANGRY state
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Don't talk to me!"))

        helloPersistentDurableStateActor.tell(GetHelloTotalCountPersitentDurable(probe.ref()))
        probe.expectMessage(HelloCountResponse(increaseCount)) // Count should not change
    }

    @Test
    fun testResetHelloCount() {
        val probe: TestProbe<Any> = testKit.createTestProbe()
        val persistenceId = PersistenceId.ofUniqueId("HelloPersistentStateActor2")
        val helloPersistentDurableStateActor = testKit.spawn(HelloPersistentDurableStateActor.create(persistenceId))

        helloPersistentDurableStateActor.tell(GetHelloTotalCountPersitentDurable(probe.ref()))
        val response = probe.expectMessageClass(HelloCountResponse::class.java)
        val totalCount: Number = response.count

        // Send Hello messages
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))

        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))

        val increaseCount :Number = totalCount.toInt() + 2

        // Verify the hello count
        helloPersistentDurableStateActor.tell(GetHelloCountPersistentDurable(probe.ref()))
        val updatedResponse = probe.expectMessageClass(HelloCountResponse::class.java)
        assertEquals(increaseCount.toInt(), updatedResponse.count)

        // Reset the hello count
        helloPersistentDurableStateActor.tell(ResetHelloCount)

        // Verify the hello count is reset
        helloPersistentDurableStateActor.tell(GetHelloCountPersistentDurable(probe.ref()))
        val resetResponse = probe.expectMessageClass(HelloCountResponse::class.java)
        assertEquals(HelloCountResponse(0), resetResponse)
    }
}