package com.example.kotlinbootlabs.actor.cluster

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.cluster.typed.Cluster
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

            testKitA = ActorTestKit.create("ClusterSystem",cluster1)
            testKitB = ActorTestKit.create("ClusterSystem",cluster2)
            standAlone = ActorTestKit.create("StandAloneSystem",standalone)

            val clusterA = Cluster.get(testKitA.system())
            val clusterB = Cluster.get(testKitB.system())
            val standAloneSystem = Cluster.get(standAlone.system())

            // Role에 따라 작동하는 Actor 구분생성
            if (clusterA.selfMember().hasRole("helloA")) {
                actorA = testKitA.spawn(ClusterHelloActorA.create(), "actorA")
            }

            if (clusterB.selfMember().hasRole("helloB")) {
                actorB = testKitB.spawn(ClusterHelloActorB.create(), "actorB")
            }

            if(standAloneSystem.selfMember().hasRole("helloA") && standAloneSystem.selfMember().hasRole("helloB")) {
                actorAO = standAlone.spawn(ClusterHelloActorA.create(), "localActorA")
                actorBO = standAlone.spawn(ClusterHelloActorB.create(), "localActorB")
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
            .actorSelection("akka://ClusterSystem@127.0.0.1:2551/user/actorA")

        val actorBSelection = testKitB.system().classicSystem()
            .actorSelection("akka://ClusterSystem@127.0.0.1:2552/user/actorB")

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

    @Test
    fun testClusterRouter(){
        val probeA = testKitB.createTestProbe<Receptionist.Listing>()
        val probeB = testKitB.createTestProbe<HelloActorAResponse>()

        var list = testKitB.system().receptionist()
        list.tell(Receptionist.find(ClusterHelloActorA.ClusterHelloActorAKey, probeA.ref))

        val listing = probeA.receiveMessage()
        val router = listing.getServiceInstances(ClusterHelloActorA.ClusterHelloActorAKey)

        router.forEach {
            it.tell(HelloA("Hello", probeB.ref))
            probeB.expectMessage(HelloAResponse("Kotlin"))
        }
    }

}