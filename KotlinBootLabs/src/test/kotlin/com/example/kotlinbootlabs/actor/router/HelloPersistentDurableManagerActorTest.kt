package com.example.kotlinbootlabs.actor.router

import com.example.kotlinbootlabs.actor.HelloActorResponse
import com.example.kotlinbootlabs.actor.HelloResponse
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit
import org.apache.pekko.actor.testkit.typed.javadsl.TestProbe
import org.apache.pekko.actor.typed.ActorRef
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HelloPersistentDurableManagerActorTest {

    companion object {
        private val testKit = ActorTestKit.create()

        @JvmStatic
        @BeforeAll
        fun setup() {
            // Setup code if needed
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testSendHelloMessage() {
        val helloManager: ActorRef<HelloManagerCommand> = testKit.spawn(HelloManagerActor.create())
        val probe = TestProbe.create<HelloActorResponse>(testKit.system())

        helloManager.tell(DistributedHelloMessage("Hello", probe.ref()))
        helloManager.tell(DistributedHelloMessage("Hello", probe.ref()))
        helloManager.tell(DistributedHelloMessage("Hello", probe.ref()))
        helloManager.tell(DistributedHelloMessage("Hello", probe.ref()))
        helloManager.tell(DistributedHelloMessage("Hello", probe.ref()))

        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))
        probe.expectMessage(HelloResponse("Kotlin"))

    }
}