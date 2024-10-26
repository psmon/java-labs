package com.example.kotlinbootlabs.actor.persistent

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe

import org.apache.pekko.persistence.typed.PersistenceId
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

        // Test in HAPPY state
        helloPersistentDurableStateActor.tell(ChangeState(State.HAPPY))
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Kotlin"))

        helloPersistentDurableStateActor.tell(GetHelloTotalCountPersitentDurable(probe.ref()))
        probe.expectMessage(HelloCountResponse(1))

        // Change state to ANGRY
        helloPersistentDurableStateActor.tell(ChangeState(State.ANGRY))

        // Test in ANGRY state
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Don't talk to me!"))

        helloPersistentDurableStateActor.tell(GetHelloTotalCountPersitentDurable(probe.ref()))
        probe.expectMessage(HelloCountResponse(1)) // Count should not change
    }

    @Test
    fun testResetHelloCount() {
        val probe: TestProbe<Any> = testKit.createTestProbe()
        val persistenceId = PersistenceId.ofUniqueId("HelloPersistentStateActor2")
        val helloPersistentDurableStateActor = testKit.spawn(HelloPersistentDurableStateActor.create(persistenceId))

        // Send Hello messages
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))
        helloPersistentDurableStateActor.tell(HelloPersistentDurable("Hello", probe.ref()))

        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))

        // Verify the hello count
        helloPersistentDurableStateActor.tell(GetHelloCountPersistentDurable(probe.ref()))
        probe.expectMessage(HelloCountResponse(2))

        // Reset the hello count
        helloPersistentDurableStateActor.tell(ResetHelloCount)

        // Verify the hello count is reset
        helloPersistentDurableStateActor.tell(GetHelloCountPersistentDurable(probe.ref()))
        probe.expectMessage(HelloCountResponse(0))
    }
}