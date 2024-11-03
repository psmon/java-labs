package com.example.kotlinbootlabs.actor.cluster

import akka.actor.Address
import akka.actor.AddressFromURIString
import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.typed.Cluster
import akka.cluster.typed.JoinSeedNodes
import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class ClusterActorTest {

    companion object {
        private lateinit var testKitA: ActorTestKit
        private lateinit var testKitB: ActorTestKit

        private lateinit var actorA: ActorRef<HelloActorACommand>
        private lateinit var actorB: ActorRef<HelloActorBCommand>


        @BeforeAll
        @JvmStatic
        fun setup() {
            val configA = ConfigFactory.load("cluster1.conf")
            val configB = ConfigFactory.load("cluster2.conf")

            testKitA = ActorTestKit.create(configA)
            testKitB = ActorTestKit.create(configB)

            actorA = testKitA.spawn(ClusterHelloActorA.create(),"localActorA")
            actorB = testKitB.spawn(ClusterHelloActorB.create(),"localActorB")

            val clusterA = Cluster.get(testKitA.system())
            val clusterB = Cluster.get(testKitB.system())

            if (clusterA.selfMember().hasRole("helloA")) {
                actorA = testKitA.spawn(ClusterHelloActorA.create(), "actorA")
            }

            if (clusterB.selfMember().hasRole("helloB")) {
                actorB = testKitB.spawn(ClusterHelloActorB.create(), "actorB")
            }
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKitB.shutdownTestKit()
            testKitA.shutdownTestKit()
        }
    }

    @Test
    fun testClusterHelloActorAandBCommunicationByLocal() {
        val probeA = testKitA.createTestProbe<HelloActorAResponse>()
        val probeB = testKitB.createTestProbe<HelloActorBResponse>()

        // HelloActorA sends a message to HelloActorB
        actorA.tell(HelloA("Hello", probeA.ref))
        probeA.expectMessage(HelloAResponse("Kotlin"))

        actorB.tell(HelloB("Hello", probeB.ref))
        probeB.expectMessage(HelloBResponse("Kotlin"))
    }

    @Test
    fun testClusterHelloActorAandBCommunicationByRemotePath() {

        val probeA = testKitA.createTestProbe<HelloActorAResponse>()
        val probeB = testKitB.createTestProbe<HelloActorBResponse>()

        // Select actors by their paths
        val actorASelection = testKitA.system().classicSystem()
            .actorSelection("akka://ClusterActorTest@127.0.0.1:2551/user/localActorA")

        val actorBSelection = testKitB.system().classicSystem()
            .actorSelection("akka://ClusterActorTest@127.0.0.1:2552/user/localActorB")

        // Send messages to the selected actors
        actorASelection.tell(HelloA("Hello", probeA.ref), null)
        probeA.expectMessage(HelloAResponse("Kotlin"))

        actorBSelection.tell(HelloB("Hello", probeB.ref), null)
        probeB.expectMessage(HelloBResponse("Kotlin"))

    }

}