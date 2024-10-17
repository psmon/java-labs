package com.example.kotlinbootlabs.ws.actor

import akka.actor.testkit.typed.javadsl.ActorTestKit
import akka.actor.typed.ActorRef
import com.example.kotlinbootlabs.actor.HelloActorResponse
import com.example.kotlinbootlabs.actor.HelloResponse
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration


class PrivacyRoomActorTest {

    companion object {
        private val testKit = ActorTestKit.create()

        @BeforeAll
        @JvmStatic
        fun setup() {
            // Setup code if needed
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            testKit.shutdownTestKit()
        }
    }

    @Test
    fun testSendMessage() {
        val probe = testKit.createTestProbe<HelloActorResponse>()
        for(i in 1..1000) {
            val identifier = "testIdentifier-$i"
            val privacyRoomActor: ActorRef<PrivacyRoomCommand> = testKit.spawn(PrivacyRoomActor.create(identifier))
            privacyRoomActor.tell(SetTestProbe(probe.ref))
        }

        for(i in 1..1000) {
            probe.expectMessage(Duration.ofSeconds(3), HelloResponse("Kotlin"))
        }
    }
}