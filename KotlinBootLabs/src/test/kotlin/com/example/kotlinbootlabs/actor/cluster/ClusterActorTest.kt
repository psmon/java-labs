package com.example.kotlinbootlabs.actor.cluster

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.TestProbe
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.AskPattern
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletionStage

class ClusterActorTest {

    companion object {
        private lateinit var testKitA: ActorTestKit
        private lateinit var testKitB: ActorTestKit
        private lateinit var systemA: ActorSystem<*>
        private lateinit var systemB: ActorSystem<*>
        private lateinit var actorA: ActorRef<HelloActorACommand>
        private lateinit var actorB: ActorRef<HelloActorBCommand>

        @BeforeAll
        @JvmStatic
        fun setup() {
            val configA = ConfigFactory.load("cluster1.conf")
            val configB = ConfigFactory.load("cluster2.conf")

            testKitA = ActorTestKit.create(configA)
            testKitB = ActorTestKit.create(configB)

            systemA = testKitA.system()
            systemB = testKitB.system()

            Cluster.get(systemA).manager().tell(Join(Cluster.get(systemA).selfMember().address()))
            Cluster.get(systemB).manager().tell(Join(Cluster.get(systemA).selfMember().address()))

            actorA = testKitA.spawn(ClusterHelloActorA.create())
            actorB = testKitB.spawn(ClusterHelloActorB.create())

        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKitA.shutdownTestKit()
            testKitB.shutdownTestKit()
        }
    }

    @Test
    fun testClusterHelloActorAandBCommunication() {
        val probeA = testKitA.createTestProbe<HelloActorAResponse>()
        val probeB = testKitB.createTestProbe<HelloActorBResponse>()

        // HelloActorA sends a message to HelloActorB
        actorA.tell(HelloA("Hello", probeA.ref))
        probeA.expectMessage(HelloAResponse("Kotlin"))

        actorB.tell(HelloB("Hello", probeB.ref))
        probeB.expectMessage(HelloBResponse("Kotlin"))

    }
}