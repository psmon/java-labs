package com.example.kotlinbootlabs.actor.cluster

import akka.actor.Address
import akka.actor.AddressFromURIString
import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
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

        private lateinit var standAlone: ActorTestKit

        private lateinit var actorA: ActorRef<HelloActorACommand>
        private lateinit var actorB: ActorRef<HelloActorBCommand>

        private lateinit var actorAO: ActorRef<HelloActorACommand>
        private lateinit var actorBO: ActorRef<HelloActorBCommand>

        @BeforeAll
        @JvmStatic
        fun setup() {
            val cluster1 = ConfigFactory.load("cluster1.conf")
            val cluster2 = ConfigFactory.load("cluster2.conf")
            val standalone = ConfigFactory.load("standalone.conf")

            testKitA = ActorTestKit.create(cluster1)
            testKitB = ActorTestKit.create(cluster2)
            standAlone = ActorTestKit.create(standalone)

            actorA = testKitA.spawn(ClusterHelloActorA.create(),"localActorA")
            actorB = testKitB.spawn(ClusterHelloActorB.create(),"localActorB")
            actorAO = standAlone.spawn(ClusterHelloActorA.create(), "localActorA")
            actorBO = standAlone.spawn(ClusterHelloActorB.create(), "localActorB")

            val clusterA = Cluster.get(testKitA.system())
            val clusterB = Cluster.get(testKitB.system())
            val standAloneSystem = Cluster.get(standAlone.system())

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
            standAlone.shutdownTestKit()
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

        // HelloActorA sends a message to HelloActorB
        actorAO.tell(HelloA("Hello", probeA.ref))
        probeA.expectMessage(HelloAResponse("Kotlin"))

        actorBO.tell(HelloB("Hello", probeB.ref))
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

    @Test
    fun testClusterPubSub() {

        val mediator = DistributedPubSub.get(standAlone.system()).mediator()
        val probeA = standAlone.createTestProbe<HelloActorAResponse>()
        val probeB = standAlone.createTestProbe<HelloActorBResponse>()

        mediator.tell(DistributedPubSubMediator.Publish("roleA", HelloA("Hello", probeA.ref)), null)
        mediator.tell(DistributedPubSubMediator.Publish("roleB", HelloB("Hello", probeB.ref)), null)

        probeA.expectMessage(HelloAResponse("Kotlin"))
        probeB.expectMessage(HelloBResponse("Kotlin"))
    }

}