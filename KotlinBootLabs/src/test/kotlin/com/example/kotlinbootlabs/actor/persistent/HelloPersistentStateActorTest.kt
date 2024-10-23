package com.example.kotlinbootlabs.actor.persistent

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe
import org.apache.pekko.persistence.typed.PersistenceId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HelloPersistentStateActorTest {

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
    fun testHelloPersistentStateActorRespondsBasedOnState() {
        val probe: TestProbe<Any> = testKit.createTestProbe()
        val persistenceId = PersistenceId.ofUniqueId("hello-persistent-actor-1")
        val helloPersistentStateActor = testKit.spawn(HelloPersistentStateActor.create(persistenceId))

        // Test in HAPPY state
        helloPersistentStateActor.tell(Hello("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Kotlin"))

        helloPersistentStateActor.tell(GetHelloTotalCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(1))

        // Change state to ANGRY
        helloPersistentStateActor.tell(ChangeState(State.ANGRY))

        // Test in ANGRY state
        helloPersistentStateActor.tell(Hello("Hello", probe.ref()))
        probe.expectMessage(HelloResponse("Don't talk to me!"))

        helloPersistentStateActor.tell(GetHelloTotalCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(1)) // Count should not change
    }

    @Test
    fun testResetHelloCount() {
        val probe: TestProbe<Any> = testKit.createTestProbe()
        val persistenceId = PersistenceId.ofUniqueId("hello-persistent-actor22")
        val helloPersistentStateActor = testKit.spawn(HelloPersistentStateActor.create(persistenceId))

        // Send Hello messages
        helloPersistentStateActor.tell(Hello("Hello", probe.ref()))
        helloPersistentStateActor.tell(Hello("Hello", probe.ref()))

        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))

        // Verify the hello count
        helloPersistentStateActor.tell(GetHelloCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(2))

        // Reset the hello count
        helloPersistentStateActor.tell(ResetHelloCount)

        // Verify the hello count is reset
        helloPersistentStateActor.tell(GetHelloCount(probe.ref()))
        probe.expectMessage(HelloCountResponse(0))
    }
}