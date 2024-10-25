package com.example.kotlinbootlabs.actor

import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MainStageActorTest {

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
    fun testCreateSocketSessionManager() {
        val probe = testKit.createTestProbe<MainStageActorResponse>()
        val mainStageActor: ActorRef<MainStageActorCommand> = testKit.spawn(MainStageActor.create())
        mainStageActor.tell(CreateSocketSessionManager(probe.ref))
        probe.expectMessageClass(SocketSessionManagerCreated::class.java)

    }

    @Test
    fun testCreateSupervisorChannelActor() {
        val probe = testKit.createTestProbe<MainStageActorResponse>()
        val mainStageActor: ActorRef<MainStageActorCommand> = testKit.spawn(MainStageActor.create())
        mainStageActor.tell(CreateSupervisorChannelActor(probe.ref))
        probe.expectMessageClass(SupervisorChannelActorCreated::class.java)
    }

}