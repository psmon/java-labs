package com.example.kotlinbootlabs.actor

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class HelloActorTest {

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
    fun testHelloActorRespondsWithKotlin() {
        val probe = testKit.createTestProbe<HelloActorResponse>()

        val helloActor = testKit.spawn(HelloActor.create())

        helloActor.tell(Hello("Hello", probe.ref()))

        probe.expectMessage(HelloResponse("Kotlin"))

        helloActor.tell(GetHelloCount(probe.ref()))

        probe.expectMessage(HelloCountResponse(1))

    }
}