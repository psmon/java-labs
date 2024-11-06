package com.example.kotlinbootlabs.actor.eventbus

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.testkit.typed.javadsl.TestProbe
import akka.actor.typed.ActorRef
import akka.actor.typed.pubsub.Topic
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PubSubActorTest {

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

    // https://www.lightbend.com/blog/benchmarking-kafka-vs-akka-brokerless-pub-sub
    // Kafka 보다,더 빠른 Made PuSub 구현

    @Test
    fun testAkkaPubSub() {
        val topic: ActorRef<Topic.Command<String>> = testKit.spawn(Topic.create(String::class.java,
            "pubsub-topic"))
        val probe = testKit.createTestProbe<String>()

        topic.tell(Topic.subscribe(probe.ref))
        topic.tell(Topic.publish("Test Message"))
        probe.expectMessage("Test Message")
    }

    @Test
    fun testAkkaPubSubPerformance() {
        val numberOfTopics = 10000
        val numberOfSubscribers = 100000
        val topics = mutableListOf<ActorRef<Topic.Command<String>>>()
        val probes = mutableListOf<TestProbe<String>>()

        // Create topics
        for (i in 1..numberOfTopics) {
            val topic = testKit.spawn(Topic.create(String::class.java, "topic-$i"))
            topics.add(topic)
        }

        // Create subscribers and subscribe them to topics
        for (i in 1..numberOfSubscribers) {
            val probe = testKit.createTestProbe<String>()
            val topicIndex = (i % numberOfTopics)
            topics[topicIndex].tell(Topic.subscribe(probe.ref))
            probes.add(probe)
        }

        // Measure the time taken to publish and receive messages
        val startTime = System.currentTimeMillis()

        // Publish messages to each topic
        for (i in 1..numberOfTopics) {
            topics[i - 1].tell(Topic.publish("Hello PubSub"))
        }

        // Verify that each subscriber receives the correct message
        probes.forEachIndexed { index, probe ->
            val topicIndex = (index % numberOfTopics) + 1
            probe.expectMessage("Hello PubSub")
        }

        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime

        println("Total messages published: $numberOfTopics")
        println("Total messages received: $numberOfSubscribers")
        println("Elapsed time: $elapsedTime ms")
    }


    @Test
    fun testPublishMessage() {
        val probe = testKit.createTestProbe<String>()
        val probe2 = testKit.createTestProbe<String>()

        val pubSubActor = testKit.spawn(PubSubActor.create())
        pubSubActor.tell(Subscribe("ch1",probe.ref))
        pubSubActor.tell(Subscribe("ch2",probe2.ref))

        pubSubActor.tell(PublishMessage("ch1","Hello, World!"))
        pubSubActor.tell(PublishMessage("ch2","Hello, World2!"))

        probe.expectMessage("Hello, World!")
        probe2.expectMessage("Hello, World2!")
    }

}