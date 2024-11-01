package com.example.kotlinbootlabs.actor.cluster

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.ManualTime
import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration

class ClusterActorTest {

    companion object {
        private lateinit var testKit: ActorTestKit
        private lateinit var manualTime: ManualTime

        @BeforeAll
        @JvmStatic
        fun setup() {
            val config = ManualTime.config().withFallback(ConfigFactory.defaultApplication())
            testKit = ActorTestKit.create(config)
            manualTime = ManualTime.get(testKit.system())
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testClusterHelloActorA() {
        val probe = testKit.createTestProbe<HelloActorAResponse>()
        val actorA = testKit.spawn(ClusterHelloActorA.create())

        actorA.tell(HelloA("Hello", probe.ref))
        probe.expectMessage(HelloAResponse("Kotlin"))
    }

    @Test
    fun testClusterHelloActorB() {
        val probe = testKit.createTestProbe<HelloActorBResponse>()
        val actorB = testKit.spawn(ClusterHelloActorB.create())

        actorB.tell(HelloB("Hello", probe.ref))
        probe.expectMessage(HelloBResponse("Kotlin"))
    }

    @Test
    fun testClusterHelloActorAandB() {
        val probeA = testKit.createTestProbe<HelloActorAResponse>()
        val probeB = testKit.createTestProbe<HelloActorBResponse>()
        val actorA = testKit.spawn(ClusterHelloActorA.create())
        val actorB = testKit.spawn(ClusterHelloActorB.create())

        actorA.tell(HelloA("Hello", probeA.ref))
        probeA.expectMessage(HelloAResponse("Kotlin"))

        actorB.tell(HelloB("Hello", probeB.ref))
        probeB.expectMessage(HelloBResponse("Kotlin"))
    }
}